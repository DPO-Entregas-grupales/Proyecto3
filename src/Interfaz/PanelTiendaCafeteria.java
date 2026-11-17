package Interfaz;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import Tienda.ItemVenta;
import Tienda.Producto;
import Tienda.ProductoCafeteria;
import Tienda.Venta;

public class PanelTiendaCafeteria extends JPanel {

    private final ControladorGUI controlador;
    private final VentanaPrincipal ventanaPrincipal;

    private JComboBox<String> cmbTipoTienda;
    private DefaultTableModel modeloCatalogo;
    private JTable tablaCatalogo;
    private DefaultTableModel modeloCarrito;
    private JSpinner spnCantidad;
    private JTextField txtCodigoDescuento;
    private JCheckBox chkUsarPuntos;
    private JSpinner spnPuntos;

    private final List<ItemVenta> carrito = new ArrayList<>();

    public PanelTiendaCafeteria(ControladorGUI controlador, VentanaPrincipal ventanaPrincipal) {
        this.controlador = controlador;
        this.ventanaPrincipal = ventanaPrincipal;
        inicializarComponentes();
        cargarCatalogo();
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout(10, 10));

        JPanel panelTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelTop.add(new JLabel("Establecimiento:"));
        cmbTipoTienda = new JComboBox<>(new String[]{"Tienda Deportiva", "Cafetería"});
        cmbTipoTienda.addActionListener(e -> {
            carrito.clear();
            actualizarTablaCarrito();
            cargarCatalogo();
        });
        panelTop.add(cmbTipoTienda);
        add(panelTop, BorderLayout.NORTH);

        JPanel panelCentro = new JPanel(new GridLayout(1, 2, 15, 0));

        modeloCatalogo = new DefaultTableModel(new String[]{"Producto", "Precio Base", "Stock"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaCatalogo = new JTable(modeloCatalogo);
        JPanel panelIzq = new JPanel(new BorderLayout());
        panelIzq.setBorder(BorderFactory.createTitledBorder("Catálogo Disponible"));
        panelIzq.add(new JScrollPane(tablaCatalogo), BorderLayout.CENTER);

        JPanel panelAgregar = new JPanel(new FlowLayout());
        panelAgregar.add(new JLabel("Cantidad:"));
        spnCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        panelAgregar.add(spnCantidad);
        JButton btnAgregar = new JButton("Agregar al carrito");
        btnAgregar.addActionListener(e -> agregarAlCarrito());
        panelAgregar.add(btnAgregar);
        panelIzq.add(panelAgregar, BorderLayout.SOUTH);

        modeloCarrito = new DefaultTableModel(new String[]{"Producto", "Cant.", "Subtotal"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable tablaCarrito = new JTable(modeloCarrito);
        JPanel panelDer = new JPanel(new BorderLayout());
        panelDer.setBorder(BorderFactory.createTitledBorder("Carrito de Compras"));
        panelDer.add(new JScrollPane(tablaCarrito), BorderLayout.CENTER);

        JButton btnVaciar = new JButton("Vaciar carrito");
        btnVaciar.addActionListener(e -> {
            carrito.clear();
            actualizarTablaCarrito();
        });
        panelDer.add(btnVaciar, BorderLayout.SOUTH);

        panelCentro.add(panelIzq);
        panelCentro.add(panelDer);
        add(panelCentro, BorderLayout.CENTER);

        JPanel panelPago = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panelPago.setBorder(BorderFactory.createTitledBorder("Finalizar Compra"));

        panelPago.add(new JLabel("Código descuento:"));
        txtCodigoDescuento = new JTextField(10);
        panelPago.add(txtCodigoDescuento);

        chkUsarPuntos = new JCheckBox("Redimir puntos:");
        spnPuntos = new JSpinner(new SpinnerNumberModel(0, 0, 100000, 10));
        panelPago.add(chkUsarPuntos);
        panelPago.add(spnPuntos);

        JButton btnComprar = new JButton("Confirmar Compra");
        btnComprar.addActionListener(e -> procesarCompra());
        panelPago.add(btnComprar);

        add(panelPago, BorderLayout.SOUTH);
    }

    private void cargarCatalogo() {
        modeloCatalogo.setRowCount(0);
        boolean esCafeteria = cmbTipoTienda.getSelectedIndex() == 1;
        Map<String, ? extends Producto> catalogo = esCafeteria
                ? controlador.getTienda().obtenerProductosCafeteria()
                : controlador.getTienda().obtenerArticulosTienda();

        for (Producto p : catalogo.values()) {
            modeloCatalogo.addRow(new Object[]{p.getNombre(), p.getPrecioBase(), p.getCantidadInventario()});
        }
    }

    private void agregarAlCarrito() {
        int fila = tablaCatalogo.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto del catálogo.");
            return;
        }
        String nombre = (String) modeloCatalogo.getValueAt(fila, 0);
        int cantidad = (int) spnCantidad.getValue();
        boolean esCafeteria = cmbTipoTienda.getSelectedIndex() == 1;

        Producto producto = esCafeteria
                ? controlador.getTienda().obtenerProductosCafeteria().get(nombre)
                : controlador.getTienda().obtenerArticulosTienda().get(nombre);

        if (producto instanceof ProductoCafeteria) {
            List<String> alergenos = controlador.getTienda().informarAlergenos(producto);
            if (!alergenos.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Alérgenos de " + nombre + ": " + String.join(", ", alergenos));
            }
        }

        for (ItemVenta item : carrito) {
            if (item.getProducto() == producto) {
                item.setCantidad(item.getCantidad() + cantidad);
                actualizarTablaCarrito();
                return;
            }
        }
        carrito.add(new ItemVenta(cantidad, producto));
        actualizarTablaCarrito();
    }

    private void actualizarTablaCarrito() {
        modeloCarrito.setRowCount(0);
        for (ItemVenta item : carrito) {
            modeloCarrito.addRow(new Object[]{
                item.getProducto().getNombre(),
                item.getCantidad(),
                item.precioItemVenta()
            });
        }
    }

    private void procesarCompra() {
        if (carrito.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El carrito está vacío.");
            return;
        }
        boolean esCafeteria = cmbTipoTienda.getSelectedIndex() == 1;
        String tipo = esCafeteria ? Venta.CAFETERIA : Venta.TIENDADEPORTIVA;
        String codigo = esCafeteria ? "" : txtCodigoDescuento.getText().trim();
        int puntos = chkUsarPuntos.isSelected() ? (int) spnPuntos.getValue() : 0;

        LocalDateTime ahora = LocalDateTime.now();
        String idVenta = controlador.getTienda().generarIdVenta();
        String idUsuario = controlador.getUsuarioAutenticado().getIdUsuario();

        try {
            if (puntos > 0) {
                controlador.getTienda().realizarVentaConPuntos(
                        idVenta, ahora.toLocalDate().toString(), ahora.getHour(),
                        idUsuario, codigo, new ArrayList<>(carrito), controlador.getClub(), tipo, puntos);
            } else {
                controlador.getTienda().realizarVenta(
                        idVenta, ahora.toLocalDate().toString(), ahora.getHour(), null,
                        idUsuario, codigo, new ArrayList<>(carrito), controlador.getClub(), tipo);
            }
            controlador.guardarTodo();
            carrito.clear();
            actualizarTablaCarrito();
            cargarCatalogo();
            ventanaPrincipal.actualizarPuntosEncabezado();
            JOptionPane.showMessageDialog(this, "Compra registrada con éxito (ID: " + idVenta + ").");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error en compra", JOptionPane.ERROR_MESSAGE);
        }
    }
}
