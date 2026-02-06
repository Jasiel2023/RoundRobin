package models;

public enum EnumEstadoProceso {
    NUEVO, // Recién creado
    LISTO, // En Cola de Procesos Listos
    EJECUTANDO, // En CPU
    BLOQUEADO_ES, // E/S
    TERMINADO // Finalizado
}