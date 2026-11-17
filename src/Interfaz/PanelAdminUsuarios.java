package Interfaz;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import Usuarios.Socio;
import Usuarios.Usuario;

public class PanelAdminUsuarios extends JPanel {

    private final ControladorGUI controlador;
    private DefaultTableModel modeloTabla;
    private JTable tablaUsuarios;

    public PanelAdminUsuarios(ControladorGUI controlador) {
        this.controlador = controlador;
        inicializarComponentes();
        cargarUsuarios();
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout(10, 10));

        modeloTabla = new DefaultTableModel(new String[]{"ID", "Nombre", "Apellido", "Rol", "Puntos", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaUsuarios = new JTable(modeloTabla);

        JScrollPane scroll = new JScrollPane(tablaUsuarios);
        scroll.setBorder(BorderFactory.createTitledBorder("Usuarios Registrados"));
        add(scroll, BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnNuevoSocio = new JButton("Registrar Socio");
        btnNuevoSocio.addActionListener(e -> mostrarDialogoNuevoSocio());

        JButton btnBajaSocio = new JButton("Dar de Baja Socio Seleccionado");
        btnBajaSocio.addActionListener(e -> darDeBajaSeleccionado());

        panelBotones.add(btnNuevoSocio);
        panelBotones.add(btnBajaSocio);
        add(panelBotones, BorderLayout.SOUTH);
    }

    private void cargarUsuarios() {
        modeloTabla.setRowCount(0);
        for (Usuario u : controlador.getClub().getUsuarios().values()) {
            String estado = "Activo";
            if (u instanceof Socio && !((Socio) u).isActivo()) {
                estado = "De baja";
            }
            modeloTabla.addRow(new Object[]{
                u.getIdUsuario(),
                u.getNombre(),
                u.getApellido(),
                u.getClass().getSimpleName(),
                u.getPuntosFidelidad(),
                estado
            });
        }
    }

    private void mostrarDialogoNuevoSocio() {
        JTextField txtNombre = new JTextField();
        JTextField txtApellido = new JTextField();
        JTextField txtLogin = new JTextField();
        JPasswordField txtPass = new JPasswordField();
        JSpinner spnEdad = new JSpinner(new SpinnerNumberModel(18, 5, 100, 1));

        JPanel formulario = new JPanel(new GridLayout(5, 2, 5, 5));
        formulario.add(new JLabel("Nombre:"));
        formulario.add(txtNombre);
        formulario.add(new JLabel("Apellido:"));
        formulario.add(txtApellido);
        formulario.add(new JLabel("Login:"));
        formulario.add(txtLogin);
        formulario.add(new JLabel("Contraseña:"));
        formulario.add(txtPass);
        formulario.add(new JLabel("Edad:"));
        formulario.add(spnEdad);

        int opcion = JOptionPane.showConfirmDialog(this, formulario, "Registrar Nuevo Socio", JOptionPane.OK_CANCEL_OPTION);
        if (opcion == JOptionPane.OK_OPTION) {
            String login = txtLogin.getText().trim();
            boolean disponible = controlador.getAutentificador().esLoginDisponible(
                    login, new java.util.ArrayList<>(controlador.getClub().getUsuarios().values()));

            if (!disponible) {
                JOptionPane.showMessageDialog(this, "Ese login ya está en uso.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String id = controlador.generarIdUsuario();
            controlador.getClub().crearSocio(
                    id,
                    txtNombre.getText().trim(),
                    txtApellido.getText().trim(),
                    login,
                    new String(txtPass.getPassword()).trim(),
                    0,
                    (int) spnEdad.getValue(),
                    true
            );
            controlador.guardarTodo();
            cargarUsuarios();
            JOptionPane.showMessageDialog(this, "Socio registrado con ID: " + id);
        }
    }

    private void darDeBajaSeleccionado() {
        int fila = tablaUsuarios.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un usuario de la tabla.");
            return;
        }
        String id = (String) modeloTabla.getValueAt(fila, 0);
        try {
            controlador.getClub().realizarBajaJugador(id);
            controlador.guardarTodo();
            cargarUsuarios();
            JOptionPane.showMessageDialog(this, "Socio dado de baja.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
