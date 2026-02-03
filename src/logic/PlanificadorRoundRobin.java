package logic;


import java.util.*;
import models.EnumEstadoProceso;
import models.Proceso;

public class PlanificadorRoundRobin {

    private int tiempoActual;
    private List<Proceso> procesosPendientes;
    private List<Proceso> procesosQueTerminanES = new ArrayList<>();
    private Queue<Proceso> colaListos;
    private Proceso procesoActual;
    private PoliticaQuantum politicaQuantum;
    private int quantumRestante;
    
    // ======================
    // NUEVO: COLA DE OPERACIONES E/S
    // ======================
    private Queue<Proceso> colaES;
    private List<Proceso> historialES; // Para visualización
    
    // Historiales existentes
    private List<Proceso> historialCPL;
    private List<Proceso> historialCPU;
    private List<EjecucionES> ejecucionesES;

    
    // Ejecuciones de CPU
    private List<EjecucionCPU> ejecucionesCompletadas;
    private EjecucionCPU ejecucionActual;
    
    private List<BloqueGantt> gantt;
    private BloqueGantt bloqueActual;
    

    // ======================
    // CLASES INTERNAS
    // ======================
    
    public static class EjecucionCPU {
        private Proceso proceso;
        private int tiempoInicio;
        private int tiempoFin;
        
        public EjecucionCPU(Proceso proceso, int tiempoInicio) {
            this.proceso = proceso;
            this.tiempoInicio = tiempoInicio;
            this.tiempoFin = -1;
        }
        
        public void setTiempoFin(int tiempoFin) {
            this.tiempoFin = tiempoFin;
        }
        
        public Proceso getProceso() { return proceso; }
        public int getTiempoInicio() { return tiempoInicio; }
        public int getTiempoFin() { return tiempoFin; }
        
        public int getDuracion() {
            return tiempoFin - tiempoInicio;
        }
    }
    
    public static class BloqueGantt {
        public Proceso proceso;
        public int tiempoInicio;
        public int duracion;
        
        public BloqueGantt(Proceso proceso, int tiempoInicio) {
            this.proceso = proceso;
            this.tiempoInicio = tiempoInicio;
            this.duracion = 0;
        }
        
        public int getTiempoFin() {
            return tiempoInicio + duracion;
        }
    }

public static class EjecucionES {
    private Proceso proceso;
    private int tiempoEntrada;      // Cuando entró a E/S
    private int tiempoSalida;       // Cuando saldrá de E/S (calculado)
    private int rafagaRestante;     // Ráfaga que tenía AL ENTRAR
    
    public EjecucionES(Proceso proceso, int tiempoEntrada, int rafagaRestante, int duracionES) {
        this.proceso = proceso;
        this.tiempoEntrada = tiempoEntrada;
        this.rafagaRestante = rafagaRestante;
        this.tiempoSalida = tiempoEntrada + duracionES;
    }
    
    public Proceso getProceso() { return proceso; }
    public int getTiempoEntrada() { return tiempoEntrada; }
    public int getTiempoSalida() { return tiempoSalida; }
    public int getRafagaRestante() { return rafagaRestante; }
    
    // Para saber si ya salió
    public boolean estaEnES(int tiempoActual) {
        return tiempoActual < tiempoSalida;
    }
}
    // ======================
    // CONSTRUCTOR
    // ======================
    public PlanificadorRoundRobin(List<Proceso> procesos, PoliticaQuantum politicaQuantum) {
        this.tiempoActual = 0;
        this.procesosPendientes = new LinkedList<>(procesos);
        this.colaListos = new LinkedList<>();
        this.procesoActual = null;
        this.politicaQuantum = politicaQuantum;
        
        // Inicializar nueva cola E/S
        this.colaES = new LinkedList<>();
        this.historialES = new LinkedList<>();
        
        // Historiales existentes
        this.historialCPL = new LinkedList<>();
        this.historialCPU = new LinkedList<>();
        this.ejecucionesCompletadas = new ArrayList<>();
        this.ejecucionActual = null;
        this.ejecucionesES = new ArrayList<>();

        this.gantt = new LinkedList<>();
        this.bloqueActual = null;

        for (Proceso p : this.procesosPendientes) {
            p.setEstado(EnumEstadoProceso.NUEVO);
        }
    }

