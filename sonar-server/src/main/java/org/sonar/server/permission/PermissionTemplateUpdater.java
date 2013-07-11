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

import com.google.common.collect.Lists;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.user.*;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;

import java.util.List;

abstract class PermissionTemplateUpdater {

  private final String templateName;
  private final String permission;
  private final String updatedReference;
  private final PermissionDao permissionDao;
  private final UserDao userDao;

  PermissionTemplateUpdater(String templateName, String permission, String updatedReference, PermissionDao permissionDao, UserDao userDao) {
    this.templateName = templateName;
    this.permission = permission;
    this.updatedReference = updatedReference;
    this.permissionDao = permissionDao;
    this.userDao = userDao;
  }

  void executeUpdate() {
    checkUserCredentials();
    Long templateId = getTemplateId(templateName);
    validatePermission(permission);
    doExecute(templateId, permission);
  }

  abstract void doExecute(Long templateId, String permission);

  Long getUserId() {
    UserDto userDto = userDao.selectActiveUserByLogin(updatedReference);
    if(userDto == null) {
      throw new BadRequestException("Unknown user: " + updatedReference);
    }
    return userDto.getId();
  }

  Long getGroupId() {
    if(DefaultGroups.isAnyone(updatedReference)) {
      return null;
    }
    GroupDto groupDto = userDao.selectGroupByName(updatedReference);
    if(groupDto == null) {
      throw new BadRequestException("Unknown group: " + updatedReference);
    }
    return groupDto.getId();
  }

  static void checkUserCredentials() {
    UserSession currentSession = UserSession.get();
    currentSession.checkLoggedIn();
    currentSession.checkGlobalPermission(Permission.SYSTEM_ADMIN);
  }

  private void validatePermission(String permission) {
    List<String> supportedPermissions = Lists.newArrayList(UserRole.ADMIN, UserRole.CODEVIEWER, UserRole.USER);
    if(permission == null || !supportedPermissions.contains(permission)) {
      throw new BadRequestException("Invalid permission: " + permission);
    }
  }

  private Long getTemplateId(String name) {
    PermissionTemplateDto permissionTemplateDto = permissionDao.selectTemplateByName(name);
    if(permissionTemplateDto == null) {
      throw new BadRequestException("Unknown template: " + name);
    }
    return permissionTemplateDto.getId();
  }
}
