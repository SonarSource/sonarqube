/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.phases;

import com.google.common.collect.Lists;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.index.PersistenceManager;
import org.sonar.batch.scan.filesystem.FileSystemLogger;
import org.sonar.batch.scan.maven.MavenPhaseExecutor;
import org.sonar.batch.scan.maven.MavenPluginsConfigurator;

import java.util.Collection;

public final class PhaseExecutor {

  public static Collection<Class> getPhaseClasses() {
    return Lists.<Class>newArrayList(DecoratorsExecutor.class, MavenPhaseExecutor.class, MavenPluginsConfigurator.class,
      PostJobsExecutor.class, SensorsExecutor.class,
      InitializersExecutor.class, ProjectInitializer.class, UpdateStatusJob.class);
  }

  private EventBus eventBus;
  private Phases phases;
  private DecoratorsExecutor decoratorsExecutor;
  private MavenPhaseExecutor mavenPhaseExecutor;
  private MavenPluginsConfigurator mavenPluginsConfigurator;
  private PostJobsExecutor postJobsExecutor;
  private InitializersExecutor initializersExecutor;
  private SensorsExecutor sensorsExecutor;
  private UpdateStatusJob updateStatusJob;
  private PersistenceManager persistenceManager;
  private SensorContext sensorContext;
  private DefaultIndex index;
  private ProjectInitializer pi;
  private ScanPersister[] persisters;
  private FileSystemLogger fsLogger;

  public PhaseExecutor(Phases phases, DecoratorsExecutor decoratorsExecutor, MavenPhaseExecutor mavenPhaseExecutor,
                       MavenPluginsConfigurator mavenPluginsConfigurator, InitializersExecutor initializersExecutor,
                       PostJobsExecutor postJobsExecutor, SensorsExecutor sensorsExecutor,
                       PersistenceManager persistenceManager, SensorContext sensorContext, DefaultIndex index,
                       EventBus eventBus, UpdateStatusJob updateStatusJob, ProjectInitializer pi,
                       ScanPersister[] persisters, FileSystemLogger fsLogger) {
    this.phases = phases;
    this.decoratorsExecutor = decoratorsExecutor;
    this.mavenPhaseExecutor = mavenPhaseExecutor;
    this.mavenPluginsConfigurator = mavenPluginsConfigurator;
    this.postJobsExecutor = postJobsExecutor;
    this.initializersExecutor = initializersExecutor;
    this.sensorsExecutor = sensorsExecutor;
    this.persistenceManager = persistenceManager;
    this.sensorContext = sensorContext;
    this.index = index;
    this.eventBus = eventBus;
    this.updateStatusJob = updateStatusJob;
    this.pi = pi;
    this.persisters = persisters;
    this.fsLogger = fsLogger;
  }

  public PhaseExecutor(Phases phases, DecoratorsExecutor decoratorsExecutor, MavenPhaseExecutor mavenPhaseExecutor,
                       MavenPluginsConfigurator mavenPluginsConfigurator, InitializersExecutor initializersExecutor,
                       PostJobsExecutor postJobsExecutor, SensorsExecutor sensorsExecutor,
                       PersistenceManager persistenceManager, SensorContext sensorContext, DefaultIndex index,
                       EventBus eventBus, ProjectInitializer pi, ScanPersister[] persisters, FileSystemLogger fsLogger) {
    this(phases, decoratorsExecutor, mavenPhaseExecutor, mavenPluginsConfigurator, initializersExecutor, postJobsExecutor,
      sensorsExecutor, persistenceManager, sensorContext, index, eventBus, null, pi, persisters, fsLogger);
  }

  /**
   * Executed on each module
   */
  public void execute(Project module) {
    pi.execute(module);
    eventBus.fireEvent(new ProjectAnalysisEvent(module, true));
    if (phases.isEnabled(Phases.Phase.MAVEN)) {
      mavenPluginsConfigurator.execute(module);
      mavenPhaseExecutor.execute(module);
    }
    if (phases.isEnabled(Phases.Phase.INIT)) {
      initializersExecutor.execute();
      fsLogger.log();
    }

    persistenceManager.setDelayedMode(true);
    if (phases.isEnabled(Phases.Phase.SENSOR)) {
      sensorsExecutor.execute(sensorContext);
    }
    if (phases.isEnabled(Phases.Phase.DECORATOR)) {
      decoratorsExecutor.execute();
    }
    persistenceManager.dump();
    persistenceManager.setDelayedMode(false);

    if (module.isRoot()) {
      for (ScanPersister persister : persisters) {
        persister.persist();
      }
      if (updateStatusJob != null) {
        updateStatusJob.execute();
      }
      if (phases.isEnabled(Phases.Phase.POSTJOB)) {
        postJobsExecutor.execute(sensorContext);
      }
    }
    cleanMemory();
    eventBus.fireEvent(new ProjectAnalysisEvent(module, false));
  }

  private void cleanMemory() {
    persistenceManager.clear();
    index.clear();
  }
}
