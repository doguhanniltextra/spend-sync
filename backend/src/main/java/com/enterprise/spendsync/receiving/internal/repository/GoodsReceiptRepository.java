package com.enterprise.spendsync.receiving.internal.repository;

import com.enterprise.spendsync.receiving.internal.domain.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, UUID> {

    Optional<GoodsReceipt> findByIdAndTenantId(UUID id, UUID tenantId);

    List<GoodsReceipt> findAllByTenantIdAndPurchaseOrderIdOrderByCreatedAtDesc(UUID tenantId, UUID purchaseOrderId);

    List<GoodsReceipt> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    @Query("SELECT COUNT(gr) FROM GoodsReceipt gr WHERE gr.tenant.id = :tenantId AND gr.receiptNumber LIKE :prefix%")
    long countByTenantIdAndReceiptNumberPrefix(@Param("tenantId") UUID tenantId, @Param("prefix") String prefix);
}
