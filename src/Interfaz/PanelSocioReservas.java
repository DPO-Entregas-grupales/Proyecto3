package Interfaz;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;
import ReservasInstalaciones.Instalacion;
import ReservasInstalaciones.Reserva;

/** Reservas de instalaciones del socio y registro de ingreso a la instalación reservada. */
public class PanelSocioReservas extends PanelBase {

    private final PanelTabla tInstalaciones = new PanelTabla("Instalaciones", "Nombre", "Tipo", "Deporte", "Capacidad");
    private final PanelTabla tReservas = new PanelTabla("Mis reservas", "ID", "Instalación", "Fecha", "Hora de inicio");

    public PanelSocioReservas(ControladorGUI controlador) {
        super(controlador);
        JPanel centro = new JPanel(new GridLayout(2, 1, 8, 8));

        JPanel arriba = new JPanel(new java.awt.BorderLayout(4, 4));
        arriba.add(tInstalaciones, java.awt.BorderLayout.CENTER);
        arriba.add(UtilGUI.barra(UtilGUI.boton("Reservar la instalación seleccionada", this, this::reservar)), java.awt.BorderLayout.SOUTH);

        JPanel abajo = new JPanel(new java.awt.BorderLayout(4, 4));
        abajo.add(tReservas, java.awt.BorderLayout.CENTER);
        abajo.add(UtilGUI.barra(UtilGUI.boton("Ingresar a la instalación de la reserva seleccionada", this, this::ingresar)), java.awt.BorderLayout.SOUTH);

        centro.add(arriba);
        centro.add(abajo);
        add(centro, java.awt.BorderLayout.CENTER);
        refrescar();
    }

    @Override
    protected void refrescar() {
        tInstalaciones.limpiar();
        List<Instalacion> ordenadas = new java.util.ArrayList<>(club().getInstalaciones().values());
        ordenadas.sort(java.util.Comparator.comparing(Instalacion::getNombre));
        for (Instalacion i : ordenadas) {
            String deporte = i.getDeporte() == null ? "sin disciplina" : i.getDeporte().getNombreDeporte();
            tInstalaciones.agregar(i.getNombre(), i.getTipo(), deporte, i.getCapacidadMaximaJugadores());
        }
        tReservas.limpiar();
        List<Reserva> mias = new java.util.ArrayList<>(club().getReservas().values());
        mias.sort(java.util.Comparator.comparing(Reserva::getFecha).reversed()
                .thenComparing((a, b) -> Integer.compare(numeroDe(b.getHoraInicio()), numeroDe(a.getHoraInicio()))));
        for (Reserva r : mias) {
            if (r.getSocio() == usuario()) {
                tReservas.agregar(r.getIdReserva(), r.getInstalacion().getNombre(), r.getFecha(), r.getHoraInicio() + "h");
            }
        }
    }

    private void reservar() {
        String inst = tInstalaciones.requerirSeleccion(0, "Seleccione una instalación de la tabla.");
        List<String> horas = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            horas.add(String.valueOf(h));
        }
        Formulario f = new Formulario("Reservar " + inst)
                .fecha("fecha", "Fecha")
                .combo("hora", "Hora de inicio (0-23)", horas)
                .entero("cant", "Cantidad de jugadores", 2, 1, 100)
                .combo("mod", "Modalidad", Arrays.asList("Sencillos", "Dobles"));
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("mod");
        String idReserva = controlador.generarIdReserva();
        String fecha = f.fechaValor("fecha");
        String hora = f.valor("hora");
        club().realizarReserva(idReserva, usuario().getIdUsuario(), inst, fecha, hora, f.entero("cant"), f.valor("mod"));
        guardar();
        refrescar();

        String mensaje = "Reserva realizada (ID: " + idReserva + ").";
        Instalacion elegida = club().getInstalaciones().get(inst);
        if (elegida.getDeporte() != null) {
            int libres = club().cuposSimultaneosLibres(elegida.getDeporte(), fecha, hora);
            if (libres >= 0) {
                mensaje += "\nCupos simultáneos libres de " + elegida.getDeporte().getNombreDeporte() + " en ese bloque: " + libres + ".";
            }
        }
        info(mensaje);
    }

    private void ingresar() {
        String idReserva = tReservas.requerirSeleccion(0, "Seleccione una reserva de la tabla.");
        Formulario f = new Formulario("Ingreso a la instalación").casilla("caliente", "¿Lleva consigo una bebida caliente?");
        if (!f.mostrar(this)) {
            return;
        }
        try {
            club().ingresarAInstalacion(usuario().getIdUsuario(), idReserva, f.marcado("caliente"));
            info("Ingreso permitido. ¡Buen juego!");
        } catch (IllegalArgumentException e) {
            UtilGUI.error(this, "Ingreso denegado: " + e.getMessage());
        }
    }
}
