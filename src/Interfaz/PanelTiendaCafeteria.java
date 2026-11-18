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
import Usuarios.Socio;

/** Compras en la tienda deportiva y la cafetería (todos los roles), con códigos, puntos y bonos de torneo. */
public class PanelTiendaCafeteria extends PanelBase {

    private final VentanaPrincipal ventanaPrincipal;

    private JComboBox<String> cmbTipoTienda;
    private DefaultTableModel modeloCatalogo;
    private JTable tablaCatalogo;
    private DefaultTableModel modeloCarrito;
    private JSpinner spnCantidad;
    private JTextField txtCodigoDescuento;
    private JCheckBox chkUsarPuntos;
    private JSpinner spnPuntos;
    private JButton btnBono;

    private final List<ItemVenta> carrito = new ArrayList<>();

    public PanelTiendaCafeteria(ControladorGUI controlador, VentanaPrincipal ventanaPrincipal) {
        super(controlador);
        this.ventanaPrincipal = ventanaPrincipal;
        inicializarComponentes();
        refrescar();
    }

    private boolean esCafeteria() {
        return cmbTipoTienda.getSelectedIndex() == 1;
    }

    private void inicializarComponentes() {
        JPanel panelTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelTop.add(new JLabel("Establecimiento:"));
        cmbTipoTienda = new JComboBox<>(new String[]{"Tienda Deportiva", "Cafetería"});
        cmbTipoTienda.addActionListener(e -> {
            carrito.clear();
            actualizarTablaCarrito();
            cargarCatalogo();
            btnBono.setEnabled(puedeUsarBono());
            txtCodigoDescuento.setEnabled(!esCafeteria());
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
        panelAgregar.add(UtilGUI.boton("Agregar al carrito", this, this::agregarAlCarrito));
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
        panelDer.add(UtilGUI.boton("Vaciar carrito", this, () -> {
            carrito.clear();
            actualizarTablaCarrito();
        }), BorderLayout.SOUTH);

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

        btnBono = UtilGUI.boton("Comprar con bono de torneo", this, this::comprarConBono);
        panelPago.add(btnBono);
        panelPago.add(UtilGUI.boton("Confirmar Compra", this, this::procesarCompra));

        add(panelPago, BorderLayout.SOUTH);
    }

    @Override
    protected void refrescar() {
        cargarCatalogo();
        btnBono.setEnabled(puedeUsarBono());
    }

    private boolean puedeUsarBono() {
        return usuario() instanceof Socio && !esCafeteria();
    }

    private Map<String, ? extends Producto> catalogo() {
        return esCafeteria() ? tienda().obtenerProductosCafeteria() : tienda().obtenerArticulosTienda();
    }

    private void cargarCatalogo() {
        modeloCatalogo.setRowCount(0);
        for (Producto p : catalogo().values()) {
            modeloCatalogo.addRow(new Object[]{p.getNombre(), UtilGUI.dinero(p.getPrecioBase()), p.getCantidadInventario()});
        }
    }

    private void agregarAlCarrito() {
        int fila = tablaCatalogo.getSelectedRow();
        if (fila == -1) {
            throw new IllegalArgumentException("Seleccione un producto del catálogo.");
        }
        String nombre = (String) modeloCatalogo.getValueAt(fila, 0);
        int cantidad = (int) spnCantidad.getValue();
        Producto producto = catalogo().get(nombre);

        if (producto instanceof ProductoCafeteria) {
            List<String> alergenos = tienda().informarAlergenos(producto);
            if (!alergenos.isEmpty()) {
                info("Alérgenos de " + nombre + ": " + String.join(", ", alergenos));
            }
        }
        agregarItem(producto, cantidad);
        actualizarTablaCarrito();
    }

    private void agregarItem(Producto producto, int cantidad) {
        for (ItemVenta item : carrito) {
            if (item.getProducto() == producto) {
                item.setCantidad(item.getCantidad() + cantidad);
                return;
            }
        }
        carrito.add(new ItemVenta(cantidad, producto));
    }

    private void actualizarTablaCarrito() {
        modeloCarrito.setRowCount(0);
        for (ItemVenta item : carrito) {
            modeloCarrito.addRow(new Object[]{item.getProducto().getNombre(), item.getCantidad(), UtilGUI.dinero(item.precioItemVenta())});
        }
    }

    private void procesarCompra() {
        if (carrito.isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío.");
        }
        String tipo = esCafeteria() ? Venta.CAFETERIA : Venta.TIENDADEPORTIVA;
        String codigo = esCafeteria() ? "" : txtCodigoDescuento.getText().trim();
        if (codigo.toUpperCase().startsWith("BONO-")) {
            throw new IllegalArgumentException("Los bonos se redimen con el botón \"Comprar con bono de torneo\".");
        }
        int puntos = chkUsarPuntos.isSelected() ? (int) spnPuntos.getValue() : 0;

        LocalDateTime ahora = LocalDateTime.now();
        String idVenta = tienda().generarIdVenta();
        String idUsuario = usuario().getIdUsuario();

        if (puntos > 0) {
            tienda().realizarVentaConPuntos(idVenta, ahora.toLocalDate().toString(), ahora.getHour(),
                    idUsuario, codigo, new ArrayList<>(carrito), club(), tipo, puntos);
        } else {
            tienda().realizarVenta(idVenta, ahora.toLocalDate().toString(), ahora.getHour(), null,
                    idUsuario, codigo, new ArrayList<>(carrito), club(), tipo);
        }
        terminarCompra();
    }

    /** Compra con bono: se arma el carrito, se aplica el bono y solo al confirmar se descuenta inventario y bono. */
    private void comprarConBono() {
        Socio socio = (Socio) usuario();
        if (!socio.tieneBonosDisponibles()) {
            throw new IllegalArgumentException("No tiene ningún bono de descuento por redimir.");
        }
        if (carrito.isEmpty()) {
            throw new IllegalArgumentException("Agregue productos al carrito antes de usar un bono.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        Venta venta = tienda().iniciarCompraTienda(tienda().generarIdVenta(), ahora.toLocalDate().toString(),
                ahora.getHour(), socio.getIdUsuario(), new ArrayList<>(carrito), club());

        List<String> bonos = new ArrayList<>();
        for (String bono : socio.getBonosDescuento()) {
            bonos.add(bono);
        }
        Formulario f = new Formulario("Aplicar bono (subtotal " + UtilGUI.dinero(venta.getSubTotal()) + ")")
                .combo("bono", "Bono a aplicar", bonos);
        if (!f.mostrar(this)) {
            return;
        }
        tienda().aplicarBonoDescuento(venta, f.valor("bono"));
        String mensaje = "Bono aplicado. Descuento: " + UtilGUI.dinero(venta.getDescuento());

        if (bonos.size() > 1 && UtilGUI.confirmar(this, mensaje + "\n¿Desea aplicar otro bono a esta compra?")) {
            try {
                tienda().aplicarBonoDescuento(venta, "");
            } catch (IllegalArgumentException e) {
                UtilGUI.error(this, "No se pudo aplicar: " + e.getMessage());
            }
        }
        if (UtilGUI.confirmar(this, "Descuento total: " + UtilGUI.dinero(venta.getDescuento()) + "\n¿Confirmar la compra?")) {
            tienda().confirmarCompraTienda(venta);
            terminarCompra();
        } else {
            info("Compra cancelada. El inventario y el bono no cambiaron.");
        }
    }

    private void terminarCompra() {
        guardar();
        carrito.clear();
        actualizarTablaCarrito();
        cargarCatalogo();
        ventanaPrincipal.actualizarPuntosEncabezado();
        List<Venta> ventas = tienda().getVentas();
        info(resumen(ventas.get(ventas.size() - 1)));
    }

    private String resumen(Venta venta) {
        StringBuilder sb = new StringBuilder("COMPRA REALIZADA: " + venta.getIdVenta() + "\n");
        for (ItemVenta item : venta.getItemsVenta()) {
            sb.append("- ").append(item.getProducto().getNombre()).append(" x").append(item.getCantidad())
                    .append(" = ").append(UtilGUI.dinero(item.precioItemVenta())).append("\n");
        }
        sb.append("Subtotal: ").append(UtilGUI.dinero(venta.getSubTotal())).append("\n");
        if (venta.getDescuento() > 0) {
            sb.append("Descuento: -").append(UtilGUI.dinero(venta.getDescuento())).append("\n");
        }
        String impuestos = Venta.CAFETERIA.equals(venta.getTipo()) ? " (con impuesto al consumo y propina sugerida)" : " (con IVA)";
        sb.append("Total a pagar").append(impuestos).append(": ").append(UtilGUI.dinero(venta.getTotal())).append("\n");
        sb.append("Puntos ganados: ").append((int) venta.getPuntosGanados()).append("\n");
        sb.append("Sus puntos de fidelidad: ").append(usuario().getPuntosFidelidad());
        return sb.toString();
    }
}
