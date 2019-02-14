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
package org.sonar.ce.task.projectanalysis.qualitymodel;

import java.util.Arrays;
import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.ce.task.projectanalysis.issue.ComponentIssuesRepositoryRule;
import org.sonar.ce.task.projectanalysis.issue.FillComponentIssuesVisitorRule;
import org.sonar.ce.task.projectanalysis.measure.MeasureAssert;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderRule;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.Uuids;
import org.sonar.db.component.BranchType;
import org.sonar.server.measure.Rating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.INFO;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.server.measure.Rating.A;
import static org.sonar.server.measure.Rating.B;
import static org.sonar.server.measure.Rating.C;
import static org.sonar.server.measure.Rating.D;
import static org.sonar.server.measure.Rating.E;

public class NewReliabilityAndSecurityRatingMeasuresVisitorTest {

  private static final long LEAK_PERIOD_SNAPSHOT_IN_MILLISEC = 12323l;
  private static final Date DEFAULT_ISSUE_CREATION_DATE = new Date(1000l);
  private static final Date BEFORE_LEAK_PERIOD_DATE = new Date(LEAK_PERIOD_SNAPSHOT_IN_MILLISEC - 5000L);
  private static final Date AFTER_LEAK_PERIOD_DATE = new Date(LEAK_PERIOD_SNAPSHOT_IN_MILLISEC + 5000L);

  static final String LANGUAGE_KEY_1 = "lKey1";

  static final int PROJECT_REF = 1;
  static final int DIR_REF = 12;
  static final int DIRECTORY_REF = 123;
  static final int FILE_1_REF = 1231;
  static final int FILE_2_REF = 1232;

