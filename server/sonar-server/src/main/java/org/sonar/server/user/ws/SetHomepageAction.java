/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.user.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

public class SetHomepageAction implements UsersWsAction {

  private static final String PARAM_TYPE = "type";
  private static final String PARAM_VALUE = "value";
  private static final String ACTION = "set_homepage";


  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION)
      .setPost(true)
      .setDescription("Set Homepage of current user.<br> Requires authentication.")
      .setSince("7.0")
      .setHandler(this);

    action.createParam(PARAM_TYPE)
      .setDescription("Type of the requested page")
      .setRequired(true)
      .setPossibleValues(HomepageTypes.keys());

    action.createParam(PARAM_VALUE)
      .setDescription("Additional information to filter the page (project or organization key)")
      .setExampleValue("my-project-key");

  }

  @Override
  public void handle(Request request, Response response) throws Exception {

    // WIP : Not implemented yet.
    response.noContent();

  }
}
