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

import org.sonar.core.platform.Module;
import org.sonar.server.permission.ws.template.AddGroupToTemplateAction;
import org.sonar.server.permission.ws.template.AddUserToTemplateAction;
import org.sonar.server.permission.ws.template.ApplyTemplateAction;
import org.sonar.server.permission.ws.template.CreateTemplateAction;
import org.sonar.server.permission.ws.template.DefaultPermissionTemplateFinder;
import org.sonar.server.permission.ws.template.RemoveGroupFromTemplateAction;
import org.sonar.server.permission.ws.template.RemoveUserFromTemplateAction;
import org.sonar.server.permission.ws.template.SearchTemplatesAction;
import org.sonar.server.permission.ws.template.SearchTemplatesDataLoader;
import org.sonar.server.permission.ws.template.SetDefaultTemplateAction;
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
      RemoveGroupFromTemplateAction.class,
      CreateTemplateAction.class,
      UpdateTemplateAction.class,
      DeleteTemplateAction.class,
      ApplyTemplateAction.class,
      SetDefaultTemplateAction.class,
      SearchTemplatesAction.class,
      // utility classes
      PermissionChangeBuilder.class,
      SearchProjectPermissionsDataLoader.class,
      SearchTemplatesDataLoader.class,
      PermissionDependenciesFinder.class,
      DefaultPermissionTemplateFinder.class);
  }
}
