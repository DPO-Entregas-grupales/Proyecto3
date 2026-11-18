package Interfaz;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;
import Partidos.Torneo;
import Partidos.TorneoConjunto;
import Partidos.TorneoIndividual;
import Tienda.CalculadoraDescuento;
import Usuarios.Administrador;
import Usuarios.Entrenador;
import Usuarios.Fisioterapeuta;
import Usuarios.Socio;
import Usuarios.Usuario;

/**
 * Torneos para todos los roles. La tabla es la misma; los botones cambian según el rol:
 * el administrador crea y finaliza torneos, el socio se inscribe, el entrenador inscribe equipos
 * y el fisioterapeuta se inscribe como empleado.
 */
public class PanelTorneos extends PanelBase {

    private final PanelTabla tabla = new PanelTabla("Torneos", "ID", "Nombre", "Tipo", "Modalidad", "Deporte",
            "Categoría", "Estado", "Inscritos", "Cupos generales", "Cupos preferenciales");

    public PanelTorneos(ControladorGUI controlador) {
        super(controlador);
        add(tabla, BorderLayout.CENTER);
        Usuario u = usuario();
        JPanel botones = new JPanel(new java.awt.GridLayout(0, 1));
        if (u instanceof Administrador) {
            botones.add(UtilGUI.barra(
                    UtilGUI.boton("Crear torneo de equipos", this, () -> crearTorneo(false)),
                    UtilGUI.boton("Crear torneo individual", this, () -> crearTorneo(true)),
                    UtilGUI.boton("Registrar partido del torneo seleccionado", this, this::registrarPartido),
                    UtilGUI.boton("Finalizar torneo seleccionado", this, this::finalizar)));
            botones.add(UtilGUI.barra(
                    UtilGUI.boton("Inscribir invitado", this, this::inscribirInvitado),
                    UtilGUI.boton("Retirar invitado", this, this::retirarInvitado)));
        } else if (u instanceof Socio) {
            botones.add(UtilGUI.barra(
                    UtilGUI.boton("Inscribirme al torneo seleccionado", this, this::inscribirSocio),
                    UtilGUI.boton("Salirme del torneo seleccionado", this, this::salirSocio),
                    UtilGUI.boton("Mis bonos de descuento", this, this::verBonos),
                    UtilGUI.boton("Historial del torneo seleccionado", this, this::historial)));
        } else {
            List<javax.swing.JButton> lista = new ArrayList<>();
            if (u instanceof Entrenador) {
                lista.add(UtilGUI.boton("Inscribir un equipo al torneo seleccionado", this, this::inscribirEquipo));
            }
            lista.add(UtilGUI.boton("Inscribirme al torneo seleccionado", this, this::inscribirEmpleado));
            lista.add(UtilGUI.boton("Salirme del torneo seleccionado", this, this::salirEmpleado));
            lista.add(UtilGUI.boton("Historial del torneo seleccionado", this, this::historial));
            botones.add(UtilGUI.barra(lista.toArray(new javax.swing.JButton[0])));
        }
        add(botones, BorderLayout.SOUTH);
        refrescar();
    }

    @Override
    protected void refrescar() {
        tabla.limpiar();
        List<Torneo> ordenados = new java.util.ArrayList<>(club().getTorneos().values());
        ordenados.sort((a, b) -> Integer.compare(numeroDe(a.getIdTorneo()), numeroDe(b.getIdTorneo())));
        for (Torneo t : ordenados) {
            String tipo = "Individual";
            int inscritos = 0;
            if (t instanceof TorneoConjunto) {
                tipo = "Equipos";
                inscritos = ((TorneoConjunto) t).getEquiposInscritos().size();
            } else if (t instanceof TorneoIndividual) {
                inscritos = ((TorneoIndividual) t).getParticipantesInscritos().size();
            }
            String modalidad = t.isCompetitivo() ? "Competitivo ($" + Math.round(t.getTarifaInscripcion()) + ")" : "Recreativo";
            tabla.agregar(t.getIdTorneo(), t.getNombre(), tipo, modalidad, t.getDeporte().getNombreDeporte(),
                    t.getCategoria(), t.getEstado(), inscritos, t.getCuposGenerales(), t.getCuposPreferenciales());
        }
    }

    // ---------- utilidades ----------

