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

import org.sonar.api.Plugins;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.ServerHttpClient;
import org.sonar.batch.DefaultResourceCreationLock;
import org.sonar.batch.ProjectConfiguration;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.components.*;
import org.sonar.batch.index.*;
import org.sonar.core.components.CacheMetricFinder;
import org.sonar.core.components.CacheRuleFinder;
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
    addComponent(ProjectConfiguration.class);
    addComponent(ProjectTree.class);
    addComponent(DefaultResourceCreationLock.class);
    addComponent(DefaultIndex.class);

    if (dryRun) {
      addComponent(ReadOnlyPersistenceManager.class);
    } else {
      addComponent(DefaultPersistenceManager.class);
      addComponent(DependencyPersister.class);
      addComponent(EventPersister.class);
      addComponent(LinkPersister.class);
      addComponent(MeasurePersister.class);
      addComponent(MemoryOptimizer.class);
      addComponent(DefaultResourcePersister.class);
      addComponent(SourcePersister.class);
    }

    addComponent(Plugins.class);
    addComponent(ServerHttpClient.class);
    addComponent(MeasuresDao.class);
    addComponent(CacheRuleFinder.class);
    addComponent(CacheMetricFinder.class);
    addComponent(PastSnapshotFinderByDate.class);
    addComponent(PastSnapshotFinderByDays.class);
    addComponent(PastSnapshotFinderByPreviousAnalysis.class);
    addComponent(PastSnapshotFinderByVersion.class);
    addComponent(PastMeasuresLoader.class);
    addComponent(PastSnapshotFinder.class);
    addCoreMetrics();
    addBatchExtensions();
  }

  private void addBatchExtensions() {
    BatchExtensionInstaller installer = getComponent(BatchExtensionInstaller.class);
    installer.install(this);
  }

  void addCoreMetrics() {
    for (Metric metric : CoreMetrics.getMetrics()) {
      addComponent(metric.getKey(), metric);
    }
  }

  @Override
  protected void doStart() {
    ProjectTree projectTree = getComponent(ProjectTree.class);
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
      uninstallChild(projectComponents);
    }
  }
}
