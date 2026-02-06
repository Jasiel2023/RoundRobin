package logic;


import java.util.*;
import models.EnumEstadoProceso;
import models.Proceso;

public class PlanificadorRoundRobin {

    /**
     * SECCIÓN 1: ATRIBUTOS Y ESTADO DEL SISTEMA
     * Define las estructuras de datos y variables de control del planificador.
     */

    private int tiempoActual;//Tiempo transcurrido en ticks correspondientes al sistema
    private List<Proceso> procesosPendientes;//Lista de procesos que no llegan aun al sistema(Esperan su tiempo de llegada)
    private List<Proceso> procesosQueTerminanES = new ArrayList<>();//Procesos que culminaro OES y se van a reencolar
    private Queue<Proceso> colaListos;//Procesos Listos esperando su turno para ejecutarse
    private Proceso procesoActual;//Proceos que tiene control de la CPU
    private int quantumRestante;//Tiempo de turno asigando para el procesos actual

    // --- GESTIÓN DE ENTRADA / SALIDA (OES) ---

    private Queue<Proceso> colaES;//Cola de espera de procesos en OES
    private List<Proceso> historialES;//Registro de prcesos que pasaron por OES ..... NO BORRES ESTO OE ES PA LA VISTA

    // --- HISTORIALES DE TRAZABILIDAD ---

    private List<Proceso> historialCPL;//Bitacora de procesos que ingressaron a la CPL
    private List<Proceso> historialCPU;//Bitacora de procesos que pasaron en la CPU
    private List<EjecucionES> ejecucionesES;//Operaciones de entrada y salida ya realizados, con sus tiempos de inicio y fin.......PREGUNTAR SI NO ENTIENDEN

    // --- REGISTROS DE EJECUCIÓN ---
    private EjecucionCPU ejecucionActual;//Ejecucion de la CPU que se eta llenando en el tick actual....ACA IGUAL POR LAS DUDAS
    private List<EjecucionCPU> ejecucionesCompletadas;//Registros de intervalos de ejecucion que han finalizado

    // --- ESTRUCTURAS PARA EL DIAGRAMA DE GANTT ---
    private List<BloqueGantt> gantt;//Lista de bloques terminados para su representacion grafica
    private BloqueGantt bloqueActual;//Bloque grafico que crece mientras el proceso esta en la CPU
    //MODELOS DE DATOS(Clases Internas)

    public static class EjecucionCPU {//Periodo especifico del proceso en la cpu
        private Proceso proceso;//Proceso que estuvo en la cpu
        private int tiempoInicio;//Tick en el que el proceso comenxo a ejecutar
        private int tiempoFin;//Tick en el que el procesos fue retirado
        
        
        public EjecucionCPU(Proceso proceso, int tiempoInicio) {//Registro de la ejecucion
            this.proceso = proceso;//Referencia del proceso
            this.tiempoInicio = tiempoInicio;//Tiempo actual de la simulacion como inicio
            this.tiempoFin = -1;//Señalizacion de que la ejecucion no a terminado
        }
        
        public void setTiempoFin(int tiempoFin) {
            // Asigna el tiempo de salida definitivo
            this.tiempoFin = tiempoFin;
        }
        
        // Retorna el proceso asociado a este registro
        public Proceso getProceso() { return proceso; }
        
        // Retorna el tick de inicio
        public int getTiempoInicio() { return tiempoInicio; }
        
        // Retorna el tick de finalización
        public int getTiempoFin() { return tiempoFin; }
        
