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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDao;
import org.sonar.core.user.UserDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.issue.index.IssueAuthorizationIndex;
import org.sonar.server.search.IndexClient;
import org.sonar.server.user.UserSession;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Used by ruby code <pre>Internal.permissions</pre>
 */
public class InternalPermissionService implements ServerComponent {

  private static final Logger LOG = LoggerFactory.getLogger(InternalPermissionService.class);

  private static final String ADD = "add";
  private static final String REMOVE = "remove";

  private static final String OBJECT_TYPE_USER = "User";
  private static final String OBJECT_TYPE_COMPONENT = "Component";
  private static final String OBJECT_TYPE_GROUP = "Group";
  private static final String NOT_FOUND_FORMAT = "%s %s does not exist";

  private final DbClient dbClient;
  private final UserDao userDao;
  private final ResourceDao resourceDao;
  private final PermissionFacade permissionFacade;
  private final PermissionFinder finder;
  private final IndexClient index;

  public InternalPermissionService(DbClient dbClient, UserDao userDao, ResourceDao resourceDao, PermissionFacade permissionFacade, PermissionFinder finder,
    IndexClient index) {
    this.dbClient = dbClient;
    this.userDao = userDao;
    this.resourceDao = resourceDao;
    this.permissionFacade = permissionFacade;
    this.finder = finder;
    this.index = index;
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

  public void addPermission(final Map<String, Object> params) {
    changePermission(ADD, params);
  }

  public void removePermission(Map<String, Object> params) {
    changePermission(REMOVE, params);
  }

  public void applyDefaultPermissionTemplate(final String componentKey) {
    UserSession.get().checkLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto component = dbClient.componentDao().getByKey(session, componentKey);
      ResourceDto provisioned = resourceDao.selectProvisionedProject(session, componentKey);
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
    synchronizePermissions();
  }

  public void applyPermissionTemplate(Map<String, Object> params) {
    UserSession.get().checkLoggedIn();

    ApplyPermissionTemplateQuery query = ApplyPermissionTemplateQuery.buildFromParams(params);
    query.validate();

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
        ComponentDto component = (ComponentDto) resourceDao.findByKey(componentKey);
        if (component == null) {
          throw new IllegalStateException("Unable to find component with key " + componentKey);
        }
        permissionFacade.applyPermissionTemplate(session, query.getTemplateKey(), component.getId());
      }

      session.commit();
    } finally {
      session.close();
    }
    synchronizePermissions();
  }

  private void changePermission(String permissionChange, Map<String, Object> params) {
    UserSession.get().checkLoggedIn();
    PermissionChangeQuery permissionChangeQuery = PermissionChangeQuery.buildFromParams(params);
    permissionChangeQuery.validate();
    applyPermissionChange(permissionChange, permissionChangeQuery);
  }

  private void applyPermissionChange(String operation, PermissionChangeQuery permissionChangeQuery) {
    DbSession session = dbClient.openSession(false);
    boolean changed;
    try {
      if (permissionChangeQuery.targetsUser()) {
        changed = applyUserPermissionChange(session, operation, permissionChangeQuery);
      } else {
        changed = applyGroupPermissionChange(session, operation, permissionChangeQuery);
      }
      if (changed) {
        session.commit();
      }
    } finally {
      session.close();
    }
    if (changed) {
      synchronizePermissions();
    }
  }

  private boolean applyGroupPermissionChange(DbSession session, String operation, PermissionChangeQuery permissionChangeQuery) {
    Long componentId = getComponentId(session, permissionChangeQuery.component());
    checkProjectAdminPermission(permissionChangeQuery.component());

    List<String> existingPermissions = permissionFacade.selectGroupPermissions(session, permissionChangeQuery.group(), componentId);
    if (shouldSkipPermissionChange(operation, existingPermissions, permissionChangeQuery)) {
      LOG.info("Skipping permission change '{} {}' for group {} as it matches the current permission scheme",
        new String[] {operation, permissionChangeQuery.permission(), permissionChangeQuery.group()});
      return false;
    } else {
      Long targetedGroup = getTargetedGroup(session, permissionChangeQuery.group());
      if (ADD.equals(operation)) {
        permissionFacade.insertGroupPermission(componentId, targetedGroup, permissionChangeQuery.permission(), session);
      } else {
        permissionFacade.deleteGroupPermission(componentId, targetedGroup, permissionChangeQuery.permission(), session);
      }
      return true;
    }
  }

  private boolean applyUserPermissionChange(DbSession session, String operation, PermissionChangeQuery permissionChangeQuery) {
    Long componentId = getComponentId(session, permissionChangeQuery.component());
    checkProjectAdminPermission(permissionChangeQuery.component());

    List<String> existingPermissions = permissionFacade.selectUserPermissions(session, permissionChangeQuery.user(), componentId);
    if (shouldSkipPermissionChange(operation, existingPermissions, permissionChangeQuery)) {
      LOG.info("Skipping permission change '{} {}' for user {} as it matches the current permission scheme",
        new String[] {operation, permissionChangeQuery.permission(), permissionChangeQuery.user()});
      return false;
    } else {
      Long targetedUser = getTargetedUser(session, permissionChangeQuery.user());
      if (ADD.equals(operation)) {
        permissionFacade.insertUserPermission(componentId, targetedUser, permissionChangeQuery.permission(), session);
      } else {
        permissionFacade.deleteUserPermission(componentId, targetedUser, permissionChangeQuery.permission(), session);
      }
      return true;
    }
  }

  private Long getTargetedUser(DbSession session, String userLogin) {
    UserDto user = userDao.selectActiveUserByLogin(userLogin, session);
    badRequestIfNullResult(user, OBJECT_TYPE_USER, userLogin);
    return user.getId();
  }

  @Nullable
  private Long getTargetedGroup(DbSession session, String group) {
    if (DefaultGroups.isAnyone(group)) {
      return null;
    } else {
      GroupDto groupDto = userDao.selectGroupByName(group, session);
      badRequestIfNullResult(groupDto, OBJECT_TYPE_GROUP, group);
      return groupDto.getId();
    }
  }

  private boolean shouldSkipPermissionChange(String operation, List<String> existingPermissions, PermissionChangeQuery permissionChangeQuery) {
    return (ADD.equals(operation) && existingPermissions.contains(permissionChangeQuery.permission())) ||
      (REMOVE.equals(operation) && !existingPermissions.contains(permissionChangeQuery.permission()));
  }

  @Nullable
  private Long getComponentId(DbSession session, @Nullable String componentKey) {
    if (componentKey == null) {
      return null;
    } else {
      ResourceDto resourceDto = resourceDao.getResource(ResourceQuery.create().setKey(componentKey), session);
      badRequestIfNullResult(resourceDto, OBJECT_TYPE_COMPONENT, componentKey);
      return resourceDto.getId();
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

  private void synchronizePermissions() {
    // The index synchronizer cannot use an existing session, otherwise it's failing with the error : org.apache.ibatis.executor.ExecutorException: Executor was closed
    DbSession session = dbClient.openSession(false);
    try {
      dbClient.issueAuthorizationDao().synchronizeAfter(session, index.get(IssueAuthorizationIndex.class).getLastSynchronization());
    } finally {
      session.close();
    }
  }
}
