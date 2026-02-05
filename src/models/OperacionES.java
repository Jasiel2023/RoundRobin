package models;

public class OperacionES {
    private final int momentoCPU;   // ms acumulados de CPU cuando se dispara
    private final int duracion;      // duración de la E/S
    private boolean ejecutada = false;

    public OperacionES(int momentoCPU, int duracion) {
        this.momentoCPU = momentoCPU;
        this.duracion = duracion;
    }

    public int getMomentoCPU() {
        return momentoCPU;
    }

    public int getDuracion() {
        return duracion;
    }

    public boolean isEjecutada() {
        return ejecutada;
    }

    public void marcarEjecutada() {
        this.ejecutada = true;
    }

    public void resetear() {
        this.ejecutada = false;
    }
}
