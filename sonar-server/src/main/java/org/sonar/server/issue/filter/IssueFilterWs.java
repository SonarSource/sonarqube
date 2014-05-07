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
package org.sonar.server.issue.filter;

import org.sonar.api.server.ws.WebService;

public class IssueFilterWs implements WebService {

  private final AppAction appAction;
  private final ShowAction showAction;
  private final FavoritesAction favoritesAction;

  public IssueFilterWs(AppAction appAction, ShowAction showAction, FavoritesAction favoritesAction) {
    this.appAction = appAction;
    this.showAction = showAction;
    this.favoritesAction = favoritesAction;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/issue_filters")
      .setSince("4.2")
      .setDescription("Issue Filters management");
    appAction.define(controller);
    showAction.define(controller);
    favoritesAction.define(controller);
    controller.done();
  }

}
