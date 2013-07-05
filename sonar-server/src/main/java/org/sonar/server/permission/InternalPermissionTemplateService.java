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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.core.user.*;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ServerErrorException;

import javax.annotation.CheckForNull;
import java.util.List;

/**
 * Used by ruby code <pre>Internal.permission_templates</pre>
 */
public class InternalPermissionTemplateService implements ServerComponent {

  private static final Logger LOG = LoggerFactory.getLogger(InternalPermissionTemplateService.class);

  private final PermissionDao permissionDao;
  private final UserDao userDao;

  public InternalPermissionTemplateService(PermissionDao permissionDao, UserDao userDao) {
    this.permissionDao = permissionDao;
    this.userDao = userDao;
  }

  @CheckForNull
  public PermissionTemplate selectPermissionTemplate(String templateName) {
    PermissionTemplateUpdater.checkUserCredentials();
    PermissionTemplateDto permissionTemplateDto = permissionDao.selectPermissionTemplate(templateName);
    return PermissionTemplate.create(permissionTemplateDto);
  }

  public List<PermissionTemplate> selectAllPermissionTemplates() {
    PermissionTemplateUpdater.checkUserCredentials();
    List<PermissionTemplate> permissionTemplates = Lists.newArrayList();
    List<PermissionTemplateDto> permissionTemplateDtos = permissionDao.selectAllPermissionTemplates();
    if(permissionTemplateDtos != null) {
      for (PermissionTemplateDto permissionTemplateDto : permissionTemplateDtos) {
        permissionTemplates.add(PermissionTemplate.create(permissionTemplateDto));
      }
    }
    return permissionTemplates;
  }

  public PermissionTemplate createPermissionTemplate(String name, String description) {
    PermissionTemplateUpdater.checkUserCredentials();
    checkThatTemplateNameIsUnique(name);
    PermissionTemplateDto permissionTemplateDto = permissionDao.createPermissionTemplate(name, description);
    if(permissionTemplateDto.getId() == null) {
      String errorMsg = "Template creation failed";
      LOG.error(errorMsg);
      throw new ServerErrorException(errorMsg);
    }
    return PermissionTemplate.create(permissionTemplateDto);
  }

  public void deletePermissionTemplate(String templateName) {
    PermissionTemplateUpdater.checkUserCredentials();
    PermissionTemplateDto permissionTemplateDto = permissionDao.selectTemplateByName(templateName);
    if(permissionTemplateDto == null) {
      String errorMsg = "Unknown template:" + templateName;
      LOG.error(errorMsg);
      throw new BadRequestException(errorMsg);
    } else {
      permissionDao.deletePermissionTemplate(permissionTemplateDto.getId());
    }
  }

  public void addUserPermission(String templateName, String permission, final String userLogin) {
    PermissionTemplateUpdater updater = new PermissionTemplateUpdater(templateName, permission, userLogin, permissionDao, userDao) {
      @Override
      protected void doExecute(Long templateId, String permission) {
        Long userId = getUserId();
        permissionDao.addUserPermission(templateId, userId, permission);
      }
    };
    updater.executeUpdate();
  }

  public void removeUserPermission(String templateName, String permission, String userLogin) {
    PermissionTemplateUpdater updater = new PermissionTemplateUpdater(templateName, permission, userLogin, permissionDao, userDao) {
      @Override
      protected void doExecute(Long templateId, String permission) {
        Long userId = getUserId();
        permissionDao.removeUserPermission(templateId, userId, permission);
      }
    };
    updater.executeUpdate();
  }

  public void addGroupPermission(String templateName, String permission, String groupName) {
    PermissionTemplateUpdater updater = new PermissionTemplateUpdater(templateName, permission, groupName, permissionDao, userDao) {
      @Override
      protected void doExecute(Long templateId, String permission) {
        Long groupId = getGroupId();
        permissionDao.addGroupPermission(templateId, groupId, permission);
      }
    };
    updater.executeUpdate();
  }

  public void removeGroupPermission(String templateName, String permission, String groupName) {
    PermissionTemplateUpdater updater = new PermissionTemplateUpdater(templateName, permission, groupName, permissionDao, userDao) {
      @Override
      protected void doExecute(Long templateId, String permission) {
        Long groupId = getGroupId();
        permissionDao.removeGroupPermission(templateId, groupId, permission);
      }
    };
    updater.executeUpdate();
  }

  private void checkThatTemplateNameIsUnique(String name) {
    List<PermissionTemplateDto> existingTemplates = permissionDao.selectAllPermissionTemplates();
    if(existingTemplates != null) {
      for (PermissionTemplateDto existingTemplate : existingTemplates) {
        if(existingTemplate.getName().equals(name)) {
          String errorMsg = "A template with that name already exists";
          LOG.error(errorMsg);
          throw new BadRequestException(errorMsg);
        }
      }
    }
  }
}
