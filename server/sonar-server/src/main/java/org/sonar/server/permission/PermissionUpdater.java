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

package org.sonar.server.permission;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.PermissionRepository;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.user.UserSession;

public class PermissionUpdater {

  private enum Operation {
    ADD, REMOVE
  }

  private static final String OBJECT_TYPE_USER = "User";
  private static final String OBJECT_TYPE_GROUP = "Group";
  private static final String NOT_FOUND_FORMAT = "%s %s does not exist";

  private final DbClient dbClient;
  private final PermissionRepository permissionRepository;
  private final IssueAuthorizationIndexer issueAuthorizationIndexer;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public PermissionUpdater(DbClient dbClient, PermissionRepository permissionRepository,
    IssueAuthorizationIndexer issueAuthorizationIndexer, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.permissionRepository = permissionRepository;
    this.issueAuthorizationIndexer = issueAuthorizationIndexer;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  public static List<String> globalPermissions() {
    return GlobalPermissions.ALL;
  }

  public void addPermission(PermissionChange change) {
    DbSession session = dbClient.openSession(false);
    try {
      applyChange(Operation.ADD, change, session);
    } finally {
      dbClient.closeSession(session);
    }
  }

  public void removePermission(PermissionChange change) {
    DbSession session = dbClient.openSession(false);
    try {
      applyChange(Operation.REMOVE, change, session);
    } finally {
      session.close();
    }
  }

  private void applyChange(Operation operation, PermissionChange change, DbSession session) {
    userSession.checkLoggedIn();
    change.validate();
    boolean changed;
    if (change.user() != null) {
      changed = applyChangeOnUser(session, operation, change);
    } else {
      changed = applyChangeOnGroup(session, operation, change);
    }
    if (changed) {
      session.commit();
      if (change.component() != null) {
        indexProjectPermissions();
      }
    }
  }

  private boolean applyChangeOnGroup(DbSession session, Operation operation, PermissionChange permissionChange) {
    Long componentId = getComponentId(session, permissionChange.component());
    checkProjectAdminPermission(permissionChange.component());

    List<String> existingPermissions = dbClient.roleDao().selectGroupPermissions(session, permissionChange.group(), componentId);
    if (shouldSkipPermissionChange(operation, existingPermissions, permissionChange)) {
      return false;
    }

    Long targetedGroup = getTargetedGroup(session, permissionChange.group());
    String permission = permissionChange.permission();
    if (Operation.ADD == operation) {
      checkNotAnyoneAndAdmin(permission, permissionChange.group());
      permissionRepository.insertGroupPermission(componentId, targetedGroup, permission, session);
    } else {
      checkAdminUsersExistOutsideTheRemovedGroup(session, permissionChange, targetedGroup);
      permissionRepository.deleteGroupPermission(componentId, targetedGroup, permission, session);
    }
    return true;
  }

  private void checkNotAnyoneAndAdmin(String permission, String group) {
    if (GlobalPermissions.SYSTEM_ADMIN.equals(permission)
      && DefaultGroups.isAnyone(group)) {
      throw new BadRequestException(String.format("It is not possible to add the '%s' permission to the '%s' group.", permission, group));
    }
  }

  private boolean applyChangeOnUser(DbSession session, Operation operation, PermissionChange permissionChange) {
    Long componentId = getComponentId(session, permissionChange.component());
    checkProjectAdminPermission(permissionChange.component());

    List<String> existingPermissions = dbClient.roleDao().selectUserPermissions(session, permissionChange.user(), componentId);
    if (shouldSkipPermissionChange(operation, existingPermissions, permissionChange)) {
      return false;
    }

    Long targetedUser = getTargetedUser(session, permissionChange.user());
    if (Operation.ADD == operation) {
      permissionRepository.insertUserPermission(componentId, targetedUser, permissionChange.permission(), session);
    } else {
      checkOtherAdminUsersExist(session, permissionChange);
      permissionRepository.deleteUserPermission(componentId, targetedUser, permissionChange.permission(), session);
    }
    return true;

  }

  private void checkOtherAdminUsersExist(DbSession session, PermissionChange permissionChange) {
    if (GlobalPermissions.SYSTEM_ADMIN.equals(permissionChange.permission())
      && dbClient.roleDao().countUserPermissions(session, permissionChange.permission(), null) <= 1) {
      throw new BadRequestException(String.format("Last user with '%s' permission. Permission cannot be removed.", GlobalPermissions.SYSTEM_ADMIN));
    }
  }

  private void checkAdminUsersExistOutsideTheRemovedGroup(DbSession session, PermissionChange permissionChange, @Nullable Long groupIdToExclude) {
    if (GlobalPermissions.SYSTEM_ADMIN.equals(permissionChange.permission())
      && groupIdToExclude != null
      && dbClient.roleDao().countUserPermissions(session, permissionChange.permission(), groupIdToExclude) <= 0) {
      throw new BadRequestException(String.format("Last group with '%s' permission. Permission cannot be removed.", GlobalPermissions.SYSTEM_ADMIN));
    }
  }

  private Long getTargetedUser(DbSession session, String userLogin) {
    UserDto user = dbClient.userDao().selectActiveUserByLogin(session, userLogin);
    badRequestIfNullResult(user, OBJECT_TYPE_USER, userLogin);
    return user.getId();
  }

  @Nullable
  private Long getTargetedGroup(DbSession session, String group) {
    if (DefaultGroups.isAnyone(group)) {
      return null;
    } else {
      GroupDto groupDto = dbClient.userDao().selectGroupByName(group, session);
      badRequestIfNullResult(groupDto, OBJECT_TYPE_GROUP, group);
      return groupDto.getId();
    }
  }

  private boolean shouldSkipPermissionChange(Operation operation, List<String> existingPermissions, PermissionChange permissionChange) {
    return (Operation.ADD == operation && existingPermissions.contains(permissionChange.permission())) ||
      (Operation.REMOVE == operation && !existingPermissions.contains(permissionChange.permission()));
  }

  @CheckForNull
  private Long getComponentId(DbSession session, @Nullable String componentKey) {
    if (componentKey == null) {
      return null;
    } else {
      ComponentDto component = componentFinder.getByKey(session, componentKey);
      return component.getId();
    }
  }

  private static Object badRequestIfNullResult(@Nullable Object component, String objectType, String objectKey) {
    if (component == null) {
      throw new BadRequestException(String.format(NOT_FOUND_FORMAT, objectType, objectKey));
    }
    return component;
  }

  private void checkProjectAdminPermission(@Nullable String projectKey) {
    if (projectKey == null) {
      userSession.checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
    } else {
      if (!userSession.hasGlobalPermission(GlobalPermissions.SYSTEM_ADMIN) && !userSession.hasProjectPermission(UserRole.ADMIN, projectKey)) {
        throw new ForbiddenException("Insufficient privileges");
      }
    }
  }

  private void indexProjectPermissions() {
    issueAuthorizationIndexer.index();
  }
}
