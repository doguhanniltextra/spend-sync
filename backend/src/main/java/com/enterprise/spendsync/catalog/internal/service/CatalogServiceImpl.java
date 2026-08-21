package com.enterprise.spendsync.catalog.internal.service;

import com.enterprise.spendsync.catalog.dto.CatalogAutofillResponse;
import com.enterprise.spendsync.catalog.dto.CatalogCategoryDto;
import com.enterprise.spendsync.catalog.dto.CatalogHealthMetricsDto;
import com.enterprise.spendsync.catalog.dto.CatalogItemResponse;
import com.enterprise.spendsync.catalog.internal.domain.CatalogCategory;
import com.enterprise.spendsync.catalog.internal.domain.CatalogItem;
import com.enterprise.spendsync.catalog.internal.repository.CatalogCategoryRepository;
import com.enterprise.spendsync.catalog.internal.repository.CatalogItemRepository;
import com.enterprise.spendsync.shared.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.cache.annotation.Cacheable;
import com.enterprise.spendsync.shared.config.RedisConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final CatalogItemRepository itemRepository;
    private final CatalogCategoryRepository categoryRepository;

    public CatalogServiceImpl(CatalogItemRepository itemRepository,
                              CatalogCategoryRepository categoryRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Page<CatalogItemResponse> searchCatalogItems(UUID tenantId, String query, UUID categoryId, Boolean activeOnly, Pageable pageable) {
        Specification<CatalogItem> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenant").get("id"), tenantId));

            boolean onlyActive = activeOnly == null || activeOnly;
            if (onlyActive) {
                predicates.add(cb.isTrue(root.get("isActive")));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (query != null && !query.trim().isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                Join<Object, Object> catJoin = root.join("category", JoinType.LEFT);
                Join<Object, Object> vendorJoin = root.join("preferredVendor", JoinType.LEFT);

                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                Predicate codeMatch = cb.like(cb.lower(root.get("itemCode")), pattern);
                Predicate catMatch = cb.like(cb.lower(catJoin.get("fullPath")), pattern);
                Predicate vendorMatch = cb.like(cb.lower(vendorJoin.get("name")), pattern);

                predicates.add(cb.or(nameMatch, descMatch, codeMatch, catMatch, vendorMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return itemRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Override
    @Cacheable(value = RedisConfig.CACHE_CATALOG_ITEMS, key = "#tenantId.toString() + ':' + #id.toString()")
    public CatalogItemResponse getCatalogItemById(UUID tenantId, UUID id) {
        CatalogItem item = itemRepository.findWithDetailsById(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("CatalogItem not found with id: " + id));
        return mapToResponse(item);
    }

    @Override
    public CatalogAutofillResponse getCatalogItemAutofill(UUID tenantId, UUID itemId) {
        CatalogItem item = itemRepository.findWithDetailsById(tenantId, itemId)
                .orElseThrow(() -> new ResourceNotFoundException("CatalogItem not found with id: " + itemId));

        String contractAlert = computeContractAlert(item.getValidUntil());

        String categoryCode = item.getCategory() != null ? item.getCategory().getCode() : "DEFAULT";
        String categoryFullPath = item.getCategory() != null ? item.getCategory().getFullPath() : "General";

        BigDecimal defaultQty = BigDecimal.ONE;
        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal vatRate = item.getVatRate() != null ? item.getVatRate() : new BigDecimal("0.20");
        BigDecimal lineTotal = unitPrice.multiply(defaultQty);

        CatalogAutofillResponse.LineItemSuggestion lineSuggestion = new CatalogAutofillResponse.LineItemSuggestion(
                item.getName() + (item.getDescription() != null && !item.getDescription().isBlank() ? " - " + item.getDescription() : ""),
                categoryCode,
                categoryFullPath,
                defaultQty,
                item.getUnitOfMeasure(),
                unitPrice,
                vatRate,
                lineTotal
        );

        CatalogAutofillResponse.SuggestedVendor suggestedVendor = null;
        if (item.getPreferredVendor() != null) {
            suggestedVendor = new CatalogAutofillResponse.SuggestedVendor(
                    item.getPreferredVendor().getId(),
                    item.getPreferredVendor().getName(),
                    item.getPreferredVendor().getTaxNumber(),
                    item.getPreferredVendor().getOrderEmail(),
                    item.getPreferredVendor().getPaymentTerms() != null ? item.getPreferredVendor().getPaymentTerms().name() : "NET_30"
            );
        }

        CatalogAutofillResponse.BudgetHint budgetHint = new CatalogAutofillResponse.BudgetHint(
                item.getGlAccountCode(),
                null
        );

        return new CatalogAutofillResponse(
                item.getId(),
                item.getItemCode(),
                item.getName(),
                lineSuggestion,
                suggestedVendor,
                budgetHint,
                contractAlert
        );
    }

    @Override
    @Cacheable(value = RedisConfig.CACHE_CATALOG_CATEGORIES, key = "#tenantId.toString()")
    public List<CatalogCategoryDto> getCategoryTree(UUID tenantId) {
        List<CatalogCategory> rootCategories = categoryRepository.findByTenantIdAndParentIsNull(tenantId);
        return rootCategories.stream()
                .map(this::mapToCategoryDtoRecursive)
                .toList();
    }

    @Override
    @Cacheable(value = RedisConfig.CACHE_ANALYTICS_DASHBOARD_KPI, key = "'health:' + #tenantId.toString()")
    public CatalogHealthMetricsDto getCatalogHealthMetrics(UUID tenantId) {
        LocalDate today = LocalDate.now();
        long totalActive = itemRepository.countByTenantIdAndIsActiveTrue(tenantId);
        long totalCategories = categoryRepository.countByTenantId(tenantId);
        long expiring30 = itemRepository.countExpiringSoon(tenantId, today, today.plusDays(30));
        long expiring7 = itemRepository.countExpiringSoon(tenantId, today, today.plusDays(7));
        long expired = itemRepository.countExpired(tenantId, today);
        long preferred = itemRepository.countByTenantIdAndIsPreferredTrueAndIsActiveTrue(tenantId);

        List<CatalogItem> topPreferred = itemRepository.findTop10ByTenantIdAndIsPreferredTrueAndIsActiveTrueOrderByUpdatedAtDesc(tenantId);
        List<CatalogHealthMetricsDto.TopItemMetric> topMetrics = topPreferred.stream()
                .map(item -> new CatalogHealthMetricsDto.TopItemMetric(
                        item.getItemCode(),
                        item.getName(),
                        item.getPreferredVendor() != null ? item.getPreferredVendor().getName() : "-",
                        item.getCategory() != null ? item.getCategory().getFullPath() : "-",
                        item.getUnitPrice() + " " + item.getCurrency()
                ))
                .toList();

        return new CatalogHealthMetricsDto(
                totalActive,
                totalCategories,
                expiring30,
                expiring7,
                expired,
                preferred,
                topMetrics
        );
    }

    private CatalogItemResponse mapToResponse(CatalogItem item) {
        String contractAlert = computeContractAlert(item.getValidUntil());

        return new CatalogItemResponse(
                item.getId(),
                item.getItemCode(),
                item.getName(),
                item.getDescription(),
                item.getCategory() != null ? item.getCategory().getId() : null,
                item.getCategory() != null ? item.getCategory().getName() : null,
                item.getCategory() != null ? item.getCategory().getFullPath() : null,
                item.getPreferredVendor() != null ? item.getPreferredVendor().getId() : null,
                item.getPreferredVendor() != null ? item.getPreferredVendor().getName() : null,
                item.getPreferredVendor() != null ? item.getPreferredVendor().getTaxNumber() : null,
                item.getPreferredVendor() != null && item.getPreferredVendor().getTier() != null ? item.getPreferredVendor().getTier().name() : null,
                item.getUnitPrice(),
                item.getCurrency(),
                item.getVatRate(),
                item.getUnitOfMeasure(),
                item.getContractReference(),
                item.getValidFrom(),
                item.getValidUntil(),
                item.isActive(),
                item.isPreferred(),
                item.getGlAccountCode(),
                contractAlert,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private CatalogCategoryDto mapToCategoryDtoRecursive(CatalogCategory category) {
        long itemCount = itemRepository.countByTenantIdAndCategory(category.getTenant().getId(), category);
        List<CatalogCategoryDto> childrenDtos = category.getChildren() != null
                ? category.getChildren().stream().map(this::mapToCategoryDtoRecursive).toList()
                : Collections.emptyList();

        return new CatalogCategoryDto(
                category.getId(),
                category.getCode(),
                category.getName(),
                category.getFullPath(),
                category.getIconCode(),
                category.getDescription(),
                category.getParent() != null ? category.getParent().getId() : null,
                itemCount,
                childrenDtos
        );
    }

    private String computeContractAlert(LocalDate validUntil) {
        if (validUntil == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        if (validUntil.isBefore(today)) {
            return "⚠️ CONTRACT EXPIRED (Expired on: " + validUntil + ")";
        }
        long daysRemaining = ChronoUnit.DAYS.between(today, validUntil);
        if (daysRemaining <= 7) {
            return "⚠️ CRITICAL CONTRACT EXPIRY: Contract expires in " + daysRemaining + " days (" + validUntil + ").";
        }
        if (daysRemaining <= 30) {
            return "ℹ️ Contract expiry notice: Contract expires in " + daysRemaining + " days (" + validUntil + ").";
        }
        return null;
    }
}
