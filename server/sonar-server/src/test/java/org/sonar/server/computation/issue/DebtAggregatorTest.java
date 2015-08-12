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
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.debt.Characteristic;
import org.sonar.server.computation.debt.DebtModelHolderImpl;
import org.sonar.server.computation.debt.MutableDebtModelHolder;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.db.rule.RuleTesting;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;

public class DebtAggregatorTest {

  /**
   * Root characteristic
   */
  public static final int PORTABILITY_ID = 1000;

  /**
   * Sub-characteristic of {@link #PORTABILITY_ID}
   */
  public static final int PORTABILITY_SOFT_ID = 1001;

  /**
   * Sub-characteristic of {@link #PORTABILITY_ID}
   */
  public static final int PORTABILITY_HARD_ID = 1002;

  /**
   * Root characteristic
   */
  public static final int RELIABILITY_ID = 1003;

  Component file = ReportComponent.builder(Component.Type.FILE, 1).build();
  Component project = ReportComponent.builder(Component.Type.PROJECT, 2).addChildren(file).build();

  DumbRule rule = new DumbRule(RuleTesting.XOO_X1).setId(100).setSubCharacteristicId(PORTABILITY_SOFT_ID);

  @org.junit.Rule
  public RuleRepositoryRule ruleRepository = new RuleRepositoryRule().add(rule);

  MutableDebtModelHolder debtModelHolder = new DebtModelHolderImpl()
    .addCharacteristics(new Characteristic(PORTABILITY_ID, "PORTABILITY", null),
      asList(new Characteristic(PORTABILITY_SOFT_ID, "PORTABILITY_HARDWARE", PORTABILITY_ID), new Characteristic(PORTABILITY_HARD_ID, "PORTABILITY_SOFTWARE", PORTABILITY_ID)))
    .addCharacteristics(new Characteristic(RELIABILITY_ID, "RELIABILITY", null),
      asList(new Characteristic(1004, "DATA_RELIABILITY", RELIABILITY_ID))
    );

  @org.junit.Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule().add(200, CoreMetrics.TECHNICAL_DEBT);

  @org.junit.Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create();

  DebtAggregator underTest = new DebtAggregator(ruleRepository, debtModelHolder, metricRepository, measureRepository);

  @Test
  public void sum_debt_of_unresolved_issues() {
    DefaultIssue unresolved1 = new DefaultIssue().setDebt(Duration.create(10)).setRuleKey(rule.getKey());
    DefaultIssue unresolved2 = new DefaultIssue().setDebt(Duration.create(30)).setRuleKey(rule.getKey());
    DefaultIssue unresolvedWithoutDebt = new DefaultIssue().setRuleKey(rule.getKey());
    DefaultIssue resolved = new DefaultIssue().setDebt(Duration.create(50)).setResolution(RESOLUTION_FIXED).setRuleKey(rule.getKey());

    underTest.beforeComponent(file);
    underTest.onIssue(file, unresolved1);
    underTest.onIssue(file, unresolved2);
    underTest.onIssue(file, unresolvedWithoutDebt);
    underTest.onIssue(file, resolved);
    underTest.afterComponent(file);

    // total debt
    assertThat(debtMeasure(file).get().getLongValue()).isEqualTo(10 + 30);

    // debt by rule
    assertThat(debtRuleMeasure(file, rule.getId()).get().getLongValue()).isEqualTo(10 + 30);

    // debt by characteristic. Root characteristics with zero values are not saved for files.
    assertThat(debtCharacteristicMeasure(file, PORTABILITY_ID).get().getLongValue()).isEqualTo(10 + 30);
    assertThat(debtCharacteristicMeasure(file, PORTABILITY_SOFT_ID).get().getLongValue()).isEqualTo(10 + 30);
    assertThat(debtCharacteristicMeasure(file, PORTABILITY_HARD_ID).isPresent()).isFalse();
    assertThat(debtCharacteristicMeasure(file, RELIABILITY_ID).isPresent()).isFalse();
  }

  @Test
  public void aggregate_debt_of_children() {
    DefaultIssue fileIssue = new DefaultIssue().setDebt(Duration.create(10)).setRuleKey(rule.getKey());
    DefaultIssue projectIssue = new DefaultIssue().setDebt(Duration.create(30)).setRuleKey(rule.getKey());

    underTest.beforeComponent(file);
    underTest.onIssue(file, fileIssue);
    underTest.afterComponent(file);
    underTest.beforeComponent(project);
    underTest.onIssue(project, projectIssue);
    underTest.afterComponent(project);

    // total debt of project
    assertThat(debtMeasure(project).get().getLongValue()).isEqualTo(10 + 30);

    // debt by rule
    assertThat(debtRuleMeasure(project, rule.getId()).get().getLongValue()).isEqualTo(10 + 30);

    // debt by characteristic. Root characteristics with zero values are stored for modules and projects.
    assertThat(debtCharacteristicMeasure(project, PORTABILITY_ID).get().getLongValue()).isEqualTo(10 + 30);
    assertThat(debtCharacteristicMeasure(project, PORTABILITY_SOFT_ID).get().getLongValue()).isEqualTo(10 + 30);
    assertThat(debtCharacteristicMeasure(project, PORTABILITY_HARD_ID).isPresent()).isFalse();
    assertThat(debtCharacteristicMeasure(project, RELIABILITY_ID).get().getLongValue()).isZero();
  }

  @CheckForNull
  private Optional<Measure> debtMeasure(Component component) {
    return measureRepository.getRawMeasure(component, metricRepository.getByKey(CoreMetrics.TECHNICAL_DEBT_KEY));
  }

  @CheckForNull
  private Optional<Measure> debtRuleMeasure(Component component, int ruleId) {
    return measureRepository.getRawRuleMeasure(component, metricRepository.getByKey(CoreMetrics.TECHNICAL_DEBT_KEY), ruleId);
  }

  @CheckForNull
  private Optional<Measure> debtCharacteristicMeasure(Component component, int characteristicId) {
    return measureRepository.getRawCharacteristicMeasure(component, metricRepository.getByKey(CoreMetrics.TECHNICAL_DEBT_KEY), characteristicId);
  }
}
