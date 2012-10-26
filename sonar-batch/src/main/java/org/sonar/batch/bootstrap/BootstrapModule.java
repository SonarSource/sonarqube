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
import org.sonar.api.config.EmailSettings;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.UriReader;
import org.sonar.batch.FakeMavenPluginExecutor;
import org.sonar.batch.MavenPluginExecutor;
import org.sonar.batch.config.BatchDatabaseSettingsLoader;
import org.sonar.batch.config.BootstrapSettings;
import org.sonar.batch.local.DryRunDatabase;
import org.sonar.batch.local.DryRunExporter;
import org.sonar.core.config.Logback;
import org.sonar.core.i18n.I18nManager;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.core.persistence.MyBatis;
import org.sonar.jpa.session.DatabaseSessionProvider;
import org.sonar.jpa.session.DefaultDatabaseConnector;
import org.sonar.jpa.session.ThreadLocalDatabaseSessionFactory;

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
    container.addSingleton(reactor);
    container.addSingleton(new PropertiesConfiguration());
    container.addSingleton(BootstrapSettings.class);
    container.addSingleton(BatchPluginRepository.class);
    container.addSingleton(ExtensionInstaller.class);
    container.addSingleton(DryRun.class);
    container.addSingleton(DryRunExporter.class);
    container.addSingleton(Logback.class);
    container.addSingleton(ServerClient.class);
    container.addSingleton(TempDirectories.class);
    container.addSingleton(HttpDownloader.class);
    container.addSingleton(UriReader.class);
    container.addSingleton(PluginDownloader.class);
    container.addSingleton(EmailSettings.class);
    container.addSingleton(I18nManager.class);
    container.addSingleton(RuleI18nManager.class);
    for (Object component : boostrapperComponents) {
      container.addSingleton(component);
    }
    container.addSingleton(BootstrapExtensionExecutor.class);
    if (!isMavenPluginExecutorRegistered()) {
      container.addSingleton(FakeMavenPluginExecutor.class);
    }
    addDatabaseComponents();
  }

  private void addDatabaseComponents() {
    container.addSingleton(JdbcDriverHolder.class);
    container.addSingleton(DryRunDatabase.class);

    // mybatis
    container.addSingleton(BatchDatabase.class);
    container.addSingleton(MyBatis.class);
    container.addSingleton(DatabaseVersion.class);
    for (Class daoClass : DaoUtils.getDaoClasses()) {
      container.addSingleton(daoClass);
    }

    // hibernate
    container.addSingleton(DefaultDatabaseConnector.class);
    container.addSingleton(ThreadLocalDatabaseSessionFactory.class);
    container.addPicoAdapter(new DatabaseSessionProvider());

    container.addSingleton(DatabaseBatchCompatibility.class);
    container.addSingleton(BatchDatabaseSettingsLoader.class);
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
    Module batchComponents = installChild(new BatchModule());
    batchComponents.start();
  }
}
