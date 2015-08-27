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
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.PermissionTemplateDao;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdminUser;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdminUserByComponentKey;
import static org.sonar.server.permission.ws.PermissionRequestValidator.MSG_TEMPLATE_NAME_NOT_BLANK;
import static org.sonar.server.permission.ws.PermissionRequestValidator.MSG_TEMPLATE_WITH_SAME_NAME;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateProjectPattern;
import static org.sonar.server.ws.WsUtils.checkRequest;

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
    PermissionTemplateDto permissionTemplateDto = permissionTemplateDao.selectByUuidWithUserAndGroupPermissions(templateKey);
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

  public PermissionTemplate createPermissionTemplate(String name, @Nullable String description, @Nullable String projectKeyPattern) {
    DbSession dbSession = dbClient.openSession(false);

    try {
      checkGlobalAdminUser(userSession);
      validateTemplateNameForCreation(dbSession, name);
      validateProjectPattern(projectKeyPattern);
      Date now = new Date(System2.INSTANCE.now());
      PermissionTemplateDto permissionTemplateDto = permissionTemplateDao.insert(dbSession, new PermissionTemplateDto()
        .setKee(Uuids.create())
        .setName(name)
        .setKeyPattern(projectKeyPattern)
        .setDescription(description)
        .setCreatedAt(now)
        .setUpdatedAt(now));
      dbSession.commit();
      return PermissionTemplate.create(permissionTemplateDto);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  public void updatePermissionTemplate(Long templateId, String newName, @Nullable String newDescription, @Nullable String projectPattern) {
    DbSession dbSession = dbClient.openSession(false);

    try {
      checkGlobalAdminUser(userSession);
      validateTemplateNameForUpdate(dbSession, newName, templateId);
      validateProjectPattern(projectPattern);
      permissionTemplateDao.update(templateId, newName, newDescription, projectPattern);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  public void deletePermissionTemplate(Long templateId) {
    checkGlobalAdminUser(userSession);
    DbSession dbSession = dbClient.openSession(false);
    try {
      permissionTemplateDao.deleteById(dbSession, templateId);
      dbSession.commit();
    } finally {
      dbClient.closeSession(dbSession);
    }
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

  /**
   * @deprecated since 5.2
   */
  @Deprecated
  private void validateTemplateNameForCreation(DbSession dbSession, String templateName) {
    checkRequest(!templateName.isEmpty(), MSG_TEMPLATE_NAME_NOT_BLANK);

    PermissionTemplateDto permissionTemplateWithSameName = dbClient.permissionTemplateDao().selectByName(dbSession, templateName);
    checkRequest(permissionTemplateWithSameName == null, format(MSG_TEMPLATE_WITH_SAME_NAME, templateName));
  }

  /**
   * @deprecated since 5.2
   */
  @Deprecated
  private void validateTemplateNameForUpdate(DbSession dbSession, String templateName, long templateId) {
    checkRequest(!templateName.isEmpty(), MSG_TEMPLATE_NAME_NOT_BLANK);

    PermissionTemplateDto permissionTemplateWithSameName = dbClient.permissionTemplateDao().selectByName(dbSession, templateName);
    checkRequest(permissionTemplateWithSameName == null || permissionTemplateWithSameName.getId() == templateId,
      format(MSG_TEMPLATE_WITH_SAME_NAME, templateName));
  }
}
