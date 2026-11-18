package Interfaz;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/** Gráfica de barras agrupadas (una barra por serie en cada categoría). */
public class GraficoBarras extends GraficoCartesiano {

    @Override
    protected boolean ceroEnY() {
        return true;
    }

    @Override
    protected void dibujarSeries(Graphics2D g) {
        int nSeries = series.size();
        int nCats = categorias.size();
        double banda = (double) plot.width / nCats;
        double anchoGrupo = Math.min(banda * 0.72, 56.0 * nSeries);
        double separacion = nSeries > 1 ? 2 : 0;
        double anchoBarra = (anchoGrupo - separacion * (nSeries - 1)) / nSeries;
        double base = pixelY(0);
        boolean etiquetar = nSeries * nCats <= 18;
        g.setFont(FUENTE_VALOR);
        FontMetrics fm = g.getFontMetrics();

        List<String> nombres = new ArrayList<>(series.keySet());
        for (int s = 0; s < nSeries; s++) {
            List<Double> valores = series.get(nombres.get(s));
            for (int c = 0; c < nCats; c++) {
                double v = valor(valores, c);
                if (Double.isNaN(v)) {
                    continue;
                }
                double x = centroX(c) - anchoGrupo / 2 + s * (anchoBarra + separacion);
                double y = pixelY(v);
                double alto = base - y;
                g.setColor(color(s));
                Rectangle zona;
                if (alto >= 0) {
                    g.fill(barraRedondeada(x, y, anchoBarra, alto));
                    zona = new Rectangle((int) x, (int) y, (int) Math.ceil(anchoBarra), (int) Math.ceil(Math.max(alto, 4)));
                } else {
                    g.fillRect((int) x, (int) base, (int) Math.ceil(anchoBarra), (int) Math.ceil(-alto));
                    zona = new Rectangle((int) x, (int) base, (int) Math.ceil(anchoBarra), (int) Math.ceil(-alto));
                }
                // zona sensible ampliada hacia arriba para que sea fácil de apuntar
                zona.grow(2, 4);
                regiones.add(new Region(zona, nombres.get(s) + " — " + categorias.get(c) + ": " + formato.apply(v)));

                if (etiquetar && v != 0) {
                    String t = formato.apply(v);
                    g.setColor(TEXTO_SECUNDARIO);
                    float ty = alto >= 0 ? (float) y - 4 : (float) (base - alto) + 11;
                    g.drawString(t, (float) (x + anchoBarra / 2 - fm.stringWidth(t) / 2.0), ty);
                }
            }
        }
    }

    /** Barra con las esquinas superiores redondeadas (4 px) y la base recta sobre el eje. */
    private Path2D barraRedondeada(double x, double y, double w, double h) {
        double r = Math.min(4, Math.min(w / 2, h));
        Path2D p = new Path2D.Double();
        p.moveTo(x, y + h);
        p.lineTo(x, y + r);
        p.quadTo(x, y, x + r, y);
        p.lineTo(x + w - r, y);
        p.quadTo(x + w, y, x + w, y + r);
        p.lineTo(x + w, y + h);
        p.closePath();
        return p;
    }
}
