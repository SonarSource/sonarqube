/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDao;
import org.sonar.core.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
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

  private final UserDao userDao;
  private final ResourceDao resourceDao;
  private final PermissionFacade permissionFacade;
  private final PermissionFinder finder;

  public InternalPermissionService(UserDao userDao, ResourceDao resourceDao, PermissionFacade permissionFacade, PermissionFinder finder) {
    this.userDao = userDao;
    this.resourceDao = resourceDao;
    this.permissionFacade = permissionFacade;
    this.finder = finder;
  }

  public List<String> globalPermissions() {
    return GlobalPermissions.ALL;
  }

  public UserWithPermissionQueryResult findUsersWithPermission(Map<String, Object> params) {
    return finder.findUsersWithPermission(WithPermissionQueryParser.toQuery(params));
  }

  public UserWithPermissionQueryResult findUsersWithPermissionTemplate(Map<String, Object> params) {
    return finder.findUsersWithPermissionTemplate(WithPermissionQueryParser.toQuery(params));
  }

  public GroupWithPermissionQueryResult findGroupsWithPermission(Map<String, Object> params) {
    return finder.findGroupsWithPermission(WithPermissionQueryParser.toQuery(params));
  }

  public void addPermission(final Map<String, Object> params) {
    changePermission(ADD, params);
  }

  public void removePermission(Map<String, Object> params) {
    changePermission(REMOVE, params);
  }

  public void applyDefaultPermissionTemplate(final String componentKey) {
    UserSession.get().checkLoggedIn();

    ComponentDto component = (ComponentDto) resourceDao.findByKey(componentKey);
    badRequestIfNullResult(component, OBJECT_TYPE_COMPONENT, componentKey);

    ResourceDto provisioned = resourceDao.selectProvisionedProject(componentKey);
    if (provisioned == null) {
      checkProjectAdminPermission(componentKey);
    } else {
      UserSession.get().checkGlobalPermission(GlobalPermissions.PROVISIONING);
    }

    permissionFacade.grantDefaultRoles(component.getId(), component.qualifier());
  }

  public void applyPermissionTemplate(Map<String, Object> params) {
    UserSession.get().checkLoggedIn();

    ApplyPermissionTemplateQuery query = ApplyPermissionTemplateQuery.buildFromParams(params);
    query.validate();

    // If only one project is selected, check user has admin permission on it, otherwise we are in the case of a bulk change and only system
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
      permissionFacade.applyPermissionTemplate(query.getTemplateKey(), component.getId());
    }
  }

  private void changePermission(String permissionChange, Map<String, Object> params) {
    UserSession.get().checkLoggedIn();
    PermissionChangeQuery permissionChangeQuery = PermissionChangeQuery.buildFromParams(params);
    permissionChangeQuery.validate();
    applyPermissionChange(permissionChange, permissionChangeQuery);
  }

  private void applyPermissionChange(String operation, PermissionChangeQuery permissionChangeQuery) {
    if (permissionChangeQuery.targetsUser()) {
      applyUserPermissionChange(operation, permissionChangeQuery);
    } else {
      applyGroupPermissionChange(operation, permissionChangeQuery);
    }
  }

  private void applyGroupPermissionChange(String operation, PermissionChangeQuery permissionChangeQuery) {
    Long componentId = getComponentId(permissionChangeQuery.component());
    checkProjectAdminPermission(permissionChangeQuery.component());

    List<String> existingPermissions = permissionFacade.selectGroupPermissions(permissionChangeQuery.group(), componentId);
    if (shouldSkipPermissionChange(operation, existingPermissions, permissionChangeQuery)) {
      LOG.info("Skipping permission change '{} {}' for group {} as it matches the current permission scheme",
        new String[] {operation, permissionChangeQuery.permission(), permissionChangeQuery.group()});
    } else {
      Long targetedGroup = getTargetedGroup(permissionChangeQuery.group());
      if (ADD.equals(operation)) {
        permissionFacade.insertGroupPermission(componentId, targetedGroup, permissionChangeQuery.permission());
      } else {
        permissionFacade.deleteGroupPermission(componentId, targetedGroup, permissionChangeQuery.permission());
      }
    }
  }

  private void applyUserPermissionChange(String operation, PermissionChangeQuery permissionChangeQuery) {
    Long componentId = getComponentId(permissionChangeQuery.component());
    checkProjectAdminPermission(permissionChangeQuery.component());

    List<String> existingPermissions = permissionFacade.selectUserPermissions(permissionChangeQuery.user(), componentId);
    if (shouldSkipPermissionChange(operation, existingPermissions, permissionChangeQuery)) {
      LOG.info("Skipping permission change '{} {}' for user {} as it matches the current permission scheme",
        new String[] {operation, permissionChangeQuery.permission(), permissionChangeQuery.user()});
    } else {
      Long targetedUser = getTargetedUser(permissionChangeQuery.user());
      if (ADD.equals(operation)) {
        permissionFacade.insertUserPermission(componentId, targetedUser, permissionChangeQuery.permission());
      } else {
        permissionFacade.deleteUserPermission(componentId, targetedUser, permissionChangeQuery.permission());
      }
    }
  }

  private Long getTargetedUser(String userLogin) {
    UserDto user = userDao.selectActiveUserByLogin(userLogin);
    badRequestIfNullResult(user, OBJECT_TYPE_USER, userLogin);
    return user.getId();
  }

  @Nullable
  private Long getTargetedGroup(String group) {
    if (DefaultGroups.isAnyone(group)) {
      return null;
    } else {
      GroupDto groupDto = userDao.selectGroupByName(group);
      badRequestIfNullResult(groupDto, OBJECT_TYPE_GROUP, group);
      return groupDto.getId();
    }
  }

  private boolean shouldSkipPermissionChange(String operation, List<String> existingPermissions, PermissionChangeQuery permissionChangeQuery) {
    return (ADD.equals(operation) && existingPermissions.contains(permissionChangeQuery.permission())) ||
      (REMOVE.equals(operation) && !existingPermissions.contains(permissionChangeQuery.permission()));
  }

  @Nullable
  private Long getComponentId(String componentKey) {
    if (componentKey == null) {
      return null;
    } else {
      ResourceDto resourceDto = resourceDao.getResource(ResourceQuery.create().setKey(componentKey));
      badRequestIfNullResult(resourceDto, OBJECT_TYPE_COMPONENT, componentKey);
      return resourceDto.getId();
    }
  }

  private void badRequestIfNullResult(Object component, String objectType, String objectKey) {
    if (component == null) {
      throw new BadRequestException(String.format(NOT_FOUND_FORMAT, objectType, objectKey));
    }
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
}
