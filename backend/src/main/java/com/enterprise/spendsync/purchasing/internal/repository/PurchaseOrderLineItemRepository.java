package com.enterprise.spendsync.purchasing.internal.repository;

import com.enterprise.spendsync.purchasing.internal.domain.PurchaseOrderLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseOrderLineItemRepository extends JpaRepository<PurchaseOrderLineItem, UUID> {

    List<PurchaseOrderLineItem> findAllByPurchaseOrderIdOrderByLineNumberAsc(UUID purchaseOrderId);
}
