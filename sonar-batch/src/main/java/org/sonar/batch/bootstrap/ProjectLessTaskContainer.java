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
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.DefaultResourceCreationLock;
import org.sonar.batch.components.PastMeasuresLoader;
import org.sonar.batch.components.PastSnapshotFinder;
import org.sonar.batch.components.PastSnapshotFinderByDate;
import org.sonar.batch.components.PastSnapshotFinderByDays;
import org.sonar.batch.components.PastSnapshotFinderByPreviousAnalysis;
import org.sonar.batch.components.PastSnapshotFinderByPreviousVersion;
import org.sonar.batch.components.PastSnapshotFinderByVersion;
import org.sonar.batch.index.DefaultPersistenceManager;
import org.sonar.batch.index.DefaultResourcePersister;
import org.sonar.batch.index.DependencyPersister;
import org.sonar.batch.index.EventPersister;
import org.sonar.batch.index.LinkPersister;
import org.sonar.batch.index.MeasurePersister;
import org.sonar.batch.index.MemoryOptimizer;
import org.sonar.batch.index.SourcePersister;
import org.sonar.batch.tasks.ListTasksTask;
import org.sonar.core.i18n.I18nManager;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.metric.CacheMetricFinder;
import org.sonar.core.notification.DefaultNotificationManager;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.persistence.SemaphoreUpdater;
import org.sonar.core.persistence.SemaphoresImpl;
import org.sonar.core.qualitymodel.DefaultModelFinder;
import org.sonar.core.resource.DefaultResourcePermissions;
import org.sonar.core.rule.CacheRuleFinder;
import org.sonar.core.user.DefaultUserFinder;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.jpa.session.DefaultDatabaseConnector;
import org.sonar.jpa.session.JpaDatabaseSession;

import javax.annotation.Nullable;

/**
 * Level-3 components. Task-level components that don't depends on project.
 */
public class ProjectLessTaskContainer extends Container {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectLessTaskContainer.class);

  private TaskDefinition taskDefinition;

  private ProjectReactor reactor;

  public ProjectLessTaskContainer(TaskDefinition task, @Nullable ProjectReactor reactor) {
    this.taskDefinition = task;
    this.reactor = reactor;
  }

  @Override
  protected void configure() {
    registerCoreComponents();
    registerDatabaseComponents();
    registerCoreProjectLessTasks();
    registerProjectLessTaskExtensions();
  }

  private void registerCoreComponents() {
    container.addSingleton(EmailSettings.class);
    container.addSingleton(I18nManager.class);
    container.addSingleton(RuleI18nManager.class);
    container.addSingleton(MeasuresDao.class);
    container.addSingleton(CacheRuleFinder.class);
    container.addSingleton(CacheMetricFinder.class);
    container.addSingleton(DefaultUserFinder.class);
    container.addSingleton(ResourceTypes.class);
    container.addSingleton(SemaphoreUpdater.class);
    container.addSingleton(SemaphoresImpl.class);
    container.addSingleton(PastSnapshotFinderByDate.class);
    container.addSingleton(PastSnapshotFinderByDays.class);
    container.addSingleton(PastSnapshotFinderByPreviousAnalysis.class);
    container.addSingleton(PastSnapshotFinderByVersion.class);
    container.addSingleton(PastSnapshotFinderByPreviousVersion.class);
    container.addSingleton(PastMeasuresLoader.class);
    container.addSingleton(PastSnapshotFinder.class);
    container.addSingleton(DefaultModelFinder.class);
    container.addSingleton(MetricProvider.class);
    container.addSingleton(DefaultResourceCreationLock.class);
    container.addSingleton(DefaultPersistenceManager.class);
    container.addSingleton(DependencyPersister.class);
    container.addSingleton(EventPersister.class);
    container.addSingleton(LinkPersister.class);
    container.addSingleton(MeasurePersister.class);
    container.addSingleton(MemoryOptimizer.class);
    container.addSingleton(DefaultResourcePermissions.class);
    container.addSingleton(DefaultResourcePersister.class);
    container.addSingleton(SourcePersister.class);
    container.addSingleton(DefaultNotificationManager.class);

  }

  private void registerDatabaseComponents() {
    container.addSingleton(JdbcDriverHolder.class);
    container.addSingleton(BatchDatabase.class);
    container.addSingleton(MyBatis.class);
    container.addSingleton(DatabaseVersion.class);
    container.addSingleton(DatabaseCompatibility.class);
    for (Class daoClass : DaoUtils.getDaoClasses()) {
      container.addSingleton(daoClass);
    }

    // hibernate
    container.addSingleton(DefaultDatabaseConnector.class);
    container.addSingleton(JpaDatabaseSession.class);
    container.addSingleton(BatchDatabaseSessionFactory.class);
  }

  private void registerCoreProjectLessTasks() {
    container.addSingleton(ListTasksTask.class);
  }

  private void registerProjectLessTaskExtensions() {
    ExtensionInstaller installer = container.getComponentByType(ExtensionInstaller.class);
    installer.installTaskExtensions(container, false);
  }

  private void logSettings() {
    LOG.info("-------------  Executing {}", taskDefinition.getName());
  }

  /**
   * Execute task
   */
  @Override
  protected void doStart() {
    boolean projectPresent = (reactor != null);
    if (ExtensionUtils.requiresProject(taskDefinition.getTask())) {
      if (!projectPresent) {
        throw new SonarException("Task '" + taskDefinition.getName() + "' requires to be run on a project");
      }
      // Create a new child container to put project specific components
      Container childModule = new ProjectTaskContainer(taskDefinition, reactor);
      try {
        installChild(childModule);
        childModule.start();
      } finally {
        childModule.stop();
        uninstallChild();
      }
    }
    else {
      Task task = container.getComponentByType(taskDefinition.getTask());
      if (task != null) {
        logSettings();
        task.execute();
      } else {
        throw new SonarException("Extension " + taskDefinition.getTask() + " was not found in declared extensions.");
      }
    }
  }

}
