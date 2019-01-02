/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue.commonrule;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRule;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolderRule;
import org.sonar.core.issue.DefaultIssue;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class CoverageRuleTest {

  private static final String PLUGIN_KEY = "java";
  private static final String QP_KEY = "qp1";

  static ReportComponent FILE = ReportComponent.builder(Component.Type.FILE, 1)
    .setFileAttributes(new FileAttributes(false, "java", 1))
    .build();

  @Rule
  public ActiveRulesHolderRule activeRuleHolder = new ActiveRulesHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.LINE_COVERAGE)
    .add(CoreMetrics.LINES_TO_COVER)
    .add(CoreMetrics.UNCOVERED_LINES)
    .add(CoreMetrics.BRANCH_COVERAGE)
    .add(CoreMetrics.CONDITIONS_TO_COVER)
    .add(CoreMetrics.UNCOVERED_CONDITIONS);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  CommonRule underTest = createRule();

  @Before
  public void setUp() {
    treeRootHolder.setRoot(ReportComponent.DUMB_PROJECT);
  }

  protected abstract CommonRule createRule();

  protected abstract RuleKey getRuleKey();

  protected abstract String getMinPropertyKey();

  protected abstract String getCoverageMetricKey();

  protected abstract String getToCoverMetricKey();

  protected abstract String getUncoveredMetricKey();

  @Test
  public void no_issue_if_enough_coverage() {
    activeRuleHolder.put(new ActiveRule(getRuleKey(), Severity.CRITICAL, ImmutableMap.of(getMinPropertyKey(), "65"), 1_000L, PLUGIN_KEY, QP_KEY));
    measureRepository.addRawMeasure(FILE.getReportAttributes().getRef(), getCoverageMetricKey(), Measure.newMeasureBuilder().create(90.0, 1));

    DefaultIssue issue = underTest.processFile(FILE, "java");

    assertThat(issue).isNull();
  }

  @Test
  public void issue_if_coverage_is_too_low() {
    activeRuleHolder.put(new ActiveRule(getRuleKey(), Severity.CRITICAL, ImmutableMap.of(getMinPropertyKey(), "65"), 1_000L, PLUGIN_KEY, QP_KEY));
    measureRepository.addRawMeasure(FILE.getReportAttributes().getRef(), getCoverageMetricKey(), Measure.newMeasureBuilder().create(20.0, 1));
    measureRepository.addRawMeasure(FILE.getReportAttributes().getRef(), getUncoveredMetricKey(), Measure.newMeasureBuilder().create(40));
    measureRepository.addRawMeasure(FILE.getReportAttributes().getRef(), getToCoverMetricKey(), Measure.newMeasureBuilder().create(50));

    DefaultIssue issue = underTest.processFile(FILE, "java");

    assertThat(issue.ruleKey()).isEqualTo(getRuleKey());
    assertThat(issue.severity()).isEqualTo(Severity.CRITICAL);
    // FIXME explain
    assertThat(issue.gap()).isEqualTo(23.0);
    assertThat(issue.message()).isEqualTo(getExpectedIssueMessage());
  }

  protected abstract String getExpectedIssueMessage();

  @Test
  public void no_issue_if_coverage_is_not_set() {
    activeRuleHolder.put(new ActiveRule(getRuleKey(), Severity.CRITICAL, ImmutableMap.of(getMinPropertyKey(), "65"), 1_000L, PLUGIN_KEY, QP_KEY));

    DefaultIssue issue = underTest.processFile(FILE, "java");

    assertThat(issue).isNull();
  }

  @Test
  public void ignored_if_rule_is_deactivated() {
    // coverage is too low, but rule is not activated
    measureRepository.addRawMeasure(FILE.getReportAttributes().getRef(), getCoverageMetricKey(), Measure.newMeasureBuilder().create(20.0, 1));

    DefaultIssue issue = underTest.processFile(FILE, "java");

    assertThat(issue).isNull();
  }
}
