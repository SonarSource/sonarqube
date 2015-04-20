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
package org.sonar.server.ui.ws;

import org.sonar.api.server.ws.WebService;

public class NavigationWs implements WebService {

  private final NavigationAction[] actions;

  public NavigationWs(NavigationAction... actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController navigation = context.createController("api/navigation")
      .setDescription("Get information required to build navigation UI components")
      .setSince("5.2");

    for (NavigationAction action : actions) {
      action.define(navigation);
    }

    navigation.done();
  }

}
