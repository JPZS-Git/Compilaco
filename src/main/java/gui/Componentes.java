package gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * Componentes visuais próprios do Compilaço: botões do cabeçalho, "pílulas"
 * do placar e abas planas.
 */
public final class Componentes {

    private Componentes() {
    }

    // ------------------------------------------------------------------
    // Botão do cabeçalho verde
    // ------------------------------------------------------------------

    /** Botão transparente do cabeçalho (clareia ao passar o mouse). */
    public static JButton botaoCabecalho(Action acao, String texto, String dica, Icones.Tipo icone) {
        return botaoCabecalho(acao, texto, dica, icone, null);
    }

    /**
     * Botão arredondado do cabeçalho. Com uma cor de preenchimento, vira um
     * botão em destaque (ex.: Compilar em verde-limão, Gerar executável em
     * branco); sem cor, é transparente e clareia ao passar o mouse.
     */
    public static JButton botaoCabecalho(Action acao, String texto, String dica,
                                         Icones.Tipo icone, Color preenchimento) {
        boolean destaque = preenchimento != null;
        JButton botao = new JButton(acao) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean sobre = getModel().isRollover();
                boolean pressionado = getModel().isPressed();
                if (destaque) {
                    Color base = isEnabled() ? preenchimento : misturar(preenchimento, Tema.GRAMADO, 0.45f);
                    g.setColor(pressionado ? misturar(base, Color.BLACK, 0.15f)
                            : sobre ? misturar(base, Color.WHITE, 0.25f) : base);
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                } else if (sobre || pressionado) {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pressionado ? 0.28f : 0.16f));
                    g.setColor(Color.WHITE);
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                g.dispose();
                super.paintComponent(g0);
            }
        };
        botao.setUI(new BasicButtonUI());
        botao.setText(texto);
        botao.setToolTipText(dica);
        Color corTexto = destaque ? Tema.GRAMADO : Color.WHITE;
        botao.setForeground(corTexto);
        botao.setIcon(Icones.de(icone, 16, corTexto));
        botao.setDisabledIcon(Icones.de(icone, 16, new Color(corTexto.getRed(), corTexto.getGreen(), corTexto.getBlue(), 110)));
        botao.setFont(Tema.fonteInterface(destaque ? Font.BOLD : Font.PLAIN, 13));
        botao.setIconTextGap(7);
        botao.setContentAreaFilled(false);
        botao.setBorderPainted(false);
        botao.setFocusPainted(false);
        botao.setOpaque(false);
        botao.setRolloverEnabled(true);
        botao.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 14));
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return botao;
    }

    /** Mistura duas cores (fração da segunda: 0 = só a primeira, 1 = só a segunda). */
    private static Color misturar(Color a, Color b, float fracao) {
        float f = Math.max(0f, Math.min(1f, fracao));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * f),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * f),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * f));
    }

    // ------------------------------------------------------------------
    // Pílula (contadores e resultado do placar)
    // ------------------------------------------------------------------

    /** Etiqueta com fundo arredondado. */
    public static class Pilula extends JLabel {

        private Color fundo = Tema.NEUTRO;
        private boolean contorno;

        public Pilula() {
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(Tema.fonteInterface(Font.BOLD, 12));
            setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        }

        /** Pílula preenchida com a cor dada. */
        public void preencher(String texto, Icon icone, Color cor) {
            setText(texto);
            setIcon(icone);
            fundo = cor;
            contorno = false;
            setForeground(Color.WHITE);
            repaint();
        }

        /** Pílula só com contorno (para contadores). */
        public void contornar(String texto, Color cor) {
            setText(texto);
            setIcon(null);
            fundo = cor;
            contorno = true;
            setForeground(cor);
            repaint();
        }

        public Color getCorFundo() {
            return fundo;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arco = getHeight();
            if (contorno) {
                g.setColor(fundo);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arco, arco);
            } else {
                g.setColor(fundo);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), arco, arco);
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }

    // ------------------------------------------------------------------
    // Abas planas (aba selecionada sublinhada em verde)
    // ------------------------------------------------------------------

    public static class AbasPlanas extends BasicTabbedPaneUI {

        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabInsets = new Insets(8, 14, 8, 14);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            tabAreaInsets = new Insets(2, 6, 0, 6);
            contentBorderInsets = new Insets(1, 0, 0, 0);
        }

        @Override
        protected int calculateTabHeight(int posicao, int indice, int alturaFonte) {
            return alturaFonte + tabInsets.top + tabInsets.bottom + 4;
        }

        @Override
        protected void paintTabBackground(Graphics g, int posicao, int indice, int x, int y, int w, int h,
                                          boolean selecionada) {
            g.setColor(selecionada ? Tema.PAINEL : Tema.FUNDO);
            g.fillRect(x, y, w, h);
        }

        @Override
        protected void paintTabBorder(Graphics g, int posicao, int indice, int x, int y, int w, int h,
                                      boolean selecionada) {
            if (selecionada) {
                g.setColor(Tema.GRAMADO_CLARO);
                g.fillRect(x + 4, y + h - 3, w - 8, 3);
            }
        }

        @Override
        protected void paintContentBorder(Graphics g, int posicao, int selecionada) {
            Rectangle area = tabPane.getBounds();
            int y = calculateTabAreaHeight(posicao, runCount, maxTabHeight);
            g.setColor(Tema.BORDA);
            g.fillRect(0, y, area.width, 1);
        }

        @Override
        protected void paintFocusIndicator(Graphics g, int posicao, Rectangle[] retangulos, int indice,
                                           Rectangle icone, Rectangle texto, boolean selecionada) {
            // sem retângulo de foco
        }

        @Override
        protected void paintText(Graphics g, int posicao, Font fonte, FontMetrics fm, int indice,
                                 String titulo, Rectangle texto, boolean selecionada) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(fonte);
            g.setColor(selecionada ? Tema.GRAMADO : Tema.TEXTO_SUAVE);
            g.drawString(titulo, texto.x, texto.y + fm.getAscent());
        }
    }

    /** Aplica as abas planas a um JTabbedPane. */
    public static void abasPlanas(JTabbedPane abas) {
        abas.setUI(new AbasPlanas());
        abas.setFont(Tema.fonteInterface(Font.PLAIN, 13));
        abas.setBackground(Tema.FUNDO);
    }

    /** Tamanho fixo de altura para um componente. */
    public static void altura(JComponent c, int altura) {
        Dimension d = c.getPreferredSize();
        c.setPreferredSize(new Dimension(d.width, altura));
    }
}
