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
package org.sonar.batch.phases;

import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.batch.events.BatchStepEvent;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.issue.ignore.scanner.IssueExclusionsLoader;
import org.sonar.batch.issue.tracking.LocalIssueTracking;
import org.sonar.batch.rule.QProfileVerifier;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.batch.scan.filesystem.FileSystemLogger;
import org.sonar.batch.scan.maven.MavenPluginsConfigurator;
import org.sonar.batch.scan.report.IssuesReports;

public final class PreviewPhaseExecutor implements PhaseExecutor {

  private final EventBus eventBus;
  private final Phases phases;
  private final MavenPluginsConfigurator mavenPluginsConfigurator;
  private final InitializersExecutor initializersExecutor;
  private final SensorsExecutor sensorsExecutor;
  private final SensorContext sensorContext;
  private final DefaultIndex index;
  private final ProjectInitializer pi;
  private final FileSystemLogger fsLogger;
  private final DefaultModuleFileSystem fs;
  private final QProfileVerifier profileVerifier;
  private final IssueExclusionsLoader issueExclusionsLoader;
  private final IssuesReports issuesReport;
  private final LocalIssueTracking localIssueTracking;

  public PreviewPhaseExecutor(Phases phases,
    MavenPluginsConfigurator mavenPluginsConfigurator, InitializersExecutor initializersExecutor,
    SensorsExecutor sensorsExecutor,
    SensorContext sensorContext, DefaultIndex index,
    EventBus eventBus, ProjectInitializer pi, FileSystemLogger fsLogger, IssuesReports jsonReport, DefaultModuleFileSystem fs, QProfileVerifier profileVerifier,
    IssueExclusionsLoader issueExclusionsLoader, LocalIssueTracking localIssueTracking) {
    this.phases = phases;
    this.mavenPluginsConfigurator = mavenPluginsConfigurator;
    this.initializersExecutor = initializersExecutor;
    this.sensorsExecutor = sensorsExecutor;
    this.sensorContext = sensorContext;
    this.index = index;
    this.eventBus = eventBus;
    this.pi = pi;
    this.fsLogger = fsLogger;
    this.issuesReport = jsonReport;
    this.fs = fs;
    this.profileVerifier = profileVerifier;
    this.issueExclusionsLoader = issueExclusionsLoader;
    this.localIssueTracking = localIssueTracking;
  }

  /**
   * Executed on each module
   */
  @Override
  public void execute(Project module) {
    pi.execute(module);

    eventBus.fireEvent(new ProjectAnalysisEvent(module, true));

    executeMavenPhase(module);

    executeInitializersPhase();

    if (phases.isEnabled(Phases.Phase.SENSOR)) {
      // Index and lock the filesystem
      indexFs();

      // Log detected languages and their profiles after FS is indexed and languages detected
      profileVerifier.execute();

      // Initialize issue exclusions
      initIssueExclusions();

      sensorsExecutor.execute(sensorContext);
    }

    if (module.isRoot()) {

      localIssueTracking();

      issuesReport();
    }

    cleanMemory();
    eventBus.fireEvent(new ProjectAnalysisEvent(module, false));
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

  private void initIssueExclusions() {
    String stepName = "Init issue exclusions";
    eventBus.fireEvent(new BatchStepEvent(stepName, true));
    issueExclusionsLoader.execute();
    eventBus.fireEvent(new BatchStepEvent(stepName, false));
  }

  private void indexFs() {
    String stepName = "Index filesystem and store sources";
    eventBus.fireEvent(new BatchStepEvent(stepName, true));
    fs.index();
    eventBus.fireEvent(new BatchStepEvent(stepName, false));
  }

  private void executeInitializersPhase() {
    if (phases.isEnabled(Phases.Phase.INIT)) {
      initializersExecutor.execute();
      fsLogger.log();
    }
  }

  private void executeMavenPhase(Project module) {
    if (phases.isEnabled(Phases.Phase.MAVEN)) {
      eventBus.fireEvent(new MavenPhaseEvent(true));
      mavenPluginsConfigurator.execute(module);
      eventBus.fireEvent(new MavenPhaseEvent(false));
    }
  }

  private void cleanMemory() {
    String cleanMemory = "Clean memory";
    eventBus.fireEvent(new BatchStepEvent(cleanMemory, true));
    index.clear();
    eventBus.fireEvent(new BatchStepEvent(cleanMemory, false));
  }
}
