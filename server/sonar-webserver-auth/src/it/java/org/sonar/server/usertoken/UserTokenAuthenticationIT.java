/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.authentication.UserAuthResult;
import org.sonar.server.authentication.UserLastConnectionDatesUpdater;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.db.user.TokenType.GLOBAL_ANALYSIS_TOKEN;
import static org.sonar.db.user.TokenType.PROJECT_ANALYSIS_TOKEN;
import static org.sonar.db.user.TokenType.USER_TOKEN;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.SONARQUBE_TOKEN;

@RunWith(DataProviderRunner.class)
public class UserTokenAuthenticationIT {

  private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

  private static final String AUTHORIZATION_HEADER = "Authorization";

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

  private final TokenGenerator tokenGenerator = mock(TokenGenerator.class);
  private final UserLastConnectionDatesUpdater userLastConnectionDatesUpdater = mock(UserLastConnectionDatesUpdater.class);
  private final AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);
  private final HttpRequest request = mock(HttpRequest.class);
  private final UserTokenAuthentication underTest = new UserTokenAuthentication(tokenGenerator, db.getDbClient(), userLastConnectionDatesUpdater, authenticationEvent);

  @Before
  public void before() {
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic " + toBase64("token:"));
    when(request.getServletPath()).thenReturn("/api/anypath");
    when(tokenGenerator.hash(EXAMPLE_OLD_USER_TOKEN)).thenReturn(OLD_USER_TOKEN_HASH);
    when(tokenGenerator.hash(EXAMPLE_NEW_USER_TOKEN)).thenReturn(NEW_USER_TOKEN_HASH);
    when(tokenGenerator.hash(EXAMPLE_PROJECT_ANALYSIS_TOKEN)).thenReturn(PROJECT_ANALYSIS_TOKEN_HASH);
    when(tokenGenerator.hash(EXAMPLE_GLOBAL_ANALYSIS_TOKEN)).thenReturn(GLOBAL_ANALYSIS_TOKEN_HASH);
  }

  @Test
  public void return_login_when_token_hash_found_in_db_and_basic_auth_used() {
    String token = "known-token";
    String tokenHash = "123456789";
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic " + toBase64(token + ":"));
    when(tokenGenerator.hash(token)).thenReturn(tokenHash);
    UserDto user1 = db.users().insertUser();
    UserTokenDto userTokenDto = db.users().insertToken(user1, t -> t.setTokenHash(tokenHash));
    UserDto user2 = db.users().insertUser();
    db.users().insertToken(user2, t -> t.setTokenHash("another-token-hash"));

    Optional<UserAuthResult> result = underTest.authenticate(request);

    assertThat(result).isPresent();
    assertThat(result.get().getTokenDto().getUuid()).isEqualTo(userTokenDto.getUuid());
    assertThat(result.get().getUserDto().getUuid())
      .isNotNull()
      .contains(user1.getUuid());
    verify(userLastConnectionDatesUpdater).updateLastConnectionDateIfNeeded(any(UserTokenDto.class));
  }

  @DataProvider
  public static Object[][] bearerHeaderName() {
    return new Object[][] {
      {"bearer"},
      {"BEARER"},
      {"Bearer"},
      {"bEarer"},
    };
  }

  @Test
  @UseDataProvider("bearerHeaderName")
  public void authenticate_withDifferentBearerHeaderNameCase_succeeds(String headerName) {
    String token = setUpValidAuthToken();
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(headerName + " " + token);

    Optional<UserAuthResult> result = underTest.authenticate(request);

    assertThat(result).isPresent();
    verify(userLastConnectionDatesUpdater).updateLastConnectionDateIfNeeded(any(UserTokenDto.class));
  }

  @Test
  public void authenticate_withValidCamelcaseBearerTokenForMetricsAction_fails() {
    String token = setUpValidAuthToken();
    when(request.getServletPath()).thenReturn("/api/monitoring/metrics");
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Bearer " + token);

    Optional<UserAuthResult> result = underTest.authenticate(request);

    assertThat(result).isEmpty();
  }

  private String setUpValidAuthToken() {
    String token = "known-token";
    String tokenHash = "123456789";
    when(tokenGenerator.hash(token)).thenReturn(tokenHash);
    UserDto user1 = db.users().insertUser();
    db.users().insertToken(user1, t -> t.setTokenHash(tokenHash));
    UserDto user2 = db.users().insertUser();
    db.users().insertToken(user2, t -> t.setTokenHash("another-token-hash"));
    return token;
  }

  @Test
  public void return_login_when_token_hash_found_in_db_and_future_expiration_date() {
    String token = "known-token";
    String tokenHash = "123456789";

    long expirationTimestamp = ZonedDateTime.now(ZoneOffset.UTC).plusDays(10).toInstant().toEpochMilli();
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic " + toBase64(token + ":"));
    when(tokenGenerator.hash(token)).thenReturn(tokenHash);
    UserDto user1 = db.users().insertUser();
    UserTokenDto userTokenDto = db.users().insertToken(user1, t -> t.setTokenHash(tokenHash).setExpirationDate(expirationTimestamp));
    UserDto user2 = db.users().insertUser();
    db.users().insertToken(user2, t -> t.setTokenHash("another-token-hash"));

    Optional<UserAuthResult> result = underTest.authenticate(request);

    assertThat(result).isPresent();
    assertThat(result.get().getTokenDto().getUuid()).isEqualTo(userTokenDto.getUuid());
    assertThat(result.get().getTokenDto().getExpirationDate()).isEqualTo(expirationTimestamp);
    assertThat(result.get().getUserDto().getUuid())
      .isNotNull()
      .contains(user1.getUuid());
    verify(userLastConnectionDatesUpdater).updateLastConnectionDateIfNeeded(any(UserTokenDto.class));
  }

  @Test
  public void return_absent_if_username_password_used() {
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic " + toBase64("login:password"));

    Optional<UserAuthResult> result = underTest.authenticate(request);

    assertThat(result).isEmpty();
    verify(userLastConnectionDatesUpdater, never()).updateLastConnectionDateIfNeeded(any(UserTokenDto.class));
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void throw_authentication_exception_if_token_hash_is_not_found() {
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic " + toBase64(EXAMPLE_OLD_USER_TOKEN + ":"));

    assertThatThrownBy(() -> underTest.authenticate(request))
      .hasMessageContaining("Token doesn't exist")
      .isInstanceOf(AuthenticationException.class);
    verify(userLastConnectionDatesUpdater, never()).updateLastConnectionDateIfNeeded(any(UserTokenDto.class));
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void throw_authentication_exception_if_token_is_expired() {
    String token = "known-token";
    String tokenHash = "123456789";
    long expirationTimestamp = System.currentTimeMillis() - 1;
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic " + toBase64(token + ":"));
    when(tokenGenerator.hash(token)).thenReturn(tokenHash);
    UserDto user1 = db.users().insertUser();
    db.users().insertToken(user1, t -> t.setTokenHash(tokenHash).setExpirationDate(expirationTimestamp));

    assertThatThrownBy(() -> underTest.authenticate(request))
      .hasMessageContaining("The token expired on " + formatDateTime(expirationTimestamp))
      .isInstanceOf(AuthenticationException.class);
    verify(userLastConnectionDatesUpdater, never()).updateLastConnectionDateIfNeeded(any(UserTokenDto.class));
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void authenticate_givenGlobalToken_resultContainsUuid() {
    UserDto user = db.users().insertUser();
    String tokenName = db.users().insertToken(user, t -> t.setTokenHash(GLOBAL_ANALYSIS_TOKEN_HASH).setType(GLOBAL_ANALYSIS_TOKEN.name())).getName();

    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic " + toBase64(EXAMPLE_GLOBAL_ANALYSIS_TOKEN + ":"));
    var result = underTest.authenticate(request);

    assertThat(result).isPresent();
    assertThat(result.get().getTokenDto().getUuid()).isNotNull();
    assertThat(result.get().getTokenDto().getType()).isEqualTo(GLOBAL_ANALYSIS_TOKEN.name());
    verify(authenticationEvent).loginSuccess(request, user.getLogin(), AuthenticationEvent.Source.local(SONARQUBE_TOKEN));
    verify(request).setAttribute("TOKEN_NAME", tokenName);
  }

  @Test
  public void authenticate_givenNewUserToken_resultContainsUuid() {
    UserDto user = db.users().insertUser();
    String tokenName = db.users().insertToken(user, t -> t.setTokenHash(NEW_USER_TOKEN_HASH).setType(USER_TOKEN.name())).getName();

    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic " + toBase64(EXAMPLE_NEW_USER_TOKEN + ":"));
    var result = underTest.authenticate(request);

    assertThat(result).isPresent();
    assertThat(result.get().getTokenDto().getUuid()).isNotNull();
    assertThat(result.get().getTokenDto().getType()).isEqualTo(USER_TOKEN.name());
    verify(authenticationEvent).loginSuccess(request, user.getLogin(), AuthenticationEvent.Source.local(SONARQUBE_TOKEN));
    verify(request).setAttribute("TOKEN_NAME", tokenName);
  }

  @Test
  public void authenticate_givenProjectToken_resultContainsUuid() {
    UserDto user = db.users().insertUser();
    String tokenName = db.users().insertToken(user, t -> t.setTokenHash(PROJECT_ANALYSIS_TOKEN_HASH)
      .setProjectUuid("project-uuid")
      .setType(PROJECT_ANALYSIS_TOKEN.name())).getName();

    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic " + toBase64(EXAMPLE_PROJECT_ANALYSIS_TOKEN + ":"));
    var result = underTest.authenticate(request);

    assertThat(result).isPresent();
    assertThat(result.get().getTokenDto().getUuid()).isNotNull();
    assertThat(result.get().getTokenDto().getType()).isEqualTo(PROJECT_ANALYSIS_TOKEN.name());
    assertThat(result.get().getTokenDto().getProjectUuid()).isEqualTo("project-uuid");
    verify(authenticationEvent).loginSuccess(request, user.getLogin(), AuthenticationEvent.Source.local(SONARQUBE_TOKEN));
    verify(request).setAttribute("TOKEN_NAME", tokenName);
  }

  @Test
  public void does_not_authenticate_from_user_token_when_token_does_not_match_active_user() {
    UserDto user = db.users().insertDisabledUser();
    db.users().insertToken(user, t -> t.setTokenHash(NEW_USER_TOKEN_HASH).setType(USER_TOKEN.name())).getName();

    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic " + toBase64(EXAMPLE_NEW_USER_TOKEN + ":"));

    assertThatThrownBy(() -> underTest.authenticate(request))
      .hasMessageContaining("User doesn't exist")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", AuthenticationEvent.Source.local(SONARQUBE_TOKEN));

    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void return_token_from_db() {
    String token = "known-token";
    String tokenHash = "123456789";
    when(tokenGenerator.hash(token)).thenReturn(tokenHash);
    UserDto user1 = db.users().insertUser();
    UserTokenDto userTokenDto = db.users().insertToken(user1, t -> t.setTokenHash(tokenHash));

    UserTokenDto result = underTest.getUserToken(token);

    assertThat(result.getUuid()).isEqualTo(userTokenDto.getUuid());
  }

  @Test
  public void return_login_when_token_hash_found_in_db2() {
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic " + toBase64("login:password"));

    Optional<UserAuthResult> result = underTest.authenticate(request);

    assertThat(result).isEmpty();
  }

  @Test
  public void return_login_when_token_hash_found_in_db3() {
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(null);

    Optional<UserAuthResult> result = underTest.authenticate(request);

    assertThat(result).isEmpty();
  }

  private static String toBase64(String text) {
    return new String(BASE64_ENCODER.encode(text.getBytes(UTF_8)));
  }
}
