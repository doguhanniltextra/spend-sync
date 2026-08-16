package com.enterprise.spendsync.core.internal.service;

import com.enterprise.spendsync.core.internal.dto.AcceptSubAccountInviteRequest;
import com.enterprise.spendsync.core.internal.dto.InviteSubAccountRequest;
import com.enterprise.spendsync.core.internal.dto.SubAccountInvitationDetailsResponse;
import com.enterprise.spendsync.core.internal.dto.SubAccountInvitationResponse;
import com.enterprise.spendsync.core.internal.dto.UserResponse;

import java.util.List;
import java.util.UUID;

public interface SubAccountInvitationService {

    SubAccountInvitationResponse inviteSubAccount(InviteSubAccountRequest request);

    SubAccountInvitationDetailsResponse getSubAccountInvitationDetails(String token);

    UserResponse acceptSubAccountInvite(AcceptSubAccountInviteRequest request);

    List<SubAccountInvitationResponse> getAllActiveInvitations();

    void revokeInvitation(UUID invitationId);
}
