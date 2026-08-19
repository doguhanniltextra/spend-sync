package com.enterprise.spendsync.catalog.internal.domain;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

/**
 * Hierarchical Category Entity for Item Master classification.
 * e.g. IT -> Hardware -> Laptop
 */
@Entity
@Table(
        name = "catalog_categories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cat_tenant_code", columnNames = {"tenant_id", "code"}),
                @UniqueConstraint(name = "uk_cat_tenant_fullpath", columnNames = {"tenant_id", "full_path"})
        },
        indexes = {
                @Index(name = "idx_cat_tenant", columnList = "tenant_id"),
                @Index(name = "idx_cat_parent", columnList = "parent_id")
        }
)
public class CatalogCategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CatalogCategory parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CatalogCategory> children = new ArrayList<>();

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "full_path", nullable = false, length = 500)
    private String fullPath;

    @Column(name = "icon_code", length = 50)
    private String iconCode;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    protected CatalogCategory() {
        super();
    }

    public CatalogCategory(Tenant tenant,
                           CatalogCategory parent,
                           String code,
                           String name,
                           String iconCode,
                           String description) {
        super();
        this.tenant = tenant;
        this.parent = parent;
        this.code = code != null ? code.trim().toUpperCase() : generateCodeFromName(name);
        this.name = name != null ? name.trim() : "";
        this.iconCode = iconCode;
        this.description = description;
        this.isActive = true;
        recalculateFullPath();
    }

    public void recalculateFullPath() {
        if (parent == null) {
            this.fullPath = this.name;
        } else {
            this.fullPath = parent.getFullPath() + " / " + this.name;
        }
    }

    private String generateCodeFromName(String name) {
        if (name == null || name.isBlank()) {
            return "CAT-" + System.currentTimeMillis();
        }
        return "CAT-" + name.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "-");
    }

    public Tenant getTenant() {
        return tenant;
    }

    public CatalogCategory getParent() {
        return parent;
    }

    public void setParent(CatalogCategory parent) {
        this.parent = parent;
        recalculateFullPath();
    }

    public List<CatalogCategory> getChildren() {
        return children;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        recalculateFullPath();
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public String getIconCode() {
        return iconCode;
    }

    public void setIconCode(String iconCode) {
        this.iconCode = iconCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
