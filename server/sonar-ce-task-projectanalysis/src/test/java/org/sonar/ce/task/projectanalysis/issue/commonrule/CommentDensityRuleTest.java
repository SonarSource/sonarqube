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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

public class CommentDensityRuleTest {

  private static final String PLUGIN_KEY = "java";
  private static final String QP_KEY = "qp1";

  static RuleKey RULE_KEY = RuleKey.of(CommonRuleKeys.commonRepositoryForLang(PLUGIN_KEY), CommonRuleKeys.INSUFFICIENT_COMMENT_DENSITY);

  static ReportComponent FILE = ReportComponent.builder(Component.Type.FILE, 1)
    .setFileAttributes(new FileAttributes(false, PLUGIN_KEY, 1))
    .build();

  static ReportComponent TEST_FILE = ReportComponent.builder(Component.Type.FILE, 1)
    .setFileAttributes(new FileAttributes(true, PLUGIN_KEY, 1))
    .build();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public ActiveRulesHolderRule activeRuleHolder = new ActiveRulesHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(CoreMetrics.COMMENT_LINES_DENSITY)
    .add(CoreMetrics.COMMENT_LINES)
    .add(CoreMetrics.NCLOC);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule().setRoot(DUMB_PROJECT);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  CommentDensityRule underTest = new CommentDensityRule(activeRuleHolder, measureRepository, metricRepository);

  @Test
  public void no_issues_if_enough_comments() {
    activeRuleHolder.put(new ActiveRule(RULE_KEY, Severity.CRITICAL, ImmutableMap.of(CommonRuleKeys.INSUFFICIENT_COMMENT_DENSITY_PROPERTY, "25"), 1_000L, PLUGIN_KEY, QP_KEY));
    measureRepository.addRawMeasure(FILE.getReportAttributes().getRef(), CoreMetrics.COMMENT_LINES_DENSITY_KEY, Measure.newMeasureBuilder().create(90.0, 1));

    DefaultIssue issue = underTest.processFile(FILE, PLUGIN_KEY);

    assertThat(issue).isNull();
  }

  @Test
  public void issue_if_not_enough_comments() {
    prepareForIssue("25", FILE, 10.0, 40, 360);

    DefaultIssue issue = underTest.processFile(FILE, PLUGIN_KEY);

    assertThat(issue.ruleKey()).isEqualTo(RULE_KEY);
    assertThat(issue.severity()).isEqualTo(Severity.CRITICAL);
    // min_comments = (min_percent * ncloc) / (1 - min_percent)
    // -> threshold of 25% for 360 ncloc is 120 comment lines. 40 are already written.
    assertThat(issue.gap()).isEqualTo(120.0 - 40.0);
    assertThat(issue.message()).isEqualTo("80 more comment lines need to be written to reach the minimum threshold of 25.0% comment density.");
  }

  @Test
  public void no_issues_on_tests() {
    prepareForIssue("25", TEST_FILE, 10.0, 40, 360);

    DefaultIssue issue = underTest.processFile(TEST_FILE, PLUGIN_KEY);

    assertThat(issue).isNull();
  }

  @Test
  public void issue_if_not_enough_comments__test_ceil() {
    prepareForIssue("25", FILE, 0.0, 0, 1);

    DefaultIssue issue = underTest.processFile(FILE, PLUGIN_KEY);

    assertThat(issue.ruleKey()).isEqualTo(RULE_KEY);
    assertThat(issue.severity()).isEqualTo(Severity.CRITICAL);
    // 1 ncloc requires 1 comment line to reach 25% of comment density
    assertThat(issue.gap()).isEqualTo(1.0);
    assertThat(issue.message()).isEqualTo("1 more comment lines need to be written to reach the minimum threshold of 25.0% comment density.");
  }

  /**
   * SQALE-110
   */
  @Test
  public void fail_if_min_density_is_100() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Minimum density of rule [common-java:InsufficientCommentDensity] is incorrect. Got [100] but must be strictly less than 100.");

    prepareForIssue("100", FILE, 0.0, 0, 1);

    underTest.processFile(FILE, PLUGIN_KEY);
  }

  private void prepareForIssue(String minDensity, ReportComponent file, double commentLineDensity, int commentLines, int ncloc) {
    activeRuleHolder.put(new ActiveRule(RULE_KEY, Severity.CRITICAL, ImmutableMap.of(CommonRuleKeys.INSUFFICIENT_COMMENT_DENSITY_PROPERTY, minDensity), 1_000L, PLUGIN_KEY, QP_KEY));
    measureRepository.addRawMeasure(file.getReportAttributes().getRef(), CoreMetrics.COMMENT_LINES_DENSITY_KEY, Measure.newMeasureBuilder().create(commentLineDensity, 1));
    measureRepository.addRawMeasure(file.getReportAttributes().getRef(), CoreMetrics.COMMENT_LINES_KEY, Measure.newMeasureBuilder().create(commentLines));
    measureRepository.addRawMeasure(file.getReportAttributes().getRef(), CoreMetrics.NCLOC_KEY, Measure.newMeasureBuilder().create(ncloc));
  }


}
