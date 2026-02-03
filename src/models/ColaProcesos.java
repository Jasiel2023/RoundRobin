package models;

import java.util.LinkedList;
import java.util.Queue;

public class ColaProcesos {

    private Queue<Proceso> cola;

    public ColaProcesos() {
        cola = new LinkedList<>();
    }

    // Agregado desde la UI
    public void agregarProceso(Proceso proceso) {
        proceso.setEstado(EnumEstadoProceso.LISTO);
        cola.add(proceso);
    }

    // FIFO: primero en entrar, primero en salir
    public Proceso obtenerSiguiente() {
        return cola.poll();
    }

    public boolean estaVacia() {
        return cola.isEmpty();
    }

    public Queue<Proceso> getCola() {
        return cola;
    }
}
