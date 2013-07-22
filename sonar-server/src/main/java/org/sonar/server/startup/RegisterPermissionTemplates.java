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

package org.sonar.server.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.PermissionDao;
import org.sonar.core.permission.PermissionTemplateDto;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.core.template.LoadedTemplateDto;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDao;
import org.sonar.server.platform.PersistentSettings;

public class RegisterPermissionTemplates {

  public static final String DEFAULT_TEMPLATE_PROPERTY = "sonar.permission.template.default";
  public static final String DEFAULT_PROJECTS_TEMPLATE_PROPERTY = "sonar.permission.template.TRK.default";

  private static final Logger LOG = LoggerFactory.getLogger(RegisterPermissionTemplates.class);

  private final LoadedTemplateDao loadedTemplateDao;
  private final PermissionDao permissionDao;
  private final UserDao userDao;
  private final PersistentSettings settings;

  public RegisterPermissionTemplates(LoadedTemplateDao loadedTemplateDao, PermissionDao permissionDao,
                                     UserDao userDao, PersistentSettings settings) {
    this.loadedTemplateDao = loadedTemplateDao;
    this.permissionDao = permissionDao;
    this.userDao = userDao;
    this.settings = settings;
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler(LOG).start("Register permission templates");

    if(shouldRegister()) {
      if(hasExistingPermissionsConfig()) {
        String projectsPermissionsKey = settings.getString(DEFAULT_PROJECTS_TEMPLATE_PROPERTY);
        setDefaultProperty(projectsPermissionsKey);
      } else {
        insertDefaultTemplate(PermissionTemplateDto.DEFAULT.getName());
        setDefaultProperty(PermissionTemplateDto.DEFAULT.getKee());
      }
      registerInitialization();
    }
    profiler.stop();
  }

  private boolean hasExistingPermissionsConfig() {
    return settings.getString(DEFAULT_PROJECTS_TEMPLATE_PROPERTY) != null;
  }

  private boolean shouldRegister() {
    return loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.PERMISSION_TEMPLATE_TYPE, PermissionTemplateDto.DEFAULT.getKee()) == 0;
  }

  private void insertDefaultTemplate(String templateName) {
    PermissionTemplateDto defaultPermissionTemplate = permissionDao
      .createPermissionTemplate(templateName, PermissionTemplateDto.DEFAULT.getDescription());
    addGroupPermission(defaultPermissionTemplate, UserRole.ADMIN, DefaultGroups.ADMINISTRATORS);
    addGroupPermission(defaultPermissionTemplate, UserRole.USER, DefaultGroups.USERS);
    addGroupPermission(defaultPermissionTemplate, UserRole.USER, DefaultGroups.ANYONE);
    addGroupPermission(defaultPermissionTemplate, UserRole.CODEVIEWER, DefaultGroups.USERS);
    addGroupPermission(defaultPermissionTemplate, UserRole.CODEVIEWER, DefaultGroups.ANYONE);
  }

  private void addGroupPermission(PermissionTemplateDto template, String permission, String groupName) {
    Long groupId;
    if(DefaultGroups.isAnyone(groupName)) {
      groupId = null;
    } else {
      GroupDto groupDto = userDao.selectGroupByName(groupName);
      if(groupDto != null) {
        groupId = groupDto.getId();
      } else {
        throw new IllegalArgumentException("Cannot setup default permission for group: " + groupName);
      }
    }
    permissionDao.addGroupPermission(template.getId(), groupId, permission);
  }

  private void registerInitialization() {
    LoadedTemplateDto loadedTemplate = new LoadedTemplateDto(PermissionTemplateDto.DEFAULT.getKee(),
      LoadedTemplateDto.PERMISSION_TEMPLATE_TYPE);
    loadedTemplateDao.insert(loadedTemplate);
  }

  private void setDefaultProperty(String defaultTemplate) {
    if (settings.getString(DEFAULT_TEMPLATE_PROPERTY) == null) {
      LOG.info("Set default permission template: " + defaultTemplate);
      settings.saveProperty(DEFAULT_TEMPLATE_PROPERTY, defaultTemplate);
    }
  }
}
