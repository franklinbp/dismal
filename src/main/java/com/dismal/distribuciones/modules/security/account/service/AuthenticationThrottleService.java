package com.dismal.distribuciones.modules.security.account.service;

import com.dismal.distribuciones.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthenticationThrottleService {

    private static final int LOGIN_ATTEMPTS = 5;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    private static final int ACCOUNT_ACTION_ATTEMPTS = 3;
    private static final Duration ACCOUNT_ACTION_WINDOW = Duration.ofHours(1);

    private final Map<String, ArrayDeque<Instant>> attempts = new ConcurrentHashMap<>();

    public void assertLoginAllowed(String email, String clientAddress) {
        assertAllowed(key("login", email, clientAddress), LOGIN_ATTEMPTS, LOGIN_WINDOW, false);
    }

    public void recordLoginFailure(String email, String clientAddress) {
        assertAllowed(key("login", email, clientAddress), LOGIN_ATTEMPTS, LOGIN_WINDOW, true);
    }

    public void clearLoginFailures(String email, String clientAddress) {
        attempts.remove(key("login", email, clientAddress));
    }

    public void consumeAccountAction(String action, String email, String clientAddress) {
        assertAllowed(key(action, email, clientAddress), ACCOUNT_ACTION_ATTEMPTS, ACCOUNT_ACTION_WINDOW, true);
    }

    private void assertAllowed(String key, int limit, Duration window, boolean record) {
        Instant now = Instant.now();
        ArrayDeque<Instant> bucket = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            Instant cutoff = now.minus(window);
            while (!bucket.isEmpty() && bucket.peekFirst().isBefore(cutoff)) {
                bucket.removeFirst();
            }
            if (bucket.size() >= limit) {
                throw new TooManyRequestsException("Demasiados intentos. Espera unos minutos antes de continuar.");
            }
            if (record) {
                bucket.addLast(now);
            }
        }
        if (attempts.size() > 10_000) {
            attempts.entrySet().removeIf(entry -> {
                ArrayDeque<Instant> value = entry.getValue();
                synchronized (value) {
                    return value.isEmpty() || value.peekLast().isBefore(now.minus(ACCOUNT_ACTION_WINDOW));
                }
            });
        }
    }

    private String key(String action, String email, String clientAddress) {
        String normalized = action + "|" + safe(email).toLowerCase() + "|" + safe(clientAddress);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }

    private String safe(String value) {
        return value == null ? "unknown" : value.trim();
    }
}
