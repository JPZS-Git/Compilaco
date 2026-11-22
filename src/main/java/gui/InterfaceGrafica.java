package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

import parser.ArquivoFonte;
import parser.Compilaco;
import parser.CompilacoConstants;
import parser.ErroCompilacao;
import parser.LexicoComRecuperacao;
import parser.No;
import parser.SimpleCharStream;
import parser.TipoErro;
import parser.Token;

/**
 * Interface gráfica do Compilaço.
 *
 * Exibe o código sendo elaborado (com numeração e contagem de linhas), os
 * tokens reconhecidos, o texto de aceitação ou não, as mensagens de erro, a
 * árvore sintática e o passo a passo do modo pânico. As linhas com erro são
 * realçadas no próprio editor.
 *
 * A interface não faz análise nenhuma: ela só chama o compilador
 * (Compilaco.criar(...).analisar()) e mostra o resultado.
 *
 * Execução: java -cp out gui.InterfaceGrafica [arquivo]
 */
public class InterfaceGrafica extends JFrame {

    private static final String MODELO = "apito_inicial()\n\n\n\nfim_de_jogo\n";

    private static final Color VERDE = new Color(0x1E7D32);
    private static final Color VERMELHO = new Color(0xB00020);
    private static final Color CINZA = new Color(0x6B6B6B);

    private static final int ABA_TOKENS = 0;
    private static final int ABA_ERROS = 1;
    private static final int ABA_ARVORE = 2;
    private static final int ABA_PANICO = 3;

    private final ArquivoFonte arquivo = new ArquivoFonte();

    private List<ErroCompilacao> errosAtuais = List.of();
    private boolean modificado = false;

    private final JTextArea editor = new JTextArea() {
        /** Ao parar o mouse sobre uma linha com erro, mostra a mensagem. */
        @Override
        public String getToolTipText(MouseEvent e) {
            try {
                int linha = getLineOfOffset(viewToModel2D(e.getPoint())) + 1;
                String mensagens = errosAtuais.stream()
                        .filter(erro -> erro.linha() == linha)
                        .map(erro -> "[" + erro.tipo() + "] " + escapar(erro.mensagem()))
                        .collect(Collectors.joining("<br>"));
                return mensagens.isEmpty() ? null : "<html>" + mensagens + "</html>";
            } catch (BadLocationException ex) {
                return null;
            }
        }
    };

    private final NumeracaoLinhas numeracao = new NumeracaoLinhas(editor);
    private final RealceErros realce = new RealceErros(editor);

    private final DefaultTableModel modeloTokens = criarModelo("Nº", "Lexema", "Token", "Linha", "Coluna");
    private final DefaultTableModel modeloErros = criarModelo("Tipo", "Linha", "Coluna", "Mensagem");
    private final JTable tabelaTokens = new JTable(modeloTokens);
    private final JTable tabelaErros = new JTable(modeloErros);
    private final JTree arvore = new JTree(new DefaultMutableTreeNode("Compile o código (F5) para ver a árvore"));
    private final JTextArea passosPanico = new JTextArea();
    private final JTextArea detalheErro = new JTextArea();
    private final JTabbedPane abas = new JTabbedPane();

    private final JLabel status = new JLabel();
    private final JLabel resultado = new JLabel();

    /** Nome de cada tipo de token, como na gramática (TIPOINT, IDENTIFICADOR...). */
    private static final Map<Integer, String> NOMES_TOKENS = nomesDosTokens();