        public int getDuracion() {//calcula la duracion total del segmento de ejecucion
            // Resta el inicio del fin para obtener el tiempo neto en CPU
            return tiempoFin - tiempoInicio;
        }
    }
    public static class EjecucionES {//Evento de Bloqueo de un proceso
        private Proceso proceso;// Referencia al proceso que se ha bloqueado
        private int tiempoEntrada;// El instante de tiempo (tick) en el que el proceso abandonó el CPU para ir a E/S
        private int tiempoSalida;// El instante futuro calculado en el que el proceso terminará su E/S
        private int rafagaRestante;//Rafaga que falta para bloquearse     
        public EjecucionES(Proceso proceso, int tiempoEntrada, int rafagaRestante, int duracionES) {
            // Asocia el proceso al evento de bloqueo
            this.proceso = proceso;
            
            // Guarda el momento de inicio del bloqueo
            this.tiempoEntrada = tiempoEntrada;
            
            // Guarda cuánta ráfaga le queda para cuando regrese al CPU
            this.rafagaRestante = rafagaRestante;
            
            // Calcula automáticamente cuándo debe despertar el proceso
            this.tiempoSalida = tiempoEntrada + duracionES;
        }
        
        // Métodos para obtener los datos del registro
        public Proceso getProceso() { return proceso; }
        public int getTiempoEntrada() { return tiempoEntrada; }
        public int getTiempoSalida() { return tiempoSalida; }
        public int getRafagaRestante() { return rafagaRestante; }
        public boolean estaEnES(int tiempoActual) {
            // Si el tiempo actual no ha llegado al tiempo de salida, sigue bloqueado
            return tiempoActual < tiempoSalida;
        }
    }
    public static class BloqueGantt {//Representacion Visual del proceso en la cpu
        public Proceso proceso;//Proceso dibujado
        public int tiempoInicio;//Punto de partida
        public int duracion;//Duracion de ticks
        
        /**
         * Constructor para inicializar un nuevo bloque en el gráfico.
         * Se crea cuando un proceso entra al CPU y no había uno igual antes.
         */
        public BloqueGantt(Proceso proceso, int tiempoInicio) {
            this.proceso = proceso;// Vincula el proceso al bloque visual
            this.tiempoInicio = tiempoInicio;// Define en qué segundo/tick empieza a dibujarse
            this.duracion = 0;// Comienza con duración 0 y aumenta en cada tick de ejecución

        }
        public int getTiempoFin() {
            // Calcula el límite derecho de la barra en el diagrama
            return tiempoInicio + duracion;
        }
    }
    
    
    
    
    
    //Procesar E/S: Determinacion de inicio y final de operaciones de entrada y salida
    
    private void procesarOperacionesES() {//Gestiona los procesos que estan en OES para volever a la CPL
        // Limpia la lista temporal de los procesos que terminaron su OES en el tick anterior.
        // Esto asegura que no re-encolemos procesos por error.
        procesosQueTerminanES.clear(); 
        
        Iterator<Proceso> iterator = colaES.iterator();//Recorre la cola OES
        
        while (iterator.hasNext()) {
            // Obtiene el siguiente proceso que está en estado de bloqueo por OES
            Proceso p = iterator.next();//Obtiene el siguiente proceso que esta en OES
            
            if (p.tickES()) {//Reduce su valor de espera en OES hasta llegar a cero
                System.out.println("P" + p.getId() + " TERMINA OES, preparando retorno");
                procesosQueTerminanES.add(p);//Se guarda el proceso para su gestion de encolacion
                iterator.remove();//Elimina proceso de OES
            }
        }
    }


    // CONSTRUCTOR

