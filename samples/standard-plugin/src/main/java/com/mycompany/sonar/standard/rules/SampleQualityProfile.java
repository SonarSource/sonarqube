package com.mycompany.sonar.standard.rules;

import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;

public class SampleQualityProfile extends ProfileDefinition {

  private SampleRuleRepository ruleRepository;

  // The component ruleRepository is injected at runtime
  public SampleQualityProfile(SampleRuleRepository ruleRepository) {
    this.ruleRepository = ruleRepository;
  }

  @Override
  public RulesProfile createProfile(ValidationMessages validation) {
    RulesProfile profile = RulesProfile.create("Sample profile", Java.KEY);
    profile.activateRule(ruleRepository.getRule1(), RulePriority.MAJOR);
    profile.activateRule(ruleRepository.getRule1(), null);
    return profile;
  }
}
