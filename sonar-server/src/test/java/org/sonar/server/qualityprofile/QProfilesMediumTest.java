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

import org.junit.Ignore;
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
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

@Ignore
@Deprecated
public class QProfilesMediumTest {

  @org.junit.Rule
  public ServerTester serverTester = new ServerTester().addComponents(XooRulesDefinition.class, XooProfileDefinition.class);

  @Test
  public void recreate_built_in_profile_from_language() throws Exception {
//    MockUserSession.set().setLogin("julien").setName("Julien").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
//
//    QProfiles qProfiles = tester.get(QProfiles.class);
//    QProfileBackup qProfileBackup = tester.get(QProfileBackup.class);
//    Rules rules = tester.get(Rules.class);
//
//    QProfile profile = qProfiles.profile("Basic", "xoo");
//
//    // Update rule x1 : update severity and update param value
//    Rule rule1 = rules.find(RuleQuery.builder().searchQuery("x1").build()).results().iterator().next();
//    qProfiles.updateActiveRuleParam(qProfiles.findByProfileAndRule(profile.id(), rule1.id()).activeRuleId(), "acceptWhitespace", "false");
//    qProfiles.activateRule(profileKey(profile), rule1.ruleKey(), "INFO");
//
//    // Disable rule x2
//    Rule rule2 = rules.find(RuleQuery.builder().searchQuery("x2").build()).results().iterator().next();
//    qProfiles.deactivateRule(QualityProfileKey.of(profile.name(), profile.language()), rule2.ruleKey());
//
//    assertThat(qProfileBackup.findDefaultProfileNamesByLanguage("xoo")).hasSize(1);
//
//    // Renamed profile
//    qProfiles.renameProfile(profile.id(), "Old Basic");
//
//    // Restore default profiles of xoo
//    qProfileBackup.recreateBuiltInProfilesByLanguage("xoo");
//
//    // Reload profile
//    profile = qProfiles.profile("Basic", "xoo");
//
//    // Verify rule x1
//    QProfileRule qProfileRule = qProfiles.searchProfileRules(ProfileRuleQuery.create(profile.id()).setNameOrKey("x1"), Paging.create(10, 1)).rules().get(0);
//    assertThat(qProfileRule.severity()).isEqualTo("MAJOR");
//    QProfileRuleParam qProfileRuleParam = qProfileRule.params().get(0);
//    assertThat(qProfileRuleParam.key()).isEqualTo("acceptWhitespace");
//    assertThat(qProfileRuleParam.value()).isEqualTo("true");
//
//    // Verify rule x2
//    assertThat(qProfiles.searchProfileRules(ProfileRuleQuery.create(profile.id()).setNameOrKey("x2"), Paging.create(10, 1)).rules().get(0)).isNotNull();
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

  private QualityProfileKey profileKey(QProfile profile){
    return QualityProfileKey.of(profile.name(), profile.language());
  }
}
