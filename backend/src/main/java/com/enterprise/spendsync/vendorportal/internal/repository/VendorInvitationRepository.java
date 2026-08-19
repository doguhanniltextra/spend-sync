package com.enterprise.spendsync.vendorportal.internal.repository;

import com.enterprise.spendsync.vendorportal.internal.domain.VendorInvitation;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorInvitationRepository extends JpaRepository<VendorInvitation, UUID> {

    Optional<VendorInvitation> findByInvitationToken(String invitationToken);

    Optional<VendorInvitation> findByTenantIdAndEmailAndStatus(UUID tenantId, String email, VendorInvitationStatus status);

    List<VendorInvitation> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
