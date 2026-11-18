package Interfaz;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import javax.swing.JPanel;
import Deportes.Equipo;
import Deportes.InscripcionIndividual;
import Usuarios.Socio;

/** Equipos, deportes individuales y ranking del socio. */
public class PanelSocioEquipos extends PanelBase {

    private final PanelTabla tEquipos = new PanelTabla("Equipos del club", "Nombre", "Deporte", "Edad máxima", "Cupos", "Posiciones");
    private final PanelTabla tRanking = new PanelTabla("Mi nivel y ranking", "Deporte", "Nivel", "Puntaje de ranking");

    public PanelSocioEquipos(ControladorGUI controlador) {
        super(controlador);
        JPanel centro = new JPanel(new GridLayout(2, 1, 8, 8));

        JPanel arriba = new JPanel(new BorderLayout(4, 4));
        arriba.add(tEquipos, BorderLayout.CENTER);
        arriba.add(UtilGUI.barra(UtilGUI.boton("Inscribirme al equipo seleccionado", this, this::inscribirEquipo)), BorderLayout.SOUTH);

        JPanel abajo = new JPanel(new BorderLayout(4, 4));
        abajo.add(tRanking, BorderLayout.CENTER);
        abajo.add(UtilGUI.barra(UtilGUI.boton("Inscribirme a un deporte individual", this, this::inscribirIndividual)), BorderLayout.SOUTH);

        centro.add(arriba);
        centro.add(abajo);
        add(centro, BorderLayout.CENTER);
        refrescar();
    }

    @Override
    protected void refrescar() {
        tEquipos.limpiar();
        for (Equipo e : club().getEquipos().values()) {
            tEquipos.agregar(e.getNombreEquipo(), e.getDeporteEquipo().getNombreDeporte(), e.getCategoriaEdad(),
                    e.getSocios().size() + "/" + e.getCupoMaximo(), String.valueOf(e.getDeporteEquipo().getPosiciones()));
        }
        tRanking.limpiar();
        Socio socio = (Socio) usuario();
        for (InscripcionIndividual i : socio.getInscripcionesIndividuales()) {
            tRanking.agregar(i.getDeporteIndividual().getNombreDeporte(), i.getNivel(), i.getPuntajeRanking());
        }
    }

    private void inscribirEquipo() {
        String nombre = tEquipos.requerirSeleccion(0, "Seleccione un equipo de la tabla.");
        Equipo equipo = club().getEquipos().get(nombre);
        Formulario f = new Formulario("Inscripción al equipo " + nombre)
                .combo("posicion", "Posición", new ArrayList<String>(equipo.getDeporteEquipo().getPosiciones()))
                .entero("numero", "Número de camiseta", 1, 0, 99);
        if (!f.mostrar(this)) {
            return;
        }
        String posicion = f.valor("posicion");
        int numero = f.entero("numero");
        club().realizarInscripcionEquipo(usuario().getIdUsuario(), nombre, posicion, numero);
        guardar();
        refrescar();
        Equipo elegido = club().getEquipos().get(nombre);
        info("Inscripción registrada: dorsal " + numero + ", posición " + posicion + ". Cupos libres en el equipo: "
                + (elegido.getCupoMaximo() - elegido.getSocios().size()) + ".");
    }

    private void inscribirIndividual() {
        Formulario f = new Formulario("Inscripción a un deporte individual")
                .combo("deporte", "Deporte", nombresDeportes(true));
        if (!f.mostrar(this)) {
            return;
        }
        f.requerir("deporte");
        club().realizarInscripcionIndividual(usuario().getIdUsuario(), f.valor("deporte"));
        guardar();
        refrescar();
        info("Inscripción registrada con nivel Principiante.");
    }
}
