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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.resources.Project;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.events.BatchStepEvent;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.batch.index.ScanPersister;
import org.sonar.batch.issue.ignore.scanner.IssueExclusionsLoader;
import org.sonar.batch.report.PublishReportJob;
import org.sonar.batch.rule.QProfileVerifier;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.batch.scan.filesystem.FileSystemLogger;
import org.sonar.batch.scan.maven.MavenPluginsConfigurator;
import org.sonar.batch.scan.report.JsonReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class DefaultPhaseExecutor implements PhaseExecutor {

  public static final Logger LOGGER = LoggerFactory.getLogger(DefaultPhaseExecutor.class);

  private final EventBus eventBus;
  private final Phases phases;
  private final DecoratorsExecutor decoratorsExecutor;
  private final MavenPluginsConfigurator mavenPluginsConfigurator;
  private final PostJobsExecutor postJobsExecutor;
  private final InitializersExecutor initializersExecutor;
  private final SensorsExecutor sensorsExecutor;
  private final PublishReportJob publishReportJob;
  private final SensorContext sensorContext;
  private final DefaultIndex index;
  private final ProjectInitializer pi;
  private final ScanPersister[] persisters;
  private final FileSystemLogger fsLogger;
  private final JsonReport jsonReport;
  private final DefaultModuleFileSystem fs;
  private final QProfileVerifier profileVerifier;
  private final IssueExclusionsLoader issueExclusionsLoader;
  private final AnalysisMode analysisMode;
  private final DatabaseSession session;
  private final ResourcePersister resourcePersister;

  public DefaultPhaseExecutor(Phases phases, DecoratorsExecutor decoratorsExecutor,
    MavenPluginsConfigurator mavenPluginsConfigurator, InitializersExecutor initializersExecutor,
    PostJobsExecutor postJobsExecutor, SensorsExecutor sensorsExecutor,
    SensorContext sensorContext, DefaultIndex index,
    EventBus eventBus, PublishReportJob publishReportJob, ProjectInitializer pi,
    ScanPersister[] persisters, FileSystemLogger fsLogger, JsonReport jsonReport, DefaultModuleFileSystem fs, QProfileVerifier profileVerifier,
    IssueExclusionsLoader issueExclusionsLoader, AnalysisMode analysisMode, DatabaseSession session, ResourcePersister resourcePersister) {
    this.phases = phases;
    this.decoratorsExecutor = decoratorsExecutor;
    this.mavenPluginsConfigurator = mavenPluginsConfigurator;
    this.postJobsExecutor = postJobsExecutor;
    this.initializersExecutor = initializersExecutor;
    this.sensorsExecutor = sensorsExecutor;
    this.sensorContext = sensorContext;
    this.index = index;
    this.eventBus = eventBus;
    this.publishReportJob = publishReportJob;
    this.pi = pi;
    this.persisters = persisters;
    this.fsLogger = fsLogger;
    this.jsonReport = jsonReport;
    this.fs = fs;
    this.profileVerifier = profileVerifier;
    this.issueExclusionsLoader = issueExclusionsLoader;
    this.analysisMode = analysisMode;
    this.session = session;
    this.resourcePersister = resourcePersister;
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

      // SONAR-2965 In case the sensor takes too much time we close the session to not face a timeout
      session.commitAndClose();
      sensorsExecutor.execute(sensorContext);
    }

    // Special case for views.
    resourcePersister.persist();

    if (phases.isEnabled(Phases.Phase.DECORATOR)) {
      decoratorsExecutor.execute();
    }

    if (module.isRoot()) {
      jsonReport.execute();

      executePersisters();
      publishReportJob();
      if (phases.isEnabled(Phases.Phase.POSTJOB)) {
        postJobsExecutor.execute(sensorContext);
      }
    }
    cleanMemory();
    eventBus.fireEvent(new ProjectAnalysisEvent(module, false));
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

  private void executePersisters() {
    if (!analysisMode.isPreview()) {
      LOGGER.info("Store results in database");
      List<ScanPersister> sortedPersisters = sortedPersisters();
      eventBus.fireEvent(new PersistersPhaseEvent(sortedPersisters, true));
      for (ScanPersister persister : sortedPersisters) {
        LOGGER.debug("Execute {}", persister.getClass().getName());
        eventBus.fireEvent(new PersisterExecutionEvent(persister, true));
        persister.persist();
        eventBus.fireEvent(new PersisterExecutionEvent(persister, false));
      }

      eventBus.fireEvent(new PersistersPhaseEvent(sortedPersisters, false));
    }
  }

  List<ScanPersister> sortedPersisters() {
    // Sort by reverse name so that ResourcePersister is executed before MeasurePersister
    List<ScanPersister> sortedPersisters = new ArrayList<>(Arrays.asList(persisters));
    Collections.sort(sortedPersisters, new Comparator<ScanPersister>() {
      @Override
      public int compare(ScanPersister o1, ScanPersister o2) {
        return o2.getClass().getName().compareTo(o1.getClass().getName());
      }
    });
    return sortedPersisters;
  }

  private void publishReportJob() {
    String stepName = "Publish report";
    eventBus.fireEvent(new BatchStepEvent(stepName, true));
    this.publishReportJob.execute();
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
