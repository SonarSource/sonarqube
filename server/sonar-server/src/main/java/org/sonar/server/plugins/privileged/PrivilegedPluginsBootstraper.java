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
package org.sonar.server.plugins.privileged;

import java.util.List;
import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerStartHandler;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.plugin.PrivilegedPluginBridge;

import static java.lang.String.format;

/**
 * Startup task to responsible to bootstrap installed Privileged plugins (if any).
 */
public class PrivilegedPluginsBootstraper implements ServerStartHandler {
  private static final Logger LOGGER = Loggers.get(PrivilegedPluginsBootstraper.class);

  private final ComponentContainer componentContainer;

  public PrivilegedPluginsBootstraper(ComponentContainer componentContainer) {
    this.componentContainer = componentContainer;
  }

  @Override
  public void onServerStart(Server server) {
    List<PrivilegedPluginBridge> bridges = componentContainer.getComponentsByType(PrivilegedPluginBridge.class);
    for (PrivilegedPluginBridge bridge : bridges) {
      Profiler profiler = Profiler.create(LOGGER).startInfo(format("Bootstrapping %s", bridge.getPluginName()));
      bridge.startPlugin(componentContainer);
      profiler.stopInfo();
    }
  }

}
