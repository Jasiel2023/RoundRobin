package views;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import logic.PlanificadorRoundRobin;
import logic.PoliticaQuantum;
import logic.QuantumFijo;
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
    private JLabel lblQuantumFijo, lblQuantumProceso;

    // Paneles de visualización
    private JPanel panelHistorialCola, panelES, panelCPU;

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
        actualizarVistaQuantum(); // Estado inicial
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
        // Campos de texto
        txtLlegada = new JTextField(5);
        txtRafaga = new JTextField(5);
        txtQuantum = new JTextField(5);
        txtQuantumProceso = new JTextField(5);
        txtIntervaloES = new JTextField(5);
        txtDuracionES = new JTextField(5);

        // Combo box
        comboTipoRR = new JComboBox<>(new String[] {
                "Quantum fijo", "Quantum por proceso"
        });

        // Etiquetas
        lblQuantumFijo = new JLabel("Quantum fijo:");
        lblQuantumProceso = new JLabel("Quantum proceso:");

        // Botones
        btnAgregar = new JButton("Agregar proceso");
        btnEditar = new JButton("Editar proceso");
        btnEliminar = new JButton("Eliminar proceso");
        btnIniciar = new JButton("Iniciar RR");
        btnPausa = new JButton("Pausar");
        btnReiniciar = new JButton("Vaciar");
    }

    private void agregarComponentesAlFormulario(JPanel panelForm) {
        // Tipo RR
        panelForm.add(new JLabel("Tipo RR:"));
        panelForm.add(comboTipoRR);

        // Llegada y ráfaga
        panelForm.add(new JLabel("Llegada:"));
        panelForm.add(txtLlegada);
        panelForm.add(new JLabel("Ráfaga:"));
        panelForm.add(txtRafaga);

        // Campos E/S
        panelForm.add(new JLabel("Momentos E/S (ej: 1-3-5):"));
        panelForm.add(txtIntervaloES);
        panelForm.add(new JLabel("Duraciones E/S (ej: 2-3-4):"));
        panelForm.add(txtDuracionES);

        // Quantum
        panelForm.add(lblQuantumFijo);
        panelForm.add(txtQuantum);
        panelForm.add(lblQuantumProceso);
        panelForm.add(txtQuantumProceso);
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

        // Panel de estados (cola, E/S, CPU) en un scroll
        JPanel panelEstados = crearPanelEstados();
        JScrollPane scrollEstados = new JScrollPane(panelEstados);
        scrollEstados.setPreferredSize(new Dimension(880, 350));

        // Panel central (tabla + estados)
        JPanel panelCentro = new JPanel(new BorderLayout(5, 5));
        panelCentro.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panelCentro.add(scroll, BorderLayout.NORTH);
        panelCentro.add(scrollEstados, BorderLayout.CENTER);

        add(panelCentro, BorderLayout.CENTER);
    }

    private void crearPanelesDeEstado() {
        panelHistorialCola = crearPanelConTitulo("Historial de Cola de Listos", 850, 100);
        panelES = crearPanelConTitulo("Operaciones de Entrada/Salida (Historial)", 850, 100);
        panelCPU = crearPanelConTitulo("CPU - Historial de ejecución", 850, 100);
    }

    private JPanel crearPanelConTitulo(String titulo, int ancho, int alto) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder(titulo));
        panel.setPreferredSize(new Dimension(ancho, alto));
        return panel;
    }

    private JPanel crearPanelEstados() {
        JPanel panelEstados = new JPanel(new GridLayout(3, 1, 5, 5));
        panelEstados.setPreferredSize(new Dimension(850, 320));
        panelEstados.add(panelHistorialCola);
        panelEstados.add(panelES);
        panelEstados.add(panelCPU);
        return panelEstados;
    }

    private void configurarListeners() {
        btnAgregar.addActionListener(e -> agregarProceso());
        btnEditar.addActionListener(e -> editarProceso());
        btnEliminar.addActionListener(e -> eliminarProceso());
        btnIniciar.addActionListener(e -> iniciarSimulacion());
        btnPausa.addActionListener(e -> togglePausa());
        btnReiniciar.addActionListener(e -> reiniciarTodo());
        comboTipoRR.addActionListener(e -> actualizarVistaQuantum());
    }

    // ======================
    // LÓGICA DE NEGOCIO
    // ======================

    private void editarProceso() {
        int filaSeleccionada = tablaProcesos.getSelectedRow();

        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this,
                    "Por favor, seleccione un proceso de la tabla para editar.",
                    "Ningún proceso seleccionado",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Proceso procesoOriginal = tableModel.getProceso(filaSeleccionada);

        // Crear diálogo de edición
        JDialog dialogoEdicion = new JDialog(this, "Editar Proceso P" + procesoOriginal.getId(), true);
        dialogoEdicion.setSize(400, 350);
        dialogoEdicion.setLocationRelativeTo(this);
        dialogoEdicion.setLayout(new BorderLayout(10, 10));

        // Panel de campos
        JPanel panelCampos = new JPanel(new GridLayout(6, 2, 10, 10));
        panelCampos.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Campos de edición
        JTextField txtLlegadaEdit = new JTextField(String.valueOf(procesoOriginal.getTiempoLlegada()));
        JTextField txtRafagaEdit = new JTextField(String.valueOf(procesoOriginal.getRafagaCPU()));
        JTextField txtMomentosESEdit = new JTextField(
                procesoOriginal.getMomentosESOriginal().equals("No") ? "" : procesoOriginal.getMomentosESOriginal());
        JTextField txtDuracionesESEdit = new JTextField(
                procesoOriginal.getDuracionesESOriginal().equals("-") ? "" : procesoOriginal.getDuracionesESOriginal());
        JTextField txtQuantumEdit = new JTextField(
                procesoOriginal.getQuantumPersonal() == -1 ? "" : String.valueOf(procesoOriginal.getQuantumPersonal()));

        // Agregar campos al panel
        panelCampos.add(new JLabel("Tiempo de Llegada:"));
        panelCampos.add(txtLlegadaEdit);
        panelCampos.add(new JLabel("Ráfaga CPU:"));
        panelCampos.add(txtRafagaEdit);
        panelCampos.add(new JLabel("Momentos E/S (ej: 1-3-5):"));
        panelCampos.add(txtMomentosESEdit);
        panelCampos.add(new JLabel("Duraciones E/S (ej: 2-3-4):"));
        panelCampos.add(txtDuracionesESEdit);
        panelCampos.add(new JLabel("Quantum Personal:"));
        panelCampos.add(txtQuantumEdit);
        panelCampos.add(new JLabel("(Dejar vacío para usar global)"));
        panelCampos.add(new JLabel(""));

        dialogoEdicion.add(panelCampos, BorderLayout.CENTER);

        // Panel de botones
        JPanel panelBotonesDialogo = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton btnGuardar = new JButton("Guardar");
        JButton btnCancelar = new JButton("Cancelar");

        btnGuardar.addActionListener(ev -> {
            try {
                // Validar y leer datos
                int llegada = leerEnteroPositivo(txtLlegadaEdit.getText(), "Llegada");
                int rafaga = leerEnteroPositivo(txtRafagaEdit.getText(), "Ráfaga", true);

                // Leer operaciones E/S del diálogo
                String textoMomentos = txtMomentosESEdit.getText().trim();
                String textoDuraciones = txtDuracionesESEdit.getText().trim();
                List<OperacionES> listaES = new ArrayList<>();

                if (!textoMomentos.isEmpty() && !textoDuraciones.isEmpty()) {
                    String[] momentos = textoMomentos.split("-");
                    String[] duraciones = textoDuraciones.split("-");

                    if (momentos.length != duraciones.length) {
                        throw new NumberFormatException("Cantidad de momentos y duraciones no coincide");
                    }

                    for (int i = 0; i < momentos.length; i++) {
                        int momento = leerEnteroPositivo(momentos[i], "Momento E/S", true);
                        int duracion = leerEnteroPositivo(duraciones[i], "Duración E/S", true);
                        listaES.add(new OperacionES(momento, duracion));
                    }
                }

                // Crear nuevo proceso conservando el ID original
                Proceso procesoEditado = new Proceso(procesoOriginal.getId(), llegada, rafaga, listaES);
                procesoEditado.setEstado(EnumEstadoProceso.NUEVO);

                // Configurar quantum personal si se especificó
                String txtQuantumVal = txtQuantumEdit.getText().trim();
                if (!txtQuantumVal.isEmpty()) {
                    int quantum = leerEnteroPositivo(txtQuantumVal, "Quantum personal", true);
                    procesoEditado.setQuantumPersonal(quantum);
                }

                // Actualizar en la tabla
                tableModel.actualizarProceso(filaSeleccionada, procesoEditado);

                dialogoEdicion.dispose();
                JOptionPane.showMessageDialog(this,
                        "Proceso editado correctamente.",
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (NumberFormatException ex) {
                mostrarErrorValidacion(ex.getMessage());
            }
        });

        btnCancelar.addActionListener(ev -> dialogoEdicion.dispose());

        panelBotonesDialogo.add(btnGuardar);
        panelBotonesDialogo.add(btnCancelar);
        dialogoEdicion.add(panelBotonesDialogo, BorderLayout.SOUTH);

        dialogoEdicion.setVisible(true);
    }

    private void eliminarProceso() {
        int filaSeleccionada = tablaProcesos.getSelectedRow();

        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this,
                    "Por favor, seleccione un proceso de la tabla para eliminar.",
                    "Ningún proceso seleccionado",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Proceso proceso = tableModel.getProceso(filaSeleccionada);

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Está seguro que desea eliminar el proceso P" + proceso.getId() + "?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirmacion == JOptionPane.YES_OPTION) {
            tableModel.eliminarProceso(filaSeleccionada);
            JOptionPane.showMessageDialog(this,
                    "Proceso P" + proceso.getId() + " eliminado correctamente.",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void agregarProceso() {
        try {
            // Leer y validar datos básicos
            int llegada = leerEnteroPositivo(txtLlegada.getText(), "Llegada");
            int rafaga = leerEnteroPositivo(txtRafaga.getText(), "Ráfaga", true);

            // Leer operaciones E/S
            List<OperacionES> listaES = leerOperacionesES();

            // Crear proceso
            Proceso proceso = crearProceso(llegada, rafaga, listaES);

            // Configurar quantum personal si es necesario
            configurarQuantumPersonal(proceso);

            // Agregar a la tabla
            tableModel.agregarProceso(proceso);

            // Limpiar campos
            limpiarCamposFormulario();

        } catch (NumberFormatException ex) {
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
    
    
    private Proceso crearProceso(int llegada, int rafaga, List<OperacionES> listaES) {
        Proceso proceso = new Proceso(llegada, rafaga, listaES);
        proceso.setEstado(EnumEstadoProceso.NUEVO);
        return proceso;
    }

    private void configurarQuantumPersonal(Proceso proceso) {
        boolean esFijo = comboTipoRR.getSelectedIndex() == 0;
        if (!esFijo) {
            int quantum = leerEnteroPositivo(txtQuantumProceso.getText(), "Quantum proceso", true);
            proceso.setQuantumPersonal(quantum);
        }
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
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay procesos para ejecutar.");
            return;
        }

        // 1. BLOQUEO DE INTERFAZ
        setEstadoControles(false); 
        btnPausa.setEnabled(true);
        btnIniciar.setEnabled(false);
        btnIniciar.setText("Ejecutando...");

        // 2. LIMPIEZA DE PANELES VISUALES
        limpiarPanel(panelHistorialCola);
        limpiarPanel(panelES);
        limpiarPanel(panelCPU);

        // 3. PREPARAR PROCESOS
        List<Proceso> procesosCargados = tableModel.getProcesos();
        for (Proceso p : procesosCargados) {
            p.restaurarEstadoInicial();
        }
        tableModel.actualizarTabla();

        // 4. CONFIGURAR PLANIFICADOR
        PoliticaQuantum politica = crearPoliticaQuantum();
        scheduler = new PlanificadorRoundRobin(procesosCargados, politica);
        
        iniciarTimerSimulacion();

    } catch (NumberFormatException ex) {
        setEstadoControles(true); // Liberar si hubo error en datos
        btnIniciar.setEnabled(true);
        btnIniciar.setText("Iniciar RR");
        mostrarErrorValidacion("Error en los datos: " + ex.getMessage());
    } catch (Exception ex) {
        setEstadoControles(true);
        btnIniciar.setEnabled(true);
        JOptionPane.showMessageDialog(this, "Error inesperado: " + ex.getMessage());
    }
}

    private PoliticaQuantum crearPoliticaQuantum() {
        boolean esFijo = comboTipoRR.getSelectedIndex() == 0;

        if (esFijo) {
            int quantum = leerEnteroPositivo(txtQuantum.getText(), "Quantum fijo", true);
            tableModel.setQuantumGlobal(quantum);
            return new QuantumFijo(quantum);
        } else {
            return new logic.QuantumPorProceso();
        }
    }

    private void iniciarTimerSimulacion() {
        timer = new Timer(1000, e -> ejecutarTickSimulacion());
        enPausa = false;
        timer.start();
    }

    private void ejecutarTickSimulacion() {
        scheduler.tick();
        actualizarInterfaz();

        if (scheduler.haTerminado()) {
            timer.stop();
            
            // --- DESBLOQUEO AUTOMÁTICO AL TERMINAR ---
            setEstadoControles(true); 
            btnIniciar.setEnabled(true); 
            btnIniciar.setText("Reiniciar Simulación");
            btnPausa.setEnabled(false); // No tiene sentido pausar algo terminado
            btnPausa.setText("Pausar");
            enPausa = false;
            // -----------------------------------------
            
            mostrarMensajeFinalizacion();
        }
    }

    private void actualizarInterfaz() {
        tableModel.actualizarTabla();
        actualizarCPU();
        actualizarHistorialCola();
        actualizarES();
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
        panelCPU.removeAll();

        List<PlanificadorRoundRobin.EjecucionCPU> ejecuciones = scheduler.getEjecucionesCPU();
        if (ejecuciones.isEmpty()) {
            panelCPU.revalidate();
            panelCPU.repaint();
            return;
        }

        JPanel panelContenedor = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));

        for (PlanificadorRoundRobin.EjecucionCPU ejec : ejecuciones) {
            JPanel panelProceso = crearPanelProcesoCPU(ejec);
            panelContenedor.add(panelProceso);
        }

        panelCPU.add(panelContenedor);
        panelCPU.revalidate();
        panelCPU.repaint();
    }

    private JPanel crearPanelProcesoCPU(PlanificadorRoundRobin.EjecucionCPU ejec) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(70, 50));
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        // Etiqueta del proceso
        JLabel lblProceso = new JLabel("P" + ejec.getProceso().getId(), SwingConstants.CENTER);
        lblProceso.setOpaque(true);
        lblProceso.setBackground(getColorForProcess(ejec.getProceso().getId()));
        lblProceso.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(lblProceso, BorderLayout.CENTER);

        // Etiqueta de tiempo
        JLabel lblTiempo = new JLabel(
                ejec.getTiempoFin() != -1 ? ejec.getTiempoInicio() + "-" + ejec.getTiempoFin()
                        : ejec.getTiempoInicio() + "-?",
                SwingConstants.CENTER);
        lblTiempo.setFont(new Font("Arial", Font.PLAIN, 11));
        lblTiempo.setForeground(Color.DARK_GRAY);
        panel.add(lblTiempo, BorderLayout.SOUTH);

        return panel;
    }

    private void actualizarHistorialCola() {
        panelHistorialCola.removeAll();

        for (Proceso p : scheduler.getHistorialCPL()) {
            if (p == null)
                continue;

            JLabel lbl = crearEtiquetaProceso(p);
            panelHistorialCola.add(lbl);
        }

        panelHistorialCola.revalidate();
        panelHistorialCola.repaint();
    }

    private JLabel crearEtiquetaProceso(Proceso proceso) {
        JLabel lbl = new JLabel("P" + proceso.getId());
        lbl.setOpaque(true);

        // Color rojo si ha sido bloqueado alguna vez, verde si nunca fue bloqueado
        if (proceso.fueBloqueadoAlgunaVez() || proceso.getEstado() == EnumEstadoProceso.TERMINADO) {
            lbl.setBackground(new Color(255, 100, 100)); // Rojo
        } else {
            lbl.setBackground(new Color(200, 255, 200)); // Verde
        }

        lbl.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        lbl.setPreferredSize(new Dimension(50, 30));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    private void actualizarES() {
        panelES.removeAll();

        List<PlanificadorRoundRobin.EjecucionES> ejecucionesES = scheduler.getEjecucionesES();

        if (ejecucionesES.isEmpty()) {
            agregarMensajeVacioES();
        } else {
            agregarEjecucionesES(ejecucionesES);
        }

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
        for (PlanificadorRoundRobin.EjecucionES ejec : ejecuciones) {
            JPanel panelEjecES = crearPanelEjecucionES(ejec);
            panelES.add(panelEjecES);
        }
    }

    private JPanel crearPanelEjecucionES(PlanificadorRoundRobin.EjecucionES ejec) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(90, 60));

        // Configurar colores según estado
        boolean enES = ejec.estaEnES(scheduler.getTiempoActual());
        Color colorFondo;
        Color colorBorde;

        if (enES) {
            colorFondo = new Color(255, 220, 180); // Naranja mientras está en E/S
            colorBorde = new Color(255, 140, 0);
        } else {
            colorFondo = new Color(255, 100, 100); // Rojo cuando sale de E/S
            colorBorde = Color.RED;
        }

        panel.setBorder(BorderFactory.createLineBorder(colorBorde, 2));

        // Etiqueta del proceso
        JLabel lblProceso = new JLabel("P" + ejec.getProceso().getId(), SwingConstants.CENTER);
        lblProceso.setOpaque(true);
        lblProceso.setBackground(colorFondo);
        lblProceso.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(lblProceso, BorderLayout.CENTER);

        // Información de E/S
        JLabel lblInfo = new JLabel(
                String.format("%d→%d", ejec.getRafagaRestante(), ejec.getTiempoSalida()),
                SwingConstants.CENTER);
        lblInfo.setFont(new Font("Arial", Font.PLAIN, 10));
        lblInfo.setForeground(Color.DARK_GRAY);
        panel.add(lblInfo, BorderLayout.SOUTH);

        // Tooltip informativo
        configurarTooltipES(panel, ejec, enES);

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
    // MÉTODOS AUXILIARES
    // ======================

    private void actualizarVistaQuantum() {
        boolean esFijo = comboTipoRR.getSelectedIndex() == 0;

        lblQuantumFijo.setVisible(esFijo);
        txtQuantum.setVisible(esFijo);
        lblQuantumProceso.setVisible(!esFijo);
        txtQuantumProceso.setVisible(!esFijo);

        revalidate();
        repaint();
    }

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

    private void detenerTimer() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
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
        // Campos de texto
        txtLlegada.setText("");
        txtRafaga.setText("");
        txtIntervaloES.setText("");
        txtDuracionES.setText("");
        txtQuantum.setText("2"); // Quantum por defecto
        txtQuantumProceso.setText("");

        // Combo box
        comboTipoRR.setSelectedIndex(0);

        // Actualizar visibilidad de campos quantum
        actualizarVistaQuantum();

        // Enfocar primer campo
        txtLlegada.requestFocus();
    }

    private void setEstadoControles(boolean activado) {
    // Botones de gestión de procesos
    btnAgregar.setEnabled(activado);
    btnEditar.setEnabled(activado);
    btnEliminar.setEnabled(activado);
    
    // Configuración global
    comboTipoRR.setEnabled(activado);
    txtQuantum.setEnabled(activado);
    
    // Campos del formulario
    txtLlegada.setEnabled(activado);
    txtRafaga.setEnabled(activado);
    txtIntervaloES.setEnabled(activado);
    txtDuracionES.setEnabled(activado);
    txtQuantumProceso.setEnabled(activado);
}
private List<OperacionES> leerOperacionesES() {
    String textoMomentos = txtIntervaloES.getText().trim();
    String textoDuraciones = txtDuracionES.getText().trim();
    
    // 1. Verificar si no hay E/S (campos vacíos)
    if (textoMomentos.isEmpty() && textoDuraciones.isEmpty()) {
        return new ArrayList<>();
    }
    
    // 2. Separar los valores por el guion
    String[] momentos = textoMomentos.split("-");
    String[] duraciones = textoDuraciones.split("-");
    
    // 3. Validar que la cantidad de datos coincida
    if (momentos.length != duraciones.length) {
        throw new NumberFormatException("La cantidad de momentos y duraciones no coincide.");
    }
    
    List<OperacionES> listaES = new ArrayList<>();
    // Usamos un Set para detectar duplicados rápidamente
    java.util.Set<Integer> momentosUnicos = new java.util.HashSet<>();
    int ultimoMomento = -1;

    for (int i = 0; i < momentos.length; i++) {
        int momento = leerEnteroPositivo(momentos[i], "Momento E/S", true);
        int duracion = leerEnteroPositivo(duraciones[i], "Duración E/S", true);
        
        // 4. VALIDACIÓN DE DUPLICADOS
        if (!momentosUnicos.add(momento)) {
            throw new NumberFormatException("Error: El momento de E/S '" + momento + "' está duplicado.");
        }

        // 5. VALIDACIÓN DE ORDEN (Opcional pero recomendada)
        if (momento < ultimoMomento) {
            throw new NumberFormatException("Los momentos deben estar en orden ascendente (ej: 2-5-8).");
        }
        
        ultimoMomento = momento;
        listaES.add(new OperacionES(momento, duracion));
    }
    
    return listaES;
}
    
}