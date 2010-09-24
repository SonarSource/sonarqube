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
import org.sonar.batch.indexer.DefaultSonarIndex;
import org.sonar.core.plugin.JpaPluginDao;
import org.sonar.jpa.session.DatabaseSessionProvider;
import org.sonar.jpa.session.DriverDatabaseConnector;
import org.sonar.jpa.session.ThreadLocalDatabaseSessionFactory;

import java.net.URLClassLoader;

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
      analyzeProjects(container);

    } finally {
      if (container != null) {
        container.stop();
      }
    }
  }

  private void analyzeProjects(MutablePicoContainer container) {
    // a child container is built to ensure database connector is up
    MutablePicoContainer batchContainer = container.makeChildContainer();
    batchContainer.as(Characteristics.CACHE).addComponent(ServerMetadata.class);
    batchContainer.as(Characteristics.CACHE).addComponent(ProjectTree.class);
    batchContainer.as(Characteristics.CACHE).addComponent(DefaultResourceCreationLock.class);
    batchContainer.as(Characteristics.CACHE).addComponent(DefaultSonarIndex.class);
    batchContainer.as(Characteristics.CACHE).addComponent(JpaPluginDao.class);
    batchContainer.as(Characteristics.CACHE).addComponent(BatchPluginRepository.class);
    batchContainer.as(Characteristics.CACHE).addComponent(Plugins.class);
    batchContainer.as(Characteristics.CACHE).addComponent(ServerHttpClient.class);
    batchContainer.as(Characteristics.CACHE).addComponent(HttpDownloader.class);
    batchContainer.start();

    ProjectTree projectTree = batchContainer.getComponent(ProjectTree.class);
    DefaultSonarIndex index = batchContainer.getComponent(DefaultSonarIndex.class);
    analyzeProject(batchContainer, index, projectTree.getRootProject());

    // batchContainer is stopped by its parent
  }

  private MutablePicoContainer buildPicoContainer() {
    MutablePicoContainer container = IocContainer.buildPicoContainer();

    register(container, configuration);
    URLClassLoader fullClassloader = RemoteClassLoader.createForJdbcDriver(configuration).getClassLoader();
    // set as the current context classloader for hibernate, else it does not find the JDBC driver.
    Thread.currentThread().setContextClassLoader(fullClassloader);

    register(container, new DriverDatabaseConnector(configuration, fullClassloader));
    register(container, ThreadLocalDatabaseSessionFactory.class);
    container.as(Characteristics.CACHE).addAdapter(new DatabaseSessionProvider());
    for (Object component : components) {
      register(container, component);
    }
    return container;
  }

  private void register(MutablePicoContainer container, Object component) {
    container.as(Characteristics.CACHE).addComponent(component);
  }

  private void analyzeProject(MutablePicoContainer container, DefaultSonarIndex index, Project project) {
    for (Project module : project.getModules()) {
      analyzeProject(container, index, module);
    }
    LOG.info("-------------  Analyzing " + project.getName());
    new ProjectBatch(container).execute(index, project);
  }
}
