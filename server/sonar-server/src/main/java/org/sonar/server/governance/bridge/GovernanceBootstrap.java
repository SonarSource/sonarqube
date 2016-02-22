/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.governance.bridge;

import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerStartHandler;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.server.governance.GovernanceBridge;

/**
 * Startup task to responsible to bootstrap the Governance plugin when it is installed.
 */
public class GovernanceBootstrap implements ServerStartHandler {
  private static final Logger LOGGER = Loggers.get(GovernanceBootstrap.class);

  private final ComponentContainer componentContainer;

  public GovernanceBootstrap(ComponentContainer componentContainer) {
    this.componentContainer = componentContainer;
  }

  @Override
  public void onServerStart(Server server) {
    GovernanceBridge governanceBridge = componentContainer.getComponentByType(GovernanceBridge.class);
    if (governanceBridge != null) {
      Profiler profiler = Profiler.create(LOGGER).startInfo("Bootstrapping Governance plugin");
      governanceBridge.startGovernance(componentContainer);
      profiler.stopInfo();
    }
  }

}
