package com.enterprise.spendsync.core.internal.repository;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlug(String slug);
    boolean existsByName(String name);
    boolean existsBySlug(String slug);
}
