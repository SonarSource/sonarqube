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

import org.apache.commons.configuration.Configuration;
import org.sonar.api.Plugin;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.batch.FakeMavenPluginExecutor;
import org.sonar.batch.MavenPluginExecutor;
import org.sonar.batch.ServerMetadata;
import org.sonar.core.plugin.JpaPluginDao;
import org.sonar.jpa.session.DatabaseSessionProvider;
import org.sonar.jpa.session.DriverDatabaseConnector;
import org.sonar.jpa.session.ThreadLocalDatabaseSessionFactory;

import java.net.URLClassLoader;

/**
 * Level 1 components
 */
public class BootstrapModule extends Module {

  private Configuration configuration;
  private Object[] boostrapperComponents;
  private ProjectReactor reactor;

  public BootstrapModule(ProjectReactor reactor, Configuration configuration, Object... boostrapperComponents) {
    this.reactor = reactor;
    this.configuration = configuration;
    this.boostrapperComponents = boostrapperComponents;
  }

  @Override
  protected void configure() {
    addComponent(reactor);
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
    for (Object component : boostrapperComponents) {
      addComponent(component);
    }
    if (!isMavenPluginExecutorRegistered()) {
      addComponent(FakeMavenPluginExecutor.class);
    }

    // LIMITATION : list of plugins to download is currently loaded from database. It should be loaded from
    // remote HTTP index.
    addComponent(JpaPluginDao.class);
    addComponent(BatchPluginRepository.class);
    addComponent(BatchExtensionInstaller.class);
    addComponent(ProjectExtensionInstaller.class);
  }

  boolean isMavenPluginExecutorRegistered() {
    for (Object component : boostrapperComponents) {
      if (component instanceof Class && MavenPluginExecutor.class.isAssignableFrom((Class<?>) component)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected void doStart() {
    addPlugins();
    Module batchComponents = installChild(new BatchModule());
    batchComponents.start();
  }

  private void addPlugins() {
    // Plugins have been loaded during the startup of BatchPluginRepository.
    // In a perfect world BatchPluginRepository should be a factory which injects new components into container, but
    // (it seems that) this feature does not exist in PicoContainer.
    // Limitation: the methods start() and stop() are not called on org.sonar.api.Plugin instances.
    for (Plugin plugin : getComponent(BatchPluginRepository.class).getPlugins()) {
      addComponent(plugin);
    }
  }
}
