package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.event.CaretListener;
import javax.swing.plaf.basic.BasicTextAreaUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainView;
import javax.swing.text.Segment;
import javax.swing.text.Utilities;
import javax.swing.text.View;

/**
 * Aparência do editor de código:
 *   - cores no código (syntax highlighting) para palavras reservadas, tipos,
 *     números, narrações (strings) e operadores;
 *   - destaque suave da linha onde está o cursor.
 *
 * As cores são só visuais: a análise de verdade continua sendo feita pelo
 * compilador ao apertar Compilar.
 */
public class EditorColorido extends BasicTextAreaUI {

    private static final Set<String> CONTROLE = Set.of(
            "var_analisa", "segue_jogo", "enquanto_bola_rolar", "campeonato");
    private static final Set<String> TIPOS = Set.of("placar", "estatistica", "narracao");
    private static final Set<String> PROGRAMA = Set.of("apito_inicial", "fim_de_jogo");
    private static final String OPERADORES = "+-*/=<>!;,(){}";

    private static final int NORMAL = 0;
    private static final int CONTROLE_K = 1;
    private static final int TIPO_K = 2;
    private static final int PROGRAMA_K = 3;
    private static final int NUMERO = 4;
    private static final int STRING = 5;
    private static final int OPERADOR = 6;

    private CaretListener repintarLinhaAtual;

    @Override
    public View create(Element elemento) {
        return new VisaoColorida(elemento);
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        repintarLinhaAtual = e -> getComponent().repaint();
        getComponent().addCaretListener(repintarLinhaAtual);
    }

    @Override
    protected void uninstallListeners() {
        getComponent().removeCaretListener(repintarLinhaAtual);
        super.uninstallListeners();
    }

    /** Fundo do editor com a linha do cursor destacada. */
    @Override
    protected void paintBackground(Graphics g) {
        JTextComponent editor = getComponent();
        g.setColor(editor.getBackground());
        g.fillRect(0, 0, editor.getWidth(), editor.getHeight());
        try {
            Rectangle2D r = editor.modelToView2D(editor.getCaretPosition());
            if (r != null) {
                g.setColor(Tema.LINHA_ATUAL);
                g.fillRect(0, (int) r.getY(), editor.getWidth(), (int) r.getHeight());
            }
        } catch (BadLocationException e) {
            // ignora
        }
    }

    /** Classifica cada caractere de uma linha (palavra reservada, número, string...). */
    static int[] classificar(String linha) {
        int n = linha.length();
        int[] classe = new int[n];
        int i = 0;
        while (i < n) {
            char c = linha.charAt(i);
            int j = i + 1;
            int tipo = NORMAL;

            if (c == '"') {
                while (j < n && linha.charAt(j) != '"' && linha.charAt(j) != '\n') {
                    j++;
                }
                if (j < n && linha.charAt(j) == '"') {
                    j++;
                }
                tipo = STRING;
            } else if (Character.isDigit(c)) {
                while (j < n && Character.isDigit(linha.charAt(j))) {
                    j++;
                }
                if (j + 1 < n && linha.charAt(j) == '.' && Character.isDigit(linha.charAt(j + 1))) {
                    j += 2;
                    while (j < n && Character.isDigit(linha.charAt(j))) {
                        j++;
                    }
                }
                tipo = NUMERO;
            } else if (Character.isLetter(c)) {
                while (j < n && (Character.isLetterOrDigit(linha.charAt(j)) || linha.charAt(j) == '_')) {
                    j++;
                }
                String palavra = linha.substring(i, j);
                if (CONTROLE.contains(palavra)) {
                    tipo = CONTROLE_K;
                } else if (TIPOS.contains(palavra)) {
                    tipo = TIPO_K;
                } else if (PROGRAMA.contains(palavra)) {
                    tipo = PROGRAMA_K;
                }
            } else if (OPERADORES.indexOf(c) >= 0) {
                tipo = OPERADOR;
            }

            for (int k = i; k < j; k++) {
                classe[k] = tipo;
            }
            i = j;
        }
        return classe;
    }

    private static Color cor(int classe, Color normal) {
        return switch (classe) {
            case CONTROLE_K -> Tema.COR_PALAVRA;
            case TIPO_K -> Tema.COR_TIPO;
            case PROGRAMA_K -> Tema.COR_PROGRAMA;
            case NUMERO -> Tema.COR_NUMERO;
            case STRING -> Tema.COR_STRING;
            case OPERADOR -> Tema.COR_OPERADOR;
            default -> normal;
        };
    }

    /** Desenha cada linha em trechos coloridos. */
    private static final class VisaoColorida extends PlainView {

        private Font negrito;

        VisaoColorida(Element elemento) {
            super(elemento);
        }

        @Override
        protected float drawUnselectedText(Graphics2D g, float x, float y, int p0, int p1)
                throws BadLocationException {
            return desenhar(g, x, y, p0, p1);
        }

        @Override
        protected float drawSelectedText(Graphics2D g, float x, float y, int p0, int p1)
                throws BadLocationException {
            return desenhar(g, x, y, p0, p1);
        }

        private float desenhar(Graphics2D g, float x, float y, int p0, int p1) throws BadLocationException {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Document doc = getDocument();
            Element raiz = doc.getDefaultRootElement();
            Element linha = raiz.getElement(raiz.getElementIndex(p0));
            int inicio = linha.getStartOffset();
            int fim = Math.min(linha.getEndOffset(), doc.getLength());
            int[] classes = classificar(doc.getText(inicio, fim - inicio));

            JComponent editor = (JComponent) getContainer();
            Font normal = editor.getFont();
            Color corNormal = editor.getForeground();
            Font fonteNegrito = negrito(normal, editor);

            Segment trecho = new Segment();
            int pos = p0;
            while (pos < p1) {
                int classe = classes[pos - inicio];
                int fimTrecho = pos;
                while (fimTrecho < p1 && classes[fimTrecho - inicio] == classe) {
                    fimTrecho++;
                }
                doc.getText(pos, fimTrecho - pos, trecho);
                g.setColor(cor(classe, corNormal));
                boolean emNegrito = classe == CONTROLE_K || classe == TIPO_K || classe == PROGRAMA_K;
                g.setFont(emNegrito ? fonteNegrito : normal);
                x = Utilities.drawTabbedText(trecho, x, y, g, this, pos);
                pos = fimTrecho;
            }
            g.setFont(normal);
            return x;
        }

        /**
         * Negrito só se ocupar a mesma largura que a fonte normal (fontes
         * monoespaçadas como Consolas), para o cursor não ficar desalinhado.
         */
        private Font negrito(Font normal, JComponent editor) {
            if (negrito == null || negrito.getSize2D() != normal.getSize2D()
                    || !negrito.getFamily().equals(normal.getFamily())) {
                Font candidato = normal.deriveFont(Font.BOLD);
                FontMetrics fmNormal = editor.getFontMetrics(normal);
                FontMetrics fmNegrito = editor.getFontMetrics(candidato);
                boolean mesmaLargura = fmNormal.charWidth('m') == fmNegrito.charWidth('m')
                        && fmNormal.charWidth('i') == fmNegrito.charWidth('i');
                negrito = mesmaLargura ? candidato : normal;
            }
            return negrito;
        }
    }
}