    private Torneo torneoSeleccionado() {
        String id = tabla.requerirSeleccion(0, "Seleccione un torneo de la tabla.");
        Torneo t = club().getTorneos().get(id);
        if (t == null) {
            throw new IllegalArgumentException("El torneo no existe.");
        }
        return t;
    }

    private Torneo torneoEnCurso() {
        Torneo t = torneoSeleccionado();
        if (!controlador.torneoEnCurso(t)) {
            throw new IllegalArgumentException("El torneo ya ha finalizado.");
        }
        return t;
    }

    private TorneoIndividual torneoIndividualEnCurso() {
        Torneo t = torneoEnCurso();
        if (!(t instanceof TorneoIndividual)) {
            throw new IllegalArgumentException("Seleccione un torneo individual.");
        }
        return (TorneoIndividual) t;
    }

    // ---------- administrador ----------

    private void crearTorneo(boolean individual) {
        Formulario f = new Formulario(individual ? "Crear torneo individual" : "Crear torneo de equipos")
                .texto("nombre", "Nombre del torneo")
                .combo("deporte", "Deporte", nombresDeportes(individual))
                .combo("cat", "Categoría (edad máxima, 99 = Mayores)", Arrays.asList(UtilGUI.CATEGORIAS))
                .entero("cupos", "Número de cupos totales", 8, 2, 1000)
                .combo("modalidad", "Modalidad", Arrays.asList("Recreativo (sin costo, premia con bono)", "Competitivo (con tarifa, premia en efectivo)"))
                .decimal("tarifa", "Tarifa de inscripción (solo competitivo)", 0, 0, 1e9, 1000);
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("nombre", "deporte");
        boolean competitivo = f.valor("modalidad").startsWith("Competitivo");
        double tarifa = f.decimal("tarifa");
        if (competitivo && tarifa <= 0) {
            throw new IllegalArgumentException("Un torneo competitivo necesita una tarifa mayor a cero.");
        }
        String id = controlador.generarIdTorneo();
        String idAdmin = usuario().getIdUsuario();
        String nombre = f.valor("nombre");
        String deporte = f.valor("deporte");
        int cat = categoria(f.valor("cat"));
        int cupos = f.entero("cupos");
        if (competitivo) {
            if (individual) {
                club().crearTorneoIndividualCompetitivo(idAdmin, id, nombre, deporte, cat, cupos, tarifa);
            } else {
                club().crearTorneoConjuntoCompetitivo(idAdmin, id, nombre, deporte, cat, cupos, tarifa);
            }
        } else if (individual) {
            club().crearTorneoIndividual(idAdmin, id, nombre, deporte, cat, cupos);
        } else {
            club().crearTorneoConjunto(idAdmin, id, nombre, deporte, cat, cupos);
        }
        guardar();
        refrescar();
        info("Torneo " + (competitivo ? "competitivo" : "recreativo") + " creado (ID: " + id + "), en curso.");
    }

    private void registrarPartido() {
        Torneo torneo = torneoEnCurso();
        String idTorneo = torneo.getIdTorneo();
        String idPartido;
        if (torneo instanceof TorneoConjunto) {
            Formulario f = new Formulario("Partido del torneo " + idTorneo)
                    .combo("e1", "Equipo 1", nombresEquipos())
                    .combo("e2", "Equipo 2", nombresEquipos())
                    .fecha("fecha", "Fecha")
                    .entero("m1", "Marcador del equipo 1", 0, 0, 500)
                    .entero("m2", "Marcador del equipo 2", 0, 0, 500);
            if (!f.mostrar(this)) {
                return;
            }
            f.requerir("e1", "e2");
            if (f.valor("e1").equals(f.valor("e2"))) {
                throw new IllegalArgumentException("Los equipos deben ser distintos.");
            }
            idPartido = controlador.generarIdPartido();
            club().registrarPartidoTorneoConjunto(idTorneo, idPartido, f.fechaValor("fecha"), f.valor("e1"), f.valor("e2"),
                    f.entero("m1") + "-" + f.entero("m2"));
        } else {
            List<String> jugadores = new ArrayList<>();
            for (Usuario u : club().getUsuarios().values()) {
                if (!(u instanceof Administrador)) {
                    jugadores.add(opcion(u));
                }
            }
            Formulario f = new Formulario("Partido del torneo " + idTorneo)
                    .combo("j1", "Jugador 1", jugadores)
                    .combo("j2", "Jugador 2", jugadores)
                    .fecha("fecha", "Fecha")
                    .entero("s1", "Sets ganados por el jugador 1", 0, 0, 10)
                    .entero("s2", "Sets ganados por el jugador 2", 0, 0, 10);
            if (!f.mostrar(this)) {
                return;
            }
            f.requerir("j1", "j2");
            if (f.valor("j1").equals(f.valor("j2"))) {
                throw new IllegalArgumentException("Los jugadores deben ser distintos.");
            }
            idPartido = controlador.generarIdPartido();
            club().registrarPartidoTorneoIndividual(idTorneo, idPartido, f.fechaValor("fecha"), idDe(f.valor("j1")),
                    idDe(f.valor("j2")), f.entero("s1"), f.entero("s2"));
        }
        guardar();
        refrescar();
        info("Partido registrado (ID: " + idPartido + "). La tabla de posiciones se actualizó.");
    }