  static final Component ROOT_PROJECT = builder(Component.Type.PROJECT, PROJECT_REF).setKey("project")
    .addChildren(
      builder(DIRECTORY, DIR_REF).setKey("dir")
        .addChildren(
          builder(DIRECTORY, DIRECTORY_REF).setKey("directory")
            .addChildren(
              builder(FILE, FILE_1_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_KEY_1, 1)).setKey("file1").build(),
              builder(FILE, FILE_2_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_KEY_1, 1)).setKey("file2").build())
            .build())
        .build())
    .build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NEW_SECURITY_RATING)
    .add(NEW_RELIABILITY_RATING);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  @Rule
  public PeriodHolderRule periodsHolder = new PeriodHolderRule().setPeriod(new Period("mode", null, LEAK_PERIOD_SNAPSHOT_IN_MILLISEC, "UUID"));

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public ComponentIssuesRepositoryRule componentIssuesRepositoryRule = new ComponentIssuesRepositoryRule(treeRootHolder);

  @Rule
  public FillComponentIssuesVisitorRule fillComponentIssuesVisitorRule = new FillComponentIssuesVisitorRule(componentIssuesRepositoryRule, treeRootHolder);

  private VisitorsCrawler underTest = new VisitorsCrawler(Arrays.asList(fillComponentIssuesVisitorRule,
    new NewReliabilityAndSecurityRatingMeasuresVisitor(metricRepository, measureRepository, componentIssuesRepositoryRule,
      periodsHolder, analysisMetadataHolder)));

  @Test
  public void measures_created_for_project_are_all_A_when_they_have_no_FILE_child() {
    ReportComponent root = builder(PROJECT, 1).build();
    treeRootHolder.setRoot(root);

    underTest.visit(root);

    verifyAddedRawMeasureOnLeakPeriod(1, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(1, NEW_RELIABILITY_RATING_KEY, A);
  }

  @Test
  public void no_measure_if_there_is_no_period() {
    periodsHolder.setPeriod(null);
    treeRootHolder.setRoot(builder(PROJECT, 1).build());

    underTest.visit(treeRootHolder.getRoot());

    assertThat(measureRepository.getAddedRawMeasures(1).values()).isEmpty();
  }

  @Test
  public void compute_new_security_rating() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newVulnerabilityIssue(10L, MAJOR).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newVulnerabilityIssue(1L, MAJOR).setCreationDate(BEFORE_LEAK_PERIOD_DATE),
      newBugIssue(1L, MAJOR).setCreationDate(AFTER_LEAK_PERIOD_DATE));
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newVulnerabilityIssue(2L, CRITICAL).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newVulnerabilityIssue(3L, MINOR).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newVulnerabilityIssue(10L, BLOCKER).setCreationDate(AFTER_LEAK_PERIOD_DATE).setResolution(RESOLUTION_FIXED));
    fillComponentIssuesVisitorRule.setIssues(DIR_REF,
      newVulnerabilityIssue(7L, BLOCKER).setCreationDate(AFTER_LEAK_PERIOD_DATE));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_SECURITY_RATING_KEY, C);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_SECURITY_RATING_KEY, D);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_SECURITY_RATING_KEY, D);
    verifyAddedRawMeasureOnLeakPeriod(DIR_REF, NEW_SECURITY_RATING_KEY, E);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SECURITY_RATING_KEY, E);
  }

  @Test
  public void compute_new_security_rating_to_A_when_no_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF);

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(DIR_REF, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SECURITY_RATING_KEY, A);
  }

  @Test
  public void compute_new_security_rating_to_A_when_no_new_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newVulnerabilityIssue(1L, MAJOR).setCreationDate(BEFORE_LEAK_PERIOD_DATE));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(DIR_REF, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SECURITY_RATING_KEY, A);
  }

  @Test
  public void compute_new_reliability_rating() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newBugIssue(10L, MAJOR).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newBugIssue(1L, MAJOR).setCreationDate(BEFORE_LEAK_PERIOD_DATE),
      newVulnerabilityIssue(1L, MAJOR).setCreationDate(AFTER_LEAK_PERIOD_DATE));
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newBugIssue(2L, CRITICAL).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newBugIssue(3L, MINOR).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newBugIssue(10L, BLOCKER).setCreationDate(AFTER_LEAK_PERIOD_DATE).setResolution(RESOLUTION_FIXED));
    fillComponentIssuesVisitorRule.setIssues(DIR_REF,
      newBugIssue(7L, BLOCKER).setCreationDate(AFTER_LEAK_PERIOD_DATE));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_RELIABILITY_RATING_KEY, C);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_RELIABILITY_RATING_KEY, D);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_RELIABILITY_RATING_KEY, D);
    verifyAddedRawMeasureOnLeakPeriod(DIR_REF, NEW_RELIABILITY_RATING_KEY, E);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_RELIABILITY_RATING_KEY, E);
  }

  @Test
  public void compute_new_reliability_rating_to_A_when_no_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF);

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(DIR_REF, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_RELIABILITY_RATING_KEY, A);
  }

  @Test
  public void compute_new_reliability_rating_to_A_when_no_new_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newBugIssue(1L, MAJOR).setCreationDate(BEFORE_LEAK_PERIOD_DATE));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(DIR_REF, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_RELIABILITY_RATING_KEY, A);
  }

  @Test
  public void compute_E_reliability_and_security_rating_on_blocker_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newBugIssue(10L, BLOCKER).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newVulnerabilityIssue(1L, BLOCKER).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newBugIssue(1L, MAJOR).setCreationDate(AFTER_LEAK_PERIOD_DATE));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_RELIABILITY_RATING_KEY, E);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SECURITY_RATING_KEY, E);
  }

  @Test
  public void compute_D_reliability_and_security_rating_on_critical_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newBugIssue(10L, CRITICAL).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newVulnerabilityIssue(15L, CRITICAL).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newCodeSmellIssue(1L, MAJOR).setCreationDate(AFTER_LEAK_PERIOD_DATE));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_RELIABILITY_RATING_KEY, D);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SECURITY_RATING_KEY, D);
  }

  @Test
  public void compute_C_reliability_and_security_rating_on_major_issue() {
    // Calculate metric not because a period is set, but because it is a PR
    periodsHolder.setPeriod(null);
    Branch b = mock(Branch.class);
    when(b.getType()).thenReturn(BranchType.PULL_REQUEST);
    analysisMetadataHolder.setBranch(b);

    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newBugIssue(10L, MAJOR).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newVulnerabilityIssue(15L, MAJOR).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newCodeSmellIssue(1L, MAJOR).setCreationDate(AFTER_LEAK_PERIOD_DATE));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_RELIABILITY_RATING_KEY, C);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SECURITY_RATING_KEY, C);
  }

  @Test
  public void compute_B_reliability_and_security_rating_on_minor_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newBugIssue(10L, MINOR).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newVulnerabilityIssue(15L, MINOR).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newCodeSmellIssue(1L, MAJOR).setCreationDate(AFTER_LEAK_PERIOD_DATE));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_RELIABILITY_RATING_KEY, B);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SECURITY_RATING_KEY, B);
  }

  @Test
  public void compute_A_reliability_and_security_rating_on_info_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newBugIssue(10L, INFO).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      newVulnerabilityIssue(15L, INFO).setCreationDate(AFTER_LEAK_PERIOD_DATE),
      // Should not be taken into account
      newCodeSmellIssue(1L, MAJOR).setCreationDate(AFTER_LEAK_PERIOD_DATE));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SECURITY_RATING_KEY, A);
  }

  private void verifyAddedRawMeasureOnLeakPeriod(int componentRef, String metricKey, Rating rating) {
    MeasureAssert.assertThat(measureRepository.getAddedRawMeasure(componentRef, metricKey))
      .hasVariation(rating.getIndex());
  }

  private static DefaultIssue newBugIssue(long effort, String severity) {
    return newIssue(effort, severity, BUG);
  }

  private static DefaultIssue newVulnerabilityIssue(long effort, String severity) {
    return newIssue(effort, severity, VULNERABILITY);
  }

  private static DefaultIssue newCodeSmellIssue(long effort, String severity) {
    return newIssue(effort, severity, CODE_SMELL);
  }

  private static DefaultIssue newIssue(long effort, String severity, RuleType type) {
    return newIssue(severity, type)
      .setEffort(Duration.create(effort));
  }

  private static DefaultIssue newIssue(String severity, RuleType type) {
    return new DefaultIssue()
      .setKey(Uuids.create())
      .setSeverity(severity)
      .setType(type)
      .setCreationDate(DEFAULT_ISSUE_CREATION_DATE);
  }

}
