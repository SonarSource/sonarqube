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
package org.sonar.server.platform;

import org.sonar.core.platform.Module;
import org.sonar.process.systeminfo.JvmPropertiesSection;
import org.sonar.process.systeminfo.JvmStateSection;
import org.sonar.server.platform.monitoring.DbConnectionSection;
import org.sonar.server.platform.monitoring.DbSection;
import org.sonar.server.platform.monitoring.EsIndexesSection;
import org.sonar.server.platform.monitoring.EsStateSection;
import org.sonar.server.platform.monitoring.LoggingSection;
import org.sonar.server.platform.monitoring.PluginsSection;
import org.sonar.server.platform.monitoring.SettingsSection;
import org.sonar.server.platform.monitoring.StandaloneSystemSection;
import org.sonar.server.platform.monitoring.cluster.AppNodesInfoLoaderImpl;
import org.sonar.server.platform.monitoring.cluster.CeQueueGlobalSection;
import org.sonar.server.platform.monitoring.cluster.EsClusterStateSection;
import org.sonar.server.platform.monitoring.cluster.GlobalInfoLoader;
import org.sonar.server.platform.monitoring.cluster.GlobalSystemSection;
import org.sonar.server.platform.monitoring.cluster.NodeSystemSection;
import org.sonar.server.platform.monitoring.cluster.ProcessInfoProvider;
import org.sonar.server.platform.monitoring.cluster.SearchNodesInfoLoaderImpl;

public class SystemInfoWriterModule extends Module {
  private final WebServer webServer;

  public SystemInfoWriterModule(WebServer webServer) {
    this.webServer = webServer;
  }

  @Override
  protected void configureModule() {
    boolean standalone = webServer.isStandalone();

    add(
      new JvmPropertiesSection("Web JVM Properties"),
      new JvmStateSection("Web JVM State"),
      DbSection.class,
      DbConnectionSection.class,
      EsIndexesSection.class,
      LoggingSection.class,
      PluginsSection.class,
      SettingsSection.class

      );
    if (standalone) {
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
