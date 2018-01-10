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
package org.sonar.server.authentication;

import java.util.Base64;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.usertoken.UserTokenAuthenticator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.BASIC;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.BASIC_TOKEN;
import static org.sonar.server.authentication.event.AuthenticationExceptionMatcher.authenticationException;

public class BasicAuthenticatorTest {

  private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

  private static final String LOGIN = "login";
  private static final String PASSWORD = "password";
  private static final String CREDENTIALS_IN_BASE64 = toBase64(LOGIN + ":" + PASSWORD);

  private static final UserDto USER = UserTesting.newUserDto().setLogin(LOGIN);

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();

  private DbSession dbSession = dbTester.getSession();

  private CredentialsAuthenticator credentialsAuthenticator = mock(CredentialsAuthenticator.class);
  private UserTokenAuthenticator userTokenAuthenticator = mock(UserTokenAuthenticator.class);

  private HttpServletRequest request = mock(HttpServletRequest.class);

  private AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);

  private BasicAuthenticator underTest = new BasicAuthenticator(dbClient, credentialsAuthenticator, userTokenAuthenticator, authenticationEvent);

  @Test
  public void authenticate_from_basic_http_header() {
    when(request.getHeader("Authorization")).thenReturn("Basic " + CREDENTIALS_IN_BASE64);
    when(credentialsAuthenticator.authenticate(LOGIN, PASSWORD, request, BASIC)).thenReturn(USER);

    underTest.authenticate(request);

    verify(credentialsAuthenticator).authenticate(LOGIN, PASSWORD, request, BASIC);
    verifyNoMoreInteractions(authenticationEvent);
  }

  @Test
  public void authenticate_from_basic_http_header_with_password_containing_semi_colon() {
    String password = "!ascii-only:-)@";
    when(request.getHeader("Authorization")).thenReturn("Basic " + toBase64(LOGIN + ":" + password));
    when(credentialsAuthenticator.authenticate(LOGIN, password, request, BASIC)).thenReturn(USER);

    underTest.authenticate(request);

    verify(credentialsAuthenticator).authenticate(LOGIN, password, request, BASIC);
    verifyNoMoreInteractions(authenticationEvent);
  }

  @Test
  public void does_not_authenticate_when_no_authorization_header() {
    underTest.authenticate(request);

    verifyZeroInteractions(credentialsAuthenticator, authenticationEvent);
  }

  @Test
  public void does_not_authenticate_when_authorization_header_is_not_BASIC() {
    when(request.getHeader("Authorization")).thenReturn("OTHER " + CREDENTIALS_IN_BASE64);

    underTest.authenticate(request);

    verifyZeroInteractions(credentialsAuthenticator, authenticationEvent);
  }

  @Test
  public void fail_to_authenticate_when_no_login() {
    when(request.getHeader("Authorization")).thenReturn("Basic " + toBase64(":" + PASSWORD));

    expectedException.expect(authenticationException().from(Source.local(BASIC)).withoutLogin().andNoPublicMessage());
    try {
      underTest.authenticate(request);
    } finally {
      verifyZeroInteractions(authenticationEvent);
    }
  }

  @Test
  public void fail_to_authenticate_when_invalid_header() {
    when(request.getHeader("Authorization")).thenReturn("Basic Invàlid");

    expectedException.expect(authenticationException().from(Source.local(BASIC)).withoutLogin().andNoPublicMessage());
    expectedException.expectMessage("Invalid basic header");
    underTest.authenticate(request);
  }

  @Test
  public void authenticate_from_user_token() {
    insertUser(UserTesting.newUserDto().setLogin(LOGIN));
    when(userTokenAuthenticator.authenticate("token")).thenReturn(Optional.of(LOGIN));
    when(request.getHeader("Authorization")).thenReturn("Basic " + toBase64("token:"));

    Optional<UserDto> userDto = underTest.authenticate(request);

    assertThat(userDto.isPresent()).isTrue();
    assertThat(userDto.get().getLogin()).isEqualTo(LOGIN);
    verify(authenticationEvent).loginSuccess(request, LOGIN, Source.local(BASIC_TOKEN));
  }

  @Test
  public void does_not_authenticate_from_user_token_when_token_is_invalid() {
    insertUser(UserTesting.newUserDto().setLogin(LOGIN));
    when(userTokenAuthenticator.authenticate("token")).thenReturn(Optional.empty());
    when(request.getHeader("Authorization")).thenReturn("Basic " + toBase64("token:"));

    expectedException.expect(authenticationException().from(Source.local(BASIC_TOKEN)).withoutLogin().andNoPublicMessage());
    try {
      underTest.authenticate(request);
    } finally {
      verifyZeroInteractions(authenticationEvent);
    }
  }

  @Test
  public void does_not_authenticate_from_user_token_when_token_does_not_match_active_user() {
    insertUser(UserTesting.newUserDto().setLogin(LOGIN));
    when(userTokenAuthenticator.authenticate("token")).thenReturn(Optional.of("Unknown user"));
    when(request.getHeader("Authorization")).thenReturn("Basic " + toBase64("token:"));

    expectedException.expect(authenticationException().from(Source.local(Method.BASIC_TOKEN)).withoutLogin().andNoPublicMessage());
    try {
      underTest.authenticate(request);
    } finally {
      verifyZeroInteractions(authenticationEvent);
    }
  }

  private UserDto insertUser(UserDto userDto) {
    dbClient.userDao().insert(dbSession, userDto);
    dbSession.commit();
    return userDto;
  }

  private static String toBase64(String text) {
    return new String(BASE64_ENCODER.encode(text.getBytes(UTF_8)));
  }

}
