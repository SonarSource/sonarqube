/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.issue;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.core.rule.RuleType;
import org.sonar.ce.common.scanner.ScannerReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.measure.ValueType;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.measure.ImpactMeasureBuilder;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_IN_SANDBOX;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.issue.impact.Severity.BLOCKER;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.Severity.INFO;
import static org.sonar.api.issue.impact.Severity.LOW;
import static org.sonar.api.issue.impact.Severity.MEDIUM;
import static org.sonar.api.issue.impact.SoftwareQuality.MAINTAINABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.RELIABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;
import static org.sonar.api.measures.CoreMetrics.ACCEPTED_ISSUES;
import static org.sonar.api.measures.CoreMetrics.ACCEPTED_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.BLOCKER_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.BLOCKER_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.BUGS;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.CONFIRMED_ISSUES;
import static org.sonar.api.measures.CoreMetrics.CONFIRMED_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.CRITICAL_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.CRITICAL_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.FALSE_POSITIVE_ISSUES;
import static org.sonar.api.measures.CoreMetrics.FALSE_POSITIVE_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.HIGH_IMPACT_ACCEPTED_ISSUES;
import static org.sonar.api.measures.CoreMetrics.HIGH_IMPACT_ACCEPTED_ISSUES_KEY;
import static org.sonar.server.metric.IssueCountMetrics.ISSUES_IN_SANDBOX;
import static org.sonar.server.metric.IssueCountMetrics.NEW_ISSUES_IN_SANDBOX;
import static org.sonar.api.measures.CoreMetrics.INFO_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.MAINTAINABILITY_ISSUES;
import static org.sonar.api.measures.CoreMetrics.MAJOR_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.MAJOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.MINOR_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.NEW_ACCEPTED_ISSUES;
import static org.sonar.api.measures.CoreMetrics.NEW_ACCEPTED_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_BLOCKER_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.NEW_BLOCKER_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_BUGS;
import static org.sonar.api.measures.CoreMetrics.NEW_BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_CODE_SMELLS;
import static org.sonar.api.measures.CoreMetrics.NEW_CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_CRITICAL_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.NEW_CRITICAL_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_INFO_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_ISSUES;
import static org.sonar.api.measures.CoreMetrics.NEW_MAJOR_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.NEW_MAJOR_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MINOR_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_ISSUES;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_ISSUES;
import static org.sonar.api.measures.CoreMetrics.NEW_VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.NEW_VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_VULNERABILITIES;
import static org.sonar.api.measures.CoreMetrics.NEW_VULNERABILITIES_KEY;
import static org.sonar.api.measures.CoreMetrics.OPEN_ISSUES;
import static org.sonar.api.measures.CoreMetrics.OPEN_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_ISSUES;
import static org.sonar.api.measures.CoreMetrics.REOPENED_ISSUES;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_ISSUES;
import static org.sonar.api.measures.CoreMetrics.VIOLATIONS;
import static org.sonar.api.measures.CoreMetrics.VIOLATIONS_KEY;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES_KEY;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.core.rule.RuleType.BUG;
import static org.sonar.core.rule.RuleType.CODE_SMELL;
import static org.sonar.core.rule.RuleType.SECURITY_HOTSPOT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.ce.task.projectanalysis.issue.IssueCounter.IMPACT_TO_JSON_METRIC_KEY;
import static org.sonar.ce.task.projectanalysis.issue.IssueCounter.IMPACT_TO_NEW_JSON_METRIC_KEY;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.measure.MeasureRepoEntry.entryOf;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_BLOCKER_ISSUES;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_BLOCKER_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_HIGH_ISSUES;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_HIGH_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_INFO_ISSUES;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_INFO_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_LOW_ISSUES;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_LOW_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MEDIUM_ISSUES;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MEDIUM_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_ISSUES;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_BLOCKER_ISSUES;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_BLOCKER_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_HIGH_ISSUES;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_HIGH_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_INFO_ISSUES;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_INFO_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_LOW_ISSUES;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_LOW_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MEDIUM_ISSUES;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MEDIUM_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_ISSUES;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_ISSUES;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_ISSUES_KEY;
import static org.sonar.test.JsonAssert.assertJson;

class IssueCounterTest {

  private static final Component FILE1 = builder(Component.Type.FILE, 1).build();
  private static final Component FILE2 = builder(Component.Type.FILE, 2).build();
  private static final Component FILE3 = builder(Component.Type.FILE, 3).build();
  private static final Component PROJECT = builder(Component.Type.PROJECT, 4).addChildren(FILE1, FILE2, FILE3).build();

  @RegisterExtension
  private final ScannerReportReaderRule reportReader = new ScannerReportReaderRule();

