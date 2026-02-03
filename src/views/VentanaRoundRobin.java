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

    private JTextField txtLlegada;
    private JTextField txtRafaga;
    private JTextField txtQuantum;
    private JTextField txtQuantumProceso;
    private JComboBox<String> comboTipoRR;
    private JLabel lblQuantumFijo;
    private JLabel lblQuantumProceso;
    private JPanel panelHistorialCola;
    private JPanel panelES;
    private JLabel lblTiempoActual;

    
    // ======================
    // NUEVO: CAMPOS PARA CONFIGURAR E/S
    // ======================
    private JTextField txtIntervaloES;
    private JTextField txtDuracionES;

    // Mantiene el orden de llegada a la cola
    private java.util.List<Proceso> historialCola = new ArrayList<>();

    // Para acceder rápido al label de cada proceso
    private Map<Integer, JLabel> historialColaLabels = new LinkedHashMap<>();
    private ProcesoTableModel tableModel;
    private PlanificadorRoundRobin scheduler;
    private Timer timer;
    private boolean enPausa = false;


    private JPanel panelCPU;

    public VentanaRoundRobin() {
        initUI();
    }
     
    private void initUI() {
        setTitle("Simulador Round Robin con Operaciones E/S");
        setSize(750, 550);  // Un poco más grande para el nuevo panel
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // ===== Panel Form =====
        JPanel panelForm = new JPanel(new FlowLayout());

        txtLlegada = new JTextField(5);
        txtRafaga = new JTextField(5);
        txtQuantum = new JTextField(5);
        txtQuantumProceso = new JTextField(5);
        
        // ======================
        // NUEVO: CAMPOS PARA E/S
        // ======================
        txtIntervaloES = new JTextField(5);
        txtDuracionES = new JTextField(5);
        txtIntervaloES.setText("");  // Valor por defecto (sin E/S)
        txtDuracionES.setText("");

        comboTipoRR = new JComboBox<>(new String[] {
            "Quantum fijo",
            "Quantum por proceso"
        });

        panelForm.add(new JLabel("Tipo RR:"));
        panelForm.add(comboTipoRR);

        JButton btnAgregar = new JButton("Agregar proceso");
        JButton btnIniciar = new JButton("Iniciar RR");
        JButton btnPausa = new JButton("Pausar");
        JButton btnReiniciar = new JButton("Reiniciar");


        panelForm.add(new JLabel("Llegada:"));
        panelForm.add(txtLlegada);
        panelForm.add(new JLabel("Ráfaga:"));
        panelForm.add(txtRafaga);
        
        // ======================
        // NUEVO: AGREGAR CAMPOS E/S AL FORMULARIO
        // ======================
        panelForm.add(new JLabel("Momentos E/S (ej: 1-3-5):"));
        panelForm.add(txtIntervaloES);
        panelForm.add(new JLabel("Duraciones E/S (ej: 2-3-4):"));
        panelForm.add(txtDuracionES);


        // Labels de quantum
        lblQuantumFijo = new JLabel("Quantum fijo:");
        lblQuantumProceso = new JLabel("Quantum proceso:");

        panelForm.add(lblQuantumFijo);
        panelForm.add(txtQuantum);

        panelForm.add(lblQuantumProceso);
        panelForm.add(txtQuantumProceso);

        panelForm.add(btnAgregar);
        panelForm.add(btnIniciar);
        panelForm.add(btnPausa);
        panelForm.add(btnReiniciar);




        // ===== Tabla =====
        tableModel = new ProcesoTableModel();
        JTable tabla = new JTable(tableModel);
        JScrollPane scroll = new JScrollPane(tabla);
        

        // ===== Panel Historial Cola =====
        panelHistorialCola = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelHistorialCola.setBorder(
            BorderFactory.createTitledBorder("Historial de Cola de Listos")
        );
        panelHistorialCola.setPreferredSize(new Dimension(730, 90));

        // NUEVO: PANEL OPERACIONES E/S (SEGUNDO)
        // ======================
        panelES = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelES.setBorder(
            BorderFactory.createTitledBorder("Operaciones de Entrada/Salida (Historial)")
        );
        panelES.setPreferredSize(new Dimension(730, 80));

        // ===== Panel CPU =====
        panelCPU = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelCPU.setBorder(
            BorderFactory.createTitledBorder("CPU - Historial de ejecución")
        );
        panelCPU.setPreferredSize(new Dimension(730, 90));
        

        // ===== Panel Centro =====
        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.add(scroll, BorderLayout.NORTH);
        
        // Panel para los tres estados
        JPanel panelEstados = new JPanel(new GridLayout(3, 1, 5, 5));
        panelEstados.add(panelHistorialCola);
        panelEstados.add(panelES);  // Agregar nuevo panel
        panelEstados.add(panelCPU);
        
        panelCentro.add(panelEstados, BorderLayout.CENTER);

        // ===== Acciones =====
        btnAgregar.addActionListener(e -> agregarProceso());
        btnIniciar.addActionListener(e -> iniciarSimulacion());
        btnPausa.addActionListener(e -> {
            if (timer == null) return; // Aún no inicia nada

            if (enPausa) {
                timer.start();
                btnPausa.setText("Pausar");
            } else {
                timer.stop();
                btnPausa.setText("Reanudar");
            }
            enPausa = !enPausa;
        });
        btnReiniciar.addActionListener(e -> reiniciarTodo());



        comboTipoRR.addActionListener(e -> actualizarVistaQuantum());

        // Estado inicial según combo
        actualizarVistaQuantum();

        // ===== Layout principal =====
        setLayout(new BorderLayout());
        add(panelForm, BorderLayout.NORTH);
        add(panelCentro, BorderLayout.CENTER);

        setVisible(true);
    }
    
    private void agregarProceso() {
        try {
            int llegada = Integer.parseInt(txtLlegada.getText());
            int rafaga = Integer.parseInt(txtRafaga.getText());

            // Validaciones básicas de llegada y ráfaga
            if (llegada < 0 || rafaga <= 0) {
                throw new NumberFormatException();
            }

            // ======================
            // NUEVO: LEER LISTAS DE E/S
            // ======================
            String textoMomentos = txtIntervaloES.getText().trim();   // ej: "1-3-5"
            String textoDuraciones = txtDuracionES.getText().trim();  // ej: "2-3-4"

            List<OperacionES> listaES = new ArrayList<>();

            // Si ambos están en "0" o vacíos => sin E/S
            boolean sinES = (textoMomentos.equals("0") || textoMomentos.isEmpty())
                        && (textoDuraciones.equals("0") || textoDuraciones.isEmpty());

            if (!sinES) {
                String[] momentos = textoMomentos.split("-");
                String[] duraciones = textoDuraciones.split("-");

                // Deben tener la misma cantidad
                if (momentos.length != duraciones.length) {
                    throw new NumberFormatException("Cantidad de momentos y duraciones no coincide");
                }

                for (int i = 0; i < momentos.length; i++) {
                    int momento = Integer.parseInt(momentos[i].trim());
                    int duracion = Integer.parseInt(duraciones[i].trim());

                    // Validaciones por cada E/S
                    if (momento <= 0 || duracion <= 0) {
                        throw new NumberFormatException("Valores de E/S inválidos");
                    }

                    listaES.add(new OperacionES(momento, duracion));
                }
            }

            // ======================
            // CREAR PROCESO
            // ======================
            Proceso p = new Proceso(llegada, rafaga, listaES);
            p.setEstado(EnumEstadoProceso.NUEVO);

            boolean esFijo = comboTipoRR.getSelectedIndex() == 0;

            if (!esFijo) {
                int qProc = Integer.parseInt(txtQuantumProceso.getText());
                if (qProc <= 0) throw new NumberFormatException();
                p.setQuantumPersonal(qProc);
            }

            tableModel.agregarProceso(p);

            // Limpiar campos
            txtLlegada.setText("");
            txtRafaga.setText("");
            txtIntervaloES.setText("");
            txtDuracionES.setText("");
            txtQuantumProceso.setText("");

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Ingrese valores numéricos válidos\n" +
                    "• Llegada ≥ 0\n" +
                    "• Ráfaga > 0\n" +
                    "• E/S en formato: 1-3-5 y 2-3-4\n" +
                    "• Cantidades deben coincidir y ser > 0",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
private void iniciarSimulacion() {
    PoliticaQuantum politica;

    boolean esFijo = comboTipoRR.getSelectedIndex() == 0;

    if (esFijo) {
        int quantum;
        try {
            quantum = Integer.parseInt(txtQuantum.getText());
            if (quantum <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Ingrese un quantum válido (> 0)",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        tableModel.setQuantumGlobal(quantum);
        politica = new QuantumFijo(quantum);
    } else {
        politica = new logic.QuantumPorProceso();
    }

    // ¡CORRECCIÓN! Usar los procesos directamente de la tabla
    // NO crear nuevas instancias
    scheduler = new PlanificadorRoundRobin(
        tableModel.getProcesos(),  // ← USAR PROCESOS ORIGINALES
        politica
    );

    timer = new Timer(1000, e -> {
        scheduler.tick();
        tableModel.actualizarTabla();
        actualizarCPU();
        actualizarHistorialCola();
        actualizarES();

        if (scheduler.haTerminado()) {
            timer.stop();
            JOptionPane.showMessageDialog(this,
                    "Simulación Round Robin terminada");
        }
    });
    enPausa = false;
    timer.start();
}    
    private void actualizarCPU() {
        panelCPU.removeAll();
        
        // Obtener ejecuciones con tiempos
        List<PlanificadorRoundRobin.EjecucionCPU> ejecuciones = 
            scheduler.getEjecucionesCPU();
        
        if (ejecuciones.isEmpty()) {
            panelCPU.revalidate();
            panelCPU.repaint();
            return;
        }
        
        // Crear panel para alinear
        JPanel panelContenedor = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        
        for (PlanificadorRoundRobin.EjecucionCPU ejec : ejecuciones) {
            // Panel para cada proceso
            JPanel panelProceso = new JPanel(new BorderLayout());
            panelProceso.setPreferredSize(new Dimension(70, 50));
            panelProceso.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            
            // Proceso (arriba)
            JLabel lblProceso = new JLabel("P" + ejec.getProceso().getId(), SwingConstants.CENTER);
            lblProceso.setOpaque(true);
            lblProceso.setBackground(getColorForProcess(ejec.getProceso().getId()));
            lblProceso.setFont(new Font("Arial", Font.BOLD, 14));
            panelProceso.add(lblProceso, BorderLayout.CENTER);
            
            // Tiempos (abajo)
            String tiempoTexto;
            if (ejec.getTiempoFin() != -1) {
                tiempoTexto = ejec.getTiempoInicio() + "-" + ejec.getTiempoFin();
            } else {
                tiempoTexto = ejec.getTiempoInicio() + "-?";
            }
            
            JLabel lblTiempo = new JLabel(tiempoTexto, SwingConstants.CENTER);
            lblTiempo.setFont(new Font("Arial", Font.PLAIN, 11));
            lblTiempo.setForeground(Color.DARK_GRAY);
            panelProceso.add(lblTiempo, BorderLayout.SOUTH);
            
            panelContenedor.add(panelProceso);
        }
        
        panelCPU.add(panelContenedor);
        panelCPU.revalidate();
        panelCPU.repaint();
    }
    
    // ======================
    // NUEVO MÉTODO: ACTUALIZAR PANEL DE OPERACIONES E/S
    // ======================
    private void actualizarES() {
        panelES.removeAll();
        
        // Obtener ejecuciones E/S (snapshots congelados)
        List<PlanificadorRoundRobin.EjecucionES> ejecucionesES = 
            scheduler.getEjecucionesES();
        
        if (ejecucionesES.isEmpty()) {
            JLabel lblVacio = new JLabel("No hay procesos en operaciones E/S", 
                SwingConstants.CENTER);
            lblVacio.setForeground(Color.GRAY);
            lblVacio.setFont(new Font("Arial", Font.ITALIC, 12));
            panelES.add(lblVacio);
        } else {
            // Mostrar cada ejecución E/S en orden
            for (PlanificadorRoundRobin.EjecucionES ejec : ejecucionesES) {
                JPanel panelEjecES = crearPanelEjecucionES(ejec);
                panelES.add(panelEjecES);
            }
        }
        
        panelES.revalidate();
        panelES.repaint();
    }
    
    // ======================
    // NUEVO MÉTODO AUXILIAR: CREAR PANEL PARA PROCESO EN E/S
    // ======================
    private JPanel crearPanelProcesoES(Proceso p) {
        JPanel panelProcesoES = new JPanel(new BorderLayout());
        panelProcesoES.setPreferredSize(new Dimension(80, 60));
        
        // Color según si está ACTUALMENTE en E/S o ya terminó
        Color colorBorde;
        Color colorFondo;
        
        if (p.isEnES()) {
            // Está ACTUALMENTE en E/S
            colorBorde = new Color(255, 140, 0); // Naranja
            colorFondo = new Color(255, 200, 150);
        } else {
            // Ya terminó E/S (historial)
            colorBorde = new Color(180, 180, 180); // Gris
            colorFondo = new Color(230, 230, 230);
        }
        
        panelProcesoES.setBorder(BorderFactory.createLineBorder(colorBorde, 2));
        
        // Parte superior: ID del proceso
        JLabel lblProceso = new JLabel("P" + p.getId(), SwingConstants.CENTER);
        lblProceso.setOpaque(true);
        lblProceso.setBackground(colorFondo);
        lblProceso.setFont(new Font("Arial", Font.BOLD, 12));
        panelProcesoES.add(lblProceso, BorderLayout.CENTER);
        
        // Parte inferior: Información E/S
        String infoES;
        if (p.isEnES()) {
            // En E/S actualmente: ráfaga-tiempoES
            infoES = p.getTiempoRestante() + "-" + p.getTiempoESRestante();
        } else {
            // Ya terminó E/S: mostrar que terminó
            infoES = "✓"; // o p.getTiempoRestante() + "-0"
        }
        
        JLabel lblInfo = new JLabel(infoES, SwingConstants.CENTER);
        lblInfo.setFont(new Font("Arial", Font.PLAIN, 10));
        lblInfo.setForeground(Color.DARK_GRAY);
        panelProcesoES.add(lblInfo, BorderLayout.SOUTH);
        
        // Tooltip
        String estado = p.isEnES() ? "ACTUALMENTE en E/S" : "Terminó E/S";
        String tooltip = String.format(
            "<html><b>Proceso P%d</b><br>" +
            "• Estado: %s<br>" +
            "• E/S después de: %d ms ejecutados<br>" +
            "• Duración E/S: %d ms</html>",
            p.getId(), estado, p.getMomentoES(), p.getDuracionES()
        );
        panelProcesoES.setToolTipText(tooltip);
        
        return panelProcesoES;
    }

    private void actualizarVistaQuantum() {
        boolean esFijo = comboTipoRR.getSelectedIndex() == 0;

        lblQuantumFijo.setVisible(esFijo);
        txtQuantum.setVisible(esFijo);

        lblQuantumProceso.setVisible(!esFijo);
        txtQuantumProceso.setVisible(!esFijo);

        revalidate();
        repaint();
    }

    private void actualizarHistorialCola() {
        panelHistorialCola.removeAll();

        for (Proceso p : scheduler.getHistorialCPL()) {
            if (p == null) continue;

            JLabel lbl = new JLabel("P" + p.getId());
            lbl.setOpaque(true);
            lbl.setBackground(new Color(200, 255, 200));
            lbl.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            lbl.setPreferredSize(new Dimension(50, 30));
            lbl.setHorizontalAlignment(SwingConstants.CENTER);

            panelHistorialCola.add(lbl);
        }

        panelHistorialCola.revalidate();
        panelHistorialCola.repaint();
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

    private JPanel crearPanelEjecucionES(PlanificadorRoundRobin.EjecucionES ejec) {
        JPanel panelEjec = new JPanel(new BorderLayout());
        panelEjec.setPreferredSize(new Dimension(90, 60));
        
        // Verificar si todavía está en E/S
        int tiempoActual = scheduler.getTiempoActual();
        boolean enES = ejec.estaEnES(tiempoActual);
        
        // Colores según estado
        Color colorFondo, colorBorde;
        if (enES) {
            colorFondo = new Color(255, 220, 180); // Naranja: en E/S
            colorBorde = new Color(255, 140, 0);
        } else {
            colorFondo = new Color(200, 230, 255); // Azul: ya salió
            colorBorde = Color.BLUE;
        }
        
        panelEjec.setBorder(BorderFactory.createLineBorder(colorBorde, 2));
        
        // Parte superior: ID del proceso
        JLabel lblProceso = new JLabel("P" + ejec.getProceso().getId(), SwingConstants.CENTER);
        lblProceso.setOpaque(true);
        lblProceso.setBackground(colorFondo);
        lblProceso.setFont(new Font("Arial", Font.BOLD, 12));
        panelEjec.add(lblProceso, BorderLayout.CENTER);
        
        // ============================================
        // PARTE INFERIOR: FORMATO "ráfaga entrada→salida"
        // ============================================
        String infoES = String.format("%d→%d", 
            ejec.getRafagaRestante(),  // Ráfaga AL ENTRAR (congelada)
            ejec.getTiempoSalida()     // Tiempo de salida
        );
        
        JLabel lblInfo = new JLabel(infoES, SwingConstants.CENTER);
        lblInfo.setFont(new Font("Arial", Font.PLAIN, 10));
        lblInfo.setForeground(Color.DARK_GRAY);
        panelEjec.add(lblInfo, BorderLayout.SOUTH);
        
        // Tooltip con información detallada
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
        panelEjec.setToolTipText(tooltip);
        
        return panelEjec;
    }

    private void reiniciarTodo() {
    // 1. Detener timer si está corriendo
    if (timer != null && timer.isRunning()) {
        timer.stop();
    }

    // 2. Resetear scheduler
    scheduler = null;

    // 3. Limpiar tabla de procesos
    tableModel.limpiar();

    // 4. Limpiar paneles visuales
    panelCPU.removeAll();
    panelES.removeAll();
    panelHistorialCola.removeAll();

    panelCPU.revalidate();
    panelCPU.repaint();

    panelES.revalidate();
    panelES.repaint();

    panelHistorialCola.revalidate();
    panelHistorialCola.repaint();

    // 5. Resetear contador de tiempo

    // 6. Mensajito opcional
    JOptionPane.showMessageDialog(this, "Simulación reiniciada. Puedes agregar nuevos procesos.");
}

}