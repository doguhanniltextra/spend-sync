package com.enterprise.spendsync.catalog.domain;

import com.enterprise.spendsync.catalog.internal.domain.CatalogCategory;
import com.enterprise.spendsync.core.internal.domain.Tenant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CatalogCategory Domain Entity Unit Tests (Hierarchical Tree & Path Construction)")
class CatalogCategoryTest {

    @Test
    @DisplayName("Should construct hierarchical full path correctly for root and nested categories")
    void shouldConstructCategoryFullPath() {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());

        CatalogCategory root = new CatalogCategory(tenant, null, "CAT-IT", "IT & Technology", null, null);
        root.setId(UUID.randomUUID());
        assertThat(root.getFullPath()).isEqualTo("IT & Technology");

        CatalogCategory sub = new CatalogCategory(tenant, root, "CAT-HW", "Hardware", null, null);
        sub.setId(UUID.randomUUID());
        assertThat(sub.getFullPath()).isEqualTo("IT & Technology / Hardware");

        CatalogCategory leaf = new CatalogCategory(tenant, sub, "CAT-LAPTOP", "Laptops", null, null);
        leaf.setId(UUID.randomUUID());
        assertThat(leaf.getFullPath()).isEqualTo("IT & Technology / Hardware / Laptops");
    }
}
