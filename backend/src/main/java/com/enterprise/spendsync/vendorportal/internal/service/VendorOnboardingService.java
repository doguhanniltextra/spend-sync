package com.enterprise.spendsync.vendorportal.internal.service;

import com.enterprise.spendsync.vendorportal.dto.VendorAcceptInviteRequest;
import com.enterprise.spendsync.vendorportal.dto.VendorAuthResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorInvitationDetailsResponse;
import com.enterprise.spendsync.vendorportal.dto.VendorInviteRequest;

import java.util.List;
import java.util.UUID;

public interface VendorOnboardingService {

    VendorInvitationDetailsResponse inviteVendor(VendorInviteRequest request, UUID invitedByUserId);

    VendorInvitationDetailsResponse getInvitationDetails(String token);

    VendorAuthResponse acceptInvitation(VendorAcceptInviteRequest request);

    List<VendorInvitationDetailsResponse> listInvitations();
}
