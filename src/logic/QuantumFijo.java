package logic;

import models.Proceso;

public class QuantumFijo implements PoliticaQuantum {
    
    private int quantum;
    
    public QuantumFijo(int quantum){
        this.quantum = quantum;
    }

    @Override
    public int obtenerQuantum(Proceso proceso){
        return quantum;
    }
}
