/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.qualityprofile;

import org.junit.Test;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.paging.Paging;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleQuery;
import org.sonar.server.rule.Rules;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class QProfilesMediumTest {

  @org.junit.Rule
  public ServerTester serverTester = new ServerTester().addComponents(XooRulesDefinition.class, XooProfileDefinition.class);

  @Test
  public void register_profile_at_startup() throws Exception {
    QProfiles qProfiles = serverTester.get(QProfiles.class);
    List<QProfile> profiles = qProfiles.profilesByLanguage("xoo");
    assertThat(profiles).hasSize(1);

    QProfile profile = profiles.get(0);
    assertThat(profile.id()).isNotNull();
    assertThat(profile.name()).isEqualTo("Basic");
    assertThat(profile.language()).isEqualTo("xoo");
    assertThat(qProfiles.defaultProfile("xoo").name()).isEqualTo("Basic");

    assertThat(qProfiles.searchProfileRules(ProfileRuleQuery.create(profile.id()), Paging.create(10, 1)).rules()).hasSize(2);

    // Rule x1
    QProfileRule qProfileRule1 = qProfiles.searchProfileRules(ProfileRuleQuery.create(profile.id()).setNameOrKey("x1"), Paging.create(10, 1)).rules().get(0);
    assertThat(qProfileRule1.key()).isEqualTo("x1");
    assertThat(qProfileRule1.severity()).isEqualTo("MAJOR");
    assertThat(qProfileRule1.params()).hasSize(1);

    QProfileRuleParam qProfileRule1Param = qProfileRule1.params().get(0);
    assertThat(qProfileRule1Param.key()).isEqualTo("acceptWhitespace");
    assertThat(qProfileRule1Param.value()).isEqualTo("true");

    // Rule x2
    QProfileRule qProfileRule2 = qProfiles.searchProfileRules(ProfileRuleQuery.create(profile.id()).setNameOrKey("x2"), Paging.create(10, 1)).rules().get(0);
    assertThat(qProfileRule2.key()).isEqualTo("x2");
    assertThat(qProfileRule2.severity()).isEqualTo("BLOCKER");
    assertThat(qProfileRule2.params()).isEmpty();
  }

  @Test
  public void recreate_built_in_profile_from_language() throws Exception {
    MockUserSession.set().setLogin("julien").setName("Julien").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    QProfiles qProfiles = serverTester.get(QProfiles.class);
    QProfileBackup qProfileBackup = serverTester.get(QProfileBackup.class);
    Rules rules = serverTester.get(Rules.class);

    QProfile profile = qProfiles.profile("Basic", "xoo");

    // Update rule x1 : update severity and update param value
    Rule rule1 = rules.find(RuleQuery.builder().searchQuery("x1").build()).results().iterator().next();
    qProfiles.updateActiveRuleParam(qProfiles.findByProfileAndRule(profile.id(), rule1.id()).activeRuleId(), "acceptWhitespace", "false");
    qProfiles.activateRule(profile.id(), rule1.id(), "INFO");

    // Disable rule x2
    Rule rule2 = rules.find(RuleQuery.builder().searchQuery("x2").build()).results().iterator().next();
    qProfiles.deactivateRule(qProfiles.profile("Basic", "xoo").id(), rule2.id());

    assertThat(qProfileBackup.findDefaultProfileNamesByLanguage("xoo")).hasSize(1);

    // Renamed profile
    qProfiles.renameProfile(profile.id(), "Old Basic");

    // Restore default profiles of xoo
    qProfileBackup.recreateBuiltInProfilesByLanguage("xoo");

    // Reload profile
    profile = qProfiles.profile("Basic", "xoo");

    // Verify rule x1
    QProfileRule qProfileRule = qProfiles.searchProfileRules(ProfileRuleQuery.create(profile.id()).setNameOrKey("x1"), Paging.create(10, 1)).rules().get(0);
    assertThat(qProfileRule.severity()).isEqualTo("MAJOR");
    QProfileRuleParam qProfileRuleParam = qProfileRule.params().get(0);
    assertThat(qProfileRuleParam.key()).isEqualTo("acceptWhitespace");
    assertThat(qProfileRuleParam.value()).isEqualTo("true");

    // Verify rule x2
    assertThat(qProfiles.searchProfileRules(ProfileRuleQuery.create(profile.id()).setNameOrKey("x2"), Paging.create(10, 1)).rules().get(0)).isNotNull();
  }

  @Test
  public void fail_to_recreate_built_in_profile_from_language_if_default_profile_already_exists() throws Exception {
    MockUserSession.set().setLogin("julien").setName("Julien").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    QProfileBackup qProfileBackup = serverTester.get(QProfileBackup.class);

    try {
      // Restore default profiles of xoo -> fail as it already exists
      qProfileBackup.recreateBuiltInProfilesByLanguage("xoo");
      fail();
    } catch (BadRequestException e) {
      assertThat(e.l10nKey()).isEqualTo("quality_profiles.profile_x_already_exists");
      assertThat(e.l10nParams()).containsOnly("Basic");
    }
  }

  public static class XooProfileDefinition extends ProfileDefinition {
    @Override
    public RulesProfile createProfile(ValidationMessages validation) {
      final RulesProfile profile = RulesProfile.create("Basic", "xoo");
      ActiveRule activeRule1 = profile.activateRule(
        org.sonar.api.rules.Rule.create("xoo", "x1").setParams(newArrayList(new RuleParam().setKey("acceptWhitespace"))),
        RulePriority.MAJOR);
      activeRule1.setParameter("acceptWhitespace", "true");

      profile.activateRule(org.sonar.api.rules.Rule.create("xoo", "x2"), RulePriority.BLOCKER);
      return profile;
    }
  }

  public static class XooRulesDefinition implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repository = context.createRepository("xoo", "xoo").setName("Xoo Repo");
      repository.createRule("x1")
        .setName("x1 name")
        .setHtmlDescription("x1 desc")
        .setSeverity(Severity.MINOR)
        .createParam("acceptWhitespace")
        .setDefaultValue("false")
        .setType(RuleParamType.BOOLEAN)
        .setDescription("Accept whitespaces on the line");

      repository.createRule("x2")
        .setName("x2 name")
        .setHtmlDescription("x2 desc")
        .setSeverity(Severity.MAJOR);
      repository.done();
    }
  }
}
