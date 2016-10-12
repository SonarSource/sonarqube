/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.startup;

import java.util.Date;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.platform.PersistentSettings;

import static org.sonar.db.loadedtemplate.LoadedTemplateDto.PERMISSION_TEMPLATE_TYPE;
import static org.sonar.server.permission.DefaultPermissionTemplates.DEFAULT_TEMPLATE_KEY;
import static org.sonar.server.permission.DefaultPermissionTemplates.DEFAULT_TEMPLATE_PROPERTY;
import static org.sonar.server.permission.DefaultPermissionTemplates.defaultRootQualifierTemplateProperty;

public class RegisterPermissionTemplates {

  private static final Logger LOG = Loggers.get(RegisterPermissionTemplates.class);

  private final DbClient dbClient;
  private final PersistentSettings settings;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public RegisterPermissionTemplates(DbClient dbClient, PersistentSettings settings, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.settings = settings;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  public void start() {
    Profiler profiler = Profiler.create(Loggers.get(getClass())).startInfo("Register permission templates");
    boolean shouldRegister = shouldRegister();

    if (hasExistingPermissionsConfig()) {
      // needs to be done at each startup in the case a plugin has just been installed. The default property must be the project one
      String defaultProjectPermissionTemplateUuid = settings.getString(defaultRootQualifierTemplateProperty(Qualifiers.PROJECT));
      setDefaultProperty(defaultProjectPermissionTemplateUuid);
    } else if (shouldRegister) {
      PermissionTemplateDto template = insertDefaultTemplate();
      setDefaultProperty(template.getUuid());
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
    return dbClient.loadedTemplateDao().countByTypeAndKey(PERMISSION_TEMPLATE_TYPE, DEFAULT_TEMPLATE_KEY) == 0;
  }

  private PermissionTemplateDto insertDefaultTemplate() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String orgUuid = defaultOrganizationProvider.get().getUuid();

      PermissionTemplateDto template = new PermissionTemplateDto()
        .setOrganizationUuid(orgUuid)
        .setName("Default template")
        .setUuid(DEFAULT_TEMPLATE_KEY)
        .setDescription("This permission template will be used as default when no other permission configuration is available")
        .setCreatedAt(new Date())
        .setUpdatedAt(new Date());

      dbClient.permissionTemplateDao().insert(dbSession, template);
      insertDefaultGroupPermissions(dbSession, template);
      dbSession.commit();
      return template;
    }
  }

  private void insertDefaultGroupPermissions(DbSession dbSession, PermissionTemplateDto template) {
    Optional<GroupDto> admins = dbClient.groupDao().selectByName(dbSession, template.getOrganizationUuid(), DefaultGroups.ADMINISTRATORS);
    if (admins.isPresent()) {
      insertGroupPermission(dbSession, template, UserRole.ADMIN, admins.get());
      insertGroupPermission(dbSession, template, UserRole.ISSUE_ADMIN, admins.get());
    } else {
      LOG.error("Cannot setup default permission for group: " + DefaultGroups.ADMINISTRATORS);
    }
    insertGroupPermission(dbSession, template, UserRole.USER, null);
    insertGroupPermission(dbSession, template, UserRole.CODEVIEWER, null);
  }

  private void insertGroupPermission(DbSession dbSession, PermissionTemplateDto template, String permission, @Nullable GroupDto group) {
    if (group == null) {
      dbClient.permissionTemplateDao().insertGroupPermission(dbSession, template.getId(), null, permission);
    } else {
      dbClient.permissionTemplateDao().insertGroupPermission(dbSession, template.getId(), group.getId(), permission);
    }
  }

  private void registerInitialization() {
    LoadedTemplateDto loadedTemplate = new LoadedTemplateDto(DEFAULT_TEMPLATE_KEY, PERMISSION_TEMPLATE_TYPE);
    dbClient.loadedTemplateDao().insert(loadedTemplate);
  }

  private void setDefaultProperty(String defaultTemplate) {
    settings.saveProperty(DEFAULT_TEMPLATE_PROPERTY, defaultTemplate);
  }
}
