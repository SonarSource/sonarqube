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

import org.sonar.api.ServerSide;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Used by ruby code <pre>Internal.permissions</pre>
 */
@ServerSide
public class InternalPermissionService {

  private enum Operation {
    ADD, REMOVE
  }

  private static final String OBJECT_TYPE_USER = "User";
  private static final String OBJECT_TYPE_GROUP = "Group";
  private static final String NOT_FOUND_FORMAT = "%s %s does not exist";

  private final DbClient dbClient;
  private final PermissionFacade permissionFacade;
  private final PermissionFinder finder;
  private final IssueAuthorizationIndexer issueAuthorizationIndexer;

  public InternalPermissionService(DbClient dbClient, PermissionFacade permissionFacade, PermissionFinder finder,
    IssueAuthorizationIndexer issueAuthorizationIndexer) {
    this.dbClient = dbClient;
    this.permissionFacade = permissionFacade;
    this.finder = finder;
    this.issueAuthorizationIndexer = issueAuthorizationIndexer;
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
    addPermission(PermissionChange.buildFromParams(params));
  }

  public void addPermission(PermissionChange change) {
    DbSession session = dbClient.openSession(false);
    try {
      applyChange(Operation.ADD, change, session);
    } finally {
      session.close();
    }
  }

  /**
   * To be used only by jruby webapp
   */
  public void removePermission(Map<String, Object> params) {
    removePermission(PermissionChange.buildFromParams(params));
  }

  public void removePermission(PermissionChange change) {
    DbSession session = dbClient.openSession(false);
    try {
      applyChange(Operation.REMOVE, change, session);
    } finally {
      session.close();
    }
  }

  public void applyDefaultPermissionTemplate(final String componentKey) {
    UserSession.get().checkLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto component = dbClient.componentDao().getByKey(session, componentKey);
      ResourceDto provisioned = dbClient.resourceDao().selectProvisionedProject(session, componentKey);
      if (provisioned == null) {
        checkProjectAdminPermission(componentKey);
      } else {
        UserSession.get().checkGlobalPermission(GlobalPermissions.PROVISIONING);
      }
      permissionFacade.grantDefaultRoles(session, component.getId(), component.qualifier());
      session.commit();
    } finally {
      session.close();
    }
    indexProjectPermissions();
  }

  public void applyPermissionTemplate(Map<String, Object> params) {
    UserSession.get().checkLoggedIn();
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
        UserSession.get().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
      }

      for (String componentKey : query.getSelectedComponents()) {
        ComponentDto component = dbClient.componentDao().getByKey(session, componentKey);
        permissionFacade.applyPermissionTemplate(session, query.getTemplateKey(), component.getId());
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
    UserSession.get().checkLoggedIn();
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

    List<String> existingPermissions = permissionFacade.selectGroupPermissions(session, permissionChange.group(), componentId);
    if (shouldSkipPermissionChange(operation, existingPermissions, permissionChange)) {
      return false;
    }

    Long targetedGroup = getTargetedGroup(session, permissionChange.group());
    if (Operation.ADD == operation) {
      permissionFacade.insertGroupPermission(componentId, targetedGroup, permissionChange.permission(), session);
    } else {
      permissionFacade.deleteGroupPermission(componentId, targetedGroup, permissionChange.permission(), session);
    }
    return true;
  }

  private boolean applyChangeOnUser(DbSession session, Operation operation, PermissionChange permissionChange) {
    Long componentId = getComponentId(session, permissionChange.component());
    checkProjectAdminPermission(permissionChange.component());

    List<String> existingPermissions = permissionFacade.selectUserPermissions(session, permissionChange.user(), componentId);
    if (shouldSkipPermissionChange(operation, existingPermissions, permissionChange)) {
      return false;
    }

    Long targetedUser = getTargetedUser(session, permissionChange.user());
    if (Operation.ADD == operation) {
      permissionFacade.insertUserPermission(componentId, targetedUser, permissionChange.permission(), session);
    } else {
      permissionFacade.deleteUserPermission(componentId, targetedUser, permissionChange.permission(), session);
    }
    return true;

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
      ComponentDto component = dbClient.componentDao().getByKey(session, componentKey);
      return component.getId();
    }
  }

  private Object badRequestIfNullResult(@Nullable Object component, String objectType, String objectKey) {
    if (component == null) {
      throw new BadRequestException(String.format(NOT_FOUND_FORMAT, objectType, objectKey));
    }
    return component;
  }

  private void checkProjectAdminPermission(@Nullable String projectKey) {
    if (projectKey == null) {
      UserSession.get().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
    } else {
      if (!UserSession.get().hasGlobalPermission(GlobalPermissions.SYSTEM_ADMIN) && !UserSession.get().hasProjectPermission(UserRole.ADMIN, projectKey)) {
        throw new ForbiddenException("Insufficient privileges");
      }
    }
  }

  private void indexProjectPermissions() {
    issueAuthorizationIndexer.index();
  }
}
