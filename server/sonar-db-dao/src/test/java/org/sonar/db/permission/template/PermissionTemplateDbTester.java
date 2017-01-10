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

package org.sonar.db.permission.template;

import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;

import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateCharacteristicDto;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;

public class PermissionTemplateDbTester {
  private final DbTester db;
  private final DbClient dbClient;
  private final DbSession dbSession;

  public PermissionTemplateDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();
  }

  public PermissionTemplateDto insertTemplate() {
    return insertTemplate(newPermissionTemplateDto());
  }

  public PermissionTemplateDto insertTemplate(OrganizationDto organizationDto) {
    return insertTemplate(newPermissionTemplateDto().setOrganizationUuid(organizationDto.getUuid()));
  }

  public PermissionTemplateDto insertTemplate(PermissionTemplateDto template) {
    PermissionTemplateDto templateInDb = dbClient.permissionTemplateDao().insert(dbSession, template);
    db.commit();

    return templateInDb;
  }

  public void addGroupToTemplate(long templateId, @Nullable Long groupId, String permission) {
    dbClient.permissionTemplateDao().insertGroupPermission(dbSession, templateId, groupId, permission);
    db.commit();
  }

  public void addUserToTemplate(long templateId, long userId, String permission) {
    dbClient.permissionTemplateDao().insertUserPermission(dbSession, templateId, userId, permission);
    db.commit();
  }

  public void addProjectCreatorToTemplate(long templateId, String permission) {
    dbClient.permissionTemplateCharacteristicDao().insert(dbSession, newPermissionTemplateCharacteristicDto()
      .setWithProjectCreator(true)
      .setTemplateId(templateId)
      .setPermission(permission));
    db.commit();
  }
}
