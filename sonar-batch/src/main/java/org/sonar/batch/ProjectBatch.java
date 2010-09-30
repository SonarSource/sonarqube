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
import org.sonar.api.batch.FileFilter;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.resources.DefaultProjectFileSystem;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.DefaultRulesManager;
import org.sonar.api.utils.IocContainer;
import org.sonar.batch.indexer.DefaultSonarIndex;
import org.sonar.core.qualitymodel.DefaultModelFinder;
import org.sonar.core.rule.DefaultRuleFinder;
import org.sonar.jpa.dao.*;

import java.util.List;

public class ProjectBatch {

  private MutablePicoContainer globalContainer;
  private MutablePicoContainer batchContainer;

  public ProjectBatch(MutablePicoContainer container) {
    this.globalContainer = container;
  }

  public void execute(DefaultSonarIndex index, Project project) {
    try {
      startChildContainer(index, project);
      SensorContext sensorContext = batchContainer.getComponent(SensorContext.class);
      for (Class<? extends CoreJob> clazz : CoreJobs.allJobs()) {
        CoreJob job = getComponent(clazz);
        job.execute(project, sensorContext);
        commit();
      }

    } finally {
      index.clear();
      stop();
    }
  }

  public void startChildContainer(DefaultSonarIndex index, Project project) {
    batchContainer = globalContainer.makeChildContainer();

    batchContainer.as(Characteristics.CACHE).addComponent(project);
    batchContainer.as(Characteristics.CACHE).addComponent(project.getPom());
    batchContainer.as(Characteristics.CACHE).addComponent(ProjectClasspath.class);
    batchContainer.as(Characteristics.CACHE).addComponent(index.getBucket(project).getSnapshot());
    batchContainer.as(Characteristics.CACHE).addComponent(project.getConfiguration());

    //need to be registered after the Configuration
    batchContainer.getComponent(BatchPluginRepository.class).registerPlugins(batchContainer);

    batchContainer.as(Characteristics.CACHE).addComponent(DaoFacade.class);
    batchContainer.as(Characteristics.CACHE).addComponent(RulesDao.class);
    batchContainer.as(Characteristics.CACHE).addComponent(org.sonar.api.database.daos.RulesDao.class);
    batchContainer.as(Characteristics.CACHE).addComponent(MeasuresDao.class);
    batchContainer.as(Characteristics.CACHE).addComponent(org.sonar.api.database.daos.MeasuresDao.class);
    batchContainer.as(Characteristics.CACHE).addComponent(ProfilesDao.class);
    batchContainer.as(Characteristics.CACHE).addComponent(AsyncMeasuresDao.class);
    batchContainer.as(Characteristics.CACHE).addComponent(AsyncMeasuresService.class);
    batchContainer.as(Characteristics.CACHE).addComponent(DefaultRulesManager.class);
    batchContainer.as(Characteristics.CACHE).addComponent(DefaultSensorContext.class);
    batchContainer.as(Characteristics.CACHE).addComponent(Languages.class);
    batchContainer.as(Characteristics.CACHE).addComponent(BatchExtensionDictionnary.class);
    batchContainer.as(Characteristics.CACHE).addComponent(DefaultTimeMachine.class);
    batchContainer.as(Characteristics.CACHE).addComponent(ViolationsDao.class);
    batchContainer.as(Characteristics.CACHE).addComponent(ViolationFilters.class);
    batchContainer.as(Characteristics.CACHE).addComponent(ResourceFilters.class);
    batchContainer.as(Characteristics.CACHE).addComponent(DefaultModelFinder.class);
    batchContainer.as(Characteristics.CACHE).addComponent(DefaultRuleFinder.class);
    batchContainer.addAdapter(new ProfileProvider());
    batchContainer.addAdapter(new CheckProfileProvider());
    loadCoreComponents(batchContainer);
    batchContainer.as(Characteristics.CACHE).addComponent(new IocContainer(batchContainer));
    batchContainer.start();

    // post-initializations
    prepareProject(project, index);
  }

  private void prepareProject(Project project, DefaultSonarIndex index) {
    project.setLanguage(getComponent(Languages.class).get(project.getLanguageKey()));
    index.selectProject(project, getComponent(ResourceFilters.class), getComponent(ViolationFilters.class), getComponent(MeasuresDao.class), getComponent(ViolationsDao.class));

    List<FileFilter> fileFilters = batchContainer.getComponents(FileFilter.class);
    ((DefaultProjectFileSystem)project.getFileSystem()).addFileFilters(fileFilters);
  }

  private void loadCoreComponents(MutablePicoContainer container) {
    for (Class<?> clazz : CoreJobs.allJobs()) {
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
      commit();
      try {
        globalContainer.removeChildContainer(batchContainer);
        batchContainer.stop();
        batchContainer = null;
      } catch (Exception e) {
        // do not log
      }
    }
  }

  public void commit() {
    getComponent(DatabaseSession.class).commit();
  }

  public <T> T getComponent(Class<T> clazz) {
    if (batchContainer != null) {
      return batchContainer.getComponent(clazz);
    }
    return globalContainer.getComponent(clazz);
  }
}
