package com.dismal.distribuciones.modules.security.repository;

import com.dismal.distribuciones.modules.security.domain.Role;
import com.dismal.distribuciones.modules.security.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);

    List<User> findByRole(Role role);

    List<User> findByRoleIn(List<Role> roles);

    List<User> findByRoleInAndEnabledTrue(List<Role> roles);

    List<User> findByRoleAndEnabledTrue(Role role);

    List<User> findByRoleAndEnabledTrueAndPhoneIsNotNullAndEmailIsNotNull(Role role);

    boolean existsByEmail(String email);

    long countByRoleAndEnabledTrue(Role role);

    @Query("SELECT u FROM User u WHERE u.role IN ('CUSTOMER','USER') AND u.enabled = true " +
            "AND u.id NOT IN (SELECT o.customer.id FROM Order o)")
    List<User> findCustomersWithNoOrders();
}
