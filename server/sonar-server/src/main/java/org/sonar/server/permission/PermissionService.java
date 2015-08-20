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
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceDto;
import org.sonar.db.permission.PermissionRepository;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.user.UserSession;

/**
 * Used by ruby code <pre>Internal.permissions</pre>
 */
@ServerSide
public class PermissionService {

  private enum Operation {
    ADD, REMOVE;
  }

  private static final String OBJECT_TYPE_USER = "User";
  private static final String OBJECT_TYPE_GROUP = "Group";
  private static final String NOT_FOUND_FORMAT = "%s %s does not exist";

  private final DbClient dbClient;
  private final PermissionRepository permissionRepository;
  private final PermissionFinder finder;
  private final IssueAuthorizationIndexer issueAuthorizationIndexer;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public PermissionService(DbClient dbClient, PermissionRepository permissionRepository, PermissionFinder finder,
    IssueAuthorizationIndexer issueAuthorizationIndexer, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.permissionRepository = permissionRepository;
    this.finder = finder;
    this.issueAuthorizationIndexer = issueAuthorizationIndexer;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  public List<String> globalPermissions() {
    return GlobalPermissions.ALL;
  }

  public UserWithPermissionQueryResult findUsersWithPermission(Map<String, Object> params) {
    return finder.findUsersWithPermission(PermissionQueryParser.toQuery(params));
  }

  public UserWithPermissionQueryResult findUsersWithPermissionTemplate(Map<String, Object> params) {
    return finder.findUsersWithPermissionTemplate(PermissionQueryParser.toQuery(params));
  }

  public GroupWithPermissionQueryResult findGroupsWithPermission(Map<String, Object> params) {
    return finder.findGroupsWithPermission(PermissionQueryParser.toQuery(params));
  }

  /**
   * To be used only by jruby webapp
   */
  public void addPermission(Map<String, Object> params) {
    PermissionChange change = PermissionChange.buildFromParams(params);
    DbSession session = dbClient.openSession(false);
    try {
      applyChange(Operation.ADD, change, session);
    } finally {
      dbClient.closeSession(session);
    }
  }

  /**
   * @deprecated since 5.2 use PermissionUpdate.addPermission instead
   */
  @Deprecated
  public void addPermission(PermissionChange change) {
    DbSession session = dbClient.openSession(false);
    try {
      applyChange(Operation.ADD, change, session);
    } finally {
      dbClient.closeSession(session);
    }
  }

  /**
   * To be used only by jruby webapp
   */
  public void removePermission(Map<String, Object> params) {
    PermissionChange change = PermissionChange.buildFromParams(params);
    DbSession session = dbClient.openSession(false);
    try {
      applyChange(Operation.REMOVE, change, session);
    } finally {
      session.close();
    }
  }

  /**
   * @deprecated since 5.2. Use PermissionUpdater.removePermission
   */
  @Deprecated
  public void removePermission(PermissionChange change) {
    DbSession session = dbClient.openSession(false);
    try {
      applyChange(Operation.REMOVE, change, session);
    } finally {
      session.close();
    }
  }

  public void applyDefaultPermissionTemplate(final String componentKey) {
    userSession.checkLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto component = componentFinder.getByKey(session, componentKey);
      ResourceDto provisioned = dbClient.resourceDao().selectProvisionedProject(session, componentKey);
      if (provisioned == null) {
        checkProjectAdminPermission(componentKey);
      } else {
        userSession.checkGlobalPermission(GlobalPermissions.PROVISIONING);
      }
      permissionRepository.grantDefaultRoles(session, component.getId(), component.qualifier());
      session.commit();
    } finally {
      session.close();
    }
    indexProjectPermissions();
  }

  public void applyPermissionTemplate(Map<String, Object> params) {
    userSession.checkLoggedIn();
    ApplyPermissionTemplateQuery query = ApplyPermissionTemplateQuery.buildFromParams(params);
    applyPermissionTemplate(query);
  }

