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
    


    // CLASES INTERNAS

    
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
    // CONSTRUCTOR
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

    // ==================================================
    // FASE 0: PROCESAR E/S (solo detectar quién termina)
    // ==================================================
    procesarOperacionesES(); // Llena procesosQueTerminanES (NO encola aún)

    // ==================================================
    // FASE 1: DETECTAR LLEGADAS
    // ==================================================
    List<Proceso> llegadasEsteTick = new ArrayList<>();

    for (Proceso p : procesosPendientes) {
        if (p.getTiempoLlegada() == tiempoActual) {
            p.setEstado(EnumEstadoProceso.LISTO);
            llegadasEsteTick.add(p);
            System.out.println("Llega P" + p.getId() + " en t=" + tiempoActual);
        }
    }

    procesosPendientes.removeIf(p -> p.getTiempoLlegada() == tiempoActual);

    // ==================================================
    // FASE 2: EJECUTAR CPU (1 unidad de tiempo)
    // ==================================================
    boolean procesoExpulsadoPorQuantum = false;
    Proceso procesoExpulsadoObj = null;

    if (procesoActual != null) {
        // Actualizar métricas
        procesoActual.incrementarTiempoEjecutado();
        if (bloqueActual != null) {
            bloqueActual.duracion++;
        }

        // Ejecutar 1 unidad
        if (procesoActual.getTiempoRestante() > 0) {
            procesoActual.setTiempoRestante(procesoActual.getTiempoRestante() - 1);
        }
        quantumRestante--;

        System.out.println("Ejecuta P" + procesoActual.getId() +
                ", Restante: " + procesoActual.getTiempoRestante() +
                ", Quantum: " + quantumRestante);

        // A) ¿Terminó?
        if (procesoActual.getTiempoRestante() == 0) {
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
        // B) ¿Debe ir a E/S? (tiene prioridad sobre quantum)
        else if (procesoActual.debeIrAES()) {
            System.out.println("P" + procesoActual.getId() + " VA A E/S");

            EjecucionES ejecucionES = new EjecucionES(
                procesoActual,
                tiempoActual,
                procesoActual.getTiempoRestante(),
                procesoActual.getDuracionESActual()
            );

            ejecucionesES.add(ejecucionES);

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
        // C) ¿Se acabó el quantum?
        else if (quantumRestante == 0) {
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

    // ==================================================
    // FASE 3: ENCOLAR SEGÚN TU PRIORIDAD
    // PRIORIDAD: LLEGADAS > QUANTUM > E/S
    // ==================================================

    // 1) Llegadas
    for (Proceso p : llegadasEsteTick) {
        colaListos.add(p);
        historialCPL.add(p);
    }

    // 2) Proceso que terminó quantum
    if (procesoExpulsadoPorQuantum && procesoExpulsadoObj != null) {
        colaListos.add(procesoExpulsadoObj);
        historialCPL.add(procesoExpulsadoObj);
        System.out.println("Reencola P" + procesoExpulsadoObj.getId() + " (quantum)");
    }

    // 3) Procesos que terminaron E/S
    for (Proceso p : procesosQueTerminanES) {
        p.setEstado(EnumEstadoProceso.LISTO);
        colaListos.add(p);
        historialCPL.add(p);
        System.out.println("P" + p.getId() + " vuelve de E/S");
    }

    // ==================================================
    // FASE 4: ASIGNAR CPU SI ESTÁ LIBRE
    // ==================================================
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

    // ==================================================
    // FASE 5: AVANZAR TIEMPO
    // ==================================================
    tiempoActual++;

    // Debug
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
    
    public void reiniciar() {
        tiempoActual = 0;
        procesosPendientes.clear();
        colaListos.clear();
        colaES.clear();
        historialES.clear();
        historialCPL.clear();
        historialCPU.clear();
        ejecucionesCompletadas.clear();
        ejecucionesES.clear();
        gantt.clear();
        procesosQueTerminanES.clear();
        
        procesoActual = null;
        ejecucionActual = null;
        bloqueActual = null;
        quantumRestante = 0;
        
        System.out.println("[DEBUG] Planificador reiniciado a estado inicial");
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