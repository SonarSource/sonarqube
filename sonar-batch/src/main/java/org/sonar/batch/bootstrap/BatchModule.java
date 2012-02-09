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

import org.sonar.api.Plugins;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.ServerHttpClient;
import org.sonar.batch.DefaultFileLinesContextFactory;
import org.sonar.batch.DefaultResourceCreationLock;
import org.sonar.batch.ProjectConfigurator;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.components.*;
import org.sonar.batch.index.*;
import org.sonar.core.metric.CacheMetricFinder;
import org.sonar.core.notification.DefaultNotificationManager;
import org.sonar.core.rule.CacheRuleFinder;
import org.sonar.core.user.DefaultUserFinder;
import org.sonar.jpa.dao.MeasuresDao;

/**
 * Level-2 components. Connected to database.
 */
public class BatchModule extends Module {

  private final boolean dryRun;

  public BatchModule(boolean dryRun) {
    this.dryRun = dryRun;
  }

  @Override
  protected void configure() {
    addCoreSingleton(ProjectTree.class);
    addCoreSingleton(ProjectFilter.class);
    addCoreSingleton(ProjectConfigurator.class);
    addCoreSingleton(DefaultResourceCreationLock.class);
    addCoreSingleton(DefaultIndex.class);
    addCoreSingleton(DefaultFileLinesContextFactory.class);

    if (dryRun) {
      addCoreSingleton(ReadOnlyPersistenceManager.class);
    } else {
      addCoreSingleton(DefaultPersistenceManager.class);
      addCoreSingleton(DependencyPersister.class);
      addCoreSingleton(EventPersister.class);
      addCoreSingleton(LinkPersister.class);
      addCoreSingleton(MeasurePersister.class);
      addCoreSingleton(MemoryOptimizer.class);
      addCoreSingleton(DefaultResourcePersister.class);
      addCoreSingleton(SourcePersister.class);
    }

    addCoreSingleton(Plugins.class);
    addCoreSingleton(ServerHttpClient.class);
    addCoreSingleton(MeasuresDao.class);
    addCoreSingleton(CacheRuleFinder.class);
    addCoreSingleton(CacheMetricFinder.class);
    addCoreSingleton(PastSnapshotFinderByDate.class);
    addCoreSingleton(PastSnapshotFinderByDays.class);
    addCoreSingleton(PastSnapshotFinderByPreviousAnalysis.class);
    addCoreSingleton(PastSnapshotFinderByVersion.class);
    addCoreSingleton(PastMeasuresLoader.class);
    addCoreSingleton(PastSnapshotFinder.class);
    addCoreSingleton(DefaultNotificationManager.class);
    addCoreSingleton(DefaultUserFinder.class);
    addCoreMetrics();
    addBatchExtensions();
  }

  private void addBatchExtensions() {
    BatchExtensionInstaller installer = getComponentByType(BatchExtensionInstaller.class);
    installer.install(this);
  }

  void addCoreMetrics() {
    for (Metric metric : CoreMetrics.getMetrics()) {
      addCoreSingleton(metric);
    }
  }

  @Override
  protected void doStart() {
    ProjectTree projectTree = getComponentByType(ProjectTree.class);
    analyze(projectTree.getRootProject());
  }

  private void analyze(Project project) {
    for (Project subProject : project.getModules()) {
      analyze(subProject);
    }

    Module projectComponents = installChild(new ProjectModule(project, dryRun));
    try {
      projectComponents.start();
    } finally {
      projectComponents.stop();
      uninstallChild();
    }
  }
}
