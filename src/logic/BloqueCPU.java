package logic;


import models.Proceso;

public class BloqueCPU {
    public Proceso proceso;
    public int duracion;

    public BloqueCPU(Proceso proceso) {
        this.proceso = proceso;
        this.duracion = 0;
    }
}