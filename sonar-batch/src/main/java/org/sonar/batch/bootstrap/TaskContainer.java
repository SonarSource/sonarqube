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
import org.sonar.api.config.EmailSettings;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskDefinition;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.DefaultFileLinesContextFactory;
import org.sonar.batch.DefaultResourceCreationLock;
import org.sonar.batch.ProjectConfigurator;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.components.PastMeasuresLoader;
import org.sonar.batch.components.PastSnapshotFinder;
import org.sonar.batch.components.PastSnapshotFinderByDate;
import org.sonar.batch.components.PastSnapshotFinderByDays;
import org.sonar.batch.components.PastSnapshotFinderByPreviousAnalysis;
import org.sonar.batch.components.PastSnapshotFinderByPreviousVersion;
import org.sonar.batch.components.PastSnapshotFinderByVersion;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.index.DefaultPersistenceManager;
import org.sonar.batch.index.DefaultResourcePersister;
import org.sonar.batch.index.DependencyPersister;
import org.sonar.batch.index.EventPersister;
import org.sonar.batch.index.LinkPersister;
import org.sonar.batch.index.MeasurePersister;
import org.sonar.batch.index.MemoryOptimizer;
import org.sonar.batch.index.SourcePersister;
import org.sonar.batch.tasks.InspectionTask;
import org.sonar.batch.tasks.ListTasksTask;
import org.sonar.core.component.ScanGraph;
import org.sonar.core.component.ScanGraphStore;
import org.sonar.core.component.ScanPerspectives;
import org.sonar.core.i18n.I18nManager;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.metric.CacheMetricFinder;
import org.sonar.core.notification.DefaultNotificationManager;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.persistence.SemaphoresImpl;
import org.sonar.core.resource.DefaultResourcePermissions;
import org.sonar.core.rule.CacheRuleFinder;
import org.sonar.core.test.TestPlanBuilder;
import org.sonar.core.test.TestableBuilder;
import org.sonar.core.user.DefaultUserFinder;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.jpa.session.DefaultDatabaseConnector;
import org.sonar.jpa.session.JpaDatabaseSession;

/**
 * Level-3 components. Task-level components that don't depends on project.
 */
public class TaskContainer extends Container {

  private static final Logger LOG = LoggerFactory.getLogger(TaskContainer.class);

  private TaskDefinition taskDefinition;
  private boolean projectPresent;

  public TaskContainer(TaskDefinition task, boolean projectPresent) {
    this.taskDefinition = task;
    this.projectPresent = projectPresent;
  }

  @Override
  protected void configure() {
    logSettings();
    registerCoreComponents();
    registerDatabaseComponents();
    registerCoreTasks();
    if (projectPresent) {
      registerCoreComponentsRequiringProject();
    }
    registerTaskExtensions();
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
    container.addSingleton(SemaphoresImpl.class);
    container.addSingleton(PastSnapshotFinderByDate.class);
    container.addSingleton(PastSnapshotFinderByDays.class);
    container.addSingleton(PastSnapshotFinderByPreviousAnalysis.class);
    container.addSingleton(PastSnapshotFinderByVersion.class);
    container.addSingleton(PastSnapshotFinderByPreviousVersion.class);
    container.addSingleton(PastMeasuresLoader.class);
    container.addSingleton(PastSnapshotFinder.class);
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

  private void registerCoreTasks() {
    container.addSingleton(ListTasksTask.class);
    if (projectPresent) {
      container.addSingleton(InspectionTask.class);
    }
  }

  private void registerTaskExtensions() {
    ExtensionInstaller installer = container.getComponentByType(ExtensionInstaller.class);
    installer.installTaskExtensions(container, projectPresent);
  }

  private void registerCoreComponentsRequiringProject() {
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
    container.addSingleton(MetricProvider.class);
    container.addSingleton(ProjectExclusions.class);
    container.addSingleton(ProjectReactorReady.class);
    container.addSingleton(ProjectTree.class);
    container.addSingleton(ProjectConfigurator.class);
    container.addSingleton(DefaultIndex.class);
    container.addSingleton(DefaultFileLinesContextFactory.class);
    container.addSingleton(ProjectLock.class);
    container.addSingleton(DryRunDatabase.class);

    // graphs
    container.addSingleton(ScanGraph.create());
    container.addSingleton(TestPlanBuilder.class);
    container.addSingleton(TestableBuilder.class);
    container.addSingleton(ScanPerspectives.class);
    container.addSingleton(ScanGraphStore.class);
  }

  private void logSettings() {
    LOG.info("-------------  Executing {}", taskDefinition.getName());
  }

  /**
   * Execute task
   */
  @Override
  protected void doStart() {
    Task task = container.getComponentByType(taskDefinition.getTask());
    if (task != null) {
      task.execute();
    } else {
      throw new SonarException("Extension " + taskDefinition.getTask() + " was not found in declared extensions.");
    }
  }

}
