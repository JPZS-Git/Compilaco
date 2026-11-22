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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

import gerador.CompiladorC;
import gerador.GeradorC;
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
 * realçadas no próprio editor. O botão "Gerar executável" traduz o programa
 * para C e chama o gcc.
 *
 * A interface não faz análise nenhuma: ela só chama o compilador
 * (Compilaco.criar(...).analisar()) e mostra o resultado.
 *
 * Execução: java -cp out gui.InterfaceGrafica [arquivo]
 */
public class InterfaceGrafica extends JFrame {

    private static final String MODELO = "apito_inicial()\n\n\n\nfim_de_jogo\n";

    private static final Color VERDE = Tema.SUCESSO;
    private static final Color VERMELHO = Tema.ERRO;
    private static final Color LARANJA = Tema.AVISO;

    private static final int ABA_TOKENS = 0;
    private static final int ABA_ERROS = 1;
    private static final int ABA_ARVORE = 2;
    private static final int ABA_PANICO = 3;
    private static final int ABA_C = 4;

    private final ArquivoFonte arquivo = new ArquivoFonte();

    private List<ErroCompilacao> errosAtuais = List.of();
    private No ultimaArvore;
    private boolean modificado = false;

    private final JTextArea editor = new JTextArea() {
        /** Editor com cores no código e destaque da linha atual. */
        @Override
        public void updateUI() {
            setUI(new EditorColorido());
        }

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
    private final JTextArea codigoC = new JTextArea();
    private final JTextArea saidaC = new JTextArea();
    private final JTabbedPane abas = new JTabbedPane();

    private final JLabel status = new JLabel();
    private final Componentes.Pilula resultado = new Componentes.Pilula();
    private final Componentes.Pilula pilulaTokens = new Componentes.Pilula();
    private final Componentes.Pilula pilulaErros = new Componentes.Pilula();

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
        divisao.setDividerSize(6);

        JPanel conteudo = new JPanel(new BorderLayout());
        conteudo.setBackground(Tema.FUNDO);
        conteudo.add(cabecalho(), BorderLayout.NORTH);
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
        editor.setFont(Tema.fonteCodigo(15));
        editor.setBackground(Tema.EDITOR_FUNDO);
        editor.setForeground(Tema.TEXTO);
        editor.setSelectionColor(Tema.SELECAO);
        editor.setSelectedTextColor(Tema.TEXTO);
        editor.setCaretColor(Tema.GRAMADO);
        editor.setTabSize(4);
        editor.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
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
        Componentes.abasPlanas(abas);
        configurarTabela(tabelaTokens, 45, 160, 160, 55, 60);
        configurarTabela(tabelaErros, 95, 50, 60, 600);
        tabelaErros.getColumnModel().getColumn(0).setCellRenderer(new RenderizadorTipoErro());
        fixarLargura(tabelaTokens, 0, 50);
        fixarLargura(tabelaTokens, 3, 60);
        fixarLargura(tabelaTokens, 4, 65);
        fixarLargura(tabelaErros, 0, 115);
        fixarLargura(tabelaErros, 1, 60);
        fixarLargura(tabelaErros, 2, 65);
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
        detalheErro.setBackground(new Color(0xFFF5F5));
        detalheErro.setForeground(Tema.TEXTO);
        detalheErro.setFont(Tema.fonteInterface(Font.PLAIN, 13));
        detalheErro.setText("Clique num erro para ver a mensagem completa e ir até a linha no editor.");

        arvore.setCellRenderer(new RenderizadorArvore());
        arvore.setFont(Tema.fonteCodigo(13));
        arvore.setRowHeight(22);
        arvore.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        passosPanico.setEditable(false);
        passosPanico.setLineWrap(true);
        passosPanico.setWrapStyleWord(true);
        passosPanico.setFont(Tema.fonteCodigo(13));
        passosPanico.setMargin(new java.awt.Insets(6, 8, 6, 8));
        passosPanico.setText("Compile o código (F5). Se houver erros, aqui aparece como o modo pânico se recuperou de cada um.");

        abas.addTab("Tokens", new JScrollPane(tabelaTokens));
        JPanel painelErros = new JPanel(new BorderLayout());
        painelErros.add(new JScrollPane(tabelaErros), BorderLayout.CENTER);
        painelErros.add(new JScrollPane(detalheErro), BorderLayout.SOUTH);
        abas.addTab("Erros", painelErros);
        abas.addTab("Árvore sintática", new JScrollPane(arvore));
        abas.addTab("Modo pânico", new JScrollPane(passosPanico));

        codigoC.setEditable(false);
        codigoC.setUI(new EditorColorido());
        codigoC.setFont(Tema.fonteCodigo(13));
        codigoC.setBackground(Tema.EDITOR_FUNDO);
        codigoC.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        saidaC.setEditable(false);
        saidaC.setLineWrap(true);
        saidaC.setFont(Tema.fonteCodigo(13));
        saidaC.setMargin(new java.awt.Insets(6, 8, 6, 8));
        saidaC.setBackground(new Color(0x1E1E1E));
        saidaC.setForeground(new Color(0xE6E6E6));
        saidaC.setCaretColor(Color.WHITE);
        limparCodigoC();

        JPanel painelC = new JPanel(new BorderLayout());
        JLabel tituloSaida = new JLabel("Compilação com gcc e execução do programa");
        tituloSaida.setIcon(Icones.de(Icones.Tipo.EXECUTAVEL, 14, Tema.TEXTO_SUAVE));
        tituloSaida.setForeground(Tema.TEXTO_SUAVE);
        tituloSaida.setFont(Tema.fonteInterface(Font.BOLD, 12));
        tituloSaida.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        JPanel painelSaida = new JPanel(new BorderLayout());
        painelSaida.add(tituloSaida, BorderLayout.NORTH);
        painelSaida.add(new JScrollPane(saidaC), BorderLayout.CENTER);
        JSplitPane divisaoC = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(codigoC), painelSaida);
        divisaoC.setResizeWeight(0.62);
        divisaoC.setBorder(null);
        painelC.add(divisaoC, BorderLayout.CENTER);
        abas.addTab("Código C", painelC);
    }

    /** Cabeçalho verde com o nome do compilador e os botões. */
    private JPanel cabecalho() {
        JLabel titulo = new JLabel("Compilaço");
        titulo.setIcon(Icones.de(Icones.Tipo.BOLA, 30, Color.WHITE));
        titulo.setIconTextGap(10);
        titulo.setForeground(Color.WHITE);
        titulo.setFont(Tema.fonteInterface(Font.BOLD, 22));

        JLabel subtitulo = new JLabel("compilador da linguagem do futebol");
        subtitulo.setForeground(new Color(0xB9DDB2));
        subtitulo.setFont(Tema.fonteInterface(Font.PLAIN, 12));
        subtitulo.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 0));

        JPanel marca = new JPanel();
        marca.setOpaque(false);
        marca.setLayout(new javax.swing.BoxLayout(marca, javax.swing.BoxLayout.Y_AXIS));
        marca.add(titulo);
        marca.add(subtitulo);

        JPanel botoes = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 4, 0));
        botoes.setOpaque(false);
        botoes.add(Componentes.botaoCabecalho(acaoNovo, "Novo", "Novo código (Ctrl+N)", Icones.Tipo.NOVO));
        botoes.add(Componentes.botaoCabecalho(acaoAbrir, "Abrir", "Abrir arquivo (Ctrl+O)", Icones.Tipo.ABRIR));
        botoes.add(Componentes.botaoCabecalho(acaoSalvar, "Salvar", "Salvar (Ctrl+S)", Icones.Tipo.SALVAR));
        botoes.add(Componentes.botaoCabecalho(acaoSalvarComo, "Salvar como", "Salvar como (Ctrl+Shift+S)", Icones.Tipo.SALVAR_COMO));
        botoes.add(javax.swing.Box.createHorizontalStrut(14));
        botoes.add(Componentes.botaoCabecalho(acaoCompilar, "Compilar", "Compilar o código (F5)", Icones.Tipo.COMPILAR, Tema.DESTAQUE));
        botoes.add(javax.swing.Box.createHorizontalStrut(8));
        botoes.add(Componentes.botaoCabecalho(acaoExecutavel, "Gerar executável", "Traduzir para C e gerar o executável com gcc (F6)", Icones.Tipo.EXECUTAVEL, Color.WHITE));

        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setBackground(Tema.GRAMADO);
        cabecalho.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 12));
        cabecalho.add(marca, BorderLayout.WEST);
        JPanel centralizar = new JPanel(new java.awt.GridBagLayout());
        centralizar.setOpaque(false);
        centralizar.add(botoes);
        cabecalho.add(centralizar, BorderLayout.EAST);
        return cabecalho;
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
        compilarMenu.add(acaoExecutavel);

        JMenuBar barra = new JMenuBar();
        barra.add(arquivoMenu);
        barra.add(compilarMenu);
        return barra;
    }

    /** Barra de baixo: linhas e cursor à esquerda; placar (tokens, erros, resultado) à direita. */
    private JPanel barraDeStatus() {
        status.setForeground(Tema.TEXTO_SUAVE);
        status.setFont(Tema.fonteInterface(Font.PLAIN, 12));
        status.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        pilulaTokens.contornar("0 tokens", Tema.NEUTRO);
        pilulaErros.contornar("0 erros", Tema.NEUTRO);

        JPanel placar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
        placar.setOpaque(false);
        placar.add(pilulaTokens);
        placar.add(pilulaErros);
        placar.add(resultado);

        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Tema.PAINEL);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDA),
                BorderFactory.createEmptyBorder(6, 0, 6, 10)));
        barra.add(status, BorderLayout.WEST);
        barra.add(placar, BorderLayout.EAST);
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
    private final Action acaoExecutavel = acaoSemCtrl("Gerar executável (F6)", KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), e -> gerarExecutavel());
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

    /** Compila o código do editor e mostra o resultado. Devolve true se não houve erros. */
    private boolean compilar() {
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
        ultimaArvore = raiz;

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
            mostrarResultado("GOL! Código aceito", VERDE);
            abas.setSelectedIndex(ABA_ARVORE);
        } else {
            mostrarResultado("FALTA GRAVE! Código não aceito", VERMELHO);
            abas.setSelectedIndex(ABA_ERROS);
        }
        return erros.isEmpty();
    }

    // ------------------------------------------------------------------
    // Geração de código: C + gcc (ponto extra)
    // ------------------------------------------------------------------

    /**
     * Compila o código, traduz a árvore para C, salva o arquivo .c, chama o
     * gcc e roda o executável, mostrando tudo na aba "Código C".
     */
    private void gerarExecutavel() {
        if (!compilar()) {
            JOptionPane.showMessageDialog(this,
                    "O código tem erros. Corrija-os antes de gerar o executável.",
                    "Gerar executável", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String nomeFonte = arquivo.getNome();
        String c;
        try {
            c = GeradorC.gerar(ultimaArvore, nomeFonte);
        } catch (RuntimeException e) {
            mostrarFalha("Não foi possível gerar o código C", e);
            return;
        }
        codigoC.setText(c);
        codigoC.setCaretPosition(0);
        abas.setSelectedIndex(ABA_C);

        Path fonteC;
        try {
            fonteC = arquivoC();
            Files.writeString(fonteC, c, StandardCharsets.UTF_8);
        } catch (IOException e) {
            mostrarFalha("Não foi possível salvar o arquivo C", e);
            return;
        }

        Path executavel = CompiladorC.executavelDe(fonteC);
        saidaC.setText("Código C salvo em:\n  " + fonteC.toAbsolutePath() + "\n\n"
                + "$ gcc " + fonteC.getFileName() + " -o " + executavel.getFileName() + "\n");
        acaoExecutavel.setEnabled(false);
        mostrarResultadoPendente("Compilando com gcc...");

        new SwingWorker<String, Void>() {
            private boolean gerou;

            @Override
            protected String doInBackground() {
                StringBuilder relatorio = new StringBuilder();
                CompiladorC.Resultado gcc;
                try {
                    gcc = CompiladorC.compilar(fonteC, executavel);
                } catch (IOException e) {
                    return "\n[gcc não encontrado]\n"
                            + "Para gerar o executável, instale o gcc (no Windows: MinGW-w64)\n"
                            + "e coloque-o no PATH. O código C já foi salvo e pode ser\n"
                            + "compilado depois com o comando acima.\n";
                }
                relatorio.append(gcc.saida());
                if (!gcc.sucesso()) {
                    relatorio.append("\n[gcc] A compilação do código C falhou.\n")
                            .append("Erros do gcc costumam indicar problemas que o Compilaço ainda não\n")
                            .append("verifica, por não ter análise semântica (ex.: variável não declarada).\n");
                    return relatorio.toString();
                }
                gerou = true;
                relatorio.append("[gcc] Executável gerado: ").append(executavel.toAbsolutePath()).append("\n\n");
                relatorio.append("$ ").append(executavel.getFileName()).append('\n');
                try {
                    CompiladorC.Resultado execucao = CompiladorC.executar(executavel);
                    relatorio.append(execucao.saida());
                    if (execucao.tempoEsgotado()) {
                        relatorio.append("\n[programa interrompido: passou de 10 segundos (laço infinito?)]\n");
                    } else {
                        relatorio.append("\n[programa encerrado com código ").append(execucao.codigoSaida()).append("]\n");
                    }
                } catch (IOException e) {
                    relatorio.append("[não foi possível executar o programa: ").append(e.getMessage()).append("]\n");
                }
                return relatorio.toString();
            }

            @Override
            protected void done() {
                try {
                    saidaC.append(get());
                } catch (Exception e) {
                    saidaC.append("\n[erro ao gerar o executável: " + e.getMessage() + "]\n");
                }
                acaoExecutavel.setEnabled(true);
                if (gerou) {
                    mostrarResultado("GOL! Código aceito e executável gerado", VERDE);
                } else {
                    mostrarResultado("GOL, mas o executável não foi gerado (veja a aba Código C)", LARANJA);
                }
            }
        }.execute();
    }

    /**
     * Onde salvar o .c: numa pasta "saida" ao lado do código-fonte
     * (ou na pasta temporária, se o código ainda não foi salvo).
     */
    private Path arquivoC() throws IOException {
        Path fonte = arquivo.getCaminho();
        Path pasta = (fonte != null && fonte.toAbsolutePath().getParent() != null)
                ? fonte.toAbsolutePath().getParent().resolve("saida")
                : Path.of(System.getProperty("java.io.tmpdir"), "compilaco");
        Files.createDirectories(pasta);
        String nome = fonte != null ? fonte.getFileName().toString().replaceFirst("\\.[^.]*$", "") : "programa";
        return pasta.resolve(nome + ".c");
    }

    private void limparCodigoC() {
        codigoC.setText("Clique em \"Gerar executável (F6)\" para traduzir o programa para C.\n\n"
                + "O código C aparece aqui; embaixo, a compilação com gcc e a saída do programa.\n"
                + "Como o Compilaço não tem comando de saída, o programa gerado imprime\n"
                + "no final o valor das variáveis declaradas.");
        saidaC.setText("");
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
        pilulaTokens.contornar(n + (n == 1 ? " token" : " tokens"), Tema.NEUTRO);
    }

    private void mostrarErros(List<ErroCompilacao> erros) {
        modeloErros.setRowCount(0);
        for (ErroCompilacao erro : erros) {
            modeloErros.addRow(new Object[] {
                    nomeDoTipo(erro.tipo()), erro.linha(), erro.coluna(), erro.mensagem()
            });
        }
        abas.setTitleAt(ABA_ERROS, "Erros (" + erros.size() + ")");
        if (erros.isEmpty()) {
            pilulaErros.contornar("0 erros", Tema.SUCESSO);
        } else {
            pilulaErros.preencher(erros.size() + (erros.size() == 1 ? " erro" : " erros"), null, Tema.ERRO);
        }
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
        pilulaTokens.contornar("0 tokens", Tema.NEUTRO);
        pilulaErros.contornar("0 erros", Tema.NEUTRO);
        abas.setTitleAt(ABA_ERROS, "Erros");
        arvore.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Compile o código (F5) para ver a árvore")));
        passosPanico.setText("Compile o código (F5). Se houver erros, aqui aparece como o modo pânico se recuperou de cada um.");
        ultimaArvore = null;
        limparCodigoC();
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
        if (!errosAtuais.isEmpty() || resultado.getCorFundo().equals(VERDE)) {
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
        status.setText("Linhas de código: " + contarLinhas() + "   ·   Linha " + linha + ", coluna " + coluna);
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
        resultado.preencher(texto, cor.equals(VERMELHO) ? null : Icones.de(Icones.Tipo.BOLA, 14, Color.WHITE), cor);
    }

    private void mostrarResultadoPendente(String texto) {
        resultado.preencher(texto, null, Tema.NEUTRO);
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
        tabela.setRowHeight(26);
        tabela.setShowGrid(false);
        tabela.setIntercellSpacing(new Dimension(0, 0));
        tabela.setFont(Tema.fonteInterface(Font.PLAIN, 13));
        tabela.getTableHeader().setFont(Tema.fonteInterface(Font.BOLD, 12));
        tabela.getTableHeader().setReorderingAllowed(false);
        for (int i = 0; i < larguras.length; i++) {
            tabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
        }
    }

    /** Coluna de largura fixa (o espaço que sobra fica para as colunas de texto). */
    private static void fixarLargura(JTable tabela, int coluna, int largura) {
        javax.swing.table.TableColumn c = tabela.getColumnModel().getColumn(coluna);
        c.setMinWidth(largura);
        c.setMaxWidth(largura);
        c.setPreferredWidth(largura);
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

    /**
     * Árvore: cada tipo de nó com seu ícone — regras da gramática (cinza),
     * tokens (azul) e erros (vermelho).
     */
    private static class RenderizadorArvore extends DefaultTreeCellRenderer {
        private static final javax.swing.Icon REGRA = Icones.de(Icones.Tipo.REGRA, 16, Tema.TEXTO_SUAVE);
        private static final javax.swing.Icon TOKEN = Icones.de(Icones.Tipo.TOKEN, 16, Tema.COR_PALAVRA);
        private static final javax.swing.Icon ERRO = Icones.de(Icones.Tipo.ERRO, 16, Tema.ERRO);

        @Override
        public Component getTreeCellRendererComponent(JTree arvore, Object valor, boolean selecionado,
                boolean expandido, boolean folha, int linha, boolean foco) {
            super.getTreeCellRendererComponent(arvore, valor, selecionado, expandido, folha, linha, foco);
            setIconTextGap(6);
            Object conteudo = ((DefaultMutableTreeNode) valor).getUserObject();
            if (conteudo instanceof No no) {
                if (no.isErro()) {
                    setIcon(ERRO);
                    setForeground(selecionado ? Color.WHITE : VERMELHO);
                    setFont(arvore.getFont().deriveFont(Font.BOLD));
                } else if (no.getToken() != null) {
                    setIcon(TOKEN);
                    setForeground(selecionado ? Color.WHITE : Tema.COR_PALAVRA);
                    setFont(arvore.getFont());
                } else {
                    setIcon(REGRA);
                    setForeground(selecionado ? Color.WHITE : Tema.TEXTO);
                    setFont(arvore.getFont());
                }
            } else {
                setIcon(null);
                setForeground(Tema.TEXTO_SUAVE);
            }
            return this;
        }
    }

    /** Coluna "Tipo" dos erros: léxico em laranja, sintático em vermelho. */
    private static class RenderizadorTipoErro extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabela, Object valor, boolean selecionado,
                boolean foco, int linha, int coluna) {
            super.getTableCellRendererComponent(tabela, valor, selecionado, foco, linha, coluna);
            setFont(tabela.getFont().deriveFont(Font.BOLD));
            if (!selecionado) {
                setForeground("Léxico".equals(valor) ? LARANJA : VERMELHO);
            }
            setIcon(Icones.de(Icones.Tipo.ERRO, 13, selecionado ? Color.WHITE : ("Léxico".equals(valor) ? LARANJA : VERMELHO)));
            return this;
        }
    }

    // ------------------------------------------------------------------
    // Início
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        Tema.instalar();
        SwingUtilities.invokeLater(() -> {
            InterfaceGrafica janela = new InterfaceGrafica();
            if (args.length > 0) {
                janela.abrir(Path.of(args[0]));
            }
            janela.setVisible(true);
        });
    }
}