  @RegisterExtension
  private final TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @RegisterExtension
  private final MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(VIOLATIONS)
    .add(OPEN_ISSUES)
    .add(REOPENED_ISSUES)
    .add(CONFIRMED_ISSUES)
    .add(BLOCKER_VIOLATIONS)
    .add(CRITICAL_VIOLATIONS)
    .add(MAJOR_VIOLATIONS)
    .add(MINOR_VIOLATIONS)
    .add(INFO_VIOLATIONS)
    .add(NEW_VIOLATIONS)
    .add(NEW_BLOCKER_VIOLATIONS)
    .add(NEW_CRITICAL_VIOLATIONS)
    .add(NEW_MAJOR_VIOLATIONS)
    .add(NEW_MINOR_VIOLATIONS)
    .add(NEW_INFO_VIOLATIONS)
    .add(FALSE_POSITIVE_ISSUES)
    .add(ACCEPTED_ISSUES)
    .add(CODE_SMELLS)
    .add(BUGS)
    .add(VULNERABILITIES)
    .add(SECURITY_HOTSPOTS)
    .add(NEW_CODE_SMELLS)
    .add(NEW_BUGS)
    .add(NEW_VULNERABILITIES)
    .add(NEW_SECURITY_HOTSPOTS)
    .add(NEW_ACCEPTED_ISSUES)
    .add(HIGH_IMPACT_ACCEPTED_ISSUES)
    .add(ISSUES_IN_SANDBOX)
    .add(NEW_ISSUES_IN_SANDBOX)
    .add(RELIABILITY_ISSUES)
    .add(MAINTAINABILITY_ISSUES)
    .add(SECURITY_ISSUES)
    .add(NEW_RELIABILITY_ISSUES)
    .add(NEW_MAINTAINABILITY_ISSUES)
    .add(NEW_SECURITY_ISSUES)
    .add(SOFTWARE_QUALITY_BLOCKER_ISSUES)
    .add(SOFTWARE_QUALITY_HIGH_ISSUES)
    .add(SOFTWARE_QUALITY_MEDIUM_ISSUES)
    .add(SOFTWARE_QUALITY_LOW_ISSUES)
    .add(SOFTWARE_QUALITY_INFO_ISSUES)
    .add(NEW_SOFTWARE_QUALITY_BLOCKER_ISSUES)
    .add(NEW_SOFTWARE_QUALITY_HIGH_ISSUES)
    .add(NEW_SOFTWARE_QUALITY_MEDIUM_ISSUES)
    .add(NEW_SOFTWARE_QUALITY_LOW_ISSUES)
    .add(NEW_SOFTWARE_QUALITY_INFO_ISSUES)
    .add(SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES)
    .add(SOFTWARE_QUALITY_RELIABILITY_ISSUES)
    .add(SOFTWARE_QUALITY_SECURITY_ISSUES)
    .add(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES)
    .add(NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES)
    .add(NEW_SOFTWARE_QUALITY_SECURITY_ISSUES);

  @RegisterExtension
  private final MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  private final NewIssueClassifier newIssueClassifier = mock(NewIssueClassifier.class);
  private final IssueCounter underTest = new IssueCounter(metricRepository, measureRepository, newIssueClassifier);
  private static int issueCounter;

