 package models;

 import java.awt.Color;
 import java.util.Random;

public class Proceso {

    private String id;
    private String nombre;
    private Integer quantum;
    private Float tiempoLlegada;
    private Float tiempoEjecucion;
    private Float operacionES;
    private Float duracionES;
    private Color color;


    private Float tiempoRestante; // Cuánto le falta para terminar (inicia igual a rafagaTotal)
    private Float tiempoEnES; // Contador para cuando está bloqueado
    private Float tiempoEjecutado; // Cuánto tiempo ha usado la CPU hasta ahora
    private Float tiempoEspera;

    public Proceso (String id, String nombre, Integer quantum, Float tiempoLlegada, Float tiempoEjecucion, Float operacionES, Float duracionES, Color color) {
        this.id = id;
        this.nombre = nombre;
        this.quantum = quantum;
        this.tiempoLlegada = tiempoLlegada;
        this.tiempoEjecucion = tiempoEjecucion;
        this.operacionES = operacionES;
        this.duracionES = duracionES;
        this.color = color;

        this.tiempoRestante = tiempoEjecucion;
        this.tiempoEnES = 0f;
        this.tiempoEjecutado = 0f;
        this.tiempoEspera = 0f;

        this.color = generarColorAleatorio();
    }

    // Genera un color pastel aleatorio para que se vea bonito en la interfaz
    private Color generarColorAleatorio() {
        Random rand = new Random();
        float r = rand.nextFloat() / 2f + 0.5f;
        float g = rand.nextFloat() / 2f + 0.5f;
        float b = rand.nextFloat() / 2f + 0.5f;
        return new Color(r, g, b);
    }

    // Verifica si el proceso ya terminó
    public boolean esTerminado() {
        return tiempoRestante <= 0;
    }

    // Verifica si en este momento exacto el proceso debe irse a Bloqueado (E/S)
    // Se cumple si: no ha terminado E/S, tiene una E/S configurada, y llegó el momento justo
    public boolean debeHacerES() {
        // Ejemplo: Si operacionES es 3, y tiempoEjecutado es 3, toca ir a E/S
        return (duracionES > 0) && (tiempoEjecutado == operacionES);
    }

    public Float getTiempoRestante() {
        return this.tiempoRestante;
    }

    public void setTiempoRestante(Float tiempoRestante) {
        this.tiempoRestante = tiempoRestante;
    }

    public Float getTiempoEnES() {
        return this.tiempoEnES;
    }

    public void setTiempoEnES(Float tiempoEnES) {
        this.tiempoEnES = tiempoEnES;
    }

    public Float getTiempoEjecutado() {
        return this.tiempoEjecutado;
    }

    public void setTiempoEjecutado(Float tiempoEjecutado) {
        this.tiempoEjecutado = tiempoEjecutado;
    }

    public Float getTiempoEspera() {
        return this.tiempoEspera;
    }

    public void setTiempoEspera(Float tiempoEspera) {
        this.tiempoEspera = tiempoEspera;
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
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

    public Float getOperacionES() {
        return this.operacionES;
    }

    public void setOperacionES(Float operacionES) {
        this.operacionES = operacionES;
    }

    public Float getDuracionES() {
        return this.duracionES;
    }

    public void setDuracionES(Float duracionES) {
        this.duracionES = duracionES;
    }

}