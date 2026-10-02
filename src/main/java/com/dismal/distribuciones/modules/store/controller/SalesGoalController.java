package com.dismal.distribuciones.modules.store.controller;

import com.dismal.distribuciones.modules.store.domain.SalesGoal;
import com.dismal.distribuciones.modules.store.service.SalesGoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/store/goals")
@RequiredArgsConstructor
public class SalesGoalController {

    private final SalesGoalService salesGoalService;

    /**
     * DTO for sales goal creation requests.
     */
    public record SalesGoalRequest(
            UUID softwareId,
            Integer targetUnits,
            BigDecimal variableCost,
            BigDecimal fixedCosts,
            LocalDate deadline
    ) {}

    @PostMapping
    public ResponseEntity<SalesGoal> createGoal(@RequestBody SalesGoalRequest request) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        // Map DTO to entity
        SalesGoal newGoal = SalesGoal.builder()
                .targetUnits(request.targetUnits())
                .variableCost(request.variableCost())
                .fixedCosts(request.fixedCosts())
                .deadline(request.deadline())
                .build();

        SalesGoal createdGoal = salesGoalService.createGoal(request.softwareId(), newGoal);
        return new ResponseEntity<>(createdGoal, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SalesGoal>> getGoalsBySoftware(@RequestParam UUID softwareId) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isAdminOrManager(authentication)) {
            return ResponseEntity.status(403).build();
        }
        List<SalesGoal> goals = salesGoalService.getGoalsBySoftware(softwareId);
        return ResponseEntity.ok(goals);
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication;
    }

    private boolean isAdminOrManager(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ADMIN")
                        || authority.getAuthority().equals("MANAGER"));
    }
}
