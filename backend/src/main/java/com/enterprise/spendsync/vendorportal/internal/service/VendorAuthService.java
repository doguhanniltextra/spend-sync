package com.enterprise.spendsync.vendorportal.internal.service;

import com.enterprise.spendsync.vendorportal.dto.VendorAuthResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorLoginRequest;
import com.enterprise.spendsync.vendorportal.dto.VendorProfileResponse;

import java.util.UUID;

public interface VendorAuthService {

    VendorAuthResponse login(VendorLoginRequest request);

    VendorProfileResponse getVendorProfile(UUID vendorUserId);
}
