package logic;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import models.EnumEstadoProceso;
import models.Proceso;

public class PlanificadorFIFO {

    private int tiempoActual;

    private final  List<Proceso> procesosPendientes; // aún no llegan
    private final Queue<Proceso> colaListos;         // FIFO
    private final List<Proceso> historialCPU;
    private Proceso procesoActual;              // en CPU

    public PlanificadorFIFO(List<Proceso> procesos) {
        this.tiempoActual = 0;
        this.procesosPendientes = new LinkedList<>(procesos);
        this.colaListos = new LinkedList<>();
        this.procesoActual = null;
        this.historialCPU = new LinkedList<>();
    }

    public int getTiempoActual() {
        return tiempoActual;
    }
    
    public List<Proceso> getHistorialCPU() {
    return historialCPU;
    }

    
    
    
    public void tick() {
        // 1. Verificar procesos que llegan
        for (Proceso p : procesosPendientes) {
            if (p.getTiempoLlegada() == tiempoActual) {
                p.setEstado(EnumEstadoProceso.LISTO);
                colaListos.add(p);
            }
        }

        procesosPendientes.removeIf(
            p -> p.getTiempoLlegada() == tiempoActual
        );

        // 2. Si no hay proceso en CPU, tomar uno
        if (procesoActual == null && !colaListos.isEmpty()) {
            procesoActual = colaListos.poll();
            procesoActual.setEstado(EnumEstadoProceso.EJECUTANDO);        
            historialCPU.add(procesoActual); // dentro del tick SIEMPRE

        }

        // 3. Ejecutar proceso actual
        if (procesoActual != null) {
            procesoActual.setTiempoRestante(
                procesoActual.getTiempoRestante() - 1
            );

            // 4. Si terminó
            if (procesoActual.getTiempoRestante() == 0) {
                procesoActual.setEstado(EnumEstadoProceso.TERMINADO);
                procesoActual = null;
            }
        }
        // 5. Avanzar tiempo
        tiempoActual++;
    }

    public Proceso getProcesoActual() {
        return procesoActual;
    }

    public boolean haTerminado() {
        return procesoActual == null 
            && colaListos.isEmpty() 
            && procesosPendientes.isEmpty();
    }


}