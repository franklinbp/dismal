package com.dismal.desktop.local.repository;

import com.dismal.desktop.local.domain.LocalLicense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalLicenseRepository extends JpaRepository<LocalLicense, String> {
}
