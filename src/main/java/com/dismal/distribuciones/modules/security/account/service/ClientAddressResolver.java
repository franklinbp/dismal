package com.dismal.distribuciones.modules.security.account.service;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientAddressResolver {

    private ClientAddressResolver() {}

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",", 2)[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null && !realIp.isBlank() ? realIp.trim() : request.getRemoteAddr();
    }
}
