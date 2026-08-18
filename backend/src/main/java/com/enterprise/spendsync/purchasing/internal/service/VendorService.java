package com.enterprise.spendsync.purchasing.internal.service;

import com.enterprise.spendsync.purchasing.internal.domain.VendorCategory;
import com.enterprise.spendsync.purchasing.internal.domain.VendorStatus;
import com.enterprise.spendsync.purchasing.internal.domain.VendorTier;
import com.enterprise.spendsync.purchasing.internal.dto.CreateVendorRequest;
import com.enterprise.spendsync.purchasing.internal.dto.UpdateVendorStatusRequest;
import com.enterprise.spendsync.purchasing.internal.dto.VendorResponse;

import java.util.List;
import java.util.UUID;

public interface VendorService {

    VendorResponse createVendor(CreateVendorRequest request);

    VendorResponse getVendorById(UUID vendorId);

    List<VendorResponse> getAllVendors(VendorStatus status, VendorCategory category, VendorTier tier);

    VendorResponse updateVendorStatus(UUID vendorId, UpdateVendorStatusRequest request);
}
