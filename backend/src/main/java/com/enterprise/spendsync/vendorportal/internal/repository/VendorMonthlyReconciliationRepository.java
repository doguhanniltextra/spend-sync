package com.enterprise.spendsync.vendorportal.internal.repository;

import com.enterprise.spendsync.vendorportal.internal.domain.VendorMonthlyReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorMonthlyReconciliationRepository extends JpaRepository<VendorMonthlyReconciliation, UUID> {

    Optional<VendorMonthlyReconciliation> findByTenantIdAndVendorIdAndPeriodYearAndPeriodMonth(UUID tenantId, UUID vendorId, int periodYear, int periodMonth);

    List<VendorMonthlyReconciliation> findAllByTenantIdAndVendorIdOrderByPeriodYearDescPeriodMonthDesc(UUID tenantId, UUID vendorId);
}
