package Interfaz;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleFunction;

/**
 * Base de las gráficas con ejes (barras y líneas). Recibe una lista de categorías para el eje X
 * y una o varias series (nombre -> un valor por categoría). Un valor Double.NaN significa "sin dato".
 */
public abstract class GraficoCartesiano extends GraficoBase {

    protected List<String> categorias = new ArrayList<>();
    protected Map<String, List<Double>> series = new LinkedHashMap<>();
    protected DoubleFunction<String> formato = GraficoBase::formatoNumero;

    /** Rectángulo donde se dibujan los datos (dentro de los ejes). */
    protected Rectangle plot;
    protected double yMin;
    protected double yMax;

    /**
     * @param titulo     título de la gráfica
     * @param ejeY       descripción de lo que mide el eje Y (con unidades)
     * @param categorias etiquetas del eje X
     * @param series     nombre de la serie -> un valor por categoría (mismo orden que categorias)
     */
    public void setDatos(String titulo, String ejeY, List<String> categorias, Map<String, List<Double>> series) {
        this.titulo = titulo;
        this.subtitulo = ejeY == null ? "" : ejeY;
        this.categorias = new ArrayList<>(categorias);
        this.series = new LinkedHashMap<>(series);
        repaint();
    }

    public void setFormatoValor(DoubleFunction<String> formato) {
        this.formato = formato;
    }

    /** Las barras siempre parten de cero; las líneas pueden ajustar el eje al rango de los datos. */
    protected abstract boolean ceroEnY();

    protected abstract void dibujarSeries(Graphics2D g);

    @Override
    protected boolean hayDatos() {
        if (categorias.isEmpty() || series.isEmpty()) {
            return false;
        }
        for (List<Double> valores : series.values()) {
            for (Double v : valores) {
                if (v != null && !v.isNaN()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Centro horizontal de la categoría i. */
    protected double centroX(int i) {
        return plot.x + plot.width * (i + 0.5) / categorias.size();
    }

    protected double pixelY(double valor) {
        return plot.y + plot.height - (valor - yMin) / (yMax - yMin) * plot.height;
    }

    protected double valor(List<Double> lista, int i) {
        if (i >= lista.size() || lista.get(i) == null) {
            return Double.NaN;
        }
        return lista.get(i);
    }

    @Override
    protected void dibujar(Graphics2D g, int x, int y, int ancho, int alto) {
        // Leyenda: solo si hay más de una serie
        int altoLeyenda = 0;
        if (series.size() > 1) {
            altoLeyenda = dibujarLeyenda(g, new ArrayList<>(series.keySet()), x, y, ancho) + 6;
        }
        g.setFont(FUENTE_NORMAL);
        FontMetrics fm = g.getFontMetrics();

        calcularEscala();
        List<Double> marcas = marcasEjeY();
        int anchoEtiquetaY = 0;
        for (double m : marcas) {
            anchoEtiquetaY = Math.max(anchoEtiquetaY, fm.stringWidth(formato.apply(m)));
        }
        int izq = x + anchoEtiquetaY + 10;
        int arriba = y + altoLeyenda + 8;
        int abajo = y + alto - 22;
        plot = new Rectangle(izq, arriba, Math.max(10, x + ancho - izq - 4), Math.max(10, abajo - arriba));

        // Cuadrícula horizontal tenue y etiquetas del eje Y
        for (double m : marcas) {
            double py = pixelY(m);
            g.setColor(GRILLA);
            g.setStroke(trazo(1f));
            g.draw(new Line2D.Double(plot.x, py, plot.x + plot.width, py));
            g.setColor(TEXTO_SECUNDARIO);
            String t = formato.apply(m);
            g.drawString(t, plot.x - 6 - fm.stringWidth(t), (float) py + 4);
        }
        // Eje base
        g.setColor(EJE);
        g.draw(new Line2D.Double(plot.x, plot.y + plot.height, plot.x + plot.width, plot.y + plot.height));

        // Etiquetas del eje X
        g.setColor(TEXTO_SECUNDARIO);
        int anchoBanda = plot.width / categorias.size();
        int paso = 1;
        int anchoMaxEtiqueta = 0;
        for (String c : categorias) {
            anchoMaxEtiqueta = Math.max(anchoMaxEtiqueta, Math.min(fm.stringWidth(c), 90));
        }
        if (anchoMaxEtiqueta > anchoBanda) {
            paso = (int) Math.ceil((double) anchoMaxEtiqueta / Math.max(anchoBanda, 1));
        }
        for (int i = 0; i < categorias.size(); i += paso) {
            String t = ajustar(fm, categorias.get(i), Math.max(anchoBanda * paso - 4, 20));
            g.drawString(t, (float) (centroX(i) - fm.stringWidth(t) / 2.0), plot.y + plot.height + 16);
        }

        dibujarSeries(g);
    }

    private void calcularEscala() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (List<Double> valores : series.values()) {
            for (Double v : valores) {
                if (v != null && !v.isNaN()) {
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                }
            }
        }
        if (ceroEnY()) {
            min = Math.min(min, 0);
            max = Math.max(max, 0);
        }
        if (max == min) {
            max = min + 1;
        }
        double paso = pasoAgradable((max - min) / 5);
        yMin = Math.floor(min / paso) * paso;
        yMax = Math.ceil(max / paso) * paso;
        if (!ceroEnY() && yMax - yMin < paso * 2) {
            yMax = yMin + paso * 2;
        }
    }

    private List<Double> marcasEjeY() {
        double paso = pasoAgradable((yMax - yMin) / 5);
        List<Double> r = new ArrayList<>();
        for (double v = yMin; v <= yMax + paso * 1e-6; v += paso) {
            r.add(v);
        }
        return r;
    }

    /** Redondea un paso a 1, 2, 5 o 10 por una potencia de diez. */
    private static double pasoAgradable(double crudo) {
        double exp = Math.floor(Math.log10(crudo));
        double base = Math.pow(10, exp);
        double f = crudo / base;
        double nice = f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10;
        return nice * base;
    }
}
