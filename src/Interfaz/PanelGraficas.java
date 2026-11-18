package Interfaz;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToIntFunction;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import Deportes.Competencia;
import Deportes.Deporte;
import Deportes.Equipo;
import Deportes.InscripcionCompetencia;
import Deportes.InscripcionIndividual;
import Estadisticas.Estadistica;
import Estadisticas.EstadisticaBaloncesto;
import Estadisticas.EstadisticaFutbol;
import Estadisticas.EstadisticaVoleibol;
import Partidos.Partido;
import Partidos.PartidoCompetencia;
import Partidos.PartidoIndividual;
import Partidos.PartidoInterno;
import SesionEntrenamiento.AsistenciaEvaluacion;
import SesionEntrenamiento.SesionEntrenamiento;
import Tienda.Venta;
import Usuarios.Administrador;
import Usuarios.Entrenador;
import Usuarios.Fisioterapeuta;
import Usuarios.Invitado;
import Usuarios.Socio;
import Usuarios.Usuario;

/**
 * Visualizaciones del club. Todas las gráficas se dibujan con los datos actuales del sistema
 * (nada es una imagen fija), se actualizan al cambiar un filtro y al volver a abrir el módulo.
 * <ol>
 *   <li>Pastel: victorias, derrotas y empates de un equipo o de un jugador.</li>
 *   <li>Barras: estadística de desempeño por jugador de un equipo.</li>
 *   <li>Barras: ingresos netos por día de la tienda y la cafetería.</li>
 *   <li>Líneas: asistencia (o nota) a los entrenamientos por semana.</li>
 *   <li>Complementaria (barras): puntos por equipo en una competencia o ranking individual.</li>
 * </ol>
 */
public class PanelGraficas extends PanelBase {

    private static final String TODOS = "Todos";
    private static final String[] TIPOS_PARTIDO = {"Todos", "Internos", "De competencia"};
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    /** Una estadística que se puede graficar: de qué clase es y cómo se lee su valor. */
    private static final class Metrica {
        final String nombre;
        final Class<? extends Estadistica> clase;
        final ToIntFunction<Estadistica> valor;

