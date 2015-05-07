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

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerSide;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.PermissionTemplateDao;
import org.sonar.core.permission.PermissionTemplateDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDao;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Used by ruby code <pre>Internal.permission_templates</pre>
 */
@ServerSide
public class InternalPermissionTemplateService {

  private final MyBatis myBatis;
  private final PermissionTemplateDao permissionTemplateDao;
  private final UserDao userDao;
  private final PermissionFinder finder;

  public InternalPermissionTemplateService(MyBatis myBatis, PermissionTemplateDao permissionTemplateDao, UserDao userDao, PermissionFinder finder) {
    this.myBatis = myBatis;
    this.permissionTemplateDao = permissionTemplateDao;
    this.userDao = userDao;
    this.finder = finder;
  }

  public UserWithPermissionQueryResult findUsersWithPermissionTemplate(Map<String, Object> params) {
    return finder.findUsersWithPermissionTemplate(PermissionQueryParser.toQuery(params));
  }

  public GroupWithPermissionQueryResult findGroupsWithPermissionTemplate(Map<String, Object> params) {
    return finder.findGroupsWithPermissionTemplate(PermissionQueryParser.toQuery(params));
  }

  @CheckForNull
  public PermissionTemplate selectPermissionTemplate(String templateKey) {
    PermissionTemplateUpdater.checkSystemAdminUser();
    PermissionTemplateDto permissionTemplateDto = permissionTemplateDao.selectPermissionTemplate(templateKey);
    return PermissionTemplate.create(permissionTemplateDto);
  }

  public List<PermissionTemplate> selectAllPermissionTemplates() {
    return selectAllPermissionTemplates(null);
  }

  public List<PermissionTemplate> selectAllPermissionTemplates(@Nullable String componentKey) {
    PermissionTemplateUpdater.checkProjectAdminUser(componentKey);
    List<PermissionTemplate> permissionTemplates = Lists.newArrayList();
    List<PermissionTemplateDto> permissionTemplateDtos = permissionTemplateDao.selectAllPermissionTemplates();
    if (permissionTemplateDtos != null) {
      for (PermissionTemplateDto permissionTemplateDto : permissionTemplateDtos) {
        permissionTemplates.add(PermissionTemplate.create(permissionTemplateDto));
      }
    }
    return permissionTemplates;
  }

  public PermissionTemplate createPermissionTemplate(String name, @Nullable String description, @Nullable String keyPattern) {
    PermissionTemplateUpdater.checkSystemAdminUser();
    validateTemplateName(null, name);
    validateKeyPattern(keyPattern);
    PermissionTemplateDto permissionTemplateDto = permissionTemplateDao.createPermissionTemplate(name, description, keyPattern);
    return PermissionTemplate.create(permissionTemplateDto);
  }

  public void updatePermissionTemplate(Long templateId, String newName, @Nullable String newDescription, @Nullable String newKeyPattern) {
    PermissionTemplateUpdater.checkSystemAdminUser();
    validateTemplateName(templateId, newName);
    validateKeyPattern(newKeyPattern);
    permissionTemplateDao.updatePermissionTemplate(templateId, newName, newDescription, newKeyPattern);
  }

  public void deletePermissionTemplate(Long templateId) {
    PermissionTemplateUpdater.checkSystemAdminUser();
    permissionTemplateDao.deletePermissionTemplate(templateId);
  }

  public void addUserPermission(String templateKey, String permission, String userLogin) {
    PermissionTemplateUpdater updater = new PermissionTemplateUpdater(templateKey, permission, userLogin, permissionTemplateDao, userDao) {
      @Override
      protected void doExecute(Long templateId, String permission) {
        Long userId = getUserId();
        permissionTemplateDao.addUserPermission(templateId, userId, permission);
      }
    };
    updater.executeUpdate();
  }

  public void removeUserPermission(String templateKey, String permission, String userLogin) {
    PermissionTemplateUpdater updater = new PermissionTemplateUpdater(templateKey, permission, userLogin, permissionTemplateDao, userDao) {
      @Override
      protected void doExecute(Long templateId, String permission) {
        Long userId = getUserId();
        permissionTemplateDao.removeUserPermission(templateId, userId, permission);
      }
    };
    updater.executeUpdate();
  }

  public void addGroupPermission(String templateKey, String permission, String groupName) {
    PermissionTemplateUpdater updater = new PermissionTemplateUpdater(templateKey, permission, groupName, permissionTemplateDao, userDao) {
      @Override
      protected void doExecute(Long templateId, String permission) {
        Long groupId = getGroupId();
        permissionTemplateDao.addGroupPermission(templateId, groupId, permission);
      }
    };
    updater.executeUpdate();
  }

  public void removeGroupPermission(String templateKey, String permission, String groupName) {
    PermissionTemplateUpdater updater = new PermissionTemplateUpdater(templateKey, permission, groupName, permissionTemplateDao, userDao) {
      @Override
      protected void doExecute(Long templateId, String permission) {
        Long groupId = getGroupId();
        permissionTemplateDao.removeGroupPermission(templateId, groupId, permission);
      }
    };
    updater.executeUpdate();
  }

  public void removeGroupFromTemplates(String groupName) {
    UserSession.get().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
    DbSession session = myBatis.openSession(false);
    try {
      GroupDto group = userDao.selectGroupByName(groupName, session);
      if (group == null) {
        throw new NotFoundException("Group does not exists : " + groupName);
      }
      permissionTemplateDao.removeByGroup(group.getId(), session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void validateTemplateName(@Nullable Long templateId, String templateName) {
    if (StringUtils.isEmpty(templateName)) {
      String errorMsg = "Name can't be blank";
      throw new BadRequestException(errorMsg);
    }
    List<PermissionTemplateDto> existingTemplates = permissionTemplateDao.selectAllPermissionTemplates();
    if (existingTemplates != null) {
      for (PermissionTemplateDto existingTemplate : existingTemplates) {
        if ((templateId == null || !existingTemplate.getId().equals(templateId)) && (existingTemplate.getName().equals(templateName))) {
          String errorMsg = "A template with that name already exists";
          throw new BadRequestException(errorMsg);
        }
      }
    }
  }

  private void validateKeyPattern(@Nullable String keyPattern) {
    if (StringUtils.isEmpty(keyPattern)) {
      return;
    }
    try {
      Pattern.compile(keyPattern);
    } catch (PatternSyntaxException e) {
      String errorMsg = "Invalid pattern: " + keyPattern + ". Should be a valid Java regular expression.";
      throw new BadRequestException(errorMsg);
    }
  }

}