  @Test
  void count_issues_by_status() {
    // bottom-up traversal -> from files to project
    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, org.sonar.api.rule.Severity.BLOCKER));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_FIXED, STATUS_CLOSED, MAJOR));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_FALSE_POSITIVE, STATUS_RESOLVED, MAJOR));
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(FILE2);
    underTest.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, org.sonar.api.rule.Severity.BLOCKER));
    underTest.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, MAJOR));
    underTest.afterComponent(FILE2);

    underTest.beforeComponent(FILE3);
    // Security hotspot should be ignored
    underTest.onIssue(FILE3, createSecurityHotspot().setStatus(STATUS_OPEN));
    underTest.afterComponent(FILE3);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertMeasures(FILE1, entry(VIOLATIONS_KEY, 1), entry(OPEN_ISSUES_KEY, 1), entry(CONFIRMED_ISSUES_KEY, 0));
    assertMeasures(FILE2, entry(VIOLATIONS_KEY, 2), entry(OPEN_ISSUES_KEY, 0), entry(CONFIRMED_ISSUES_KEY, 2));
    assertMeasures(FILE3, entry(VIOLATIONS_KEY, 0));
    assertMeasures(PROJECT, entry(VIOLATIONS_KEY, 3), entry(OPEN_ISSUES_KEY, 1), entry(CONFIRMED_ISSUES_KEY, 2));
  }

  @Test
  void count_issues_by_resolution() {
    // bottom-up traversal -> from files to project
    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, org.sonar.api.rule.Severity.BLOCKER));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_FIXED, STATUS_CLOSED, MAJOR));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_FALSE_POSITIVE, STATUS_RESOLVED, MAJOR));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_RESOLVED, MAJOR));
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(FILE2);
    underTest.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, org.sonar.api.rule.Severity.BLOCKER));
    underTest.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, MAJOR));
    underTest.onIssue(FILE2, createIssue(RESOLUTION_WONT_FIX, STATUS_RESOLVED, MAJOR));
    underTest.afterComponent(FILE2);

    underTest.beforeComponent(FILE3);
    // Security hotspot should be ignored
    underTest.onIssue(FILE3, createSecurityHotspot().setResolution(RESOLUTION_WONT_FIX));
    underTest.afterComponent(FILE3);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertMeasures(FILE1, entry(VIOLATIONS_KEY, 1), entry(FALSE_POSITIVE_ISSUES_KEY, 1), entry(ACCEPTED_ISSUES_KEY, 1));
    assertMeasures(FILE2, entry(VIOLATIONS_KEY, 2), entry(FALSE_POSITIVE_ISSUES_KEY, 0), entry(ACCEPTED_ISSUES_KEY, 1));
    assertMeasures(FILE3, entry(VIOLATIONS_KEY, 0));
    assertMeasures(PROJECT, entry(VIOLATIONS_KEY, 3), entry(FALSE_POSITIVE_ISSUES_KEY, 1), entry(ACCEPTED_ISSUES_KEY, 2));
  }

  @Test
  void count_unresolved_issues_by_severity() {
    // bottom-up traversal -> from files to project
    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, org.sonar.api.rule.Severity.BLOCKER));
    // this resolved issue is ignored
    underTest.onIssue(FILE1, createIssue(RESOLUTION_FIXED, STATUS_CLOSED, MAJOR));
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(FILE2);
    underTest.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, org.sonar.api.rule.Severity.BLOCKER));
    underTest.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, MAJOR));
    underTest.afterComponent(FILE2);

    underTest.beforeComponent(PROJECT);
    // Security hotspot should be ignored
    underTest.onIssue(FILE3, createSecurityHotspot().setSeverity(MAJOR));
    underTest.afterComponent(PROJECT);

    assertMeasures(FILE1, entry(BLOCKER_VIOLATIONS_KEY, 1), entry(CRITICAL_VIOLATIONS_KEY, 0), entry(MAJOR_VIOLATIONS_KEY, 0));
    assertMeasures(FILE2, entry(BLOCKER_VIOLATIONS_KEY, 1), entry(CRITICAL_VIOLATIONS_KEY, 0), entry(MAJOR_VIOLATIONS_KEY, 1));
    assertMeasures(PROJECT, entry(BLOCKER_VIOLATIONS_KEY, 2), entry(CRITICAL_VIOLATIONS_KEY, 0), entry(MAJOR_VIOLATIONS_KEY, 1));
  }

  @Test
  void count_unresolved_issues_by_type() {
    // bottom-up traversal -> from files to project
    // file1 : one open code smell, one closed code smell (which will be excluded from metric)
    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, org.sonar.api.rule.Severity.BLOCKER).setType(CODE_SMELL));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_FIXED, STATUS_CLOSED, MAJOR).setType(CODE_SMELL));
    underTest.afterComponent(FILE1);

    // file2 : one bug
    underTest.beforeComponent(FILE2);
    underTest.onIssue(FILE2, createIssue(null, STATUS_CONFIRMED, org.sonar.api.rule.Severity.BLOCKER).setType(BUG));
    underTest.afterComponent(FILE2);

    // file3 : one unresolved security hotspot
    underTest.beforeComponent(FILE3);
    underTest.onIssue(FILE3, createSecurityHotspot());
    underTest.onIssue(FILE3, createSecurityHotspot().setResolution(RESOLUTION_WONT_FIX).setStatus(STATUS_CLOSED));
    underTest.afterComponent(FILE3);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertMeasures(FILE1, entry(CODE_SMELLS_KEY, 1), entry(BUGS_KEY, 0), entry(VULNERABILITIES_KEY, 0), entry(SECURITY_HOTSPOTS_KEY, 0));
    assertMeasures(FILE2, entry(CODE_SMELLS_KEY, 0), entry(BUGS_KEY, 1), entry(VULNERABILITIES_KEY, 0), entry(SECURITY_HOTSPOTS_KEY, 0));
    assertMeasures(FILE3, entry(CODE_SMELLS_KEY, 0), entry(BUGS_KEY, 0), entry(VULNERABILITIES_KEY, 0), entry(SECURITY_HOTSPOTS_KEY, 1));
    assertMeasures(PROJECT, entry(CODE_SMELLS_KEY, 1), entry(BUGS_KEY, 1), entry(VULNERABILITIES_KEY, 0), entry(SECURITY_HOTSPOTS_KEY, 1));
  }

  @Test
  void count_new_issues() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);

    underTest.beforeComponent(FILE1);
    // created before -> existing issues (so ignored)
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, org.sonar.api.rule.Severity.BLOCKER).setType(CODE_SMELL));
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, org.sonar.api.rule.Severity.BLOCKER).setType(BUG));

    // created after -> 4 new issues but 1 is closed
    underTest.onIssue(FILE1, createNewIssue(null, STATUS_OPEN, CRITICAL).setType(CODE_SMELL));
    underTest.onIssue(FILE1, createNewIssue(null, STATUS_OPEN, CRITICAL).setType(BUG));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_FIXED, STATUS_CLOSED, MAJOR).setType(BUG));
    underTest.onIssue(FILE1, createNewSecurityHotspot());
    underTest.onIssue(FILE1, createNewSecurityHotspot().setResolution(RESOLUTION_WONT_FIX).setStatus(STATUS_CLOSED));
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(FILE2);
    underTest.afterComponent(FILE2);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertIntValue(FILE1, entry(NEW_VIOLATIONS_KEY, 2), entry(NEW_CRITICAL_VIOLATIONS_KEY, 2), entry(NEW_BLOCKER_VIOLATIONS_KEY, 0),
      entry(NEW_MAJOR_VIOLATIONS_KEY, 0),
      entry(NEW_CODE_SMELLS_KEY, 1), entry(NEW_BUGS_KEY, 1), entry(NEW_VULNERABILITIES_KEY, 0), entry(NEW_SECURITY_HOTSPOTS_KEY, 1));
    assertIntValue(PROJECT, entry(NEW_VIOLATIONS_KEY, 2), entry(NEW_CRITICAL_VIOLATIONS_KEY, 2), entry(NEW_BLOCKER_VIOLATIONS_KEY, 0),
      entry(NEW_MAJOR_VIOLATIONS_KEY, 0),
      entry(NEW_CODE_SMELLS_KEY, 1), entry(NEW_BUGS_KEY, 1), entry(NEW_VULNERABILITIES_KEY, 0), entry(NEW_SECURITY_HOTSPOTS_KEY, 1));
  }

  @Test
  void count_new_accepted_issues() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);

    underTest.beforeComponent(FILE1);
    // created before -> existing issues (so ignored)
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, CRITICAL));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_RESOLVED, CRITICAL));

    // created after -> 2 accepted, 1 open, 1 hotspot
    underTest.onIssue(FILE1, createNewIssue(null, STATUS_OPEN, CRITICAL));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_RESOLVED, CRITICAL));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_RESOLVED, CRITICAL));
    underTest.onIssue(FILE1, createNewSecurityHotspot());
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertIntValue(FILE1, entry(NEW_VIOLATIONS_KEY, 1), entry(NEW_ACCEPTED_ISSUES_KEY, 2), entry(NEW_SECURITY_HOTSPOTS_KEY, 1));
    assertIntValue(PROJECT, entry(NEW_VIOLATIONS_KEY, 1), entry(NEW_ACCEPTED_ISSUES_KEY, 2), entry(NEW_SECURITY_HOTSPOTS_KEY, 1));
  }

  @Test
  void onIssue_shouldCountOverallSoftwareQualitiesMeasures() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);

    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, MAINTAINABILITY, HIGH));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, MAINTAINABILITY, MEDIUM));

    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, MAINTAINABILITY, HIGH));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_RESOLVED, MAINTAINABILITY, HIGH));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, MAINTAINABILITY, MEDIUM));

    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, SoftwareQuality.SECURITY, HIGH));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, SoftwareQuality.SECURITY, MEDIUM));

    underTest.onIssue(FILE1, createNewSecurityHotspot());
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    Set<Map.Entry<String, Measure>> entries = measureRepository.getRawMeasures(FILE1).entrySet();

    assertOverallSoftwareQualityMeasures(MAINTAINABILITY, getImpactMeasure(4, 2, 2, 0, 0, 0), entries);
    assertOverallSoftwareQualityMeasures(SoftwareQuality.SECURITY, getImpactMeasure(2, 1, 1, 0, 0, 0), entries);
    assertOverallSoftwareQualityMeasures(RELIABILITY, getImpactMeasure(0, 0, 0, 0, 0, 0), entries);
  }

  @Test
  void onIssue_shouldCountByImpactSeverity() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);

    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, Map.of(MAINTAINABILITY, BLOCKER, SECURITY, BLOCKER)));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, Map.of(MAINTAINABILITY, HIGH, SECURITY, BLOCKER)));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, RELIABILITY, HIGH));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, Map.of(SECURITY, MEDIUM, MAINTAINABILITY, LOW)));
    // Should not count because it is resolved
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_RESOLVED, Map.of(SECURITY, BLOCKER, MAINTAINABILITY, INFO)));

    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, MAINTAINABILITY, HIGH));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, Map.of(SECURITY, MEDIUM, MAINTAINABILITY, LOW)));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, Map.of(MAINTAINABILITY, INFO, SECURITY, INFO)));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, Map.of(MAINTAINABILITY, INFO, SECURITY, INFO)));

    underTest.afterComponent(FILE1);

    underTest.beforeComponent(PROJECT);
    underTest.onIssue(PROJECT, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, Map.of(SECURITY, MEDIUM, MAINTAINABILITY, LOW)));
    underTest.afterComponent(PROJECT);

    assertIntValue(FILE1,
      entry(SOFTWARE_QUALITY_BLOCKER_ISSUES_KEY, 2),
      entry(SOFTWARE_QUALITY_HIGH_ISSUES_KEY, 3),
      entry(SOFTWARE_QUALITY_MEDIUM_ISSUES_KEY, 2),
      entry(SOFTWARE_QUALITY_LOW_ISSUES_KEY, 2),
      entry(SOFTWARE_QUALITY_INFO_ISSUES_KEY, 2)
    );

    assertIntValue(FILE1,
      entry(NEW_SOFTWARE_QUALITY_BLOCKER_ISSUES_KEY, 0),
      entry(NEW_SOFTWARE_QUALITY_HIGH_ISSUES_KEY, 1),
      entry(NEW_SOFTWARE_QUALITY_MEDIUM_ISSUES_KEY, 1),
      entry(NEW_SOFTWARE_QUALITY_LOW_ISSUES_KEY, 1),
      entry(NEW_SOFTWARE_QUALITY_INFO_ISSUES_KEY, 2)
    );

    assertIntValue(PROJECT,
      entry(SOFTWARE_QUALITY_BLOCKER_ISSUES_KEY, 2),
      entry(SOFTWARE_QUALITY_HIGH_ISSUES_KEY, 3),
      entry(SOFTWARE_QUALITY_MEDIUM_ISSUES_KEY, 3),
      entry(SOFTWARE_QUALITY_LOW_ISSUES_KEY, 3),
      entry(SOFTWARE_QUALITY_INFO_ISSUES_KEY, 2)
    );

    assertIntValue(PROJECT,
      entry(NEW_SOFTWARE_QUALITY_BLOCKER_ISSUES_KEY, 0),
      entry(NEW_SOFTWARE_QUALITY_HIGH_ISSUES_KEY, 1),
      entry(NEW_SOFTWARE_QUALITY_MEDIUM_ISSUES_KEY, 2),
      entry(NEW_SOFTWARE_QUALITY_LOW_ISSUES_KEY, 2),
      entry(NEW_SOFTWARE_QUALITY_INFO_ISSUES_KEY, 2)
    );
  }

  @Test
  void onIssue_shouldCountBySoftwareQuality() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);

    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, Map.of(MAINTAINABILITY, HIGH, SECURITY, BLOCKER)));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, RELIABILITY, HIGH));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, Map.of(SECURITY, MEDIUM, MAINTAINABILITY, LOW)));
    // Should not count because it is resolved
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_RESOLVED, Map.of(SECURITY, BLOCKER, MAINTAINABILITY, INFO)));

    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, MAINTAINABILITY, HIGH));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, Map.of(SECURITY, MEDIUM, MAINTAINABILITY, LOW)));

    underTest.afterComponent(FILE1);

    underTest.beforeComponent(PROJECT);
    underTest.onIssue(PROJECT, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, Map.of(SECURITY, MEDIUM, MAINTAINABILITY, LOW)));
    underTest.afterComponent(PROJECT);

    assertIntValue(FILE1,
      entry(SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY, 4),
      entry(SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY, 1),
      entry(SOFTWARE_QUALITY_SECURITY_ISSUES_KEY, 3)
    );

    assertIntValue(FILE1,
      entry(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY, 2),
      entry(NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY, 0),
      entry(NEW_SOFTWARE_QUALITY_SECURITY_ISSUES_KEY, 1)
    );

    assertIntValue(PROJECT,
      entry(SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY, 5),
      entry(SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY, 1),
      entry(SOFTWARE_QUALITY_SECURITY_ISSUES_KEY, 4)
    );

    assertIntValue(PROJECT,
      entry(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY, 3),
      entry(NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY, 0),
      entry(NEW_SOFTWARE_QUALITY_SECURITY_ISSUES_KEY, 2)
    );
  }

  @Test
  void onIssue_shouldCountNewSoftwareQualitiesMeasures() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);

    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, MAINTAINABILITY, HIGH));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, MAINTAINABILITY, HIGH));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_RESOLVED, MAINTAINABILITY, HIGH));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, MAINTAINABILITY, MEDIUM));

    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, RELIABILITY, HIGH));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, RELIABILITY, LOW));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_RESOLVED, RELIABILITY, HIGH));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, RELIABILITY, MEDIUM));

    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, SoftwareQuality.SECURITY, MEDIUM));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, SoftwareQuality.SECURITY, LOW));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, SoftwareQuality.SECURITY, HIGH));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, SoftwareQuality.SECURITY, HIGH));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_RESOLVED, SoftwareQuality.SECURITY, HIGH));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_OPEN, SoftwareQuality.SECURITY, MEDIUM));

    underTest.afterComponent(FILE1);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    Set<Map.Entry<String, Measure>> entries = measureRepository.getRawMeasures(FILE1).entrySet();

    assertNewSoftwareQualityMeasures(MAINTAINABILITY, getImpactMeasure(2, 1, 1, 0, 0, 0), entries);
    assertNewSoftwareQualityMeasures(RELIABILITY, getImpactMeasure(2, 0, 1, 1, 0, 0), entries);
    assertNewSoftwareQualityMeasures(SoftwareQuality.SECURITY, getImpactMeasure(4, 2, 1, 1, 0, 0), entries);
  }

  private static Map<String, Long> getImpactMeasure(long total, long high, long medium, long low) {
    Map<String, Long> map = new LinkedHashMap<>();
    map.put(LOW.name(), low);
    map.put(MEDIUM.name(), medium);
    map.put(HIGH.name(), high);
    map.put(ImpactMeasureBuilder.TOTAL_KEY, total);
    return map;
  }

  private static Map<String, Long> getImpactMeasure(long total, long high, long medium, long low, long info, long blocker) {
    Map<String, Long> map = getImpactMeasure(total, high, medium, low);
    map.put(INFO.name(), info);
    map.put(BLOCKER.name(), blocker);
    return map;
  }

  private void assertOverallSoftwareQualityMeasures(SoftwareQuality softwareQuality, Map<? extends String, Long> expectedMap,
    Set<Map.Entry<String, Measure>> actualRaw) {
    assertSoftwareQualityMeasures(softwareQuality, expectedMap, actualRaw, IMPACT_TO_JSON_METRIC_KEY);
  }

  private void assertNewSoftwareQualityMeasures(SoftwareQuality softwareQuality, Map<? extends String, Long> expectedMap,
    Set<Map.Entry<String, Measure>> actualRaw) {
    assertSoftwareQualityMeasures(softwareQuality, expectedMap, actualRaw, IMPACT_TO_NEW_JSON_METRIC_KEY);
  }

  private void assertSoftwareQualityMeasures(SoftwareQuality softwareQuality, Map<? extends String, Long> expectedMap,
    Set<Map.Entry<String, Measure>> actualRaw, Map<String, String> impactToMetricMap) {

    Map.Entry<String, Measure> softwareQualityMap = actualRaw.stream()
      .filter(e -> e.getKey().equals(impactToMetricMap.get(softwareQuality.name())))
      .findFirst()
      .get();

    assertJson(softwareQualityMap.getValue().getData()).isSimilarTo(new Gson().toJson(expectedMap));
  }

  @Test
  void count_high_impact_accepted_issues() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);

    underTest.beforeComponent(FILE1);
    // created before -> existing issues with 2 high impact accepted (High and Blocker)
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, HIGH));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_RESOLVED, HIGH));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_RESOLVED, Map.of(MAINTAINABILITY, BLOCKER, RELIABILITY, HIGH)));
    underTest.onIssue(FILE1, createIssue(RESOLUTION_WONT_FIX, STATUS_RESOLVED, MEDIUM));

    // created after -> 2 high impact accepted
    underTest.onIssue(FILE1, createNewIssue(null, STATUS_OPEN, HIGH));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_RESOLVED, HIGH));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_RESOLVED, HIGH));
    underTest.onIssue(FILE1, createNewIssue(RESOLUTION_WONT_FIX, STATUS_RESOLVED, Map.of(MAINTAINABILITY, MEDIUM, RELIABILITY, HIGH)));
    underTest.onIssue(FILE1, createNewSecurityHotspot());
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertIntValue(FILE1, entry(VIOLATIONS_KEY, 2), entry(NEW_VIOLATIONS_KEY, 1), entry(NEW_ACCEPTED_ISSUES_KEY, 3),
      entry(HIGH_IMPACT_ACCEPTED_ISSUES_KEY, 5));
    assertIntValue(PROJECT, entry(VIOLATIONS_KEY, 2), entry(NEW_VIOLATIONS_KEY, 1), entry(NEW_ACCEPTED_ISSUES_KEY, 3),
      entry(HIGH_IMPACT_ACCEPTED_ISSUES_KEY, 5));
  }

  @Test
  void exclude_hotspots_from_issue_counts() {
    // bottom-up traversal -> from files to project
    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createSecurityHotspot());
    underTest.onIssue(FILE1, createSecurityHotspot());
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(FILE2);
    underTest.onIssue(FILE2, createSecurityHotspot());
    underTest.afterComponent(FILE2);

    underTest.beforeComponent(FILE3);
    underTest.afterComponent(FILE3);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertMeasures(FILE1, entry(VIOLATIONS_KEY, 0), entry(OPEN_ISSUES_KEY, 0), entry(CONFIRMED_ISSUES_KEY, 0));
    assertMeasures(FILE2, entry(VIOLATIONS_KEY, 0), entry(OPEN_ISSUES_KEY, 0), entry(CONFIRMED_ISSUES_KEY, 0));
    assertMeasures(FILE3, entry(VIOLATIONS_KEY, 0));
    assertMeasures(PROJECT, entry(VIOLATIONS_KEY, 0), entry(OPEN_ISSUES_KEY, 0), entry(CONFIRMED_ISSUES_KEY, 0));
  }

  @Test
  void exclude_new_hotspots_from_issue_counts() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);

    underTest.beforeComponent(FILE1);
    // created before -> existing issues (so ignored)
    underTest.onIssue(FILE1, createSecurityHotspot());
    underTest.onIssue(FILE1, createSecurityHotspot());

    // created after, but closed
    underTest.onIssue(FILE1, createNewSecurityHotspot().setStatus(STATUS_RESOLVED).setResolution(RESOLUTION_WONT_FIX));

    for (String severity : Arrays.asList(CRITICAL, org.sonar.api.rule.Severity.BLOCKER, MAJOR)) {
      DefaultIssue issue = createNewSecurityHotspot();
      issue.setSeverity(severity);
      underTest.onIssue(FILE1, issue);
    }
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(FILE2);
    underTest.afterComponent(FILE2);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertIntValue(FILE1, entry(NEW_VIOLATIONS_KEY, 0), entry(NEW_CRITICAL_VIOLATIONS_KEY, 0), entry(NEW_BLOCKER_VIOLATIONS_KEY, 0),
      entry(NEW_MAJOR_VIOLATIONS_KEY, 0),
      entry(NEW_VULNERABILITIES_KEY, 0));
    assertIntValue(PROJECT, entry(NEW_VIOLATIONS_KEY, 0), entry(NEW_CRITICAL_VIOLATIONS_KEY, 0), entry(NEW_BLOCKER_VIOLATIONS_KEY, 0),
      entry(NEW_MAJOR_VIOLATIONS_KEY, 0),
      entry(NEW_VULNERABILITIES_KEY, 0));
  }

  @SafeVarargs
  private void assertIntValue(Component componentRef, MapEntry<String, Integer>... entries) {
    assertThat(measureRepository.getRawMeasures(componentRef).entrySet()
      .stream()
      .filter(e -> e.getValue().getValueType() == ValueType.INT)
      .map(e -> entry(e.getKey(), e.getValue().getIntValue())))
      .contains(entries);
  }

  @SafeVarargs
  private void assertMeasures(Component componentRef, Map.Entry<String, Integer>... entries) {
    List<MeasureRepoEntry> expected = stream(entries)
      .map(e -> entryOf(e.getKey(), newMeasureBuilder().create(e.getValue())))
      .toList();

    assertThat(measureRepository.getRawMeasures(componentRef).entrySet().stream().map(e -> entryOf(e.getKey(), e.getValue())))
      .containsAll(expected);
  }

  private DefaultIssue createNewIssue(@Nullable String resolution, String status, String severity) {
    return createNewIssue(resolution, status, severity, CODE_SMELL);
  }

  private DefaultIssue createNewIssue(@Nullable String resolution, String status, Severity impactSeverity) {
    return createNewIssue(resolution, status, MAINTAINABILITY, impactSeverity);
  }

  private DefaultIssue createNewIssue(@Nullable String resolution, String status, SoftwareQuality softwareQuality,
    Severity impactSeverity) {
    DefaultIssue issue = createNewIssue(resolution, status, MAJOR, CODE_SMELL);
    issue.addImpact(softwareQuality, impactSeverity);
    return issue;
  }

  private DefaultIssue createNewIssue(@Nullable String resolution, String status, Map<SoftwareQuality, Severity> impaxts) {
    DefaultIssue issue = createNewIssue(resolution, status, MAJOR, CODE_SMELL);
    issue.replaceImpacts(impaxts);
    return issue;
  }

  private DefaultIssue createNewIssue(@Nullable String resolution, String status, String severity, RuleType ruleType) {
    DefaultIssue issue = createIssue(resolution, status, severity, ruleType);
    when(newIssueClassifier.isNew(any(), eq(issue))).thenReturn(true);
    return issue;
  }

  private static DefaultIssue createIssue(@Nullable String resolution, String status, String severity) {
    return createIssue(resolution, status, severity, CODE_SMELL);
  }

  private static DefaultIssue createIssue(@Nullable String resolution, String status, Severity impactSeverity) {
    return createIssue(resolution, status, MAINTAINABILITY, impactSeverity);
  }

  private static DefaultIssue createIssue(@Nullable String resolution, String status, SoftwareQuality softwareQuality,
    Severity impactSeverity) {
    DefaultIssue issue = createIssue(resolution, status, MAJOR, CODE_SMELL);
    issue.addImpact(softwareQuality, impactSeverity);
    return issue;
  }

  private static DefaultIssue createIssue(@Nullable String resolution, String status, Map<SoftwareQuality, Severity> impacts) {
    DefaultIssue issue = createIssue(resolution, status, MAJOR, CODE_SMELL);
    issue.replaceImpacts(impacts);
    return issue;
  }

  private static DefaultIssue createIssue(@Nullable String resolution, String status, String severity, RuleType ruleType) {
    return new DefaultIssue()
      .setKey(String.valueOf(++issueCounter))
      .setResolution(resolution).setStatus(status)
      .setSeverity(severity).setRuleKey(RuleTesting.XOO_X1)
      .setType(ruleType);
  }

  private static DefaultIssue createSecurityHotspot() {
    return createIssue(null, STATUS_OPEN, "MAJOR", SECURITY_HOTSPOT);
  }

  private DefaultIssue createNewSecurityHotspot() {
    return createNewIssue(null, STATUS_OPEN, "MAJOR", SECURITY_HOTSPOT);
  }

  @Test
  void onIssue_whenSandboxIssues_shouldCountOnlyInSandboxMetrics() {
    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, org.sonar.api.rule.Severity.BLOCKER));
    underTest.onIssue(FILE1, createIssue(null, STATUS_IN_SANDBOX, org.sonar.api.rule.Severity.BLOCKER));
    underTest.onIssue(FILE1, createIssue(null, STATUS_IN_SANDBOX, org.sonar.api.rule.Severity.MAJOR));
    underTest.onIssue(FILE1, createIssue(null, STATUS_CONFIRMED, MAJOR));
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertMeasures(FILE1, 
      entry(ISSUES_IN_SANDBOX.key(), 2),
      entry(BLOCKER_VIOLATIONS_KEY, 1), // Only non-sandbox issues
      entry(MAJOR_VIOLATIONS_KEY, 1), // Only non-sandbox issues  
      entry(VIOLATIONS_KEY, 2) // Only non-sandbox issues
    );
    assertMeasures(PROJECT,
      entry(ISSUES_IN_SANDBOX.key(), 2),
      entry(BLOCKER_VIOLATIONS_KEY, 1),
      entry(MAJOR_VIOLATIONS_KEY, 1),
      entry(VIOLATIONS_KEY, 2)
    );
  }

  @Test
  void onIssue_whenNewSandboxIssues_shouldCountOnlyInNewSandboxMetrics() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);

    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createNewIssue(null, STATUS_OPEN, org.sonar.api.rule.Severity.BLOCKER));
    underTest.onIssue(FILE1, createNewIssue(null, STATUS_IN_SANDBOX, org.sonar.api.rule.Severity.BLOCKER));
    underTest.onIssue(FILE1, createNewIssue(null, STATUS_IN_SANDBOX, MAJOR, BUG));
    underTest.onIssue(FILE1, createNewIssue(null, STATUS_CONFIRMED, MAJOR, BUG));
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertMeasures(FILE1,
      entry(NEW_ISSUES_IN_SANDBOX.key(), 2),
      entry(NEW_BLOCKER_VIOLATIONS_KEY, 1), // Only non-sandbox new issues
      entry(NEW_MAJOR_VIOLATIONS_KEY, 1), // Only non-sandbox new issues
      entry(NEW_VIOLATIONS_KEY, 2), // Only non-sandbox new issues
      entry(NEW_BUGS_KEY, 1) // Only non-sandbox new issues
    );
    assertMeasures(PROJECT,
      entry(NEW_ISSUES_IN_SANDBOX.key(), 2),
      entry(NEW_BLOCKER_VIOLATIONS_KEY, 1),
      entry(NEW_MAJOR_VIOLATIONS_KEY, 1),
      entry(NEW_VIOLATIONS_KEY, 2),
      entry(NEW_BUGS_KEY, 1)
    );
  }

  @Test
  void onIssue_whenSandboxIssues_shouldExcludeFromSoftwareQualityMeasures() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);

    underTest.beforeComponent(FILE1);
    underTest.onIssue(FILE1, createIssue(null, STATUS_OPEN, MAINTAINABILITY, HIGH));
    underTest.onIssue(FILE1, createIssue(null, STATUS_IN_SANDBOX, MAINTAINABILITY, HIGH));
    underTest.onIssue(FILE1, createIssue(null, STATUS_IN_SANDBOX, SECURITY, BLOCKER));
    underTest.onIssue(FILE1, createNewIssue(null, STATUS_OPEN, RELIABILITY, MEDIUM));
    underTest.onIssue(FILE1, createNewIssue(null, STATUS_IN_SANDBOX, RELIABILITY, MEDIUM));
    underTest.afterComponent(FILE1);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);

    assertMeasures(FILE1,
      entry(ISSUES_IN_SANDBOX.key(), 3),
      entry(NEW_ISSUES_IN_SANDBOX.key(), 1),
      entry(SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES.key(), 1), // Only non-sandbox issues
      entry(SOFTWARE_QUALITY_SECURITY_ISSUES.key(), 0), // Sandbox issue not counted
      entry(SOFTWARE_QUALITY_RELIABILITY_ISSUES.key(), 1), // Only non-sandbox new issues
      entry(NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES.key(), 1) // Only non-sandbox new issues
    );
    assertMeasures(PROJECT,
      entry(ISSUES_IN_SANDBOX.key(), 3),
      entry(NEW_ISSUES_IN_SANDBOX.key(), 1),
      entry(SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES.key(), 1),
      entry(SOFTWARE_QUALITY_SECURITY_ISSUES.key(), 0),
      entry(SOFTWARE_QUALITY_RELIABILITY_ISSUES.key(), 1),
      entry(NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES.key(), 1)
    );
  }
}
