package Interfaz;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JTabbedPane;
import Tienda.ArticuloTienda;
import Tienda.ProductoCafeteria;

/** Inventario de la tienda deportiva y la cafetería: crear productos y reabastecer. */
public class PanelAdminInventario extends PanelBase {

    private static final List<String> ALERGENOS = Arrays.asList("Maní", "Gluten", "Lácteos", "Huevo", "Soya", "Frutos secos", "Pescado");

    private final PanelTabla tTienda = new PanelTabla("Artículos de la tienda deportiva", "Producto", "Precio base", "Stock");
    private final PanelTabla tCafeteria = new PanelTabla("Productos de la cafetería", "Producto", "Precio base", "Stock");
    private final JTabbedPane tabs = new JTabbedPane();

    public PanelAdminInventario(ControladorGUI controlador) {
        super(controlador);
        tabs.addTab("Tienda deportiva", tTienda);
        tabs.addTab("Cafetería", tCafeteria);
        add(tabs, BorderLayout.CENTER);
        add(UtilGUI.barra(
                UtilGUI.boton("Crear artículo de tienda", this, this::crearArticulo),
                UtilGUI.boton("Crear producto de cafetería", this, this::crearProducto),
                UtilGUI.boton("Reabastecer producto seleccionado", this, this::reabastecer)), BorderLayout.SOUTH);
        refrescar();
    }

    @Override
    protected void refrescar() {
        tTienda.limpiar();
        for (ArticuloTienda a : tienda().obtenerArticulosTienda().values()) {
            tTienda.agregar(a.getNombre(), UtilGUI.dinero(a.getPrecioBase()), a.getCantidadInventario());
        }
        tCafeteria.limpiar();
        for (ProductoCafeteria p : tienda().obtenerProductosCafeteria().values()) {
            tCafeteria.agregar(p.getNombre(), UtilGUI.dinero(p.getPrecioBase()), p.getCantidadInventario());
        }
    }

    private void crearArticulo() {
        Formulario f = new Formulario("Crear artículo de tienda")
                .texto("nombre", "Nombre")
                .decimal("precio", "Precio base", 10000, 0, 1e9, 1000)
                .entero("stock", "Stock", 10, 0, 100000)
                .comboEditable("cat", "Categoría", Arrays.asList("Raquetas", "Balones", "Mallas", "Uniformes", "Pelotas", "Accesorios"));
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("nombre", "cat");
        validarPrecio(f.decimal("precio"));
        tienda().crearArticuloTienda(f.valor("nombre"), f.decimal("precio"), f.entero("stock"), f.valor("cat"));
        guardar();
        refrescar();
        info("Artículo registrado.");
    }

    private void crearProducto() {
        Formulario f = new Formulario("Crear producto de cafetería")
                .texto("nombre", "Nombre")
                .decimal("precio", "Precio base", 5000, 0, 1e9, 500)
                .entero("stock", "Stock", 10, 0, 100000)
                .casilla("caliente", "¿Bebida caliente?")
                .lista("alergenos", "Alérgenos (selección múltiple, ninguno si no tiene)", ALERGENOS)
                .texto("otros", "Otros alérgenos (opcional, separados por coma)");
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("nombre");
        validarPrecio(f.decimal("precio"));
        List<String> alergenos = new ArrayList<>(f.seleccionados("alergenos"));
        for (String otro : f.valor("otros").split(",")) {
            if (!otro.trim().isEmpty()) {
                alergenos.add(otro.trim());
            }
        }
        tienda().crearProductoCafeteria(f.valor("nombre"), f.decimal("precio"), f.entero("stock"),
                f.marcado("caliente"), String.join(",", alergenos));
        guardar();
        refrescar();
        info("Producto registrado.");
    }

    private void validarPrecio(double precio) {
        if (precio <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero.");
        }
    }

    private void reabastecer() {
        PanelTabla activa = tabs.getSelectedIndex() == 0 ? tTienda : tCafeteria;
        String nombre = activa.requerirSeleccion(0, "Seleccione un producto de la tabla.");
        Formulario f = new Formulario("Reabastecer " + nombre).entero("cant", "Cantidad a agregar", 10, 1, 100000);
        if (!f.mostrar(this)) {
            return;
        }
        tienda().reabastecer(nombre, f.entero("cant"));
        guardar();
        refrescar();
        info("Inventario actualizado.");
    }
}
