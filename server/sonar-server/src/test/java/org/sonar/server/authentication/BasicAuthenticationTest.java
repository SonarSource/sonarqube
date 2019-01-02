/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.usertoken.UserTokenAuthentication;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.BASIC;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.BASIC_TOKEN;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;
import static org.sonar.server.authentication.event.AuthenticationExceptionMatcher.authenticationException;

public class BasicAuthenticationTest {

  private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

  private static final String A_LOGIN = "login";
  private static final String A_PASSWORD = "password";
  private static final String CREDENTIALS_IN_BASE64 = toBase64(A_LOGIN + ":" + A_PASSWORD);

  private static final UserDto USER = UserTesting.newUserDto().setLogin(A_LOGIN);

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();

  private CredentialsAuthentication credentialsAuthentication = mock(CredentialsAuthentication.class);
  private UserTokenAuthentication userTokenAuthentication = mock(UserTokenAuthentication.class);

  private HttpServletRequest request = mock(HttpServletRequest.class);

  private AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);

  private BasicAuthentication underTest = new BasicAuthentication(dbClient, credentialsAuthentication, userTokenAuthentication, authenticationEvent);

  @Test
  public void authenticate_from_basic_http_header() {
    when(request.getHeader("Authorization")).thenReturn("Basic " + CREDENTIALS_IN_BASE64);
    Credentials credentials = new Credentials(A_LOGIN, A_PASSWORD);
    when(credentialsAuthentication.authenticate(credentials, request, BASIC)).thenReturn(USER);

    underTest.authenticate(request);

    verify(credentialsAuthentication).authenticate(credentials, request, BASIC);
    verifyNoMoreInteractions(authenticationEvent);
  }

  @Test
  public void authenticate_from_basic_http_header_with_password_containing_semi_colon() {
    String password = "!ascii-only:-)@";
    when(request.getHeader("Authorization")).thenReturn("Basic " + toBase64(A_LOGIN + ":" + password));
    when(credentialsAuthentication.authenticate(new Credentials(A_LOGIN, password), request, BASIC)).thenReturn(USER);

    underTest.authenticate(request);

    verify(credentialsAuthentication).authenticate(new Credentials(A_LOGIN, password), request, BASIC);
    verifyNoMoreInteractions(authenticationEvent);
  }

  @Test
  public void does_not_authenticate_when_no_authorization_header() {
    underTest.authenticate(request);

    verifyZeroInteractions(credentialsAuthentication, authenticationEvent);
  }

  @Test
  public void does_not_authenticate_when_authorization_header_is_not_BASIC() {
    when(request.getHeader("Authorization")).thenReturn("OTHER " + CREDENTIALS_IN_BASE64);

    underTest.authenticate(request);

    verifyZeroInteractions(credentialsAuthentication, authenticationEvent);
  }

  @Test
  public void fail_to_authenticate_when_no_login() {
    when(request.getHeader("Authorization")).thenReturn("Basic " + toBase64(":" + A_PASSWORD));

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
    UserDto user = db.users().insertUser();
    when(userTokenAuthentication.authenticate("token")).thenReturn(Optional.of(user.getUuid()));
    when(request.getHeader("Authorization")).thenReturn("Basic " + toBase64("token:"));

    Optional<UserDto> userAuthenticated = underTest.authenticate(request);

    assertThat(userAuthenticated.isPresent()).isTrue();
    assertThat(userAuthenticated.get().getLogin()).isEqualTo(user.getLogin());
    verify(authenticationEvent).loginSuccess(request, user.getLogin(), Source.local(BASIC_TOKEN));
  }

  @Test
  public void does_not_authenticate_from_user_token_when_token_is_invalid() {
    when(userTokenAuthentication.authenticate("token")).thenReturn(Optional.empty());
    when(request.getHeader("Authorization")).thenReturn("Basic " + toBase64("token:"));

    expectedException.expect(authenticationException().from(Source.local(BASIC_TOKEN)).withoutLogin().andNoPublicMessage());
    try {
      underTest.authenticate(request);
    } finally {
      verifyZeroInteractions(authenticationEvent);
    }
  }

  @Test
  public void does_not_authenticate_from_user_token_when_token_does_not_match_existing_user() {
    when(userTokenAuthentication.authenticate("token")).thenReturn(Optional.of("Unknown user"));
    when(request.getHeader("Authorization")).thenReturn("Basic " + toBase64("token:"));

    expectedException.expect(authenticationException().from(Source.local(Method.BASIC_TOKEN)).withoutLogin().andNoPublicMessage());
    try {
      underTest.authenticate(request);
    } finally {
      verifyZeroInteractions(authenticationEvent);
    }
  }

  @Test
  public void does_not_authenticate_from_user_token_when_token_does_not_match_active_user() {
    UserDto user = db.users().insertDisabledUser();
    when(userTokenAuthentication.authenticate("token")).thenReturn(Optional.of(user.getUuid()));
    when(request.getHeader("Authorization")).thenReturn("Basic " + toBase64("token:"));

    expectedException.expect(authenticationException().from(Source.local(Method.BASIC_TOKEN)).withoutLogin().andNoPublicMessage());
    try {
      underTest.authenticate(request);
    } finally {
      verifyZeroInteractions(authenticationEvent);
    }
  }

  private static String toBase64(String text) {
    return new String(BASE64_ENCODER.encode(text.getBytes(UTF_8)));
  }

}
