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

import java.util.Collections;
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
import org.sonar.server.rule.CommonRuleKeys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.DUMB_PROJECT;

public class TestErrorRuleTest {

  private static final String PLUGIN_KEY = "java";
  private static final String QP_KEY = "qp1";

  static RuleKey RULE_KEY = RuleKey.of(CommonRuleKeys.commonRepositoryForLang("java"), CommonRuleKeys.FAILED_UNIT_TESTS);

  static ReportComponent FILE = ReportComponent.builder(Component.Type.FILE, 1)
    .setFileAttributes(new FileAttributes(true, "java", 1))
    .setName("FooTest.java")
    .build();

  @Rule
  public ActiveRulesHolderRule activeRuleHolder = new ActiveRulesHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.TEST_ERRORS)
    .add(CoreMetrics.TEST_FAILURES);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule().setRoot(DUMB_PROJECT);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  CommonRule underTest = new TestErrorRule(activeRuleHolder, measureRepository, metricRepository);

  @Test
  public void issue_if_errors_or_failures() {
    activeRuleHolder.put(new ActiveRule(RULE_KEY, Severity.CRITICAL, Collections.emptyMap(), 1_000L, PLUGIN_KEY, QP_KEY));
    measureRepository.addRawMeasure(FILE.getReportAttributes().getRef(), CoreMetrics.TEST_ERRORS_KEY, Measure.newMeasureBuilder().create(2));
    measureRepository.addRawMeasure(FILE.getReportAttributes().getRef(), CoreMetrics.TEST_FAILURES_KEY, Measure.newMeasureBuilder().create(1));

    DefaultIssue issue = underTest.processFile(FILE, "java");

    assertThat(issue.ruleKey()).isEqualTo(RULE_KEY);
    assertThat(issue.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(issue.gap()).isEqualTo(3.0);
    assertThat(issue.message()).isEqualTo("Fix failing unit tests on file \"FooTest.java\".");
  }

  @Test
  public void no_issues_if_zero_errors_and_failures() {
    activeRuleHolder.put(new ActiveRule(RULE_KEY, Severity.CRITICAL, Collections.emptyMap(), 1_000L, PLUGIN_KEY, QP_KEY));
    measureRepository.addRawMeasure(FILE.getReportAttributes().getRef(), CoreMetrics.TEST_ERRORS_KEY, Measure.newMeasureBuilder().create(0));
    measureRepository.addRawMeasure(FILE.getReportAttributes().getRef(), CoreMetrics.TEST_FAILURES_KEY, Measure.newMeasureBuilder().create(0));

    DefaultIssue issue = underTest.processFile(FILE, "java");

    assertThat(issue).isNull();
  }

  @Test
  public void no_issues_if_test_measures_are_absent() {
    activeRuleHolder.put(new ActiveRule(RULE_KEY, Severity.CRITICAL, Collections.emptyMap(), 1_000L, PLUGIN_KEY, QP_KEY));

    DefaultIssue issue = underTest.processFile(FILE, "java");

    assertThat(issue).isNull();
  }
}
