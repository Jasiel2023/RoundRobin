package views;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import logic.PlanificadorRoundRobin;
import models.EnumEstadoProceso;
import models.OperacionES;
import models.Proceso;

public class VentanaRoundRobin extends JFrame {

    // ======================
    // COMPONENTES DE INTERFAZ
    // ======================

    // Campos de entrada
    private JTextField txtLlegada, txtRafaga, txtQuantum, txtQuantumProceso;
    private JTextField txtIntervaloES, txtDuracionES; // Campos para E/S
    private JComboBox<String> comboTipoRR;
    private JLabel lblQuantumProceso;

    // Paneles de visualización
    private JPanel panelHistorialCola, panelES, panelCPU, panelTiempos;

    // Botones de control
    private JButton btnAgregar, btnEditar, btnEliminar, btnIniciar, btnPausa, btnReiniciar;

    // Tabla de procesos
    private JTable tablaProcesos;

    // ======================
    // MODELOS Y CONTROL
    // ======================
    private ProcesoTableModel tableModel;
    private PlanificadorRoundRobin scheduler;
    private Timer timer;
    private boolean enPausa = false;

    // Datos para visualización
    private List<Proceso> historialCola = new ArrayList<>();
    private Map<Integer, JLabel> historialColaLabels = new LinkedHashMap<>();

    public VentanaRoundRobin() {
        initUI();
    }

    // ======================
    // INICIALIZACIÓN DE INTERFAZ
    // ======================

    private void initUI() {
        configurarVentanaPrincipal();
        crearPanelFormulario();
        crearPanelVisualizacion();
        configurarListeners();
        setVisible(true);
    }

