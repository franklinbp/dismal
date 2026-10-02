package com.dismal.distribuciones.modules.security.account.repository;

import com.dismal.distribuciones.modules.security.account.domain.AccountActionToken;
import com.dismal.distribuciones.modules.security.account.domain.AccountTokenType;
import com.dismal.distribuciones.modules.security.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AccountActionTokenRepository extends JpaRepository<AccountActionToken, UUID> {

    Optional<AccountActionToken> findByTokenHashAndType(String tokenHash, AccountTokenType type);

    @Modifying
    @Query("update AccountActionToken token set token.consumedAt = :consumedAt " +
            "where token.user = :user and token.type = :type and token.consumedAt is null")
    int consumeActiveTokens(
            @Param("user") User user,
            @Param("type") AccountTokenType type,
            @Param("consumedAt") LocalDateTime consumedAt
    );
}
