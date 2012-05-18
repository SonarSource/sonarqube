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

import org.apache.commons.configuration.PropertiesConfiguration;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.batch.FakeMavenPluginExecutor;
import org.sonar.batch.MavenPluginExecutor;
import org.sonar.batch.RemoteServerMetadata;
import org.sonar.batch.ServerMetadata;
import org.sonar.batch.config.BatchDatabaseSettingsLoader;
import org.sonar.batch.config.BatchSettings;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.core.persistence.MyBatis;
import org.sonar.jpa.session.DatabaseSessionProvider;
import org.sonar.jpa.session.DefaultDatabaseConnector;
import org.sonar.jpa.session.ThreadLocalDatabaseSessionFactory;

import java.net.URLClassLoader;

/**
 * Level 1 components
 */
public class BootstrapModule extends Module {

  private Object[] boostrapperComponents;
  private ProjectReactor reactor;

  public BootstrapModule(ProjectReactor reactor, Object... boostrapperComponents) {
    this.reactor = reactor;
    this.boostrapperComponents = boostrapperComponents;
  }

  @Override
  protected void configure() {
    addCoreSingleton(reactor);
    addCoreSingleton(new PropertiesConfiguration());
    addCoreSingleton(BatchSettings.class);
    addCoreSingleton(DryRun.class);
    addCoreSingleton(ServerMetadata.class);// registered here because used by BootstrapClassLoader
    addCoreSingleton(TempDirectories.class);// registered here because used by BootstrapClassLoader
    addCoreSingleton(HttpDownloader.class);// registered here because used by BootstrapClassLoader
    addCoreSingleton(ArtifactDownloader.class);// registered here because used by BootstrapClassLoader
    addCoreSingleton(JdbcDriverHolder.class);

    URLClassLoader bootstrapClassLoader = getComponentByType(JdbcDriverHolder.class).getClassLoader();
    // set as the current context classloader for hibernate, else it does not find the JDBC driver.
    Thread.currentThread().setContextClassLoader(bootstrapClassLoader);

    addCoreSingleton(RemoteServerMetadata.class);
    // mybatis
    addCoreSingleton(BatchDatabase.class);
    addCoreSingleton(MyBatis.class);
    addCoreSingleton(DatabaseVersion.class);
    addCoreSingleton(DatabaseBatchCompatibility.class);
    for (Class daoClass : DaoUtils.getDaoClasses()) {
      addCoreSingleton(daoClass);
    }

    // hibernate
    addCoreSingleton(DefaultDatabaseConnector.class);
    addCoreSingleton(ThreadLocalDatabaseSessionFactory.class);
    addAdapter(new DatabaseSessionProvider());

    for (Object component : boostrapperComponents) {
      addCoreSingleton(component);
    }
    if (!isMavenPluginExecutorRegistered()) {
      addCoreSingleton(FakeMavenPluginExecutor.class);
    }

    addCoreSingleton(BatchPluginRepository.class);
    addCoreSingleton(BatchExtensionInstaller.class);
    addCoreSingleton(BatchDatabaseSettingsLoader.class);
  }

  boolean isMavenPluginExecutorRegistered() {
    if (boostrapperComponents != null) {
      for (Object component : boostrapperComponents) {
        if (component instanceof Class && MavenPluginExecutor.class.isAssignableFrom((Class<?>) component)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  protected void doStart() {
    boolean dryRun = getComponentByType(DryRun.class).isEnabled();
    Module batchComponents = installChild(new BatchModule(dryRun));
    batchComponents.start();
  }
}
