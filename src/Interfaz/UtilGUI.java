package Interfaz;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Utilidades comunes para todos los paneles (mensajes, botones y texto). */
public final class UtilGUI {

    /** Categorías de edad máxima que maneja el club (99 = Mayores). */
    public static final String[] CATEGORIAS = {"13", "15", "17", "99"};

    private UtilGUI() {
    }

    public static void info(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "No se pudo completar", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirmar(Component padre, String mensaje) {
        return JOptionPane.showConfirmDialog(padre, mensaje, "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    /**
     * Crea un botón que ejecuta la acción y muestra en un diálogo cualquier error de validación
     * (las excepciones IllegalArgumentException que lanza el modelo).
     */
    public static JButton boton(String texto, Component padre, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> {
            try {
                accion.run();
            } catch (IllegalArgumentException | IllegalStateException ex) {
                error(padre, ex.getMessage());
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                error(padre, "Error inesperado: " + ex);
            }
        });
        return b;
    }

    public static JPanel barra(JButton... botones) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        for (JButton b : botones) {
            p.add(b);
        }
        return p;
    }

    public static String dinero(double valor) {
        return "$" + String.format(Locale.US, "%.2f", valor);
    }

    public static JTextArea areaTexto() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return area;
    }

    public static String unir(List<String> lineas) {
        if (lineas == null || lineas.isEmpty()) {
            return "(sin registros)";
        }
        StringBuilder sb = new StringBuilder();
        for (String l : lineas) {
            sb.append("- ").append(l).append("\n");
        }
        return sb.toString();
    }

    public static void mostrarTexto(Component padre, String titulo, String texto) {
        JTextArea area = areaTexto();
        area.setText(texto);
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(560, 300));
        JOptionPane.showMessageDialog(padre, scroll, titulo, JOptionPane.PLAIN_MESSAGE);
    }
}
