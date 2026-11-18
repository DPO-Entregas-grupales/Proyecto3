package Interfaz;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

/**
 * Base de las gráficas hechas a mano con Java2D. Se encarga del fondo, el título, la leyenda,
 * el mensaje "sin datos" y los tooltips al pasar el mouse sobre cada marca (barra, punto o porción).
 * Las gráficas se dibujan siempre desde los datos que reciben, nunca desde una imagen.
 */
public abstract class GraficoBase extends JPanel {

    /** Colores categóricos en orden fijo (azul, naranja, aqua, amarillo, magenta, verde, violeta, rojo). */
    protected static final Color[] PALETA = {
        new Color(0x2A78D6), new Color(0xEB6834), new Color(0x1BAF7A), new Color(0xEDA100),
        new Color(0xE87BA4), new Color(0x008300), new Color(0x6250D6), new Color(0xE34948)
    };
    protected static final Color SUPERFICIE = new Color(0xFC, 0xFC, 0xFB);
    protected static final Color TEXTO = new Color(0x0B0B0B);
    protected static final Color TEXTO_SECUNDARIO = new Color(0x52514E);
    protected static final Color GRILLA = new Color(0xE4E3DE);
    protected static final Color EJE = new Color(0xB9B8B2);

    protected static final Font FUENTE_TITULO = new Font("SansSerif", Font.BOLD, 14);
    protected static final Font FUENTE_NORMAL = new Font("SansSerif", Font.PLAIN, 11);
    protected static final Font FUENTE_VALOR = new Font("SansSerif", Font.PLAIN, 10);

    /** Zona sensible al mouse con el texto que se muestra como tooltip. */
    protected static class Region {
        final Shape forma;
        final String texto;

        Region(Shape forma, String texto) {
            this.forma = forma;
            this.texto = texto;
        }
    }

    protected String titulo = "";
    protected String subtitulo = "";
    protected String mensajeVacio = "Sin datos para mostrar con los filtros actuales.";
    protected final List<Region> regiones = new ArrayList<>();

    protected GraficoBase() {
        setBackground(SUPERFICIE);
        setOpaque(true);
        setPreferredSize(new Dimension(520, 340));
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    protected static Color color(int indice) {
        return PALETA[indice % PALETA.length];
    }

    protected abstract boolean hayDatos();

    /** Dibuja el contenido de la gráfica en el área libre que queda bajo el título. */
    protected abstract void dibujar(Graphics2D g, int x, int y, int ancho, int alto);

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        regiones.clear();

        g.setColor(SUPERFICIE);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setFont(FUENTE_TITULO);
        g.setColor(TEXTO);
        g.drawString(titulo, 16, 24);
        int yInicio = 34;
        if (!subtitulo.isEmpty()) {
            g.setFont(FUENTE_NORMAL);
            g.setColor(TEXTO_SECUNDARIO);
            g.drawString(subtitulo, 16, 40);
            yInicio = 48;
        }

        if (!hayDatos()) {
            g.setFont(FUENTE_NORMAL);
            g.setColor(TEXTO_SECUNDARIO);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(mensajeVacio, (getWidth() - fm.stringWidth(mensajeVacio)) / 2, getHeight() / 2);
        } else {
            dibujar(g, 16, yInicio, getWidth() - 32, getHeight() - yInicio - 12);
        }
        g.dispose();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        for (int i = regiones.size() - 1; i >= 0; i--) {
            if (regiones.get(i).forma.contains(e.getPoint())) {
                return regiones.get(i).texto;
            }
        }
        return null;
    }

    /**
     * Dibuja una leyenda horizontal (con saltos de línea si no cabe) y devuelve el alto que ocupó.
     * El texto va en color de texto; el color de la serie solo se ve en el cuadrito.
     */
    protected int dibujarLeyenda(Graphics2D g, List<String> nombres, int x, int y, int anchoMax) {
        g.setFont(FUENTE_NORMAL);
        FontMetrics fm = g.getFontMetrics();
        int cx = x;
        int cy = y;
        int paso = 20;
        for (int i = 0; i < nombres.size(); i++) {
            String nombre = nombres.get(i);
            int anchoItem = 10 + 6 + fm.stringWidth(nombre) + 16;
            if (cx + anchoItem > x + anchoMax && cx > x) {
                cx = x;
                cy += paso;
            }
            g.setColor(color(i));
            g.fill(new RoundRectangle2D.Double(cx, cy, 10, 10, 3, 3));
            g.setColor(TEXTO_SECUNDARIO);
            g.drawString(nombre, cx + 16, cy + 9);
            cx += anchoItem;
        }
        return cy - y + paso;
    }

    /** Recorta el texto con "…" para que quepa en el ancho dado. */
    protected static String ajustar(FontMetrics fm, String texto, int ancho) {
        if (fm.stringWidth(texto) <= ancho) {
            return texto;
        }
        String t = texto;
        while (t.length() > 1 && fm.stringWidth(t + "…") > ancho) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "…";
    }

    protected static BasicStroke trazo(float ancho) {
        return new BasicStroke(ancho, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    /** Formato por defecto: enteros sin decimales y el resto con dos decimales. */
    protected static String formatoNumero(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.format(java.util.Locale.US, "%.2f", v);
    }
}
