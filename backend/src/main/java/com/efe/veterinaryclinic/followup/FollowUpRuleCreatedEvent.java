
package com.efe.veterinaryclinic.followup;

public record FollowUpRuleCreatedEvent(
        String species,
        String procedureType
) {
}

