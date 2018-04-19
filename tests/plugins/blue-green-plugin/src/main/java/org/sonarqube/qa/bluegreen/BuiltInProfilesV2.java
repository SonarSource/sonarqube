package org.sonarqube.qa.bluegreen;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

public class BuiltInProfilesV2 implements BuiltInQualityProfilesDefinition {
  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile("Profile One", "xoo");
    profile.setDefault(true);
    profile.activateRule(RulesDefinitionV1.REPOSITORY_KEY, "a").overrideSeverity("BLOCKER");
    profile.activateRule(RulesDefinitionV1.REPOSITORY_KEY, "b").overrideSeverity("CRITICAL");
    profile.done();
  }
}

