package com.enterprise.spendsync.catalog.internal.repository;

import com.enterprise.spendsync.catalog.internal.domain.CatalogCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CatalogCategoryRepository extends JpaRepository<CatalogCategory, UUID> {

    List<CatalogCategory> findByTenantId(UUID tenantId);

    List<CatalogCategory> findByTenantIdAndParentIsNull(UUID tenantId);

    Optional<CatalogCategory> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<CatalogCategory> findByTenantIdAndCode(UUID tenantId, String code);

    Optional<CatalogCategory> findByTenantIdAndFullPath(UUID tenantId, String fullPath);

    long countByTenantId(UUID tenantId);

    @Query("SELECT c FROM CatalogCategory c LEFT JOIN FETCH c.children WHERE c.tenant.id = :tenantId AND c.parent IS NULL ORDER BY c.name ASC")
    List<CatalogCategory> findRootCategoriesWithChildren(@Param("tenantId") UUID tenantId);
}
