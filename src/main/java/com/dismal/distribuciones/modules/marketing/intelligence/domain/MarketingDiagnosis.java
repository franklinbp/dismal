package com.dismal.distribuciones.modules.marketing.intelligence.domain;

public enum MarketingDiagnosis {
    PROBLEMA_CONFIANZA,   // Muchos clics/mensajes, pocas ventas
    PROBLEMA_VISIBILIDAD, // Pocos mensajes/clics
    PROBLEMA_PERCEPCION,  // Precio alto vs valor percibido
    PROBLEMA_COMPLEJIDAD, // Producto difícil de entender
    OPTIMO,               // Todo bien
    SIN_DATOS
}
