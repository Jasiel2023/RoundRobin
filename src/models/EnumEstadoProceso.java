package models;

public enum EnumEstadoProceso {
    NUEVO,          // Recién creado
    LISTO,          // En cola ready
    EJECUTANDO,     // En CPU
    BLOQUEADO_ES,      // E/S
    TERMINADO       // Finalizado   
}