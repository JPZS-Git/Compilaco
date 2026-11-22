package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

/**
 * Identidade visual do Compilaço: cores (tema "gramado"), fontes e a
 * aparência geral dos componentes Swing.
 */
public final class Tema {

    // cores da identidade
    public static final Color GRAMADO = new Color(0x1B5E20);       // verde escuro (cabeçalho)
    public static final Color GRAMADO_CLARO = new Color(0x2E7D32);
    public static final Color DESTAQUE = new Color(0xC6F432);       // verde-limão (botão principal)
    public static final Color FUNDO = new Color(0xF4F6F3);
    public static final Color PAINEL = Color.WHITE;
    public static final Color BORDA = new Color(0xDDE3DA);
    public static final Color TEXTO = new Color(0x1F2A1F);
    public static final Color TEXTO_SUAVE = new Color(0x6B766B);

    // estados
    public static final Color SUCESSO = new Color(0x2E7D32);
    public static final Color ERRO = new Color(0xC62828);
    public static final Color AVISO = new Color(0xE08E0B);
    public static final Color NEUTRO = new Color(0x78867A);

    // editor
    public static final Color EDITOR_FUNDO = new Color(0xFCFDFB);
    public static final Color LINHA_ATUAL = new Color(0xEEF3EA);
    public static final Color SELECAO = new Color(0xC8E6C9);

    // cores do código (syntax highlighting)
    public static final Color COR_PALAVRA = new Color(0x1565C0);    // palavras reservadas
    public static final Color COR_TIPO = new Color(0x7B1FA2);       // placar, estatistica, narracao
    public static final Color COR_NUMERO = new Color(0xD84315);
    public static final Color COR_STRING = new Color(0x2E7D32);
    public static final Color COR_OPERADOR = new Color(0x546E7A);
    public static final Color COR_PROGRAMA = new Color(0x00695C);   // apito_inicial, fim_de_jogo

    private static Font monoespacada;

    private Tema() {
    }

    /** Aplica a aparência (Nimbus com as cores do Compilaço). */
    public static void instalar() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.put("control", new ColorUIResource(FUNDO));
                    UIManager.put("nimbusBase", new ColorUIResource(new Color(0x3F5E45)));
                    UIManager.put("nimbusBlueGrey", new ColorUIResource(new Color(0xB9C6B7)));
                    UIManager.put("nimbusFocus", new ColorUIResource(new Color(0x7CB342)));
                    UIManager.put("nimbusSelectionBackground", new ColorUIResource(GRAMADO_CLARO));
                    UIManager.put("nimbusSelectedText", new ColorUIResource(Color.WHITE));
                    UIManager.put("text", new ColorUIResource(TEXTO));
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // segue com a aparência padrão
        }
        UIManager.put("ToolTip.background", new ColorUIResource(new Color(0x263238)));
        UIManager.put("ToolTip.foreground", new ColorUIResource(Color.WHITE));
        UIManager.put("Table.alternateRowColor", new ColorUIResource(new Color(0xF6F8F5)));
        UIManager.put("defaultFont", new FontUIResource(fonteInterface(Font.PLAIN, 13)));
    }

    /** Fonte da interface (Segoe UI no Windows). */
    public static Font fonteInterface(int estilo, float tamanho) {
        String nome = primeiraDisponivel("Segoe UI", "Inter", "Noto Sans", "DejaVu Sans", "SansSerif");
        return new Font(nome, estilo, Math.round(tamanho));
    }

    /** Fonte do código (Consolas no Windows). */
    public static Font fonteCodigo(float tamanho) {
        if (monoespacada == null) {
            monoespacada = new Font(primeiraDisponivel("Consolas", "JetBrains Mono", "Cascadia Mono",
                    "DejaVu Sans Mono", "Courier New", Font.MONOSPACED), Font.PLAIN, 15);
        }
        return monoespacada.deriveFont(tamanho);
    }

    private static String primeiraDisponivel(String... nomes) {
        List<String> disponiveis = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String nome : nomes) {
            if (disponiveis.contains(nome)) {
                return nome;
            }
        }
        return nomes[nomes.length - 1];
    }
}
