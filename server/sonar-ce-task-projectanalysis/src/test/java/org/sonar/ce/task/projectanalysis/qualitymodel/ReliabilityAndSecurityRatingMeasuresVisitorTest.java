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
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.VisitorsCrawler;
import org.sonar.ce.task.projectanalysis.issue.ComponentIssuesRepositoryRule;
import org.sonar.ce.task.projectanalysis.issue.FillComponentIssuesVisitorRule;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.Uuids;
import org.sonar.server.measure.Rating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING_KEY;
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
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.toEntries;
import static org.sonar.server.measure.Rating.A;
import static org.sonar.server.measure.Rating.B;
import static org.sonar.server.measure.Rating.C;
import static org.sonar.server.measure.Rating.D;
import static org.sonar.server.measure.Rating.E;

public class ReliabilityAndSecurityRatingMeasuresVisitorTest {

  static final String LANGUAGE_KEY_1 = "lKey1";

  static final int PROJECT_REF = 1;
  static final int DIRECTORY_REF = 123;
  static final int FILE_1_REF = 1231;
  static final int FILE_2_REF = 1232;

  static final Component ROOT_PROJECT = builder(Component.Type.PROJECT, PROJECT_REF).setKey("project")
    .addChildren(
      builder(DIRECTORY, DIRECTORY_REF).setKey("directory")
        .addChildren(
          builder(FILE, FILE_1_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_KEY_1, 1)).setKey("file1").build(),
          builder(FILE, FILE_2_REF).setFileAttributes(new FileAttributes(false, LANGUAGE_KEY_1, 1)).setKey("file2").build())
        .build())
    .build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(RELIABILITY_RATING)
    .add(SECURITY_RATING);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  @Rule
  public ComponentIssuesRepositoryRule componentIssuesRepositoryRule = new ComponentIssuesRepositoryRule(treeRootHolder);

  @Rule
  public FillComponentIssuesVisitorRule fillComponentIssuesVisitorRule = new FillComponentIssuesVisitorRule(componentIssuesRepositoryRule, treeRootHolder);

  VisitorsCrawler underTest = new VisitorsCrawler(
    Arrays.asList(fillComponentIssuesVisitorRule, new ReliabilityAndSecurityRatingMeasuresVisitor(metricRepository, measureRepository, componentIssuesRepositoryRule)));

  @Test
  public void measures_created_for_project_are_all_A_when_they_have_no_FILE_child() {
    ReportComponent root = builder(PROJECT, 1).build();
    treeRootHolder.setRoot(root);

    underTest.visit(root);

    assertThat(measureRepository.getRawMeasures(root)
      .entrySet().stream().map(e -> entryOf(e.getKey(), e.getValue())))
      .containsOnly(
        entryOf(RELIABILITY_RATING_KEY, createRatingMeasure(A)),
        entryOf(SECURITY_RATING_KEY, createRatingMeasure(A)));
  }

  @Test
  public void compute_reliability_rating() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF, newBugIssue(10L, MAJOR), newBugIssue(1L, MAJOR),
      // Should not be taken into account
      newVulnerabilityIssue(5L, MINOR));
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF, newBugIssue(2L, CRITICAL), newBugIssue(3L, MINOR),
      // Should not be taken into account
      newBugIssue(10L, BLOCKER).setResolution(RESOLUTION_FIXED));
    fillComponentIssuesVisitorRule.setIssues(PROJECT_REF, newBugIssue(7L, BLOCKER));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasure(FILE_1_REF, RELIABILITY_RATING_KEY, C);
    verifyAddedRawMeasure(FILE_2_REF, RELIABILITY_RATING_KEY, D);
    verifyAddedRawMeasure(DIRECTORY_REF, RELIABILITY_RATING_KEY, D);
    verifyAddedRawMeasure(PROJECT_REF, RELIABILITY_RATING_KEY, E);
  }

  @Test
  public void compute_security_rating() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF, newVulnerabilityIssue(10L, MAJOR), newVulnerabilityIssue(1L, MAJOR),
      // Should not be taken into account
      newBugIssue(1L, MAJOR));
    fillComponentIssuesVisitorRule.setIssues(FILE_2_REF, newVulnerabilityIssue(2L, CRITICAL), newVulnerabilityIssue(3L, MINOR),
      // Should not be taken into account
      newVulnerabilityIssue(10L, BLOCKER).setResolution(RESOLUTION_FIXED));
    fillComponentIssuesVisitorRule.setIssues(PROJECT_REF, newVulnerabilityIssue(7L, BLOCKER));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasure(FILE_1_REF, SECURITY_RATING_KEY, C);
    verifyAddedRawMeasure(FILE_2_REF, SECURITY_RATING_KEY, D);
    verifyAddedRawMeasure(DIRECTORY_REF, SECURITY_RATING_KEY, D);
    verifyAddedRawMeasure(PROJECT_REF, SECURITY_RATING_KEY, E);
  }

  @Test
  public void compute_E_reliability_and_security_rating_on_blocker_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF, newBugIssue(10L, BLOCKER), newVulnerabilityIssue(1L, BLOCKER),
      // Should not be taken into account
      newBugIssue(1L, MAJOR));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasure(PROJECT_REF, RELIABILITY_RATING_KEY, E);
    verifyAddedRawMeasure(PROJECT_REF, SECURITY_RATING_KEY, E);
  }

  @Test
  public void compute_D_reliability_and_security_rating_on_critical_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF, newBugIssue(10L, CRITICAL), newVulnerabilityIssue(15L, CRITICAL),
      // Should not be taken into account
      newCodeSmellIssue(1L, MAJOR));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasure(PROJECT_REF, RELIABILITY_RATING_KEY, D);
    verifyAddedRawMeasure(PROJECT_REF, SECURITY_RATING_KEY, D);
  }

  @Test
  public void compute_C_reliability_and_security_rating_on_major_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF, newBugIssue(10L, MAJOR), newVulnerabilityIssue(15L, MAJOR),
      // Should not be taken into account
      newCodeSmellIssue(1L, MAJOR));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasure(PROJECT_REF, RELIABILITY_RATING_KEY, C);
    verifyAddedRawMeasure(PROJECT_REF, SECURITY_RATING_KEY, C);
  }

  @Test
  public void compute_B_reliability_and_security_rating_on_minor_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF, newBugIssue(10L, MINOR), newVulnerabilityIssue(15L, MINOR),
      // Should not be taken into account
      newCodeSmellIssue(1L, MAJOR));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasure(PROJECT_REF, RELIABILITY_RATING_KEY, B);
    verifyAddedRawMeasure(PROJECT_REF, SECURITY_RATING_KEY, B);
  }

  @Test
  public void compute_A_reliability_and_security_rating_on_info_issue() {
    treeRootHolder.setRoot(ROOT_PROJECT);
    fillComponentIssuesVisitorRule.setIssues(FILE_1_REF, newBugIssue(10L, INFO), newVulnerabilityIssue(15L, INFO),
      // Should not be taken into account
      newCodeSmellIssue(1L, MAJOR));

    underTest.visit(ROOT_PROJECT);

    verifyAddedRawMeasure(PROJECT_REF, RELIABILITY_RATING_KEY, A);
    verifyAddedRawMeasure(PROJECT_REF, SECURITY_RATING_KEY, A);
  }

  private void verifyAddedRawMeasure(int componentRef, String metricKey, Rating rating) {
    assertThat(toEntries(measureRepository.getAddedRawMeasures(componentRef))).contains(entryOf(metricKey, newMeasureBuilder().create(rating.getIndex(), rating.name())));
  }

  private static Measure createRatingMeasure(Rating rating) {
    return newMeasureBuilder().create(rating.getIndex(), rating.name());
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
      .setCreationDate(new Date(1000l));
  }

}
