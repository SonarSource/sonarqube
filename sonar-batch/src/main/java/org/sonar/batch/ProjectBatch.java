/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import org.picocontainer.Characteristics;
import org.picocontainer.MutablePicoContainer;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.*;
import org.sonar.api.rules.DefaultRulesManager;
import org.sonar.api.utils.IocContainer;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.bootstrap.BatchPluginRepository;
import org.sonar.batch.components.PastViolationsLoader;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.index.DefaultResourcePersister;
import org.sonar.batch.phases.Phases;
import org.sonar.core.components.DefaultModelFinder;
import org.sonar.jpa.dao.*;

public class ProjectBatch {

  private MutablePicoContainer globalContainer;
  private MutablePicoContainer batchContainer;

  public ProjectBatch(MutablePicoContainer container) {
    this.globalContainer = container;
  }

  public void execute(DefaultIndex index, Project project) {
    try {
      startChildContainer(index, project);
      batchContainer.getComponent(Phases.class).execute(project);

    } finally {
      stop();
    }
  }

  public void startChildContainer(DefaultIndex index, Project project) {
    batchContainer = globalContainer.makeChildContainer();

    batchContainer.as(Characteristics.CACHE).addComponent(project);
    batchContainer.as(Characteristics.CACHE).addComponent(project.getPom());
    batchContainer.as(Characteristics.CACHE).addComponent(ProjectClasspath.class);
    batchContainer.as(Characteristics.CACHE).addComponent(DefaultProjectFileSystem.class);
    batchContainer.as(Characteristics.CACHE).addComponent(project.getConfiguration());

    // need to be registered after the Configuration
    batchContainer.getComponent(BatchPluginRepository.class).registerPlugins(batchContainer);

    batchContainer.as(Characteristics.CACHE).addComponent(DaoFacade.class);
    batchContainer.as(Characteristics.CACHE).addComponent(RulesDao.class);

    // the Snapshot component will be removed when asynchronous measures are improved (required for AsynchronousMeasureSensor)
    batchContainer.as(Characteristics.CACHE)
        .addComponent(globalContainer.getComponent(DefaultResourcePersister.class).getSnapshot(project));

    batchContainer.as(Characteristics.CACHE).addComponent(org.sonar.api.database.daos.RulesDao.class);
    batchContainer.as(Characteristics.CACHE).addComponent(org.sonar.api.database.daos.MeasuresDao.class);
    batchContainer.as(Characteristics.CACHE).addComponent(ProfilesDao.class);
    batchContainer.as(Characteristics.CACHE).addComponent(AsyncMeasuresDao.class);
    batchContainer.as(Characteristics.CACHE).addComponent(AsyncMeasuresService.class);
    batchContainer.as(Characteristics.CACHE).addComponent(DefaultRulesManager.class);
    batchContainer.as(Characteristics.CACHE).addComponent(DefaultSensorContext.class);
    batchContainer.as(Characteristics.CACHE).addComponent(Languages.class);
    batchContainer.as(Characteristics.CACHE).addComponent(BatchExtensionDictionnary.class);
    batchContainer.as(Characteristics.CACHE).addComponent(DefaultTimeMachine.class);
    batchContainer.as(Characteristics.CACHE).addComponent(ViolationFilters.class);
    batchContainer.as(Characteristics.CACHE).addComponent(ResourceFilters.class);
    batchContainer.as(Characteristics.CACHE).addComponent(DefaultModelFinder.class);
    batchContainer.as(Characteristics.CACHE).addComponent(TimeMachineConfiguration.class);
    batchContainer.as(Characteristics.CACHE).addComponent(PastViolationsLoader.class);
    batchContainer.addAdapter(new ProfileProvider());
    batchContainer.addAdapter(new CheckProfileProvider());
    loadCoreComponents(batchContainer);
    batchContainer.as(Characteristics.CACHE).addComponent(new IocContainer(batchContainer));
    batchContainer.start();

    // post-initializations
    prepareProject(project, index);
  }

  private void prepareProject(Project project, DefaultIndex index) {
    Language language = getComponent(Languages.class).get(project.getLanguageKey());
    if (language == null) {
      throw new SonarException("Language with key '" + project.getLanguageKey() + "' not found");
    }
    project.setLanguage(language);

    index.setCurrentProject(project,
        getComponent(ResourceFilters.class),
        getComponent(ViolationFilters.class),
        getComponent(RulesProfile.class));

    // TODO See http://jira.codehaus.org/browse/SONAR-2126
    // previously MavenProjectBuilder was responsible for creation of ProjectFileSystem
    project.setFileSystem(getComponent(ProjectFileSystem.class));
  }

  private void loadCoreComponents(MutablePicoContainer container) {
    container.as(Characteristics.CACHE).addComponent(Phases.class);
    for (Class clazz : Phases.getPhaseClasses()) {
      container.as(Characteristics.CACHE).addComponent(clazz);
    }
    for (Metric metric : CoreMetrics.getMetrics()) {
      container.as(Characteristics.CACHE).addComponent(metric.getKey(), metric);
    }
    for (Metrics metricRepo : container.getComponents(Metrics.class)) {
      for (Metric metric : metricRepo.getMetrics()) {
        container.as(Characteristics.CACHE).addComponent(metric.getKey(), metric);
      }
    }
  }

  private void stop() {
    if (batchContainer != null) {
      try {
        globalContainer.removeChildContainer(batchContainer);
        batchContainer.stop();
        batchContainer = null;
      } catch (Exception e) {
        // do not log
      }
    }
  }

  public <T> T getComponent(Class<T> clazz) {
    if (batchContainer != null) {
      return batchContainer.getComponent(clazz);
    }
    return globalContainer.getComponent(clazz);
  }
}
