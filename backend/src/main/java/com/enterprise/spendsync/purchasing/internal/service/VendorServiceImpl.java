package com.enterprise.spendsync.purchasing.internal.service;

import com.enterprise.spendsync.core.internal.domain.Tenant;
import com.enterprise.spendsync.core.internal.repository.TenantRepository;
import com.enterprise.spendsync.purchasing.internal.domain.Vendor;
import com.enterprise.spendsync.purchasing.internal.domain.VendorCategory;
import com.enterprise.spendsync.purchasing.internal.domain.VendorStatus;
import com.enterprise.spendsync.purchasing.internal.domain.VendorTier;
import com.enterprise.spendsync.purchasing.internal.dto.CreateVendorRequest;
import com.enterprise.spendsync.purchasing.internal.dto.UpdateVendorStatusRequest;
import com.enterprise.spendsync.purchasing.internal.dto.VendorResponse;
import com.enterprise.spendsync.purchasing.internal.repository.VendorRepository;
import com.enterprise.spendsync.shared.exception.SpendSyncException;
import com.enterprise.spendsync.shared.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;
    private final TenantRepository tenantRepository;

    public VendorServiceImpl(VendorRepository vendorRepository, TenantRepository tenantRepository) {
        this.vendorRepository = vendorRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional
    public VendorResponse createVendor(CreateVendorRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new SpendSyncException("Tenant not found", HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND") {});

        if (vendorRepository.existsByTaxNumberAndTenantId(request.taxNumber().trim(), tenantId)) {
            throw new SpendSyncException(
                    "Vendor with Tax Number '" + request.taxNumber() + "' already exists in this tenant.",
                    HttpStatus.CONFLICT,
                    "DUPLICATE_VENDOR_TAX_NUMBER"
            ) {};
        }

        Vendor vendor = new Vendor(
                tenant,
                request.name().trim(),
                request.taxNumber().trim(),
                request.taxOffice(),
                request.category(),
                request.tier(),
                request.isEInvoiceRegistered() != null ? request.isEInvoiceRegistered() : true,
                request.orderEmail().trim().toLowerCase(),
                request.phoneNumber(),
                request.address(),
                request.city(),
                request.country(),
                request.paymentTerms(),
                request.bankName(),
                request.iban()
        );

        Vendor saved = vendorRepository.save(vendor);
        return VendorResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VendorResponse getVendorById(UUID vendorId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Vendor vendor = vendorRepository.findByIdAndTenantId(vendorId, tenantId)
                .orElseThrow(() -> new SpendSyncException("Vendor not found", HttpStatus.NOT_FOUND, "VENDOR_NOT_FOUND") {});
        return VendorResponse.from(vendor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorResponse> getAllVendors(VendorStatus status, VendorCategory category, VendorTier tier) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        List<Vendor> vendors;
        if (status != null) {
            vendors = vendorRepository.findAllByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status);
        } else if (category != null) {
            vendors = vendorRepository.findAllByTenantIdAndCategoryOrderByCreatedAtDesc(tenantId, category);
        } else if (tier != null) {
            vendors = vendorRepository.findAllByTenantIdAndTierOrderByCreatedAtDesc(tenantId, tier);
        } else {
            vendors = vendorRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
        }
        return vendors.stream().map(VendorResponse::from).toList();
    }

    @Override
    @Transactional
    public VendorResponse updateVendorStatus(UUID vendorId, UpdateVendorStatusRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Vendor vendor = vendorRepository.findByIdAndTenantId(vendorId, tenantId)
                .orElseThrow(() -> new SpendSyncException("Vendor not found", HttpStatus.NOT_FOUND, "VENDOR_NOT_FOUND") {});

        vendor.setStatus(request.status());
        Vendor saved = vendorRepository.save(vendor);
        return VendorResponse.from(saved);
    }
}
