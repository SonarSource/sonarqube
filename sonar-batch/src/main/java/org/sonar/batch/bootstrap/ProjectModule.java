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
package org.sonar.batch.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.rules.DefaultRulesManager;
import org.sonar.api.utils.IocContainer;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.*;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.batch.config.ProjectSettings;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.index.DefaultResourcePersister;
import org.sonar.batch.phases.Phases;
import org.sonar.batch.phases.PhasesTimeProfiler;
import org.sonar.core.qualitymodel.DefaultModelFinder;
import org.sonar.jpa.dao.DaoFacade;
import org.sonar.jpa.dao.ProfilesDao;
import org.sonar.jpa.dao.RulesDao;

public class ProjectModule extends Module {
  private static final Logger LOG = LoggerFactory.getLogger(ProjectModule.class);
  private Project project;
  private boolean dryRun;

  public ProjectModule(Project project, boolean dryRun) {
    this.project = project;
    this.dryRun = dryRun;
  }

  @Override
  protected void configure() {
    logSettings();
    addCoreComponents();
    addProjectComponents();
    addProjectPluginExtensions();
  }


  private void addProjectComponents() {
    ProjectDefinition projectDefinition = getComponentByType(ProjectTree.class).getProjectDefinition(project);
    addCoreSingleton(projectDefinition);
    addCoreSingleton(project);
    addCoreSingleton(project.getConfiguration());
    addCoreSingleton(ProjectSettings.class);
    addCoreSingleton(IocContainer.class);

    for (Object component : projectDefinition.getContainerExtensions()) {
      addCoreSingleton(component);
    }
    addCoreSingleton(DefaultProjectClasspath.class);
    addCoreSingleton(DefaultProjectFileSystem2.class);
    addCoreSingleton(DaoFacade.class);
    addCoreSingleton(RulesDao.class);

    if (!dryRun) {
      // the Snapshot component will be removed when asynchronous measures are improved (required for AsynchronousMeasureSensor)
      addCoreSingleton(getComponentByType(DefaultResourcePersister.class).getSnapshot(project));
    }
    addCoreSingleton(TimeMachineConfiguration.class);
    addCoreSingleton(org.sonar.api.database.daos.MeasuresDao.class);
    addCoreSingleton(ProfilesDao.class);
    addCoreSingleton(DefaultRulesManager.class);
    addCoreSingleton(DefaultSensorContext.class);
    addCoreSingleton(Languages.class);
    addCoreSingleton(BatchExtensionDictionnary.class);
    addCoreSingleton(DefaultTimeMachine.class);
    addCoreSingleton(ViolationFilters.class);
    addCoreSingleton(ResourceFilters.class);
    addCoreSingleton(DefaultModelFinder.class);
    addCoreSingleton(DefaultProfileLoader.class);
    addAdapter(new ProfileProvider());
  }

  private void addCoreComponents() {
    addCoreSingleton(EventBus.class);
    addCoreSingleton(Phases.class);
    addCoreSingleton(PhasesTimeProfiler.class);
    for (Class clazz : Phases.getPhaseClasses(dryRun)) {
      addCoreSingleton(clazz);
    }
  }

  private void addProjectPluginExtensions() {
    addCoreSingleton(ProjectExtensionInstaller.class);
    ProjectExtensionInstaller installer = getComponentByType(ProjectExtensionInstaller.class);
    installer.install(this);
  }


  private void logSettings() {
    // TODO move these logs in a dedicated component
    LOG.info("-------------  Analyzing {}", project.getName());
  }

  /**
   * Analyze project
   */
  @Override
  protected void doStart() {
    Language language = getComponentByType(Languages.class).get(project.getLanguageKey());
    if (language == null) {
      throw new SonarException("Language with key '" + project.getLanguageKey() + "' not found");
    }
    project.setLanguage(language);

    DefaultIndex index = getComponentByType(DefaultIndex.class);
    index.setCurrentProject(project,
      getComponentByType(ResourceFilters.class),
      getComponentByType(ViolationFilters.class),
      getComponentByType(RulesProfile.class));

    // TODO See http://jira.codehaus.org/browse/SONAR-2126
    // previously MavenProjectBuilder was responsible for creation of ProjectFileSystem
    project.setFileSystem(getComponentByType(ProjectFileSystem.class));

    getComponentByType(Phases.class).execute(project);
  }
}
