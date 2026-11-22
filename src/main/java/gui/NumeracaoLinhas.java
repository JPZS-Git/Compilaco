package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

/**
 * Coluna com a numeração das linhas, exibida ao lado do editor.
 *
 * As linhas que têm erro aparecem com fundo vermelho claro e número em
 * vermelho, junto com o realce da própria linha no editor.
 */
public class NumeracaoLinhas extends JComponent implements DocumentListener {

    private static final Color FUNDO = new Color(0xF3F3F3);
    private static final Color NUMERO = new Color(0x8A8A8A);
    private static final Color FUNDO_ERRO = new Color(0xF8C9C9);
    private static final Color NUMERO_ERRO = new Color(0xB00020);
    private static final Color BORDA = new Color(0xDDDDDD);

    private final JTextArea editor;
    private Set<Integer> linhasComErro = Set.of();

    public NumeracaoLinhas(JTextArea editor) {
        this.editor = editor;
        setFont(editor.getFont());
        editor.getDocument().addDocumentListener(this);
    }

    /** Define as linhas (contadas a partir de 1) que devem aparecer marcadas. */
    public void setLinhasComErro(Set<Integer> linhas) {
        this.linhasComErro = Set.copyOf(linhas);
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int digitos = Math.max(3, String.valueOf(editor.getLineCount()).length());
        int largura = fm.charWidth('0') * digitos + 18;
        return new Dimension(largura, editor.getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Rectangle area = g.getClipBounds();
        g2.setColor(FUNDO);
        g2.fillRect(area.x, area.y, area.width, area.height);
        g2.setColor(BORDA);
        g2.drawLine(getWidth() - 1, area.y, getWidth() - 1, area.y + area.height);

        FontMetrics fm = g2.getFontMetrics(getFont());
        Font normal = getFont();
        Font negrito = normal.deriveFont(Font.BOLD);

        try {
            int primeira = editor.getLineOfOffset(editor.viewToModel2D(new Point(0, area.y)));
            int ultima = editor.getLineOfOffset(editor.viewToModel2D(new Point(0, area.y + area.height)));

            for (int linha = primeira; linha <= ultima; linha++) {
                Rectangle2D r = editor.modelToView2D(editor.getLineStartOffset(linha));
                if (r == null) {
                    continue;
                }
                int y = (int) r.getY();
                int altura = (int) r.getHeight();
                boolean erro = linhasComErro.contains(linha + 1);

                if (erro) {
                    g2.setColor(FUNDO_ERRO);
                    g2.fillRect(0, y, getWidth() - 1, altura);
                }

                String numero = String.valueOf(linha + 1);
                g2.setFont(erro ? negrito : normal);
                g2.setColor(erro ? NUMERO_ERRO : NUMERO);
                int x = getWidth() - 9 - g2.getFontMetrics().stringWidth(numero);
                g2.drawString(numero, x, y + altura - fm.getDescent());
            }
        } catch (BadLocationException e) {
            // documento mudou durante a pintura: a próxima pintura corrige
        }
    }

    private void atualizar() {
        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        atualizar();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        atualizar();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        atualizar();
    }
}
