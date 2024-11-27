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
package org.sonar.server.common.organization;

import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;

public class OrganizationService {

  private final DbClient dbClient;

  public OrganizationService(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public OrganizationDto getOrganizationByKey(String key) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.organizationDao().selectByKey(dbSession, key)
          .orElseThrow(() -> new IllegalArgumentException("No organization found by key: " + key));
    }
  }

  public OrganizationDto getOrganizationByUuid(String orgUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.organizationDao().selectByUuid(dbSession, orgUuid)
              .orElseThrow(() -> new IllegalArgumentException("No organization found by uuid: " + orgUuid));
    }
  }
}
