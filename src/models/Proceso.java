package models;

import java.util.ArrayList;
import java.util.List;
import logic.PoliticaQuantum;

public class Proceso {

    private static int contador = 1;

    private final int id;
    private final int tiempoLlegada;
    private final int rafagaCPU;
    private final String momentosESOriginal;
    private final String duracionesESOriginal;

    private int tiempoRestante;
    private int tiempoInicio = -1;
    private int tiempoFin = -1;
    private EnumEstadoProceso estado;
    private int quantumPersonal = -1;

    // ======================
    // SOPORTE PARA MÚLTIPLES E/S
    // ======================
    private List<OperacionES> operacionesES = new ArrayList<>();
    private int indiceESActual = 0; // Qué E/S toca ejecutar

    private int tiempoEjecutadoAcumulado = 0;
    private int tiempoESRestante = 0;
    private boolean enES = false;
    private boolean fueBloqueadoAlgunaVez = false; // Bandera para marcar si pasó por E/S

    // En Proceso.java

    // ======================
    // CONSTRUCTORES
    // ======================

    // Sin E/S
    public Proceso(int tiempoLlegada, int rafagaCPU, String momentosESOriginal, String duracionesESOriginal) {
        this.id = contador++;
        this.tiempoLlegada = tiempoLlegada;
        this.rafagaCPU = rafagaCPU;
        this.tiempoRestante = rafagaCPU;
        this.estado = EnumEstadoProceso.NUEVO;
        this.momentosESOriginal = momentosESOriginal;
        this.duracionesESOriginal = duracionesESOriginal;
    }

    public Proceso(int tiempoLlegada, int rafagaCPU, List<OperacionES> operacionesES) {
        this.id = contador++;
        this.tiempoLlegada = tiempoLlegada;
        this.rafagaCPU = rafagaCPU;
        this.tiempoRestante = rafagaCPU;
        this.estado = EnumEstadoProceso.NUEVO;

        if (operacionesES != null) {
            this.operacionesES.addAll(operacionesES);
        }

        // Guardar versión "de presentación"
        if (operacionesES == null || operacionesES.isEmpty()) {
            this.momentosESOriginal = "No";
            this.duracionesESOriginal = "-";
        } else {
            this.momentosESOriginal = operacionesES.stream()
                    .map(op -> String.valueOf(op.getMomentoCPU()))
                    .reduce((a, b) -> a + "-" + b)
                    .orElse("No");

            this.duracionesESOriginal = operacionesES.stream()
                    .map(op -> String.valueOf(op.getDuracion()))
                    .reduce((a, b) -> a + "-" + b)
                    .orElse("-");
        }
    }

    // Constructor para edición (conserva el ID original)
    public Proceso(int idOriginal, int tiempoLlegada, int rafagaCPU, List<OperacionES> operacionesES) {
        this.id = idOriginal; // Usar el ID original, no incrementar contador
        this.tiempoLlegada = tiempoLlegada;
        this.rafagaCPU = rafagaCPU;
        this.tiempoRestante = rafagaCPU;
        this.estado = EnumEstadoProceso.NUEVO;

        if (operacionesES != null) {
            this.operacionesES.addAll(operacionesES);
        }

        // Guardar versión "de presentación"
        if (operacionesES == null || operacionesES.isEmpty()) {
            this.momentosESOriginal = "No";
            this.duracionesESOriginal = "-";
        } else {
            this.momentosESOriginal = operacionesES.stream()
                    .map(op -> String.valueOf(op.getMomentoCPU()))
                    .reduce((a, b) -> a + "-" + b)
                    .orElse("No");

            this.duracionesESOriginal = operacionesES.stream()
                    .map(op -> String.valueOf(op.getDuracion()))
                    .reduce((a, b) -> a + "-" + b)
                    .orElse("-");
        }
    }

    // ======================
    // GETTERS / SETTERS BASE
    // ======================
    public int getId() {
        return id;
    }

    public int getTiempoLlegada() {
        return tiempoLlegada;
    }

    public int getRafagaCPU() {
        return rafagaCPU;
    }

    public int getTiempoRestante() {
        return tiempoRestante;
    }

    public void setTiempoRestante(int tiempoRestante) {
        this.tiempoRestante = tiempoRestante;
    }

    public EnumEstadoProceso getEstado() {
        return estado;
    }

    public void setEstado(EnumEstadoProceso estado) {
        this.estado = estado;
    }

    public int getTiempoInicio() {
        return tiempoInicio;
    }

    public void setTiempoInicio(int tiempoInicio) {
        this.tiempoInicio = tiempoInicio;
    }

    public int getTiempoFin() {
        return tiempoFin;
    }

    public void setTiempoFin(int tiempoFin) {
        this.tiempoFin = tiempoFin;
    }

    public int getQuantumPersonal() {
        return quantumPersonal;
    }

    public void setQuantumPersonal(int quantumPersonal) {
        this.quantumPersonal = quantumPersonal;
    }

    public String getMomentosESOriginal() {
        return momentosESOriginal;
    }

    public String getDuracionesESOriginal() {
        return duracionesESOriginal;
    }

    public int getTiempoEjecutadoAcumulado() {
        return tiempoEjecutadoAcumulado;
    }

