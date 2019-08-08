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
package org.sonar.server.qualityprofile.ws;

import org.sonar.core.platform.Module;

public class QProfilesWsModule extends Module {
  @Override
  protected void configureModule() {
    add(
      AddProjectAction.class,
      AddGroupAction.class,
      AddUserAction.class,
      BackupAction.class,
      ActivateRulesAction.class,
      DeactivateRulesAction.class,
      CompareAction.class,
      CopyAction.class,
      ChangelogAction.class,
      ChangeParentAction.class,
      CreateAction.class,
      DeleteAction.class,
      ExportAction.class,
      ExportersAction.class,
      ImportersAction.class,
      InheritanceAction.class,
      QProfilesWs.class,
      QProfileWsSupport.class,
      ProjectsAction.class,
      RenameAction.class,
      RemoveProjectAction.class,
      RemoveGroupAction.class,
      RemoveUserAction.class,
      RestoreAction.class,
      RestoreBuiltInAction.class,
      ActivateRuleAction.class,
      DeactivateRuleAction.class,
      SearchAction.class,
      SearchGroupsAction.class,
      SearchUsersAction.class,
      SetDefaultAction.class,
      ShowAction.class
    );
  }
}
