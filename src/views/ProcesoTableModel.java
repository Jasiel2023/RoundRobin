package views;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import models.Proceso;

public class ProcesoTableModel extends AbstractTableModel {

    private final List<Proceso> procesos;

    // Quantum global (fijo)
    private int quantumGlobal = 1; // valor por defecto, cámbialo desde la vista

    private final String[] columnas = {
        "ID", "Llegada", "Ráfaga", "Quantum",
        "O E/S", "Dur. O E/S",
        "Estado", "Restante"
    };

    public ProcesoTableModel() {
        procesos = new ArrayList<>();
    }

    // ======================
    // NUEVO: setter para quantum global
    // ======================
    public void setQuantumGlobal(int quantumGlobal) {
        this.quantumGlobal = quantumGlobal;
        fireTableDataChanged();
    }

    public int getQuantumGlobal() {
        return quantumGlobal;
    }

    public void agregarProceso(Proceso p) {
        procesos.add(p);

        // ======================
        // ORDENAR POR PRIORIDAD:
        // 1) Llegada
        // 2) Quantum (personal o global)
        // 3) Si tiene E/S (los que tienen E/S después)
        // ======================
        procesos.sort(Comparator
            .comparingInt(Proceso::getTiempoLlegada)
            .thenComparingInt(pr -> {
                int q = pr.getQuantumPersonal();
                return (q == -1) ? quantumGlobal : q;
            })
            .thenComparingInt(pr -> pr.getMomentoES() > 0 ? 1 : 0)
        );

        fireTableDataChanged();
    }

    public Proceso getProceso(int fila) {
        return procesos.get(fila);
    }

    public List<Proceso> getProcesos() {
        return procesos;
    }

    @Override
    public int getRowCount() {
        return procesos.size();
    }

    @Override
    public int getColumnCount() {
        return columnas.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnas[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Proceso p = procesos.get(rowIndex);

        switch (columnIndex) {
            case 0 -> {
                return "P" + p.getId();
            }
            case 1 -> {
                return p.getTiempoLlegada();
            }
            case 2 -> {
                return p.getRafagaCPU();
            }
            case 3 -> {
                // ======================
                // MOSTRAR SIEMPRE NÚMERO:
                // si es personal -> ese número
                // si es global -> mostrar quantumGlobal
                // ======================
                int q = p.getQuantumPersonal();
                return (q == -1) ? quantumGlobal : q;
            }
            case 4 -> { // O E/S
                String s = p.getMomentosESOriginal();
                return (s == null || s.isEmpty()) ? "No" : s;
            }
            case 5 -> { // Duración O E/S
                String s = p.getDuracionesESOriginal();
                return (s == null || s.isEmpty()) ? "-" : s;
            }

            case 6 -> {
                return p.getEstado();
            }
            case 7 -> {
                return p.getTiempoRestante();
            }
            default -> {
                return "";
            }
        }
    }

    public void actualizarTabla() {
        fireTableDataChanged();
    }

    public void limpiar() {
    procesos.clear();
    fireTableDataChanged();
}

}
