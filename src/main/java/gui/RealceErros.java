package gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

import parser.ErroCompilacao;

/**
 * Realce visual dos erros no editor (ponto extra do trabalho):
 *   - a linha inteira com erro ganha fundo vermelho claro;
 *   - o trecho onde o erro foi encontrado ganha um sublinhado ondulado vermelho.
 *
 * Os realces acompanham o texto: se o usuário digitar antes deles, eles se
 * deslocam junto, até a próxima compilação.
 */
public class RealceErros {

    private static final Color FUNDO_LINHA = new Color(0xFDE2E2);
    private static final Color ONDULADO = new Color(0xD0021B);

    private final JTextArea editor;
    private final List<Object> marcas = new ArrayList<>();
    private final Highlighter.HighlightPainter pintorLinha = new PintorLinha();
    private final Highlighter.HighlightPainter pintorOndulado = new PintorOndulado();

    public RealceErros(JTextArea editor) {
        this.editor = editor;
    }

    /** Remove todos os realces. */
    public void limpar() {
        Highlighter h = editor.getHighlighter();
        for (Object marca : marcas) {
            h.removeHighlight(marca);
        }
        marcas.clear();
        editor.repaint();
    }

    /** Realça as linhas e os trechos de todos os erros da lista. */
    public void marcar(List<ErroCompilacao> erros) {
        limpar();
        Highlighter h = editor.getHighlighter();
        String texto = editor.getText();

        // primeiro o fundo das linhas (uma vez por linha), depois os sublinhados,
        // para que o fundo de um erro não cubra o sublinhado de outro
        Set<Integer> linhasMarcadas = new HashSet<>();
        List<int[]> trechos = new ArrayList<>();

        for (ErroCompilacao erro : erros) {
            if (erro.linha() <= 0) {
                continue; // erro sem posição (ex.: arquivo vazio)
            }
            try {
                int linha = Math.min(erro.linha() - 1, editor.getLineCount() - 1);
                int inicioLinha = editor.getLineStartOffset(linha);
                int fimLinha = editor.getLineEndOffset(linha);

                if (linhasMarcadas.add(linha)) {
                    marcas.add(h.addHighlight(inicioLinha, Math.max(fimLinha, inicioLinha + 1), pintorLinha));
                }

                // trecho do erro: da coluna informada até o próximo espaço
                int fimTexto = fimLinha;
                while (fimTexto > inicioLinha && Character.isWhitespace(texto.charAt(fimTexto - 1))) {
                    fimTexto--;
                }
                int inicio = Math.min(inicioLinha + Math.max(0, erro.coluna() - 1), fimTexto);
                int fim = inicio;
                while (fim < fimTexto && !Character.isWhitespace(texto.charAt(fim))) {
                    fim++;
                }
                if (fim > inicio) {
                    trechos.add(new int[] { inicio, fim });
                }
            } catch (BadLocationException | StringIndexOutOfBoundsException e) {
                // posição fora do texto atual: só não realça esse erro
            }
        }

        for (int[] trecho : trechos) {
            try {
                marcas.add(h.addHighlight(trecho[0], trecho[1], pintorOndulado));
            } catch (BadLocationException e) {
                // ignora
            }
        }
        editor.repaint();
    }

    /** Pinta o fundo da linha inteira, de borda a borda do editor. */
    private static class PintorLinha implements Highlighter.HighlightPainter {
        @Override
        public void paint(Graphics g, int p0, int p1, Shape limites, JTextComponent c) {
            try {
                Rectangle r = c.modelToView2D(p0).getBounds();
                g.setColor(FUNDO_LINHA);
                g.fillRect(0, r.y, c.getWidth(), r.height);
            } catch (BadLocationException e) {
                // ignora
            }
        }
    }

    /** Desenha um sublinhado ondulado sob o trecho do erro. */
    private static class PintorOndulado implements Highlighter.HighlightPainter {
        @Override
        public void paint(Graphics g, int p0, int p1, Shape limites, JTextComponent c) {
            try {
                Rectangle inicio = c.modelToView2D(p0).getBounds();
                Rectangle fim = c.modelToView2D(p1).getBounds();
                int y = inicio.y + inicio.height - 3;
                g.setColor(ONDULADO);
                for (int x = inicio.x; x < fim.x; x += 4) {
                    g.drawLine(x, y, x + 2, y + 2);
                    g.drawLine(x + 2, y + 2, x + 4, y);
                }
            } catch (BadLocationException e) {
                // ignora
            }
        }
    }
}