    private void finalizar() {
        Torneo seleccionado = torneoSeleccionado();
        String id = seleccionado.getIdTorneo();
        if (!UtilGUI.confirmar(this, "¿Finalizar el torneo " + seleccionado.getNombre() + "?")) {
            return;
        }
        club().finalizarTorneo(usuario().getIdUsuario(), id, tienda());
        guardar();
        refrescar();

        Torneo torneo = club().getTorneos().get(id);
        StringBuilder sb = new StringBuilder("El torneo quedó en estado 'Finalizado'.\n");
        String primero = torneo.obtenerPrimerLugar();
        if (primero == null) {
            sb.append("No hubo participantes: no se otorgó ningún premio.");
        } else {
            sb.append("Primer lugar: ").append(controlador.nombreParticipante(primero)).append("\n");
            if (torneo.isCompetitivo()) {
                if (torneo.getGanadorPremio() == null) {
                    sb.append("El primer lugar es un empleado: no recibe el premio en efectivo.");
                } else {
                    sb.append("Premio en efectivo: $").append(Math.round(torneo.getPremio()))
                            .append(" (").append(Math.round(Torneo.PORCENTAJE_PREMIO * 100)).append("% de $")
                            .append(Math.round(torneo.getTotalRecaudado())).append(" recaudados).");
                }
            } else {
                sb.append("Si el ganador es socio, su bono ya quedó disponible para la tienda deportiva.");
            }
        }
        info(sb.toString());
    }

    private void inscribirInvitado() {
        TorneoIndividual torneo = torneoIndividualEnCurso();
        String idTorneo = torneo.getIdTorneo();
        Formulario f = new Formulario("Inscribir un invitado en " + idTorneo)
                .texto("id", "ID del invitado existente (vacío para crear uno nuevo)")
                .texto("nombre", "Nombre (solo si es nuevo)")
                .texto("apellido", "Apellido (solo si es nuevo)")
                .entero("edad", "Edad (solo si es nuevo)", 18, 5, 100);
        if (!f.mostrar(this)) {
            return;
        }
        String idInvitado = f.valor("id");
        boolean nuevo = idInvitado.isEmpty();
        if (nuevo) {
            f.requerir("nombre", "apellido");
            idInvitado = controlador.generarIdUsuario();
            club().crearInvitado(idInvitado, f.valor("nombre"), f.valor("apellido"), f.entero("edad"));
        }
        try {
            club().inscribirInvitadoTorneoIndividual(usuario().getIdUsuario(), idInvitado, idTorneo);
        } catch (IllegalArgumentException e) {
            if (nuevo) {
                club().getUsuarios().remove(idInvitado);
            }
            throw e;
        }
        guardar();
        refrescar();
        String mensaje = "Invitado inscrito con un cupo general (ID del invitado: " + idInvitado + ").";
        if (torneo.isCompetitivo()) {
            mensaje += "\nSe registró el cobro de la tarifa (el pago lo gestiona un tercero).";
        }
        info(mensaje);
    }

    private void retirarInvitado() {
        TorneoIndividual torneo = torneoIndividualEnCurso();
        Formulario f = new Formulario("Retirar un invitado de " + torneo.getIdTorneo()).texto("id", "ID del invitado");
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("id");
        club().desinscribirParticipanteTorneoIndividual(f.valor("id"), torneo.getIdTorneo());
        guardar();
        refrescar();
        info("Se eliminó la inscripción y se liberó el cupo.");
    }

    // ---------- socio ----------

