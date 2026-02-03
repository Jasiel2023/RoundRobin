package logic;

import models.Proceso;

public class QuantumPorProceso implements PoliticaQuantum {

    @Override
    public int obtenerQuantum(Proceso proceso) {
        return proceso.getQuantumPersonal();
    }
}