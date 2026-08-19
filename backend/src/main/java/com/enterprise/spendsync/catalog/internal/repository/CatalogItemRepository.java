package com.enterprise.spendsync.catalog.internal.repository;

import com.enterprise.spendsync.catalog.internal.domain.CatalogCategory;
import com.enterprise.spendsync.catalog.internal.domain.CatalogItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, UUID>, JpaSpecificationExecutor<CatalogItem> {

    Optional<CatalogItem> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<CatalogItem> findByTenantIdAndItemCode(UUID tenantId, String itemCode);

    List<CatalogItem> findByTenantId(UUID tenantId);

    long countByTenantIdAndIsActiveTrue(UUID tenantId);

    long countByTenantIdAndIsPreferredTrueAndIsActiveTrue(UUID tenantId);

    long countByTenantIdAndCategory(UUID tenantId, CatalogCategory category);

    @Query("SELECT COUNT(i) FROM CatalogItem i WHERE i.tenant.id = :tenantId AND i.isActive = true AND i.validUntil IS NOT NULL AND i.validUntil >= :today AND i.validUntil <= :thresholdDate")
    long countExpiringSoon(@Param("tenantId") UUID tenantId, @Param("today") LocalDate today, @Param("thresholdDate") LocalDate thresholdDate);

    @Query("SELECT COUNT(i) FROM CatalogItem i WHERE i.tenant.id = :tenantId AND i.isActive = true AND i.validUntil IS NOT NULL AND i.validUntil < :today")
    long countExpired(@Param("tenantId") UUID tenantId, @Param("today") LocalDate today);

    List<CatalogItem> findTop10ByTenantIdAndIsPreferredTrueAndIsActiveTrueOrderByUpdatedAtDesc(UUID tenantId);

    @Query("SELECT i FROM CatalogItem i LEFT JOIN FETCH i.category LEFT JOIN FETCH i.preferredVendor WHERE i.tenant.id = :tenantId AND i.id = :id")
    Optional<CatalogItem> findWithDetailsById(@Param("tenantId") UUID tenantId, @Param("id") UUID id);
}
