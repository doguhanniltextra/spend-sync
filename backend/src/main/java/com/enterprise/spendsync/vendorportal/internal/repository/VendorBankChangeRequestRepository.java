package com.enterprise.spendsync.vendorportal.internal.repository;

import com.enterprise.spendsync.vendorportal.internal.domain.BankChangeRequestStatus;
import com.enterprise.spendsync.vendorportal.internal.domain.VendorBankChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VendorBankChangeRequestRepository extends JpaRepository<VendorBankChangeRequest, UUID> {

    List<VendorBankChangeRequest> findAllByTenantIdAndVendorIdOrderByCreatedAtDesc(UUID tenantId, UUID vendorId);

    List<VendorBankChangeRequest> findAllByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, BankChangeRequestStatus status);

    List<VendorBankChangeRequest> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
