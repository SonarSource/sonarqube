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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.tester.ServerTester;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class QProfileCopierMediumTest {

  static final QualityProfileKey XOO_PROFILE_1 = QualityProfileKey.of("P1", "xoo");
  static final QualityProfileKey XOO_CHILD_1 = QualityProfileKey.of("P1CHILD", "xoo");
  static final QualityProfileKey XOO_PROFILE_2 = QualityProfileKey.of("P2", "xoo");
  static final RuleKey XOO_RULE_1 = RuleKey.of("xoo", "x1");
  static final RuleKey XOO_RULE_2 = RuleKey.of("xoo", "x2");

  @ClassRule
  public static ServerTester tester = new ServerTester();

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
    RuleDto xooRule1 = RuleTesting.newDto(XOO_RULE_1)
      .setSeverity("MINOR").setLanguage("xoo");
    RuleDto xooRule2 = RuleTesting.newDto(XOO_RULE_2)
      .setSeverity("MAJOR").setLanguage("xoo");
    db.ruleDao().insert(dbSession, xooRule1, xooRule2);
    db.ruleDao().addRuleParam(dbSession, xooRule1, RuleParamDto.createFor(xooRule1)
      .setName("max").setDefaultValue("10").setType(RuleParamType.INTEGER.type()));

    // create pre-defined profile
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_PROFILE_1));
    dbSession.commit();
    dbSession.clearCache();
  }

  @After
  public void after() throws Exception {
    dbSession.close();
  }

  @Test
  public void create_target_profile() throws Exception {
    // source
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_1, XOO_RULE_1));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);

    // target does not exist
    copier.copy(XOO_PROFILE_1, XOO_PROFILE_2);

    verifyOneActiveRule(XOO_PROFILE_2, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
  }

  @Test
  public void update_target_profile() throws Exception {
    // source with x1 activated
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_1, XOO_RULE_1));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);

    // create target with both x1 and x2 activated
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_PROFILE_2));
    activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_2, XOO_RULE_1));
    activation.setSeverity(Severity.CRITICAL);
    activation.setParameter("max", "20");
    ruleActivator.activate(dbSession, activation);
    dbSession.commit();
    activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_2, XOO_RULE_2));
    activation.setSeverity(Severity.CRITICAL);
    ruleActivator.activate(dbSession, activation);
    dbSession.commit();
    dbSession.clearCache();

    // copy -> reset x1 and deactivate x2
    copier.copy(XOO_PROFILE_1, XOO_PROFILE_2);

    verifyOneActiveRule(XOO_PROFILE_2, Severity.BLOCKER, null, ImmutableMap.of("max", "7"));
  }

  @Test
  public void create_target_profile_with_same_parent_than_source() throws Exception {
    // two profiles : parent and its child
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_CHILD_1)
      .setParent(XOO_PROFILE_1.name()));
    dbSession.commit();

    // parent and child with x1 activated
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_1, XOO_RULE_1));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);
    dbSession.clearCache();

    // copy child -> profile2 is created with parent P1
    copier.copy(XOO_CHILD_1, XOO_PROFILE_2);

    verifyOneActiveRule(XOO_PROFILE_2, Severity.BLOCKER, ActiveRuleDto.INHERITED, ImmutableMap.of("max", "7"));
    QualityProfileDto profile2Dto = db.qualityProfileDao().getByKey(dbSession, XOO_PROFILE_2);
    assertThat(profile2Dto.getParent()).isEqualTo(XOO_PROFILE_1.name());
  }

  @Test
  public void fail_to_copy_on_self() throws Exception {
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_1, XOO_RULE_1));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);

    try {
      copier.copy(XOO_PROFILE_1, XOO_PROFILE_1);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Source and target profiles are equal: P1:xoo");
    }
  }

  @Test
  public void fail_to_copy_on_different_language() throws Exception {
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_1, XOO_RULE_1));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    ruleActivator.activate(activation);

    try {
      copier.copy(XOO_PROFILE_1, QualityProfileKey.of("NEW", "java"));
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Source and target profiles do not have the same language: P1:xoo and NEW:java");
    }
  }

  private void verifyOneActiveRule(QualityProfileKey profileKey, String expectedSeverity,
                                   @Nullable String expectedInheritance, Map<String, String> expectedParams) {

    List<ActiveRule> activeRules = index.findByProfile(profileKey);
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
