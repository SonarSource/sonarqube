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
import org.sonar.core.permission.GlobalPermission;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.RoleDao;
import org.sonar.core.user.UserDao;
import org.sonar.core.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
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

  private final RoleDao roleDao;
  private final UserDao userDao;
  private final ResourceDao resourceDao;
  private final PermissionFacade permissionFacade;

  public InternalPermissionService(RoleDao roleDao, UserDao userDao, ResourceDao resourceDao, PermissionFacade permissionFacade) {
    this.roleDao = roleDao;
    this.userDao = userDao;
    this.resourceDao = resourceDao;
    this.permissionFacade = permissionFacade;
  }

  public void addPermission(final Map<String, Object> params) {
    changePermission(ADD, params);
  }

  public void removePermission(Map<String, Object> params) {
    changePermission(REMOVE, params);
  }

  public void applyPermissionTemplate(Map<String, Object> params) {
    UserSession.get().checkLoggedIn();
    UserSession.get().checkGlobalPermission(GlobalPermission.SYSTEM_ADMIN);
    ApplyPermissionTemplateQuery query = ApplyPermissionTemplateQuery.buildFromParams(params);
    query.validate();
    for (String component : query.getSelectedComponents()) {
      permissionFacade.applyPermissionTemplate(query.getTemplateKey(), Long.parseLong(component));
    }
  }

  private void changePermission(String permissionChange, Map<String, Object> params) {
    UserSession.get().checkLoggedIn();
    UserSession.get().checkGlobalPermission(GlobalPermission.SYSTEM_ADMIN);
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
    List<String> existingPermissions = roleDao.selectGroupPermissions(permissionChangeQuery.group());
    if (shouldSkipPermissionChange(operation, existingPermissions, permissionChangeQuery.permission())) {
      LOG.info("Skipping permission change '{} {}' for group {} as it matches the current permission scheme",
        new String[]{operation, permissionChangeQuery.permission(), permissionChangeQuery.group()});
    } else {
      Long targetedGroup = getTargetedGroup(permissionChangeQuery.group());
      if (ADD.equals(operation)) {
        permissionFacade.insertGroupPermission(getComponentId(permissionChangeQuery.component()), targetedGroup, permissionChangeQuery.permission());
      } else {
        permissionFacade.deleteGroupPermission(getComponentId(permissionChangeQuery.component()), targetedGroup, permissionChangeQuery.permission());
      }
    }
  }

  private void applyUserPermissionChange(String operation, PermissionChangeQuery permissionChangeQuery) {
    List<String> existingPermissions = roleDao.selectUserPermissions(permissionChangeQuery.user());
    if (shouldSkipPermissionChange(operation, existingPermissions, permissionChangeQuery.permission())) {
      LOG.info("Skipping permission change '{} {}' for user {} as it matches the current permission scheme",
        new String[]{operation, permissionChangeQuery.permission(), permissionChangeQuery.user()});
    } else {
      Long targetedUser = getTargetedUser(permissionChangeQuery.user());
      if (ADD.equals(operation)) {
        permissionFacade.insertUserPermission(getComponentId(permissionChangeQuery.component()), targetedUser, permissionChangeQuery.permission());
      } else {
        permissionFacade.deleteUserPermission(getComponentId(permissionChangeQuery.component()), targetedUser, permissionChangeQuery.permission());
      }
    }
  }

  private Long getTargetedUser(String userLogin) {
    UserDto user = userDao.selectActiveUserByLogin(userLogin);
    if (user == null) {
      throw new BadRequestException("User " + userLogin + " does not exist");
    }
    return user.getId();
  }

  @Nullable
  private Long getTargetedGroup(String group) {
    if (DefaultGroups.isAnyone(group)) {
      return null;
    } else {
      GroupDto groupDto = userDao.selectGroupByName(group);
      if (groupDto == null) {
        throw new BadRequestException("Group " + group + " does not exist");
      }
      return groupDto.getId();
    }
  }

  private boolean shouldSkipPermissionChange(String operation, List<String> existingPermissions, String role) {
    return (ADD.equals(operation) && existingPermissions.contains(role)) ||
      (REMOVE.equals(operation) && !existingPermissions.contains(role));
  }

  @Nullable
  private Long getComponentId(String componentKey) {
    if (componentKey == null) {
      return null;
    } else {
      ResourceDto resourceDto = resourceDao.getResource(ResourceQuery.create().setKey(componentKey));
      if (resourceDto == null) {
        throw new BadRequestException("Component " + componentKey + " does not exists.");
      }
      return resourceDto.getId();
    }
  }
}
