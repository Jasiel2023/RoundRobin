package models;

import java.util.ArrayList;
import java.util.List;

public class Proceso {

    // ==========================================
    // 1. ATRIBUTOS
    // ==========================================
    private static int contador = 1;//Gnera automaticamente IDS

    private final int id;//Identificador del proceso
    private final int tiempoLlegada;//Momento en que el proceso entra al sistema
    private final int rafagaCPU;//Tiempo del proceso que necesita para
    private  int quantumVariable; // Quantum específico asignado a este proceso
    
    // Estos strings son para facilitar la visualización en tablas/UI
    private final String momentosESOriginal;
    private final String duracionesESOriginal;

    private int tiempoRestante;//Tiempo restante que le falta para ejecutarse
    private int tiempoInicio = -1;//Segundo en que toco la CPU por primera vez
    private int tiempoFin = -1;//Segundo en que termino su rafaga
    private EnumEstadoProceso estado;//Estado del proceso

    //Atributos para la gestion de operaciones de entrada y salida
    private List<OperacionES> operacionesES = new ArrayList<>();//Lista de todas la OES porgramados
    private int indiceESActual = 0; //Indica cual es la OES en atenderser
    private int tiempoEjecutadoAcumulado = 0;// Tiempo total que el proceso ha estado DENTRO del CPU (se usa para activar las E/S)
    private int tiempoESRestante = 0;//Cuenta regresiva del proceso en OES
    private boolean enES = false;//Verificar si el proceso esta enOES
    private boolean fueBloqueadoAlgunaVez = false;//Se usa para verificar si el proceso paso a OES
    
    // ==========================================
    // 2. CONSTRUCTORES (Arreglados para manejar Quantum)
    // ==========================================

    /**
     * CONSTRUCTOR 1: Se usa principalmente cuando los datos de E/S ya vienen como texto (Strings).
     * Útil para cargar datos rápidos o desde archivos donde ya se tiene el formato "1-2-3".
     */
    public Proceso(int tiempoLlegada, int rafagaCPU, int quantumVariable, String momentosESOriginal, String duracionesESOriginal) {
        this.id = contador++; // Asigna ID único e incrementa el contador global
        this.tiempoLlegada = tiempoLlegada;
        this.rafagaCPU = rafagaCPU;
        this.quantumVariable = quantumVariable;
        this.tiempoRestante = rafagaCPU; // Al nacer, el tiempo restante es igual a la ráfaga total
        this.estado = EnumEstadoProceso.NUEVO;
        this.momentosESOriginal = momentosESOriginal;
        this.duracionesESOriginal = duracionesESOriginal;
    }

    /**
     * CONSTRUCTOR 2: Se usa al agregar un proceso nuevo desde el formulario.
     * Recibe una lista de objetos 'OperacionES' y genera automáticamente los textos para la tabla.
     */
    public Proceso(int tiempoLlegada, int rafagaCPU, int quantumVariable, List<OperacionES> operacionesES) {
        this.id = contador++; 
        this.tiempoLlegada = tiempoLlegada;
        this.rafagaCPU = rafagaCPU;
        this.quantumVariable = quantumVariable;
        this.tiempoRestante = rafagaCPU;
        this.estado = EnumEstadoProceso.NUEVO;

        // Copia las operaciones de E/S a la lista interna del proceso
        if (operacionesES != null) {
            this.operacionesES.addAll(operacionesES);
        }

        // --- Lógica de Formateo para la Tabla ---
        if (operacionesES == null || operacionesES.isEmpty()) {
            this.momentosESOriginal = "No";
            this.duracionesESOriginal = "-";
        } else {
            // Convierte la lista de momentos [1, 3] en un String "1-3" usando Streams de Java
            this.momentosESOriginal = operacionesES.stream()
                    .map(op -> String.valueOf(op.getMomentoCPU()))
                    .reduce((a, b) -> a + "-" + b)
                    .orElse("No");

            // Convierte la lista de duraciones [2, 5] en un String "2-5"
            this.duracionesESOriginal = operacionesES.stream()
                    .map(op -> String.valueOf(op.getDuracion()))
                    .reduce((a, b) -> a + "-" + b)
                    .orElse("-");
        }
    }

    /**
     * CONSTRUCTOR 3: Se usa exclusivamente para la EDICIÓN de procesos.
     * La diferencia clave es que recibe un 'idOriginal' para NO generar un nuevo ID (P1 no se vuelve P5).
     */
    public Proceso(int idOriginal, int tiempoLlegada, int rafagaCPU, int quantumVariable, List<OperacionES> operacionesES) {
        this.id = idOriginal; // Mantiene el ID que ya tenía antes de ser editado
        this.tiempoLlegada = tiempoLlegada;
        this.rafagaCPU = rafagaCPU;
        this.quantumVariable = quantumVariable;
        this.tiempoRestante = rafagaCPU;
        this.estado = EnumEstadoProceso.NUEVO;

        if (operacionesES != null) {
            this.operacionesES.addAll(operacionesES);
        }

        // Re-genera los Strings de visualización por si el usuario cambió las E/S en la edición
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
    }    // ==========================================
    // 3. LÓGICA DE E/S Y EJECUCIÓN
    // ==========================================

   
    public boolean debeIrAES() {//Verifica si el proceso necesita interrumpir su uso de CPU para OES
        if (enES || indiceESActual >= operacionesES.size()) return false;//False: en OES o No tiene OES

        OperacionES op = operacionesES.get(indiceESActual);//Busca enfocarse en la siguiente OES que le toca conforma aumenta su indice


        return !op.isEjecutada() && tiempoEjecutadoAcumulado >= op.getMomentoCPU();//Verifiac que la Operacion no se haya hecho ya y que el tiempo de ejecucion total coincia con OES
    }

