/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.monitoring;

import org.sonar.api.config.Configuration;
import org.sonar.core.platform.Module;
import org.sonar.process.systeminfo.JvmPropertiesSection;
import org.sonar.process.systeminfo.JvmStateSection;
import org.sonar.server.platform.WebServer;
import org.sonar.server.platform.monitoring.cluster.AppNodesInfoLoaderImpl;
import org.sonar.server.platform.monitoring.cluster.CeQueueGlobalSection;
import org.sonar.server.platform.monitoring.cluster.EsClusterStateSection;
import org.sonar.server.platform.monitoring.cluster.GlobalInfoLoader;
import org.sonar.server.platform.monitoring.cluster.GlobalSystemSection;
import org.sonar.server.platform.monitoring.cluster.NodeSystemSection;
import org.sonar.server.platform.monitoring.cluster.ProcessInfoProvider;
import org.sonar.server.platform.monitoring.cluster.SearchNodesInfoLoaderImpl;
import org.sonar.server.platform.ws.ClusterSystemInfoWriter;
import org.sonar.server.platform.ws.InfoAction;
import org.sonar.server.platform.ws.StandaloneSystemInfoWriter;

public class WebSystemInfoModule extends Module {
  private final Configuration configuration;
  private final WebServer webServer;

  public WebSystemInfoModule(Configuration configuration, WebServer webServer) {
    this.configuration = configuration;
    this.webServer = webServer;
  }

  @Override
  protected void configureModule() {
    boolean sonarcloud = configuration.getBoolean("sonar.sonarcloud.enabled").orElse(false);
    boolean standalone = webServer.isStandalone();

    add(
      new JvmPropertiesSection("Web JVM Properties"),
      new JvmStateSection("Web JVM State"),
      DbSection.class,
      DbConnectionSection.class,
      EsIndexesSection.class,
      LoggingSection.class,
      PluginsSection.class,
      SettingsSection.class,
      InfoAction.class

      );
    if (standalone || sonarcloud) {
      add(
        EsStateSection.class,
        StandaloneSystemSection.class,
        StandaloneSystemInfoWriter.class

      );
    } else {
      add(
        CeQueueGlobalSection.class,
        EsClusterStateSection.class,
        GlobalSystemSection.class,
        NodeSystemSection.class,

        ProcessInfoProvider.class,
        GlobalInfoLoader.class,
        AppNodesInfoLoaderImpl.class,
        SearchNodesInfoLoaderImpl.class,
        ClusterSystemInfoWriter.class

      );
    }
  }

}
