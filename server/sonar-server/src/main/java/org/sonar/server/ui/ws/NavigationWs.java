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
package org.sonar.server.ui.ws;

import org.sonar.api.server.ws.WebService;

public class NavigationWs implements WebService {

  private final NavigationWsAction[] actions;

  public NavigationWs(NavigationWsAction... actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController navigation = context.createController("api/navigation")
      .setDescription("Get information required to build navigation UI components")
      .setSince("5.2");

    for (NavigationWsAction action : actions) {
      action.define(navigation);
    }

    navigation.done();
  }

}
