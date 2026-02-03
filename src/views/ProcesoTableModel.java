package views;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import models.Proceso;

public class ProcesoTableModel extends AbstractTableModel {

    private final List<Proceso> procesos;
    
    // ======================
    // CAMBIAR NOMBRES DE COLUMNAS
    // ======================
    private final String[] columnas = {
        "ID", "Llegada", "Ráfaga", "Quantum", 
        "O E/S", "Dur. O E/S",  // ← CAMBIADO: "E/S cada" → "O E/S"
        "Estado", "Restante"
    };

    public ProcesoTableModel() {
        procesos = new ArrayList<>();
    }

    public void agregarProceso(Proceso p) {
        procesos.add(p);
        fireTableRowsInserted(procesos.size() - 1, procesos.size() - 1);
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
                // Quantum: personal o global
                if (p.getQuantumPersonal() == -1) {
                    return "Global";
                } else {
                    return p.getQuantumPersonal();
                }
            }
            case 4 -> {
                // ======================
                // CAMBIADO: usar getMomentoES() en lugar de getIntervaloES()
                // ======================
                int momentoES = p.getMomentoES();
                return momentoES > 0 ? momentoES : "No";
            }
            case 5 -> {
                int duracionES = p.getDuracionES();
                return duracionES > 0 ? duracionES : "-";
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
}