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

import com.google.common.collect.ImmutableSet;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.permission.ComponentPermissions;
import org.sonar.core.permission.GlobalPermissions;

public class PermissionsWs implements WebService {

  @Override
  public void define(Context context) {
    NewController controller = context.createController("api/permissions");
    controller.setDescription("Permissions");
    controller.setSince("3.7");

    defineAddAction(controller);
    defineRemoveAction(controller);

    controller.done();
  }

  private void defineAddAction(NewController controller) {
    NewAction action = controller.createAction("add")
      .setDescription("Add a global or a project permission. Requires Administer System permission for global permissions, " +
        "requires Administer permission on project for project permissions")
      .setSince("3.7")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE);
    action.createParam("permission")
      .setDescription("Key of the permission to add")
      .setRequired(true)
      .setPossibleValues(ImmutableSet.<String>builder().addAll(GlobalPermissions.ALL).addAll(ComponentPermissions.ALL).build())
      .setExampleValue("shareDashboard");
    action.createParam("user")
      .setDescription("User login. Required if group is not set")
      .setExampleValue("myuser");
    action.createParam("group")
      .setDescription("Group name or \"" + DefaultGroups.ANYONE + "\". Required if user is not set")
      .setExampleValue(DefaultGroups.ADMINISTRATORS);
    action.createParam("component")
      .setDescription("Key of the project. Required if a project permission is set. Available since version 4.0")
      .setExampleValue("org.codehaus.sonar");
    RailsHandler.addFormatParam(action);
  }

  private void defineRemoveAction(NewController controller) {
    NewAction action = controller.createAction("remove")
      .setDescription("Remove a global or a project permission. Requires Administer System permission for global permissions, " +
        "requires Administer permission on project for project permissions")
      .setSince("3.7")
      .setPost(true)
      .setHandler(RailsHandler.INSTANCE);

    action.createParam("permission")
      .setDescription("Key of the permission to remove")
      .setRequired(true)
      .setPossibleValues(ImmutableSet.<String>builder().addAll(GlobalPermissions.ALL).addAll(ComponentPermissions.ALL).build())
      .setExampleValue("shareDashboard");
    action.createParam("user")
      .setDescription("User login. Required if group is not set")
      .setExampleValue("myuser");
    action.createParam("group")
      .setDescription("Group name or \"" + DefaultGroups.ANYONE + "\". Required if user is not set")
      .setExampleValue(DefaultGroups.ADMINISTRATORS);
    action.createParam("component")
      .setDescription("Key of the project. Required if a project permission is set. Available since version 4.0")
      .setExampleValue("org.codehaus.sonar");
    RailsHandler.addFormatParam(action);
  }

}
