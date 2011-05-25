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

import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.rules.DefaultRulesManager;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.*;
import org.sonar.batch.bootstrapper.ProjectDefinition;
import org.sonar.batch.components.PastViolationsLoader;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.index.DefaultResourcePersister;
import org.sonar.batch.phases.Phases;
import org.sonar.batch.phases.PhasesTimeProfiler;
import org.sonar.core.components.DefaultModelFinder;
import org.sonar.jpa.dao.*;

import java.util.Arrays;

public class ProjectModule extends Module {
  private static final Logger LOG = LoggerFactory.getLogger(ProjectModule.class);
  private Project project;

  public ProjectModule(Project project) {
    this.project = project;
  }

  @Override
  protected void configure() {
    logSettings();
    addCoreComponents();
    addProjectComponents();
    addProjectPluginExtensions();
  }


  private void addProjectComponents() {
    ProjectDefinition projectDefinition = getComponent(ProjectTree.class).getProjectDefinition(project);
    addComponent(projectDefinition);
    for (Object component : projectDefinition.getContainerExtensions()) {
      addComponent(component);
      if (component instanceof MavenProject) {
        // For backward compatibility we must set POM and actual packaging
        MavenProject pom = (MavenProject) component;
        project.setPom(pom);
        project.setPackaging(pom.getPackaging());
      }
    }

    addComponent(project);
    addComponent(DefaultProjectClasspath.class);
    addComponent(DefaultProjectFileSystem2.class);
    addComponent(project.getConfiguration());
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

  private void addCoreComponents() {
    addComponent(EventBus.class);
    addComponent(Phases.class);
    addComponent(PhasesTimeProfiler.class);
    for (Class clazz : Phases.getPhaseClasses()) {
      addComponent(clazz);
    }

    // TODO move metrics to BatchComponents
    for (Metric metric : CoreMetrics.getMetrics()) {
      addComponent(metric.getKey(), metric);
    }
    for (Metrics metricRepo : getComponents(Metrics.class)) {
      for (Metric metric : metricRepo.getMetrics()) {
        addComponent(metric.getKey(), metric);
      }
    }
  }

  private void addProjectPluginExtensions() {
    ProjectExtensionInstaller installer = getComponent(ProjectExtensionInstaller.class);
    installer.install(this, project);
  }


  private void logSettings() {
    // TODO move these logs in a dedicated component
    LOG.info("-------------  Analyzing {}", project.getName());

    String[] exclusionPatterns = project.getExclusionPatterns();
    if (exclusionPatterns != null && exclusionPatterns.length > 0) {
      LOG.info("Excluded sources : {}", Arrays.toString(exclusionPatterns));
    }
  }

  /**
   * Analyze project
   */
  @Override
  protected void doStart() {
    Language language = getComponent(Languages.class).get(project.getLanguageKey());
    if (language == null) {
      throw new SonarException("Language with key '" + project.getLanguageKey() + "' not found");
    }
    project.setLanguage(language);

    DefaultIndex index = getComponent(DefaultIndex.class);
    index.setCurrentProject(project,
        getComponent(ResourceFilters.class),
        getComponent(ViolationFilters.class),
        getComponent(RulesProfile.class));

    // TODO See http://jira.codehaus.org/browse/SONAR-2126
    // previously MavenProjectBuilder was responsible for creation of ProjectFileSystem
    project.setFileSystem(getComponent(ProjectFileSystem.class));

    getComponent(Phases.class).execute(project);
  }
}