        Metrica(String nombre, Class<? extends Estadistica> clase, ToIntFunction<Estadistica> valor) {
            this.nombre = nombre;
            this.clase = clase;
            this.valor = valor;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    private static <T extends Estadistica> Metrica metrica(String nombre, Class<T> clase, ToIntFunction<T> lector) {
        return new Metrica(nombre, clase, e -> lector.applyAsInt(clase.cast(e)));
    }

    private static final List<Metrica> METRICAS = List.of(
            metrica("Goles", EstadisticaFutbol.class, EstadisticaFutbol::getGoles),
            metrica("Asistencias", EstadisticaFutbol.class, EstadisticaFutbol::getAsistencias),
            metrica("Tarjetas amarillas", EstadisticaFutbol.class, EstadisticaFutbol::getTarjetasAmarillas),
            metrica("Tarjetas rojas", EstadisticaFutbol.class, EstadisticaFutbol::getTarjetasRojas),
            metrica("Puntos", EstadisticaBaloncesto.class, EstadisticaBaloncesto::getPuntos),
            metrica("Rebotes", EstadisticaBaloncesto.class, EstadisticaBaloncesto::getRebotes),
            metrica("Aces", EstadisticaVoleibol.class, EstadisticaVoleibol::getAces),
            metrica("Bloqueos", EstadisticaVoleibol.class, EstadisticaVoleibol::getBloqueos));

    /** Evita que los listeners de los combos disparen actualizaciones mientras se vuelven a llenar. */
    private boolean cargando;

    // ----- 1. Pastel -----
    private final GraficoPastel pastel = new GraficoPastel();
    private final JComboBox<String> pVer = new JComboBox<>(new String[]{"Equipo", "Jugador (deporte individual)"});
    private final JComboBox<String> pSujeto = new JComboBox<>();
    private final JComboBox<String> pTipo = new JComboBox<>(TIPOS_PARTIDO);
    private final JComboBox<String> pDeporte = new JComboBox<>();
    private final JTextField pDesde = new JTextField(9);
    private final JTextField pHasta = new JTextField(9);

    // ----- 2. Barras: desempeño por jugador -----
    private final GraficoBarras barrasJugadores = new GraficoBarras();
    private final JComboBox<String> bEquipo = new JComboBox<>();
    private final JComboBox<Metrica> bMetrica = new JComboBox<>();
    private final JComboBox<String> bTipo = new JComboBox<>(TIPOS_PARTIDO);
    private final JComboBox<String> bCompetencia = new JComboBox<>();
    private final JTextField bDesde = new JTextField(9);
    private final JTextField bHasta = new JTextField(9);

    // ----- 3. Barras: ingresos -----
    private final GraficoBarras barrasIngresos = new GraficoBarras();
    private final JTextField iInicio = new JTextField(9);
    private final JSpinner iDias = new JSpinner(new SpinnerNumberModel(5, 1, 31, 1));
    private final JComboBox<String> iTipo = new JComboBox<>(new String[]{"Tienda y cafetería", "Solo tienda deportiva", "Solo cafetería"});
    private final JComboBox<String> iCliente = new JComboBox<>(
            new String[]{"Todos los clientes", "Socios", "Entrenadores", "Fisioterapeutas", "Administradores", "Invitados"});

    // ----- 4. Líneas: asistencia semanal -----
    private final GraficoLineas lineas = new GraficoLineas();
    private final JComboBox<String> lEquipo = new JComboBox<>();
    private final JComboBox<String> lJugador = new JComboBox<>();
    private final JComboBox<String> lMetrica = new JComboBox<>(new String[]{"Asistencia (% de convocados)", "Nota promedio de desempeño"});
    private final JComboBox<Integer> lSemanas = new JComboBox<>(new Integer[]{4, 6, 8, 12});
    private final JTextField lHasta = new JTextField(9);

    // ----- 5. Complementaria: posiciones y ranking -----
    private final GraficoBarras barrasRanking = new GraficoBarras();
    private final JComboBox<String> rVer = new JComboBox<>(new String[]{"Puntos por equipo en una competencia", "Ranking individual de un deporte"});
    private final JComboBox<String> rObjetivo = new JComboBox<>();
    private final JComboBox<String> rNivel = new JComboBox<>(new String[]{TODOS, "Principiante", "Intermedio", "Avanzado"});

    public PanelGraficas(ControladorGUI controlador) {
        super(controlador);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Resultados (pastel)", panelPastel());
        tabs.addTab("Desempeño por jugador (barras)", panelBarrasJugadores());
        // Los ingresos de la tienda y la cafetería solo los ve el administrador
        if (usuario() == null || usuario() instanceof Administrador) {
            tabs.addTab("Ingresos (barras)", panelIngresos());
        }
        tabs.addTab("Asistencia semanal (líneas)", panelLineas());
        tabs.addTab("Posiciones y ranking", panelRanking());
        lSemanas.setSelectedItem(8);
        add(tabs, BorderLayout.CENTER);
        refrescar();
    }

    // =====================================================================================
    // Refresco general
    // =====================================================================================

    @Override
    protected void refrescar() {
        cargando = true;
        try {
            llenar(pSujeto, pVer.getSelectedIndex() == 0 ? nombresEquipos() : jugadoresConRanking(), null);
            llenar(pDeporte, nombresDeportes(true), TODOS);
            llenar(bEquipo, nombresEquipos(), null);
            llenar(bCompetencia, nombresCompetencias(), "Todas");
            llenar(lEquipo, nombresEquipos(), "Todos los equipos");
            llenarMetricas();
            llenarJugadoresLinea();
            llenarObjetivoRanking();
        } finally {
            cargando = false;
        }
        habilitarCampos();
        actualizarTodas();
    }

    /** Vuelve a llenar un combo conservando, si todavía existe, lo que estaba elegido. */
    private void llenar(JComboBox<String> combo, List<String> opciones, String primera) {
        Object actual = combo.getSelectedItem();
        combo.removeAllItems();
        if (primera != null) {
            combo.addItem(primera);
        }
        for (String o : opciones) {
            combo.addItem(o);
        }
        if (actual != null) {
            combo.setSelectedItem(actual);
        }
    }

    private void actualizarTodas() {
        silencioso(this::actualizarPastel);
        silencioso(this::actualizarBarrasJugadores);
        silencioso(this::actualizarIngresos);
        silencioso(this::actualizarLineas);
        silencioso(this::actualizarRanking);
    }

    /** Ejecuta la actualización mostrando un mensaje si un filtro es inválido (fecha mal escrita, etc.). */
    private void conAviso(Runnable r) {
        if (cargando) {
            return;
        }
        try {
            r.run();
        } catch (IllegalArgumentException ex) {
            UtilGUI.error(this, ex.getMessage());
        }
    }

    /** Igual que conAviso pero sin mensajes: se usa al abrir el módulo para no molestar con avisos. */
    private void silencioso(Runnable r) {
        try {
            r.run();
        } catch (IllegalArgumentException ex) {
            // el filtro tiene un valor inválido; el aviso se muestra cuando la persona pulsa "Actualizar"
        }
    }

    private void habilitarCampos() {
        boolean jugador = pVer.getSelectedIndex() == 1;
        pTipo.setEnabled(!jugador);
        pDeporte.setEnabled(jugador);
        bCompetencia.setEnabled(bTipo.getSelectedIndex() == 2);
        rNivel.setEnabled(rVer.getSelectedIndex() == 1);
    }

    // =====================================================================================
    // Utilidades de interfaz
    // =====================================================================================

    private static JPanel filtros(Component[]... filas) {
        JPanel p = new JPanel(new java.awt.GridLayout(0, 1));
        for (Component[] fila : filas) {
            JPanel f = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            for (Component c : fila) {
                f.add(c);
            }
            p.add(f);
        }
        return p;
    }

    private static Component[] fila(Component... c) {
        return c;
    }

    private JPanel contenedor(JPanel filtros, Component grafico, String nota) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        p.add(filtros, BorderLayout.NORTH);
        p.add(grafico, BorderLayout.CENTER);
        if (nota != null) {
            // JTextArea para que la nota se ajuste al ancho de la ventana en lugar de cortarse
            JTextArea l = new JTextArea(nota);
            l.setEditable(false);
            l.setFocusable(false);
            l.setLineWrap(true);
            l.setWrapStyleWord(true);
            l.setOpaque(false);
            l.setForeground(java.awt.Color.GRAY);
            l.setFont(l.getFont().deriveFont(11f));
            p.add(l, BorderLayout.SOUTH);
        }
        return p;
    }

