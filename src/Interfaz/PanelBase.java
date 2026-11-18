package Interfaz;

import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import Deportes.Deporte;
import Modelo.ClubDeportivoSeneca;
import Modelo.Tienda;
import Usuarios.Socio;
import Usuarios.Usuario;

/**
 * Base de todos los paneles de módulo. Cada vez que el panel se hace visible
 * (por ejemplo, al cambiar de módulo en el menú) se llama a refrescar() para que
 * las tablas siempre muestren los datos actuales del club.
 */
public abstract class PanelBase extends JPanel {

    protected final ControladorGUI controlador;

    protected PanelBase(ControladorGUI controlador) {
        this.controlador = controlador;
        setLayout(new BorderLayout(10, 10));
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refrescar();
            }
        });
    }

    /** Vuelve a llenar las tablas con los datos actuales. */
    protected abstract void refrescar();

    protected ClubDeportivoSeneca club() {
        return controlador.getClub();
    }

    protected Tienda tienda() {
        return controlador.getTienda();
    }

    protected Usuario usuario() {
        return controlador.getUsuarioAutenticado();
    }

    protected void guardar() {
        controlador.guardarTodo();
    }

    protected void info(String mensaje) {
        UtilGUI.info(this, mensaje);
    }

    // ---------- listas de opciones para los combos ----------

    protected List<String> nombresEquipos() {
        return new ArrayList<>(club().getEquipos().keySet());
    }

    protected List<String> nombresCompetencias() {
        return new ArrayList<>(club().getCompetencias().keySet());
    }

    protected List<String> nombresInstalaciones() {
        return new ArrayList<>(club().getInstalaciones().keySet());
    }

    /** Nombres de deportes. individual = null para todos, true solo individuales, false solo de conjunto. */
    protected List<String> nombresDeportes(Boolean individual) {
        List<String> r = new ArrayList<>();
        for (Deporte d : club().getDeportes().values()) {
            if (individual == null || d.esIndividual() == individual) {
                r.add(d.getNombreDeporte());
            }
        }
        return r;
    }

    /** Opciones "id - nombre apellido" de los usuarios que sean del tipo dado. */
    protected List<String> opcionesUsuarios(Class<? extends Usuario> tipo) {
        List<String> r = new ArrayList<>();
        for (Usuario u : club().getUsuarios().values()) {
            if (tipo.isInstance(u)) {
                r.add(opcion(u));
            }
        }
        return r;
    }

    protected String opcion(Usuario u) {
        return u.getIdUsuario() + " - " + u.getNombre() + " " + u.getApellido();
    }

    /** Extrae el id de una opción "id - nombre apellido". */
    protected String idDe(String opcion) {
        return opcion.split(" - ")[0].trim();
    }

    protected List<Socio> sociosDe(List<String> opciones) {
        List<Socio> r = new ArrayList<>();
        for (String o : opciones) {
            Usuario u = club().getUsuarios().get(idDe(o));
            if (u instanceof Socio) {
                r.add((Socio) u);
            }
        }
        return r;
    }

    /** Orden natural de identificadores como "15", "ENT-12" o "TOR-3": por el número con el que terminan. */
    protected static int numeroDe(String id) {
        if (id == null) {
            return 0;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s*$").matcher(id);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    protected static int categoria(String texto) {
        return Integer.parseInt(texto.trim());
    }
}