    public InterfaceGrafica() {
        super("Compilaço");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sair();
            }
        });

        montarEditor();
        montarAbas();

        JSplitPane divisao = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, painelEditor(), abas);
        divisao.setResizeWeight(0.55);
        divisao.setDividerLocation(640);
        divisao.setBorder(null);

        JPanel conteudo = new JPanel(new BorderLayout());
        conteudo.add(barraDeFerramentas(), BorderLayout.NORTH);
        conteudo.add(divisao, BorderLayout.CENTER);
        conteudo.add(barraDeStatus(), BorderLayout.SOUTH);
        setContentPane(conteudo);
        setJMenuBar(menu());

        editor.setText(MODELO);
        editor.setCaretPosition("apito_inicial()\n".length());
        modificado = false;
        mostrarResultadoPendente("Escreva o código e compile (F5)");
        atualizarTitulo();
        atualizarStatus();

        setSize(1200, 760);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------
    // Montagem da tela
    // ------------------------------------------------------------------

    private void montarEditor() {
        editor.setFont(fonteMonoespacada(15));
        editor.setTabSize(4);
        editor.setMargin(new java.awt.Insets(4, 8, 4, 8));
        numeracao.setFont(editor.getFont());
        ToolTipManager.sharedInstance().registerComponent(editor);

        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                codigoAlterado();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                codigoAlterado();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        editor.addCaretListener(e -> atualizarStatus());
    }

    private JScrollPane painelEditor() {
        JScrollPane rolagem = new JScrollPane(editor);
        rolagem.setRowHeaderView(numeracao);
        rolagem.setBorder(BorderFactory.createEmptyBorder());
        return rolagem;
    }

    private void montarAbas() {
        configurarTabela(tabelaTokens, 45, 160, 160, 55, 60);
        configurarTabela(tabelaErros, 95, 50, 60, 600);
        tabelaErros.getSelectionModel().addListSelectionListener(e -> {
            int linha = tabelaErros.getSelectedRow();
            if (!e.getValueIsAdjusting() && linha >= 0) {
                detalheErro.setText(modeloErros.getValueAt(linha, 0) + " — linha "
                        + modeloErros.getValueAt(linha, 1) + ", coluna " + modeloErros.getValueAt(linha, 2)
                        + "\n" + modeloErros.getValueAt(linha, 3));
                irPara((int) modeloErros.getValueAt(linha, 1), (int) modeloErros.getValueAt(linha, 2));
            }
        });

        detalheErro.setEditable(false);
        detalheErro.setLineWrap(true);
        detalheErro.setWrapStyleWord(true);
        detalheErro.setRows(3);
        detalheErro.setMargin(new java.awt.Insets(6, 8, 6, 8));
        detalheErro.setBackground(new Color(0xFFF6F6));
        detalheErro.setText("Clique num erro para ver a mensagem completa e ir até a linha no editor.");

        arvore.setCellRenderer(new RenderizadorArvore());
        arvore.setFont(fonteMonoespacada(13));
        arvore.setRowHeight(0);

        passosPanico.setEditable(false);
        passosPanico.setLineWrap(true);
        passosPanico.setWrapStyleWord(true);
        passosPanico.setFont(fonteMonoespacada(13));
        passosPanico.setMargin(new java.awt.Insets(6, 8, 6, 8));
        passosPanico.setText("Compile o código (F5). Se houver erros, aqui aparece como o modo pânico se recuperou de cada um.");

        abas.addTab("Tokens", new JScrollPane(tabelaTokens));
        JPanel painelErros = new JPanel(new BorderLayout());
        painelErros.add(new JScrollPane(tabelaErros), BorderLayout.CENTER);
        painelErros.add(new JScrollPane(detalheErro), BorderLayout.SOUTH);
        abas.addTab("Erros", painelErros);
        abas.addTab("Árvore sintática", new JScrollPane(arvore));
        abas.addTab("Modo pânico", new JScrollPane(passosPanico));
    }

    private JToolBar barraDeFerramentas() {
        JToolBar barra = new JToolBar();
        barra.setFloatable(false);
        barra.add(new JButton(acaoNovo));
        barra.add(new JButton(acaoAbrir));
        barra.add(new JButton(acaoSalvar));
        barra.add(new JButton(acaoSalvarComo));
        barra.addSeparator();
        JButton compilar = new JButton(acaoCompilar);
        compilar.setFont(compilar.getFont().deriveFont(Font.BOLD));
        barra.add(compilar);
        return barra;
    }

    private JMenuBar menu() {
        JMenu arquivoMenu = new JMenu("Arquivo");
        arquivoMenu.add(acaoNovo);
        arquivoMenu.add(acaoAbrir);
        arquivoMenu.add(acaoSalvar);
        arquivoMenu.add(acaoSalvarComo);
        arquivoMenu.addSeparator();
        arquivoMenu.add(acaoSair);

        JMenu compilarMenu = new JMenu("Compilar");
        compilarMenu.add(acaoCompilar);

        JMenuBar barra = new JMenuBar();
        barra.add(arquivoMenu);
        barra.add(compilarMenu);
        return barra;
    }

    private JPanel barraDeStatus() {
        status.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        resultado.setOpaque(true);
        resultado.setFont(resultado.getFont().deriveFont(Font.BOLD));
        resultado.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));

        JPanel barra = new JPanel(new BorderLayout());
        barra.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xD0D0D0)));
        barra.add(status, BorderLayout.WEST);
        barra.add(resultado, BorderLayout.EAST);
        return barra;
    }

    // ------------------------------------------------------------------
    // Ações (botões, menu e atalhos)
    // ------------------------------------------------------------------

    private final Action acaoNovo = acao("Novo", KeyEvent.VK_N, 0, e -> novo());
    private final Action acaoAbrir = acao("Abrir", KeyEvent.VK_O, 0, e -> abrir());
    private final Action acaoSalvar = acao("Salvar", KeyEvent.VK_S, 0, e -> salvar(false));
    private final Action acaoSalvarComo = acao("Salvar como", KeyEvent.VK_S, InputEvent.SHIFT_DOWN_MASK, e -> salvar(true));
    private final Action acaoCompilar = acaoSemCtrl("Compilar (F5)", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), e -> compilar());
    private final Action acaoSair = acao("Sair", KeyEvent.VK_Q, 0, e -> sair());

    private void novo() {
        if (!podeDescartar()) {
            return;
        }
        arquivo.novo();
        editor.setText(MODELO);
        editor.setCaretPosition("apito_inicial()\n".length());
        modificado = false;
        limparResultados("Escreva o código e compile (F5)");
        atualizarTitulo();
    }

    private void abrir() {
        if (!podeDescartar()) {
            return;
        }
        try {
            String codigo = arquivo.abrir(this);
            if (codigo != null) {
                carregar(codigo);
            }
        } catch (IOException e) {
            mostrarFalha("Não foi possível abrir o arquivo", e);
        }
    }

    /** Abre um arquivo já conhecido (passado por argumento). */
    public void abrir(Path caminho) {
        try {
            carregar(arquivo.abrir(caminho));
        } catch (IOException e) {
            mostrarFalha("Não foi possível abrir o arquivo", e);
        }
    }

    private void carregar(String codigo) {
        editor.setText(codigo);
        editor.setCaretPosition(0);
        modificado = false;
        limparResultados("Arquivo aberto: compile (F5)");
        atualizarTitulo();
    }

    private boolean salvar(boolean perguntarNome) {
        try {
            boolean salvou = perguntarNome
                    ? arquivo.salvarComo(this, editor.getText())
                    : arquivo.salvar(this, editor.getText());
            if (salvou) {
                modificado = false;
                atualizarTitulo();
            }
            return salvou;
        } catch (IOException e) {
            mostrarFalha("Não foi possível salvar o arquivo", e);
            return false;
        }
    }

    private void sair() {
        if (podeDescartar()) {
            dispose();
            System.exit(0);
        }
    }

    /** Pergunta o que fazer com alterações não salvas. */
    private boolean podeDescartar() {
        if (!modificado) {
            return true;
        }
        int resposta = JOptionPane.showConfirmDialog(this,
                "O código foi alterado. Deseja salvar antes de continuar?",
                "Alterações não salvas",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (resposta == JOptionPane.YES_OPTION) {
            return salvar(false);
        }
        return resposta == JOptionPane.NO_OPTION;
    }

    // ------------------------------------------------------------------
    // Compilação: chama o compilador e mostra o resultado
    // ------------------------------------------------------------------

    private void compilar() {
        String codigo = editor.getText();
        List<ErroCompilacao> erros = new ArrayList<>();
        List<String> passos = List.of();
        No raiz;

        try {
            Compilaco parser = Compilaco.criar(new StringReader(codigo));
            raiz = parser.analisar();
            erros.addAll(parser.erros);
            passos = parser.rastro;
        } catch (RuntimeException e) {
            raiz = No.erro("falha interna do compilador");
            erros.add(new ErroCompilacao(TipoErro.INTERNO, 0, 0,
                    "Erro interno do compilador: " + e.getMessage()));
        }

        // erros léxicos e sintáticos na ordem em que aparecem no código
        erros.sort(Comparator.comparingInt(ErroCompilacao::linha)
                .thenComparingInt(ErroCompilacao::coluna));
        errosAtuais = List.copyOf(erros);

        mostrarTokens(codigo);
        mostrarErros(erros);
        mostrarArvore(raiz);
        mostrarPassosPanico(passos);

        realce.marcar(erros);
        numeracao.setLinhasComErro(erros.stream()
                .map(ErroCompilacao::linha)
                .filter(linha -> linha > 0)
                .collect(Collectors.toSet()));

        if (erros.isEmpty()) {
            mostrarResultado("GOL! Código aceito: análise léxica e sintática sem erros", VERDE);
            abas.setSelectedIndex(ABA_ARVORE);
        } else {
            mostrarResultado("FALTA GRAVE: código não aceito. O VAR encontrou "
                    + erros.size() + (erros.size() == 1 ? " erro" : " erros"), VERMELHO);
            abas.setSelectedIndex(ABA_ERROS);
        }
    }

    private void mostrarTokens(String codigo) {
        modeloTokens.setRowCount(0);
        LexicoComRecuperacao lexico = new LexicoComRecuperacao(
                new SimpleCharStream(new StringReader(codigo)), new ArrayList<>());
        int n = 0;
        for (Token t = lexico.getNextToken(); t.kind != CompilacoConstants.EOF; t = lexico.getNextToken()) {
            modeloTokens.addRow(new Object[] {
                    ++n, t.image, NOMES_TOKENS.getOrDefault(t.kind, String.valueOf(t.kind)),
                    t.beginLine, t.beginColumn
            });
        }
        abas.setTitleAt(ABA_TOKENS, "Tokens (" + n + ")");
    }

    private void mostrarErros(List<ErroCompilacao> erros) {
        modeloErros.setRowCount(0);
        for (ErroCompilacao erro : erros) {
            modeloErros.addRow(new Object[] {
                    nomeDoTipo(erro.tipo()), erro.linha(), erro.coluna(), erro.mensagem()
            });
        }
        abas.setTitleAt(ABA_ERROS, "Erros (" + erros.size() + ")");
        detalheErro.setText(erros.isEmpty()
                ? "Nenhum erro: o código foi aceito."
                : "Clique num erro para ver a mensagem completa e ir até a linha no editor.");
    }

    private void mostrarArvore(No raiz) {
        arvore.setModel(new DefaultTreeModel(converter(raiz)));
        for (int i = 0; i < arvore.getRowCount(); i++) {
            arvore.expandRow(i);
        }
    }

    private void mostrarPassosPanico(List<String> passos) {
        if (passos.isEmpty()) {
            passosPanico.setText("Nenhum erro sintático: o modo pânico não precisou agir.");
        } else {
            passosPanico.setText("Recuperação de erros (modo pânico)\n"
                    + "Em cada erro, o parser descarta tokens até achar um do conjunto de sincronização.\n\n"
                    + String.join("\n", passos));
        }
        passosPanico.setCaretPosition(0);
    }

    /** Converte a árvore do compilador (No) em nós do JTree. */
    private static DefaultMutableTreeNode converter(No no) {
        DefaultMutableTreeNode item = new DefaultMutableTreeNode(no);
        for (No filho : no.getFilhos()) {
            item.add(converter(filho));
        }
        return item;
    }

    private void limparResultados(String mensagem) {
        errosAtuais = List.of();
        realce.limpar();
        numeracao.setLinhasComErro(Set.of());
        modeloTokens.setRowCount(0);
        modeloErros.setRowCount(0);
        detalheErro.setText("Clique num erro para ver a mensagem completa e ir até a linha no editor.");
        abas.setTitleAt(ABA_TOKENS, "Tokens");
        abas.setTitleAt(ABA_ERROS, "Erros");
        arvore.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Compile o código (F5) para ver a árvore")));
        passosPanico.setText("Compile o código (F5). Se houver erros, aqui aparece como o modo pânico se recuperou de cada um.");
        mostrarResultadoPendente(mensagem);
    }

    // ------------------------------------------------------------------
    // Estado na tela
    // ------------------------------------------------------------------

    private void codigoAlterado() {
        if (!modificado) {
            modificado = true;
            atualizarTitulo();
        }
        if (!errosAtuais.isEmpty() || resultado.getBackground().equals(VERDE)) {
            mostrarResultadoPendente("Código alterado: compile de novo (F5)");
        }
        SwingUtilities.invokeLater(this::atualizarStatus);
    }

    private void atualizarStatus() {
        int linha = 1;
        int coluna = 1;
        try {
            int pos = editor.getCaretPosition();
            linha = editor.getLineOfOffset(pos) + 1;
            coluna = pos - editor.getLineStartOffset(linha - 1) + 1;
        } catch (BadLocationException e) {
            // mantém 1:1
        }
        status.setText("Linhas de código: " + contarLinhas() + "     Linha " + linha + ", coluna " + coluna);
    }

    /** Linhas do código; a quebra de linha no fim do arquivo não conta como linha nova. */
    private int contarLinhas() {
        String texto = editor.getText();
        if (texto.isEmpty()) {
            return 0;
        }
        int linhas = editor.getLineCount();
        return texto.endsWith("\n") ? linhas - 1 : linhas;
    }

    private void atualizarTitulo() {
        setTitle("Compilaço — " + arquivo.getNome() + (modificado ? " *" : ""));
    }

    private void mostrarResultado(String texto, Color cor) {
        resultado.setText(texto);
        resultado.setForeground(Color.WHITE);
        resultado.setBackground(cor);
    }

    private void mostrarResultadoPendente(String texto) {
        resultado.setText(texto);
        resultado.setForeground(Color.WHITE);
        resultado.setBackground(CINZA);
    }

    /** Leva o cursor do editor até a posição de um erro. */
    private void irPara(int linha, int coluna) {
        if (linha <= 0) {
            return;
        }
        try {
            int l = Math.min(linha - 1, editor.getLineCount() - 1);
            int inicio = editor.getLineStartOffset(l);
            int fim = editor.getLineEndOffset(l);
            editor.setCaretPosition(Math.min(inicio + Math.max(0, coluna - 1), Math.max(inicio, fim - 1)));
            editor.requestFocusInWindow();
        } catch (BadLocationException e) {
            // ignora
        }
    }

    private void mostrarFalha(String titulo, Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), titulo, JOptionPane.ERROR_MESSAGE);
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private static String nomeDoTipo(TipoErro tipo) {
        return switch (tipo) {
            case LEXICO -> "Léxico";
            case SINTATICO -> "Sintático";
            case ENTRADA -> "Entrada";
            case INTERNO -> "Interno";
        };
    }

    private static Map<Integer, String> nomesDosTokens() {
        Map<Integer, String> nomes = new HashMap<>();
        for (Field campo : CompilacoConstants.class.getFields()) {
            if (campo.getType() == int.class && Modifier.isStatic(campo.getModifiers())
                    && !campo.getName().equals("DEFAULT")) {
                try {
                    nomes.putIfAbsent(campo.getInt(null), campo.getName());
                } catch (IllegalAccessException e) {
                    // ignora
                }
            }
        }
        return nomes;
    }

    private static DefaultTableModel criarModelo(String... colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int coluna) {
                return getRowCount() > 0 ? getValueAt(0, coluna).getClass() : Object.class;
            }
        };
    }

    private static void configurarTabela(JTable tabela, int... larguras) {
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setFillsViewportHeight(true);
        tabela.setRowHeight(22);
        tabela.getTableHeader().setReorderingAllowed(false);
        for (int i = 0; i < larguras.length; i++) {
            tabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
        }
    }

    private static Font fonteMonoespacada(int tamanho) {
        List<String> disponiveis = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String nome : new String[] { "Consolas", "JetBrains Mono", "DejaVu Sans Mono", "Courier New" }) {
            if (disponiveis.contains(nome)) {
                return new Font(nome, Font.PLAIN, tamanho);
            }
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, tamanho);
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private interface Executar {
        void executar(ActionEvent e);
    }

    /** Ação com atalho Ctrl+tecla (+ modificadores extras). */
    private static Action acao(String nome, int tecla, int extras, Executar executar) {
        return acaoSemCtrl(nome, KeyStroke.getKeyStroke(tecla, InputEvent.CTRL_DOWN_MASK | extras), executar);
    }

    private static Action acaoSemCtrl(String nome, KeyStroke atalho, Executar executar) {
        Action a = new AbstractAction(nome) {
            @Override
            public void actionPerformed(ActionEvent e) {
                executar.executar(e);
            }
        };
        a.putValue(Action.ACCELERATOR_KEY, atalho);
        return a;
    }

    /** Árvore: nós de erro em vermelho, tokens (folhas) em azul. */
    private static class RenderizadorArvore extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree arvore, Object valor, boolean selecionado,
                boolean expandido, boolean folha, int linha, boolean foco) {
            super.getTreeCellRendererComponent(arvore, valor, selecionado, expandido, folha, linha, foco);
            Object conteudo = ((DefaultMutableTreeNode) valor).getUserObject();
            if (conteudo instanceof No no) {
                if (no.isErro()) {
                    setForeground(VERMELHO);
                    setFont(arvore.getFont().deriveFont(Font.BOLD));
                } else if (no.getToken() != null) {
                    setForeground(selecionado ? getTextSelectionColor() : new Color(0x1A4F9C));
                    setFont(arvore.getFont());
                } else {
                    setFont(arvore.getFont());
                }
            }
            return this;
        }
    }

    // ------------------------------------------------------------------
    // Início
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // segue com a aparência padrão do Java
        }
        SwingUtilities.invokeLater(() -> {
            InterfaceGrafica janela = new InterfaceGrafica();
            if (args.length > 0) {
                janela.abrir(Path.of(args[0]));
            }
            janela.setVisible(true);
        });
    }
}
