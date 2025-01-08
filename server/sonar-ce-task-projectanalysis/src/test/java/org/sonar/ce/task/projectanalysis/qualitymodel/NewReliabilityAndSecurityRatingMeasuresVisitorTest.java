/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.ce.task.projectanalysis.issue.ComponentIssuesRepositoryRule;
import org.sonar.ce.task.projectanalysis.issue.FillComponentIssuesVisitorRule;
import org.sonar.ce.task.projectanalysis.issue.NewIssueClassifier;
import org.sonar.ce.task.projectanalysis.measure.MeasureAssert;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.Uuids;
import org.sonar.server.measure.Rating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_RATING;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_RATING;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY;

class NewReliabilityAndSecurityRatingMeasuresVisitorTest {

  private static final long LEAK_PERIOD_SNAPSHOT_IN_MILLISEC = 12323L;
  private static final Date DEFAULT_ISSUE_CREATION_DATE = new Date(1000L);
  private static final Date AFTER_LEAK_PERIOD_DATE = new Date(LEAK_PERIOD_SNAPSHOT_IN_MILLISEC + 5000L);

  private static final String LANGUAGE_KEY_1 = "lKey1";

  private static final int PROJECT_REF = 1;
  private static final int ROOT_DIR_REF = 12;
  private static final int DIRECTORY_REF = 123;
  private static final int FILE_1_REF = 1231;
  private static final int FILE_2_REF = 1232;

