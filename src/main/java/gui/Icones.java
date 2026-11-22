package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/**
 * Ícones desenhados no próprio código (sem arquivos de imagem), para os
 * botões e para a árvore sintática.
 */
public final class Icones {

    public enum Tipo {
        NOVO, ABRIR, SALVAR, SALVAR_COMO, COMPILAR, EXECUTAVEL, BOLA,
        REGRA, TOKEN, ERRO
    }

    private Icones() {
    }

    public static Icon de(Tipo tipo, int tamanho, Color cor) {
        return new Icone(tipo, tamanho, cor);
    }

    /** Linha poligonal pelos pontos (x1, y1, x2, y2, ...). */
    private static Path2D linha(double... pontos) {
        Path2D caminho = new Path2D.Double();
        caminho.moveTo(pontos[0], pontos[1]);
        for (int i = 2; i < pontos.length; i += 2) {
            caminho.lineTo(pontos[i], pontos[i + 1]);
        }
        return caminho;
    }

    private static final class Icone implements Icon {
        private final Tipo tipo;
        private final int tamanho;
        private final Color cor;

        Icone(Tipo tipo, int tamanho, Color cor) {
            this.tipo = tipo;
            this.tamanho = tamanho;
            this.cor = cor;
        }

        @Override
        public int getIconWidth() {
            return tamanho;
        }

        @Override
        public int getIconHeight() {
            return tamanho;
        }

        @Override
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.translate(x, y);
            double s = tamanho / 16.0; // desenhos feitos numa grade de 16x16
            g.scale(s, s);
            g.setColor(cor);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (tipo) {
                case NOVO -> {
                    Path2D folha = new Path2D.Double();
                    folha.moveTo(3.5, 1.5); folha.lineTo(9.5, 1.5); folha.lineTo(12.5, 4.5);
                    folha.lineTo(12.5, 14.5); folha.lineTo(3.5, 14.5); folha.closePath();
                    g.draw(folha);
                    g.draw(linha(9.5, 1.5, 9.5, 4.5, 12.5, 4.5));
                }
                case ABRIR -> {
                    Path2D pasta = new Path2D.Double();
                    pasta.moveTo(1.5, 3.5); pasta.lineTo(6, 3.5); pasta.lineTo(7.5, 5); pasta.lineTo(14.5, 5);
                    pasta.lineTo(14.5, 13.5); pasta.lineTo(1.5, 13.5); pasta.closePath();
                    g.draw(pasta);
                    g.draw(linha(1.5, 7.5, 14.5, 7.5));
                }
                case SALVAR, SALVAR_COMO -> {
                    g.draw(new RoundRectangle2D.Double(2, 2, 12, 12, 2, 2));
                    g.draw(linha(5, 2, 5, 6, 11, 6, 11, 2));
                    g.draw(new RoundRectangle2D.Double(5, 9, 6, 5, 1, 1));
                    if (tipo == Tipo.SALVAR_COMO) {
                        g.fill(new Ellipse2D.Double(11.5, 10.5, 4.5, 4.5));
                    }
                }
                case COMPILAR -> {
                    Path2D play = new Path2D.Double();
                    play.moveTo(4, 2.5); play.lineTo(13.5, 8); play.lineTo(4, 13.5); play.closePath();
                    g.fill(play);
                }
                case EXECUTAVEL -> {
                    g.draw(new RoundRectangle2D.Double(1.5, 2.5, 13, 11, 2, 2));
                    g.draw(linha(4, 6, 6.5, 8, 4, 10));
                    g.draw(linha(8, 10.5, 12, 10.5));
                }
                case BOLA -> {
                    g.setStroke(new BasicStroke(1.3f));
                    g.draw(new Ellipse2D.Double(1, 1, 14, 14));
                    Path2D pentagono = new Path2D.Double();
                    for (int i = 0; i < 5; i++) {
                        double a = Math.toRadians(-90 + i * 72);
                        double px = 8 + 3.2 * Math.cos(a);
                        double py = 8 + 3.2 * Math.sin(a);
                        if (i == 0) {
                            pentagono.moveTo(px, py);
                        } else {
                            pentagono.lineTo(px, py);
                        }
                    }
                    pentagono.closePath();
                    g.fill(pentagono);
                    for (int i = 0; i < 5; i++) {
                        double a = Math.toRadians(-90 + i * 72);
                        g.draw(linha(8 + 3.2 * Math.cos(a), 8 + 3.2 * Math.sin(a),
                                8 + 7 * Math.cos(a), 8 + 7 * Math.sin(a)));
                    }
                }
                case REGRA -> {
                    g.draw(new RoundRectangle2D.Double(2.5, 3.5, 11, 9, 3, 3));
                    g.draw(linha(5.5, 7, 10.5, 7));
                    g.draw(linha(5.5, 9.5, 9, 9.5));
                }
                case TOKEN -> g.fill(new Ellipse2D.Double(4.5, 4.5, 7, 7));
                case ERRO -> {
                    g.fill(new Ellipse2D.Double(1.5, 1.5, 13, 13));
                    g.setColor(Color.WHITE);
                    g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(linha(8, 4.5, 8, 8.8));
                    g.fill(new Ellipse2D.Double(7, 10.6, 2, 2));
                }
            }
            g.dispose();
        }
    }
}
