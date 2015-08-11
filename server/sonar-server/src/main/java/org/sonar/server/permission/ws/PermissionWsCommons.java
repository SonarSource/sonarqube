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

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSortedSet;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.ComponentPermissions;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.user.UserSession;

import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;

public class PermissionWsCommons {

  public static final String PARAM_PERMISSION = "permission";
  public static final String PARAM_GROUP_NAME = "groupName";
  public static final String PARAM_GROUP_ID = "groupId";
  public static final String PARAM_PROJECT_UUID = "projectId";
  public static final String PARAM_PROJECT_KEY = "projectKey";
  public static final String PARAM_USER_LOGIN = "login";
  static final String PROJECT_PERMISSIONS_ONE_LINE = Joiner.on(", ").join(ComponentPermissions.ALL);
  static final String GLOBAL_PERMISSIONS_ONE_LINE = Joiner.on(", ").join(GlobalPermissions.ALL);
  private static final String PERMISSION_PARAM_DESCRIPTION = String.format("Permission" +
    "<ul>" +
    "<li>Possible values for global permissions: %s</li>" +
    "<li>Possible values for project permissions %s</li>" +
    "</ul>",
    GLOBAL_PERMISSIONS_ONE_LINE,
    PROJECT_PERMISSIONS_ONE_LINE);
  private static final ImmutableSortedSet<Comparable<?>> POSSIBLE_PERMISSIONS = ImmutableSortedSet.naturalOrder()
    .addAll(GlobalPermissions.ALL)
    .addAll(ComponentPermissions.ALL)
    .build();

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserSession userSession;

  public PermissionWsCommons(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userSession = userSession;
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

  public PermissionChange buildUserPermissionChange(Request request) {
    String permission = request.mandatoryParam(PARAM_PERMISSION);
    String userLogin = request.mandatoryParam(PARAM_USER_LOGIN);

    DbSession dbSession = dbClient.openSession(false);
    try {
      PermissionChange permissionChange = new PermissionChange()
        .setPermission(permission)
        .setUserLogin(userLogin);
      addProjectToPermissionChange(dbSession, permissionChange, request.param(PARAM_PROJECT_UUID), request.param(PARAM_PROJECT_KEY));

      return permissionChange;
    } finally {
      dbClient.closeSession(dbSession);
    }

  }

  public PermissionChange buildGroupPermissionChange(DbSession dbSession, Request request) {
    String permission = request.mandatoryParam(PARAM_PERMISSION);
    String groupNameParam = request.param(PARAM_GROUP_NAME);
    Long groupId = request.paramAsLong(PARAM_GROUP_ID);
    String projectUuid = request.param(PARAM_PROJECT_UUID);
    String projectKey = request.param(PARAM_PROJECT_KEY);

    String groupName = searchGroupName(dbSession, groupNameParam, groupId);

    PermissionChange permissionChange = new PermissionChange()
      .setPermission(permission)
      .setGroupName(groupName);
    addProjectToPermissionChange(dbSession, permissionChange, projectUuid, projectKey);

    return permissionChange;
  }

  private void addProjectToPermissionChange(DbSession dbSession, PermissionChange permissionChange, @Nullable String projectUuid, @Nullable String projectKey) {
    ComponentDto project = null;
    if (isProjectUuidOrProjectKeyProvided(projectUuid, projectKey)) {
      project = componentFinder.getProjectByUuidOrKey(dbSession, projectUuid, projectKey);
      permissionChange.setComponentKey(project.key());
    }

    checkPermissionAndProjectParameters(permissionChange.permission(), Optional.fromNullable(project));
  }

  private static void checkParameters(@Nullable String groupName, @Nullable Long groupId) {
    if (groupName != null ^ groupId != null) {
      return;
    }

    throw new BadRequestException("Group name or group id must be provided, not both");
  }

  static boolean isProjectUuidOrProjectKeyProvided(@Nullable String projectUuid, @Nullable String projectKey) {
    return projectUuid != null || projectKey != null;
  }

  Optional<ComponentDto> searchProject(Request request) {
    String projectUuid = request.param(PARAM_PROJECT_UUID);
    String projectKey = request.param(PARAM_PROJECT_KEY);

    DbSession dbSession = dbClient.openSession(false);
    try {
      if (isProjectUuidOrProjectKeyProvided(projectUuid, projectKey)) {
        return Optional.of(componentFinder.getProjectByUuidOrKey(dbSession, projectUuid, projectKey));
      }
      return Optional.absent();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  void checkPermissions(Optional<ComponentDto> project, String permission) {
    checkPermissionAndProjectParameters(permission, project);
    userSession.checkLoggedIn();

    if (userSession.hasGlobalPermission(SYSTEM_ADMIN) || projectPresentAndAdminPermissionsOnIt(project)) {
      return;
    }

    userSession.checkGlobalPermission(SYSTEM_ADMIN);
  }

  boolean projectPresentAndAdminPermissionsOnIt(Optional<ComponentDto> project) {
    return project.isPresent() && userSession.hasProjectPermissionByUuid(UserRole.ADMIN, project.get().projectUuid());
  }

  static void checkPermissionAndProjectParameters(String permission, Optional<ComponentDto> project) {
    if (project.isPresent()) {
      if (!ComponentPermissions.ALL.contains(permission)) {
        throw new BadRequestException(String.format("Incorrect value '%s' for project permissions. Values allowed: %s.", permission, PROJECT_PERMISSIONS_ONE_LINE));
      }
    } else {
      if (!GlobalPermissions.ALL.contains(permission)) {
        throw new BadRequestException(String.format("Incorrect value '%s' for global permissions. Values allowed: %s.", permission, GLOBAL_PERMISSIONS_ONE_LINE));
      }
    }
  }

  static void createPermissionParam(WebService.NewAction action) {
    action.createParam(PARAM_PERMISSION)
      .setDescription(PERMISSION_PARAM_DESCRIPTION)
      .setRequired(true)
      .setPossibleValues(POSSIBLE_PERMISSIONS);
  }
}
