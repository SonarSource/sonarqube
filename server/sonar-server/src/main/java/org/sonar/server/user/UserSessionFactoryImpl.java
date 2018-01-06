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
package org.sonar.server.user;

import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.user.UserDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.OrganizationFlags;

import static java.util.Objects.requireNonNull;

@ServerSide
public class UserSessionFactoryImpl implements UserSessionFactory {

  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final OrganizationFlags organizationFlags;

  public UserSessionFactoryImpl(DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider,
    OrganizationFlags organizationFlags) {
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.organizationFlags = organizationFlags;
  }

  @Override
  public ServerUserSession create(UserDto user) {
    requireNonNull(user, "UserDto must not be null");
    return new ServerUserSession(dbClient, organizationFlags, defaultOrganizationProvider, user);
  }

  @Override
  public ServerUserSession createAnonymous() {
    return new ServerUserSession(dbClient, organizationFlags, defaultOrganizationProvider, null);
  }
}