    private <T> void alCambiar(JComboBox<T> combo, Runnable r) {
        combo.addActionListener(e -> conAviso(r));
    }

    private void alEnter(JTextField campo, Runnable r) {
        campo.addActionListener(e -> conAviso(r));
    }

    private static LocalDate fechaOpcional(JTextField campo, String nombre) {
        String t = campo.getText().trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha \"" + nombre + "\" debe tener el formato AAAA-MM-DD.");
        }
    }

    private static LocalDate leerFecha(String texto) {
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException | NullPointerException e) {
            return null;
        }
    }

    private static boolean enRango(LocalDate f, LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) {
            return true;
        }
        return f != null && (desde == null || !f.isBefore(desde)) && (hasta == null || !f.isAfter(hasta));
    }

    /** Convierte "2-1" en {2, 1}; devuelve null si el texto no tiene ese formato. */
    private static int[] leerMarcador(String marcador) {
        if (marcador == null) {
            return null;
        }
        String[] partes = marcador.trim().split("-");
        if (partes.length != 2) {
            return null;
        }
        try {
            return new int[]{Integer.parseInt(partes[0].trim()), Integer.parseInt(partes[1].trim())};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String sinTildes(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    private static String nombreCompleto(Usuario u) {
        return u.getNombre() + " " + u.getApellido();
    }

    private List<String> jugadoresConRanking() {
        List<String> r = new ArrayList<>();
        for (Usuario u : club().getUsuarios().values()) {
            if (u instanceof Socio && !((Socio) u).getInscripcionesIndividuales().isEmpty()) {
                r.add(opcion(u));
            }
        }
        return r;
    }

    private static boolean esFinalizado(Partido p) {
        return "Finalizado".equalsIgnoreCase(p.getEstadoPartido());
    }

    // =====================================================================================
    // 1. Pastel de resultados
    // =====================================================================================

    private JPanel panelPastel() {
        alCambiar(pVer, () -> {
            cargando = true;
            llenar(pSujeto, pVer.getSelectedIndex() == 0 ? nombresEquipos() : jugadoresConRanking(), null);
            cargando = false;
            habilitarCampos();
            actualizarPastel();
        });
        alCambiar(pSujeto, this::actualizarPastel);
        alCambiar(pTipo, this::actualizarPastel);
        alCambiar(pDeporte, this::actualizarPastel);
        alEnter(pDesde, this::actualizarPastel);
        alEnter(pHasta, this::actualizarPastel);
        JPanel f = filtros(
                fila(new JLabel("Ver resultados de:"), pVer, pSujeto, new JLabel("Partidos:"), pTipo, new JLabel("Deporte:"), pDeporte),
                fila(new JLabel("Desde (AAAA-MM-DD):"), pDesde, new JLabel("Hasta:"), pHasta,
                        UtilGUI.boton("Actualizar", this, this::actualizarPastel)));
        return contenedor(f, pastel, "Solo cuenta partidos finalizados. Los campos de fecha pueden quedar vacíos.");
    }

    private void actualizarPastel() {
        String sujeto = (String) pSujeto.getSelectedItem();
        if (sujeto == null) {
            pastel.setDatos(pVer.getSelectedIndex() == 0 ? "Resultados por equipo" : "Resultados por jugador", new LinkedHashMap<>());
            return;
        }
        LocalDate desde = fechaOpcional(pDesde, "Desde");
        LocalDate hasta = fechaOpcional(pHasta, "Hasta");
        int victorias = 0;
        int derrotas = 0;
        int empates = 0;

        if (pVer.getSelectedIndex() == 0) {
            int tipo = pTipo.getSelectedIndex(); // 0 todos, 1 internos, 2 competencia
            for (Partido p : club().getPartidos().values()) {
                if (!esFinalizado(p) || !enRango(leerFecha(p.getFecha()), desde, hasta)) {
                    continue;
                }
                // El marcador es "a-b": primero el equipo 1 (partido interno) o el del club (competencia)
                boolean primero;
                if (p instanceof PartidoInterno && (tipo == 0 || tipo == 1)) {
                    PartidoInterno i = (PartidoInterno) p;
                    if (i.getEquipo1().getNombreEquipo().equals(sujeto)) {
                        primero = true;
                    } else if (i.getEquipo2().getNombreEquipo().equals(sujeto)) {
                        primero = false;
                    } else {
                        continue;
                    }
                } else if (p instanceof PartidoCompetencia && (tipo == 0 || tipo == 2)
                        && ((PartidoCompetencia) p).getEquipo().getNombreEquipo().equals(sujeto)) {
                    primero = true;
                } else {
                    continue;
                }
                int[] m = leerMarcador(p.getMarcadorFinal());
                if (m == null) {
                    continue;
                }
                int propios = primero ? m[0] : m[1];
                int rival = primero ? m[1] : m[0];
                if (propios > rival) {
                    victorias++;
                } else if (propios < rival) {
                    derrotas++;
                } else {
                    empates++;
                }
            }
        } else {
            String idJugador = idDe(sujeto);
            String deporte = (String) pDeporte.getSelectedItem();
            boolean todosDeportes = deporte == null || TODOS.equals(deporte);
            for (Partido p : club().getPartidos().values()) {
                if (!(p instanceof PartidoIndividual) || !esFinalizado(p) || !enRango(leerFecha(p.getFecha()), desde, hasta)) {
                    continue;
                }
                PartidoIndividual pi = (PartidoIndividual) p;
                if (!todosDeportes && (pi.getDeporte() == null || !deporte.equals(pi.getDeporte().getNombreDeporte()))) {
                    continue;
                }
                // El marcador individual es "sets del socio 1 - sets del socio 2"
                boolean esUno = pi.getSocio1() != null && pi.getSocio1().getIdUsuario().equals(idJugador);
                boolean esDos = pi.getSocio2() != null && pi.getSocio2().getIdUsuario().equals(idJugador);
                int[] m = leerMarcador(p.getMarcadorFinal());
                if ((!esUno && !esDos) || m == null) {
                    continue;
                }
                int propios = esUno ? m[0] : m[1];
                int rival = esUno ? m[1] : m[0];
                if (propios > rival) {
                    victorias++;
                } else if (propios < rival) {
                    derrotas++;
                } else {
                    empates++;
                }
            }
        }
        Map<String, Double> datos = new LinkedHashMap<>();
        datos.put("Victorias", (double) victorias);
        datos.put("Derrotas", (double) derrotas);
        datos.put("Empates", (double) empates);
        String nombre = pVer.getSelectedIndex() == 0 ? sujeto : sujeto.substring(sujeto.indexOf(" - ") + 3);
        pastel.setDatos("Resultados de " + nombre + " (" + (victorias + derrotas + empates) + (victorias + derrotas + empates == 1 ? " partido finalizado)" : " partidos finalizados)"), datos);
    }

    // =====================================================================================
    // 2. Barras: estadística por jugador de un equipo
    // =====================================================================================

    private JPanel panelBarrasJugadores() {
        alCambiar(bEquipo, () -> {
            cargando = true;
            llenarMetricas();
            cargando = false;
            actualizarBarrasJugadores();
        });
        alCambiar(bMetrica, this::actualizarBarrasJugadores);
        alCambiar(bTipo, () -> {
            habilitarCampos();
            actualizarBarrasJugadores();
        });
        alCambiar(bCompetencia, this::actualizarBarrasJugadores);
        alEnter(bDesde, this::actualizarBarrasJugadores);
        alEnter(bHasta, this::actualizarBarrasJugadores);
        JPanel f = filtros(
                fila(new JLabel("Equipo:"), bEquipo, new JLabel("Estadística:"), bMetrica),
                fila(new JLabel("Partidos:"), bTipo, new JLabel("Competencia:"), bCompetencia,
                        new JLabel("Desde (AAAA-MM-DD):"), bDesde, new JLabel("Hasta:"), bHasta,
                        UtilGUI.boton("Actualizar", this, this::actualizarBarrasJugadores)));
        return contenedor(f, barrasJugadores, "Suma de la estadística de cada jugador del equipo en los partidos que cumplen los filtros.");
    }

    /** Estadísticas que tiene sentido graficar para un equipo: las de su deporte y las que ya tiene registradas. */
    private void llenarMetricas() {
        Metrica anterior = (Metrica) bMetrica.getSelectedItem();
        bMetrica.removeAllItems();
        Equipo equipo = club().getEquipos().get((String) bEquipo.getSelectedItem());
        if (equipo == null) {
            return;
        }
        String deporte = sinTildes(equipo.getDeporteEquipo().getNombreDeporte());
        for (Metrica m : METRICAS) {
            boolean porDeporte = (m.clase == EstadisticaFutbol.class && (deporte.contains("futbol") || deporte.contains("futsal")))
                    || (m.clase == EstadisticaBaloncesto.class && (deporte.contains("baloncesto") || deporte.contains("basket")))
                    || (m.clase == EstadisticaVoleibol.class && (deporte.contains("voleibol") || deporte.contains("voley")));
            if (porDeporte || equipoTieneEstadisticas(equipo, m.clase)) {
                bMetrica.addItem(m);
            }
        }
        if (anterior != null) {
            for (int i = 0; i < bMetrica.getItemCount(); i++) {
                if (bMetrica.getItemAt(i).nombre.equals(anterior.nombre)) {
                    bMetrica.setSelectedIndex(i);
                }
            }
        }
    }

    private boolean equipoTieneEstadisticas(Equipo equipo, Class<? extends Estadistica> clase) {
        for (Estadistica e : club().getEstadisticas()) {
            if (clase.isInstance(e) && equipo.getSocios().contains(e.getSocio())) {
                return true;
            }
        }
        return false;
    }

    private void actualizarBarrasJugadores() {
        Equipo equipo = club().getEquipos().get((String) bEquipo.getSelectedItem());
        Metrica metrica = (Metrica) bMetrica.getSelectedItem();
        if (equipo == null || metrica == null) {
            barrasJugadores.setDatos("Desempeño por jugador", "", new ArrayList<>(), new LinkedHashMap<>());
            return;
        }
        LocalDate desde = fechaOpcional(bDesde, "Desde");
        LocalDate hasta = fechaOpcional(bHasta, "Hasta");
        int tipo = bTipo.getSelectedIndex();
        String competencia = bCompetencia.getSelectedIndex() <= 0 ? null : (String) bCompetencia.getSelectedItem();

        Map<Socio, Integer> totales = new LinkedHashMap<>();
        for (Socio s : equipo.getSocios()) {
            totales.put(s, 0);
        }
        for (Estadistica e : club().getEstadisticas()) {
            Partido p = e.getPartido();
            if (!metrica.clase.isInstance(e) || !totales.containsKey(e.getSocio()) || p == null
                    || !enRango(leerFecha(p.getFecha()), desde, hasta)) {
                continue;
            }
            if (tipo == 1 && !(p instanceof PartidoInterno)) {
                continue;
            }
            if (tipo == 2) {
                if (!(p instanceof PartidoCompetencia)) {
                    continue;
                }
                Competencia c = ((PartidoCompetencia) p).getCompetencia();
                if (competencia != null && (c == null || !competencia.equals(c.getNombreCompetencia()))) {
                    continue;
                }
            }
            totales.merge(e.getSocio(), metrica.valor.applyAsInt(e), Integer::sum);
        }
        List<String> nombres = new ArrayList<>();
        List<Double> valores = new ArrayList<>();
        for (Map.Entry<Socio, Integer> entrada : totales.entrySet()) {
            nombres.add(nombreCompleto(entrada.getKey()));
            valores.add((double) entrada.getValue());
        }
        Map<String, List<Double>> series = new LinkedHashMap<>();
        series.put(metrica.nombre, valores);
        barrasJugadores.setFormatoValor(GraficoBase::formatoNumero);
        barrasJugadores.setDatos(metrica.nombre + " por jugador — " + equipo.getNombreEquipo(), metrica.nombre, nombres, series);
    }

    // =====================================================================================
    // 3. Barras: ingresos de la tienda y la cafetería
    // =====================================================================================

    private JPanel panelIngresos() {
        alCambiar(iTipo, this::actualizarIngresos);
        alCambiar(iCliente, this::actualizarIngresos);
        alEnter(iInicio, this::actualizarIngresos);
        iDias.addChangeListener(e -> conAviso(this::actualizarIngresos));
        JPanel f = filtros(fila(new JLabel("Primer día (AAAA-MM-DD):"), iInicio, new JLabel("Días:"), iDias,
                new JLabel("Ventas:"), iTipo, new JLabel("Cliente:"), iCliente,
                UtilGUI.boton("Actualizar", this, this::actualizarIngresos)));
        return contenedor(f, barrasIngresos,
                "Ingreso neto = subtotal − descuento. No incluye el IVA (19 %) de la tienda, ni el impuesto (8 %) y la propina de la cafetería. "
                + "Si deja vacío el primer día se muestran los últimos días con ventas. El sistema no maneja mensualidades, por eso no aparecen.");
    }

    private boolean clienteCoincide(Usuario u, int filtro) {
        return switch (filtro) {
            case 1 -> u instanceof Socio;
            case 2 -> u instanceof Entrenador;
            case 3 -> u instanceof Fisioterapeuta;
            case 4 -> u instanceof Administrador;
            case 5 -> u instanceof Invitado;
            default -> true;
        };
    }

    private void actualizarIngresos() {
        int dias = (Integer) iDias.getValue();
        LocalDate inicio = fechaOpcional(iInicio, "Primer día");
        if (inicio == null) {
            // por defecto, la ventana termina en la última venta registrada (o en hoy si no hay ventas)
            LocalDate ultima = null;
            for (Venta v : tienda().getVentas()) {
                LocalDate f = leerFecha(v.getFecha());
                if (f != null && (ultima == null || f.isAfter(ultima))) {
                    ultima = f;
                }
            }
            inicio = (ultima == null ? LocalDate.now() : ultima).minusDays(dias - 1L);
        }
        double[] tienda = new double[dias];
        double[] cafeteria = new double[dias];
        int filtroCliente = iCliente.getSelectedIndex();
        for (Venta v : tienda().getVentas()) {
            LocalDate f = leerFecha(v.getFecha());
            if (f == null || f.isBefore(inicio) || !f.isBefore(inicio.plusDays(dias))
                    || (v.getUsuario() != null && !clienteCoincide(v.getUsuario(), filtroCliente))
                    || (v.getUsuario() == null && filtroCliente != 0)) {
                continue;
            }
            int i = (int) java.time.temporal.ChronoUnit.DAYS.between(inicio, f);
            if (Venta.CAFETERIA.equalsIgnoreCase(v.getTipo())) {
                cafeteria[i] += v.getSubTotal();
            } else {
                tienda[i] += v.getSubTotal() - v.getDescuento();
            }
        }
        boolean verTienda = iTipo.getSelectedIndex() != 2;
        boolean verCafeteria = iTipo.getSelectedIndex() != 1;
        List<String> categorias = new ArrayList<>();
        List<Double> vTienda = new ArrayList<>();
        List<Double> vCafeteria = new ArrayList<>();
        for (int i = 0; i < dias; i++) {
            categorias.add(inicio.plusDays(i).format(DIA_MES));
            vTienda.add(verTienda ? tienda[i] : Double.NaN);
            vCafeteria.add(verCafeteria ? cafeteria[i] : Double.NaN);
        }
        // Siempre las mismas dos series en el mismo orden: cada área conserva su color aunque se filtre la otra
        Map<String, List<Double>> series = new LinkedHashMap<>();
        series.put("Tienda deportiva", vTienda);
        series.put("Cafetería", vCafeteria);
        NumberFormat dinero = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO"));
        barrasIngresos.setFormatoValor(v -> "$" + dinero.format(Math.round(v)));
        barrasIngresos.setDatos("Ingresos netos del " + inicio.format(DIA_MES) + " al " + inicio.plusDays(dias - 1L).format(DIA_MES),
                "Pesos (COP), sin impuestos", categorias, series);
    }

    // =====================================================================================
    // 4. Líneas: asistencia semanal a los entrenamientos
    // =====================================================================================

    private JPanel panelLineas() {
        alCambiar(lEquipo, () -> {
            cargando = true;
            llenarJugadoresLinea();
            cargando = false;
            actualizarLineas();
        });
        alCambiar(lJugador, this::actualizarLineas);
        alCambiar(lMetrica, this::actualizarLineas);
        alCambiar(lSemanas, this::actualizarLineas);
        alEnter(lHasta, this::actualizarLineas);
        JPanel f = filtros(
                fila(new JLabel("Equipo:"), lEquipo, new JLabel("Jugador:"), lJugador, new JLabel("Medida:"), lMetrica),
                fila(new JLabel("Semanas:"), lSemanas, new JLabel("Hasta (AAAA-MM-DD):"), lHasta,
                        UtilGUI.boton("Actualizar", this, this::actualizarLineas)));
        return contenedor(f, lineas,
                "Cada punto resume los 7 días que empiezan en la fecha del eje. Si deja vacío \"Hasta\" termina en la última sesión registrada.");
    }

    private void llenarJugadoresLinea() {
        Equipo equipo = club().getEquipos().get((String) lEquipo.getSelectedItem());
        List<String> opciones = new ArrayList<>();
        if (equipo != null) {
            for (Socio s : equipo.getSocios()) {
                opciones.add(opcion(s));
            }
        }
        llenar(lJugador, opciones, "Todos los jugadores");
    }

    private void actualizarLineas() {
        int semanas = (Integer) lSemanas.getSelectedItem();
        LocalDate fin = fechaOpcional(lHasta, "Hasta");
        if (fin == null) {
            for (AsistenciaEvaluacion a : club().getAsistenciaEvaluaciones()) {
                LocalDate f = fechaSesion(a);
                if (f != null && (fin == null || f.isAfter(fin))) {
                    fin = f;
                }
            }
            if (fin == null) {
                fin = LocalDate.now();
            }
        }
        LocalDate inicio = fin.minusDays(7L * semanas - 1);

        Equipo equipo = club().getEquipos().get((String) lEquipo.getSelectedItem());
        String jugador = lJugador.getSelectedIndex() <= 0 ? null : idDe((String) lJugador.getSelectedItem());

        int[][] conteo = new int[semanas][3]; // asistió, llegó tarde, faltó
        double[] sumaNotas = new double[semanas];
        int[] nNotas = new int[semanas];
        for (AsistenciaEvaluacion a : club().getAsistenciaEvaluaciones()) {
            LocalDate f = fechaSesion(a);
            if (f == null || f.isBefore(inicio) || f.isAfter(fin) || a.getSocio() == null) {
                continue;
            }
            if (equipo != null && !equipo.getSocios().contains(a.getSocio())) {
                continue;
            }
            if (jugador != null && !jugador.equals(a.getSocio().getIdUsuario())) {
                continue;
            }
            int semana = (int) (java.time.temporal.ChronoUnit.DAYS.between(inicio, f) / 7);
            String estado = a.getEstadoAsistencia();
            if ("Asistió".equals(estado)) {
                conteo[semana][0]++;
            } else if ("Llegó tarde".equals(estado)) {
                conteo[semana][1]++;
            } else if ("Faltó".equals(estado)) {
                conteo[semana][2]++;
            }
            if (a.getNotaDesempeno() > 0) {
                sumaNotas[semana] += a.getNotaDesempeno();
                nNotas[semana]++;
            }
        }
        List<String> categorias = new ArrayList<>();
        for (int s = 0; s < semanas; s++) {
            categorias.add(inicio.plusDays(7L * s).format(DIA_MES));
        }
        Map<String, List<Double>> series = new LinkedHashMap<>();
        String sujeto = equipo == null ? "todos los equipos" : equipo.getNombreEquipo();
        if (jugador != null) {
            sujeto = nombreCompleto(club().getUsuarios().get(jugador));
        }
        if (lMetrica.getSelectedIndex() == 0) {
            String[] nombres = {"Asistió", "Llegó tarde", "Faltó"};
            for (int e = 0; e < 3; e++) {
                List<Double> valores = new ArrayList<>();
                for (int s = 0; s < semanas; s++) {
                    int total = conteo[s][0] + conteo[s][1] + conteo[s][2];
                    valores.add(total == 0 ? Double.NaN : 100.0 * conteo[s][e] / total);
                }
                series.put(nombres[e], valores);
            }
            lineas.setFormatoValor(v -> GraficoBase.formatoNumero(v) + " %");
            lineas.setDatos("Asistencia a entrenamientos — " + sujeto, "Porcentaje de las asistencias registradas en la semana", categorias, series);
        } else {
            List<Double> valores = new ArrayList<>();
            for (int s = 0; s < semanas; s++) {
                valores.add(nNotas[s] == 0 ? Double.NaN : sumaNotas[s] / nNotas[s]);
            }
            series.put("Nota promedio", valores);
            lineas.setFormatoValor(GraficoBase::formatoNumero);
            lineas.setDatos("Nota de desempeño promedio — " + sujeto, "Nota de 1 a 5 (las asistencias sin nota no cuentan)", categorias, series);
        }
    }

    private LocalDate fechaSesion(AsistenciaEvaluacion a) {
        SesionEntrenamiento s = a.getSesionEntrenamiento();
        return s == null ? null : leerFecha(s.getFecha());
    }

    // =====================================================================================
    // 5. Complementaria: posiciones de una competencia y ranking individual
    // =====================================================================================

    private JPanel panelRanking() {
        alCambiar(rVer, () -> {
            cargando = true;
            llenarObjetivoRanking();
            cargando = false;
            habilitarCampos();
            actualizarRanking();
        });
        alCambiar(rObjetivo, this::actualizarRanking);
        alCambiar(rNivel, this::actualizarRanking);
        JPanel f = filtros(fila(new JLabel("Ver:"), rVer, new JLabel("Competencia / deporte:"), rObjetivo,
                new JLabel("Nivel:"), rNivel, UtilGUI.boton("Actualizar", this, this::actualizarRanking)));
        return contenedor(f, barrasRanking, "Los puntos de una competencia se suman al registrar el marcador de un partido de competencia; el ranking individual cambia al registrar resultados de partidos individuales.");
    }

    private void llenarObjetivoRanking() {
        llenar(rObjetivo, rVer.getSelectedIndex() == 0 ? nombresCompetencias() : nombresDeportes(true), null);
    }

    private void actualizarRanking() {
        String objetivo = (String) rObjetivo.getSelectedItem();
        List<String> nombres = new ArrayList<>();
        List<Double> valores = new ArrayList<>();
        String titulo;
        String ejeY;
        String serie;
        if (rVer.getSelectedIndex() == 0) {
            Competencia c = objetivo == null ? null : club().getCompetencias().get(objetivo);
            titulo = "Puntos por equipo" + (c == null ? "" : " — " + c.getNombreCompetencia());
            ejeY = "Puntos acumulados en la competencia";
            serie = "Puntos";
            if (c != null) {
                List<InscripcionCompetencia> orden = new ArrayList<>(c.getInscripcionesCompetencia());
                orden.sort((a, b) -> Integer.compare(b.getPuntosAcumulados(), a.getPuntosAcumulados()));
                for (InscripcionCompetencia i : orden) {
                    nombres.add(i.getEquipo().getNombreEquipo());
                    valores.add((double) i.getPuntosAcumulados());
                }
            }
        } else {
            Deporte d = objetivo == null ? null : club().getDeportes().get(objetivo);
            titulo = "Ranking individual" + (d == null ? "" : " — " + d.getNombreDeporte());
            ejeY = "Puntaje de ranking";
            serie = "Puntaje";
            if (d != null) {
                String nivel = rNivel.getSelectedIndex() <= 0 ? null : (String) rNivel.getSelectedItem();
                List<InscripcionIndividual> orden = new ArrayList<>();
                for (InscripcionIndividual i : club().getInscripcionesIndividual()) {
                    if (i.getDeporteIndividual() == d && (nivel == null || nivel.equals(i.getNivel()))) {
                        orden.add(i);
                    }
                }
                orden.sort((a, b) -> Double.compare(b.getPuntajeRanking(), a.getPuntajeRanking()));
                for (InscripcionIndividual i : orden) {
                    nombres.add(nombreCompleto(i.getSocio()));
                    valores.add(i.getPuntajeRanking());
                }
            }
        }
        Map<String, List<Double>> series = new LinkedHashMap<>();
        series.put(serie, valores);
        barrasRanking.setFormatoValor(GraficoBase::formatoNumero);
        barrasRanking.setDatos(titulo, ejeY, nombres, series);
    }
}