    // ======================
    // MÉTODO TICK (ACTUALIZADO CON E/S)
    // ======================
 public void tick() {
    System.out.println("\n=== Tick " + tiempoActual + " ===");
    
    // ============================================
    // FASE 0: PROCESAR OPERACIONES E/S (PRIMERO)
    // ============================================
    procesarOperacionesES();
    
    // ============================================
    // FASE 1: MANEJAR LLEGADAS DE PROCESOS NUEVOS
    // ============================================
    List<Proceso> llegadasEsteTick = new ArrayList<>();
    
    for (Proceso p : procesosPendientes) {
        if (p.getTiempoLlegada() == tiempoActual) {
            p.setEstado(EnumEstadoProceso.LISTO);
            llegadasEsteTick.add(p);
            historialCPL.add(p);
            System.out.println("Llega P" + p.getId() + " en t=" + tiempoActual);
        }
    }
    
    for (Proceso p : llegadasEsteTick) {
        colaListos.add(p);
    }
    
    // 2. SEGUNDO: Procesos que terminaron E/S en este tick (PRIORIDAD 3)
    for (Proceso p : procesosQueTerminanES) {
        colaListos.add(p);
        historialCPL.add(p);
        System.out.println("P" + p.getId() + " TERMINA E/S, vuelve a cola");
    }
    procesosPendientes.removeIf(p -> p.getTiempoLlegada() == tiempoActual);
    
    // ============================================
    // FASE 2: MANEJAR PROCESO ACTUAL EN CPU
    // ============================================
    boolean procesoExpulsadoPorQuantum = false;
    Proceso procesoExpulsadoObj = null;
    boolean procesoFueAES = false;
    
    if (procesoActual != null) {
        // 1. Actualizar tiempo ejecutado acumulado
        procesoActual.incrementarTiempoEjecutado();
        
        // 2. Actualizar bloque Gantt
        if (bloqueActual != null) {
            bloqueActual.duracion++;
        }
        
        // 3. Ejecutar (reducir ráfaga y quantum)
        procesoActual.setTiempoRestante(procesoActual.getTiempoRestante() - 1);
        quantumRestante--;
        
        System.out.println("Ejecuta P" + procesoActual.getId() + 
                         ", Restante: " + procesoActual.getTiempoRestante() + 
                         ", Quantum: " + quantumRestante +
                         ", EjecAcum: " + procesoActual.getTiempoEjecutadoAcumulado());
        
        // ============================================
        // VERIFICAR CONDICIONES DE SALIDA DE CPU
        // ============================================
        
        // A. ¿Terminó completamente?
        if (procesoActual.getTiempoRestante() == 0) {
            // Registrar fin de ejecución
            if (ejecucionActual != null) {
                ejecucionActual.setTiempoFin(tiempoActual);
                ejecucionesCompletadas.add(ejecucionActual);
            }
            
            procesoActual.setEstado(EnumEstadoProceso.TERMINADO);
            procesoActual.setTiempoFin(tiempoActual);
            System.out.println("P" + procesoActual.getId() + " TERMINA en t=" + tiempoActual);
            
            procesoActual = null;
            ejecucionActual = null;
            bloqueActual = null;
            
        } 
        // B. ¿Es momento de ir a E/S? (PRIORIDAD sobre quantum)
        else if (procesoActual.debeIrAES()) {
            System.out.println("P" + procesoActual.getId() + " VA A E/S");
            
            // Guardar SNAPSHOT del momento de entrada
            EjecucionES ejecucionES = new EjecucionES(
                procesoActual,
                tiempoActual,                    // Tiempo de entrada
                procesoActual.getTiempoRestante(), // Ráfaga AL ENTRAR (NO cambiará)
                procesoActual.getDuracionES()      // Duración
            );
            ejecucionesES.add(ejecucionES);
            
            // Mover a cola E/S (lógica normal)
            if (ejecucionActual != null) {
                ejecucionActual.setTiempoFin(tiempoActual);
                ejecucionesCompletadas.add(ejecucionActual);
            }
            
            procesoActual.iniciarES();
            colaES.add(procesoActual);
            
            procesoActual = null;
            ejecucionActual = null;
            bloqueActual = null;
        }
        // C. ¿Terminó quantum? (solo si NO fue a E/S)
        else if (quantumRestante == 0) {
            // Registrar fin de ejecución
            if (ejecucionActual != null) {
                ejecucionActual.setTiempoFin(tiempoActual);
                ejecucionesCompletadas.add(ejecucionActual);
            }
            
            procesoActual.setEstado(EnumEstadoProceso.LISTO);
            procesoExpulsadoPorQuantum = true;
            procesoExpulsadoObj = procesoActual;
            
            System.out.println("P" + procesoActual.getId() + " TERMINA QUANTUM en t=" + tiempoActual);
            
            procesoActual = null;
            ejecucionActual = null;
            bloqueActual = null;
        }
    }
    
    // ============================================
    // FASE 3: ENCOLAR PROCESOS SEGÚN PRIORIDAD
    // ============================================
    // 1. Ya encolamos nuevas llegadas (FASE 1)
    
    // 2. SEGUNDO: Proceso que terminó quantum (si aplica)
    if (procesoExpulsadoPorQuantum && procesoExpulsadoObj != null) {
        colaListos.add(procesoExpulsadoObj);
        historialCPL.add(procesoExpulsadoObj);
        System.out.println("Reencola P" + procesoExpulsadoObj.getId() + " (quantum)");
    }
    
    // 3. TERCERO: Procesos que terminaron E/S ya fueron encolados en FASE 0
    // PERO: Cuando procesos terminan E/S, deben respetar el orden de prioridad
    //       respecto a los que llegaron en el mismo tick
    
    // ============================================
    // FASE 4: ASIGNAR NUEVO PROCESO A CPU (si está libre)
    // ============================================
        if (procesoActual == null && !colaListos.isEmpty()) {
        procesoActual = colaListos.poll();
        procesoActual.setEstado(EnumEstadoProceso.EJECUTANDO);
        quantumRestante = politicaQuantum.obtenerQuantum(procesoActual);
        
        ejecucionActual = new EjecucionCPU(procesoActual, tiempoActual);
        historialCPU.add(procesoActual);
        
        bloqueActual = new BloqueGantt(procesoActual, tiempoActual);
        gantt.add(bloqueActual);
        
        System.out.println("Asigna P" + procesoActual.getId() + 
                         " a CPU en t=" + tiempoActual + 
                         ", Quantum: " + quantumRestante);
    }
    
    // ============================================
    // FASE 5: AVANZAR TIEMPO
    // ============================================
    tiempoActual++;
    
    // DEBUG: Mostrar estado
    mostrarEstadoDebug();
}

