package com.dismal.distribuciones.modules.marketing.service;

import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.marketing.domain.StrategyAction;
import com.dismal.distribuciones.modules.marketing.domain.StrategyActionStatus;
import com.dismal.distribuciones.modules.marketing.dto.StrategyActionCreateRequest;
import com.dismal.distribuciones.modules.marketing.dto.StrategyActionResponse;
import com.dismal.distribuciones.modules.marketing.dto.StrategyActionStatusRequest;
import com.dismal.distribuciones.modules.marketing.dto.StrategyActionUpdateRequest;
import com.dismal.distribuciones.modules.marketing.repository.StrategyActionRepository;
import com.dismal.distribuciones.modules.planning.domain.SalesTarget;
import com.dismal.distribuciones.modules.planning.repository.SalesTargetRepository;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StrategyActionService {

    private final StrategyActionRepository strategyActionRepository;
    private final SoftwareRepository softwareRepository;
    private final SalesTargetRepository salesTargetRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<StrategyActionResponse> list(StrategyActionStatus status, UUID productId, int page, int size) {
        Specification<StrategyAction> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (productId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("product").get("id"), productId));
        }
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return strategyActionRepository.findAll(spec, pageRequest).map(this::toResponse);
    }

    @Transactional
    public StrategyActionResponse create(StrategyActionCreateRequest request) {
        StrategyAction action = StrategyAction.builder()
                .product(resolveProduct(request.productId()))
                .target(resolveTarget(request.targetId()))
                .source(request.source())
                .priority(request.priority())
                .status(StrategyActionStatus.PENDIENTE)
                .recommendedChannel(request.recommendedChannel())
                .title(request.title())
                .description(request.description())
                .assignedTo(resolveUser(request.assignedTo()))
                .dueDate(request.dueDate())
                .build();
        return toResponse(strategyActionRepository.save(action));
    }

    @Transactional
    public StrategyActionResponse update(UUID id, StrategyActionUpdateRequest request) {
        StrategyAction action = getAction(id);
        action.setProduct(resolveProduct(request.productId()));
        action.setTarget(resolveTarget(request.targetId()));
        action.setSource(request.source());
        action.setPriority(request.priority());
        action.setRecommendedChannel(request.recommendedChannel());
        action.setTitle(request.title());
        action.setDescription(request.description());
        action.setAssignedTo(resolveUser(request.assignedTo()));
        action.setDueDate(request.dueDate());
        action.setResultNotes(request.resultNotes());
        return toResponse(strategyActionRepository.save(action));
    }

    @Transactional
    public StrategyActionResponse updateStatus(UUID id, StrategyActionStatusRequest request) {
        StrategyAction action = getAction(id);
        action.setStatus(request.status());
        action.setResultNotes(request.resultNotes());
        if (request.status() == StrategyActionStatus.HECHA) {
            action.setCompletedAt(LocalDateTime.now());
        } else {
            action.setCompletedAt(null);
        }
        return toResponse(strategyActionRepository.save(action));
    }

    @Transactional
    public void discard(UUID id) {
        StrategyAction action = getAction(id);
        action.setStatus(StrategyActionStatus.DESCARTADA);
        strategyActionRepository.save(action);
    }

    @Transactional(readOnly = true)
    public long countOpenActions() {
        return strategyActionRepository.countByStatusIn(List.of(
                StrategyActionStatus.PENDIENTE,
                StrategyActionStatus.EN_PROGRESO
        ));
    }

    private StrategyAction getAction(UUID id) {
        return strategyActionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Strategy action not found with id: " + id));
    }

    private Software resolveProduct(UUID productId) {
        if (productId == null) {
            return null;
        }
        return softwareRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Software not found with id: " + productId));
    }

    private SalesTarget resolveTarget(UUID targetId) {
        if (targetId == null) {
            return null;
        }
        return salesTargetRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales target not found with id: " + targetId));
    }

    private User resolveUser(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private StrategyActionResponse toResponse(StrategyAction action) {
        Software product = action.getProduct();
        SalesTarget target = action.getTarget();
        User assignedTo = action.getAssignedTo();
        return new StrategyActionResponse(
                action.getId(),
                product != null ? product.getId() : null,
                product != null ? product.getName() : null,
                target != null ? target.getId() : null,
                action.getSource(),
                action.getPriority(),
                action.getStatus(),
                action.getRecommendedChannel(),
                action.getTitle(),
                action.getDescription(),
                assignedTo != null ? assignedTo.getId() : null,
                assignedTo != null ? formatName(assignedTo) : null,
                action.getDueDate(),
                action.getCompletedAt(),
                action.getResultNotes(),
                action.getCreatedAt(),
                action.getUpdatedAt()
        );
    }

    private String formatName(User user) {
        String first = user.getFirstname() != null ? user.getFirstname() : "";
        String last = user.getLastname() != null ? user.getLastname() : "";
        String name = (first + " " + last).trim();
        return !name.isBlank() ? name : user.getEmail();
    }
}
