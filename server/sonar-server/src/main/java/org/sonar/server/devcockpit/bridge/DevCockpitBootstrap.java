/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.devcockpit.bridge;

import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerStartHandler;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.devcockpit.DevCockpitBridge;

/**
 * Startup task to responsible to bootstrap the Developer Cockpit plugin when it is installed.
 */
public class DevCockpitBootstrap implements ServerStartHandler {
  private static final Logger LOGGER = Loggers.get(DevCockpitBootstrap.class);

  private final ComponentContainer componentContainer;

  public DevCockpitBootstrap(ComponentContainer componentContainer) {
    this.componentContainer = componentContainer;
  }

  @Override
  public void onServerStart(Server server) {
    DevCockpitBridge bridge = componentContainer.getComponentByType(DevCockpitBridge.class);
    if (bridge != null) {
      Profiler profiler = Profiler.create(LOGGER).startInfo("Bootstrapping Developer Cockpit");
      bridge.startDevCockpit(componentContainer);
      profiler.stopInfo();
    }
  }

}
