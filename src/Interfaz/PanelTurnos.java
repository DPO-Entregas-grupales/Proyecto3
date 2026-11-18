package Interfaz;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JPanel;
import Turnos.Turno;
import Usuarios.Administrador;
import Usuarios.Empleado;
import Usuarios.Usuario;

/**
 * Turnos. El administrador crea turnos y asigna empleados; el entrenador y el fisioterapeuta
 * ven sus turnos y solicitan cambios.
 */
public class PanelTurnos extends PanelBase {

    private final boolean esAdmin;
    private final PanelTabla tClub = new PanelTabla("Turnos del club", "ID", "Día", "Jornada", "Entrenadores", "Fisioterapeutas");
    private final PanelTabla tMios = new PanelTabla("Mis turnos", "ID", "Día", "Jornada", "Entrenadores", "Fisioterapeutas");

    public PanelTurnos(ControladorGUI controlador) {
        super(controlador);
        esAdmin = usuario() instanceof Administrador;
        if (esAdmin) {
            add(tClub, BorderLayout.CENTER);
            add(UtilGUI.barra(
                    UtilGUI.boton("Crear turno", this, this::crearTurno),
                    UtilGUI.boton("Asignar empleado al turno seleccionado", this, this::asignarEmpleado)), BorderLayout.SOUTH);
        } else {
            JPanel centro = new JPanel(new GridLayout(2, 1, 8, 8));
            centro.add(tMios);
            centro.add(tClub);
            add(centro, BorderLayout.CENTER);
            add(UtilGUI.barra(UtilGUI.boton("Solicitar cambio: dejar el turno seleccionado en \"Mis turnos\" "
                    + "y pasar al seleccionado en \"Turnos del club\"", this, this::solicitarCambio)), BorderLayout.SOUTH);
        }
        refrescar();
    }

    private void llenar(PanelTabla tabla, Iterable<Turno> turnos) {
        tabla.limpiar();
        for (Turno t : turnos) {
            tabla.agregar(t.getIdTurno(), t.getDiaSemana(), t.isEsDiurno() ? "Diurno" : "Nocturno",
                    t.getEntrenadores().size(), t.getFisioterapeutas().size());
        }
    }

    @Override
    protected void refrescar() {
        llenar(tClub, club().getTurnos().values());
        if (!esAdmin && usuario() instanceof Empleado) {
            llenar(tMios, ((Empleado) usuario()).getTurnos().values());
        }
    }

    private void crearTurno() {
        Formulario f = new Formulario("Crear turno")
                .texto("id", "ID del turno")
                .entero("dia", "Día de la semana (1-7)", 1, 1, 7)
                .casilla("diurno", "¿Turno diurno?");
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("id");
        if (club().getTurnos().containsKey(f.valor("id"))) {
            throw new IllegalArgumentException("Ya existe un turno con ese ID.");
        }
        club().crearTurno(f.valor("id"), f.entero("dia"), f.marcado("diurno"));
        guardar();
        refrescar();
        info("Turno creado.");
    }

    private void asignarEmpleado() {
        String idTurno = tClub.requerirSeleccion(0, "Seleccione un turno de la tabla.");
        Formulario f = new Formulario("Asignar empleado al turno " + idTurno)
                .combo("empleado", "Empleado", opcionesUsuarios(Empleado.class));
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("empleado");
        String idEmpleado = idDe(f.valor("empleado"));
        Usuario u = club().getUsuarios().get(idEmpleado);
        if (!(u instanceof Empleado)) {
            throw new IllegalArgumentException("El empleado no existe.");
        }
        if (((Empleado) u).getTurnos().containsKey(idTurno)) {
            throw new IllegalArgumentException("El empleado ya tiene ese turno.");
        }
        club().asignarTurnoAEmpleado(idTurno, idEmpleado);
        guardar();
        refrescar();
        info("Turno asignado.");
    }

    private void solicitarCambio() {
        String actual = tMios.requerirSeleccion(0, "Seleccione en \"Mis turnos\" el turno que deja.");
        String nuevo = tClub.requerirSeleccion(0, "Seleccione en \"Turnos del club\" el turno al que pasa.");
        Empleado empleado = (Empleado) usuario();
        boolean aprobada = club().realizarSolicitudCambioTurno(empleado.getIdUsuario(), actual, nuevo);
        guardar();
        refrescar();
        if (aprobada) {
            info("Solicitud aprobada: ahora tiene el turno " + nuevo + ".");
        } else {
            info("Solicitud rechazada: el turno que deja quedaría sin el mínimo de 2 entrenadores y 1 fisioterapeuta.");
        }
    }
}
