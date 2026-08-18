package com.enterprise.spendsync.payment.internal.repository;

import com.enterprise.spendsync.payment.internal.domain.PaymentBatchItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentBatchItemRepository extends JpaRepository<PaymentBatchItem, UUID> {

    List<PaymentBatchItem> findAllByPaymentBatchId(UUID paymentBatchId);

    @Query("SELECT COUNT(pbi) > 0 FROM PaymentBatchItem pbi " +
           "JOIN pbi.paymentBatch pb " +
           "WHERE pbi.supplierInvoice.id = :invoiceId " +
           "AND pb.status IN ('DRAFT', 'APPROVED', 'DISPATCHED')")
    boolean isInvoiceAlreadyInActiveBatch(@Param("invoiceId") UUID invoiceId);
}
