package com.dismal.distribuciones.modules.store.domain;

public enum LicenseStatus {
    ACTIVE,     // The license is active and in use.
    INACTIVE,   // The license is not currently in use but can be activated.
    EXPIRED,    // The license has expired.
    DAMAGED     // The license is marked as damaged and cannot be used.
}