  private static final Component ROOT_PROJECT = builder(Component.Type.PROJECT, PROJECT_REF).setKey("project")
    .addChildren(
      builder(DIRECTORY, ROOT_DIR_REF).setKey("dir")
        .addChildren(
          builder(DIRECTORY, DIRECTORY_REF).setKey("directory")
            .addChildren(
              builder(FILE, FILE_1_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_KEY_1, 1)).setKey("file1").build(),
              builder(FILE, FILE_2_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_KEY_1, 1)).setKey("file2").build())
            .build())
        .build())
    .build();

  @RegisterExtension
  private final TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @RegisterExtension
  private final MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NEW_SECURITY_RATING)
    .add(NEW_RELIABILITY_RATING)
    .add(NEW_SOFTWARE_QUALITY_RELIABILITY_RATING)
    .add(NEW_SOFTWARE_QUALITY_SECURITY_RATING);

  @RegisterExtension
  private final MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private final ComponentIssuesRepositoryRule componentIssuesRepositoryRule = new ComponentIssuesRepositoryRule(treeRootHolder);

  @RegisterExtension
  private final FillComponentIssuesVisitorRule fillComponentIssuesVisitorRule =
    new FillComponentIssuesVisitorRule(componentIssuesRepositoryRule, treeRootHolder);

  private final NewIssueClassifier newIssueClassifier = mock(NewIssueClassifier.class);
  private final VisitorsCrawler underTest = new VisitorsCrawler(Arrays.asList(fillComponentIssuesVisitorRule,
    new NewReliabilityAndSecurityRatingMeasuresVisitor(metricRepository, measureRepository, componentIssuesRepositoryRule, newIssueClassifier)));

  @BeforeEach
  void before() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);
  }

  @Test
  void measures_created_for_project_are_all_A_when_they_have_no_FILE_child() {
    ReportComponent root = builder(PROJECT, 1).build();
    treeRootHolder.setRoot(root);

    underTest.visit(root);

    verifyAddedRawMeasureOnLeakPeriod(1, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(1, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(1, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(1, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, A);
  }

  @Test
  void no_measure_if_there_is_no_period() {
    when(newIssueClassifier.isEnabled()).thenReturn(false);
    treeRootHolder.setRoot(builder(PROJECT, 1).build());

    underTest.visit(treeRootHolder.getRoot());

    assertThat(measureRepository.getAddedRawMeasures(1).values()).isEmpty();
  }

  @Test
  void compute_new_security_rating() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newVulnerabilityIssue(10L, MAJOR),
      // Should not be taken into account
      oldVulnerabilityIssue(1L, MAJOR),
      newBugIssue(1L, MAJOR));
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newVulnerabilityIssue(2L, CRITICAL),
      newVulnerabilityIssue(3L, MINOR),
      // Should not be taken into account
      newVulnerabilityIssue(10L, BLOCKER).setResolution(RESOLUTION_FIXED));
    fillComponentIssuesVisitorRule.setIssues(ROOT_DIR_REF, newVulnerabilityIssue(7L, BLOCKER));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_SECURITY_RATING_KEY, C);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_SECURITY_RATING_KEY, D);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_SECURITY_RATING_KEY, D);
    verifyAddedRawMeasureOnLeakPeriod(ROOT_DIR_REF, NEW_SECURITY_RATING_KEY, E);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SECURITY_RATING_KEY, E);
  }

  @Test
  void compute_new_software_quality_security_rating() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newImpactIssue(SoftwareQuality.SECURITY, Severity.MEDIUM),
      // Should not be taken into account
      oldImpactIssue(SoftwareQuality.SECURITY, Severity.HIGH));
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newImpactIssue(SoftwareQuality.SECURITY, Severity.LOW),
      newImpactIssue(SoftwareQuality.SECURITY, Severity.BLOCKER),
      // Should not be taken into account
      oldImpactIssue(SoftwareQuality.SECURITY, Severity.HIGH));
    fillComponentIssuesVisitorRule.setIssues(ROOT_DIR_REF, newImpactIssue(SoftwareQuality.SECURITY, Severity.HIGH));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, C);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, E);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, E);
    verifyAddedRawMeasureOnLeakPeriod(ROOT_DIR_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, E);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, E);
  }

  @Test
  void compute_new_security_rating_to_A_when_no_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF);

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(ROOT_DIR_REF, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SECURITY_RATING_KEY, A);
  }

  @Test
  void compute_new_software_quality_security_rating_to_A_when_no_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF);

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(ROOT_DIR_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, A);
  }

  @Test
  void compute_new_security_rating_to_A_when_no_new_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF, oldVulnerabilityIssue(1L, MAJOR));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(ROOT_DIR_REF, NEW_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SECURITY_RATING_KEY, A);
  }

  @Test
  void compute_new_software_quality_security_rating_to_A_when_no_new_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF, oldImpactIssue(SoftwareQuality.SECURITY, Severity.HIGH));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(ROOT_DIR_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, A);
  }

  @Test
  void compute_new_reliability_rating() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newBugIssue(10L, MAJOR),
      // Should not be taken into account
      oldBugIssue(1L, MAJOR),
      newVulnerabilityIssue(1L, MAJOR));
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newBugIssue(2L, CRITICAL),
      newBugIssue(3L, MINOR),
      // Should not be taken into account
      newBugIssue(10L, BLOCKER).setResolution(RESOLUTION_FIXED));
    fillComponentIssuesVisitorRule.setIssues(ROOT_DIR_REF,
      newBugIssue(7L, BLOCKER));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_RELIABILITY_RATING_KEY, C);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_RELIABILITY_RATING_KEY, D);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_RELIABILITY_RATING_KEY, D);
    verifyAddedRawMeasureOnLeakPeriod(ROOT_DIR_REF, NEW_RELIABILITY_RATING_KEY, E);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_RELIABILITY_RATING_KEY, E);
  }

  @Test
  void compute_new_software_quality_reliability_rating() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newImpactIssue(SoftwareQuality.RELIABILITY, Severity.MEDIUM),
      // Should not be taken into account
      oldImpactIssue(SoftwareQuality.RELIABILITY, Severity.HIGH));
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF,
      newImpactIssue(SoftwareQuality.RELIABILITY, Severity.INFO),
      newImpactIssue(SoftwareQuality.RELIABILITY, Severity.HIGH),
      // Should not be taken into account
      oldImpactIssue(SoftwareQuality.RELIABILITY, Severity.HIGH));
    fillComponentIssuesVisitorRule.setIssues(ROOT_DIR_REF, newImpactIssue(SoftwareQuality.RELIABILITY, Severity.HIGH));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, C);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, D);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, D);
    verifyAddedRawMeasureOnLeakPeriod(ROOT_DIR_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, D);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, D);
  }

  @Test
  void compute_new_reliability_rating_to_A_when_no_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF);

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(ROOT_DIR_REF, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_RELIABILITY_RATING_KEY, A);
  }

  @Test
  void compute_new_software_quality_reliability_rating_to_A_when_no_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF);

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(ROOT_DIR_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, A);
  }

  @Test
  void compute_new_reliability_rating_to_A_when_no_new_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF, oldBugIssue(1L, MAJOR));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(ROOT_DIR_REF, NEW_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_RELIABILITY_RATING_KEY, A);
  }

  @Test
  void compute_new_software_quality_reliability_rating_to_A_when_no_new_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF, oldImpactIssue(SoftwareQuality.RELIABILITY, Severity.HIGH));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(FILE_1_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(FILE_2_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(DIRECTORY_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(ROOT_DIR_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, A);
  }

  @Test
  void compute_E_reliability_and_security_rating_on_blocker_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newBugIssue(10L, BLOCKER),
      newVulnerabilityIssue(1L, BLOCKER),
      // Should not be taken into account
      newBugIssue(1L, MAJOR));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_RELIABILITY_RATING_KEY, E);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SECURITY_RATING_KEY, E);
  }

  @Test
  void compute_E_software_quality_reliability_and_security_rating_on_blocker_severity_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newImpactIssue(SoftwareQuality.RELIABILITY, Severity.BLOCKER),
      newImpactIssue(SoftwareQuality.SECURITY, Severity.BLOCKER),
      // Should not be taken into account
      newCodeSmellIssue(1L, MAJOR));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, E);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, E);
  }

  @Test
  void compute_D_reliability_and_security_rating_on_critical_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newBugIssue(10L, CRITICAL),
      newVulnerabilityIssue(15L, CRITICAL),
      // Should not be taken into account
      newCodeSmellIssue(1L, MAJOR));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_RELIABILITY_RATING_KEY, D);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SECURITY_RATING_KEY, D);
  }

  @Test
  void compute_D_software_quality_reliability_and_security_rating_on_high_severity_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newImpactIssue(SoftwareQuality.RELIABILITY, Severity.HIGH),
      newImpactIssue(SoftwareQuality.SECURITY, Severity.HIGH),
      // Should not be taken into account
      newCodeSmellIssue(1L, MAJOR));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, D);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, D);
  }

  @Test
  void compute_C_reliability_and_security_rating_on_major_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newBugIssue(10L, MAJOR),
      newVulnerabilityIssue(15L, MAJOR),
      // Should not be taken into account
      newCodeSmellIssue(1L, MAJOR));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_RELIABILITY_RATING_KEY, C);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SECURITY_RATING_KEY, C);
  }

  @Test
  void compute_C_software_quality_reliability_and_security_rating_on_medium_severity_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newImpactIssue(SoftwareQuality.RELIABILITY, Severity.MEDIUM),
      newImpactIssue(SoftwareQuality.SECURITY, Severity.MEDIUM),
      // Should not be taken into account
      newCodeSmellIssue(1L, MAJOR));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, C);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, C);
  }

  @Test
  void compute_B_reliability_and_security_rating_on_minor_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newBugIssue(10L, MINOR),
      newVulnerabilityIssue(15L, MINOR),
      // Should not be taken into account
      newCodeSmellIssue(1L, MAJOR));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_RELIABILITY_RATING_KEY, B);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SECURITY_RATING_KEY, B);
  }

  @Test
  void compute_B_software_quality_reliability_and_security_rating_on_low_severity_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newImpactIssue(SoftwareQuality.RELIABILITY, Severity.LOW),
      newImpactIssue(SoftwareQuality.SECURITY, Severity.LOW),
      // Should not be taken into account
      newCodeSmellIssue(1L, MAJOR));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, B);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, B);
  }

  @Test
  void compute_A_reliability_and_security_rating_on_info_issue() {
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

  @Test
  void compute_A_software_quality_reliability_and_security_rating_on_info_severity_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      newImpactIssue(SoftwareQuality.RELIABILITY, Severity.INFO),
      newImpactIssue(SoftwareQuality.SECURITY, Severity.INFO),
      // Should not be taken into account
      newCodeSmellIssue(1L, MAJOR));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, A);
  }

  @Test
  void compute_A_software_quality_reliability_and_security_rating_when_no_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF,
      // Should not be taken into account
      newCodeSmellIssue(1L, MAJOR));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasureOnLeakPeriod(PROJECT_REF, NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY, A);
  }

  private void verifyAddedRawMeasureOnLeakPeriod(int componentRef, String metricKey, Rating rating) {
    MeasureAssert.assertThat(measureRepository.getAddedRawMeasure(componentRef, metricKey)).hasValue(rating.getIndex());
  }

  private DefaultIssue newBugIssue(long effort, String severity) {
    return createIssue(effort, severity, BUG, true);
  }

  private DefaultIssue oldBugIssue(long effort, String severity) {
    return createIssue(effort, severity, BUG, false);
  }

  private DefaultIssue newVulnerabilityIssue(long effort, String severity) {
    return createIssue(effort, severity, VULNERABILITY, true);
  }

  private DefaultIssue oldVulnerabilityIssue(long effort, String severity) {
    return createIssue(effort, severity, VULNERABILITY, false);
  }

  private DefaultIssue newCodeSmellIssue(long effort, String severity) {
    return createIssue(effort, severity, CODE_SMELL, true);
  }

  private DefaultIssue newImpactIssue(SoftwareQuality softwareQuality, Severity severity) {
    return createIssue(softwareQuality, severity, true);
  }

  private DefaultIssue oldImpactIssue(SoftwareQuality softwareQuality, Severity severity) {
    return createIssue(softwareQuality, severity, false);
  }

  private DefaultIssue createIssue(long effort, String severity, RuleType type, boolean isNew) {
    DefaultIssue issue = createIssue(severity, type)
      .setEffort(Duration.create(effort));
    when(newIssueClassifier.isNew(any(), eq(issue))).thenReturn(isNew);
    return issue;
  }

  private static DefaultIssue createIssue(String severity, RuleType type) {
    return new DefaultIssue()
      .setKey(UuidFactoryFast.getInstance().create())
      .setSeverity(severity)
      .setType(type)
      .setCreationDate(DEFAULT_ISSUE_CREATION_DATE);
  }

  private DefaultIssue createIssue(SoftwareQuality softwareQuality, Severity severity, boolean isNew) {
    DefaultIssue issue = new DefaultIssue()
      .setKey(Uuids.create())
      .addImpact(softwareQuality, severity)
      .setType(BUG)
      .setSeverity("BLOCKER")
      .setCreationDate(new Date(1000L));
    when(newIssueClassifier.isNew(any(), eq(issue))).thenReturn(isNew);
    return issue;
  }
}