    // ======================
    // MÉTODOS AUXILIARES
    // ======================
    
    /**
     * Procesa las operaciones E/S (se llama al inicio de cada tick)
     */
    private void procesarOperacionesES() {
    procesosQueTerminanES.clear(); // Limpiar lista temporal
    
    Iterator<Proceso> iterator = colaES.iterator();
    while (iterator.hasNext()) {
        Proceso p = iterator.next();
        
        // Actualizar tiempo de E/S
        if (p.tickES()) {
            // tickES() retorna true si terminó la E/S
            System.out.println("P" + p.getId() + " TERMINA E/S, vuelve a cola");
            
            // NO añadir directamente a colaListos aquí
            // Guardar en lista temporal para encolar en orden correcto
            procesosQueTerminanES.add(p);
            
            iterator.remove();
        }
    }
}
    /**
     * Muestra estado de debug en consola
     */
    private void mostrarEstadoDebug() {
        if (!ejecucionesCompletadas.isEmpty()) {
            System.out.println("Ejecuciones CPU completadas:");
            for (EjecucionCPU ejec : ejecucionesCompletadas) {
                System.out.println("  P" + ejec.getProceso().getId() + 
                                 ": " + ejec.getTiempoInicio() + "-" + ejec.getTiempoFin());
            }
        }
        
        System.out.println("Cola Listos: " + colaListos.size() + " procesos");
        System.out.println("Cola E/S: " + colaES.size() + " procesos");
        if (!colaES.isEmpty()) {
            for (Proceso p : colaES) {
                System.out.println("  P" + p.getId() + ": " + 
                                 p.getTiempoRestante() + "-" + p.getTiempoESRestante());
            }
        }
    }
    
    // ======================
    // MÉTODO HA TERMINADO (ACTUALIZADO)
    // ======================
    public boolean haTerminado() {
        return procesoActual == null
            && colaListos.isEmpty()
            && colaES.isEmpty()      // ← NUEVO: También debe estar vacía la cola E/S
            && procesosPendientes.isEmpty();
    }

    // ======================
    // GETTERS
    // ======================
    
    public Proceso getProcesoActual() {
        return procesoActual;
    }
    
    public List<Proceso> getHistorialCPL() {
        return new ArrayList<>(historialCPL);
    }
    
    public List<Proceso> getHistorialCPU() {
        return new ArrayList<>(historialCPU);
    }
    
    // NUEVO: Getter para procesos en E/S
    public List<Proceso> getProcesosEnES() {
        return new ArrayList<>(colaES);
    }
    
    // NUEVO: Getter para historial E/S (visualización)
    public List<Proceso> getHistorialES() {
        return new ArrayList<>(historialES);
    }
    
    public List<EjecucionCPU> getEjecucionesCPU() {
        return new ArrayList<>(ejecucionesCompletadas);
    }
    
    public List<BloqueGantt> getGantt() {
        return gantt;
    }
    
    public int getTiempoActual() {
        return tiempoActual;
    }
    
    public Queue<Proceso> getColaListos() {
        return new LinkedList<>(colaListos);
    }
    
    // NUEVO: Getter para cola E/S
    public Queue<Proceso> getColaES() {
        return new LinkedList<>(colaES);
    }

    // Getter para las ejecuciones E/S:
    public List<EjecucionES> getEjecucionesES() {
        return new ArrayList<>(ejecucionesES);
    }
}