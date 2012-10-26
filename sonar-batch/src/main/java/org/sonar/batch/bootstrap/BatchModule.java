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
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ResourceTypes;
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
import org.sonar.core.metric.CacheMetricFinder;
import org.sonar.core.notification.DefaultNotificationManager;
import org.sonar.core.rule.CacheRuleFinder;
import org.sonar.core.user.DefaultUserFinder;
import org.sonar.jpa.dao.MeasuresDao;

/**
 * Level-2 components. Connected to database.
 */
public class BatchModule extends Module {

  @Override
  protected void configure() {
    container.addSingleton(ProjectTree.class);
    container.addSingleton(ProjectConfigurator.class);
    container.addSingleton(DefaultResourceCreationLock.class);
    container.addSingleton(DefaultIndex.class);
    container.addSingleton(DefaultFileLinesContextFactory.class);
    container.addSingleton(DefaultPersistenceManager.class);
    container.addSingleton(DependencyPersister.class);
    container.addSingleton(EventPersister.class);
    container.addSingleton(LinkPersister.class);
    container.addSingleton(MeasurePersister.class);
    container.addSingleton(MemoryOptimizer.class);
    container.addSingleton(DefaultResourcePersister.class);
    container.addSingleton(SourcePersister.class);
    container.addSingleton(Plugins.class);
    container.addSingleton(MeasuresDao.class);
    container.addSingleton(CacheRuleFinder.class);
    container.addSingleton(CacheMetricFinder.class);
    container.addSingleton(PastSnapshotFinderByDate.class);
    container.addSingleton(PastSnapshotFinderByDays.class);
    container.addSingleton(PastSnapshotFinderByPreviousAnalysis.class);
    container.addSingleton(PastSnapshotFinderByVersion.class);
    container.addSingleton(PastSnapshotFinderByPreviousVersion.class);
    container.addSingleton(PastMeasuresLoader.class);
    container.addSingleton(PastSnapshotFinder.class);
    container.addSingleton(DefaultNotificationManager.class);
    container.addSingleton(DefaultUserFinder.class);
    container.addSingleton(ResourceTypes.class);
    container.addSingleton(MetricProvider.class);
    addBatchExtensions();
  }

  private void addBatchExtensions() {
    ExtensionInstaller installer = container.getComponentByType(ExtensionInstaller.class);
    installer.install(container, InstantiationStrategy.BATCH);
  }

  @Override
  protected void doStart() {
    ProjectTree projectTree = container.getComponentByType(ProjectTree.class);
    analyze(projectTree.getRootProject());
  }

  private void analyze(Project project) {
    for (Project subProject : project.getModules()) {
      analyze(subProject);
    }

    Module projectComponents = installChild(new ProjectModule(project));
    try {
      projectComponents.start();
    } finally {
      projectComponents.stop();
      uninstallChild();
    }
  }
}
