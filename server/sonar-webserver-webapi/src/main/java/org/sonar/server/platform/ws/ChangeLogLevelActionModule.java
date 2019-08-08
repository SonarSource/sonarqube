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
package org.sonar.server.platform.ws;

import org.sonar.api.config.Configuration;
import org.sonar.core.platform.Module;
import org.sonar.server.platform.WebServer;

public class ChangeLogLevelActionModule extends Module {
  private final WebServer webServer;
  private final Configuration configuration;

  public ChangeLogLevelActionModule(WebServer webServer, Configuration configuration) {
    this.webServer = webServer;
    this.configuration = configuration;
  }

  @Override
  protected void configureModule() {
    add(ChangeLogLevelAction.class);
    if (configuration.getBoolean("sonar.sonarcloud.enabled").orElse(false) || webServer.isStandalone()) {
      add(ChangeLogLevelStandaloneService.class);
    } else {
      add(ChangeLogLevelClusterService.class);
    }
  }
}
