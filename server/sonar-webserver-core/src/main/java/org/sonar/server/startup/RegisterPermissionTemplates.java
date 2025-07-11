/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import org.sonar.api.Startable;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.template.DefaultTemplates;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static org.sonar.server.property.InternalProperties.DEFAULT_PROJECT_TEMPLATE;

public class RegisterPermissionTemplates implements Startable {

  private static final Logger LOG = Loggers.get(RegisterPermissionTemplates.class);

  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final System2 system2;
  private final DefaultGroupFinder defaultGroupFinder;

  public RegisterPermissionTemplates(DbClient dbClient, UuidFactory uuidFactory, System2 system2, DefaultGroupFinder defaultGroupFinder) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
    this.defaultGroupFinder = defaultGroupFinder;
  }

  @Override
  public void start() {
    Profiler profiler = Profiler.create(Loggers.get(getClass())).startInfo("Register permission templates");

    try (DbSession dbSession = dbClient.openSession(false)) {
      String defaultOrganizationUuid = dbClient.organizationDao().getDefaultOrganization(dbSession).getUuid();
      Optional<DefaultTemplates> defaultTemplates = dbClient.organizationDao().getDefaultTemplates(dbSession, defaultOrganizationUuid);
      if (!defaultTemplates.isPresent()) {
        PermissionTemplateDto defaultTemplate = getOrInsertDefaultTemplate(dbSession, defaultOrganizationUuid);
        dbClient.organizationDao().setDefaultTemplates(dbSession, defaultOrganizationUuid, new DefaultTemplates().setProjectUuid(defaultTemplate.getUuid()));
        dbSession.commit();
      }
    }

    profiler.stopDebug();
  }

  @Override
  public void stop() {
    // nothing to do
  }

  private PermissionTemplateDto getOrInsertDefaultTemplate(DbSession dbSession, String defaultOrganizationUuid) {
    PermissionTemplateDto template = new PermissionTemplateDto()
      .setName("Default template")
      .setUuid(uuidFactory.create())
      .setOrganizationUuid(defaultOrganizationUuid)
      .setDescription("This permission template will be used as default when no other permission configuration is available")
      .setCreatedAt(new Date(system2.now()))
      .setUpdatedAt(new Date(system2.now()));

    dbClient.permissionTemplateDao().insert(dbSession, template);
    insertDefaultGroupPermissions(dbSession, template);
    dbSession.commit();
    return template;
  }

  private void insertDefaultGroupPermissions(DbSession dbSession, PermissionTemplateDto template) {
    insertPermissionForAdministrators(dbSession, template);
    insertPermissionsForDefaultGroup(dbSession, template);
  }

  private void insertPermissionForAdministrators(DbSession dbSession, PermissionTemplateDto template) {
    Optional<GroupDto> admins = dbClient.groupDao().selectByName(dbSession, template.getOrganizationUuid(), DefaultGroups.ADMINISTRATORS);
    if (admins.isPresent()) {
      insertGroupPermission(dbSession, template, UserRole.ADMIN, admins.get());
    } else {
      LOG.error("Cannot setup default permission for group: " + DefaultGroups.ADMINISTRATORS);
    }
  }

  private void insertPermissionsForDefaultGroup(DbSession dbSession, PermissionTemplateDto template) {
    GroupDto defaultGroup = defaultGroupFinder.findDefaultGroup(dbSession, template.getOrganizationUuid());
    insertGroupPermission(dbSession, template, UserRole.USER, defaultGroup);
    insertGroupPermission(dbSession, template, UserRole.CODEVIEWER, defaultGroup);
    insertGroupPermission(dbSession, template, UserRole.ISSUE_ADMIN, defaultGroup);
    insertGroupPermission(dbSession, template, UserRole.SECURITYHOTSPOT_ADMIN, defaultGroup);

    dbClient.groupDao().selectByName(dbSession, template.getOrganizationUuid(), "Members").ifPresent(membersGroup -> {
      insertGroupPermission(dbSession, template, UserRole.USER, membersGroup);
      insertGroupPermission(dbSession, template, UserRole.CODEVIEWER, membersGroup);
      insertGroupPermission(dbSession, template, UserRole.ISSUE_ADMIN, membersGroup);
      insertGroupPermission(dbSession, template, UserRole.SECURITYHOTSPOT_ADMIN, membersGroup);
    });
  }

  private void insertGroupPermission(DbSession dbSession, PermissionTemplateDto template, String permission, GroupDto group) {
    dbClient.permissionTemplateDao().insertGroupPermission(dbSession, template.getUuid(), group.getUuid(), permission, template.getName(), group.getName(), template.getOrganizationUuid());
  }

}
