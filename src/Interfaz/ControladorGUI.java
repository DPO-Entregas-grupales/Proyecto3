package Interfaz;

import java.util.ArrayList;
import java.util.List;
import Modelo.Autentificador;
import Modelo.ClubDeportivoSeneca;
import Modelo.Tienda;
import Partidos.Torneo;
import Usuarios.Empleado;
import Usuarios.Invitado;
import Usuarios.Socio;
import Usuarios.Usuario;

public class ControladorGUI {

    private static final String RUTA_CLUB = "./datos/ClubDeportivo.json";
    private static final String RUTA_TIENDA = "./datos/Tienda.json";
    private static final String RUTA_USUARIOS = "./datos/Usuarios.json";

    private final ClubDeportivoSeneca club;
    private final Tienda tienda;
    private final Autentificador autentificador;
    private Usuario usuarioAutenticado;

    private int contadorUsuarios = 1;
    private int contadorReservas = 1;
    private int contadorPartidos = 1;

    public ControladorGUI() {
        this.club = new ClubDeportivoSeneca();
        this.tienda = new Tienda();
        this.autentificador = new Autentificador();
        cargarDatos();
    }

    private void cargarDatos() {
        club.cargarUsuarios(RUTA_USUARIOS);
        club.cargarClubDeportivo(RUTA_CLUB);
        tienda.cargarTienda(RUTA_TIENDA, club);
        registrarCodigosDescuento();

        contadorUsuarios = obtenerMaxId(club.getUsuarios().keySet()) + 1;
        contadorReservas = obtenerMaxId(club.getReservas().keySet()) + 1;
        contadorPartidos = obtenerMaxId(club.getPartidos().keySet()) + 1;
    }

    private void registrarCodigosDescuento() {
        for (Usuario u : club.getUsuarios().values()) {
            if (u instanceof Empleado) {
                tienda.getCalculadoraDescuento().agregarCodigoValido(((Empleado) u).getCodigoDescuento());
            }
            if (u instanceof Socio) {
                for (String bono : ((Socio) u).getBonosDescuento()) {
                    tienda.registrarBonoTorneo(bono);
                }
            }
        }
    }

    /** Registra el código de descuento de un empleado recién creado. */
    public void registrarCodigoDescuento(String codigo) {
        tienda.getCalculadoraDescuento().agregarCodigoValido(codigo);
    }

    public void guardarTodo() {
        club.salvarUsuarios(RUTA_USUARIOS);
        club.salvarClubDeportivo(RUTA_CLUB);
        tienda.salvarTienda(RUTA_TIENDA);
    }

    public Usuario iniciarSesion(String login, String password) {
        if (club.getUsuarios().isEmpty()) {
            throw new IllegalStateException("No hay usuarios registrados en " + RUTA_USUARIOS);
        }
        List<Usuario> listaUsuarios = new ArrayList<>(club.getUsuarios().values());
        Usuario u = autentificador.autenticar(login, password, listaUsuarios);

        if (u == null) {
            throw new IllegalArgumentException("Login o contraseña incorrectos.");
        }
        if (u instanceof Invitado) {
            throw new IllegalArgumentException("Los invitados no tienen acceso al sistema.");
        }
        if (u instanceof Socio && !((Socio) u).isActivo()) {
            throw new IllegalArgumentException("Su cuenta está dada de baja.");
        }

        this.usuarioAutenticado = u;
        return u;
    }

    public void cerrarSesion() {
        this.usuarioAutenticado = null;
    }

    public String generarIdUsuario() {
        return String.valueOf(contadorUsuarios++);
    }

    public String generarIdReserva() {
        return String.valueOf(contadorReservas++);
    }

    public String generarIdPartido() {
        return String.valueOf(contadorPartidos++);
    }

    /** Siguiente ID de torneo con el formato TOR-n. */
    public String generarIdTorneo() {
        int max = 0;
        for (String id : club.getTorneos().keySet()) {
            String digitos = id.replaceAll("[^0-9]", "");
            if (!digitos.isEmpty()) {
                max = Math.max(max, Integer.parseInt(digitos));
            }
        }
        return "TOR-" + (max + 1);
    }

    /** Nombre legible de un participante de torneo (si es un usuario) o la clave tal cual (si es un equipo). */
    public String nombreParticipante(String clave) {
        Usuario usuario = club.getUsuarios().get(clave);
        if (usuario != null) {
            return usuario.getNombre() + " " + usuario.getApellido();
        }
        return clave;
    }

    public boolean torneoEnCurso(Torneo t) {
        return Torneo.EN_CURSO.equalsIgnoreCase(t.getEstado());
    }

    private int obtenerMaxId(Iterable<String> llaves) {
        int max = 0;
        if (llaves != null) {
            for (String key : llaves) {
                try {
                    int idNum = Integer.parseInt(key);
                    if (idNum > max) {
                        max = idNum;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return max;
    }

    public ClubDeportivoSeneca getClub() {
        return club;
    }

    public Tienda getTienda() {
        return tienda;
    }

    public Autentificador getAutentificador() {
        return autentificador;
    }

    public Usuario getUsuarioAutenticado() {
        return usuarioAutenticado;
    }
}
