/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Date;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;

public class TestDefaultOrganizationProvider implements DefaultOrganizationProvider {

  private final DefaultOrganizationProvider delegate;

  private TestDefaultOrganizationProvider(DefaultOrganizationProvider delegate) {
    this.delegate = delegate;
  }

  public static TestDefaultOrganizationProvider from(DbTester dbTester) {
    return new TestDefaultOrganizationProvider(new DbTesterDefaultOrganizationProvider(dbTester));
  }

  public static TestDefaultOrganizationProvider fromUuid(String uuid) {
    long createdAt = new Date().getTime();
    return new TestDefaultOrganizationProvider(
      new ImmutableDefaultOrganizationProvider(
        DefaultOrganization.newBuilder()
          .setUuid(uuid)
          .setKey("key_" + uuid)
          .setName("name_" + uuid)
          .setCreatedAt(createdAt)
          .setUpdatedAt(createdAt)
          .build()));
  }

  @Override
  public DefaultOrganization get() {
    return delegate.get();
  }

  private static final class ImmutableDefaultOrganizationProvider implements DefaultOrganizationProvider {
    private final DefaultOrganization defaultOrganization;

    private ImmutableDefaultOrganizationProvider(DefaultOrganization defaultOrganization) {
      this.defaultOrganization = defaultOrganization;
    }

    @Override
    public DefaultOrganization get() {
      return defaultOrganization;
    }
  }

  private static final class DbTesterDefaultOrganizationProvider implements DefaultOrganizationProvider {
    private final DbTester dbTester;
    private DefaultOrganization defaultOrganization = null;

    private DbTesterDefaultOrganizationProvider(DbTester dbTester) {
      this.dbTester = dbTester;
    }

    @Override
    public DefaultOrganization get() {
      if (defaultOrganization == null) {
        defaultOrganization = toDefaultOrganization(dbTester.getDefaultOrganization());
      }
      return defaultOrganization;
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
}
