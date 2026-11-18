package Interfaz;

import java.awt.BorderLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import Deportes.Competencia;
import Deportes.Deporte;
import Deportes.Equipo;
import Deportes.InscripcionCompetencia;
import Usuarios.Socio;

/** Deportes, equipos y competencias (solo administrador). */
public class PanelAdminDeportes extends PanelBase {

    private final PanelTabla tDeportes = new PanelTabla("Deportes", "Nombre", "Tipo", "Posiciones", "Máx. instalaciones simultáneas");
    private final PanelTabla tEquipos = new PanelTabla("Equipos", "Nombre", "Deporte", "Edad máxima", "Cupos", "Jugadores habilitados");
    private final PanelTabla tCompetencias = new PanelTabla("Competencias", "Nombre", "Entidad", "Temporada", "Formato", "Fase actual",
            "Categoría", "Equipos inscritos", "Partidos");
    private final PanelTabla tPosiciones = new PanelTabla("Tabla de posiciones de la competencia seleccionada", "Puesto", "Equipo", "Deporte", "Puntos");
    private final JTextArea salida = UtilGUI.areaTexto();

    public PanelAdminDeportes(ControladorGUI controlador) {
        super(controlador);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Deportes", panelDeportes());
        tabs.addTab("Equipos", panelEquipos());
        tabs.addTab("Competencias", panelCompetencias());
        add(tabs, BorderLayout.CENTER);
        refrescar();
    }

    @Override
    protected void refrescar() {
        tDeportes.limpiar();
        for (Deporte d : club().getDeportes().values()) {
            String tipo = d.esIndividual() ? "Individual" : "Conjunto";
            Object maximo = d.getMaxInstalacionesSimultaneas() == 0 ? "Sin límite" : (Object) d.getMaxInstalacionesSimultaneas();
            tDeportes.agregar(d.getNombreDeporte(), tipo, String.valueOf(d.getPosiciones()), maximo);
        }
        tEquipos.limpiar();
        List<Equipo> ordenados = new java.util.ArrayList<>(club().getEquipos().values());
        ordenados.sort(java.util.Comparator.comparing((Equipo e) -> e.getDeporteEquipo().getNombreDeporte())
                .thenComparing(Equipo::getCategoriaEdad, java.util.Comparator.reverseOrder())
                .thenComparing(Equipo::getNombreEquipo));
        for (Equipo e : ordenados) {
            tEquipos.agregar(e.getNombreEquipo(), e.getDeporteEquipo().getNombreDeporte(), e.getCategoriaEdad(),
                    e.getSocios().size() + "/" + e.getCupoMaximo(), e.contarJugadoresHabilitados());
        }
        String seleccionada = tCompetencias.seleccionado(0);
        tCompetencias.limpiar();
        List<Competencia> ordenadas = new java.util.ArrayList<>(club().getCompetencias().values());
        ordenadas.sort(java.util.Comparator.comparing(Competencia::getNombreCompetencia, String.CASE_INSENSITIVE_ORDER));
        for (Competencia c : ordenadas) {
            tCompetencias.agregar(c.getNombreCompetencia(), c.getEntidadOrganizadora(), c.getAnioTemporada(), c.getFormato(),
                    c.getFaseActual(), c.getCategoria(), c.getInscripcionesCompetencia().size(), c.getPartidosCompetencia().size());
        }
        if (seleccionada != null) {
            tCompetencias.seleccionarFila(0, seleccionada);
        }
        if (tCompetencias.seleccionado(0) == null && tCompetencias.filas() > 0) {
            tCompetencias.seleccionarFila(0, tCompetencias.valorEn(0, 0));
        }
        actualizarPosiciones();
    }

    /** Llena la tabla de posiciones con la competencia elegida en la tabla de arriba (mayor puntaje primero). */
    private void actualizarPosiciones() {
        tPosiciones.limpiar();
        String nombre = tCompetencias.seleccionado(0);
        Competencia competencia = nombre == null ? null : club().getCompetencias().get(nombre);
        if (competencia == null) {
            return;
        }
        List<InscripcionCompetencia> orden = new ArrayList<>(competencia.getInscripcionesCompetencia());
        orden.sort((a, b) -> Integer.compare(b.getPuntosAcumulados(), a.getPuntosAcumulados()));
        int puesto = 1;
        for (InscripcionCompetencia i : orden) {
            tPosiciones.agregar(puesto++, i.getEquipo().getNombreEquipo(), i.getEquipo().getDeporteEquipo().getNombreDeporte(),
                    i.getPuntosAcumulados());
        }
    }

    // ---------- Deportes ----------

