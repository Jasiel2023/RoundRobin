package models;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;

public class Planificador {


    private Queue<Proceso> colaListos;
    private List<Proceso> colaBloqueados;
    private List<Proceso> colaTerminados;
    private List<Proceso> colaNuevos;

    private Proceso procesoEnCPU;
    private Float tiempoActual;
    private int contadorQuantum;

    public Planificador(Queue<Proceso> colaListos, List<Proceso> colaBloqueados, List<Proceso> colaTerminados, List<Proceso> colaNuevos, Proceso procesoEnCPU, Float tiempoActual, int contadorQuantum) {
        this.colaListos = colaListos;
        this.colaBloqueados = colaBloqueados;
        this.colaTerminados = colaTerminados;
        this.colaNuevos = colaNuevos;
        this.procesoEnCPU = null;
        this.tiempoActual = 0f;
        this.contadorQuantum = 0;
    }

    public void agregarProcesoAListos(Proceso proceso) {
        if (proceso.getTiempoLlegada() == 0) {
            colaListos.add(proceso);
        }else{
            colaNuevos.add(proceso);
        }
    }

    public void ejecutarPaso() {
         verificarLlegadas();
         gestionarBloqueados();
         if (procesoEnCPU != null) {
            procesarCPU();
         } else{
            despachadorProceso();
         }

         tiempoActual++;
    }

    private void verificarLlegadas() {
        Iterator<Proceso> it = colaNuevos.iterator();
        while (it.hasNext()) {
            Proceso proceso = it.next();
            if (proceso.getTiempoLlegada() <= tiempoActual) {
                colaListos.add(proceso);
                it.remove();
            }
        }
    }

    private void gestionarBloqueados() {
        if (colaBloqueados.isEmpty()) {
            return;
        }
        Iterator<Proceso> it = colaBloqueados.iterator();

        while (it.hasNext()) {
            Proceso p = it.next();
            p.setTiempoEnES(p.getTiempoEnES() + 1);

            if (p.getTiempoEnES() >= p.getDuracionES()) {
                p.setTiempoEnES(0f);
                colaListos.add(p);
                it.remove();
            }
        }
    }

    private void despachadorProceso() {
        if (!colaListos.isEmpty()) {
            procesoEnCPU = colaListos.poll();
            contadorQuantum = 0;
        }
    }

    private void procesarCPU() {
        // Lógica para procesar el proceso en CPU

        procesoEnCPU.setTiempoRestante(procesoEnCPU.getTiempoRestante() - 1);
        procesoEnCPU.setTiempoEjecutado(procesoEnCPU.getTiempoEjecutado() + 1);
        contadorQuantum++;

        if (procesoEnCPU.esTerminado()) {
            colaTerminados.add(procesoEnCPU);
            procesoEnCPU = null;
            return;
        }

        if (procesoEnCPU.debeHacerES()) {
            colaBloqueados.add(procesoEnCPU);
            procesoEnCPU = null;
            return;
        }

        if (contadorQuantum >= procesoEnCPU.getQuantum()) {
            colaListos.add(procesoEnCPU);
            procesoEnCPU = null;
            return;
        }
    }

    public Boolean esFinSimulacion(){
        return colaListos.isEmpty() && 
        colaBloqueados.isEmpty() && 
        colaNuevos.isEmpty() && 
        procesoEnCPU == null;
    }
    
    public Queue<Proceso> getColaListos() {
        return this.colaListos;
    }

    public void setColaListos(Queue<Proceso> colaListos) {
        this.colaListos = colaListos;
    }

    public List<Proceso> getColaBloqueados() {
        return this.colaBloqueados;
    }

    public void setColaBloqueados(List<Proceso> colaBloqueados) {
        this.colaBloqueados = colaBloqueados;
    }

    public List<Proceso> getColaTerminados() {
        return this.colaTerminados;
    }

    public void setColaTerminados(List<Proceso> colaTerminados) {
        this.colaTerminados = colaTerminados;
    }

    public List<Proceso> getColaNuevos() {
        return this.colaNuevos;
    }

    public void setColaNuevos(List<Proceso> colaNuevos) {
        this.colaNuevos = colaNuevos;
    }

    public Proceso getProcesoEnCPU() {
        return this.procesoEnCPU;
    }

    public void setProcesoEnCPU(Proceso procesoEnCPU) {
        this.procesoEnCPU = procesoEnCPU;
    }

    public Float getTiempoActual() {
        return this.tiempoActual;
    }

    public void setTiempoActual(Float tiempoActual) {
        this.tiempoActual = tiempoActual;
    }

    public int getContadorQuantum() {
        return this.contadorQuantum;
    }

    public void setContadorQuantum(int contadorQuantum) {
        this.contadorQuantum = contadorQuantum;
    }

}
