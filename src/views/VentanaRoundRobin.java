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
    private JTextField txtIntervaloES, txtDuracionES;  // Campos para E/S
    private JComboBox<String> comboTipoRR;
    private JLabel lblQuantumFijo, lblQuantumProceso;
    
    // Paneles de visualización
    private JPanel panelHistorialCola, panelES, panelCPU;
    
    // Botones de control
    private JButton btnAgregar, btnIniciar, btnPausa, btnReiniciar;
    
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
        btnIniciar = new JButton("Iniciar RR");
        btnPausa = new JButton("Pausar");
        btnReiniciar = new JButton("Reiniciar");
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
        JTable tabla = new JTable(tableModel);
        tabla.setRowHeight(25);
        JScrollPane scroll = new JScrollPane(tabla);
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
        btnIniciar.addActionListener(e -> iniciarSimulacion());
        btnPausa.addActionListener(e -> togglePausa());
        btnReiniciar.addActionListener(e -> reiniciarTodo());
        comboTipoRR.addActionListener(e -> actualizarVistaQuantum());
    }
    
    // ======================
    // LÓGICA DE NEGOCIO
    // ======================
    
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
    
    private List<OperacionES> leerOperacionesES() {
        String textoMomentos = txtIntervaloES.getText().trim();
        String textoDuraciones = txtDuracionES.getText().trim();
        
        // Verificar si no hay E/S (campos vacíos o solo contienen 0)
        if (textoMomentos.isEmpty() && textoDuraciones.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Validar formato
        String[] momentos = textoMomentos.split("-");
        String[] duraciones = textoDuraciones.split("-");
        
        if (momentos.length != duraciones.length) {
            throw new NumberFormatException("Cantidad de momentos y duraciones no coincide");
        }
        
        // Crear lista de operaciones
        List<OperacionES> listaES = new ArrayList<>();
        for (int i = 0; i < momentos.length; i++) {
            int momento = leerEnteroPositivo(momentos[i], "Momento E/S", true);
            int duracion = leerEnteroPositivo(duraciones[i], "Duración E/S", true);
            listaES.add(new OperacionES(momento, duracion));
        }
        
        return listaES;
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
            PoliticaQuantum politica = crearPoliticaQuantum();
            
            // Usar procesos originales de la tabla
            scheduler = new PlanificadorRoundRobin(tableModel.getProcesos(), politica);
            
            iniciarTimerSimulacion();
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error en configuración: " + ex.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
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
        if (timer == null) return;
        
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
            ejec.getTiempoFin() != -1 ? 
            ejec.getTiempoInicio() + "-" + ejec.getTiempoFin() : 
            ejec.getTiempoInicio() + "-?", 
            SwingConstants.CENTER
        );
        lblTiempo.setFont(new Font("Arial", Font.PLAIN, 11));
        lblTiempo.setForeground(Color.DARK_GRAY);
        panel.add(lblTiempo, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void actualizarHistorialCola() {
        panelHistorialCola.removeAll();
        
        for (Proceso p : scheduler.getHistorialCPL()) {
            if (p == null) continue;
            
            JLabel lbl = crearEtiquetaProceso(p.getId(), p.getEstado());
            panelHistorialCola.add(lbl);
        }
        
        panelHistorialCola.revalidate();
        panelHistorialCola.repaint();
    }
    
    private JLabel crearEtiquetaProceso(int id, EnumEstadoProceso estado) {
        JLabel lbl = new JLabel("P" + id);
        lbl.setOpaque(true);
        
        // Color rojo si está bloqueado o terminado, verde si está en lista
        if (estado == EnumEstadoProceso.BLOQUEADO_ES || estado == EnumEstadoProceso.TERMINADO) {
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
            SwingConstants.CENTER
        );
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
            ejec.getTiempoSalida() - ejec.getTiempoEntrada()
        );
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
            new Color(220, 220, 220)  // Gris claro - P8
        };
        
        return colors[(id - 1) % colors.length];
    }
    
    private void reiniciarTodo() {
        detenerTimer();
        limpiarScheduler();
        limpiarTabla();
        limpiarPaneles();
        mostrarMensajeReinicio();
    }
    
    private void detenerTimer() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
    }
    
    private void limpiarScheduler() {
        scheduler = null;
    }
    
    private void limpiarTabla() {
        tableModel.limpiar();
    }
    
    private void limpiarPaneles() {
        panelCPU.removeAll();
        panelES.removeAll();
        panelHistorialCola.removeAll();
        
        panelCPU.revalidate();
        panelCPU.repaint();
        panelES.revalidate();
        panelES.repaint();
        panelHistorialCola.revalidate();
        panelHistorialCola.repaint();
    }
    
    private void mostrarMensajeReinicio() {
        JOptionPane.showMessageDialog(this, 
            "Simulación reiniciada. Puedes agregar nuevos procesos.",
            "Reinicio Completado",
            JOptionPane.INFORMATION_MESSAGE);
    }
}