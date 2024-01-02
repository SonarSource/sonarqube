/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference.ValueDifference;
import org.assertj.core.data.MapEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventService;
import org.sonar.server.qualityprofile.QProfileComparison.ActiveRuleDiff;
import org.sonar.server.qualityprofile.QProfileComparison.QProfileComparisonResult;
import org.sonar.server.qualityprofile.builtin.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.util.IntegerTypeValidation;
import org.sonar.server.util.TypeValidations;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class QProfileComparisonIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().anonymous();
  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();

  private DbSession dbSession;
  private QProfileRules qProfileRules;
  private QProfileComparison comparison;

  private RuleDto xooRule1;
  private RuleDto xooRule2;
  private QProfileDto left;
  private QProfileDto right;

  @Before
  public void before() {
    DbClient db = dbTester.getDbClient();
    dbSession = db.openSession(false);
    RuleIndex ruleIndex = new RuleIndex(es.client(), System2.INSTANCE);
    ActiveRuleIndexer activeRuleIndexer = new ActiveRuleIndexer(db, es.client());
    QualityProfileChangeEventService qualityProfileChangeEventService = mock(QualityProfileChangeEventService.class);
    SonarQubeVersion sonarQubeVersion = new SonarQubeVersion(Version.create(10, 3));
    RuleActivator ruleActivator = new RuleActivator(System2.INSTANCE, db, new TypeValidations(singletonList(new IntegerTypeValidation())),
      userSession, mock(Configuration.class), sonarQubeVersion);
    qProfileRules = new QProfileRulesImpl(db, ruleActivator, ruleIndex, activeRuleIndexer, qualityProfileChangeEventService);
    comparison = new QProfileComparison(db);

    xooRule1 = RuleTesting.newXooX1().setSeverity("MINOR");
    xooRule2 = RuleTesting.newXooX2().setSeverity("MAJOR");
    db.ruleDao().insert(dbSession, xooRule1);
    db.ruleDao().insert(dbSession, xooRule2);
    db.ruleDao().insertRuleParam(dbSession, xooRule1, RuleParamDto.createFor(xooRule1)
      .setName("max").setType(RuleParamType.INTEGER.type()));
    db.ruleDao().insertRuleParam(dbSession, xooRule1, RuleParamDto.createFor(xooRule1)
      .setName("min").setType(RuleParamType.INTEGER.type()));

    left = QualityProfileTesting.newQualityProfileDto().setLanguage("xoo");
    right = QualityProfileTesting.newQualityProfileDto().setLanguage("xoo");
    db.qualityProfileDao().insert(dbSession, left, right);

    dbSession.commit();
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void compare_empty_profiles() {
    QProfileComparisonResult result = comparison.compare(dbSession, left, right);
    assertThat(result.left().getKee()).isEqualTo(left.getKee());
    assertThat(result.right().getKee()).isEqualTo(right.getKee());
    assertThat(result.same()).isEmpty();
    assertThat(result.inLeft()).isEmpty();
    assertThat(result.inRight()).isEmpty();
    assertThat(result.modified()).isEmpty();
    assertThat(result.collectRuleKeys()).isEmpty();
  }

  @Test
  public void compare_same() {
    RuleActivation commonActivation = RuleActivation.create(xooRule1.getUuid(), Severity.CRITICAL,
      ImmutableMap.of("min", "7", "max", "42"));
    qProfileRules.activateAndCommit(dbSession, left, singleton(commonActivation));
    qProfileRules.activateAndCommit(dbSession, right, singleton(commonActivation));

    QProfileComparisonResult result = comparison.compare(dbSession, left, right);
    assertThat(result.left().getKee()).isEqualTo(left.getKee());
    assertThat(result.right().getKee()).isEqualTo(right.getKee());
    assertThat(result.same()).isNotEmpty().containsOnlyKeys(xooRule1.getKey());
    assertThat(result.inLeft()).isEmpty();
    assertThat(result.inRight()).isEmpty();
    assertThat(result.modified()).isEmpty();
    assertThat(result.collectRuleKeys()).containsOnly(xooRule1.getKey());
  }

  @Test
  public void compare_only_left() {
    RuleActivation activation = RuleActivation.create(xooRule1.getUuid());
    qProfileRules.activateAndCommit(dbSession, left, singleton(activation));

    QProfileComparisonResult result = comparison.compare(dbSession, left, right);
    assertThat(result.left().getKee()).isEqualTo(left.getKee());
    assertThat(result.right().getKee()).isEqualTo(right.getKee());
    assertThat(result.same()).isEmpty();
    assertThat(result.inLeft()).isNotEmpty().containsOnlyKeys(xooRule1.getKey());
    assertThat(result.inRight()).isEmpty();
    assertThat(result.modified()).isEmpty();
    assertThat(result.collectRuleKeys()).containsOnly(xooRule1.getKey());
  }

  @Test
  public void compare_only_right() {
    qProfileRules.activateAndCommit(dbSession, right, singleton(RuleActivation.create(xooRule1.getUuid())));

    QProfileComparisonResult result = comparison.compare(dbSession, left, right);
    assertThat(result.left().getKee()).isEqualTo(left.getKee());
    assertThat(result.right().getKee()).isEqualTo(right.getKee());
    assertThat(result.same()).isEmpty();
    assertThat(result.inLeft()).isEmpty();
    assertThat(result.inRight()).isNotEmpty().containsOnlyKeys(xooRule1.getKey());
    assertThat(result.modified()).isEmpty();
    assertThat(result.collectRuleKeys()).containsOnly(xooRule1.getKey());
  }

  @Test
  public void compare_disjoint() {
    qProfileRules.activateAndCommit(dbSession, left, singleton(RuleActivation.create(xooRule1.getUuid())));
    qProfileRules.activateAndCommit(dbSession, right, singleton(RuleActivation.create(xooRule2.getUuid())));

    QProfileComparisonResult result = comparison.compare(dbSession, left, right);
    assertThat(result.left().getKee()).isEqualTo(left.getKee());
    assertThat(result.right().getKee()).isEqualTo(right.getKee());
    assertThat(result.same()).isEmpty();
    assertThat(result.inLeft()).isNotEmpty().containsOnlyKeys(xooRule1.getKey());
    assertThat(result.inRight()).isNotEmpty().containsOnlyKeys(xooRule2.getKey());
    assertThat(result.modified()).isEmpty();
    assertThat(result.collectRuleKeys()).containsOnly(xooRule1.getKey(), xooRule2.getKey());
  }

  @Test
  public void compare_modified_severity() {
    qProfileRules.activateAndCommit(dbSession, left, singleton(RuleActivation.create(xooRule1.getUuid(), Severity.CRITICAL, null)));
    qProfileRules.activateAndCommit(dbSession, right, singleton(RuleActivation.create(xooRule1.getUuid(), Severity.BLOCKER, null)));

    QProfileComparisonResult result = comparison.compare(dbSession, left, right);
    assertThat(result.left().getKee()).isEqualTo(left.getKee());
    assertThat(result.right().getKee()).isEqualTo(right.getKee());
    assertThat(result.same()).isEmpty();
    assertThat(result.inLeft()).isEmpty();
    assertThat(result.inRight()).isEmpty();
    assertThat(result.modified()).isNotEmpty().containsOnlyKeys(xooRule1.getKey());
    assertThat(result.collectRuleKeys()).containsOnly(xooRule1.getKey());

    ActiveRuleDiff activeRuleDiff = result.modified().get(xooRule1.getKey());
    assertThat(activeRuleDiff.leftSeverity()).isEqualTo(Severity.CRITICAL);
    assertThat(activeRuleDiff.rightSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(activeRuleDiff.paramDifference().areEqual()).isTrue();
  }

  @Test
  public void compare_modified_param() {
    qProfileRules.activateAndCommit(dbSession, left, singleton(RuleActivation.create(xooRule1.getUuid(), null, ImmutableMap.of("max", "20"))));
    qProfileRules.activateAndCommit(dbSession, right, singleton(RuleActivation.create(xooRule1.getUuid(), null, ImmutableMap.of("max", "30"))));

    QProfileComparisonResult result = comparison.compare(dbSession, left, right);
    assertThat(result.left().getKee()).isEqualTo(left.getKee());
    assertThat(result.right().getKee()).isEqualTo(right.getKee());
    assertThat(result.same()).isEmpty();
    assertThat(result.inLeft()).isEmpty();
    assertThat(result.inRight()).isEmpty();
    assertThat(result.modified()).isNotEmpty().containsOnlyKeys(xooRule1.getKey());
    assertThat(result.collectRuleKeys()).containsOnly(xooRule1.getKey());

    ActiveRuleDiff activeRuleDiff = result.modified().get(xooRule1.getKey());
    assertThat(activeRuleDiff.leftSeverity()).isEqualTo(activeRuleDiff.rightSeverity()).isEqualTo(xooRule1.getSeverityString());
    assertThat(activeRuleDiff.paramDifference().areEqual()).isFalse();
    assertThat(activeRuleDiff.paramDifference().entriesDiffering()).isNotEmpty();
    ValueDifference<String> paramDiff = activeRuleDiff.paramDifference().entriesDiffering().get("max");
    assertThat(paramDiff.leftValue()).isEqualTo("20");
    assertThat(paramDiff.rightValue()).isEqualTo("30");
  }

  @Test
  public void compare_different_params() {
    qProfileRules.activateAndCommit(dbSession, left, singleton(RuleActivation.create(xooRule1.getUuid(), null, ImmutableMap.of("max", "20"))));
    qProfileRules.activateAndCommit(dbSession, right, singleton(RuleActivation.create(xooRule1.getUuid(), null, ImmutableMap.of("min", "5"))));

    QProfileComparisonResult result = comparison.compare(dbSession, left, right);
    assertThat(result.left().getKee()).isEqualTo(left.getKee());
    assertThat(result.right().getKee()).isEqualTo(right.getKee());
    assertThat(result.same()).isEmpty();
    assertThat(result.inLeft()).isEmpty();
    assertThat(result.inRight()).isEmpty();
    assertThat(result.modified()).isNotEmpty().containsOnlyKeys(xooRule1.getKey());
    assertThat(result.collectRuleKeys()).containsOnly(xooRule1.getKey());

    ActiveRuleDiff activeRuleDiff = result.modified().get(xooRule1.getKey());
    assertThat(activeRuleDiff.leftSeverity()).isEqualTo(activeRuleDiff.rightSeverity()).isEqualTo(xooRule1.getSeverityString());
    assertThat(activeRuleDiff.paramDifference().areEqual()).isFalse();
    assertThat(activeRuleDiff.paramDifference().entriesDiffering()).isEmpty();
    assertThat(activeRuleDiff.paramDifference().entriesOnlyOnLeft()).containsExactly(MapEntry.entry("max", "20"));
    assertThat(activeRuleDiff.paramDifference().entriesOnlyOnRight()).containsExactly(MapEntry.entry("min", "5"));
  }
}
