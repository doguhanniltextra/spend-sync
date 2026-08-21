package com.enterprise.spendsync.catalog.domain;

import com.enterprise.spendsync.catalog.internal.domain.CatalogCategory;
import com.enterprise.spendsync.catalog.internal.domain.CatalogItem;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CatalogItem Domain Entity Pure Unit Tests")
class CatalogItemTest {

    private Tenant tenant;
    private CatalogCategory category;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("SpendSync Global");

        category = new CatalogCategory(tenant, null, "CAT-HW", "Hardware", null, null);
        category.setId(UUID.randomUUID());

        vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setName("Apple Distribution TR");
    }

    @Test
    @DisplayName("Should initialize CatalogItem with default VAT, active status and uppercase item code")
    void shouldInitializeCatalogItemDefaults() {
        CatalogItem item = new CatalogItem(
                tenant,
                "mac-pro-16",
                "MacBook Pro 16 M3",
                "Apple MacBook Pro 16 inch M3 Max",
                category,
                vendor,
                new BigDecimal("120000.00"),
                "TRY",
                new BigDecimal("0.20"),
                "PIECE",
                "CNT-2026-001",
                LocalDate.now(),
                LocalDate.now().plusYears(1),
                true,
                "GL-150-01",
                null
        );

        assertThat(item.getItemCode()).isEqualTo("MAC-PRO-16");
        assertThat(item.getName()).isEqualTo("MacBook Pro 16 M3");
        assertThat(item.getUnitPrice()).isEqualByComparingTo(new BigDecimal("120000.00"));
        assertThat(item.getVatRate()).isEqualByComparingTo(new BigDecimal("0.20"));
        assertThat(item.isActive()).isTrue();
        assertThat(item.isPreferred()).isTrue();
        assertThat(item.getContractReference()).isEqualTo("CNT-2026-001");
    }

    @Test
    @DisplayName("Should correctly update mutable properties and toggle active flag")
    void shouldUpdateCatalogItemProperties() {
        CatalogItem item = new CatalogItem(
                tenant, "SRV-01", "Rack Server", "Base 1U server", category, vendor,
                new BigDecimal("50000.00"), "USD", new BigDecimal("0.20"), "PIECE",
                null, null, null, false, null, null
        );

        item.setUnitPrice(new BigDecimal("55000.00"));
        item.setActive(false);
        item.setPreferred(true);

        assertThat(item.getUnitPrice()).isEqualByComparingTo(new BigDecimal("55000.00"));
        assertThat(item.isActive()).isFalse();
        assertThat(item.isPreferred()).isTrue();
    }
}
