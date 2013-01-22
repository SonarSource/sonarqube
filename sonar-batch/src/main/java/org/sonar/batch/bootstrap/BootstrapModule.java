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
import org.sonar.api.utils.UriReader;
import org.sonar.batch.FakeMavenPluginExecutor;
import org.sonar.batch.MavenPluginExecutor;
import org.sonar.core.config.Logback;

/**
 * Level 1 components
 */
public class BootstrapModule extends Module {

  private Object[] boostrapperComponents;
  private ProjectReactor reactor;
  private GlobalBatchProperties globalProperties;
  private String taskCommand;

  /**
   * @deprecated Use {@link #BootstrapModule(GlobalBatchProperties, String, ProjectReactor, Object...)}
   */
  @Deprecated
  public BootstrapModule(ProjectReactor reactor, Object... boostrapperComponents) {
    this(new GlobalBatchProperties(), null, reactor, boostrapperComponents);
  }

  public BootstrapModule(GlobalBatchProperties globalProperties, String taskCommand, ProjectReactor reactor,
      Object... boostrapperComponents) {
    this.globalProperties = globalProperties;
    this.taskCommand = taskCommand;
    this.reactor = reactor;
    this.boostrapperComponents = boostrapperComponents;
  }

  @Override
  protected void configure() {
    container.addSingleton(globalProperties);
    if (reactor != null) {
      container.addSingleton(reactor);
    }
    container.addSingleton(new PropertiesConfiguration());
    container.addSingleton(BootstrapSettings.class);
    container.addSingleton(ServerClient.class);
    container.addSingleton(BatchSettings.class);
    container.addSingleton(BatchPluginRepository.class);
    container.addSingleton(ExtensionInstaller.class);
    container.addSingleton(Logback.class);
    container.addSingleton(ServerMetadata.class);
    container.addSingleton(org.sonar.batch.ServerMetadata.class);
    container.addSingleton(TempDirectories.class);
    container.addSingleton(HttpDownloader.class);
    container.addSingleton(UriReader.class);
    container.addSingleton(PluginDownloader.class);
    for (Object component : boostrapperComponents) {
      if (component != null) {
        container.addSingleton(component);
      }
    }
    if (!isMavenPluginExecutorRegistered()) {
      container.addSingleton(FakeMavenPluginExecutor.class);
    }
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
    Module taskBootstrap = installChild(new TaskBootstrapModule(taskCommand));
    taskBootstrap.start();
  }
}
