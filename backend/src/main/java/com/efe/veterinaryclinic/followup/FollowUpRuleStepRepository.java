package com.efe.veterinaryclinic.followup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowUpRuleStepRepository
        extends JpaRepository<FollowUpRuleStep, Long> {

    List<FollowUpRuleStep> findByRuleIdOrderByStepNumberAsc(Long ruleId);
}