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

import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.permission.PermissionChange;

public class PermissionWsCommons {

  public static final String PARAM_PERMISSION = "permission";
  public static final String PARAM_GROUP_NAME = "groupName";
  public static final String PARAM_GROUP_ID = "groupId";
  public static final String PARAM_PROJECT_ID = "projectId";
  public static final String PARAM_PROJECT_KEY = "projectKey";

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;

  public PermissionWsCommons(DbClient dbClient, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
  }

  public String searchGroupName(DbSession dbSession, @Nullable String groupNameParam, @Nullable Long groupId) {
    checkParameters(groupNameParam, groupId);
    if (groupNameParam != null) {
      return groupNameParam;
    }

    GroupDto group = dbClient.groupDao().selectById(dbSession, groupId);
    if (group == null) {
      throw new NotFoundException(String.format("Group with id '%d' is not found", groupId));
    }

    return group.getName();
  }

  public PermissionChange buildGroupPermissionChange(DbSession dbSession, Request request) {
    String permission = request.mandatoryParam(PARAM_PERMISSION);
    String groupNameParam = request.param(PARAM_GROUP_NAME);
    Long groupId = request.paramAsLong(PARAM_GROUP_ID);
    String projectUuid = request.param(PARAM_PROJECT_ID);
    String projectKey = request.param(PARAM_PROJECT_KEY);

    String groupName = searchGroupName(dbSession, groupNameParam, groupId);

    PermissionChange permissionChange = new PermissionChange()
      .setPermission(permission)
      .setGroup(groupName);
    if (isProjectUuidOrProjectKeyProvided(projectUuid, projectKey)) {
      ComponentDto project = componentFinder.getProjectByUuidOrKey(dbSession, projectUuid, projectKey);
      permissionChange.setComponentKey(project.key());
    }

    return permissionChange;
  }

  private static void checkParameters(@Nullable String groupName, @Nullable Long groupId) {
    if (groupName != null ^ groupId != null) {
      return;
    }

    throw new BadRequestException("Group name or group id must be provided, not both");
  }

  private static boolean isProjectUuidOrProjectKeyProvided(@Nullable String projectUuid, @Nullable String projectKey) {
    return projectUuid != null || projectKey != null;
  }
}
