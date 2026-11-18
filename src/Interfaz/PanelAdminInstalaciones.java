package Interfaz;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import ReservasInstalaciones.Instalacion;

/** Instalaciones (registrar, horarios, máximo simultáneo por deporte) y turnos del club. */
public class PanelAdminInstalaciones extends PanelBase {

    private final PanelTabla tabla = new PanelTabla("Instalaciones", "Nombre", "Tipo", "Deporte", "Capacidad");

    public PanelAdminInstalaciones(ControladorGUI controlador) {
        super(controlador);
        JPanel panelInst = new JPanel(new BorderLayout(8, 8));
        panelInst.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panelInst.add(tabla, BorderLayout.CENTER);
        panelInst.add(UtilGUI.barra(
                UtilGUI.boton("Registrar instalación", this, this::registrar),
                UtilGUI.boton("Generar horarios de la seleccionada", this, this::generarHorarios),
                UtilGUI.boton("Máximo de instalaciones simultáneas por deporte", this, this::definirMaximo)), BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Instalaciones", panelInst);
        tabs.addTab("Turnos", new PanelTurnos(controlador));
        add(tabs, BorderLayout.CENTER);
        refrescar();
    }

    @Override
    protected void refrescar() {
        tabla.limpiar();
        List<Instalacion> ordenadas = new java.util.ArrayList<>(club().getInstalaciones().values());
        ordenadas.sort(java.util.Comparator.comparing(Instalacion::getNombre));
        for (Instalacion i : ordenadas) {
            String deporte = i.getDeporte() == null ? "sin disciplina" : i.getDeporte().getNombreDeporte();
            tabla.agregar(i.getNombre(), i.getTipo(), deporte, i.getCapacidadMaximaJugadores());
        }
    }

    /** Tipos ya usados en el club primero (así se respeta cómo están escritos en los datos) y luego los típicos. */
    private List<String> tiposDeInstalacion() {
        Set<String> tipos = new LinkedHashSet<>();
        for (Instalacion i : club().getInstalaciones().values()) {
            tipos.add(i.getTipo());
        }
        tipos.addAll(Arrays.asList("Cancha de tenis", "Cancha de pádel", "Mesa de tenis de mesa",
                "Cancha de fútbol", "Cancha de baloncesto", "Cancha de voleibol"));
        return new ArrayList<>(tipos);
    }

    private void registrar() {
        Formulario f = new Formulario("Registrar instalación")
                .texto("nombre", "Nombre")
                .comboEditable("tipo", "Tipo de instalación", tiposDeInstalacion())
                .casilla("techada", "¿Techada?")
                .entero("cap", "Capacidad", 10, 1, 1000)
                .combo("deporte", "Deporte", nombresDeportes(null));
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("nombre", "tipo", "deporte");
        club().crearInstalacion(f.valor("nombre"), f.valor("tipo"), f.marcado("techada"), f.entero("cap"), f.valor("deporte"));
        guardar();
        refrescar();
        info("Instalación creada.");
    }

    private void generarHorarios() {
        String nombre = tabla.requerirSeleccion(0, "Seleccione una instalación de la tabla.");
        Formulario f = new Formulario("Generar horarios de " + nombre)
                .fecha("fecha", "Fecha")
                .entero("ini", "Hora inicio (0-23)", 8, 0, 23)
                .entero("fin", "Hora fin (0-23)", 20, 0, 23);
        if (!f.mostrar(this)) {
            return;
        }
        if (f.entero("fin") < f.entero("ini")) {
            throw new IllegalArgumentException("La hora de fin no puede ser menor que la de inicio.");
        }
        club().nuevosHorarios(nombre, f.fechaValor("fecha"), f.entero("ini"), f.entero("fin"));
        guardar();
        info("Horarios agregados.");
    }

    private void definirMaximo() {
        Formulario f = new Formulario("Máximo de instalaciones simultáneas")
                .combo("deporte", "Deporte", nombresDeportes(null))
                .entero("max", "Máximo simultáneo (0 = sin límite)", 0, 0, 100);
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("deporte");
        club().definirMaximoInstalacionesSimultaneas(f.valor("deporte"), f.entero("max"));
        guardar();
        info("Máximo actualizado.");
    }
}
