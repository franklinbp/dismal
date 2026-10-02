package com.dismal.distribuciones.modules.security.account.service;

import com.dismal.distribuciones.exception.BadRequestException;
import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.integrations.notifications.dto.NotificationSendResult;
import com.dismal.distribuciones.modules.integrations.notifications.service.EmailNotificationService;
import com.dismal.distribuciones.modules.security.account.domain.AccountActionToken;
import com.dismal.distribuciones.modules.security.account.domain.AccountTokenType;
import com.dismal.distribuciones.modules.security.account.dto.UpdateProfileRequest;
import com.dismal.distribuciones.modules.security.account.repository.AccountActionTokenRepository;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.customer.service.CustomerMarketService;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.security.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountIdentityService {

    private static final Duration VERIFICATION_TTL = Duration.ofHours(24);
    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(30);
    private static final String GENERIC_RESET_MESSAGE =
            "Si el correo está registrado, enviaremos instrucciones para recuperar el acceso.";

    private final UserRepository userRepository;
    private final AccountActionTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotificationService emailNotificationService;
    private final AuthenticationThrottleService throttleService;
    private final CustomerMarketService customerMarketService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.account.ec-storefront-url:https://dismal.net}")
    private String ecuadorStorefrontUrl;

    @Value("${app.account.pe-storefront-url:https://dismal.net.pe}")
    private String peruStorefrontUrl;

    @Transactional
    public void issueVerification(User user, String country) {
        if (user == null || user.isEmailVerified()) {
            return;
        }
        String rawToken = createToken(user, AccountTokenType.EMAIL_VERIFICATION, VERIFICATION_TTL);
        String link = storefrontUrl(country) + "/cuenta/verificar?token=" + rawToken;
        String name = displayName(user);
        NotificationSendResult result = emailNotificationService.sendAccountEmail(
                user.getEmail(),
                "Verifica tu cuenta Dismal",
                "Hola " + name + ",\n\nVerifica tu correo para proteger tus compras y licencias:\n" + link +
                        "\n\nEste enlace vence en 24 horas.",
                accountEmailHtml("Verifica tu cuenta", name,
                        "Confirma tu correo para proteger el historial de compras y las licencias digitales.",
                        "Verificar correo", link, "Este enlace vence en 24 horas.")
        );
        if (!result.sent()) {
            log.warn("Verification email was not sent for user {}: {}", user.getId(), result.message());
        }
    }

    @Transactional
    public String requestPasswordReset(String email, String country, String clientAddress) {
        String normalizedEmail = normalizeEmail(email);
        throttleService.consumeAccountAction("password-reset", normalizedEmail, clientAddress);
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null || !user.isEnabled()) {
            return GENERIC_RESET_MESSAGE;
        }
        String rawToken = createToken(user, AccountTokenType.PASSWORD_RESET, PASSWORD_RESET_TTL);
        String link = storefrontUrl(country) + "/cuenta/restablecer?token=" + rawToken;
        String name = displayName(user);
        NotificationSendResult result = emailNotificationService.sendAccountEmail(
                user.getEmail(),
                "Recupera tu acceso a Dismal",
                "Hola " + name + ",\n\nUsa este enlace para crear una nueva contraseña:\n" + link +
                        "\n\nEste enlace vence en 30 minutos. Si no lo solicitaste, ignora este correo.",
                accountEmailHtml("Recupera tu acceso", name,
                        "Recibimos una solicitud para cambiar la contraseña de tu cuenta.",
                        "Crear nueva contraseña", link,
                        "El enlace vence en 30 minutos y solo puede utilizarse una vez.")
        );
        if (!result.sent()) {
            log.warn("Password reset email was not sent for user {}: {}", user.getId(), result.message());
        }
        return GENERIC_RESET_MESSAGE;
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        AccountActionToken token = requireActiveToken(rawToken, AccountTokenType.EMAIL_VERIFICATION);
        User user = token.getUser();
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        consume(token);
        tokenRepository.consumeActiveTokens(user, AccountTokenType.EMAIL_VERIFICATION, LocalDateTime.now());
    }

    @Transactional
    public void resendVerification(String email, String country, String clientAddress) {
        User user = requireUser(email);
        if (user.isEmailVerified()) {
            return;
        }
        throttleService.consumeAccountAction("email-verification", user.getEmail(), clientAddress);
        issueVerification(user, country);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        AccountActionToken token = requireActiveToken(rawToken, AccountTokenType.PASSWORD_RESET);
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(user.getEmailVerifiedAt() != null ? user.getEmailVerifiedAt() : LocalDateTime.now());
        incrementSecurityVersion(user);
        userRepository.save(user);
        consume(token);
        tokenRepository.consumeActiveTokens(user, AccountTokenType.PASSWORD_RESET, LocalDateTime.now());
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = requireUser(email);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("La contraseña actual no es correcta.");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BadRequestException("La nueva contraseña debe ser diferente.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        incrementSecurityVersion(user);
        userRepository.save(user);
        tokenRepository.consumeActiveTokens(user, AccountTokenType.PASSWORD_RESET, LocalDateTime.now());
    }

    @Transactional
    public User updateProfile(String email, UpdateProfileRequest request) {
        User user = requireUser(email);
        user.setFirstname(clean(request.firstName()));
        user.setLastname(clean(request.lastName()));
        user.setPhone(PhoneNormalizer.normalize(request.phone()));
        User saved = userRepository.save(user);
        StorefrontCountry country = request.country() != null ? request.country() : StorefrontCountry.EC;
        customerMarketService.updateBillingProfile(saved, country, request.taxId(), request.billingEmail());
        return saved;
    }

    public User requireUser(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
    }

    private String createToken(User user, AccountTokenType type, Duration ttl) {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.consumeActiveTokens(user, type, now);
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokenRepository.save(AccountActionToken.builder()
                .user(user)
                .type(type)
                .tokenHash(hash(rawToken))
                .expiresAt(now.plus(ttl))
                .build());
        return rawToken;
    }

    private AccountActionToken requireActiveToken(String rawToken, AccountTokenType type) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("El enlace no es válido.");
        }
        AccountActionToken token = tokenRepository.findByTokenHashAndType(hash(rawToken.trim()), type)
                .orElseThrow(() -> new BadRequestException("El enlace no es válido o ya fue utilizado."));
        if (token.getConsumedAt() != null || !token.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("El enlace expiró o ya fue utilizado.");
        }
        return token;
    }

    private void consume(AccountActionToken token) {
        token.setConsumedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    private void incrementSecurityVersion(User user) {
        int current = user.getSecurityVersion() != null ? user.getSecurityVersion() : 0;
        user.setSecurityVersion(current + 1);
    }

    private String storefrontUrl(String country) {
        return "PE".equalsIgnoreCase(country) ? trimSlash(peruStorefrontUrl) : trimSlash(ecuadorStorefrontUrl);
    }

    private String trimSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email requerido.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String displayName(User user) {
        String name = ((user.getFirstname() != null ? user.getFirstname() : "") + " " +
                (user.getLastname() != null ? user.getLastname() : "")).trim();
        return name.isBlank() ? "cliente" : name;
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }

    private String accountEmailHtml(String title, String name, String description,
                                    String action, String link, String footer) {
        String safeTitle = escapeHtml(title);
        String safeName = escapeHtml(name);
        String safeDescription = escapeHtml(description);
        String safeAction = escapeHtml(action);
        String safeLink = escapeHtml(link);
        String safeFooter = escapeHtml(footer);
        return "<!doctype html><html><body style=\"margin:0;background:#f4f6f8;font-family:Arial,sans-serif;color:#172033\">" +
                "<div style=\"max-width:600px;margin:0 auto;padding:32px 20px\"><div style=\"background:#fff;border:1px solid #dce3e9;padding:32px\">" +
                "<div style=\"font-weight:800;color:#17243e;margin-bottom:28px\">Dismal | Software y Licencias Digitales</div>" +
                "<h1 style=\"font-size:26px;margin:0 0 16px\">" + safeTitle + "</h1><p>Hola " + safeName + ",</p>" +
                "<p style=\"line-height:1.6\">" + safeDescription + "</p>" +
                "<p style=\"margin:28px 0\"><a href=\"" + safeLink + "\" style=\"background:#087dbb;color:#fff;text-decoration:none;padding:13px 20px;font-weight:700\">" + safeAction + "</a></p>" +
                "<p style=\"font-size:13px;color:#627084\">" + safeFooter + "</p></div></div></body></html>";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
