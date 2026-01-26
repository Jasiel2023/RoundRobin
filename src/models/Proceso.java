 package models;

public class Proceso {

    
    private Integer id;
    private String nombre;
    private Integer quantum;
    private Float tiempoLlegada;
    private Float tiempoEjecucion;
    private Float operacionEntradaSalida;
    private Float duracionOperacion;

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return this.nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getQuantum() {
        return this.quantum;
    }

    public void setQuantum(Integer quantum) {
        this.quantum = quantum;
    }

    public Float getTiempoLlegada() {
        return this.tiempoLlegada;
    }

    public void setTiempoLlegada(Float tiempoLlegada) {
        this.tiempoLlegada = tiempoLlegada;
    }

    public Float getTiempoEjecucion() {
        return this.tiempoEjecucion;
    }

    public void setTiempoEjecucion(Float tiempoEjecucion) {
        this.tiempoEjecucion = tiempoEjecucion;
    }

    public Float getOperacionEntradaSalida() {
        return this.operacionEntradaSalida;
    }

    public void setOperacionEntradaSalida(Float operacionEntradaSalida) {
        this.operacionEntradaSalida = operacionEntradaSalida;
    }

    public Float getDuracionOperacion() {
        return this.duracionOperacion;
    }

    public void setDuracionOperacion(Float duracionOperacion) {
        this.duracionOperacion = duracionOperacion;
    }

}