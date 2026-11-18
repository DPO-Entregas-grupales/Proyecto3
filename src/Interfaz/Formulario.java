package Interfaz;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * Construye diálogos de entrada de datos. Se agregan los campos con una clave y luego
 * se leen por esa clave después de mostrar el diálogo.
 *
 * <pre>
 * Formulario f = new Formulario("Crear equipo").texto("nombre", "Nombre").entero("cupo", "Cupo", 10, 1, 50);
 * if (f.mostrar(this)) { String n = f.valor("nombre"); int c = f.entero("cupo"); }
 * </pre>
 */
public class Formulario {

    private final String titulo;
    private final JPanel panel = new JPanel(new GridBagLayout());
    private final Map<String, JComponent> campos = new LinkedHashMap<>();
    private final Map<String, String> etiquetas = new LinkedHashMap<>();
    private int fila = 0;

    public Formulario(String titulo) {
        this.titulo = titulo;
    }

    private Formulario agregar(String clave, String etiqueta, JComponent componente) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = fila;
        gbc.insets = new Insets(4, 4, 4, 8);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        panel.add(new JLabel(etiqueta + ":"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(componente, gbc);
        campos.put(clave, componente);
        etiquetas.put(clave, etiqueta);
        fila++;
        return this;
    }

    public Formulario texto(String clave, String etiqueta) {
        return agregar(clave, etiqueta, new JTextField(18));
    }

    public Formulario texto(String clave, String etiqueta, String inicial) {
        return agregar(clave, etiqueta, new JTextField(inicial, 18));
    }

    public Formulario clave(String clave, String etiqueta) {
        return agregar(clave, etiqueta, new JPasswordField(18));
    }

    public Formulario fecha(String clave, String etiqueta) {
        return agregar(clave, etiqueta + " (AAAA-MM-DD)", new JTextField(LocalDate.now().toString(), 18));
    }

    public Formulario entero(String clave, String etiqueta, int inicial, int min, int max) {
        return agregar(clave, etiqueta, new JSpinner(new SpinnerNumberModel(inicial, min, max, 1)));
    }

    public Formulario decimal(String clave, String etiqueta, double inicial, double min, double max, double paso) {
        return agregar(clave, etiqueta, new JSpinner(new SpinnerNumberModel(inicial, min, max, paso)));
    }

    public Formulario combo(String clave, String etiqueta, Collection<String> opciones) {
        JComboBox<String> combo = new JComboBox<>(opciones.toArray(new String[0]));
        return agregar(clave, etiqueta, combo);
    }

    /** Combo con sugerencias que además permite escribir un valor distinto. */
    public Formulario comboEditable(String clave, String etiqueta, Collection<String> opciones) {
        JComboBox<String> combo = new JComboBox<>(opciones.toArray(new String[0]));
        combo.setEditable(true);
        return agregar(clave, etiqueta, combo);
    }

    public Formulario casilla(String clave, String etiqueta) {
        return agregar(clave, etiqueta, new JCheckBox());
    }

    /** Lista de selección múltiple. */
    public Formulario lista(String clave, String etiqueta, Collection<String> opciones) {
        DefaultListModel<String> modelo = new DefaultListModel<>();
        for (String o : opciones) {
            modelo.addElement(o);
        }
        JList<String> lista = new JList<>(modelo);
        lista.setVisibleRowCount(6);
        JScrollPane scroll = new JScrollPane(lista);
        scroll.setPreferredSize(new Dimension(260, 110));
        campos.put(clave, lista);
        etiquetas.put(clave, etiqueta);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = fila;
        gbc.insets = new Insets(4, 4, 4, 8);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        panel.add(new JLabel(etiqueta + ":"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(scroll, gbc);
        fila++;
        return this;
    }

    /** Muestra el diálogo. Devuelve true si el usuario pulsó Aceptar. */
    public boolean mostrar(Component padre) {
        int r = JOptionPane.showConfirmDialog(padre, panel, titulo, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return r == JOptionPane.OK_OPTION;
    }

    // ---------- lectura de valores ----------

    /** Texto de un campo de texto, contraseña o combo (ya sin espacios sobrantes). */
    public String valor(String clave) {
        JComponent c = campos.get(clave);
        if (c instanceof JPasswordField) {
            return new String(((JPasswordField) c).getPassword()).trim();
        }
        if (c instanceof JTextField) {
            return ((JTextField) c).getText().trim();
        }
        if (c instanceof JComboBox) {
            Object o = ((JComboBox<?>) c).getSelectedItem();
            return o == null ? "" : o.toString();
        }
        throw new IllegalArgumentException("El campo " + clave + " no es de texto.");
    }

    /** Igual que valor pero valida que sea una fecha ISO (AAAA-MM-DD). */
    public String fechaValor(String clave) {
        String v = valor(clave);
        try {
            LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha debe tener el formato AAAA-MM-DD (por ejemplo 2026-11-18).");
        }
        return v;
    }

    public int entero(String clave) {
        return ((Number) valorNumerico(clave)).intValue();
    }

    public double decimal(String clave) {
        return ((Number) valorNumerico(clave)).doubleValue();
    }

    private Object valorNumerico(String clave) {
        JSpinner s = (JSpinner) campos.get(clave);
        try {
            s.commitEdit();
        } catch (ParseException e) {
            throw new IllegalArgumentException("Valor numérico inválido en: " + etiquetas.get(clave));
        }
        return s.getValue();
    }

    public boolean marcado(String clave) {
        return ((JCheckBox) campos.get(clave)).isSelected();
    }

    public List<String> seleccionados(String clave) {
        JList<?> lista = (JList<?>) campos.get(clave);
        List<String> r = new ArrayList<>();
        for (Object o : lista.getSelectedValuesList()) {
            r.add(o.toString());
        }
        return r;
    }

    /** Lanza un error si alguno de los campos indicados está vacío. */
    public void requerir(String... claves) {
        for (String clave : claves) {
            if (valor(clave).isEmpty()) {
                throw new IllegalArgumentException("Complete el campo: " + etiquetas.get(clave));
            }
        }
    }
}
