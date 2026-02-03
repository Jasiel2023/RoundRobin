 package models;

import logic.PoliticaQuantum;

public class Proceso {

    private static int contador = 1;

    private final int id;
    private final int tiempoLlegada;
    private final int rafagaCPU;
    private int tiempoRestante;
    private int tiempoInicio = -1;
    private int tiempoFin = -1;
    private EnumEstadoProceso estado;
    private int quantumPersonal = -1;
    
    // ======================
    // CAMBIOS PARA UNA SOLA E/S
    // ======================
    private int momentoES;           // Después de cuántos ms de EJECUCIÓN hace E/S
    private int duracionES;          // Cuánto dura la E/S
    private int tiempoEjecutadoAcumulado = 0; // Total ms ejecutados en CPU
    private int tiempoESRestante = 0; // Tiempo restante en E/S actual
    private boolean enES = false;    // Si está en E/S ahora
    private boolean esEjecutada = false; // Si YA se ejecutó la E/S (solo una vez)

    // ======================
    // CONSTRUCTORES
    // ======================
    
    // Constructor SIMPLE (UI) - SIN E/S
    public Proceso(int tiempoLlegada, int rafagaCPU) {
        this.id = contador++;
        this.tiempoLlegada = tiempoLlegada;
        this.rafagaCPU = rafagaCPU;
        this.tiempoRestante = rafagaCPU;
        this.estado = EnumEstadoProceso.NUEVO;
        this.momentoES = 0;          // Sin E/S por defecto
        this.duracionES = 0;
    }
    
    // Constructor CON E/S (UNA SOLA)
    public Proceso(int tiempoLlegada, int rafagaCPU, int momentoES, int duracionES) {
        this.id = contador++;
        this.tiempoLlegada = tiempoLlegada;
        this.rafagaCPU = rafagaCPU;
        this.tiempoRestante = rafagaCPU;
        this.estado = EnumEstadoProceso.NUEVO;
        this.momentoES = momentoES;
        this.duracionES = duracionES;
    }

    // ======================
    // GETTERS Y SETTERS BASE
    // ======================
    public int getId() { return id; }
    public int getTiempoLlegada() { return tiempoLlegada; }
    public int getRafagaCPU() { return rafagaCPU; }
    public int getTiempoRestante() { return tiempoRestante; }
    public void setTiempoRestante(int tiempoRestante) { this.tiempoRestante = tiempoRestante; }
    public EnumEstadoProceso getEstado() { return estado; }
    public void setEstado(EnumEstadoProceso estado) { this.estado = estado; }
    public int getTiempoInicio() { return tiempoInicio; }
    public void setTiempoInicio(int tiempoInicio) { this.tiempoInicio = tiempoInicio; }
    public int getTiempoFin() { return tiempoFin; }
    public void setTiempoFin(int tiempoFin) { this.tiempoFin = tiempoFin; }
    public int getQuantumPersonal() { return quantumPersonal; }
    public void setQuantumPersonal(int quantumPersonal) { this.quantumPersonal = quantumPersonal; }
    
    // ======================
    // GETTERS Y SETTERS PARA E/S (UNA SOLA)
    // ======================
    public int getMomentoES() { return momentoES; }
    public void setMomentoES(int momentoES) { this.momentoES = momentoES; }
    
    public int getDuracionES() { return duracionES; }
    public void setDuracionES(int duracionES) { this.duracionES = duracionES; }
    
    public int getTiempoEjecutadoAcumulado() { return tiempoEjecutadoAcumulado; }
    public void setTiempoEjecutadoAcumulado(int tiempoEjecutadoAcumulado) { 
        this.tiempoEjecutadoAcumulado = tiempoEjecutadoAcumulado; 
    }
    
    public int getTiempoESRestante() { return tiempoESRestante; }
    public void setTiempoESRestante(int tiempoESRestante) { 
        this.tiempoESRestante = tiempoESRestante; 
    }
    
    public boolean isEnES() { return enES; }
    public void setEnES(boolean enES) { this.enES = enES; }
    
    public boolean isEsEjecutada() { return esEjecutada; }
    public void setEsEjecutada(boolean esEjecutada) { this.esEjecutada = esEjecutada; }
    
    // ======================
    // MÉTODOS DE LÓGICA E/S (UNA SOLA)
    // ======================
    
    /**
     * Verifica si el proceso debe ir a E/S en este momento
     * Condición: No está ya en E/S, NO se ha ejecutado antes, y ha ejecutado suficiente tiempo
     */
    public boolean debeIrAES() {
        return !enES && 
               !esEjecutada &&         // Solo si NO se ha ejecutado antes
               momentoES > 0 &&        // Tiene E/S configurada
               tiempoEjecutadoAcumulado >= momentoES; // Ha ejecutado suficiente
    }
    
    /**
     * Inicia la operación de E/S (solo una vez)
     */
    public void iniciarES() {
        this.enES = true;
        this.esEjecutada = true;       // Marcar como ejecutada (solo una vez)
        this.tiempoESRestante = duracionES;
        this.estado = EnumEstadoProceso.BLOQUEADO_ES;
    }
    
    /**
     * Actualiza el tiempo de E/S (se llama cada tick cuando está en E/S)
     * @return true si terminó la E/S, false si aún continúa
     */
    public boolean tickES() {
        if (tiempoESRestante > 0) {
            tiempoESRestante--;
            if (tiempoESRestante == 0) {
                terminarES();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Termina la operación de E/S
     */
    public void terminarES() {
        this.enES = false;
        this.estado = EnumEstadoProceso.LISTO;
    }
    
    /**
     * Incrementa el tiempo ejecutado acumulado
     */
    public void incrementarTiempoEjecutado() {
        this.tiempoEjecutadoAcumulado++;
    }
    
    /**
     * Verifica si el proceso tiene E/S configurada
     */
    public boolean tieneES() {
        return momentoES > 0 && duracionES > 0;
    }

    // ======================
    // UTILIDAD
    // ======================
    public boolean estaTerminado() {
        return tiempoRestante <= 0;
    }

    @Override
    public String toString() {
        String esInfo = tieneES() ? 
            String.format(", E/S=después de %dms por %dms", momentoES, duracionES) : 
            ", sin E/S";
            
        return "P" + id +
               " (llegada=" + tiempoLlegada +
               ", rafaga=" + rafagaCPU +
               ", estado=" + estado + esInfo + ")";
    }

    public int getQuantumUsado(PoliticaQuantum politica) {
        return politica.obtenerQuantum(this);
    }
}