    private JPanel panelDeportes() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(tDeportes, BorderLayout.CENTER);
        p.add(UtilGUI.barra(UtilGUI.boton("Crear deporte", this, this::crearDeporte)), BorderLayout.SOUTH);
        return p;
    }

    private void crearDeporte() {
        Formulario f = new Formulario("Crear deporte")
                .texto("nombre", "Nombre del deporte")
                .entero("min", "Mínimo de jugadores", 1, 1, 50)
                .texto("posiciones", "Posiciones (separadas por coma, vacío si no aplica)")
                .casilla("empate", "¿Permite empate?")
                .entero("pv", "Puntos por victoria", 3, 0, 100)
                .entero("pd", "Puntos por derrota", 0, 0, 100)
                .entero("pe", "Puntos por empate", 1, 0, 100);
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("nombre");
        if (club().getDeportes().containsKey(f.valor("nombre"))) {
            throw new IllegalArgumentException("Ya existe un deporte con ese nombre.");
        }
        List<String> posiciones = new ArrayList<>();
        if (!f.valor("posiciones").isEmpty()) {
            for (String pos : f.valor("posiciones").split(",")) {
                posiciones.add(pos.trim());
            }
        }
        club().crearDeporte(f.valor("nombre"), f.entero("min"), posiciones, f.marcado("empate"),
                f.entero("pv"), f.entero("pd"), f.entero("pe"));
        guardar();
        refrescar();
        info("Deporte creado.");
    }

    // ---------- Equipos ----------

    private JPanel panelEquipos() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(tEquipos, BorderLayout.CENTER);
        p.add(UtilGUI.barra(
                UtilGUI.boton("Crear equipo", this, this::crearEquipo),
                UtilGUI.boton("Dar de baja equipo seleccionado", this, this::bajaEquipo)), BorderLayout.SOUTH);
        return p;
    }

    private void crearEquipo() {
        Formulario f = new Formulario("Crear equipo")
                .texto("nombre", "Nombre del equipo")
                .entero("cupo", "Cupo máximo", 15, 1, 100)
                .combo("cat", "Edad máxima de la categoría (99 = Mayores)", Arrays.asList(UtilGUI.CATEGORIAS))
                .combo("deporte", "Deporte", nombresDeportes(false));
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("nombre", "deporte");
        club().crearEquipo(f.valor("nombre"), f.entero("cupo"), categoria(f.valor("cat")), f.valor("deporte"), true);
        guardar();
        refrescar();
        info("Equipo creado.");
    }

    private void bajaEquipo() {
        String nombre = tEquipos.requerirSeleccion(0, "Seleccione un equipo de la tabla.");
        if (!UtilGUI.confirmar(this, "¿Dar de baja el equipo " + nombre + "?")) {
            return;
        }
        club().realizarBajaEquipo(nombre);
        guardar();
        refrescar();
        info("Equipo dado de baja.");
    }

    // ---------- Competencias ----------

    private JPanel panelCompetencias() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tCompetencias.getTabla().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarPosiciones();
            }
        });
        JScrollPane scroll = new JScrollPane(salida);
        scroll.setBorder(BorderFactory.createTitledBorder("Resultado de consultas"));
        JSplitPane abajo = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tPosiciones, scroll);
        abajo.setResizeWeight(0.6);
        JSplitPane centro = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tCompetencias, abajo);
        centro.setResizeWeight(0.55);
        p.add(centro, BorderLayout.CENTER);
        p.add(UtilGUI.barra(
                UtilGUI.boton("Crear competencia", this, this::crearCompetencia),
                UtilGUI.boton("Inscribir equipo en competencia", this, this::inscribirEquipo),
                UtilGUI.boton("Consultar goleador", this, this::consultarGoleador)), BorderLayout.SOUTH);
        return p;
    }

    private void crearCompetencia() {
        Formulario f = new Formulario("Crear competencia")
                .texto("nombre", "Nombre de la competencia")
                .combo("entidad", "Entidad organizadora", Arrays.asList(Competencia.ASCUN, Competencia.CERROS))
                .entero("anio", "Año / temporada", LocalDate.now().getYear(), 2000, 2100)
                .comboEditable("formato", "Formato", Arrays.asList("Liga todos contra todos", "Fase de grupos y eliminación directa"))
                .comboEditable("fase", "Fase actual", Arrays.asList("Liga", "Fase de grupos", "Eliminación directa"))
                .combo("cat", "Categoría (edad máxima, 99 = Mayores)", Arrays.asList(UtilGUI.CATEGORIAS));
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("nombre", "entidad", "formato", "fase");
        if (club().getCompetencias().containsKey(f.valor("nombre"))) {
            throw new IllegalArgumentException("Ya existe una competencia con ese nombre.");
        }
        club().crearCompetencia(f.valor("nombre"), f.valor("entidad"), String.valueOf(f.entero("anio")), f.valor("formato"),
                f.valor("fase"), categoria(f.valor("cat")));
        guardar();
        refrescar();
        info("Competencia creada.");
    }

    private void inscribirEquipo() {
        Formulario f = new Formulario("Inscribir equipo en competencia")
                .combo("equipo", "Equipo", nombresEquipos())
                .combo("competencia", "Competencia", nombresCompetencias())
                .lista("nomina", "Nómina de jugadores (varios)", opcionesUsuarios(Socio.class));
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("equipo", "competencia");
        List<Socio> nomina = sociosDe(f.seleccionados("nomina"));
        club().realizarInscripcionCompetencia(0, "-", f.valor("equipo"), f.valor("competencia"), nomina);
        guardar();
        refrescar();
        info("Equipo inscrito en la competencia.");
    }

    private void consultarGoleador() {
        Formulario f = new Formulario("Goleador de una competencia").combo("competencia", "Competencia", nombresCompetencias());
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("competencia");
        String competencia = f.valor("competencia");
        Socio goleador = club().goleadorTorneoCompetencia(competencia);
        if (goleador == null) {
            salida.setText("Todavía no hay goles registrados en " + competencia + ".");
        } else {
            salida.setText("Goleador de " + competencia + ": " + goleador.getNombre() + " " + goleador.getApellido()
                    + " con " + club().golesEnCompetencia(competencia, goleador) + " goles.");
        }
    }
}