  void applyPermissionTemplate(ApplyPermissionTemplateQuery query) {
    query.validate();

    boolean projectsChanged = false;
    DbSession session = dbClient.openSession(false);
    try {
      // If only one project is selected, check user has admin permission on it, otherwise we are in the case of a bulk change and only
      // system
      // admin has permission to do it
      if (query.getSelectedComponents().size() == 1) {
        checkProjectAdminPermission(query.getSelectedComponents().get(0));
      } else {
        checkProjectAdminPermission(null);
        userSession.checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
      }

      for (String componentKey : query.getSelectedComponents()) {
        ComponentDto component = componentFinder.getByKey(session, componentKey);
        permissionRepository.applyPermissionTemplate(session, query.getTemplateKey(), component.getId());
        projectsChanged = true;
      }
      session.commit();
    } finally {
      session.close();
    }
    if (projectsChanged) {
      indexProjectPermissions();
    }
  }

  private void applyChange(Operation operation, PermissionChange change, DbSession session) {
    userSession.checkLoggedIn();
    change.validate();
    boolean changed;
    if (change.userLogin() != null) {
      changed = applyChangeOnUser(session, operation, change);
    } else {
      changed = applyChangeOnGroup(session, operation, change);
    }
    if (changed) {
      session.commit();
      if (change.componentKey() != null) {
        indexProjectPermissions();
      }
    }
  }

  private boolean applyChangeOnGroup(DbSession session, Operation operation, PermissionChange permissionChange) {
    Long componentId = getComponentId(session, permissionChange.componentKey());
    checkProjectAdminPermission(permissionChange.componentKey());

    List<String> existingPermissions = dbClient.roleDao().selectGroupPermissions(session, permissionChange.groupName(), componentId);
    if (shouldSkipPermissionChange(operation, existingPermissions, permissionChange)) {
      return false;
    }

    Long targetedGroup = getTargetedGroup(session, permissionChange.groupName());
    String permission = permissionChange.permission();
    if (Operation.ADD == operation) {
      checkNotAnyoneAndAdmin(permission, permissionChange.groupName());
      permissionRepository.insertGroupPermission(componentId, targetedGroup, permission, session);
    } else {
      checkAdminUsersExistOutsideTheRemovedGroup(session, permissionChange, targetedGroup);
      permissionRepository.deleteGroupPermission(componentId, targetedGroup, permission, session);
    }
    return true;
  }

  private static void checkNotAnyoneAndAdmin(String permission, String group) {
    if (GlobalPermissions.SYSTEM_ADMIN.equals(permission)
      && DefaultGroups.isAnyone(group)) {
      throw new BadRequestException(String.format("It is not possible to add the '%s' permission to the '%s' group.", permission, group));
    }
  }

  private boolean applyChangeOnUser(DbSession session, Operation operation, PermissionChange permissionChange) {
    Long componentId = getComponentId(session, permissionChange.componentKey());
    checkProjectAdminPermission(permissionChange.componentKey());

    List<String> existingPermissions = dbClient.roleDao().selectUserPermissions(session, permissionChange.userLogin(), componentId);
    if (shouldSkipPermissionChange(operation, existingPermissions, permissionChange)) {
      return false;
    }

    Long targetedUser = getTargetedUser(session, permissionChange.userLogin());
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
    return badRequestIfNullResult(user, OBJECT_TYPE_USER, userLogin).getId();
  }

  @Nullable
  private Long getTargetedGroup(DbSession session, String group) {
    if (DefaultGroups.isAnyone(group)) {
      return null;
    } else {
      GroupDto groupDto = dbClient.groupDao().selectByName(session, group);
      return badRequestIfNullResult(groupDto, OBJECT_TYPE_GROUP, group).getId();
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

  private static <T> T badRequestIfNullResult(@Nullable T component, String objectType, String objectKey) {
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
