package Interfaz;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class VentanaLogin extends JFrame {

    private final ControladorGUI controlador;
    private JTextField txtLogin;
    private JPasswordField txtPassword;

    public VentanaLogin(ControladorGUI controlador) {
        this.controlador = controlador;
        configurarVentana();
        inicializarComponentes();
    }

    private void configurarVentana() {
        setTitle("Club Deportivo Séneca - Inicio de Sesión");
        setSize(420, 270);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void inicializarComponentes() {
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel lblTitulo = new JLabel("CLUB DEPORTIVO SÉNECA", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        panelPrincipal.add(lblTitulo, BorderLayout.NORTH);

        JPanel panelFormulario = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panelFormulario.add(new JLabel("Login:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        txtLogin = new JTextField(15);
        panelFormulario.add(txtLogin, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panelFormulario.add(new JLabel("Contraseña:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        txtPassword = new JPasswordField(15);
        panelFormulario.add(txtPassword, gbc);

        panelPrincipal.add(panelFormulario, BorderLayout.CENTER);

        JButton btnIngresar = new JButton("Iniciar Sesión");
        btnIngresar.addActionListener(e -> autenticar());
        getRootPane().setDefaultButton(btnIngresar);

        JPanel panelBoton = new JPanel();
        panelBoton.add(btnIngresar);
        panelPrincipal.add(panelBoton, BorderLayout.SOUTH);

        add(panelPrincipal);
    }

    private void autenticar() {
        String login = txtLogin.getText().trim();
        String pass = new String(txtPassword.getPassword()).trim();

        if (login.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese login y contraseña.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            controlador.iniciarSesion(login, pass);
            VentanaPrincipal principal = new VentanaPrincipal(controlador);
            principal.setVisible(true);
            this.dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de acceso", JOptionPane.ERROR_MESSAGE);
            txtPassword.setText("");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ControladorGUI controlador = new ControladorGUI();
            new VentanaLogin(controlador).setVisible(true);
        });
    }
}
