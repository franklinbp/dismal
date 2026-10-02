package com.dismal.distribuciones.modules.marketing.intelligence.domain;

public enum MarketingState {
    RENTABLE,       // Verde: Ventas constantes, buena conversión
    BAJA_TRACCION,  // Amarillo: Ventas ocasionales
    ALERTA_COMERCIAL, // Rojo: Interés pero sin ventas
    PAUSADO,        // Negro: Sin interés ni ventas
    DESCONOCIDO     // Datos insuficientes
}
