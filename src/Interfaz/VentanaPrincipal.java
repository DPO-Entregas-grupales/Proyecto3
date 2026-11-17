package Interfaz;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import Usuarios.Administrador;
import Usuarios.Entrenador;
import Usuarios.Fisioterapeuta;
import Usuarios.Socio;
import Usuarios.Usuario;

public class VentanaPrincipal extends JFrame {

    private final ControladorGUI controlador;
    private JLabel lblPuntos;
    private JPanel panelMenuLateral;
    private JPanel panelContenedor;
    private CardLayout cardLayout;

    public VentanaPrincipal(ControladorGUI controlador) {
        this.controlador = controlador;
        configurarVentana();
        inicializarComponentes();
        cargarModulosPorRol();
    }

    private void configurarVentana() {
        setTitle("Club Deportivo Séneca - Sistema de Gestión");
        setSize(1050, 680);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controlador.guardarTodo();
                System.exit(0);
            }
        });
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout());

        Usuario u = controlador.getUsuarioAutenticado();
        String rol = obtenerNombreRol(u);

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBackground(new Color(30, 41, 59));
        panelSuperior.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel lblUsuario = new JLabel("Sesión: " + u.getNombre() + " " + u.getApellido() + "  |  Rol: " + rol);
        lblUsuario.setForeground(Color.WHITE);
        lblUsuario.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel panelDerechaSuperior = new JPanel();
        panelDerechaSuperior.setOpaque(false);

        lblPuntos = new JLabel("Puntos de fidelidad: " + u.getPuntosFidelidad() + "   ");
        lblPuntos.setForeground(new Color(250, 204, 21));
        lblPuntos.setFont(new Font("SansSerif", Font.BOLD, 13));

        JButton btnCerrarSesion = new JButton("Cerrar Sesión");
        btnCerrarSesion.addActionListener(e -> cerrarSesion());

        panelDerechaSuperior.add(lblPuntos);
        panelDerechaSuperior.add(btnCerrarSesion);

        panelSuperior.add(lblUsuario, BorderLayout.WEST);
        panelSuperior.add(panelDerechaSuperior, BorderLayout.EAST);
        add(panelSuperior, BorderLayout.NORTH);

        panelMenuLateral = new JPanel(new GridLayout(12, 1, 5, 6));
        panelMenuLateral.setPreferredSize(new Dimension(240, 0));
        panelMenuLateral.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        panelMenuLateral.setBackground(new Color(241, 245, 249));
        add(panelMenuLateral, BorderLayout.WEST);

        cardLayout = new CardLayout();
        panelContenedor = new JPanel(cardLayout);
        panelContenedor.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        add(panelContenedor, BorderLayout.CENTER);
    }

    private void cargarModulosPorRol() {
        Usuario u = controlador.getUsuarioAutenticado();

        if (u instanceof Administrador) {
            agregarModulo("USUARIOS", "Gestión de Usuarios", new PanelAdminUsuarios(controlador));
            agregarModulo("DEPORTES", "Deportes y Equipos", crearPanelEnConstruccion("Deportes, Equipos y Competencias"));
            agregarModulo("INSTALACIONES", "Instalaciones y Turnos", crearPanelEnConstruccion("Instalaciones, Horarios y Turnos"));
            agregarModulo("INVENTARIO", "Inventario Tienda/Cafetería", crearPanelEnConstruccion("Gestión de Tienda y Cafetería"));
            agregarModulo("TORNEOS_ADMIN", "Partidos y Torneos", crearPanelEnConstruccion("Programar Partidos y Gestionar Torneos"));
            agregarModulo("REPORTES", "Reportes y Consultas", crearPanelEnConstruccion("Reportes del Club y Ventas"));
        } else if (u instanceof Entrenador) {
            agregarModulo("ENTRENADOR", "Partidos y Entrenamientos", crearPanelEnConstruccion("Marcadores, Estadísticas y Asistencia"));
            agregarModulo("TURNOS", "Cambio de Turno", crearPanelEnConstruccion("Solicitud de Cambio de Turno"));
            agregarModulo("TORNEOS", "Torneos", crearPanelEnConstruccion("Inscripción de Equipos e Individual en Torneos"));
        } else if (u instanceof Fisioterapeuta) {
            agregarModulo("TURNOS", "Cambio de Turno", crearPanelEnConstruccion("Solicitud de Cambio de Turno"));
            agregarModulo("TORNEOS", "Torneos", crearPanelEnConstruccion("Torneos Individuales e Historial"));
        } else if (u instanceof Socio) {
            agregarModulo("DEPORTES_SOCIO", "Equipos y Ranking", crearPanelEnConstruccion("Inscripción a Equipos / Individual y Ranking"));
            agregarModulo("PARTIDOS_SOCIO", "Partidos Individuales", crearPanelEnConstruccion("Programar y Registrar Partidos Individuales"));
            agregarModulo("RESERVAS", "Reservar Instalación", crearPanelEnConstruccion("Reservas e Ingreso a Instalaciones"));
            agregarModulo("TORNEOS", "Torneos y Bonos", crearPanelEnConstruccion("Torneos Individuales, Historial y Bonos"));
        }

        agregarModulo("TIENDA", "Tienda y Cafetería", new PanelTiendaCafeteria(controlador, this));
    }

    private void agregarModulo(String claveCard, String textoBoton, JPanel panel) {
        panelContenedor.add(panel, claveCard);
        JButton btn = new JButton(textoBoton);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.addActionListener(e -> cardLayout.show(panelContenedor, claveCard));
        panelMenuLateral.add(btn);
    }

    public void actualizarPuntosEncabezado() {
        Usuario u = controlador.getUsuarioAutenticado();
        if (u != null) {
            lblPuntos.setText("Puntos de fidelidad: " + u.getPuntosFidelidad() + "   ");
        }
    }

    private void cerrarSesion() {
        controlador.guardarTodo();
        controlador.cerrarSesion();
        new VentanaLogin(controlador).setVisible(true);
        this.dispose();
    }

    private String obtenerNombreRol(Usuario u) {
        if (u instanceof Administrador) return "Administrador";
        if (u instanceof Entrenador) return "Entrenador";
        if (u instanceof Fisioterapeuta) return "Fisioterapeuta";
        return "Socio";
    }

    private JPanel crearPanelEnConstruccion(String titulo) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel("Módulo: " + titulo + " (Reemplazar por su JPanel)", SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 16));
        p.add(lbl, BorderLayout.CENTER);
        return p;
    }
}
