
package com.efe.veterinaryclinic.followup;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "follow_up_rule_steps")
@Getter
public class FollowUpRuleStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private FollowUpRule rule;

    @Column(nullable = false)
    private int stepNumber;

    @Column(nullable = false)
    private int intervalValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IntervalUnit intervalUnit;

    protected FollowUpRuleStep() {
    }

    public FollowUpRuleStep(
            FollowUpRule rule,
            int stepNumber,
            int intervalValue,
            IntervalUnit intervalUnit) {

        this.rule = rule;
        this.stepNumber = stepNumber;
        this.intervalValue = intervalValue;
        this.intervalUnit = intervalUnit;
    }
}
