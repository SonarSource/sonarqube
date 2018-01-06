/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.sonar.process.systeminfo.JvmPropertiesSection;
import org.sonar.process.systeminfo.JvmStateSection;
import org.sonar.server.platform.monitoring.cluster.AppNodesInfoLoaderImpl;
import org.sonar.server.platform.monitoring.cluster.CeQueueGlobalSection;
import org.sonar.server.platform.monitoring.cluster.GlobalInfoLoader;
import org.sonar.server.platform.monitoring.cluster.GlobalSystemSection;
import org.sonar.server.platform.monitoring.cluster.LoggingSection;
import org.sonar.server.platform.monitoring.cluster.NodeSystemSection;
import org.sonar.server.platform.monitoring.cluster.ProcessInfoProvider;
import org.sonar.server.platform.monitoring.cluster.EsClusterStateSection;
import org.sonar.server.platform.monitoring.cluster.SearchNodesInfoLoaderImpl;
import org.sonar.server.platform.ws.ClusterSystemInfoWriter;
import org.sonar.server.platform.ws.InfoAction;
import org.sonar.server.platform.ws.StandaloneSystemInfoWriter;

public class WebSystemInfoModule {

  private WebSystemInfoModule() {
    // do not instantiate
  }

  public static Object[] forStandaloneMode() {
    return new Object[] {
      new JvmPropertiesSection("Web JVM Properties"),
      new JvmStateSection("Web JVM State"),
      DbSection.class,
      DbConnectionSection.class,
      EsStateSection.class,
      EsIndexesSection.class,
      LoggingSection.class,
      PluginsSection.class,
      SettingsSection.class,
      StandaloneSystemSection.class,

      OfficialDistribution.class,
      StandaloneSystemInfoWriter.class,
      InfoAction.class
    };
  }

  public static Object[] forClusterMode() {
    return new Object[] {
      new JvmPropertiesSection("Web JVM Properties"),
      new JvmStateSection("Web JVM State"),
      CeQueueGlobalSection.class,
      DbSection.class,
      DbConnectionSection.class,
      EsIndexesSection.class,
      EsClusterStateSection.class,
      GlobalSystemSection.class,
      LoggingSection.class,
      NodeSystemSection.class,
      PluginsSection.class,
      SettingsSection.class,

      OfficialDistribution.class,

      ProcessInfoProvider.class,
      GlobalInfoLoader.class,
      AppNodesInfoLoaderImpl.class,
      SearchNodesInfoLoaderImpl.class,
      ClusterSystemInfoWriter.class,
      InfoAction.class
    };
  }
}
