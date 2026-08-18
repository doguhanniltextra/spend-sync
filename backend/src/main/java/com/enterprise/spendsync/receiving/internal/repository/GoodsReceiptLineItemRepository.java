package com.enterprise.spendsync.receiving.internal.repository;

import com.enterprise.spendsync.receiving.internal.domain.GoodsReceiptLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface GoodsReceiptLineItemRepository extends JpaRepository<GoodsReceiptLineItem, UUID> {

    List<GoodsReceiptLineItem> findAllByGoodsReceiptId(UUID goodsReceiptId);

    @Query("SELECT COALESCE(SUM(li.acceptedQuantity), 0) FROM GoodsReceiptLineItem li " +
           "JOIN li.goodsReceipt gr " +
           "WHERE li.purchaseOrderLineItem.id = :poLineId " +
           "AND gr.status = 'COMPLETED'")
    BigDecimal sumAcceptedQuantityByPoLineId(@Param("poLineId") UUID poLineId);
}
