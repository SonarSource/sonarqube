/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.usertoken;

import java.util.EnumMap;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.authentication.UserLastConnectionDatesUpdater;

public class UserTokenAuthentication {

  public static final String PROJECT_KEY_SCANNER_HEADER = "PROJECT_KEY";

  private static final Set<String> SCANNER_ENDPOINTS = Set.of(
    "/api/settings/values",
    "/api/plugins/installed",
    "/api/project_branches/list",
    "/api/project_pull_requests/list",
    "/api/qualityprofiles/search",
    "/api/rules/search",
    "/batch/project",
    "/api/metrics/search",
    "/api/new_code_periods/show",
    "/api/ce/submit");

  private static final EnumMap<TokenType, Set<String>> ALLOWLIST_ENDPOINTS_FOR_TOKEN_TYPES = new EnumMap<>(TokenType.class);

  static {
    ALLOWLIST_ENDPOINTS_FOR_TOKEN_TYPES.put(TokenType.GLOBAL_ANALYSIS_TOKEN, SCANNER_ENDPOINTS);
    ALLOWLIST_ENDPOINTS_FOR_TOKEN_TYPES.put(TokenType.PROJECT_ANALYSIS_TOKEN, SCANNER_ENDPOINTS);
  }

  private final TokenGenerator tokenGenerator;
  private final DbClient dbClient;
  private final UserLastConnectionDatesUpdater userLastConnectionDatesUpdater;

  public UserTokenAuthentication(TokenGenerator tokenGenerator, DbClient dbClient, UserLastConnectionDatesUpdater userLastConnectionDatesUpdater) {
    this.tokenGenerator = tokenGenerator;
    this.dbClient = dbClient;
    this.userLastConnectionDatesUpdater = userLastConnectionDatesUpdater;
  }

  /**
   * Returns the user token details including if the token hash is found and the user has provided valid token type.
   *
   * The returned uuid included in the UserTokenAuthenticationResult  is not validated. If database is corrupted
   * (table USER_TOKENS badly purged for instance), then the uuid may not relate to a valid user.
   *
   * In case of any issues only the error message is included in UserTokenAuthenticationResult
   */
  public UserTokenAuthenticationResult authenticate(String token, String requestedEndpoint, @Nullable String analyzedProjectKey) {
    String tokenHash = tokenGenerator.hash(token);
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserTokenDto userToken = dbClient.userTokenDao().selectByTokenHash(dbSession, tokenHash);
      if (userToken == null) {
        return new UserTokenAuthenticationResult("Token doesn't exist");
      }
      if (!isValidTokenType(userToken, analyzedProjectKey, requestedEndpoint)) {
        return new UserTokenAuthenticationResult("Invalid token");
      }
      userLastConnectionDatesUpdater.updateLastConnectionDateIfNeeded(userToken);
      return new UserTokenAuthenticationResult(userToken.getUserUuid(), userToken.getName());
    }
  }

  private static boolean isValidTokenType(UserTokenDto userToken, @Nullable String analyzedProjectKey, String requestedEndpoint) {
    TokenType tokenType = TokenType.valueOf(userToken.getType());

    return validateProjectKeyForScannerToken(tokenType, userToken, analyzedProjectKey)
      && shouldBeAbleToAccessEndpoint(tokenType, requestedEndpoint);
  }

  private static boolean shouldBeAbleToAccessEndpoint(TokenType tokenType, String requestedEndpoint) {
    Set<String> allowedEndpoints = ALLOWLIST_ENDPOINTS_FOR_TOKEN_TYPES.get(tokenType);
    if (allowedEndpoints == null) {
      return true; // no allowlist configured for the token type - all endpoints are allowed
    }
    return allowedEndpoints.stream().anyMatch(requestedEndpoint::startsWith);
  }

  private static boolean validateProjectKeyForScannerToken(TokenType tokenType, UserTokenDto userToken, @Nullable String analyzedProjectKey) {
    if (tokenType != TokenType.PROJECT_ANALYSIS_TOKEN) {
      return true;
    }
    return analyzedProjectKey != null && analyzedProjectKey.equals(userToken.getProjectKey());
  }

  public static class UserTokenAuthenticationResult {

    String authenticatedUserUuid;
    String errorMessage;
    String tokenName;

    public UserTokenAuthenticationResult(String authenticatedUserUuid, String tokenName) {
      this.authenticatedUserUuid = authenticatedUserUuid;
      this.tokenName = tokenName;
    }

    public UserTokenAuthenticationResult(String errorMessage) {
      this.errorMessage = errorMessage;
    }

    public String getAuthenticatedUserUuid() {
      return authenticatedUserUuid;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public String getTokenName() {
      return tokenName;
    }

  }
}
