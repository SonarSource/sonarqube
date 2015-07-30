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

package org.sonar.server.permission.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionChange;

public class AddUserAction implements PermissionsWsAction {

  public static final String ACTION = "add_user";
  public static final String PARAM_PERMISSION = "permission";
  public static final String PARAM_USER_LOGIN = "login";

  private final PermissionService permissionService;

  public AddUserAction(PermissionService permissionService) {
    this.permissionService = permissionService;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setDescription("Add permission to a user.<br /> Requires 'Administer System' permission.")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_PERMISSION)
      .setDescription("Permission")
      .setRequired(true)
      .setPossibleValues(GlobalPermissions.ALL);

    action.createParam(PARAM_USER_LOGIN)
      .setRequired(true)
      .setDescription("User login")
      .setExampleValue("g.hopper");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String permission = request.mandatoryParam(PARAM_PERMISSION);
    String userLogin = request.mandatoryParam(PARAM_USER_LOGIN);
    permissionService.addPermission(
      new PermissionChange()
        .setPermission(permission)
        .setUser(userLogin)
      );

    response.noContent();
  }
}
