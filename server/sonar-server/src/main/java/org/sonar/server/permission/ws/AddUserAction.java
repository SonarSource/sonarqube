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
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.permission.PermissionUpdater;

import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PROJECT_KEY;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PROJECT_UUID;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_USER_LOGIN;
import static org.sonar.server.permission.ws.PermissionWsCommons.createPermissionParam;

public class AddUserAction implements PermissionsWsAction {

  public static final String ACTION = "add_user";

  private final PermissionUpdater permissionUpdater;
  private final PermissionWsCommons permissionWsCommons;

  public AddUserAction(PermissionUpdater permissionUpdater, PermissionWsCommons permissionWsCommons) {
    this.permissionUpdater = permissionUpdater;
    this.permissionWsCommons = permissionWsCommons;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setDescription("Add permission to a user.<br /> " +
        "If the project id or project key is provided, a project permission is created.<br />" +
        "Requires 'Administer System' permission.")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    createPermissionParam(action);

    action.createParam(PARAM_USER_LOGIN)
      .setRequired(true)
      .setDescription("User login")
      .setExampleValue("g.hopper");

    action.createParam(PARAM_PROJECT_UUID)
      .setDescription("Project id")
      .setExampleValue("ce4c03d6-430f-40a9-b777-ad877c00aa4d");

    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("Project key")
      .setExampleValue("org.apache.hbas:hbase");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    PermissionChange permissionChange = permissionWsCommons.buildUserPermissionChange(request);
    permissionUpdater.addPermission(permissionChange);

    response.noContent();
  }
}
