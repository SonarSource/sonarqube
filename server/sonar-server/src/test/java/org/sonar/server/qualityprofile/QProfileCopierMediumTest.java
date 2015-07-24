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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class QProfileCopierMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbClient db;
  DbSession dbSession;
  ActiveRuleIndex index;
  RuleActivator ruleActivator;
  QProfileCopier copier;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    ruleActivator = tester.get(RuleActivator.class);
    index = tester.get(ActiveRuleIndex.class);
    copier = tester.get(QProfileCopier.class);

    // create pre-defined rules
    RuleDto xooRule1 = RuleTesting.newXooX1().setSeverity("MINOR");
    RuleDto xooRule2 = RuleTesting.newXooX2().setSeverity("MAJOR");
    db.deprecatedRuleDao().insert(dbSession, xooRule1, xooRule2);
    db.deprecatedRuleDao().insertRuleParam(dbSession, xooRule1, RuleParamDto.createFor(xooRule1)
      .setName("max").setDefaultValue("10").setType(RuleParamType.INTEGER.type()));

    // create pre-defined profile
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1());
    dbSession.commit();
    dbSession.clearCache();
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void create_target_profile() {
    // source
    RuleActivation activation = new RuleActivation(RuleTesting.XOO_X1);
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(dbSession, activation, QProfileTesting.XOO_P1_KEY);
    dbSession.commit();
    dbSession.clearCache();

    // target does not exist
    copier.copyToName(QProfileTesting.XOO_P1_KEY, QProfileTesting.XOO_P2_NAME.getName());

    verifyOneActiveRule(QProfileTesting.XOO_P2_NAME, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
  }

  @Test
  public void update_target_profile() {
    // source with x1 activated
    RuleActivation activation = new RuleActivation(RuleTesting.XOO_X1);
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(dbSession, activation, QProfileTesting.XOO_P1_KEY);
    dbSession.commit();
    dbSession.clearCache();

    // create target with both x1 and x2 activated
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP2());
    activation = new RuleActivation(RuleTesting.XOO_X1);
    activation.setSeverity(Severity.CRITICAL);
    activation.setParameter("max", "20");
    ruleActivator.activate(dbSession, activation, QProfileTesting.XOO_P2_KEY);
    activation = new RuleActivation(RuleTesting.XOO_X2);
    activation.setSeverity(Severity.CRITICAL);
    ruleActivator.activate(dbSession, activation, QProfileTesting.XOO_P2_KEY);
    dbSession.commit();
    dbSession.clearCache();

    // copy -> reset x1 and deactivate x2
    copier.copyToName(QProfileTesting.XOO_P1_KEY, QProfileTesting.XOO_P2_NAME.getName());

    verifyOneActiveRule(QProfileTesting.XOO_P2_KEY, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
  }

  @Test
  public void create_target_profile_with_same_parent_than_source() {
    // two profiles : parent and its child
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP2().setParentKee(QProfileTesting.XOO_P1_KEY));

    // parent and child with x1 activated
    RuleActivation activation = new RuleActivation(RuleTesting.XOO_X1);
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(dbSession, activation, QProfileTesting.XOO_P1_KEY);
    dbSession.commit();
    dbSession.clearCache();

    // copy child -> profile2 is created with parent P1
    copier.copyToName(QProfileTesting.XOO_P1_KEY, QProfileTesting.XOO_P2_NAME.getName());

    verifyOneActiveRule(QProfileTesting.XOO_P2_KEY, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    QualityProfileDto profile2Dto = db.qualityProfileDao().selectByKey(dbSession, QProfileTesting.XOO_P2_KEY);
    assertThat(profile2Dto.getParentKee()).isEqualTo(QProfileTesting.XOO_P1_KEY);
  }

  @Test
  public void fail_to_copy_on_self() {
    RuleActivation activation = new RuleActivation(RuleTesting.XOO_X1);
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(dbSession, activation, QProfileTesting.XOO_P1_KEY);
    dbSession.commit();
    dbSession.clearCache();

    try {
      copier.copyToName(QProfileTesting.XOO_P1_KEY, QProfileTesting.XOO_P1_NAME.getName());
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Source and target profiles are equal: P1");
    }
  }

  private void verifyOneActiveRule(QProfileName profileName, String expectedSeverity,
    @Nullable String expectedInheritance, Map<String, String> expectedParams) {
    QualityProfileDto dto = db.qualityProfileDao().selectByNameAndLanguage(profileName.getName(), profileName.getLanguage(), dbSession);
    verifyOneActiveRule(dto.getKey(), expectedSeverity, expectedInheritance, expectedParams);
  }

  private void verifyOneActiveRule(String profileKey, String expectedSeverity,
    @Nullable String expectedInheritance, Map<String, String> expectedParams) {

    List<ActiveRule> activeRules = Lists.newArrayList(index.findByProfile(profileKey));
    assertThat(activeRules).hasSize(1);
    ActiveRule activeRule = activeRules.get(0);
    assertThat(activeRule.severity()).isEqualTo(expectedSeverity);
    assertThat(activeRule.inheritance()).isEqualTo(expectedInheritance == null ? ActiveRule.Inheritance.NONE : ActiveRule.Inheritance.valueOf(expectedInheritance));

    // verify parameters
    assertThat(activeRule.params()).hasSize(expectedParams.size());
    for (Map.Entry<String, String> entry : expectedParams.entrySet()) {
      String value = activeRule.params().get(entry.getKey());
      assertThat(value).isEqualTo(entry.getValue());
    }
  }
}
