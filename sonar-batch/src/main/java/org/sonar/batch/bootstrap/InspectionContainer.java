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
package org.sonar.batch.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.batch.DefaultProfileLoader;
import org.sonar.batch.DefaultProjectClasspath;
import org.sonar.batch.DefaultProjectFileSystem2;
import org.sonar.batch.DefaultSensorContext;
import org.sonar.batch.DefaultTimeMachine;
import org.sonar.batch.ProfileProvider;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.ResourceFilters;
import org.sonar.batch.ViolationFilters;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.batch.local.DryRunExporter;
import org.sonar.batch.phases.Phases;
import org.sonar.batch.phases.PhasesTimeProfiler;
import org.sonar.core.qualitymodel.DefaultModelFinder;
import org.sonar.jpa.dao.ProfilesDao;
import org.sonar.jpa.dao.RulesDao;

public class InspectionContainer extends Container {
  private static final Logger LOG = LoggerFactory.getLogger(InspectionContainer.class);
  private Project project;

  public InspectionContainer(Project project) {
    this.project = project;
  }

  @Override
  protected void configure() {
    logSettings();
    addCoreComponents();
    addPluginExtensions();
  }

  private void addCoreComponents() {
    ProjectDefinition projectDefinition = container.getComponentByType(ProjectTree.class).getProjectDefinition(project);
    container.addSingleton(projectDefinition);
    container.addSingleton(project.getConfiguration());
    container.addSingleton(project);
    container.addSingleton(ProjectSettings.class);

    // hack to initialize commons-configuration before ExtensionProviders
    container.getComponentByType(ProjectSettings.class);

    container.addSingleton(EventBus.class);
    container.addSingleton(Phases.class);
    container.addSingleton(PhasesTimeProfiler.class);
    for (Class clazz : Phases.getPhaseClasses()) {
      container.addSingleton(clazz);
    }
    container.addSingleton(UnsupportedProperties.class);

    for (Object component : projectDefinition.getContainerExtensions()) {
      container.addSingleton(component);
    }
    container.addSingleton(Languages.class);
    container.addSingleton(DefaultProjectClasspath.class);
    container.addSingleton(DefaultProjectFileSystem2.class);
    container.addSingleton(RulesDao.class);

    // the Snapshot component will be removed when asynchronous measures are improved (required for AsynchronousMeasureSensor)
    container.addSingleton(container.getComponentByType(ResourcePersister.class).getSnapshot(project));

    container.addSingleton(TimeMachineConfiguration.class);
    container.addSingleton(org.sonar.api.database.daos.MeasuresDao.class);
    container.addSingleton(ProfilesDao.class);
    container.addSingleton(DefaultSensorContext.class);
    container.addSingleton(BatchExtensionDictionnary.class);
    container.addSingleton(DefaultTimeMachine.class);
    container.addSingleton(ViolationFilters.class);
    container.addSingleton(ResourceFilters.class);
    container.addSingleton(DefaultModelFinder.class);
    container.addSingleton(DefaultProfileLoader.class);
    container.addSingleton(DryRunExporter.class);
    container.addPicoAdapter(new ProfileProvider());
  }

  private void addPluginExtensions() {
    ExtensionInstaller installer = container.getComponentByType(ExtensionInstaller.class);
    installer.installInspectionExtensions(container);
  }

  private void logSettings() {
    LOG.info("-------------  Inspecting {}", project.getName());
  }

  /**
   * Analyze project
   */
  @Override
  protected void doStart() {
    DefaultIndex index = container.getComponentByType(DefaultIndex.class);
    index.setCurrentProject(project,
        container.getComponentByType(ResourceFilters.class),
        container.getComponentByType(ViolationFilters.class),
        container.getComponentByType(RulesProfile.class));

    container.getComponentByType(Phases.class).execute(project);
  }
}
