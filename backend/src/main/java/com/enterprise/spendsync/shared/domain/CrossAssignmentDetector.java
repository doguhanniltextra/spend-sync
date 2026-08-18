package com.enterprise.spendsync.shared.domain;

import com.enterprise.spendsync.core.internal.domain.Facility;
import com.enterprise.spendsync.core.internal.domain.LegalEntity;
import org.springframework.stereotype.Component;

/**
 * Cross-Assignment Detection Engine.
 * Inspects whether a requisition/purchase order's source Legal Entity matches the delivery Facility's Legal Entity.
 */
@Component
public class CrossAssignmentDetector {

    /**
     * Evaluates cross-assignment between source Legal Entity and delivery Facility.
     *
     * @param sourceLegalEntity the Legal Entity funding the spend
     * @param deliveryFacility the physical Facility receiving the items
     * @return a {@link CrossAssignmentWarning} object
     */
    public CrossAssignmentWarning detect(LegalEntity sourceLegalEntity, Facility deliveryFacility) {
        if (sourceLegalEntity == null || deliveryFacility == null || deliveryFacility.getLegalEntity() == null) {
            return CrossAssignmentWarning.none();
        }

        LegalEntity facilityLegalEntity = deliveryFacility.getLegalEntity();

        if (sourceLegalEntity.getId().equals(facilityLegalEntity.getId())) {
            return CrossAssignmentWarning.none();
        }

        return CrossAssignmentWarning.of(
                sourceLegalEntity.getId(),
                sourceLegalEntity.getName(),
                facilityLegalEntity.getId(),
                facilityLegalEntity.getName(),
                deliveryFacility.getId(),
                deliveryFacility.getName()
        );
    }
}
