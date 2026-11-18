package Interfaz;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** Tabla de solo lectura con título, selección de una fila y utilidades para llenarla. */
public class PanelTabla extends JPanel {

    private final DefaultTableModel modelo;
    private final JTable tabla;

    public PanelTabla(String titulo, String... columnas) {
        super(new BorderLayout());
        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        tabla.setRowHeight(22);
        setBorder(BorderFactory.createTitledBorder(titulo));
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    /** Vuelve a seleccionar la fila cuyo valor en la columna dada coincide (útil tras refrescar la tabla). */
    public void seleccionarFila(int columna, String valor) {
        for (int i = 0; i < modelo.getRowCount(); i++) {
            Object v = modelo.getValueAt(i, columna);
            if (v != null && v.toString().equals(valor)) {
                int vista = tabla.convertRowIndexToView(i);
                tabla.setRowSelectionInterval(vista, vista);
                return;
            }
        }
    }

    /** Valor de la celda (fila, columna) del modelo, en el orden en que se agregaron las filas. */
    public String valorEn(int fila, int columna) {
        Object v = modelo.getValueAt(fila, columna);
        return v == null ? null : v.toString();
    }

    public void limpiar() {
        modelo.setRowCount(0);
    }

    public void agregar(Object... fila) {
        modelo.addRow(fila);
    }

    /** Valor de la columna indicada en la fila seleccionada, o null si no hay selección. */
    public String seleccionado(int columna) {
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            return null;
        }
        Object valor = modelo.getValueAt(tabla.convertRowIndexToModel(fila), columna);
        return valor == null ? null : valor.toString();
    }

    /** Igual que seleccionado, pero lanza un error con el mensaje dado si no hay fila seleccionada. */
    public String requerirSeleccion(int columna, String mensaje) {
        String valor = seleccionado(columna);
        if (valor == null) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor;
    }

    public int filas() {
        return modelo.getRowCount();
    }

    public JTable getTabla() {
        return tabla;
    }
}
