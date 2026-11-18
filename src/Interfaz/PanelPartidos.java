package Interfaz;

import java.awt.BorderLayout;
import java.util.List;
import Partidos.Partido;
import Partidos.PartidoCompetencia;
import Partidos.PartidoInterno;
import Usuarios.Administrador;
import Usuarios.Socio;

/**
 * Partidos de equipos. El administrador los programa; el entrenador registra el marcador,
 * las estadísticas de los jugadores y consulta si un equipo puede jugar.
 */
public class PanelPartidos extends PanelBase {

    private final boolean esAdmin;
    private final PanelTabla tabla = new PanelTabla("Partidos de equipos", "ID", "Tipo", "Detalle", "Fecha", "Marcador", "Estado");

    public PanelPartidos(ControladorGUI controlador) {
        super(controlador);
        esAdmin = usuario() instanceof Administrador;
        add(tabla, BorderLayout.CENTER);
        if (esAdmin) {
            add(UtilGUI.barra(
                    UtilGUI.boton("Programar partido interno", this, this::programarInterno),
                    UtilGUI.boton("Programar partido de competencia", this, this::programarCompetencia)), BorderLayout.SOUTH);
        } else {
            add(UtilGUI.barra(
                    UtilGUI.boton("Registrar marcador del partido seleccionado", this, this::registrarMarcador),
                    UtilGUI.boton("Registrar estadísticas de un jugador", this, this::registrarEstadisticas),
                    UtilGUI.boton("Consultar si un equipo puede jugar", this, this::consultarDisponibilidad)), BorderLayout.SOUTH);
        }
        refrescar();
    }

    @Override
    protected void refrescar() {
        tabla.limpiar();
        List<Partido> ordenados = new java.util.ArrayList<>(club().getPartidos().values());
        ordenados.sort(java.util.Comparator.comparing(Partido::getFecha).reversed()
                .thenComparing((a, b) -> Integer.compare(numeroDe(b.getIdPartido()), numeroDe(a.getIdPartido()))));
        for (Partido p : ordenados) {
            if (p instanceof PartidoInterno) {
                PartidoInterno i = (PartidoInterno) p;
                tabla.agregar(p.getIdPartido(), "Interno", i.getEquipo1().getNombreEquipo() + " vs " + i.getEquipo2().getNombreEquipo(),
                        p.getFecha(), p.getMarcadorFinal(), p.getEstadoPartido());
            } else if (p instanceof PartidoCompetencia) {
                PartidoCompetencia c = (PartidoCompetencia) p;
                tabla.agregar(p.getIdPartido(), "Competencia", c.getEquipo().getNombreEquipo() + " vs " + c.getInstitucionRival(),
                        p.getFecha(), p.getMarcadorFinal(), p.getEstadoPartido());
            }
        }
    }

    // ---------- Administrador ----------

    private void programarInterno() {
        Formulario f = new Formulario("Programar partido interno")
                .combo("e1", "Equipo 1", nombresEquipos())
                .combo("e2", "Equipo 2", nombresEquipos())
                .fecha("fecha", "Fecha");
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("e1", "e2");
        if (f.valor("e1").equals(f.valor("e2"))) {
            throw new IllegalArgumentException("Los equipos deben ser distintos.");
        }
        String id = controlador.generarIdPartido();
        club().crearPartidoInterno(id, f.fechaValor("fecha"), "0-0", "Programado", f.valor("e1"), f.valor("e2"));
        guardar();
        refrescar();
        info("Partido interno programado (ID: " + id + ").");
    }

    private void programarCompetencia() {
        Formulario f = new Formulario("Programar partido de competencia")
                .combo("equipo", "Equipo local", nombresEquipos())
                .combo("competencia", "Competencia", nombresCompetencias())
                .texto("rival", "Institución rival")
                .texto("escenario", "Escenario")
                .fecha("fecha", "Fecha");
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("equipo", "competencia", "rival", "escenario");
        String id = controlador.generarIdPartido();
        // Valida que el equipo esté inscrito y asocia el partido a la competencia (necesario para sumar puntos)
        club().programarPartidoCompetencia(id, f.fechaValor("fecha"), f.valor("equipo"), f.valor("competencia"),
                f.valor("rival"), f.valor("escenario"));
        guardar();
        refrescar();
        info("Partido de competencia programado (ID: " + id + ").");
    }

