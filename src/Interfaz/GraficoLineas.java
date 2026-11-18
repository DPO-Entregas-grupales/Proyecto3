package Interfaz;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/** Gráfica de líneas con marcadores (una línea por serie; las categorías suelen ser semanas o fechas). */
public class GraficoLineas extends GraficoCartesiano {

    @Override
    protected boolean ceroEnY() {
        return false;
    }

    @Override
    protected void dibujarSeries(Graphics2D g) {
        List<String> nombres = new ArrayList<>(series.keySet());
        boolean etiquetar = nombres.size() == 1 && categorias.size() <= 10;
        g.setFont(FUENTE_VALOR);
        FontMetrics fm = g.getFontMetrics();

        for (int s = 0; s < nombres.size(); s++) {
            List<Double> valores = series.get(nombres.get(s));
            // Línea: se corta donde falta un dato (NaN)
            Path2D linea = new Path2D.Double();
            boolean abierto = false;
            for (int c = 0; c < categorias.size(); c++) {
                double v = valor(valores, c);
                if (Double.isNaN(v)) {
                    abierto = false;
                    continue;
                }
                if (!abierto) {
                    linea.moveTo(centroX(c), pixelY(v));
                    abierto = true;
                } else {
                    linea.lineTo(centroX(c), pixelY(v));
                }
            }
            g.setColor(color(s));
            g.setStroke(trazo(2f));
            g.draw(linea);

            // Marcadores de 8 px con un anillo del color de la superficie para separarlos de la línea
            for (int c = 0; c < categorias.size(); c++) {
                double v = valor(valores, c);
                if (Double.isNaN(v)) {
                    continue;
                }
                double x = centroX(c);
                double y = pixelY(v);
                g.setColor(SUPERFICIE);
                g.fill(new Ellipse2D.Double(x - 6, y - 6, 12, 12));
                g.setColor(color(s));
                g.fill(new Ellipse2D.Double(x - 4, y - 4, 8, 8));
                regiones.add(new Region(new Ellipse2D.Double(x - 9, y - 9, 18, 18),
                        nombres.get(s) + " — " + categorias.get(c) + ": " + formato.apply(v)));
                if (etiquetar) {
                    String t = formato.apply(v);
                    g.setFont(FUENTE_VALOR);
                    g.setColor(TEXTO_SECUNDARIO);
                    g.drawString(t, (float) (x - fm.stringWidth(t) / 2.0), (float) y - 10);
                }
            }
        }
    }
}