    public PlanificadorRoundRobin(List<Proceso> procesos) {//Estado inical del planificador con los procesos proporcionados
        this.tiempoActual = 0;//cronometro global de la simulacion tick(0)
        this.procesosPendientes = new LinkedList<>(procesos);//Procesos para el manejo de extraccion de procesos listos
        this.colaListos = new LinkedList<>();//Inicializacion de procesos listos para la CPU
        this.procesoActual = null;//No hay ningun proceso en la cpu
        
        //Inicialización de estructuras para Entrada/Salida
        
        this.colaES = new LinkedList<>();//Cola de operaciones en espera 
        this.historialES = new LinkedList<>();//Historial de procesos en Operaciones de entrada y salida
        
        // --- Inicialización de Historiales y Registros ---
        
        this.historialCPL = new LinkedList<>();//Historial de procesos listos
        this.historialCPU = new LinkedList<>();//Historial de procesos en la CPU        
        this.ejecucionesCompletadas = new ArrayList<>();//Lista de periodos de ejecucion
        this.ejecucionActual = null;//Auxiliar del proceso que se ejecuta actualmente
        this.ejecucionesES = new ArrayList<>();//Lista de todos los registros de operaciones de entrada y salida de un proceso

        // --- Estructuras Visuales ---
        
        this.gantt = new LinkedList<>();//Lista de bloques a dibujar en el diagrama de gantt
        this.bloqueActual = null;//Bloque visual que se esta desarrollando actualmente

        // --- Preparación Final de los Procesos ---
        
        // Recorre todos los procesos cargados y les asigna el estado inicial "NUEVO"
        for (Proceso p : this.procesosPendientes) {
            p.setEstado(EnumEstadoProceso.NUEVO);
        }
    }

    
    //Tick: Unidad de tiempo del sistema 
        public void tick() { //Encargado de ejecutar procesos en cada unidad de tiempo
        
        System.out.println("\n=== Tick " + tiempoActual + " ===");//Impresion de tick en consola

        // ==================================================
        // FASE 0: ACTUALIZAR PROCESOS EN OPERACIÓN DE E/S
        // ==================================================

        procesarOperacionesES();// Revisa la "Cola de E/S" para ver qué procesos terminaron su espera en este tick

        // ==================================================
        // FASE 1: DETECTAR LLEGADAS
        // ==================================================
        List<Proceso> llegadasEsteTick = new ArrayList<>();// Lista temporal para capturar procesos que entran al sistema justo ahora

        for (Proceso p : procesosPendientes) {//Para cada proceso que este en la lista de procesos pendientes
            if (p.getTiempoLlegada() == tiempoActual) {// Verifica si el tiempo de llegada del proceso coincide con el reloj actual

                p.setEstado(EnumEstadoProceso.LISTO); // Cambia el estado visual
                llegadasEsteTick.add(p);               // Lo guarda para encolarlo más tarde
                System.out.println("Llega P" + p.getId() + " en t=" + tiempoActual);
            }
        }
        procesosPendientes.removeIf(p -> p.getTiempoLlegada() == tiempoActual);// Saca de "pendientes" a los procesos que ya ingresaron al sistema

        // ==================================================
        // FASE 2: EJECUTAR CPU (Lógica de ráfaga y desalojo)
        // ==================================================
        boolean procesoExpulsadoPorQuantum = false;//Verificara si cumplio el cuantum para sacarlo a puñete del cpu
        Proceso procesoExpulsadoObj = null; //guardara objetos expulsados en este caso procesos

        if (procesoActual != null) {
        
            procesoActual.incrementarTiempoEjecutado();//Incrementa el tiempo que el proceso ha estado físicamente en el CPU
            
            if (bloqueActual != null) {//Aumento del bloque visual Gantt activo
                bloqueActual.duracion++;
            }

            // Reduce el trabajo pendiente (Ráfaga) del proceso
            if (procesoActual.getTiempoRestante() > 0) {
                procesoActual.setTiempoRestante(procesoActual.getTiempoRestante() - 1);
            }
            // Consume una unidad del Quantum asignado
            quantumRestante--;

            System.out.println("Ejecuta P" + procesoActual.getId() + 
                            ", Restante: " + procesoActual.getTiempoRestante() + 
                            ", Quantum: " + quantumRestante);

            // --- SUBFASE A: ¿El proceso terminó su trabajo? ---
            if (procesoActual.getTiempoRestante() == 0) {//Verifica si el trabajo pendiente es 0
                if (ejecucionActual != null) {
                    ejecucionActual.setTiempoFin(tiempoActual); // Cierra el registro de Ejecución CPU
                    ejecucionesCompletadas.add(ejecucionActual);//Mueve esta ejecucion a ejecuciones completadas
                }
                procesoActual.setEstado(EnumEstadoProceso.TERMINADO);
                procesoActual.setTiempoFin(tiempoActual); // Marca la hora de muerte del proceso
                System.out.println("P" + procesoActual.getId() + " TERMINA en t=" + tiempoActual);

                // Libera el CPU
                procesoActual = null;
                ejecucionActual = null;
                bloqueActual = null;
            }
            
            // --- SUBFASE B: ¿El proceso solicita una OES? (Prioridad sobre Quantum) ---
            else if (procesoActual.debeIrAES()) {
                System.out.println("P" + procesoActual.getId() + " INICIA OES");

                // CREA EL REGISTRO DE OES: Anota inicio, salida calculada y ráfaga pendiente
                EjecucionES ejecucionES = new EjecucionES(
                    procesoActual,
                    tiempoActual,
                    procesoActual.getTiempoRestante(),
                    procesoActual.getDuracionESActual()
                );
                ejecucionesES.add(ejecucionES); // Se guarda en la bitácora histórica de OES

                if (ejecucionActual != null) {
                    ejecucionActual.setTiempoFin(tiempoActual); // Cierra ejecución de CPU
                    ejecucionesCompletadas.add(ejecucionActual);
                }

                procesoActual.iniciarES(); // Cambia lógica interna del proceso
                colaES.add(procesoActual); // Se va a la cola de espera de periféricos

                // Libera el CPU
                procesoActual = null;
                ejecucionActual = null;
                bloqueActual = null;
            }
            
            // --- SUBFASE C: ¿Se agotó el tiempo de Quantum? ---
            else if (quantumRestante == 0) {
                if (ejecucionActual != null) {
                    ejecucionActual.setTiempoFin(tiempoActual);
                    ejecucionesCompletadas.add(ejecucionActual);
                }

                procesoActual.setEstado(EnumEstadoProceso.LISTO);
                procesoExpulsadoPorQuantum = true; // Bandera para reencolar al final
                procesoExpulsadoObj = procesoActual;

                System.out.println("P" + procesoActual.getId() + " EXPULSADO por Quantum");

                // Libera el CPU
                procesoActual = null;
                ejecucionActual = null;
                bloqueActual = null;
            }
        }

        // ==================================================
        // FASE 3: ENCOLAMIENTO POR PRIORIDAD
        // ==================================================
        // 1) Primero entran los procesos que acaban de llegar de la calle (Nuevos)
        for (Proceso p : llegadasEsteTick) {
            colaListos.add(p);
            historialCPL.add(p);
        }

        // 2) Segundo entra el proceso que el CPU acaba de echar por Quantum
        if (procesoExpulsadoPorQuantum && procesoExpulsadoObj != null) {
            colaListos.add(procesoExpulsadoObj);
            historialCPL.add(procesoExpulsadoObj);
        }

        // 3) Al final entran los procesos que vienen regresando de una OES
        for (Proceso p : procesosQueTerminanES) {
            p.setEstado(EnumEstadoProceso.LISTO);
            colaListos.add(p);
            historialCPL.add(p);
            System.out.println("P" + p.getId() + " vuelve de OES");
        }

        // ==================================================
        // FASE 4: DESPACHO (Asignar CPU si está vacío)
        // ==================================================
        if (procesoActual == null && !colaListos.isEmpty()) {
            procesoActual = colaListos.poll(); // Saca al primero de la cola de listos
            procesoActual.setEstado(EnumEstadoProceso.EJECUTANDO);

            // ASIGNACIÓN DE QUANTUM VARIABLE: Cada proceso trae su propio tiempo de turno
            this.quantumRestante = procesoActual.getQuantumVariable(); 

            // Crea nuevos registros para la bitácora de CPU y el gráfico de Gantt
            ejecucionActual = new EjecucionCPU(procesoActual, tiempoActual);
            historialCPU.add(procesoActual);
            bloqueActual = new BloqueGantt(procesoActual, tiempoActual);
            gantt.add(bloqueActual);

            System.out.println("Asigna P" + procesoActual.getId() + 
                            " a CPU con Quantum: " + quantumRestante);
        }

        // ==================================================
        // FASE 5: AVANZAR TIEMPO
        // ==================================================
        // El tick termina y el reloj global aumenta para el siguiente ciclo
        tiempoActual++;

        // Muestra en consola el resumen de las colas para depuración
        mostrarEstadoDebug();
    }