    // ---------- Entrenador ----------

    private String partidoSeleccionado(String estadoRequerido, String mensajeEstado) {
        String id = tabla.requerirSeleccion(0, "Seleccione un partido de la tabla.");
        Partido p = club().getPartidos().get(id);
        if (p == null || !estadoRequerido.equalsIgnoreCase(p.getEstadoPartido())) {
            throw new IllegalArgumentException(mensajeEstado);
        }
        return id;
    }

    private void registrarMarcador() {
        String id = partidoSeleccionado("Programado", "Solo se puede registrar el marcador de partidos programados.");
        Formulario f = new Formulario("Marcador final del partido " + id)
                .entero("a", "Marcador del equipo del club (equipo 1)", 0, 0, 500)
                .entero("b", "Marcador del rival (equipo 2)", 0, 0, 500);
        if (!f.mostrar(this)) {
            return;
        }
        club().registrarMarcadorPartido(id, f.entero("a") + "-" + f.entero("b"));
        guardar();
        refrescar();
        info("Marcador registrado. El partido quedó finalizado.");
    }

    private void registrarEstadisticas() {
        String idPartido = partidoSeleccionado("Finalizado", "Seleccione un partido finalizado para registrar estadísticas.");
        Partido partido = club().getPartidos().get(idPartido);
        String deporte = partido.getDeporte().getNombreDeporte();
        boolean futbol = deporte.equalsIgnoreCase("Fútbol");
        boolean baloncesto = deporte.equalsIgnoreCase("Baloncesto");
        boolean voleibol = deporte.equalsIgnoreCase("Voleibol");
        if (!futbol && !baloncesto && !voleibol) {
            throw new IllegalArgumentException("Este deporte no tiene estadísticas por jugador definidas.");
        }

        Formulario f = new Formulario("Estadísticas de un jugador (" + deporte + ")")
                .combo("socio", "Jugador", opcionesUsuarios(Socio.class));
        if (futbol) {
            f.entero("goles", "Goles", 0, 0, 50).entero("asist", "Asistencias", 0, 0, 50)
                    .entero("am", "Tarjetas amarillas", 0, 0, 5).entero("ro", "Tarjetas rojas", 0, 0, 5);
        } else if (baloncesto) {
            f.entero("puntos", "Puntos", 0, 0, 200).entero("reb", "Rebotes", 0, 0, 100);
        } else {
            f.entero("aces", "Aces", 0, 0, 100).entero("blo", "Bloqueos", 0, 0, 100);
        }
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("socio");
        String idSocio = idDe(f.valor("socio"));
        String mensaje = "Estadísticas registradas.";
        if (futbol) {
            boolean suspendido = club().registrarEstadisticaFutbol(idPartido, idSocio, f.entero("goles"), f.entero("asist"),
                    f.entero("am"), f.entero("ro"));
            if (suspendido) {
                mensaje += "\nEl jugador queda suspendido para el siguiente partido de su equipo.";
            }
        } else if (baloncesto) {
            club().registrarEstadisticaBaloncesto(idPartido, idSocio, f.entero("puntos"), f.entero("reb"));
        } else {
            club().registrarEstadisticaVoleibol(idPartido, idSocio, f.entero("aces"), f.entero("blo"));
        }
        guardar();
        info(mensaje);
    }

    private void consultarDisponibilidad() {
        Formulario f = new Formulario("Disponibilidad de un equipo").combo("equipo", "Equipo", nombresEquipos());
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("equipo");
        List<String> lineas = club().consultarDisponibilidadEquipo(f.valor("equipo"));
        UtilGUI.mostrarTexto(this, "Disponibilidad de " + f.valor("equipo"), String.join("\n", lineas));
    }
}
