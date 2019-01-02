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
package org.sonar.server.plugins.ws;

import org.sonar.api.server.ws.WebService;

/**
 * WebService bound to URL {@code api/plugins}.
 */
public class PluginsWs implements WebService {
  private final PluginsWsAction[] actions;

  public PluginsWs(PluginsWsAction... actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/plugins");
    controller.setDescription("Manage the plugins on the server, including installing, uninstalling, and upgrading.")
      .setSince("5.2");

    for (PluginsWsAction action : actions) {
      action.define(controller);
    }

    controller.done();
  }
}