    private void mostrarEstadoDebug() {//Seguimiento tecnico en la consola del simulador
        if (!ejecucionesCompletadas.isEmpty()) {//Verficacion de procesos que pasoron por la cpu y terminaron su rafaga
            System.out.println("Ejecuciones CPU completadas:");
            
            for (EjecucionCPU ejec : ejecucionesCompletadas) {//Recorrido de la lista de ejecuciones historicas
                // Imprime el ID del proceso y el intervalo de tiempo (inicio-fin) que ocupó el CPU
                System.out.println("  P" + ejec.getProceso().getId() + 
                                ": " + ejec.getTiempoInicio() + "-" + ejec.getTiempoFin());
            }
        }
        
        System.out.println("Cola Listos: " + colaListos.size() + " procesos");//Procesos que estan en la cola de listos
        System.out.println("Cola E/S: " + colaES.size() + " procesos");//Procesos que estan en OES
        
        if (!colaES.isEmpty()) {//Si esta ocupado
            for (Proceso p : colaES) {

                System.out.println("  P" + p.getId() + ": " + 
                                p.getTiempoRestante() + " (CPU) - " + 
                                p.getTiempoESRestante() + " (OES)");
            }
        }
    }

    public boolean haTerminado() {//Determina si la simulacion ha finalizado
        return procesoActual == null         // El CPU está vacío
            && colaListos.isEmpty()          // No hay nadie esperando para entrar al CPU
            && colaES.isEmpty()              // No hay procesos realizando Operaciones de Entrada/Salida
            && procesosPendientes.isEmpty(); // No faltan procesos por llegar de la "calle"
    }