    public void iniciarES() {
        // 1. VALIDACIÓN: Verifica si aún quedan operaciones de E/S pendientes por ejecutar.
        // Si el índice ya recorrió toda la lista, simplemente sale del método.
        if (indiceESActual >= operacionesES.size()) return;

        // 2. RECUPERACIÓN: Obtiene los datos de la operación de E/S que toca realizar ahora.
        OperacionES op = operacionesES.get(indiceESActual);

        // 3. ACTUALIZACIÓN DE OPERACIÓN: Marca esta operación específica como "ejecutada"
        // para que no se vuelva a disparar en el futuro.
        op.marcarEjecutada();

        // 4. CAMBIO DE ESTADO INTERNO:
        // Activa la bandera que indica que el proceso está en un periférico.
        this.enES = true;
        
        // Configura la cuenta regresiva con la duración que tenga esa E/S (ej: 3 segundos).
        this.tiempoESRestante = op.getDuracion();
        
        // Cambia el estado oficial a BLOQUEADO_ES para que el planificador lo saque del CPU.
        this.estado = EnumEstadoProceso.BLOQUEADO_ES;
        
        // Marca de por vida al proceso como "bloqueado alguna vez" (para cambiar su color a rojo en la UI).
        this.fueBloqueadoAlgunaVez = true;
    }

    public boolean tickES() {//Simula paso del tiempo mientras el proceso esta en OES
    // Verifica si al proceso todavía le queda tiempo de espera en el periférico
    if (tiempoESRestante > 0) {
        
        tiempoESRestante--;//Descuenta unidad de tiempo(1 tick)
        
        // Verifica si con este descuento la espera ha llegado a su fin
        if (tiempoESRestante == 0) {
            

            terminarES();//Limpia de la cola de OES
            
            // Retorna true para avisarle al Planificador: "¡Ya terminé, reencólame!"
            return true; 
        }
    }
    
    // Retorna false si el proceso sigue ocupado en el periférico 
    // o si ni siquiera tenía una OES pendiente
    return false;
}
    
    private void terminarES() {
        this.enES = false;
        this.estado = EnumEstadoProceso.LISTO;
        this.indiceESActual++;
    }

    public void incrementarTiempoEjecutado() {
        this.tiempoEjecutadoAcumulado++;
    }

    // ==========================================
    // 4. MÉTRICAS Y CONTROL
    // ==========================================

    public int calcularTiempoEspera() {
        if (tiempoFin == -1) return -1;
        // Espera = Fin - Llegada - RáfagaCPU - Tiempo pasado en E/S
        return tiempoFin - tiempoLlegada - rafagaCPU - getDuracionTotalES();
    }

    public int calcularTiempoEjecucion() {
        if (tiempoFin == -1) return -1;
        return tiempoFin - tiempoLlegada; // Tiempo de retorno (Turnaround)
    }

    public int getDuracionTotalES() {
        return operacionesES.stream().mapToInt(OperacionES::getDuracion).sum();
    }

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
        operacionesES.forEach(OperacionES::reiniciar);
    }

    public static void reiniciarContadorGlobal() {
        contador = 1;
    }

    // ==========================================
    // 5. GETTERS Y SETTERS
    // ==========================================
    public int getId() {  return id; }
    public int getTiempoLlegada() { return tiempoLlegada; }
    public int getRafagaCPU() { return rafagaCPU; }
    public int getQuantumVariable() { return quantumVariable; } // IMPORTANTE: Este es el que usará el Planificador
    public void setQuantumVariable(int quantumVariable) {
        this.quantumVariable = quantumVariable;
    }
    public int getTiempoRestante() { return tiempoRestante; }
    public void setTiempoRestante(int t) { this.tiempoRestante = t; }
    public EnumEstadoProceso getEstado() { return estado; }
    public void setEstado(EnumEstadoProceso e) { this.estado = e; }
    public int getTiempoFin() { return tiempoFin; }
    public void setTiempoFin(int t) { this.tiempoFin = t; }
    public int getTiempoInicio() { return tiempoInicio;}
    public void setTiempoInicio(int t) { 
        if (this.tiempoInicio == -1) this.tiempoInicio = t; 
    }
    public int getTiempoESRestante() { return tiempoESRestante; }
    public int getDuracionESActual() {
        return (indiceESActual < operacionesES.size()) ? operacionesES.get(indiceESActual).getDuracion() : 0;
    }
    public int getMomentoES() {
        if (indiceESActual < operacionesES.size()) {
            return operacionesES.get(indiceESActual).getMomentoCPU();
        }
        return 0;}

    public String getMomentosESOriginal() { return momentosESOriginal; }
    public String getDuracionesESOriginal() { return duracionesESOriginal; }
    
        // Getter para saber si ha sido bloqueado alguna vez
    public boolean fueBloqueadoAlgunaVez() {
        return fueBloqueadoAlgunaVez;
    }
    @Override
    public String toString() {
        return "P" + id + " (Q:" + quantumVariable + ", Rest:" + tiempoRestante + ")";
    }
}