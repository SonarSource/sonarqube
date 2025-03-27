/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.user.TokenType;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;

import static java.lang.String.format;
import static org.sonar.db.permission.ProjectPermission.SCAN;
import static org.sonar.db.permission.ProjectPermission.USER;

public class TokenUserSession extends ServerUserSession {

  private static final String TOKEN_ASSERTION_ERROR_MESSAGE = "Unsupported token type %s";
  private static final Set<GlobalPermission> GLOBAL_ANALYSIS_TOKEN_SUPPORTED_PERMISSIONS = EnumSet.of(GlobalPermission.SCAN, GlobalPermission.PROVISION_PROJECTS);
  private final UserTokenDto userToken;

  public TokenUserSession(DbClient dbClient, UserDto user, UserTokenDto userToken) {
    super(dbClient, user, false);
    this.userToken = userToken;
  }

  @Override
  protected boolean hasEntityUuidPermission(ProjectPermission permission, String entityUuid) {
    TokenType tokenType = TokenType.valueOf(userToken.getType());
    return switch (tokenType) {
      case USER_TOKEN -> super.hasEntityUuidPermission(permission, entityUuid);
      case PROJECT_ANALYSIS_TOKEN -> SCAN.equals(permission) &&
        entityUuid.equals(userToken.getProjectUuid()) &&
        (super.hasEntityUuidPermission(SCAN, entityUuid) || super.hasPermissionImpl(GlobalPermission.SCAN));
      case GLOBAL_ANALYSIS_TOKEN ->
        // The case with a global analysis token has to return false always, since it is based on the assumption that the user
        // has global analysis privileges
        false;
      default -> throw new IllegalArgumentException(format(TOKEN_ASSERTION_ERROR_MESSAGE, tokenType.name()));
    };

  }

  @Override
  protected boolean hasPermissionImpl(GlobalPermission permission) {
    TokenType tokenType = TokenType.valueOf(userToken.getType());
    return switch (tokenType) {
      case USER_TOKEN -> super.hasPermissionImpl(permission);
      case PROJECT_ANALYSIS_TOKEN ->
        // The case with a project analysis token has to return false always, delegating the result to the super class would allow
        // the project analysis token to work for multiple projects in case the user has Global Permissions.
        false;
      case GLOBAL_ANALYSIS_TOKEN ->
        GLOBAL_ANALYSIS_TOKEN_SUPPORTED_PERMISSIONS.contains(permission) && super.hasPermissionImpl(permission);
      default -> throw new IllegalArgumentException(format(TOKEN_ASSERTION_ERROR_MESSAGE, tokenType.name()));
    };
  }

  @Override
  protected <T extends EntityDto> List<T> doKeepAuthorizedEntities(ProjectPermission permission, Collection<T> entities) {
    TokenType tokenType = TokenType.valueOf(userToken.getType());
    return switch (tokenType) {
      case USER_TOKEN, GLOBAL_ANALYSIS_TOKEN -> super.doKeepAuthorizedEntities(permission, entities);
      case PROJECT_ANALYSIS_TOKEN ->
        (SCAN.equals(permission) || USER.equals(permission)) ? entities.stream()
          .filter(entity -> entity.getUuid().equals(userToken.getProjectUuid()))
          .toList() : Collections.emptyList();
      default -> throw new IllegalArgumentException(format(TOKEN_ASSERTION_ERROR_MESSAGE, tokenType.name()));
    };
  }

  /**
   * Required to override doKeepAuthorizedComponents to handle the case of a project analysis token
   */
  @Override
  protected Set<String> keepAuthorizedProjectsUuids(DbSession dbSession, ProjectPermission permission, Collection<String> entityUuids) {
    TokenType tokenType = TokenType.valueOf(userToken.getType());
    return switch (tokenType) {
      case USER_TOKEN, GLOBAL_ANALYSIS_TOKEN -> super.keepAuthorizedProjectsUuids(dbSession, permission, entityUuids);
      case PROJECT_ANALYSIS_TOKEN ->
        (SCAN.equals(permission) || USER.equals(permission)) ? Collections.singleton(userToken.getProjectUuid()) : Collections.emptySet();
      default -> throw new IllegalArgumentException(format(TOKEN_ASSERTION_ERROR_MESSAGE, tokenType.name()));
    };
  }

  public UserTokenDto getUserToken() {
    return userToken;
  }

  /**
   * @return the type of the token, based on the {@link TokenType} enum
   */
  public TokenType getTokenType() {
    return TokenType.valueOf(userToken.getType());
  }
}
