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

import java.net.URLClassLoader;
import java.util.Arrays;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Plugins;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.ServerHttpClient;
import org.sonar.batch.bootstrap.BatchPluginRepository;
import org.sonar.batch.bootstrap.BootstrapClassLoader;
import org.sonar.batch.bootstrap.ExtensionDownloader;
import org.sonar.batch.bootstrap.TempDirectories;
import org.sonar.batch.components.PastMeasuresLoader;
import org.sonar.batch.components.PastSnapshotFinder;
import org.sonar.batch.components.PastSnapshotFinderByDate;
import org.sonar.batch.components.PastSnapshotFinderByDays;
import org.sonar.batch.components.PastSnapshotFinderByPreviousAnalysis;
import org.sonar.batch.components.PastSnapshotFinderByVersion;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.index.DefaultPersistenceManager;
import org.sonar.batch.index.DefaultResourcePersister;
import org.sonar.batch.index.DependencyPersister;
import org.sonar.batch.index.EventPersister;
import org.sonar.batch.index.LinkPersister;
import org.sonar.batch.index.MeasurePersister;
import org.sonar.batch.index.MemoryOptimizer;
import org.sonar.batch.index.SourcePersister;
import org.sonar.batch.index.ViolationPersister;
import org.sonar.core.components.CacheMetricFinder;
import org.sonar.core.components.CacheRuleFinder;
import org.sonar.core.plugin.JpaPluginDao;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.jpa.session.DatabaseSessionProvider;
import org.sonar.jpa.session.DriverDatabaseConnector;
import org.sonar.jpa.session.ThreadLocalDatabaseSessionFactory;

public class Batch {

  private static final Logger LOG = LoggerFactory.getLogger(Batch.class);

  private Configuration configuration;
  private Object[] components;

  public Batch(Configuration configuration, Object... components) {
    this.configuration = configuration;
    this.components = components;
  }

  public void execute() {
    Module bootstrapComponents = null;
    try {
      bootstrapComponents = new BootstrapComponents().init().start();
      analyzeModules(bootstrapComponents);
    } finally {
      if (bootstrapComponents != null) {
        bootstrapComponents.stop();
      }
    }
  }

  private void analyzeModules(Module bootstrapComponents) {
    Module batchComponents = bootstrapComponents.installChild(new BatchComponents());
    batchComponents.start();

    ProjectTree projectTree = batchComponents.getComponent(ProjectTree.class);
    DefaultIndex index = batchComponents.getComponent(DefaultIndex.class);
    analyzeModule(batchComponents, index, projectTree.getRootProject());

    // batchContainer is stopped by its parent
  }

  private static class BatchComponents extends Module {
    @Override
    protected void configure() {
      addComponent(ProjectTree.class);
      addComponent(DefaultResourceCreationLock.class);
      addComponent(DefaultIndex.class);
      addComponent(DefaultPersistenceManager.class);
      addComponent(DependencyPersister.class);
      addComponent(EventPersister.class);
      addComponent(LinkPersister.class);
      addComponent(MeasurePersister.class);
      addComponent(EventBus.class);
      addComponent(MemoryOptimizer.class);
      addComponent(DefaultResourcePersister.class);
      addComponent(SourcePersister.class);
      addComponent(ViolationPersister.class);
      addComponent(JpaPluginDao.class);
      addComponent(BatchPluginRepository.class);
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
    }
  }

  private class BootstrapComponents extends Module {
    @Override
    protected void configure() {
      addComponent(configuration);
      addComponent(ServerMetadata.class);// registered here because used by BootstrapClassLoader
      addComponent(TempDirectories.class);// registered here because used by BootstrapClassLoader
      addComponent(HttpDownloader.class);// registered here because used by BootstrapClassLoader
      addComponent(ExtensionDownloader.class);// registered here because used by BootstrapClassLoader
      addComponent(BootstrapClassLoader.class);

      URLClassLoader bootstrapClassLoader = getComponent(BootstrapClassLoader.class).getClassLoader();
      // set as the current context classloader for hibernate, else it does not find the JDBC driver.
      Thread.currentThread().setContextClassLoader(bootstrapClassLoader);

      addComponent(new DriverDatabaseConnector(configuration, bootstrapClassLoader));
      addComponent(ThreadLocalDatabaseSessionFactory.class);
      addAdapter(new DatabaseSessionProvider());
      for (Object component : components) {
        addComponent(component);
      }
      if (!isMavenPluginExecutorRegistered()) {
        addComponent(FakeMavenPluginExecutor.class);
      }
    }
  }

  boolean isMavenPluginExecutorRegistered() {
    for (Object component : components) {
      if (component instanceof Class && MavenPluginExecutor.class.isAssignableFrom((Class<?>) component)) {
        return true;
      }
    }
    return false;
  }

  private void analyzeModule(Module batchComponents, DefaultIndex index, Project project) {
    for (Project module : project.getModules()) {
      analyzeModule(batchComponents, index, module);
    }
    LOG.info("-------------  Analyzing {}", project.getName());

    String[] exclusionPatterns = project.getExclusionPatterns();
    if (exclusionPatterns != null && exclusionPatterns.length > 0) {
      LOG.info("Excluded sources : {}", Arrays.toString(exclusionPatterns));
    }

    new ProjectBatch(batchComponents).execute(index, project);
  }
}
