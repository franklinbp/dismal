package com.dismal.distribuciones.modules.admin.controller;

import com.dismal.distribuciones.modules.admin.dto.AdminImportResult;
import com.dismal.distribuciones.modules.admin.dto.AdminResetPasswordResponse;
import com.dismal.distribuciones.modules.admin.dto.AdminUserRequest;
import com.dismal.distribuciones.modules.admin.dto.AdminUserResponse;
import com.dismal.distribuciones.modules.admin.dto.AdminUserType;
import com.dismal.distribuciones.modules.admin.service.AdminUserService;
import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private static final Set<Role> CLIENT_ROLES = Set.of(Role.USER, Role.CUSTOMER);
    private static final Set<Role> INTERNAL_ROLES = Set.of(Role.ADMIN, Role.MANAGER);

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> listUsers(
            @RequestParam(required = false) AdminUserType type,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) StorefrontCountry country,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        if (type == AdminUserType.INTERNAL && !isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(adminUserService.listUsers(type, q, country, page, size));
    }

    @PostMapping
    public ResponseEntity<AdminUserResponse> createUser(@RequestBody AdminUserRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        if (isManager(authentication)) {
            if (request.role() != null && !CLIENT_ROLES.contains(request.role())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(adminUserService.createUser(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminUserResponse> updateUser(@PathVariable UUID id, @RequestBody AdminUserRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        if (isManager(authentication)) {
            if (request.role() != null) {
                return ResponseEntity.status(403).build();
            }
            Role targetRole = adminUserService.getUserRole(id);
            if (!CLIENT_ROLES.contains(targetRole)) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(adminUserService.updateUser(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam(required = false) StorefrontCountry country,
            @RequestBody StatusRequest request
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        if (request == null || request.enabled() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (isManager(authentication)) {
            Role targetRole = adminUserService.getUserRole(id);
            if (!CLIENT_ROLES.contains(targetRole)) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(adminUserService.updateStatus(id, country, request.enabled()));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<AdminResetPasswordResponse> resetPassword(@PathVariable UUID id) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }
        String tempPassword = adminUserService.resetPassword(id);
        return ResponseEntity.ok(new AdminResetPasswordResponse(tempPassword));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminImportResult> importClients(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(defaultValue = "CLIENT") AdminUserType type,
            @RequestParam(defaultValue = "EC") StorefrontCountry country
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        if (type != AdminUserType.CLIENT) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(adminUserService.importClients(file, format, country));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportClients(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(defaultValue = "CLIENT") AdminUserType type,
            @RequestParam(defaultValue = "EC") StorefrontCountry country
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        if (type != AdminUserType.CLIENT) {
            return ResponseEntity.badRequest().build();
        }
        byte[] data = adminUserService.exportClients(format, country);
        String ext = "xlsx".equalsIgnoreCase(format) ? "xlsx" : "csv";
        String filename = "clientes-" + LocalDate.now() + "." + ext;
        MediaType mediaType = "xlsx".equalsIgnoreCase(format)
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.parseMediaType("text/csv");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(data);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> exportClientTemplate(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(defaultValue = "CLIENT") AdminUserType type,
            @RequestParam(defaultValue = "EC") StorefrontCountry country
    ) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        if (type != AdminUserType.CLIENT) {
            return ResponseEntity.badRequest().build();
        }
        byte[] data = adminUserService.exportClientTemplate(format, country);
        String ext = "xlsx".equalsIgnoreCase(format) ? "xlsx" : "csv";
        String filename = "plantilla-clientes." + ext;
        MediaType mediaType = "xlsx".equalsIgnoreCase(format)
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.parseMediaType("text/csv");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(data);
    }

    private boolean isAdminOrManager(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ADMIN")
                        || authority.getAuthority().equals("MANAGER"));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ADMIN"));
    }

    private boolean isManager(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("MANAGER"));
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication;
    }

    public record StatusRequest(Boolean enabled) {}
}
