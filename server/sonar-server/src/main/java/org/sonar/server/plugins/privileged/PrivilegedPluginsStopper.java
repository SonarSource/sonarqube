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
import org.picocontainer.Startable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.plugin.PrivilegedPluginBridge;

import static java.lang.String.format;

/**
 * As an component of PlatformLevel4, this class is responsible for notifying shutdown to the installed Privileged plugins
 * (if any) when its installed.
 */
public class PrivilegedPluginsStopper implements Startable {
  private static final Logger LOGGER = Loggers.get(PrivilegedPluginsStopper.class);

  private final ComponentContainer platformContainer;

  public PrivilegedPluginsStopper(ComponentContainer platformContainer) {
    this.platformContainer = platformContainer;
  }

  @Override
  public void start() {
    // nothing to do, privileged plugins are started by PrivilegedPluginsBootstraper
  }

  @Override
  public void stop() {
    List<PrivilegedPluginBridge> bridges = platformContainer.getComponentsByType(PrivilegedPluginBridge.class);
    for (PrivilegedPluginBridge bridge : bridges) {
      Profiler profiler = Profiler.create(LOGGER).startInfo(format("Stopping %s", bridge.getPluginName()));
      bridge.stopPlugin();
      profiler.stopInfo();
    }
  }
}
