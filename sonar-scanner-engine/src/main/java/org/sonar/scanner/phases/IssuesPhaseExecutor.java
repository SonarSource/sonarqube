/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.phases;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.scanner.events.BatchStepEvent;
import org.sonar.scanner.events.EventBus;
import org.sonar.scanner.issue.ignore.scanner.IssueExclusionsLoader;
import org.sonar.scanner.issue.tracking.IssueTransition;
import org.sonar.scanner.rule.QProfileVerifier;
import org.sonar.scanner.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.scanner.scan.filesystem.FileIndexer;
import org.sonar.scanner.scan.report.IssuesReports;

public final class IssuesPhaseExecutor extends AbstractPhaseExecutor {

  private static final Logger LOG = LoggerFactory.getLogger(IssuesPhaseExecutor.class);

  private final EventBus eventBus;
  private final IssuesReports issuesReport;
  private final IssueTransition localIssueTracking;

  public IssuesPhaseExecutor(InitializersExecutor initializersExecutor, PostJobsExecutor postJobsExecutor, SensorsExecutor sensorsExecutor, SensorContext sensorContext,
    EventBus eventBus, IssuesReports jsonReport, DefaultModuleFileSystem fs, QProfileVerifier profileVerifier,
    IssueExclusionsLoader issueExclusionsLoader, IssueTransition localIssueTracking, InputModuleHierarchy moduleHierarchy, FileIndexer fileIndexer,
    CoverageExclusions coverageExclusions) {
    super(initializersExecutor, postJobsExecutor, sensorsExecutor, sensorContext, moduleHierarchy, eventBus, fs, profileVerifier, issueExclusionsLoader, fileIndexer,
      coverageExclusions);
    this.eventBus = eventBus;
    this.issuesReport = jsonReport;
    this.localIssueTracking = localIssueTracking;
  }

  @Override
  protected void executeOnRoot() {
    localIssueTracking();
    issuesReport();
    LOG.info("ANALYSIS SUCCESSFUL");
  }

  private void localIssueTracking() {
    String stepName = "Local Issue Tracking";
    eventBus.fireEvent(new BatchStepEvent(stepName, true));
    localIssueTracking.execute();
    eventBus.fireEvent(new BatchStepEvent(stepName, false));
  }

  private void issuesReport() {
    String stepName = "Issues Reports";
    eventBus.fireEvent(new BatchStepEvent(stepName, true));
    issuesReport.execute();
    eventBus.fireEvent(new BatchStepEvent(stepName, false));
  }

}
