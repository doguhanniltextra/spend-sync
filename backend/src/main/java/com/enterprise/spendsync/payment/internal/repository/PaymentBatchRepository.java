package com.enterprise.spendsync.payment.internal.repository;

import com.enterprise.spendsync.payment.internal.domain.PaymentBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentBatchRepository extends JpaRepository<PaymentBatch, UUID> {

    Optional<PaymentBatch> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<PaymentBatch> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    List<PaymentBatch> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    @Query("SELECT COUNT(pb) FROM PaymentBatch pb WHERE pb.tenant.id = :tenantId AND pb.batchNumber LIKE :prefix%")
    long countByTenantIdAndBatchNumberPrefix(@Param("tenantId") UUID tenantId, @Param("prefix") String prefix);
}
