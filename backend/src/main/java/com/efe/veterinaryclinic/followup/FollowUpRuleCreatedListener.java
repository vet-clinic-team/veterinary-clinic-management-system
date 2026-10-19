
package com.efe.veterinaryclinic.followup;

import com.efe.veterinaryclinic.vaccination.VaccinationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FollowUpRuleCreatedListener {

    private final VaccinationService vaccinationService;

    public FollowUpRuleCreatedListener(
            VaccinationService vaccinationService) {
        this.vaccinationService = vaccinationService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onRuleCreated(FollowUpRuleCreatedEvent event) {

        System.out.println("RULE RECALCULATION EVENT: "
                + event.species() + " - " + event.procedureType());

        vaccinationService.recalculateForNewRule(
                event.species(),
                event.procedureType()
        );
    }
}

