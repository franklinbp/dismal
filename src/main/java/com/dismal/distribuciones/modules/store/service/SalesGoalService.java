package com.dismal.distribuciones.modules.store.service;

import com.dismal.distribuciones.exception.ResourceNotFoundException;
import com.dismal.distribuciones.modules.store.domain.SalesGoal;
import com.dismal.distribuciones.modules.store.domain.Software;
import com.dismal.distribuciones.modules.store.repository.SalesGoalRepository;
import com.dismal.distribuciones.modules.store.repository.SoftwareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalesGoalService {

    private final SalesGoalRepository salesGoalRepository;
    private final SoftwareRepository softwareRepository;

    @Transactional
    public SalesGoal createGoal(UUID softwareId, SalesGoal goal) {
        Software software = softwareRepository.findById(softwareId)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot create goal: Software not found with ID: " + softwareId));

        goal.setSoftware(software);
        return salesGoalRepository.save(goal);
    }

    @Transactional(readOnly = true)
    public List<SalesGoal> getGoalsBySoftware(UUID softwareId) {
        return salesGoalRepository.findBySoftwareId(softwareId);
    }
}
