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
package org.sonar.batch;

import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.DefaultProjectFileSystem;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.rules.DefaultRulesManager;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrap.BatchPluginRepository;
import org.sonar.batch.components.PastViolationsLoader;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.index.DefaultResourcePersister;
import org.sonar.batch.phases.Phases;
import org.sonar.batch.phases.PhasesTimeProfiler;
import org.sonar.core.components.DefaultModelFinder;
import org.sonar.jpa.dao.AsyncMeasuresDao;
import org.sonar.jpa.dao.AsyncMeasuresService;
import org.sonar.jpa.dao.DaoFacade;
import org.sonar.jpa.dao.ProfilesDao;
import org.sonar.jpa.dao.RulesDao;

public class ProjectBatch {

  private Module globalComponents;

  public ProjectBatch(Module globalComponents) {
    this.globalComponents = globalComponents;
  }

  public void execute(DefaultIndex index, Project project) {
    Module projectComponents = null;
    try {
      projectComponents = startChildContainer(index, project);

      projectComponents.getComponent(Phases.class).execute(project);

    } finally {
      if (projectComponents != null) {
        try {
          globalComponents.uninstallChild(projectComponents);
          projectComponents.stop();
        } catch (Exception e) {
          // do not log
        }
      }
    }
  }

  public Module startChildContainer(DefaultIndex index, Project project) {
    Module projectComponents = globalComponents.installChild(new ProjectComponents(project));
    projectComponents.install(new ProjectCoreComponents());
    projectComponents.start();

    // post-initializations

    Language language = projectComponents.getComponent(Languages.class).get(project.getLanguageKey());
    if (language == null) {
      throw new SonarException("Language with key '" + project.getLanguageKey() + "' not found");
    }
    project.setLanguage(language);

    index.setCurrentProject(project,
        projectComponents.getComponent(ResourceFilters.class),
        projectComponents.getComponent(ViolationFilters.class),
        projectComponents.getComponent(RulesProfile.class));

    // TODO See http://jira.codehaus.org/browse/SONAR-2126
    // previously MavenProjectBuilder was responsible for creation of ProjectFileSystem
    project.setFileSystem(projectComponents.getComponent(ProjectFileSystem.class));

    return projectComponents;
  }

  private static class ProjectComponents extends Module {
    private Project project;

    public ProjectComponents(Project project) {
      this.project = project;
    }

    @Override
    protected void configure() {
      addComponent(project);
      addComponent(project.getPom());
      addComponent(ProjectClasspath.class);
      addComponent(DefaultProjectFileSystem.class);
      addComponent(project.getConfiguration());

      // need to be registered after the Configuration
      getComponent(BatchPluginRepository.class).registerPlugins(getContainer());

      addComponent(DaoFacade.class);
      addComponent(RulesDao.class);

      // the Snapshot component will be removed when asynchronous measures are improved (required for AsynchronousMeasureSensor)
      addComponent(getComponent(DefaultResourcePersister.class).getSnapshot(project));

      addComponent(org.sonar.api.database.daos.MeasuresDao.class);
      addComponent(ProfilesDao.class);
      addComponent(AsyncMeasuresDao.class);
      addComponent(AsyncMeasuresService.class);
      addComponent(DefaultRulesManager.class);
      addComponent(DefaultSensorContext.class);
      addComponent(Languages.class);
      addComponent(BatchExtensionDictionnary.class);
      addComponent(DefaultTimeMachine.class);
      addComponent(ViolationFilters.class);
      addComponent(ResourceFilters.class);
      addComponent(DefaultModelFinder.class);
      addComponent(TimeMachineConfiguration.class);
      addComponent(PastViolationsLoader.class);
      addComponent(ProfileLoader.class, DefaultProfileLoader.class);

      addAdapter(new ProfileProvider());
      addAdapter(new CheckProfileProvider());
    }
  }

  private static class ProjectCoreComponents extends Module {
    @Override
    protected void configure() {
      addComponent(Phases.class);
      addComponent(PhasesTimeProfiler.class);
      for (Class clazz : Phases.getPhaseClasses()) {
        addComponent(clazz);
      }
      for (Metric metric : CoreMetrics.getMetrics()) {
        addComponent(metric.getKey(), metric);
      }
      for (Metrics metricRepo : getComponents(Metrics.class)) {
        for (Metric metric : metricRepo.getMetrics()) {
          addComponent(metric.getKey(), metric);
        }
      }
    }
  }

}
