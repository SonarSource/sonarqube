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

import org.apache.commons.configuration.Configuration;
import org.picocontainer.Characteristics;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Plugins;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.IocContainer;
import org.sonar.api.utils.ServerHttpClient;
import org.sonar.batch.bootstrap.BatchPluginRepository;
import org.sonar.batch.bootstrap.BootstrapClassLoader;
import org.sonar.batch.bootstrap.ExtensionDownloader;
import org.sonar.batch.bootstrap.TempDirectories;
import org.sonar.batch.components.*;
import org.sonar.batch.index.*;
import org.sonar.core.components.CacheMetricFinder;
import org.sonar.core.components.CacheRuleFinder;
import org.sonar.core.plugin.JpaPluginDao;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.jpa.session.DatabaseSessionProvider;
import org.sonar.jpa.session.DriverDatabaseConnector;
import org.sonar.jpa.session.ThreadLocalDatabaseSessionFactory;

import java.net.URLClassLoader;
import java.util.Arrays;

public class Batch {

  private static final Logger LOG = LoggerFactory.getLogger(Batch.class);

  private Configuration configuration;
  private Object[] components;

  public Batch(Configuration configuration, Object... components) {
    this.configuration = configuration;
    this.components = components;
  }

  public void execute() {
    MutablePicoContainer container = null;
    try {
      container = buildPicoContainer();
      container.start();
      analyzeModules(container);

    } finally {
      if (container != null) {
        container.stop();
      }
    }
  }

  private void analyzeModules(MutablePicoContainer container) {
    // a child container is built to ensure database connector is up
    MutablePicoContainer batchContainer = container.makeChildContainer();
    batchContainer.as(Characteristics.CACHE).addComponent(ProjectTree.class);
    batchContainer.as(Characteristics.CACHE).addComponent(DefaultResourceCreationLock.class);
    batchContainer.as(Characteristics.CACHE).addComponent(DefaultIndex.class);
    batchContainer.as(Characteristics.CACHE).addComponent(DefaultPersistenceManager.class);
    batchContainer.as(Characteristics.CACHE).addComponent(DependencyPersister.class);
    batchContainer.as(Characteristics.CACHE).addComponent(EventPersister.class);
    batchContainer.as(Characteristics.CACHE).addComponent(LinkPersister.class);
    batchContainer.as(Characteristics.CACHE).addComponent(MeasurePersister.class);
    batchContainer.as(Characteristics.CACHE).addComponent(DefaultResourcePersister.class);
    batchContainer.as(Characteristics.CACHE).addComponent(SourcePersister.class);
    batchContainer.as(Characteristics.CACHE).addComponent(ViolationPersister.class);
    batchContainer.as(Characteristics.CACHE).addComponent(JpaPluginDao.class);
    batchContainer.as(Characteristics.CACHE).addComponent(BatchPluginRepository.class);
    batchContainer.as(Characteristics.CACHE).addComponent(Plugins.class);
    batchContainer.as(Characteristics.CACHE).addComponent(ServerHttpClient.class);
    batchContainer.as(Characteristics.CACHE).addComponent(MeasuresDao.class);
    batchContainer.as(Characteristics.CACHE).addComponent(CacheRuleFinder.class);
    batchContainer.as(Characteristics.CACHE).addComponent(CacheMetricFinder.class);
    batchContainer.as(Characteristics.CACHE).addComponent(PastSnapshotFinderByDate.class);
    batchContainer.as(Characteristics.CACHE).addComponent(PastSnapshotFinderByDays.class);
    batchContainer.as(Characteristics.CACHE).addComponent(PastSnapshotFinderByPreviousAnalysis.class);
    batchContainer.as(Characteristics.CACHE).addComponent(PastSnapshotFinderByVersion.class);
    batchContainer.as(Characteristics.CACHE).addComponent(PastMeasuresLoader.class);
    batchContainer.as(Characteristics.CACHE).addComponent(PastSnapshotFinder.class);
    batchContainer.start();

    ProjectTree projectTree = batchContainer.getComponent(ProjectTree.class);
    DefaultIndex index = batchContainer.getComponent(DefaultIndex.class);
    analyzeModule(batchContainer, index, projectTree.getRootProject());

    // batchContainer is stopped by its parent
  }

  private MutablePicoContainer buildPicoContainer() {
    MutablePicoContainer container = IocContainer.buildPicoContainer();

    register(container, configuration);
    register(container, ServerMetadata.class);// registered here because used by BootstrapClassLoader
    register(container, TempDirectories.class);// registered here because used by BootstrapClassLoader
    register(container, HttpDownloader.class);// registered here because used by BootstrapClassLoader
    register(container, ExtensionDownloader.class);// registered here because used by BootstrapClassLoader
    register(container, BootstrapClassLoader.class);

    URLClassLoader bootstrapClassLoader = container.getComponent(BootstrapClassLoader.class).getClassLoader();
    // set as the current context classloader for hibernate, else it does not find the JDBC driver.
    Thread.currentThread().setContextClassLoader(bootstrapClassLoader);

    register(container, new DriverDatabaseConnector(configuration, bootstrapClassLoader));
    register(container, ThreadLocalDatabaseSessionFactory.class);
    container.as(Characteristics.CACHE).addAdapter(new DatabaseSessionProvider());
    for (Object component : components) {
      register(container, component);
    }
    if (!isMavenPluginExecutorRegistered()) {
      register(container, FakeMavenPluginExecutor.class);
    }
    return container;
  }

  boolean isMavenPluginExecutorRegistered() {
    for (Object component : components) {
      if (component instanceof Class && MavenPluginExecutor.class.isAssignableFrom((Class<?>) component)) {
        return true;
      }
    }
    return false;
  }

  private void register(MutablePicoContainer container, Object component) {
    container.as(Characteristics.CACHE).addComponent(component);
  }

  private void analyzeModule(MutablePicoContainer container, DefaultIndex index, Project project) {
    for (Project module : project.getModules()) {
      analyzeModule(container, index, module);
    }
    LOG.info("-------------  Analyzing {}", project.getName());

    String[] exclusionPatterns = project.getExclusionPatterns();
    if (exclusionPatterns != null && exclusionPatterns.length > 0) {
      LOG.info("Excluded sources : {}", Arrays.toString(exclusionPatterns));
    }

    new ProjectBatch(container).execute(index, project);
  }
}
