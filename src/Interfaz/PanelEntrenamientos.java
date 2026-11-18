package Interfaz;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import SesionEntrenamiento.SesionEntrenamiento;
import Usuarios.Socio;

/** Entrenamientos del entrenador: programar sesiones y registrar la asistencia de los jugadores. */
public class PanelEntrenamientos extends PanelBase {

    private static final String INDIVIDUAL = "(deporte individual)";

    private final PanelTabla tabla = new PanelTabla("Mis entrenamientos", "ID", "Fecha", "Hora", "Convocados");

    public PanelEntrenamientos(ControladorGUI controlador) {
        super(controlador);
        add(tabla, BorderLayout.CENTER);
        add(UtilGUI.barra(
                UtilGUI.boton("Programar entrenamiento", this, this::programar),
                UtilGUI.boton("Registrar asistencia del entrenamiento seleccionado", this, this::registrarAsistencia)), BorderLayout.SOUTH);
        refrescar();
    }

    @Override
    protected void refrescar() {
        tabla.limpiar();
        List<SesionEntrenamiento> ordenadas = new java.util.ArrayList<>(club().getSesionesEntrenamiento().values());
        ordenadas.sort(java.util.Comparator.comparing(SesionEntrenamiento::getFecha).reversed()
                .thenComparing((a, b) -> Integer.compare(numeroDe(b.getIdEntrenamiento()), numeroDe(a.getIdEntrenamiento()))));
        for (SesionEntrenamiento s : ordenadas) {
            if (s.getEntrenador() == usuario()) {
                tabla.agregar(s.getIdEntrenamiento(), s.getFecha(), s.getHora(), s.getEntrenamientoSocios().size());
            }
        }
    }

    /** Horas de 06:00 a 21:30 cada 30 minutos. */
    private List<String> horasDelDia() {
        List<String> horas = new ArrayList<>();
        for (int h = 6; h <= 21; h++) {
            horas.add(String.format("%02d:00", h));
            horas.add(String.format("%02d:30", h));
        }
        return horas;
    }

    private void programar() {
        List<String> equipos = new ArrayList<>();
        equipos.add(INDIVIDUAL);
        equipos.addAll(nombresEquipos());
        List<String> individuales = nombresDeportes(true);
        individuales.add(0, "");

        Formulario f = new Formulario("Programar entrenamiento")
                .fecha("fecha", "Fecha")
                .combo("hora", "Hora de inicio", horasDelDia())
                .entero("duracion", "Duración en minutos", 90, 15, 300)
                .combo("instalacion", "Instalación", nombresInstalaciones())
                .combo("equipo", "Equipo convocado", equipos)
                .combo("deporte", "Deporte individual (si no es de equipo)", individuales)
                .lista("socios", "Socios convocados (solo individual; vacío = todos los inscritos)", opcionesUsuarios(Socio.class));
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("instalacion");
        boolean individual = f.valor("equipo").equals(INDIVIDUAL);
        String equipo = individual ? "" : f.valor("equipo");
        String deporte = "";
        List<String> ids = new ArrayList<>();
        if (individual) {
            deporte = f.valor("deporte");
            if (deporte.isEmpty()) {
                throw new IllegalArgumentException("Elija el deporte individual convocado.");
            }
            for (String o : f.seleccionados("socios")) {
                ids.add(idDe(o));
            }
        }
        String id = club().generarIdSesionEntrenamiento();
        club().programarSesionEntrenamiento(id, f.fechaValor("fecha"), f.valor("hora"), f.entero("duracion"),
                usuario().getIdUsuario(), f.valor("instalacion"), equipo, deporte, ids);
        guardar();
        refrescar();
        info("Entrenamiento programado (ID: " + id + ").");
    }

    private void registrarAsistencia() {
        String idSesion = tabla.requerirSeleccion(0, "Seleccione un entrenamiento de la tabla.");
        Formulario f = new Formulario("Asistencia al entrenamiento " + idSesion)
                .combo("socio", "Jugador", opcionesUsuarios(Socio.class))
                .combo("estado", "Estado", Arrays.asList("Asistió", "Faltó", "Llegó tarde"))
                .decimal("nota", "Nota de desempeño de 1 a 5 (0 = omitir)", 0, 0, 5, 0.5)
                .texto("comentario", "Comentario (opcional)");
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("socio");
        club().registrarAsistencia(idSesion, idDe(f.valor("socio")), f.valor("estado"), f.decimal("nota"), f.valor("comentario"));
        guardar();
        info("Asistencia registrada.");
    }
}
