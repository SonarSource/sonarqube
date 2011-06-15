/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import java.util.Collection;
import java.util.List;

public final class Phases {

  public static Collection<Class> getPhaseClasses(boolean dryRun) {
    List<Class> classes = Lists.<Class>newArrayList(DecoratorsExecutor.class, MavenPhaseExecutor.class, MavenPluginsConfigurator.class,
        PostJobsExecutor.class, SensorsExecutor.class,
        InitializersExecutor.class);
    if (!dryRun) {
      classes.add(UpdateStatusJob.class);
    }
    return classes;
  }

  private EventBus eventBus;
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

  public Phases(DecoratorsExecutor decoratorsExecutor, MavenPhaseExecutor mavenPhaseExecutor,
                MavenPluginsConfigurator mavenPluginsConfigurator, InitializersExecutor initializersExecutor,
                PostJobsExecutor postJobsExecutor, SensorsExecutor sensorsExecutor,
                PersistenceManager persistenceManager, SensorContext sensorContext, DefaultIndex index,
                EventBus eventBus, UpdateStatusJob updateStatusJob) {
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
  }

  public Phases(DecoratorsExecutor decoratorsExecutor, MavenPhaseExecutor mavenPhaseExecutor,
                MavenPluginsConfigurator mavenPluginsConfigurator, InitializersExecutor initializersExecutor,
                PostJobsExecutor postJobsExecutor, SensorsExecutor sensorsExecutor,
                PersistenceManager persistenceManager, SensorContext sensorContext, DefaultIndex index,
                EventBus eventBus) {
    this(decoratorsExecutor, mavenPhaseExecutor, mavenPluginsConfigurator, initializersExecutor, postJobsExecutor,
        sensorsExecutor, persistenceManager, sensorContext, index, eventBus, null);
  }

  /**
   * Executed on each module
   */
  public void execute(Project project) {
    eventBus.fireEvent(new ProjectAnalysisEvent(project, true));
    mavenPluginsConfigurator.execute(project);
    mavenPhaseExecutor.execute(project);
    initializersExecutor.execute();

    persistenceManager.setDelayedMode(true);
    sensorsExecutor.execute(sensorContext);
    decoratorsExecutor.execute();
    persistenceManager.dump();
    persistenceManager.setDelayedMode(false);

    if (project.isRoot()) {
      if (updateStatusJob != null) {
        updateStatusJob.execute();
      }
      postJobsExecutor.execute(sensorContext);
    }
    cleanMemory();
    eventBus.fireEvent(new ProjectAnalysisEvent(project, false));
  }

  private void cleanMemory() {
    persistenceManager.clear();
    index.clear();
  }
}
