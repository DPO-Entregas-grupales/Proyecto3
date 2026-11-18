package Interfaz;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Gráfica de pastel: cada porción es un valor; la leyenda muestra el valor y el porcentaje. */
public class GraficoPastel extends GraficoBase {

    private Map<String, Double> valores = new LinkedHashMap<>();

    /**
     * @param titulo  título de la gráfica
     * @param valores nombre de la porción -> valor (por ejemplo "Victorias" -> 7)
     */
    public void setDatos(String titulo, Map<String, Double> valores) {
        this.titulo = titulo;
        this.subtitulo = "";
        this.valores = new LinkedHashMap<>(valores);
        repaint();
    }

    private double total() {
        double t = 0;
        for (double v : valores.values()) {
            t += v;
        }
        return t;
    }

    @Override
    protected boolean hayDatos() {
        return total() > 0;
    }

    @Override
    protected void dibujar(Graphics2D g, int x, int y, int ancho, int alto) {
        double total = total();
        int diametro = Math.max(40, Math.min(alto - 8, (int) (ancho * 0.55)));
        double px = x + 8;
        double py = y + (alto - diametro) / 2.0;

        List<String> nombres = new ArrayList<>(valores.keySet());
        double inicio = 90; // las porciones empiezan arriba y avanzan en sentido horario
        g.setFont(FUENTE_NORMAL);
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < nombres.size(); i++) {
            double v = valores.get(nombres.get(i));
            if (v <= 0) {
                continue;
            }
            double extension = -360.0 * v / total;
            Arc2D porcion = new Arc2D.Double(px, py, diametro, diametro, inicio, extension, Arc2D.PIE);
            g.setColor(color(i));
            g.fill(porcion);
            g.setColor(SUPERFICIE); // separador de 2 px entre porciones
            g.setStroke(trazo(2f));
            g.draw(porcion);
            regiones.add(new Region(porcion, nombres.get(i) + ": " + formatoNumero(v)
                    + " (" + String.format(java.util.Locale.US, "%.1f", 100 * v / total) + "%)"));

            // Porcentaje dentro de la porción si es lo bastante grande
            if (v / total >= 0.08) {
                double medio = Math.toRadians(inicio + extension / 2);
                double r = diametro * 0.30;
                String t = Math.round(100 * v / total) + "%";
                double cx = px + diametro / 2.0 + Math.cos(medio) * r;
                double cy = py + diametro / 2.0 - Math.sin(medio) * r;
                g.setColor(esClaro(color(i)) ? TEXTO : Color.WHITE);
                g.drawString(t, (float) (cx - fm.stringWidth(t) / 2.0), (float) cy + 4);
            }
            inicio += extension;
        }

        // Leyenda vertical a la derecha: cuadrito de color + nombre + valor (porcentaje)
        int lx = (int) px + diametro + 28;
        int ly = y + Math.max(0, (alto - nombres.size() * 24) / 2) + 10;
        for (int i = 0; i < nombres.size(); i++) {
            double v = valores.get(nombres.get(i));
            g.setColor(color(i));
            g.fillRoundRect(lx, ly - 9, 10, 10, 3, 3);
            g.setColor(TEXTO);
            String t = nombres.get(i) + ": " + formatoNumero(v) + " (" + Math.round(100 * v / total) + "%)";
            g.drawString(ajustar(fm, t, Math.max(40, x + ancho - lx - 16)), lx + 16, ly);
            ly += 24;
        }
    }

    private static boolean esClaro(Color c) {
        double luminancia = 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
        return luminancia > 150;
    }
}
