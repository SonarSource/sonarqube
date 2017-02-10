/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.rule.ws;

import org.sonar.api.server.ServerSide;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;

@ServerSide
public class RuleWsSupport {
  private final UserSession userSession;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public RuleWsSupport(UserSession userSession, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.userSession = userSession;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  public void checkQProfileAdminPermission() {
    userSession
      .checkLoggedIn()
      .checkOrganizationPermission(defaultOrganizationProvider.get().getUuid(), GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }
}
