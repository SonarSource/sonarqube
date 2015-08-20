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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.ServerSide;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.PermissionTemplateDao;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdminUser;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdminUserByComponentKey;

/**
 * Used by ruby code <pre>Internal.permission_templates</pre>
 */
@ServerSide
public class PermissionTemplateService {

  private final DbClient dbClient;
  private final PermissionTemplateDao permissionTemplateDao;
  private final PermissionFinder finder;
  private final UserSession userSession;

  public PermissionTemplateService(DbClient dbClient, UserSession userSession, PermissionFinder finder) {
    this.dbClient = dbClient;
    this.permissionTemplateDao = dbClient.permissionTemplateDao();
    this.finder = finder;
    this.userSession = userSession;
  }

  public UserWithPermissionQueryResult findUsersWithPermissionTemplate(Map<String, Object> params) {
    return finder.findUsersWithPermissionTemplate(PermissionQueryParser.toQuery(params));
  }

  public GroupWithPermissionQueryResult findGroupsWithPermissionTemplate(Map<String, Object> params) {
    return finder.findGroupsWithPermissionTemplate(PermissionQueryParser.toQuery(params));
  }

  @CheckForNull
  public PermissionTemplate selectPermissionTemplate(String templateKey) {
    checkGlobalAdminUser(userSession);
    PermissionTemplateDto permissionTemplateDto = permissionTemplateDao.selectPermissionTemplate(templateKey);
    return PermissionTemplate.create(permissionTemplateDto);
  }

  public List<PermissionTemplate> selectAllPermissionTemplates() {
    return selectAllPermissionTemplates(null);
  }

  public List<PermissionTemplate> selectAllPermissionTemplates(@Nullable String componentKey) {
    checkProjectAdminUserByComponentKey(userSession, componentKey);
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
    checkGlobalAdminUser(userSession);
    validateTemplateName(null, name);
    validateKeyPattern(keyPattern);
    PermissionTemplateDto permissionTemplateDto = permissionTemplateDao.insertPermissionTemplate(name, description, keyPattern);
    return PermissionTemplate.create(permissionTemplateDto);
  }

  public void updatePermissionTemplate(Long templateId, String newName, @Nullable String newDescription, @Nullable String newKeyPattern) {
    checkGlobalAdminUser(userSession);
    validateTemplateName(templateId, newName);
    validateKeyPattern(newKeyPattern);
    permissionTemplateDao.updatePermissionTemplate(templateId, newName, newDescription, newKeyPattern);
  }

  public void deletePermissionTemplate(Long templateId) {
    checkGlobalAdminUser(userSession);
    permissionTemplateDao.deletePermissionTemplate(templateId);
  }

  /**
   * @deprecated since 5.2 can be removed when Ruby doesn't rely on PermissionTemplateService
   */
  @Deprecated
  public void addUserPermission(String templateKey, String permission, String userLogin) {
    PermissionTemplateUpdater updater = new PermissionTemplateUpdater(dbClient, userSession, templateKey, permission, userLogin) {
      @Override
      protected void doExecute(Long templateId, String permission) {
        Long userId = getUserId();
        permissionTemplateDao.insertUserPermission(templateId, userId, permission);
      }
    };
    updater.executeUpdate();
  }

  /**
   * @deprecated since 5.2 can be removed when Ruby doesn't rely on PermissionTemplateService
   */
  @Deprecated
  public void removeUserPermission(String templateKey, String permission, String userLogin) {
    PermissionTemplateUpdater updater = new PermissionTemplateUpdater(dbClient, userSession, templateKey, permission, userLogin) {
      @Override
      protected void doExecute(Long templateId, String permission) {
        Long userId = getUserId();
        permissionTemplateDao.deleteUserPermission(templateId, userId, permission);
      }
    };
    updater.executeUpdate();
  }

  /**
   * @deprecated since 5.2 can be removed when Ruby doesn't rely on PermissionTemplateService
   */
  @Deprecated
  public void addGroupPermission(String templateKey, String permission, String groupName) {
    PermissionTemplateUpdater updater = new PermissionTemplateUpdater(dbClient, userSession, templateKey, permission, groupName) {
      @Override
      protected void doExecute(Long templateId, String permission) {
        Long groupId = getGroupId();
        permissionTemplateDao.insertGroupPermission(templateId, groupId, permission);
      }
    };
    updater.executeUpdate();
  }

  /**
   * @deprecated since 5.2 can be removed when Ruby doesn't rely on PermissionTemplateService
   */
  @Deprecated
  public void removeGroupPermission(String templateKey, String permission, String groupName) {
    PermissionTemplateUpdater updater = new PermissionTemplateUpdater(dbClient, userSession, templateKey, permission, groupName) {
      @Override
      protected void doExecute(Long templateId, String permission) {
        Long groupId = getGroupId();
        permissionTemplateDao.deleteGroupPermission(templateId, groupId, permission);
      }
    };
    updater.executeUpdate();
  }

  public void removeGroupFromTemplates(String groupName) {
    userSession.checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);
    DbSession session = dbClient.openSession(false);
    try {
      GroupDto group = dbClient.groupDao().selectByName(session, groupName);
      if (group == null) {
        throw new NotFoundException("Group does not exists : " + groupName);
      }
      permissionTemplateDao.deleteByGroup(session, group.getId());
      session.commit();
    } finally {
      dbClient.closeSession(session);
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

  private static void validateKeyPattern(@Nullable String keyPattern) {
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
