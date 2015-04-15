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

import com.google.common.collect.MapDifference.ValueDifference;
import org.assertj.core.data.MapEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.QProfileComparison.ActiveRuleDiff;
import org.sonar.server.qualityprofile.QProfileComparison.QProfileComparisonResult;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.tester.ServerTester;

import static org.assertj.core.api.Assertions.assertThat;

public class QProfileComparisonMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().addXoo();

  DbClient db;
  DbSession dbSession;
  ActiveRuleIndex index;
  RuleActivator ruleActivator;
  QProfileComparison comparison;

  RuleDto xooRule1;
  RuleDto xooRule2;
  QualityProfileDto left;
  QualityProfileDto right;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    ruleActivator = tester.get(RuleActivator.class);
    comparison = tester.get(QProfileComparison.class);

    xooRule1 = RuleTesting.newXooX1().setSeverity("MINOR");
    xooRule2 = RuleTesting.newXooX2().setSeverity("MAJOR");
    db.ruleDao().insert(dbSession, xooRule1, xooRule2);
    db.ruleDao().addRuleParam(dbSession, xooRule1, RuleParamDto.createFor(xooRule1)
      .setName("max").setType(RuleParamType.INTEGER.type()));
    db.ruleDao().addRuleParam(dbSession, xooRule1, RuleParamDto.createFor(xooRule1)
      .setName("min").setType(RuleParamType.INTEGER.type()));

    left = QProfileTesting.newXooP1();
    right = QProfileTesting.newXooP2();
    db.qualityProfileDao().insert(dbSession, left, right);
    dbSession.commit();
    dbSession.clearCache();
  }

  @After
  public void after() throws Exception {
    dbSession.close();
  }

  @Test
  public void compare_empty_profiles() throws Exception {
    QProfileComparisonResult result = comparison.compare(left.getKey(), right.getKey());
    assertThat(result.left().getKey()).isEqualTo(left.getKey());
    assertThat(result.right().getKey()).isEqualTo(right.getKey());
    assertThat(result.same()).isEmpty();
    assertThat(result.inLeft()).isEmpty();
    assertThat(result.inRight()).isEmpty();
    assertThat(result.modified()).isEmpty();
    assertThat(result.collectRuleKeys()).isEmpty();
  }

  @Test
  public void compare_same() throws Exception {
    RuleActivation commonActivation = new RuleActivation(xooRule1.getKey())
      .setSeverity(Severity.CRITICAL)
      .setParameter("min", "7")
      .setParameter("max", "42");
    ruleActivator.activate(dbSession, commonActivation, left);
    ruleActivator.activate(dbSession, commonActivation, right);
    dbSession.commit();

    QProfileComparisonResult result = comparison.compare(left.getKey(), right.getKey());
    assertThat(result.left().getKey()).isEqualTo(left.getKey());
    assertThat(result.right().getKey()).isEqualTo(right.getKey());
    assertThat(result.same()).isNotEmpty().containsOnlyKeys(xooRule1.getKey());
    assertThat(result.inLeft()).isEmpty();
    assertThat(result.inRight()).isEmpty();
    assertThat(result.modified()).isEmpty();
    assertThat(result.collectRuleKeys()).containsOnly(xooRule1.getKey());
  }

  @Test
  public void compare_only_left() throws Exception {
    ruleActivator.activate(dbSession, new RuleActivation(xooRule1.getKey()), left);
    dbSession.commit();

    QProfileComparisonResult result = comparison.compare(left.getKey(), right.getKey());
    assertThat(result.left().getKey()).isEqualTo(left.getKey());
    assertThat(result.right().getKey()).isEqualTo(right.getKey());
    assertThat(result.same()).isEmpty();
    assertThat(result.inLeft()).isNotEmpty().containsOnlyKeys(xooRule1.getKey());
    assertThat(result.inRight()).isEmpty();
    assertThat(result.modified()).isEmpty();
    assertThat(result.collectRuleKeys()).containsOnly(xooRule1.getKey());
  }

  @Test
  public void compare_only_right() throws Exception {
    ruleActivator.activate(dbSession, new RuleActivation(xooRule1.getKey()), right);
    dbSession.commit();

    QProfileComparisonResult result = comparison.compare(left.getKey(), right.getKey());
    assertThat(result.left().getKey()).isEqualTo(left.getKey());
    assertThat(result.right().getKey()).isEqualTo(right.getKey());
    assertThat(result.same()).isEmpty();
    assertThat(result.inLeft()).isEmpty();
    assertThat(result.inRight()).isNotEmpty().containsOnlyKeys(xooRule1.getKey());
    assertThat(result.modified()).isEmpty();
    assertThat(result.collectRuleKeys()).containsOnly(xooRule1.getKey());
  }

  @Test
  public void compare_disjoint() throws Exception {
    ruleActivator.activate(dbSession, new RuleActivation(xooRule1.getKey()), left);
    ruleActivator.activate(dbSession, new RuleActivation(xooRule2.getKey()), right);
    dbSession.commit();

    QProfileComparisonResult result = comparison.compare(left.getKey(), right.getKey());
    assertThat(result.left().getKey()).isEqualTo(left.getKey());
    assertThat(result.right().getKey()).isEqualTo(right.getKey());
    assertThat(result.same()).isEmpty();
    assertThat(result.inLeft()).isNotEmpty().containsOnlyKeys(xooRule1.getKey());
    assertThat(result.inRight()).isNotEmpty().containsOnlyKeys(xooRule2.getKey());
    assertThat(result.modified()).isEmpty();
    assertThat(result.collectRuleKeys()).containsOnly(xooRule1.getKey(), xooRule2.getKey());
  }

  @Test
  public void compare_modified_severity() throws Exception {
    ruleActivator.activate(dbSession, new RuleActivation(xooRule1.getKey()).setSeverity(Severity.CRITICAL), left);
    ruleActivator.activate(dbSession, new RuleActivation(xooRule1.getKey()).setSeverity(Severity.BLOCKER), right);
    dbSession.commit();

    QProfileComparisonResult result = comparison.compare(left.getKey(), right.getKey());
    assertThat(result.left().getKey()).isEqualTo(left.getKey());
    assertThat(result.right().getKey()).isEqualTo(right.getKey());
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
  public void compare_modified_param() throws Exception {
    ruleActivator.activate(dbSession, new RuleActivation(xooRule1.getKey()).setParameter("max", "20"), left);
    ruleActivator.activate(dbSession, new RuleActivation(xooRule1.getKey()).setParameter("max", "30"), right);
    dbSession.commit();

    QProfileComparisonResult result = comparison.compare(left.getKey(), right.getKey());
    assertThat(result.left().getKey()).isEqualTo(left.getKey());
    assertThat(result.right().getKey()).isEqualTo(right.getKey());
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
  public void compare_different_params() throws Exception {
    ruleActivator.activate(dbSession, new RuleActivation(xooRule1.getKey()).setParameter("max", "20"), left);
    ruleActivator.activate(dbSession, new RuleActivation(xooRule1.getKey()).setParameter("min", "5"), right);
    dbSession.commit();

    QProfileComparisonResult result = comparison.compare(left.getKey(), right.getKey());
    assertThat(result.left().getKey()).isEqualTo(left.getKey());
    assertThat(result.right().getKey()).isEqualTo(right.getKey());
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

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_unknown_left() throws Exception {
    comparison.compare("polop", right.getKey());
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_unknown_right() throws Exception {
    comparison.compare(left.getKey(), "polop");
  }
}
