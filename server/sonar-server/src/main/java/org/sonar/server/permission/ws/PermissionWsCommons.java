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
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.ComponentPermissions;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.component.ComponentFinder;
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
    if (groupNameParam != null) {
      return groupNameParam;
    }

    GroupDto group = dbClient.groupDao().selectById(dbSession, groupId);
    if (group == null) {
      throw new NotFoundException(String.format("Group with id '%d' is not found", groupId));
    }

    return group.getName();
  }

  public PermissionChange buildUserPermissionChange(DbSession dbSession, PermissionRequest request) {
    PermissionChange permissionChange = new PermissionChange()
      .setPermission(request.permission())
      .setUserLogin(request.userLogin());
    addProjectToPermissionChange(dbSession, permissionChange, request);

    return permissionChange;
  }

  public PermissionChange buildGroupPermissionChange(DbSession dbSession, PermissionRequest request) {
    String groupName = searchGroupName(dbSession, request.groupName(), request.groupId());

    PermissionChange permissionChange = new PermissionChange()
      .setPermission(request.permission())
      .setGroupName(groupName);
    addProjectToPermissionChange(dbSession, permissionChange, request);

    return permissionChange;
  }

  private void addProjectToPermissionChange(DbSession dbSession, PermissionChange permissionChange, PermissionRequest request) {
    if (request.hasProject()) {
      ComponentDto project = componentFinder.getProjectByUuidOrKey(dbSession, request.projectUuid(), request.projectKey());
      permissionChange.setComponentKey(project.key());
    }
  }

  Optional<ComponentDto> searchProject(PermissionRequest request) {
    if (!request.hasProject()) {
      return Optional.absent();
    }

    DbSession dbSession = dbClient.openSession(false);
    try {
      return Optional.of(componentFinder.getProjectByUuidOrKey(dbSession, request.projectUuid(), request.projectKey()));
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  void checkPermissions(Optional<ComponentDto> project) {
    userSession.checkLoggedIn();

    if (userSession.hasGlobalPermission(SYSTEM_ADMIN) || projectPresentAndAdminPermissionsOnIt(project)) {
      return;
    }

    userSession.checkGlobalPermission(SYSTEM_ADMIN);
  }

  boolean projectPresentAndAdminPermissionsOnIt(Optional<ComponentDto> project) {
    return project.isPresent() && userSession.hasProjectPermissionByUuid(UserRole.ADMIN, project.get().projectUuid());
  }

  static void createPermissionParam(WebService.NewAction action) {
    action.createParam(PARAM_PERMISSION)
      .setDescription(PERMISSION_PARAM_DESCRIPTION)
      .setRequired(true)
      .setPossibleValues(POSSIBLE_PERMISSIONS);
  }
}
