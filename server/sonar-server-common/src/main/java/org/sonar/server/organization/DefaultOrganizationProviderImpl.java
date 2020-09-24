/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.organization;

import java.util.Optional;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.property.InternalProperties;

import static com.google.common.base.Preconditions.checkState;

public class DefaultOrganizationProviderImpl implements DefaultOrganizationProvider {
  private final DbClient dbClient;
  private DefaultOrganization cache;

  public DefaultOrganizationProviderImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public DefaultOrganization get() {
    if (cache == null) {
      cache = getDefaultOrganization(dbClient);
    }

    return cache;
  }

  public static DefaultOrganization getDefaultOrganization(DbClient dbClient) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<String> uuid = dbClient.internalPropertiesDao().selectByKey(dbSession, InternalProperties.DEFAULT_ORGANIZATION);
      checkState(uuid.isPresent() && !uuid.get().isEmpty(), "No Default organization uuid configured");
      Optional<OrganizationDto> dto = dbClient.organizationDao().selectByUuid(dbSession, uuid.get());
      checkState(dto.isPresent(), "Default organization with uuid '%s' does not exist", uuid.get());
      return toDefaultOrganization(dto.get());
    }
  }

  private static DefaultOrganization toDefaultOrganization(OrganizationDto organizationDto) {
    return DefaultOrganization.newBuilder()
      .setUuid(organizationDto.getUuid())
      .setKey(organizationDto.getKey())
      .setName(organizationDto.getName())
      .setCreatedAt(organizationDto.getCreatedAt())
      .setUpdatedAt(organizationDto.getUpdatedAt())
      .build();
  }
}
