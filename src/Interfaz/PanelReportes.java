package Interfaz;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/** Reportes y consultas del administrador (los mismos del menú de consola). */
public class PanelReportes extends PanelBase {

    private static final String[] REPORTES = {
        "Inventario de instalaciones y productos",
        "Historial de reservas",
        "Historial de partidos y resultados",
        "Historial de entrenamientos",
        "Asistencia a entrenamientos",
        "Goleadores de una competencia",
        "Tabla de posiciones de una competencia",
        "Ranking de un deporte individual",
        "Reporte de ventas (diario, semanal o mensual)"
    };

    private final JComboBox<String> cmbReporte = new JComboBox<>(REPORTES);
    private final JComboBox<String> cmbCompetencia = new JComboBox<>();
    private final JComboBox<String> cmbDeporte = new JComboBox<>();
    private final JComboBox<String> cmbPeriodo = new JComboBox<>(new String[]{"Diario", "Semanal (últimos 7 días)", "Mensual"});
    private final JTextField txtFecha = new JTextField(LocalDate.now().toString(), 9);
    private final JTextArea salida = UtilGUI.areaTexto();

    public PanelReportes(ControladorGUI controlador) {
        super(controlador);
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.add(new JLabel("Reporte:"));
        top.add(cmbReporte);
        top.add(new JLabel("Competencia:"));
        top.add(cmbCompetencia);
        top.add(new JLabel("Deporte:"));
        top.add(cmbDeporte);
        top.add(new JLabel("Periodo:"));
        top.add(cmbPeriodo);
        top.add(new JLabel("Fecha ref.:"));
        top.add(txtFecha);
        top.add(UtilGUI.boton("Generar", this, this::generar));
        add(top, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(salida);
        scroll.setBorder(BorderFactory.createTitledBorder("Resultado"));
        add(scroll, BorderLayout.CENTER);

        cmbReporte.addActionListener(e -> actualizarCampos());
        actualizarCampos();
        refrescar();
    }

    @Override
    protected void refrescar() {
        cmbDeporte.removeAllItems();
        for (String d : nombresDeportes(true)) {
            cmbDeporte.addItem(d);
        }
        Object competenciaActual = cmbCompetencia.getSelectedItem();
        cmbCompetencia.removeAllItems();
        for (String c : nombresCompetencias()) {
            cmbCompetencia.addItem(c);
        }
        if (competenciaActual != null) {
            cmbCompetencia.setSelectedItem(competenciaActual);
        }
    }

    /** Solo habilita los campos que necesita el reporte elegido. */
    private void actualizarCampos() {
        int i = cmbReporte.getSelectedIndex();
        cmbCompetencia.setEnabled(i == 5 || i == 6);
        cmbDeporte.setEnabled(i == 7);
        cmbPeriodo.setEnabled(i == 8);
        txtFecha.setEnabled(i == 8);
    }

    private void generar() {
        List<String> lineas = new ArrayList<>();
        switch (cmbReporte.getSelectedIndex()) {
            case 0 -> {
                lineas.addAll(club().reporteInventarioInstalaciones());
                lineas.addAll(tienda().reporteInventarioProductos());
            }
            case 1 -> lineas = club().reporteHistorialReservas();
            case 2 -> lineas = club().reporteHistorialPartidos();
            case 3 -> lineas = club().reporteHistorialSesiones();
            case 4 -> lineas = club().reporteAsistencia();
            case 5 -> lineas = club().reporteGoleadores(competenciaRequerida());
            case 6 -> lineas = club().reporteTablaPosiciones(competenciaRequerida());
            case 7 -> {
                String deporte = (String) cmbDeporte.getSelectedItem();
                if (deporte == null) {
                    throw new IllegalArgumentException("No hay deportes individuales registrados.");
                }
                lineas = club().reporteRankingIndividual(deporte);
            }
            default -> lineas = reporteVentas();
        }
        salida.setText(UtilGUI.unir(lineas));
        salida.setCaretPosition(0);
    }

    private String competenciaRequerida() {
        String c = (String) cmbCompetencia.getSelectedItem();
        if (c == null) {
            throw new IllegalArgumentException("Todavía no hay competencias creadas.");
        }
        return c;
    }

    private List<String> reporteVentas() {
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(txtFecha.getText().trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha inválida (use AAAA-MM-DD).");
        }
        LocalDate inicio = fecha;
        LocalDate fin = fecha;
        int periodo = cmbPeriodo.getSelectedIndex();
        if (periodo == 1) {
            inicio = fecha.minusDays(6);
        } else if (periodo == 2) {
            inicio = fecha.withDayOfMonth(1);
            fin = fecha.withDayOfMonth(fecha.lengthOfMonth());
        }
        List<String> lineas = new ArrayList<>();
        lineas.add("Ventas del " + inicio + " al " + fin + ":");
        lineas.addAll(tienda().reporteVentas(inicio.toString(), fin.toString()));
        return lineas;
    }
}
