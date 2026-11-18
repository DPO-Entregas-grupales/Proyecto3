package Interfaz;

import java.awt.BorderLayout;
import java.util.ArrayList;
import Usuarios.Socio;
import Usuarios.Usuario;

/** Gestión de usuarios: registrar socios, entrenadores, fisioterapeutas y administradores, y dar de baja socios. */
public class PanelAdminUsuarios extends PanelBase {

    private final PanelTabla tabla = new PanelTabla("Usuarios Registrados", "ID", "Nombre", "Apellido", "Rol", "Puntos", "Estado");

    public PanelAdminUsuarios(ControladorGUI controlador) {
        super(controlador);
        add(tabla, BorderLayout.CENTER);
        add(UtilGUI.barra(
                UtilGUI.boton("Registrar Socio", this, () -> registrar("Socio")),
                UtilGUI.boton("Registrar Entrenador", this, () -> registrar("Entrenador")),
                UtilGUI.boton("Registrar Fisioterapeuta", this, () -> registrar("Fisioterapeuta")),
                UtilGUI.boton("Registrar Administrador", this, () -> registrar("Administrador")),
                UtilGUI.boton("Dar de baja socio seleccionado", this, this::darDeBaja)), BorderLayout.SOUTH);
        refrescar();
    }

    @Override
    protected void refrescar() {
        tabla.limpiar();
        for (Usuario u : club().getUsuarios().values()) {
            String estado = "Activo";
            if (u instanceof Socio && !((Socio) u).isActivo()) {
                estado = "De baja";
            }
            tabla.agregar(u.getIdUsuario(), u.getNombre(), u.getApellido(), u.getClass().getSimpleName(),
                    u.getPuntosFidelidad(), estado);
        }
    }

    private void registrar(String rol) {
        Formulario f = new Formulario("Registrar " + rol)
                .texto("nombre", "Nombre")
                .texto("apellido", "Apellido")
                .texto("login", "Login")
                .clave("pass", "Contraseña");
        boolean esSocio = rol.equals("Socio");
        boolean esEmpleado = rol.equals("Entrenador") || rol.equals("Fisioterapeuta");
        if (esSocio) {
            f.entero("edad", "Edad", 18, 5, 100);
        } else {
            f.entero("puntos", "Puntos iniciales", 0, 0, 100000);
        }
        if (esEmpleado) {
            f.texto("codigo", "Código de descuento");
        }
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("nombre", "apellido", "login", "pass");
        if (esEmpleado) {
            f.requerir("codigo");
        }

        String login = f.valor("login");
        if (!controlador.getAutentificador().esLoginDisponible(login, new ArrayList<>(club().getUsuarios().values()))) {
            throw new IllegalArgumentException("Ese login ya está en uso.");
        }

        String id = controlador.generarIdUsuario();
        String nombre = f.valor("nombre");
        String apellido = f.valor("apellido");
        String pass = f.valor("pass");
        switch (rol) {
            case "Socio" -> club().crearSocio(id, nombre, apellido, login, pass, 0, f.entero("edad"), true);
            case "Entrenador" -> {
                club().crearEntrenador(id, nombre, apellido, login, pass, f.entero("puntos"), f.valor("codigo"));
                controlador.registrarCodigoDescuento(f.valor("codigo"));
            }
            case "Fisioterapeuta" -> {
                club().crearFisioterapeuta(id, nombre, apellido, login, pass, f.entero("puntos"), f.valor("codigo"));
                controlador.registrarCodigoDescuento(f.valor("codigo"));
            }
            default -> club().crearAdministrador(id, nombre, apellido, login, pass, f.entero("puntos"));
        }
        guardar();
        refrescar();
        info(rol + " registrado con ID: " + id);
    }

    private void darDeBaja() {
        String id = tabla.requerirSeleccion(0, "Seleccione un usuario de la tabla.");
        if (!UtilGUI.confirmar(this, "¿Dar de baja al socio con ID " + id + "?")) {
            return;
        }
        club().realizarBajaJugador(id);
        guardar();
        refrescar();
        info("Socio dado de baja.");
    }
}
