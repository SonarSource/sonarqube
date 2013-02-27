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

import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileExclusions;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.batch.DefaultProfileLoader;
import org.sonar.batch.DefaultProjectClasspath;
import org.sonar.batch.DefaultSensorContext;
import org.sonar.batch.DefaultTimeMachine;
import org.sonar.batch.ResourceFilters;
import org.sonar.batch.ViolationFilters;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.local.DryRunExporter;
import org.sonar.batch.phases.Phases;
import org.sonar.batch.phases.PhasesTimeProfiler;
import org.sonar.batch.scan.LastSnapshots;
import org.sonar.batch.scan.filesystem.DeprecatedFileSystemAdapter;
import org.sonar.batch.scan.filesystem.ExclusionFilters;
import org.sonar.batch.scan.filesystem.FileSystemLogger;
import org.sonar.batch.scan.filesystem.LanguageFilters;
import org.sonar.batch.scan.filesystem.ModuleFileSystemProvider;
import org.sonar.jpa.dao.ProfilesDao;
import org.sonar.jpa.dao.RulesDao;

public abstract class ModuleContainer extends Container {

  private Project project;
  private ProjectDefinition projectDefinition;
  private Snapshot snapshot;

  public ModuleContainer(Project project, ProjectDefinition projectDefinition, Snapshot snapshot) {
    this.project = project;
    this.projectDefinition = projectDefinition;
    this.snapshot = snapshot;
  }

  @Override
  protected void configure() {
    addCoreComponents();
    addPluginExtensions();
  }

  private void addCoreComponents() {
    container.addSingleton(projectDefinition);
    container.addSingleton(project.getConfiguration());
    container.addSingleton(project);
    for (Object component : projectDefinition.getContainerExtensions()) {
      container.addSingleton(component);
    }
    // the Snapshot component will be removed when asynchronous measures are improved (required for AsynchronousMeasureSensor)
    container.addSingleton(snapshot);

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

    container.addSingleton(RulesDao.class);
    container.addSingleton(LastSnapshots.class);
    container.addSingleton(Languages.class);

    // file system
    container.addSingleton(PathResolver.class);
    container.addSingleton(FileExclusions.class);
    container.addSingleton(LanguageFilters.class);
    container.addSingleton(ExclusionFilters.class);
    container.addSingleton(DefaultProjectClasspath.class);
    container.addPicoAdapter(new ModuleFileSystemProvider());
    container.addSingleton(DeprecatedFileSystemAdapter.class);
    container.addSingleton(FileSystemLogger.class);

    container.addSingleton(TimeMachineConfiguration.class);
    container.addSingleton(org.sonar.api.database.daos.MeasuresDao.class);
    container.addSingleton(ProfilesDao.class);
    container.addSingleton(DefaultSensorContext.class);
    container.addSingleton(BatchExtensionDictionnary.class);
    container.addSingleton(DefaultTimeMachine.class);
    container.addSingleton(ViolationFilters.class);
    container.addSingleton(ResourceFilters.class);
    container.addSingleton(DefaultProfileLoader.class);
    container.addSingleton(DryRunExporter.class);
  }

  private void addPluginExtensions() {
    ExtensionInstaller installer = container.getComponentByType(ExtensionInstaller.class);
    installer.installInspectionExtensions(container);
  }

}
