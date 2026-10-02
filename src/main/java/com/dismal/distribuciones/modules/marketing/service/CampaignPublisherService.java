package com.dismal.distribuciones.modules.marketing.service;

import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignPublisherService {

    private final UserRepository userRepository;
    /**
     * Fetches all users of a specific role who have a non-null phone and email.
     * @param targetRole The role to target (e.g., CUSTOMER).
     * @return A list of eligible users.
     */
    @Transactional(readOnly = true)
    public List<User> getEligibleCustomers(Role targetRole) {
        return userRepository.findByRoleAndEnabledTrueAndPhoneIsNotNullAndEmailIsNotNull(targetRole);
    }
}
