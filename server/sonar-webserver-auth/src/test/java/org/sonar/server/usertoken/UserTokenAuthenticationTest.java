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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.authentication.UserLastConnectionDatesUpdater;
import org.sonar.server.usertoken.UserTokenAuthentication.UserTokenAuthenticationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserTokenAuthenticationTest {

  private static final String EXAMPLE_SCANNER_ENDPOINT = "/api/settings/values.protobuf";
  private static final String EXAMPLE_USER_ENDPOINT = "/api/editions/set_license";

  private static final String EXAMPLE_PROJECT_KEY = "my-project-key";

  private static final String EXAMPLE_OLD_USER_TOKEN = "StringWith40CharactersThatIsOldUserToken";
  private static final String EXAMPLE_NEW_USER_TOKEN = "squ_StringWith44CharactersThatIsNewUserToken";
  private static final String EXAMPLE_GLOBAL_ANALYSIS_TOKEN = "sqa_StringWith44CharactersWhichIsGlobalToken";
  private static final String EXAMPLE_PROJECT_ANALYSIS_TOKEN = "sqp_StringWith44CharactersThatIsProjectToken";

  private static final String OLD_USER_TOKEN_HASH = "old-user-token-hash";
  private static final String NEW_USER_TOKEN_HASH = "new-user-token-hash";
  private static final String PROJECT_ANALYSIS_TOKEN_HASH = "project-analysis-token-hash";
  private static final String GLOBAL_ANALYSIS_TOKEN_HASH = "global-analysis-token-hash";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private TokenGenerator tokenGenerator = mock(TokenGenerator.class);
  private UserLastConnectionDatesUpdater userLastConnectionDatesUpdater = mock(UserLastConnectionDatesUpdater.class);

  private UserTokenAuthentication underTest = new UserTokenAuthentication(tokenGenerator, db.getDbClient(), userLastConnectionDatesUpdater);

  @Before
  public void before() {
    when(tokenGenerator.hash(EXAMPLE_OLD_USER_TOKEN)).thenReturn(OLD_USER_TOKEN_HASH);
    when(tokenGenerator.hash(EXAMPLE_NEW_USER_TOKEN)).thenReturn(NEW_USER_TOKEN_HASH);
    when(tokenGenerator.hash(EXAMPLE_PROJECT_ANALYSIS_TOKEN)).thenReturn(PROJECT_ANALYSIS_TOKEN_HASH);
    when(tokenGenerator.hash(EXAMPLE_GLOBAL_ANALYSIS_TOKEN)).thenReturn(GLOBAL_ANALYSIS_TOKEN_HASH);
  }

  @Test
  public void return_login_when_token_hash_found_in_db() {
    String token = "known-token";
    String tokenHash = "123456789";
    when(tokenGenerator.hash(token)).thenReturn(tokenHash);
    UserDto user1 = db.users().insertUser();
    db.users().insertToken(user1, t -> t.setTokenHash(tokenHash));
    UserDto user2 = db.users().insertUser();
    db.users().insertToken(user2, t -> t.setTokenHash("another-token-hash"));

    UserTokenAuthenticationResult result = underTest.authenticate(token, EXAMPLE_USER_ENDPOINT, null);

    assertThat(result.getAuthenticatedUserUuid())
      .isNotNull()
      .contains(user1.getUuid());
    verify(userLastConnectionDatesUpdater).updateLastConnectionDateIfNeeded(any(UserTokenDto.class));
  }

  @Test
  public void return_absent_if_token_hash_is_not_found() {
    var result = underTest.authenticate(EXAMPLE_OLD_USER_TOKEN, EXAMPLE_USER_ENDPOINT, null);

    assertThat(result.getAuthenticatedUserUuid()).isNull();
    verify(userLastConnectionDatesUpdater, never()).updateLastConnectionDateIfNeeded(any(UserTokenDto.class));
  }

  @Test
  public void authenticate_givenProjectTokenAndUserEndpoint_fillErrorMessage() {
    UserDto user = db.users().insertUser();
    db.users().insertToken(user, t -> t.setTokenHash(PROJECT_ANALYSIS_TOKEN_HASH).setType(TokenType.PROJECT_ANALYSIS_TOKEN.name()));

    var authenticate = underTest.authenticate(EXAMPLE_PROJECT_ANALYSIS_TOKEN, EXAMPLE_USER_ENDPOINT, EXAMPLE_PROJECT_KEY);

    assertThat(authenticate.getErrorMessage()).isNotNull().contains("Invalid token");
  }

  @Test
  public void authenticate_givenProjectTokenAndUserEndpoint_InvalidTokenErrorMessage() {
    UserDto user = db.users().insertUser();
    db.users().insertToken(user, t -> t.setTokenHash(PROJECT_ANALYSIS_TOKEN_HASH).setType(TokenType.PROJECT_ANALYSIS_TOKEN.name()));

    var result = underTest.authenticate(EXAMPLE_PROJECT_ANALYSIS_TOKEN, EXAMPLE_USER_ENDPOINT, EXAMPLE_PROJECT_KEY);

    assertThat(result.getErrorMessage()).isNotNull().contains("Invalid token");
  }

  @Test
  public void authenticate_givenGlobalTokenAndScannerEndpoint_resultContainsUuid() {
    UserDto user = db.users().insertUser();
    db.users().insertToken(user, t -> t.setTokenHash(GLOBAL_ANALYSIS_TOKEN_HASH).setType(TokenType.GLOBAL_ANALYSIS_TOKEN.name()));

    var result = underTest.authenticate(EXAMPLE_GLOBAL_ANALYSIS_TOKEN, EXAMPLE_SCANNER_ENDPOINT, EXAMPLE_PROJECT_KEY);

    assertThat(result.getAuthenticatedUserUuid()).isNotNull();
    assertThat(result.getErrorMessage()).isNull();
  }

  @Test
  public void authenticate_givenNewUserTokenAndScannerEndpoint_resultContainsUuid() {
    UserDto user = db.users().insertUser();
    db.users().insertToken(user, t -> t.setTokenHash(NEW_USER_TOKEN_HASH).setType(TokenType.USER_TOKEN.name()));

    var result = underTest.authenticate(EXAMPLE_NEW_USER_TOKEN, EXAMPLE_SCANNER_ENDPOINT, null);

    assertThat(result.getAuthenticatedUserUuid()).isNotNull();
    assertThat(result.getErrorMessage()).isNull();
  }

  @Test
  public void authenticate_givenProjectTokenAndScannerEndpointAndValidProjectKey_resultContainsUuid() {
    UserDto user = db.users().insertUser();
    db.users().insertToken(user, t -> t.setTokenHash(PROJECT_ANALYSIS_TOKEN_HASH)
      .setProjectKey("project-key")
      .setType(TokenType.PROJECT_ANALYSIS_TOKEN.name()));

    var result = underTest.authenticate(EXAMPLE_PROJECT_ANALYSIS_TOKEN, EXAMPLE_SCANNER_ENDPOINT, "project-key");

    assertThat(result.getAuthenticatedUserUuid()).isNotNull();
    assertThat(result.getErrorMessage()).isNull();
  }

  @Test
  public void authenticate_givenProjectTokenAndScannerEndpointAndWrongProjectKey_resultContainsErrorMessage() {
    UserDto user = db.users().insertUser();
    db.users().insertToken(user, t -> t.setTokenHash(PROJECT_ANALYSIS_TOKEN_HASH)
      .setProjectKey("project-key")
      .setType(TokenType.PROJECT_ANALYSIS_TOKEN.name()));

    var result = underTest.authenticate(EXAMPLE_PROJECT_ANALYSIS_TOKEN, EXAMPLE_SCANNER_ENDPOINT, "project-key-2");

    assertThat(result.getAuthenticatedUserUuid()).isNull();
    assertThat(result.getErrorMessage()).isNotNull();
  }
}
