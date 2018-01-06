/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.qualityprofile;

import java.util.Arrays;
import org.junit.Test;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInActiveRule;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInQualityProfile;
import org.sonar.api.utils.ValidationMessages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class BuiltInQProfileDefinitionsBridgeTest {

  @Test
  public void noProfileDefinitions() {
    BuiltInQProfileDefinitionsBridge bridge = new BuiltInQProfileDefinitionsBridge();

    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    bridge.define(context);

    assertThat(context.profilesByLanguageAndName()).isEmpty();
  }

  @Test
  public void bridgeProfileDefinitions() {
    BuiltInQProfileDefinitionsBridge bridge = new BuiltInQProfileDefinitionsBridge(new Profile1(), new NullProfile(), new ProfileWithError());

    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    bridge.define(context);

    assertThat(context.profilesByLanguageAndName()).hasSize(1);
    assertThat(context.profilesByLanguageAndName().get("xoo")).hasSize(1);

    BuiltInQualityProfile profile1 = context.profile("xoo", "Profile 1");
    assertThat(profile1).isNotNull();
    assertThat(profile1.rules()).hasSize(3);
    BuiltInActiveRule defaultSeverity = profile1.rule(RuleKey.of("repo1", "defaultSeverity"));
    assertThat(defaultSeverity).isNotNull();
    assertThat(defaultSeverity.overriddenSeverity()).isNull();
    assertThat(defaultSeverity.overriddenParams()).isEmpty();

    assertThat(profile1.rule(RuleKey.of("repo1", "overrideSeverity")).overriddenSeverity()).isEqualTo(Severity.CRITICAL);

    assertThat(profile1.rule(RuleKey.of("repo1", "overrideParam")).overriddenParams())
      .extracting(BuiltInQualityProfilesDefinition.OverriddenParam::key, BuiltInQualityProfilesDefinition.OverriddenParam::overriddenValue).containsOnly(tuple("param", "value"));
  }

  private class Profile1 extends ProfileDefinition {
    @Override
    public RulesProfile createProfile(ValidationMessages validation) {
      RulesProfile profile1 = RulesProfile.create("Profile 1", "xoo");

      profile1.activateRule(Rule.create("repo1", "defaultSeverity"), null);
      profile1.activateRule(Rule.create("repo1", "overrideSeverity"), RulePriority.CRITICAL);
      Rule ruleWithParam = Rule.create("repo1", "overrideParam");
      ruleWithParam.setParams(Arrays.asList(new RuleParam(ruleWithParam, "param", "", "")));
      ActiveRule arWithParam = profile1.activateRule(ruleWithParam, null);
      arWithParam.setParameter("param", "value");

      return profile1;
    }
  }

  private class NullProfile extends ProfileDefinition {
    @Override
    public RulesProfile createProfile(ValidationMessages validation) {
      return null;
    }
  }

  private class ProfileWithError extends ProfileDefinition {
    @Override
    public RulesProfile createProfile(ValidationMessages validation) {
      validation.addErrorText("Foo");
      return RulesProfile.create("Profile with errors", "xoo");
    }
  }
}
