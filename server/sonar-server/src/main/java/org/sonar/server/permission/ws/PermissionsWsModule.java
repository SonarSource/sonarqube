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
package org.sonar.server.permission.ws;

import org.sonar.core.platform.Module;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.permission.ws.template.AddGroupToTemplateAction;
import org.sonar.server.permission.ws.template.AddProjectCreatorToTemplateAction;
import org.sonar.server.permission.ws.template.AddUserToTemplateAction;
import org.sonar.server.permission.ws.template.ApplyTemplateAction;
import org.sonar.server.permission.ws.template.BulkApplyTemplateAction;
import org.sonar.server.permission.ws.template.CreateTemplateAction;
import org.sonar.server.permission.ws.template.DeleteTemplateAction;
import org.sonar.server.permission.ws.template.RemoveGroupFromTemplateAction;
import org.sonar.server.permission.ws.template.RemoveProjectCreatorFromTemplateAction;
import org.sonar.server.permission.ws.template.RemoveUserFromTemplateAction;
import org.sonar.server.permission.ws.template.SearchTemplatesAction;
import org.sonar.server.permission.ws.template.SetDefaultTemplateAction;
import org.sonar.server.permission.ws.template.TemplateGroupsAction;
import org.sonar.server.permission.ws.template.TemplateUsersAction;
import org.sonar.server.permission.ws.template.UpdateTemplateAction;

public class PermissionsWsModule extends Module {
  @Override
  protected void configureModule() {
    add(
      PermissionsWs.class,
      // actions
      AddGroupAction.class,
      AddUserAction.class,
      RemoveGroupAction.class,
      RemoveUserAction.class,
      UsersAction.class,
      GroupsAction.class,
      SearchGlobalPermissionsAction.class,
      SearchProjectPermissionsAction.class,
      RemoveUserFromTemplateAction.class,
      AddUserToTemplateAction.class,
      AddGroupToTemplateAction.class,
      AddProjectCreatorToTemplateAction.class,
      RemoveProjectCreatorFromTemplateAction.class,
      RemoveGroupFromTemplateAction.class,
      CreateTemplateAction.class,
      UpdateTemplateAction.class,
      DeleteTemplateAction.class,
      ApplyTemplateAction.class,
      SetDefaultTemplateAction.class,
      SearchTemplatesAction.class,
      TemplateUsersAction.class,
      TemplateGroupsAction.class,
      BulkApplyTemplateAction.class,
      // utility classes
      PermissionWsSupport.class,
      PermissionServiceImpl.class,
      RequestValidator.class,
      WsParameters.class);
  }
}
