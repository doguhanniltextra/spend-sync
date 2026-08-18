package com.enterprise.spendsync.requisition.internal.repository;

import com.enterprise.spendsync.requisition.internal.domain.RequisitionLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequisitionLineItemRepository extends JpaRepository<RequisitionLineItem, UUID> {

    List<RequisitionLineItem> findAllByRequisitionIdOrderByLineNumberAsc(UUID requisitionId);
}
