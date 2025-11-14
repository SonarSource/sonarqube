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
package org.sonar.server.authentication;

import java.util.Base64;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.usertoken.UserTokenAuthentication;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.BASIC;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.SONARQUBE_TOKEN;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

public class BasicAuthenticationTest {

  private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

  private static final String A_LOGIN = "login";
  private static final String A_PASSWORD = "password";
  private static final String CREDENTIALS_IN_BASE64 = toBase64(A_LOGIN + ":" + A_PASSWORD);

  private static final UserDto USER = UserTesting.newUserDto().setLogin(A_LOGIN);

  private static final String EXAMPLE_ENDPOINT = "/api/ce/submit";
  private static final String AUTHORIZATION_HEADER = "Authorization";

  @Rule
  public DbTester db = DbTester.create();

  private final CredentialsAuthentication credentialsAuthentication = mock(CredentialsAuthentication.class);
  private final UserTokenAuthentication userTokenAuthentication = mock(UserTokenAuthentication.class);

  private final HttpRequest request = mock(HttpRequest.class);

  private final AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);

  private final BasicAuthentication underTest = new BasicAuthentication(credentialsAuthentication, userTokenAuthentication);

  @Before
  public void before() {
    String contextPath = "localhost";
    when(request.getRequestURI()).thenReturn(contextPath + EXAMPLE_ENDPOINT);
    when(request.getContextPath()).thenReturn(contextPath);
  }

  @Test
  public void authenticate_from_basic_http_header() {
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic " + CREDENTIALS_IN_BASE64);
    Credentials credentials = new Credentials(A_LOGIN, A_PASSWORD);
    when(credentialsAuthentication.authenticate(credentials, request, BASIC)).thenReturn(USER);

    underTest.authenticate(request);

    verify(credentialsAuthentication).authenticate(credentials, request, BASIC);
    verifyNoMoreInteractions(authenticationEvent);
  }

  @Test
  public void authenticate_from_basic_http_header_with_password_containing_semi_colon() {
    String password = "!ascii-only:-)@";
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic " + toBase64(A_LOGIN + ":" + password));
    when(credentialsAuthentication.authenticate(new Credentials(A_LOGIN, password), request, BASIC)).thenReturn(USER);

    underTest.authenticate(request);

    verify(credentialsAuthentication).authenticate(new Credentials(A_LOGIN, password), request, BASIC);
    verifyNoMoreInteractions(authenticationEvent);
  }

  @Test
  public void does_not_authenticate_when_no_authorization_header() {
    underTest.authenticate(request);

    verifyNoInteractions(credentialsAuthentication, authenticationEvent);
  }

  @Test
  public void does_not_authenticate_when_authorization_header_is_not_BASIC() {
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("OTHER " + CREDENTIALS_IN_BASE64);

    underTest.authenticate(request);

    verifyNoInteractions(credentialsAuthentication, authenticationEvent);
  }

  @Test
  public void fail_to_authenticate_when_no_login() {
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic " + toBase64(":" + A_PASSWORD));

    assertThatThrownBy(() -> underTest.authenticate(request))
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.local(BASIC));

    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void fail_to_authenticate_when_invalid_header() {
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic Invàlid");

    assertThatThrownBy(() -> underTest.authenticate(request))
      .hasMessage("Invalid basic header")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.local(BASIC));
  }

  @Test
  public void authenticate_from_user_token() {
    UserDto user = db.users().insertUser();
    when(userTokenAuthentication.authenticate(request)).thenReturn(Optional.of(new UserAuthResult(user, new UserTokenDto().setName("my-token"), UserAuthResult.AuthType.TOKEN)));
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic " + toBase64("token:"));

    Optional<UserDto> userAuthenticated = underTest.authenticate(request);

    assertThat(userAuthenticated).isPresent();
    assertThat(userAuthenticated.get().getLogin()).isEqualTo(user.getLogin());
  }

  @Test
  public void does_not_authenticate_from_user_token_when_token_is_invalid() {
    when(userTokenAuthentication.authenticate(request)).thenReturn(Optional.empty());
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic " + toBase64("token:"));

    assertThatThrownBy(() -> underTest.authenticate(request))
      .hasMessage("User doesn't exist")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.local(SONARQUBE_TOKEN));

    verifyNoInteractions(authenticationEvent);
    verify(request, times(0)).setAttribute(anyString(), anyString());
  }

  @Test
  public void does_not_authenticate_from_user_token_when_token_does_not_match_existing_user() {
    when(userTokenAuthentication.authenticate(request)).thenThrow(AuthenticationException.newBuilder()
      .setSource(AuthenticationEvent.Source.local(AuthenticationEvent.Method.SONARQUBE_TOKEN))
      .setMessage("User doesn't exist")
      .build());
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Basic " + toBase64("token:"));

    assertThatThrownBy(() -> underTest.authenticate(request))
      .hasMessageContaining("User doesn't exist")
      .isInstanceOf(AuthenticationException.class)
      .hasFieldOrPropertyWithValue("source", Source.local(SONARQUBE_TOKEN));

    verifyNoInteractions(authenticationEvent);
  }

  private static String toBase64(String text) {
    return new String(BASE64_ENCODER.encode(text.getBytes(UTF_8)));
  }

}
