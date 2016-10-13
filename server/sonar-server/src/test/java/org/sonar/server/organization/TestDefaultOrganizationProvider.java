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

import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;

public class TestDefaultOrganizationProvider implements DefaultOrganizationProvider {

  private final DbTester dbTester;

  private TestDefaultOrganizationProvider(DbTester dbTester) {
    this.dbTester = dbTester;
  }

  public static TestDefaultOrganizationProvider from(DbTester dbTester) {
    return new TestDefaultOrganizationProvider(dbTester);
  }

  @Override
  public DefaultOrganization get() {
    return toDefaultOrganization(dbTester.getDefaultOrganization());
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
