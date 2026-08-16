package com.enterprise.spendsync.core.internal.repository;

import com.enterprise.spendsync.core.internal.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    List<User> findAllByTenantId(UUID tenantId);
    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);
    Optional<User> findByEmail(String email);
    boolean existsByTenantIdAndEmail(UUID tenantId, String email);
    boolean existsByEmail(String email);
}
