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
package org.sonar.server.user;

import java.util.EnumSet;
import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.user.TokenType;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;

public class TokenUserSession extends ServerUserSession {

  private static final String SCAN = "scan";
  private static final Set<GlobalPermission> GLOBAL_ANALYSIS_TOKEN_SUPPORTED_PERMISSIONS = EnumSet.of(GlobalPermission.SCAN, GlobalPermission.PROVISION_PROJECTS);
  private final UserTokenDto userToken;

  public TokenUserSession(DbClient dbClient, UserDto user, UserTokenDto userToken) {
    super(dbClient, user);
    this.userToken = userToken;
  }

  @Override
  protected boolean hasProjectUuidPermission(String permission, String projectUuid) {
    TokenType tokenType = TokenType.valueOf(userToken.getType());
    switch (tokenType) {
      case USER_TOKEN:
        return super.hasProjectUuidPermission(permission, projectUuid);
      case PROJECT_ANALYSIS_TOKEN:
        return SCAN.equals(permission) &&
          projectUuid.equals(userToken.getProjectUuid()) &&
          (super.hasProjectUuidPermission(SCAN, projectUuid) || super.hasPermissionImpl(GlobalPermission.SCAN));
      case GLOBAL_ANALYSIS_TOKEN:
        //The case with a global analysis token has to return false always, since it is based on the assumption that the user
        // has global analysis privileges
        return false;
      default:
        throw new IllegalArgumentException("Unsupported token type " + tokenType.name());
    }

  }

  @Override
  protected boolean hasPermissionImpl(GlobalPermission permission) {
    TokenType tokenType = TokenType.valueOf(userToken.getType());
    switch (tokenType) {
      case USER_TOKEN:
        return super.hasPermissionImpl(permission);
      case PROJECT_ANALYSIS_TOKEN:
        //The case with a project analysis token has to return false always, delegating the result to the super class would allow
        //the project analysis token to work for multiple projects in case the user has Global Permissions.
        return false;
      case GLOBAL_ANALYSIS_TOKEN:
        return GLOBAL_ANALYSIS_TOKEN_SUPPORTED_PERMISSIONS.contains(permission) && super.hasPermissionImpl(permission);
      default:
        throw new IllegalArgumentException("Unsupported token type " + tokenType.name());
    }
  }

  public UserTokenDto getUserToken() {
    return userToken;
  }
}
