package Interfaz;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import Partidos.Partido;
import Partidos.PartidoIndividual;
import Usuarios.Socio;
import Usuarios.Usuario;

/** Partidos de deportes individuales del socio: programar, registrar resultado y estadísticas. */
public class PanelSocioPartidos extends PanelBase {

    private final PanelTabla tabla = new PanelTabla("Mis partidos individuales", "ID", "Deporte", "Jugador 1", "Jugador 2", "Fecha", "Marcador", "Estado");

    public PanelSocioPartidos(ControladorGUI controlador) {
        super(controlador);
        add(tabla, BorderLayout.CENTER);
        add(UtilGUI.barra(
                UtilGUI.boton("Programar partido", this, this::programar),
                UtilGUI.boton("Registrar resultado del partido seleccionado", this, this::registrarResultado),
                UtilGUI.boton("Registrar mis estadísticas del partido seleccionado", this, this::registrarEstadisticas)), BorderLayout.SOUTH);
        refrescar();
    }

    @Override
    protected void refrescar() {
        tabla.limpiar();
        List<Partido> ordenados = new java.util.ArrayList<>(club().getPartidos().values());
        ordenados.sort(java.util.Comparator.comparing(Partido::getFecha).reversed()
                .thenComparing((a, b) -> Integer.compare(numeroDe(b.getIdPartido()), numeroDe(a.getIdPartido()))));
        for (Partido p : ordenados) {
            if (p instanceof PartidoIndividual) {
                PartidoIndividual i = (PartidoIndividual) p;
                if (i.getSocio1() == usuario() || i.getSocio2() == usuario()) {
                    String deporte = p.getDeporte() == null ? "" : p.getDeporte().getNombreDeporte();
                    tabla.agregar(p.getIdPartido(), deporte, i.getSocio1().getNombre(), i.getSocio2().getNombre(),
                            p.getFecha(), p.getMarcadorFinal(), p.getEstadoPartido());
                }
            }
        }
    }

    private String partidoSeleccionado(String estado, String mensajeEstado) {
        String id = tabla.requerirSeleccion(0, "Seleccione un partido de la tabla.");
        Partido p = club().getPartidos().get(id);
        if (p == null || !estado.equalsIgnoreCase(p.getEstadoPartido())) {
            throw new IllegalArgumentException(mensajeEstado);
        }
        return id;
    }

    private void programar() {
        List<String> rivales = new ArrayList<>();
        for (Usuario u : club().getUsuarios().values()) {
            if (u instanceof Socio && u != usuario()) {
                rivales.add(opcion(u));
            }
        }
        Formulario f = new Formulario("Programar partido individual")
                .combo("deporte", "Deporte individual", nombresDeportes(true))
                .combo("rival", "Socio rival", rivales)
                .fecha("fecha", "Fecha");
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("deporte", "rival");
        String id = controlador.generarIdPartido();
        club().programarPartidoIndividual(id, f.fechaValor("fecha"), usuario().getIdUsuario(), idDe(f.valor("rival")), f.valor("deporte"));
        guardar();
        refrescar();
        info("Partido individual programado (ID: " + id + ").");
    }

    private void registrarResultado() {
        String id = partidoSeleccionado("Programado", "Seleccione un partido programado para registrar su resultado.");
        Formulario f = new Formulario("Resultado del partido " + id)
                .entero("propios", "Sets ganados por usted", 0, 0, 10)
                .entero("rival", "Sets ganados por su rival", 0, 0, 10);
        if (!f.mostrar(this)) {
            return;
        }
        club().registrarResultadoIndividual(id, usuario().getIdUsuario(), f.entero("propios"), f.entero("rival"));
        guardar();
        refrescar();
        info("Resultado registrado. El ranking de los dos jugadores se actualizó.");
    }

    private void registrarEstadisticas() {
        String id = partidoSeleccionado("Finalizado", "Seleccione un partido finalizado para registrar sus estadísticas.");
        Partido partido = club().getPartidos().get(id);
        boolean mesa = partido.getDeporte().getNombreDeporte().toLowerCase().contains("mesa");
        Formulario f = new Formulario("Mis estadísticas del partido " + id).entero("sets", "Sets ganados", 0, 0, 10);
        if (mesa) {
            f.entero("puntos", "Puntos totales", 0, 0, 500);
        } else {
            f.entero("games", "Games ganados", 0, 0, 100)
                    .entero("aces", "Aces", 0, 0, 100)
                    .entero("errores", "Errores no forzados", 0, 0, 200);
        }
        if (!f.mostrar(this)) {
            return;
        }
        int games = mesa ? 0 : f.entero("games");
        int aces = mesa ? 0 : f.entero("aces");
        int errores = mesa ? 0 : f.entero("errores");
        int puntos = mesa ? f.entero("puntos") : 0;
        club().registrarEstadisticaRaqueta(id, usuario().getIdUsuario(), f.entero("sets"), games, aces, errores, puntos);
        guardar();
        info("Estadísticas registradas.");
    }
}