    // ======================
    // LÓGICA DE E/S MÚLTIPLE
    // ======================

    /**
     * Verifica si debe entrar a E/S según el tiempo de CPU acumulado
     */
    public boolean debeIrAES() {
        if (enES)
            return false;
        if (indiceESActual >= operacionesES.size())
            return false;

        OperacionES op = operacionesES.get(indiceESActual);
        return !op.isEjecutada() && tiempoEjecutadoAcumulado >= op.getMomentoCPU();
    }

    /**
     * Inicia la siguiente E/S
     */
    public void iniciarES() {
        if (indiceESActual >= operacionesES.size())
            return;

        OperacionES op = operacionesES.get(indiceESActual);
        op.marcarEjecutada();

        this.enES = true;
        this.tiempoESRestante = op.getDuracion();
        this.estado = EnumEstadoProceso.BLOQUEADO_ES;
        this.fueBloqueadoAlgunaVez = true; // Marcar que ha sido bloqueado
    }

    /**
     * Tick de E/S
     * 
     * @return true si terminó la E/S
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
     * Termina la E/S actual
     */
    public void terminarES() {
        this.enES = false;
        this.estado = EnumEstadoProceso.LISTO;
        this.indiceESActual++; // Pasamos a la siguiente E/S
    }

    /**
     * Se llama cada vez que ejecuta 1 ms en CPU
     */
    public void incrementarTiempoEjecutado() {
        this.tiempoEjecutadoAcumulado++;
    }

    public boolean tieneES() {
        return !operacionesES.isEmpty();
    }

    // ======================
    // UTILIDAD
    // ======================
    public boolean estaTerminado() {
        return tiempoRestante <= 0;
    }

    @Override
    public String toString() {
        return "P" + id +
                " (llegada=" + tiempoLlegada +
                ", rafaga=" + rafagaCPU +
                ", estado=" + estado +
                ", ejecutadoCPU=" + tiempoEjecutadoAcumulado +
                ", E/S restantes=" + (operacionesES.size() - indiceESActual) + ")";
    }

    public int getQuantumUsado(PoliticaQuantum politica) {
        return politica.obtenerQuantum(this);
    }

    // Devuelve la duración de la E/S actual (si existe)
    public int getDuracionESActual() {
        if (indiceESActual < operacionesES.size()) {
            return operacionesES.get(indiceESActual).getDuracion();
        }
        return 0;
    }

    // Devuelve el tiempo restante de la E/S actual
    public int getTiempoESRestante() {
        return tiempoESRestante;
    }

    public int getDuracionES() {
        return getDuracionESActual();
    }

    // ¿Está actualmente en E/S?
    public boolean isEnES() {
        return enES;
    }

    public int getMomentoES() {
        if (indiceESActual < operacionesES.size()) {
            return operacionesES.get(indiceESActual).getMomentoCPU();
        }
        return 0;
    }

    // Getter para saber si ha sido bloqueado alguna vez
    public boolean fueBloqueadoAlgunaVez() {
        return fueBloqueadoAlgunaVez;
    }

    /**
     * Calcula la duración total de todas las operaciones E/S del proceso
     * 
     * @return suma de las duraciones de todas las operaciones E/S
     */
    public int getDuracionTotalES() {
        return operacionesES.stream()
                .mapToInt(OperacionES::getDuracion)
                .sum();
    }

    /**
     * Calcula el tiempo de espera del proceso
     * Fórmula: Tiempo de espera = tiempoFin - tiempoLlegada - rafagaCPU -
     * duracionTotalES
     * 
     * @return tiempo de espera del proceso, o -1 si el proceso no ha terminado
     */
    public int calcularTiempoEspera() {
        if (tiempoFin == -1) {
            return -1; // Proceso no ha terminado
        }
        return tiempoFin - tiempoLlegada - rafagaCPU - getDuracionTotalES();
    }

    /**
     * Calcula el tiempo de ejecución (turnaround time) del proceso
     * Fórmula: Tiempo de ejecución = tiempoFin - tiempoLlegada
     * 
     * @return tiempo de ejecución del proceso, o -1 si el proceso no ha terminado
     */
    public int calcularTiempoEjecucion() {
        if (tiempoFin == -1) {
            return -1; // Proceso no ha terminado
        }
        return tiempoFin - tiempoLlegada;
    }

    /**
     * Reinicia todos los estados del proceso para una nueva simulación.
     * Mantiene los datos originales (id, llegada, ráfaga, operaciones E/S)
     * pero reinicia todo el estado de ejecución.
     */
    public void reiniciarParaSimulacion() {
        this.tiempoRestante = this.rafagaCPU;
        this.tiempoInicio = -1;
        this.tiempoFin = -1;
        this.estado = EnumEstadoProceso.NUEVO;
        this.indiceESActual = 0;
        this.tiempoEjecutadoAcumulado = 0;
        this.tiempoESRestante = 0;
        this.enES = false;
        this.fueBloqueadoAlgunaVez = false;

        // Reiniciar todas las operaciones E/S
        for (OperacionES op : operacionesES) {
            op.reiniciar();
        }
    }

    public static void reiniciarContadorGlobal() {
        contador = 1;
        System.out.println("[DEBUG] Contador de procesos reiniciado a 1");
    }

}