    private void configurarVentanaPrincipal() {
        setTitle("Simulador Round Robin con Operaciones E/S");
        setSize(900, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
    }

    private void crearPanelFormulario() {
        // Inicializar componentes
        inicializarComponentesFormulario();

        // Panel principal que contiene todo
        JPanel panelPrincipal = new JPanel(new BorderLayout(5, 5));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel de entrada de datos (arriba)
        JPanel panelEntrada = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        agregarComponentesAlFormulario(panelEntrada);
        panelPrincipal.add(panelEntrada, BorderLayout.NORTH);

        // Panel de botones (centro, centrado)
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panelBotones.setBorder(BorderFactory.createTitledBorder("Controles"));
        panelBotones.add(btnAgregar);
        panelBotones.add(btnEditar);
        panelBotones.add(btnEliminar);
        panelBotones.add(btnIniciar);
        panelBotones.add(btnPausa);
        panelBotones.add(btnReiniciar);
        panelPrincipal.add(panelBotones, BorderLayout.CENTER);

        add(panelPrincipal, BorderLayout.NORTH);
    }

    private void inicializarComponentesFormulario() {
        // 1. Campos de texto (Primero dales vida con 'new')
        txtLlegada = new JTextField(5);
        txtRafaga = new JTextField(5);
        txtQuantumProceso = new JTextField(5); // <-- Asegúrate que esta línea esté
        txtIntervaloES = new JTextField(5);
        txtDuracionES = new JTextField(5);

        // 2. Etiquetas (Dales vida con 'new')
        lblQuantumProceso = new JLabel("Quantum:"); // <-- ESTA LÍNEA DEBE IR ANTES QUE EL SETVISIBLE

        // 3. Ahora sí puedes llamar a métodos del objeto
        lblQuantumProceso.setVisible(true);
        txtQuantumProceso.setVisible(true);

        // 4. Botones
        btnAgregar = new JButton("Agregar proceso");
        btnEditar = new JButton("Editar proceso");
        btnEliminar = new JButton("Eliminar proceso");
        btnIniciar = new JButton("Iniciar RR");
        btnPausa = new JButton("Pausar");
        btnReiniciar = new JButton("Vaciar");
    }

    private void agregarComponentesAlFormulario(JPanel panelForm) {
        panelForm.removeAll(); // Limpia cualquier residuo previo

        // Fila 1: Datos básicos
        panelForm.add(new JLabel("Llegada:"));
        panelForm.add(txtLlegada);
        panelForm.add(new JLabel("Ráfaga:"));
        panelForm.add(txtRafaga);

        // Fila 2: El Quantum (Bien etiquetado)
        panelForm.add(new JLabel("Quantum:")); // Etiqueta simple y directa
        panelForm.add(txtQuantumProceso);

        // Fila 3: Entrada / Salida
        panelForm.add(new JLabel("Operaciones E/S (ej: 1-3):"));
        panelForm.add(txtIntervaloES);
        panelForm.add(new JLabel("Duración E/S (ej: 2-4):"));
        panelForm.add(txtDuracionES);

        panelForm.revalidate();
        panelForm.repaint();
    }

    private void crearPanelVisualizacion() {
        // Crear tabla de procesos
        tableModel = new ProcesoTableModel();
        tablaProcesos = new JTable(tableModel);
        tablaProcesos.setRowHeight(25);
        tablaProcesos.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(tablaProcesos);
        scroll.setPreferredSize(new Dimension(880, 150));

        // Crear paneles de estado
        crearPanelesDeEstado();

        // Panel combinado con todos los estados y tiempos
        JPanel panelTodosCombinado = new JPanel();
        panelTodosCombinado.setLayout(new BoxLayout(panelTodosCombinado, BoxLayout.Y_AXIS));
        panelTodosCombinado.add(panelHistorialCola);
        panelTodosCombinado.add(Box.createVerticalStrut(5));
        panelTodosCombinado.add(panelES);
        panelTodosCombinado.add(Box.createVerticalStrut(5));
        panelTodosCombinado.add(panelCPU);
        panelTodosCombinado.add(Box.createVerticalStrut(5));
        panelTodosCombinado.add(panelTiempos);

        // Un único scroll para todo
        JScrollPane scrollCombinado = new JScrollPane(panelTodosCombinado);
        scrollCombinado.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollCombinado.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollCombinado.getVerticalScrollBar().setUnitIncrement(16);

        // Panel central (tabla + panel combinado)
        JPanel panelCentro = new JPanel(new BorderLayout(5, 5));
        panelCentro.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panelCentro.add(scroll, BorderLayout.NORTH);
        panelCentro.add(scrollCombinado, BorderLayout.CENTER);

        add(panelCentro, BorderLayout.CENTER);
    }

    private void crearPanelesDeEstado() {
        panelHistorialCola = crearPanelConTitulo("Historial de Cola de Listos", 850, 100);
        panelES = crearPanelConTitulo("Operaciones de Entrada/Salida (Historial)", 850, 100);
        panelCPU = crearPanelConTitulo("CPU - Historial de ejecución", 850, 100);
        panelTiempos = crearPanelConTitulo("Tiempos de Espera y Ejecución", 850, 150);
        panelTiempos.setLayout(new BorderLayout());
    }

    private JPanel crearPanelConTitulo(String titulo, int ancho, int alto) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder(titulo));
        panel.setPreferredSize(new Dimension(ancho, alto));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, alto));
        return panel;
    }

    private void configurarListeners() {
        btnAgregar.addActionListener(e -> agregarProceso());
        btnEditar.addActionListener(e -> editarProceso());
        btnEliminar.addActionListener(e -> eliminarProceso());
        btnIniciar.addActionListener(e -> iniciarSimulacion());
        btnPausa.addActionListener(e -> togglePausa());
        btnReiniciar.addActionListener(e -> reiniciarTodo());
    }

    // ======================
    // LÓGICA DE NEGOCIO
    // ======================

    private void editarProceso() {
        // Obtiene el índice de la fila que el usuario clickeó en la tabla
        int filaSeleccionada = tablaProcesos.getSelectedRow();

        // Si no hay ninguna fila seleccionada (valor -1), lanza un aviso y detiene el método
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this,
                    "Por favor, seleccione un proceso de la tabla para editar.",
                    "Ningún proceso seleccionado",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Recupera el objeto Proceso guardado en esa posición de la tabla
        Proceso procesoOriginal = tableModel.getProceso(filaSeleccionada);

        // Configuración visual de la ventanita (diálogo) de edición
        JDialog dialogoEdicion = new JDialog(this, "Editar Proceso P" + procesoOriginal.getId(), true);
        dialogoEdicion.setSize(400, 350);
        dialogoEdicion.setLocationRelativeTo(this);
        dialogoEdicion.setLayout(new BorderLayout(10, 10));

        // Crea la cuadrícula para organizar etiquetas y cuadros de texto
        JPanel panelCampos = new JPanel(new GridLayout(6, 2, 10, 10));
        panelCampos.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Rellena los cuadros de texto con la información actual del proceso
        JTextField txtLlegadaEdit = new JTextField(String.valueOf(procesoOriginal.getTiempoLlegada()));
        JTextField txtRafagaEdit = new JTextField(String.valueOf(procesoOriginal.getRafagaCPU()));
        
        // Si el proceso no tiene momentos de E/S, deja el campo en blanco; si tiene, los muestra
        JTextField txtMomentosESEdit = new JTextField(
                procesoOriginal.getMomentosESOriginal().equals("No") ? "" : procesoOriginal.getMomentosESOriginal());
        
        // Lo mismo para las duraciones de las E/S
        JTextField txtDuracionesESEdit = new JTextField(
                procesoOriginal.getDuracionesESOriginal().equals("-") ? "" : procesoOriginal.getDuracionesESOriginal());
        
        // Muestra el quantum personal; si es -1 (usa el global) lo deja en blanco
        JTextField txtQuantumEdit = new JTextField(
                procesoOriginal.getQuantumVariable() == -1 ? "" : String.valueOf(procesoOriginal.getQuantumVariable()));

        // Agrega los componentes visuales al panel de campos
        panelCampos.add(new JLabel("Tiempo de llegada:"));
        panelCampos.add(txtLlegadaEdit);
        panelCampos.add(new JLabel("Ráfaga CPU:"));
        panelCampos.add(txtRafagaEdit);
        panelCampos.add(new JLabel("Momentos E/S (ej: 1-3-5):"));
        panelCampos.add(txtMomentosESEdit);
        panelCampos.add(new JLabel("Duraciones E/S (ej: 2-3-4):"));
        panelCampos.add(txtDuracionesESEdit);
        panelCampos.add(new JLabel("Quantum personal:"));
        panelCampos.add(txtQuantumEdit);
        panelCampos.add(new JLabel("(Dejar vacío para usar global)"));
        panelCampos.add(new JLabel(""));

        dialogoEdicion.add(panelCampos, BorderLayout.CENTER);

        // Configuración de los botones de la parte inferior
        JPanel panelBotonesDialogo = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton btnGuardar = new JButton("Guardar");
        JButton btnCancelar = new JButton("Cancelar");

        // Lógica que se ejecuta al presionar "Guardar"
        btnGuardar.addActionListener(ev -> {
            try {
                // Convierte el texto de los campos a números enteros
                int llegada = leerEnteroPositivo(txtLlegadaEdit.getText(), "Llegada");
                int rafaga = leerEnteroPositivo(txtRafagaEdit.getText(), "Ráfaga", true);

                // Procesa las cadenas de texto separadas por guiones (ej: "1-3")
                String textoMomentos = txtMomentosESEdit.getText().trim();
                String textoDuraciones = txtDuracionesESEdit.getText().trim();
                List<OperacionES> listaES = new ArrayList<>();

                // Si el usuario escribió algo en los campos de E/S...
                if (!textoMomentos.isEmpty() && !textoDuraciones.isEmpty()) {
                    String[] momentos = textoMomentos.split("-"); // Divide el texto por cada guion
                    String[] duraciones = textoDuraciones.split("-");

                    // Valida que por cada momento haya una duración correspondiente
                    if (momentos.length != duraciones.length) {
                        throw new NumberFormatException("Cantidad de operaciones y duraciones no coincide");
                    }

                    // Convierte cada par (momento, duración) en un objeto OperacionES y lo añade a la lista
                    for (int i = 0; i < momentos.length; i++) {
                        int momento = leerEnteroPositivo(momentos[i], "Momento E/S", true);
                        int duracion = leerEnteroPositivo(duraciones[i], "Duración E/S", true);
                        listaES.add(new OperacionES(momento, duracion));
                    }
                }

                // Crea un objeto Proceso totalmente nuevo con la información editada pero el MISMO ID
                Proceso procesoEditado = new Proceso(procesoOriginal.getId(), llegada, rafaga, listaES);
                procesoEditado.setEstado(EnumEstadoProceso.NUEVO); // Asegura que inicie en estado NUEVO

                // Si el campo de Quantum no está vacío, asigna el valor personalizado al nuevo proceso
                String txtQuantumVal = txtQuantumEdit.getText().trim();
                if (!txtQuantumVal.isEmpty()) {
                    int quantum = leerEnteroPositivo(txtQuantumVal, "Quantum personal", true);
                    procesoEditado.setQuantumVariable(quantum);
                }

                // Reemplaza el proceso viejo por el editado en el modelo de la tabla
                tableModel.actualizarProceso(filaSeleccionada, procesoEditado);

                // Cierra el diálogo y avisa que todo salió bien
                dialogoEdicion.dispose();
                JOptionPane.showMessageDialog(this,
                        "Proceso editado correctamente.",
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (NumberFormatException ex) {
                // Si hubo un error en los números o el formato, muestra el mensaje de error
                mostrarErrorValidacion(ex.getMessage());
            }
        });

        // Acción del botón Cancelar: simplemente cierra la ventana sin guardar nada
        btnCancelar.addActionListener(ev -> dialogoEdicion.dispose());

        panelBotonesDialogo.add(btnGuardar);
        panelBotonesDialogo.add(btnCancelar);
        dialogoEdicion.add(panelBotonesDialogo, BorderLayout.SOUTH);

        // Hace visible la ventana de edición
        dialogoEdicion.setVisible(true);
    }

    private void eliminarProceso() {
        // Busca el índice de la fila que el usuario tiene seleccionada actualmente en la tabla
        int filaSeleccionada = tablaProcesos.getSelectedRow();

        // Si no hay ninguna fila seleccionada (getSelectedRow devuelve -1), muestra un aviso
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this,
                    "Por favor, seleccione un proceso de la tabla para eliminar.",
                    "Ningún proceso seleccionado",
                    JOptionPane.WARNING_MESSAGE);
            return; // Sale del método para evitar errores al intentar borrar nada
        }

        // Recupera el objeto Proceso de la fila seleccionada para poder mostrar su ID en el mensaje
        Proceso proceso = tableModel.getProceso(filaSeleccionada);

        // Lanza una ventana de confirmación (SÍ/NO) para evitar borrados accidentales
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Está seguro que desea eliminar el proceso P" + proceso.getId() + "?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        // Si el usuario presiona el botón "SÍ" (YES_OPTION)...
        if (confirmacion == JOptionPane.YES_OPTION) {
            // Llama al modelo de la tabla para que elimine físicamente el proceso de la lista y de la vista
            tableModel.eliminarProceso(filaSeleccionada);
            
            // Informa al usuario que el proceso con ese ID específico fue borrado con éxito
            JOptionPane.showMessageDialog(this,
                    "Proceso P" + proceso.getId() + " eliminado correctamente.",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void agregarProceso() {
        try {
            // 1. CAPTURA DE DATOS: Lee y valida que los textos de los campos sean números positivos
            // 'txtLlegada' es el segundo en que el proceso entra al sistema
            int llegada = leerEnteroPositivo(txtLlegada.getText(), "Llegada");
            
            // 'rafaga' es el tiempo total de trabajo que el proceso requiere en el CPU
            int rafaga = leerEnteroPositivo(txtRafaga.getText(), "Ráfaga", true);
            
            // 'quantum' es el tiempo de turno personalizado que tendrá este proceso (Quantum Variable)
            int quantum = leerEnteroPositivo(txtQuantumProceso.getText(), "Quantum", true); // Obligatorio

            // 2. OPERACIONES DE E/S: Llama a la función que convierte los guiones (ej: 1-3) 
            // en una lista de objetos de Entrada/Salida, validando que no superen la ráfaga
            List<OperacionES> listaES = leerOperacionesES(rafaga);

            // 3. CREACIÓN: Instancia el objeto Proceso con los valores recolectados
            Proceso proceso = new Proceso(llegada, rafaga, quantum, listaES);
            
            // Establece el estado inicial en NUEVO para que aparezca correctamente en la tabla/interfaz
            proceso.setEstado(EnumEstadoProceso.NUEVO);
            
            // Asigna el Quantum Variable específicamente a este proceso para que el Planificador lo reconozca
            proceso.setQuantumVariable(quantum); 

            // 4. ACTUALIZACIÓN: Envía el nuevo proceso al modelo de la tabla para que se muestre en pantalla
            tableModel.agregarProceso(proceso);
            
            // Borra el texto de los campos del formulario para permitir el ingreso de un nuevo proceso
            limpiarCamposFormulario();

        } catch (NumberFormatException ex) {
            // Si el usuario escribió letras o números negativos, atrapa el error y muestra el mensaje
            mostrarErrorValidacion(ex.getMessage());
        }
    }
    
    private int leerEnteroPositivo(String texto, String campo) {
        return leerEnteroPositivo(texto, campo, false);
    }

    private int leerEnteroPositivo(String texto, String campo, boolean estricto) {
        int valor = Integer.parseInt(texto.trim());
        if (estricto ? valor <= 0 : valor < 0) {
            throw new NumberFormatException(campo + " debe ser " + (estricto ? "> 0" : "≥ 0"));
        }
        return valor;
    }

    private List<OperacionES> leerOperacionesES(int rafagaTotal) {
        // 1. CAPTURA: Obtiene el texto de los campos, eliminando espacios en blanco innecesarios
        String textoMomentos = txtIntervaloES.getText().trim();
        String textoDuraciones = txtDuracionES.getText().trim();

        // Si ambos campos están vacíos, significa que el proceso no hace E/S. Retorna una lista vacía.
        if (textoMomentos.isEmpty() && textoDuraciones.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. FRAGMENTACIÓN: Divide las cadenas de texto por cada guion "-" para obtener cada número por separado
        String[] momentos = textoMomentos.split("-");
        String[] duraciones = textoDuraciones.split("-");

        // 3. VALIDACIÓN DE PARES: Verifica que por cada momento de inicio haya una duración definida
        if (momentos.length != duraciones.length) {
            throw new NumberFormatException("La cantidad de operaciones y duraciones no coincide.");
        }

        // 4. PROCESAMIENTO: Prepara la lista de objetos y una variable para controlar el orden
        List<OperacionES> listaES = new ArrayList<>();
        int ultimoMomento = -1; // Almacena el momento de la E/S anterior para comparar con la siguiente

        // Bucle para recorrer y validar cada una de las operaciones ingresadas
        for (int i = 0; i < momentos.length; i++) {
            // Convierte el fragmento de texto a número entero positivo
            int momento = leerEnteroPositivo(momentos[i], "Momento E/S", true);
            int duracion = leerEnteroPositivo(duraciones[i], "Duración E/S", true);

            // --- SUBFASE DE VALIDACIÓN LÓGICA ---

            // A. VALIDACIÓN: El momento de la E/S debe ocurrir MIENTRAS el proceso existe (dentro de su ráfaga)
            if (momento >= rafagaTotal) {
                throw new NumberFormatException("El momento E/S (" + momento + 
                    ") no puede ser mayor o igual a la ráfaga total (" + rafagaTotal + ").");
            }

            // B. VALIDACIÓN: Las E/S deben ser secuenciales (Crecientes). No puedes hacer una en el seg 3 y luego una en el 1.
            if (momento <= ultimoMomento) {
                throw new NumberFormatException("Los operaciones de E/S de valor distinto y de orden creciente (ej: 1-2-4)" +
                    "(Error en: " + momento + ").");
            }

            // Si pasa las pruebas, se crea el objeto OperacionES y se añade a la lista del proceso
            listaES.add(new OperacionES(momento, duracion));
            
            // Actualiza el 'ultimoMomento' para que la siguiente operación se compare con esta
            ultimoMomento = momento;
        }

        // Retorna la lista de operaciones lista para ser asignada al Proceso
        return listaES;
    } 
    
    private Proceso crearProceso(int llegada, int rafaga,int quantum, List<OperacionES> listaES) {
        Proceso proceso = new Proceso(llegada, rafaga,quantum, listaES);
        proceso.setEstado(EnumEstadoProceso.NUEVO);
        return proceso;
    }


    private void limpiarCamposFormulario() {
        txtLlegada.setText("");
        txtRafaga.setText("");
        txtIntervaloES.setText("");
        txtDuracionES.setText("");
        txtQuantumProceso.setText("");
    }

    private void mostrarErrorValidacion(String mensaje) {
        String mensajeCompleto = "Error en los datos ingresados:\n" + mensaje;
        JOptionPane.showMessageDialog(this, mensajeCompleto, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void iniciarSimulacion() {
        try {
            // 1. VERIFICACIÓN: Revisa si el usuario agregó al menos un proceso a la tabla
            if (tableModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No hay procesos para ejecutar.");
                return; // Detiene el inicio si la tabla está vacía
            }

            // 2. BLOQUEO DE INTERFAZ: Desactiva botones de agregar, editar o eliminar
            // Esto evita que se modifiquen los datos mientras el algoritmo está calculando
            setEstadoEdicion(false); 
            btnPausa.setEnabled(true); // Activa el botón de pausa para que el usuario tenga control
            btnIniciar.setText("Ejecutando..."); // Cambia el texto del botón para indicar actividad

            // 3. RESETEO DE DATOS: Limpia cualquier residuo de simulaciones anteriores
            // Pone los tiempos ejecutados en 0, marca OES como pendientes, etc.
            for (Proceso p : tableModel.getProcesos()) {
                p.reiniciarParaSimulacion();
            }

            // 4. INICIALIZACIÓN DEL MOTOR: Crea una nueva instancia del Planificador Round Robin
            // Le pasa la lista de procesos que el usuario configuró en la tabla
            scheduler = new PlanificadorRoundRobin(tableModel.getProcesos());
            
            // 5. ARRANQUE: Inicia el Timer (el reloj que llamará al método 'tick' repetidamente)
            iniciarTimerSimulacion();

        } catch (NumberFormatException ex) {
            // En caso de que algún dato numérico esté mal formado al momento de arrancar
            setEstadoEdicion(true); // Rehabilitar botones de edición para que el usuario corrija el error
            mostrarErrorValidacion("Error en configuración: " + ex.getMessage());
        }
    }

    private void iniciarTimerSimulacion() {
        /**
         * 1. CREACIÓN DEL RELOJ (Timer):
         * Se configura para que se dispare cada 1000 milisegundos (1 segundo).
         * Cada vez que pase ese segundo, ejecuta el método 'ejecutarTickSimulacion()'.
         * 'ejecutarTickSimulacion' es el que mueve los procesos de una cola a otra.
         */
        timer = new Timer(1000, e -> ejecutarTickSimulacion());

        // 2. CONTROL DE ESTADO: 
        // Asegura que la bandera de pausa esté desactivada para que el motor pueda correr.
        enPausa = false;

        // 3. ARRANQUE:
        // Pone en marcha el cronómetro. A partir de este momento, el Gantt empezará a dibujarse.
        timer.start();
    }
    
    private void ejecutarTickSimulacion() {
        scheduler.tick();
        actualizarInterfaz();

        if (scheduler.haTerminado()) {
            timer.stop();
            setEstadoEdicion(true); // --- DESBLOQUEO AQUÍ ---
            btnIniciar.setText("Iniciar RR");
            mostrarMensajeFinalizacion();
        }
    }

    private void actualizarInterfaz() {
        tableModel.actualizarTabla();
        actualizarCPU();
        actualizarHistorialCola();
        actualizarES();
        actualizarTiempos();
    }

    private void mostrarMensajeFinalizacion() {
        JOptionPane.showMessageDialog(this,
                "Simulación Round Robin terminada",
                "Simulación Completada",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void togglePausa() {
        if (timer == null)
            return;

        if (enPausa) {
            timer.start();
            btnPausa.setText("Pausar");
        } else {
            timer.stop();
            btnPausa.setText("Reanudar");
        }
        enPausa = !enPausa;
    }

    // ======================
    // ACTUALIZACIÓN DE INTERFAZ
    // ======================

    private void actualizarCPU() {
        // 1. LIMPIEZA: Borra todo lo que haya actualmente en el panel del CPU 
        // para poder dibujar el estado actualizado desde cero.
        panelCPU.removeAll();

        // 2. OBTENCIÓN DE DATOS: Le pide al planificador la lista de procesos 
        // que están ocupando el CPU actualmente (en Round Robin suele ser uno, o ninguno).
        List<PlanificadorRoundRobin.EjecucionCPU> ejecuciones = scheduler.getEjecucionesCPU();
        
        // Si no hay procesos ejecutándose, refresca el panel vacío y termina el método.
        if (ejecuciones.isEmpty()) {
            panelCPU.revalidate(); // Avisa al gestor de diseño que el panel cambió
            panelCPU.repaint();    // Redibuja el panel (ahora vacío)
            return;
        }

        // 3. CONTENEDOR VISUAL: Crea un panel intermedio para organizar los 
        // cuadritos de los procesos de forma centrada y ordenada.
        JPanel panelContenedor = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));

        // 4. DIBUJO INDIVIDUAL: Por cada proceso que esté en ejecución...
        for (PlanificadorRoundRobin.EjecucionCPU ejec : ejecuciones) {
            // Llama a una función auxiliar para crear el "cuadrito" visual con el ID y color del proceso.
            JPanel panelProceso = crearPanelProcesoCPU(ejec);
            // Agrega ese cuadrito al contenedor.
            panelContenedor.add(panelProceso);
        }

        // 5. ACTUALIZACIÓN FINAL: Agrega el contenedor lleno al panel principal del CPU.
        panelCPU.add(panelContenedor);
        
        // Fuerza a Java a reconocer los cambios visuales y a mostrar la nueva información en pantalla.
        panelCPU.revalidate();
        panelCPU.repaint();
    }

    private JPanel crearPanelProcesoCPU(PlanificadorRoundRobin.EjecucionCPU ejec) {
        // 1. CONFIGURACIÓN DEL RECUADRO: Crea el contenedor principal del bloque
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(70, 50)); // Define un tamaño fijo para que todos se vean iguales
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1)); // Le pone un borde gris fino

        // 2. ETIQUETA DEL PROCESO: El nombre (ej: P1, P2)
        JLabel lblProceso = new JLabel("P" + ejec.getProceso().getId(), SwingConstants.CENTER);
        lblProceso.setOpaque(true); // Necesario para que el color de fondo se pueda ver
        
        // Asigna el color único que le corresponde a este proceso según su ID
        lblProceso.setBackground(getColorForProcess(ejec.getProceso().getId()));
        lblProceso.setFont(new Font("Arial", Font.BOLD, 14)); // Texto en negrita y más grande
        panel.add(lblProceso, BorderLayout.CENTER); // Lo coloca en el centro del recuadro

        // 3. ETIQUETA DE TIEMPO: Muestra el rango de tiempo (ej: 0-3)
        // Lógica: Si el proceso aún está corriendo, muestra el inicio y un "?" (ej: 5-?)
        // Si ya terminó su turno, muestra el rango completo (ej: 5-8).
        JLabel lblTiempo = new JLabel(
                ejec.getTiempoFin() != -1 ? ejec.getTiempoInicio() + "-" + ejec.getTiempoFin()
                                        : ejec.getTiempoInicio() + "-?",
                SwingConstants.CENTER);
        
        lblTiempo.setFont(new Font("Arial", Font.PLAIN, 11)); // Fuente más pequeña para el tiempo
        lblTiempo.setForeground(Color.DARK_GRAY); // Color gris oscuro para que no resalte más que el ID
        panel.add(lblTiempo, BorderLayout.SOUTH); // Lo coloca en la parte inferior del recuadro

        // Retorna el "cuadrito" terminado para que el método actualizarCPU lo muestre
        return panel;
    }

    private void actualizarHistorialCola() {
        // 1. LIMPIEZA: Borra todos los componentes visuales anteriores del panel de la cola.
        // Esto es necesario para redibujar la cola desde cero en cada tick de la simulación.
        panelHistorialCola.removeAll();

        // 2. RECORRIDO: Obtiene la lista de procesos que están actualmente en la 
        // Cola de Procesos Listos (CPL) desde el planificador.
        for (Proceso p : scheduler.getHistorialCPL()) {
            
            // SEGURIDAD: Si por algún error un proceso en la lista es nulo, 
            // lo ignora y pasa al siguiente para evitar que el programa se cierre.
            if (p == null)
                continue;

            // 3. CREACIÓN VISUAL: Llama a un método auxiliar que crea una pequeña 
            // etiqueta (JLabel) con el nombre y color del proceso.
            JLabel lbl = crearEtiquetaProceso(p);
            
            // 4. ADICIÓN: Agrega la etiqueta al panel para que sea visible en la fila.
            panelHistorialCola.add(lbl);
        }

        // 5. REFRESCO: Indica a Swing (la librería gráfica) que el contenido del panel 
        // ha cambiado y debe volver a pintarlo en la ventana.
        panelHistorialCola.revalidate();
        panelHistorialCola.repaint();
    }

    private JLabel crearEtiquetaProceso(Proceso proceso) {
        // 1. CREACIÓN: Instancia la etiqueta con el nombre corto del proceso (ej: "P1")
        JLabel lbl = new JLabel("P" + proceso.getId());
        
        // Hace que la etiqueta sea opaca para que el color de fondo (Background) sea visible
        lbl.setOpaque(true);

        /**
         * 2. LÓGICA DE COLORES:
         * El color de la ficha indica el historial o estado del proceso:
         * - ROJO: Si el proceso ya pasó por una E/S (fue bloqueado) o si ya terminó su ejecución.
         * - VERDE: Si es un proceso "limpio" que nunca ha dejado el CPU por un bloqueo de E/S.
         */
        if (proceso.fueBloqueadoAlgunaVez() || proceso.getEstado() == EnumEstadoProceso.TERMINADO) {
            lbl.setBackground(new Color(255, 100, 100)); // Rojo suave
        } else {
            lbl.setBackground(new Color(200, 255, 200)); // Verde suave
        }

        // 3. ESTILO VISUAL:
        // Le pone un borde negro sólido para resaltar la ficha
        lbl.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        // Define el tamaño de la ficha (50 pixeles de ancho por 30 de alto)
        lbl.setPreferredSize(new Dimension(50, 30));
        
        // Centra el texto "P1" tanto horizontal como verticalmente dentro de la ficha
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Retorna la etiqueta lista para ser añadida a la interfaz (Cola de Listos, Bloqueados, etc.)
        return lbl;
    }

    private void actualizarES() {
        // 1. LIMPIEZA: Borra todos los componentes actuales del panel de E/S.
        // Esto permite redibujar el estado de los procesos bloqueados en cada segundo de la simulación.
        panelES.removeAll();

        // 2. OBTENCIÓN DE DATOS: Le pide al planificador la lista de procesos 
        // que están actualmente realizando una operación de Entrada/Salida.
        List<PlanificadorRoundRobin.EjecucionES> ejecucionesES = scheduler.getEjecucionesES();

        // 3. DECISIÓN VISUAL:
        // Si la lista está vacía, no hay procesos bloqueados.
        if (ejecucionesES.isEmpty()) {
            // Muestra un texto o panel indicando que el canal de E/S está libre.
            agregarMensajeVacioES();
        } else {
            // Si hay procesos, llama al método que dibuja las barras o etiquetas de cada uno.
            agregarEjecucionesES(ejecucionesES);
        }

        // 4. ACTUALIZACIÓN GRÁFICA: Obliga al panel a refrescarse para que el usuario
        // vea los cambios (procesos entrando o saliendo de E/S) inmediatamente.
        panelES.revalidate();
        panelES.repaint();
    }

    private void agregarMensajeVacioES() {
        JLabel lblVacio = new JLabel("No hay procesos en operaciones E/S", SwingConstants.CENTER);
        lblVacio.setForeground(Color.GRAY);
        lblVacio.setFont(new Font("Arial", Font.ITALIC, 12));
        panelES.add(lblVacio);
    }

    private void agregarEjecucionesES(List<PlanificadorRoundRobin.EjecucionES> ejecuciones) {
        /**
         * 1. RECORRIDO (Bucle):
         * Itera sobre la lista de procesos que el planificador marcó como 
         * actualmente activos en un dispositivo de Entrada/Salida.
         */
        for (PlanificadorRoundRobin.EjecucionES ejec : ejecuciones) {
            
            /**
             * 2. CREACIÓN DE COMPONENTE:
             * Para cada ejecución de E/S, llama al método auxiliar que construye 
             * el pequeño panel visual (con el ID del proceso, tiempos, etc.).
             */
            JPanel panelEjecES = crearPanelEjecucionES(ejec);
            
            /**
             * 3. ADICIÓN AL CONTENEDOR:
             * Inserta el panel recién creado dentro de 'panelES', que es el 
             * área designada en la ventana principal para mostrar los procesos bloqueados.
             */
            panelES.add(panelEjecES);
        }
    }

    private JPanel crearPanelEjecucionES(PlanificadorRoundRobin.EjecucionES ejec) {
        // 1. ESTRUCTURA: Crea el panel principal con un diseño de bordes y tamaño fijo
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(90, 60));

        // 2. LÓGICA DE ESTADO: Verifica si el proceso está actualmente en E/S o ya terminó
        // Le pregunta al scheduler si el tiempo actual está dentro del rango de esta E/S
        boolean enES = ejec.estaEnES(scheduler.getTiempoActual());
        Color colorFondo;
        Color colorBorde;

        // Si está en proceso de E/S, se pinta de Naranja (indicando "trabajando en periférico")
        if (enES) {
            colorFondo = new Color(255, 220, 180); // Naranja claro
            colorBorde = new Color(255, 140, 0);   // Naranja fuerte
        } else {
            // Si ya terminó su tiempo de E/S pero sigue en el panel, se pinta de Rojo
            colorFondo = new Color(255, 100, 100); // Rojo claro
            colorBorde = Color.RED;               // Rojo fuerte
        }

        // Aplica el borde con el color definido arriba y un grosor de 2 pixeles
        panel.setBorder(BorderFactory.createLineBorder(colorBorde, 2));

        // 3. IDENTIFICACIÓN: Crea la etiqueta con el nombre del proceso (ej: P1)
        JLabel lblProceso = new JLabel("P" + ejec.getProceso().getId(), SwingConstants.CENTER);
        lblProceso.setOpaque(true); // Permite que se vea el color de fondo
        lblProceso.setBackground(colorFondo);
        lblProceso.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(lblProceso, BorderLayout.CENTER); // Se coloca en la parte central

        // 4. DATOS TÉCNICOS: Muestra información de la ráfaga y el tiempo de salida
        // El formato es: [Ráfaga que le queda] → [Segundo en que sale de E/S]
        JLabel lblInfo = new JLabel(
                String.format("%d→%d", ejec.getRafagaRestante(), ejec.getTiempoSalida()),
                SwingConstants.CENTER);
        lblInfo.setFont(new Font("Arial", Font.PLAIN, 10));
        lblInfo.setForeground(Color.DARK_GRAY);
        panel.add(lblInfo, BorderLayout.SOUTH); // Se coloca en la parte inferior

        // 5. AYUDA VISUAL: Configura el mensaje flotante (tooltip) que aparece al pasar el mouse
        // Muestra detalles adicionales sobre la operación de E/S
        configurarTooltipES(panel, ejec, enES);

        // Retorna el componente listo para ser añadido al panelES
        return panel;
    }

    private void configurarTooltipES(JPanel panel, PlanificadorRoundRobin.EjecucionES ejec, boolean enES) {
        String estado = enES ? "ACTUALMENTE en E/S" : "E/S completada";
        String tooltip = String.format(
                "<html><b>Proceso P%d</b><br>" +
                        "• Estado: %s<br>" +
                        "• Entró a E/S en: t=%d ms<br>" +
                        "• Saldrá de E/S en: t=%d ms<br>" +
                        "• Ráfaga al entrar: %d ms<br>" +
                        "• Duración E/S: %d ms</html>",
                ejec.getProceso().getId(),
                estado,
                ejec.getTiempoEntrada(),
                ejec.getTiempoSalida(),
                ejec.getRafagaRestante(),
                ejec.getTiempoSalida() - ejec.getTiempoEntrada());
        panel.setToolTipText(tooltip);
    }

    // ======================
    // TIEMPOS DE ESPERA Y EJECUCIÓN (COMBINADO)
    // ======================

    private void actualizarTiempos() {
        panelTiempos.removeAll();

        List<Proceso> procesos = tableModel.getProcesos();

        if (procesos.isEmpty()) {
            JLabel lblVacio = new JLabel("No hay procesos registrados", SwingConstants.CENTER);
            lblVacio.setForeground(Color.GRAY);
            lblVacio.setFont(new Font("Arial", Font.ITALIC, 12));
            panelTiempos.add(lblVacio, BorderLayout.CENTER);
        } else {
            // Panel con layout vertical para mostrar texto
            JPanel panelContenido = new JPanel();
            panelContenido.setLayout(new BoxLayout(panelContenido, BoxLayout.Y_AXIS));
            panelContenido.setBackground(Color.WHITE);
            panelContenido.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            int sumaTiemposEspera = 0;
            int sumaTiemposEjecucion = 0;
            int procesosTerminados = 0;

            // Título de Tiempos de Espera
            JLabel lblTituloEspera = new JLabel("TIEMPOS DE ESPERA");
            lblTituloEspera.setFont(new Font("Monospaced", Font.BOLD, 12));
            lblTituloEspera.setForeground(new Color(0, 100, 0));
            lblTituloEspera.setAlignmentX(Component.LEFT_ALIGNMENT);
            panelContenido.add(lblTituloEspera);
            panelContenido.add(Box.createVerticalStrut(3));

            // Tiempos de espera para cada proceso
            for (Proceso p : procesos) {
                int tiempoEspera = p.calcularTiempoEspera();
                String textoFormula = crearTextoTiempoEspera(p, tiempoEspera);

                JLabel lblFormula = new JLabel(textoFormula);
                lblFormula.setFont(new Font("Monospaced", Font.PLAIN, 12));
                lblFormula.setAlignmentX(Component.LEFT_ALIGNMENT);
                panelContenido.add(lblFormula);

                if (tiempoEspera >= 0) {
                    sumaTiemposEspera += tiempoEspera;
                    procesosTerminados++;
                }
            }

            // Promedio de espera
            String textoPromedioEspera = crearTextoPromedio("espera", sumaTiemposEspera, procesosTerminados);
            JLabel lblPromedioEspera = new JLabel(textoPromedioEspera);
            lblPromedioEspera.setFont(new Font("Monospaced", Font.BOLD, 12));
            lblPromedioEspera.setForeground(new Color(0, 100, 0));
            lblPromedioEspera.setAlignmentX(Component.LEFT_ALIGNMENT);
            panelContenido.add(lblPromedioEspera);

            // Separador entre secciones
            panelContenido.add(Box.createVerticalStrut(10));

            // Título de Tiempos de Ejecución
            JLabel lblTituloEjecucion = new JLabel("TIEMPOS DE EJECUCIÓN");
            lblTituloEjecucion.setFont(new Font("Monospaced", Font.BOLD, 12));
            lblTituloEjecucion.setForeground(new Color(0, 0, 150));
            lblTituloEjecucion.setAlignmentX(Component.LEFT_ALIGNMENT);
            panelContenido.add(lblTituloEjecucion);
            panelContenido.add(Box.createVerticalStrut(3));

            // Tiempos de ejecución para cada proceso
            for (Proceso p : procesos) {
                int tiempoEjecucion = p.calcularTiempoEjecucion();
                String textoFormula = crearTextoTiempoEjecucion(p, tiempoEjecucion);

                JLabel lblFormula = new JLabel(textoFormula);
                lblFormula.setFont(new Font("Monospaced", Font.PLAIN, 12));
                lblFormula.setAlignmentX(Component.LEFT_ALIGNMENT);
                panelContenido.add(lblFormula);

                if (tiempoEjecucion >= 0) {
                    sumaTiemposEjecucion += tiempoEjecucion;
                }
            }

            // Promedio de ejecución
            String textoPromedioEjecucion = crearTextoPromedio("ejecución", sumaTiemposEjecucion, procesosTerminados);
            JLabel lblPromedioEjecucion = new JLabel(textoPromedioEjecucion);
            lblPromedioEjecucion.setFont(new Font("Monospaced", Font.BOLD, 12));
            lblPromedioEjecucion.setForeground(new Color(0, 0, 150));
            lblPromedioEjecucion.setAlignmentX(Component.LEFT_ALIGNMENT);
            panelContenido.add(lblPromedioEjecucion);

            JScrollPane scrollContenido = new JScrollPane(panelContenido);
            scrollContenido.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollContenido.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollContenido.setBorder(null);

            panelTiempos.add(scrollContenido, BorderLayout.CENTER);
        }

        panelTiempos.revalidate();
        panelTiempos.repaint();
    }

    private String crearTextoTiempoEspera(Proceso proceso, int tiempoEspera) {
        if (tiempoEspera >= 0) {
            return String.format("P%d = %d - %d - %d - %d = %d ms",
                    proceso.getId(),
                    proceso.getTiempoFin(),
                    proceso.getTiempoLlegada(),
                    proceso.getRafagaCPU(),
                    proceso.getDuracionTotalES(),
                    tiempoEspera);
        } else {
            return String.format("P%d = En ejecución...", proceso.getId());
        }
    }

    private String crearTextoTiempoEjecucion(Proceso proceso, int tiempoEjecucion) {
        if (tiempoEjecucion >= 0) {
            return String.format("P%d = %d - %d = %d ms",
                    proceso.getId(),
                    proceso.getTiempoFin(),
                    proceso.getTiempoLlegada(),
                    tiempoEjecucion);
        } else {
            return String.format("P%d = En ejecución...", proceso.getId());
        }
    }

    private String crearTextoPromedio(String tipo, int suma, int procesosTerminados) {
        if (procesosTerminados > 0) {
            double promedio = (double) suma / procesosTerminados;
            return String.format("Promedio %s = (%d) / %d = %.2f ms", tipo, suma, procesosTerminados, promedio);
        } else {
            return String.format("Promedio %s = N/A (ningún proceso terminado)", tipo);
        }
    }

    // ======================
    // MÉTODOS AUXILIARES
    // ======================



    private Color getColorForProcess(int id) {
        Color[] colors = {
                new Color(180, 220, 255), // Azul claro - P1
                new Color(180, 255, 180), // Verde claro - P2
                new Color(255, 220, 180), // Naranja claro - P3
                new Color(255, 180, 220), // Rosa claro - P4
                new Color(220, 180, 255), // Lila claro - P5
                new Color(180, 255, 255), // Cyan claro - P6
                new Color(255, 255, 180), // Amarillo claro - P7
                new Color(220, 220, 220) // Gris claro - P8
        };

        return colors[(id - 1) % colors.length];
    }

    // Metodos de reinicio

    private void reiniciarTodo() {
        System.out.println("\n[REINICIO] Iniciando reinicio completo...");

        // 1. PREGUNTAR CONFIRMACIÓN SI HAY DATOS
        boolean hayProcesos = tableModel.getRowCount() > 0;
        boolean simulacionActiva = timer != null && timer.isRunning();

        if (hayProcesos || simulacionActiva) {
            int opcion = JOptionPane.showConfirmDialog(
                    this,
                    "¿Está seguro de reiniciar todo?\n\n" +
                            "• " + (simulacionActiva ? "Simulación en curso será detenida\n" : "") +
                            "• " + (hayProcesos ? tableModel.getRowCount() + " procesos serán eliminados\n" : "") +
                            "• Todos los paneles se limpiarán\n\n" +
                            "Esta acción no se puede deshacer.",
                    "Confirmar reinicio completo",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (opcion != JOptionPane.YES_OPTION) {
                System.out.println("[REINICIO] Cancelado por el usuario");
                return;
            }
        }

        try {
            // 2. DETENER SIMULACIÓN ACTUAL
            if (simulacionActiva) {
                System.out.println("[REINICIO] Deteniendo timer...");
                timer.stop();
                timer = null;
                enPausa = false;
                if (btnPausa != null) {
                    btnPausa.setText("Pausar");
                }
            }

            // 3. REINICIAR CONTADOR GLOBAL DE PROCESOS
            System.out.println("[REINICIO] Reiniciando contador de IDs...");
            Proceso.reiniciarContadorGlobal();

            // 4. LIMPIAR TABLA DE PROCESOS
            System.out.println("[REINICIO] Limpiando tabla de procesos...");
            tableModel.limpiar();

            // 5. REINICIAR PLANIFICADOR SI EXISTE
            if (scheduler != null) {
                System.out.println("[REINICIO] Reiniciando planificador interno...");
                scheduler.reiniciar();
                scheduler = null;
            }

            // 6. LIMPIAR PANELES VISUALES
            System.out.println("[REINICIO] Limpiando paneles visuales...");
            limpiarPanel(panelHistorialCola);
            limpiarPanel(panelES);
            limpiarPanel(panelCPU);
            limpiarPanel(panelTiempos);

            // 7. RESETEAR CAMPOS DEL FORMULARIO A VALORES POR DEFECTO
            System.out.println("[REINICIO] Reseteando formulario...");
            resetearFormulario();

            // 8. FEEDBACK VISUAL TEMPORAL
            if (btnReiniciar != null) {
                btnReiniciar.setBackground(new Color(180, 255, 180));
                btnReiniciar.setText("✓ REINICIADO");

                // Temporizador para restaurar texto después de 2 segundos
                Timer timerFeedback = new Timer(2000, e -> {
                    btnReiniciar.setText("Vaciar");
                    btnReiniciar.setBackground(null);
                    ((Timer) e.getSource()).stop();
                });
                timerFeedback.setRepeats(false);
                timerFeedback.start();
            }

            // 9. MENSAJE FINAL
            JOptionPane.showMessageDialog(this,
                    "✅ Sistema reiniciado exitosamente\n\n" +
                            "Estado actual:\n" +
                            "• Contador de procesos: 1 (reiniciado)\n" +
                            "• Tabla de procesos: vacía\n" +
                            "• Paneles visuales: limpios\n" +
                            "• Formulario: reseteado\n\n" +
                            "Listo para nueva simulación.",
                    "Reinicio completo finalizado",
                    JOptionPane.INFORMATION_MESSAGE);

            System.out.println("[REINICIO] Completo exitosamente");

        } catch (Exception ex) {
            System.err.println("[REINICIO ERROR] " + ex.getMessage());
            ex.printStackTrace();

            JOptionPane.showMessageDialog(this,
                    "❌ Error durante el reinicio:\n" + ex.getMessage() +
                            "\n\nPor favor, cierre y reinicie la aplicación.",
                    "Error crítico",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarPanel(JPanel panel) {
        panel.removeAll();
        panel.revalidate();
        panel.repaint();
    }

    /**
     * Resetea todos los campos del formulario a valores por defecto
     */
    private void resetearFormulario() {
        txtLlegada.setText("");
        txtRafaga.setText("");
        txtIntervaloES.setText("");
        txtDuracionES.setText("");
        txtQuantumProceso.setText(""); // Deja el cuadro en blanco para el nuevo proceso
        txtLlegada.requestFocus();
    }

    private void setEstadoEdicion(boolean habilitado) {
    // Bloquear/Desbloquear botones
    btnAgregar.setEnabled(habilitado);
    btnEditar.setEnabled(habilitado);
    btnEliminar.setEnabled(habilitado);
    btnIniciar.setEnabled(habilitado);
    
    // Bloquear/Desbloquear campos de texto para evitar cambios accidentales
    txtLlegada.setEnabled(habilitado);
    txtRafaga.setEnabled(habilitado);
    txtIntervaloES.setEnabled(habilitado);
    txtDuracionES.setEnabled(habilitado);
    txtQuantumProceso.setEnabled(habilitado);

    // Bloquear la tabla para que no se pueda seleccionar ni editar nada
    tablaProcesos.setEnabled(habilitado);
}

}