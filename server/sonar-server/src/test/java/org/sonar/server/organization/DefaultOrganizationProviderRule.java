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
package org.sonar.server.organization;

import java.util.Optional;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.property.InternalProperties;

import static com.google.common.base.Preconditions.checkState;

public class DefaultOrganizationProviderRule implements DefaultOrganizationProvider {

  private final DbTester dbTester;

  public static DefaultOrganizationProviderRule create(DbTester dbTester) {
    return new DefaultOrganizationProviderRule(dbTester);
  }

  @Override
  public DefaultOrganization get() {
    DbSession dbSession = dbTester.getSession();
    Optional<String> uuid = dbTester.getDbClient().internalPropertiesDao().selectByKey(dbSession, InternalProperties.DEFAULT_ORGANIZATION);
    checkState(uuid.isPresent() && !uuid.get().isEmpty(), "No Default organization uuid configured");
    Optional<OrganizationDto> dto = dbTester.getDbClient().organizationDao().selectByUuid(dbSession, uuid.get());
    checkState(dto.isPresent(), "Default organization with uuid '%s' does not exist", uuid.get());
    return toDefaultOrganization(dto.get());
  }

  public OrganizationDto getDto() {
    String uuid = get().getUuid();
    return dbTester.getDbClient().organizationDao().selectByUuid(dbTester.getSession(), uuid)
        .orElseThrow(() -> new IllegalStateException("Missing default organization in database [uuid=" + uuid + "]"));
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

  private DefaultOrganizationProviderRule(DbTester dbTester) {
    this.dbTester = dbTester;
  }
}
