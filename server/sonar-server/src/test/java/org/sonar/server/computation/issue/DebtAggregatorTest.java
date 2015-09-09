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
package org.sonar.server.computation.issue;

import com.google.common.base.Optional;
import javax.annotation.CheckForNull;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.debt.CharacteristicImpl;
import org.sonar.server.computation.debt.DebtModelHolderRule;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.MetricRepositoryRule;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;

public class DebtAggregatorTest {

  /**
   * Root characteristic
   */
  static final int PORTABILITY_ID = 1000;

  /**
   * Sub-characteristic of {@link #PORTABILITY_ID}
   */
  static final int PORTABILITY_SOFT_ID = 1001;

  /**
   * Sub-characteristic of {@link #PORTABILITY_ID}
   */
  static final int PORTABILITY_HARD_ID = 1002;

  /**
   * Root characteristic
   */
  static final int RELIABILITY_ID = 1003;

  /**
   * Sub-characteristic of {@link #RELIABILITY_ID}
   */
  static final int DATA_RELIABILITY_ID = 1004;

  static final Component FILE = ReportComponent.builder(Component.Type.FILE, 1).build();
  static final Component PROJECT = ReportComponent.builder(Component.Type.PROJECT, 2).addChildren(FILE).build();

  static final DumbRule RULE = new DumbRule(RuleTesting.XOO_X1).setId(100).setSubCharacteristicId(PORTABILITY_SOFT_ID);

  @org.junit.Rule
  public RuleRepositoryRule ruleRepository = new RuleRepositoryRule().add(RULE);

  @org.junit.Rule
  public DebtModelHolderRule debtModelHolder = new DebtModelHolderRule()
    .addCharacteristics(
      new CharacteristicImpl(PORTABILITY_ID, "PORTABILITY", null),
      asList(new CharacteristicImpl(PORTABILITY_SOFT_ID, "PORTABILITY_HARDWARE", PORTABILITY_ID),
        new CharacteristicImpl(PORTABILITY_HARD_ID, "PORTABILITY_SOFTWARE", PORTABILITY_ID)))
    .addCharacteristics(new CharacteristicImpl(RELIABILITY_ID, "RELIABILITY", null),
      asList(new CharacteristicImpl(DATA_RELIABILITY_ID, "DATA_RELIABILITY", RELIABILITY_ID))
    );

  @org.junit.Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule().add(200, CoreMetrics.TECHNICAL_DEBT);

  @org.junit.Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(PROJECT, metricRepository);

  DebtAggregator underTest = new DebtAggregator(ruleRepository, debtModelHolder, metricRepository, measureRepository);

  @Test
  public void sum_debt_of_unresolved_issues() {
    DefaultIssue unresolved1 = newIssue(10);
    DefaultIssue unresolved2 = newIssue(30);
    DefaultIssue unresolvedWithoutDebt = newIssue();
    DefaultIssue resolved = newIssue(50).setResolution(RESOLUTION_FIXED);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, unresolvedWithoutDebt);
    underTest.onIssue(FILE, resolved);
    underTest.afterComponent(FILE);

    // total debt
    assertThat(debtMeasure(FILE).get().getLongValue()).isEqualTo(10 + 30);

    // debt by rule
    assertThat(debtRuleMeasure(FILE, RULE.getId()).get().getLongValue()).isEqualTo(10 + 30);

    // debt by characteristic. Root characteristics with zero values are not saved for files.
    assertThat(debtCharacteristicMeasure(FILE, PORTABILITY_ID).get().getLongValue()).isEqualTo(10 + 30);
    assertThat(debtCharacteristicMeasure(FILE, PORTABILITY_SOFT_ID).get().getLongValue()).isEqualTo(10 + 30);
    assertThat(debtCharacteristicMeasure(FILE, PORTABILITY_HARD_ID)).isAbsent();
    assertThat(debtCharacteristicMeasure(FILE, RELIABILITY_ID)).isAbsent();
  }

  @Test
  public void aggregate_debt_of_children() {
    DefaultIssue fileIssue = newIssue(10);
    DefaultIssue projectIssue = newIssue(30);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, fileIssue);
    underTest.afterComponent(FILE);
    underTest.beforeComponent(PROJECT);
    underTest.onIssue(PROJECT, projectIssue);
    underTest.afterComponent(PROJECT);

    // total debt of project
    assertThat(debtMeasure(PROJECT).get().getLongValue()).isEqualTo(10 + 30);

    // debt by rule
    assertThat(debtRuleMeasure(PROJECT, RULE.getId()).get().getLongValue()).isEqualTo(10 + 30);

    // debt by characteristic. Root characteristics with zero values are stored for modules and projects.
    assertThat(debtCharacteristicMeasure(PROJECT, PORTABILITY_ID).get().getLongValue()).isEqualTo(10 + 30);
    assertThat(debtCharacteristicMeasure(PROJECT, PORTABILITY_SOFT_ID).get().getLongValue()).isEqualTo(10 + 30);
    assertThat(debtCharacteristicMeasure(PROJECT, PORTABILITY_HARD_ID)).isAbsent();
    assertThat(debtCharacteristicMeasure(PROJECT, RELIABILITY_ID).get().getLongValue()).isZero();
  }

  @Test
  public void sum_debt_of_issues_without_debt() {
    DefaultIssue fileIssue = newIssue();
    DefaultIssue projectIssue = newIssue();

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, fileIssue);
    underTest.afterComponent(FILE);
    underTest.beforeComponent(PROJECT);
    underTest.onIssue(PROJECT, projectIssue);
    underTest.afterComponent(PROJECT);

    // total debt of project
    assertThat(debtMeasure(PROJECT).get().getLongValue()).isZero();

    // debt by rule
    assertThat(debtRuleMeasure(PROJECT, RULE.getId())).isAbsent();

    // debt by characteristic. Root characteristics with zero values are stored for modules and projects.
    assertThat(debtCharacteristicMeasure(PROJECT, PORTABILITY_ID).get().getLongValue()).isZero();
    assertThat(debtCharacteristicMeasure(PROJECT, PORTABILITY_SOFT_ID)).isAbsent();
    assertThat(debtCharacteristicMeasure(PROJECT, PORTABILITY_HARD_ID)).isAbsent();
    assertThat(debtCharacteristicMeasure(PROJECT, RELIABILITY_ID).get().getLongValue()).isZero();
  }

  @CheckForNull
  private Optional<Measure> debtMeasure(Component component) {
    return measureRepository.getAddedRawMeasure(component, TECHNICAL_DEBT_KEY);
  }

  @CheckForNull
  private Optional<Measure> debtRuleMeasure(Component component, int ruleId) {
    return measureRepository.getAddedRawRuleMeasure(component, TECHNICAL_DEBT_KEY, ruleId);
  }

  @CheckForNull
  private Optional<Measure> debtCharacteristicMeasure(Component component, int characteristicId) {
    return measureRepository.getAddedRawCharacteristicMeasure(component, TECHNICAL_DEBT_KEY, characteristicId);
  }

  private static DefaultIssue newIssue(long debt){
    return newIssue().setDebt(Duration.create(debt));
  }

  private static DefaultIssue newIssue(){
    return new DefaultIssue().setRuleKey(RULE.getKey());
  }
}
