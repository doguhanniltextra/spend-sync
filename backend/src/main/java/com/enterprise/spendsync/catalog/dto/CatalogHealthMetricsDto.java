package com.enterprise.spendsync.catalog.dto;

import java.util.List;

public record CatalogHealthMetricsDto(
        long totalActiveItems,
        long totalCategories,
        long expiringIn30DaysCount,
        long expiringIn7DaysCount,
        long expiredItemsCount,
        long preferredItemsCount,
        List<TopItemMetric> topPreferredItems
) {
    public record TopItemMetric(
            String itemCode,
            String name,
            String vendorName,
            String categoryFullPath,
            String unitPriceFormatted
    ) {}
}