    // --- MÉTODOS DE CONSULTA (GETTERS) ---

    public Proceso getProcesoActual() {
        return procesoActual;
    }

    public List<Proceso> getHistorialCPL() {
        return new ArrayList<>(historialCPL); // Copia del historial de la Cola de Listos
    }

    public List<Proceso> getHistorialCPU() {
        return new ArrayList<>(historialCPU); // Copia del historial de uso del CPU
    }

    // Retorna los procesos que están bloqueados actualmente realizando una OES
    public List<Proceso> getProcesosEnES() {
        return new ArrayList<>(colaES);
    }

    public void reiniciar() {//Sirve para dejar la simulacion como nueva
        tiempoActual = 0;
        quantumRestante = 0;
        
        // Limpieza de colas y listas
        procesosPendientes.clear();
        colaListos.clear();
        colaES.clear();
        procesosQueTerminanES.clear();
        
        // Limpieza de registros históricos
        historialES.clear();
        historialCPL.clear();
        historialCPU.clear();
        ejecucionesCompletadas.clear();
        ejecucionesES.clear();
        
        // Limpieza de datos visuales
        gantt.clear();
        
        // Reset de punteros de control
        procesoActual = null;
        ejecucionActual = null;
        bloqueActual = null;
        
        System.out.println("[DEBUG] Planificador reiniciado a estado inicial");
    }

    // Retorna el historial acumulado de procesos que pasaron por OES
    public List<Proceso> getHistorialES() {
        return new ArrayList<>(historialES);
    }

    // Retorna los registros cerrados de intervalos de CPU (para tablas de resultados)
    public List<EjecucionCPU> getEjecucionesCPU() {
        return new ArrayList<>(ejecucionesCompletadas);
    }

    // Retorna los bloques de dibujo para el Diagrama de Gantt
    public List<BloqueGantt> getGantt() {
        return gantt;
    }

    public int getTiempoActual() {
        return tiempoActual;
    }

    public Queue<Proceso> getColaListos() {
        return new LinkedList<>(colaListos);
    }

    public Queue<Proceso> getColaES() {
        return new LinkedList<>(colaES);
    }

    //Retorna la bitácora completa de las Operaciones de Entrada y Salida (OES) 
    public List<EjecucionES> getEjecucionesES() {
        return new ArrayList<>(ejecucionesES);
    }
}