    private void inscribirSocio() {
        TorneoIndividual torneo = torneoIndividualEnCurso();
        String idUsuario = usuario().getIdUsuario();
        club().inscribirSocioTorneoIndividual(idUsuario, torneo.getIdTorneo());
        guardar();
        refrescar();
        String mensaje = torneo.usoCupoPreferencial(idUsuario)
                ? "Inscripción registrada con un cupo preferencial (por su ranking)."
                : "Inscripción registrada con un cupo general.";
        if (torneo.isCompetitivo()) {
            mensaje += "\nSe registró el cobro de la tarifa: $" + Math.round(torneo.getTarifaInscripcion())
                    + " (el pago lo gestiona un tercero).";
        }
        info(mensaje);
    }

    private void salirSocio() {
        TorneoIndividual torneo = torneoIndividualEnCurso();
        club().desinscribirSocioTorneoIndividual(usuario().getIdUsuario(), torneo.getIdTorneo());
        guardar();
        refrescar();
        String mensaje = "Se eliminó su inscripción y se liberó el cupo.";
        if (torneo.isCompetitivo()) {
            mensaje += "\nSe descontó su tarifa del total recaudado.";
        }
        info(mensaje);
    }

    private void verBonos() {
        Socio socio = (Socio) usuario();
        if (!socio.tieneBonosDisponibles()) {
            info("No tiene bonos de descuento por redimir.");
            return;
        }
        int porcentaje = (int) Math.round(CalculadoraDescuento.DESCUENTO_BONO_TORNEO * 100);
        StringBuilder sb = new StringBuilder();
        for (String bono : socio.getBonosDescuento()) {
            sb.append("- ").append(bono).append(" (").append(porcentaje).append("% de descuento en la tienda deportiva)\n");
        }
        sb.append("\nUn bono se aplica durante una compra (módulo Tienda) y no se acumula con otros descuentos.");
        UtilGUI.mostrarTexto(this, "Mis bonos de descuento", sb.toString());
    }

    // ---------- entrenador / fisioterapeuta ----------

    private void inscribirEquipo() {
        Torneo torneo = torneoEnCurso();
        if (!(torneo instanceof TorneoConjunto)) {
            throw new IllegalArgumentException("Seleccione un torneo de equipos.");
        }
        Formulario f = new Formulario("Inscribir un equipo en " + torneo.getIdTorneo()).combo("equipo", "Equipo", nombresEquipos());
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("equipo");
        String nombreEquipo = f.valor("equipo");
        club().inscribirEquipoTorneoConjunto(usuario().getIdUsuario(), torneo.getIdTorneo(), nombreEquipo);
        guardar();
        refrescar();
        TorneoConjunto tc = (TorneoConjunto) club().getTorneos().get(torneo.getIdTorneo());
        String mensaje = tc.getEquiposCupoPreferencial().contains(nombreEquipo)
                ? "Equipo inscrito con un cupo preferencial (por su desempeño)."
                : "Equipo inscrito con un cupo general.";
        if (tc.isCompetitivo()) {
            mensaje += "\nSe registró el cobro de la tarifa del equipo: $" + Math.round(tc.getTarifaInscripcion())
                    + " (el pago lo gestiona un tercero).";
        }
        info(mensaje);
    }

    private void inscribirEmpleado() {
        TorneoIndividual torneo = torneoIndividualEnCurso();
        club().inscribirEmpleadoTorneoIndividual(usuario().getIdUsuario(), torneo.getIdTorneo());
        guardar();
        refrescar();
        String mensaje = "Inscripción registrada con un cupo general.";
        if (torneo.isCompetitivo()) {
            mensaje += "\nInscripción gratuita para empleados. Si queda en el primer lugar, no recibe el premio en efectivo.";
        }
        info(mensaje);
    }

    private void salirEmpleado() {
        TorneoIndividual torneo = torneoIndividualEnCurso();
        club().desinscribirParticipanteTorneoIndividual(usuario().getIdUsuario(), torneo.getIdTorneo());
        guardar();
        refrescar();
        info("Se eliminó su inscripción y se liberó el cupo.");
    }

    // ---------- compartido ----------

    private void historial() {
        Torneo torneo = torneoSeleccionado();
        String texto = club().consultarHistorialTorneo(usuario().getIdUsuario(), torneo.getIdTorneo());
        UtilGUI.mostrarTexto(this, "Historial en " + torneo.getNombre(), texto);
    }
}
