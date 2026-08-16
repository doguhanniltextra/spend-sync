package com.enterprise.spendsync.core.internal.repository;

import com.enterprise.spendsync.core.internal.domain.UserInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserInvitationRepository extends JpaRepository<UserInvitation, UUID> {
    Optional<UserInvitation> findByInviteToken(String inviteToken);
    List<UserInvitation> findAllByTenantId(UUID tenantId);
    List<UserInvitation> findAllByTenantIdAndEmail(UUID tenantId, String email);
    boolean existsByTenantIdAndEmailAndIsAcceptedFalse(UUID tenantId, String email);
}
