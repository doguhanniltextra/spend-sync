package com.enterprise.spendsync.shared.domain;

import java.util.UUID;

/**
 * Value Object representing a Cross-Entity delivery alert (ISO 9001 / TTK md. 367).
 * Triggered when a PR or PO is funded by one Legal Entity, but delivery is made to a Facility belonging to another Legal Entity.
 */
public record CrossAssignmentWarning(
        boolean isCrossEntity,
        String warningCode,
        String warningMessage,
        UUID sourceLegalEntityId,
        String sourceLegalEntityName,
        UUID targetFacilityLegalEntityId,
        String targetFacilityLegalEntityName,
        UUID facilityId,
        String facilityName
) {
    public static CrossAssignmentWarning none() {
        return new CrossAssignmentWarning(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static CrossAssignmentWarning of(
            UUID sourceLegalEntityId,
            String sourceLegalEntityName,
            UUID targetFacilityLegalEntityId,
            String targetFacilityLegalEntityName,
            UUID facilityId,
            String facilityName
    ) {
        String message = String.format(
                "WARNING: Delivery facility '%s' (%s) belongs to a different company than the purchasing legal entity '%s'. " +
                "Please verify that the invoice and accounting entries are issued on behalf of '%s'.",
                facilityName,
                targetFacilityLegalEntityName,
                sourceLegalEntityName,
                sourceLegalEntityName
        );

        return new CrossAssignmentWarning(
                true,
                "CROSS_ENTITY_DELIVERY",
                message,
                sourceLegalEntityId,
                sourceLegalEntityName,
                targetFacilityLegalEntityId,
                targetFacilityLegalEntityName,
                facilityId,
                facilityName
        );
    }
}
