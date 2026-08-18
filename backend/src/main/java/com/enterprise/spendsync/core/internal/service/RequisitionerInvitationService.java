package com.enterprise.spendsync.core.internal.service;

import com.enterprise.spendsync.core.internal.dto.GenerateRequisitionerLinkRequest;
import com.enterprise.spendsync.core.internal.dto.JoinAsRequisitionerRequest;
import com.enterprise.spendsync.core.internal.dto.RequisitionerLinkDetailsResponse;
import com.enterprise.spendsync.core.internal.dto.RequisitionerLinkResponse;
import com.enterprise.spendsync.core.internal.dto.UserResponse;

public interface RequisitionerInvitationService {

    RequisitionerLinkResponse generateRequisitionerLink(GenerateRequisitionerLinkRequest request);

    RequisitionerLinkDetailsResponse getRequisitionerLinkDetails(String token);

    UserResponse joinAsRequisitioner(JoinAsRequisitionerRequest request);
}
