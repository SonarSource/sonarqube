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

package org.sonar.server.startup;

import org.sonar.api.resources.Qualifiers;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.permission.DefaultPermissionTemplates;
import org.sonar.server.platform.PersistentSettings;

import static org.sonar.server.permission.DefaultPermissionTemplates.DEFAULT_TEMPLATE_PROPERTY;
import static org.sonar.server.permission.DefaultPermissionTemplates.defaultRootQualifierTemplateProperty;

public class RegisterPermissionTemplates {

  private static final Logger LOG = Loggers.get(RegisterPermissionTemplates.class);

  private final DbClient dbClient;
  private final PersistentSettings settings;

  public RegisterPermissionTemplates(DbClient dbClient, PersistentSettings settings) {
    this.dbClient = dbClient;
    this.settings = settings;
  }

  public void start() {
    Profiler profiler = Profiler.create(Loggers.get(getClass())).startInfo("Register permission templates");
    boolean shouldRegister = shouldRegister();

    if (hasExistingPermissionsConfig()) {
      // needs to be done at each startup in the case a plugin has just been installed. The default property must be the project one
      String defaultProjectPermissionTemplateUuid = settings.getString(defaultRootQualifierTemplateProperty(Qualifiers.PROJECT));
      setDefaultProperty(defaultProjectPermissionTemplateUuid);
    } else if (shouldRegister) {
      insertDefaultTemplate();
      setDefaultProperty(DefaultPermissionTemplates.DEFAULT_TEMPLATE.getUuid());
    }

    if (shouldRegister) {
      registerInitialization();
    }

    profiler.stopDebug();
  }

  private boolean hasExistingPermissionsConfig() {
    return settings.getString(defaultRootQualifierTemplateProperty(Qualifiers.PROJECT)) != null;
  }

  private boolean shouldRegister() {
    return dbClient.loadedTemplateDao().countByTypeAndKey(LoadedTemplateDto.PERMISSION_TEMPLATE_TYPE, DefaultPermissionTemplates.DEFAULT_TEMPLATE.getUuid()) == 0;
  }

  private void insertDefaultTemplate() {
    DbSession dbSession = dbClient.openSession(false);
    try {
      PermissionTemplateDto defaultPermissionTemplate =
        dbClient.permissionTemplateDao().insert(dbSession, DefaultPermissionTemplates.DEFAULT_TEMPLATE);
      addGroupPermission(defaultPermissionTemplate, UserRole.ADMIN, DefaultGroups.ADMINISTRATORS);
      addGroupPermission(defaultPermissionTemplate, UserRole.ISSUE_ADMIN, DefaultGroups.ADMINISTRATORS);
      addGroupPermission(defaultPermissionTemplate, UserRole.USER, DefaultGroups.ANYONE);
      addGroupPermission(defaultPermissionTemplate, UserRole.CODEVIEWER, DefaultGroups.ANYONE);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private void addGroupPermission(PermissionTemplateDto template, String permission, String groupName) {
    Long groupId = null;
    if (DefaultGroups.isAnyone(groupName)) {
      groupId = null;
    } else {
      DbSession dbSession = dbClient.openSession(false);
      try {
        GroupDto groupDto = dbClient.groupDao().selectByName(dbSession, groupName);
        if (groupDto != null) {
          groupId = groupDto.getId();
        } else {
          LOG.error("Cannot setup default permission for group: " + groupName);
        }
      } finally {
        dbClient.closeSession(dbSession);
      }
    }
    dbClient.permissionTemplateDao().insertGroupPermission(template.getId(), groupId, permission);
  }

  private void registerInitialization() {
    LoadedTemplateDto loadedTemplate = new LoadedTemplateDto(DefaultPermissionTemplates.DEFAULT_TEMPLATE.getUuid(),
      LoadedTemplateDto.PERMISSION_TEMPLATE_TYPE);
    dbClient.loadedTemplateDao().insert(loadedTemplate);
  }

  private void setDefaultProperty(String defaultTemplate) {
    settings.saveProperty(DEFAULT_TEMPLATE_PROPERTY, defaultTemplate);
  }
}
