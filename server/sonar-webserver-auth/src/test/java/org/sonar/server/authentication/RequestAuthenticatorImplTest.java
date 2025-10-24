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
package org.sonar.server.authentication;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.tester.AnonymousMockUserSession;
import org.sonar.server.tester.MockUserSession;
import org.sonar.server.user.GithubWebhookUserSession;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserSessionFactory;
import org.sonar.server.usertoken.UserTokenAuthentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.TokenType.USER_TOKEN;
import static org.sonar.db.user.UserTesting.newUserDto;

public class RequestAuthenticatorImplTest {

  private static final UserDto A_USER = newUserDto();
  private static final UserTokenDto A_USER_TOKEN = mockUserTokenDto(A_USER);

  private final HttpRequest request = mock(HttpRequest.class);
  private final HttpResponse response = mock(HttpResponse.class);
  private final JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);
  private final BasicAuthentication basicAuthentication = mock(BasicAuthentication.class);
  private final UserTokenAuthentication userTokenAuthentication = mock(UserTokenAuthentication.class);
  private final GithubWebhookAuthentication githubWebhookAuthentication = mock(GithubWebhookAuthentication.class);
  private final HttpHeadersAuthentication httpHeadersAuthentication = mock(HttpHeadersAuthentication.class);
  private final UserSessionFactory sessionFactory = mock(UserSessionFactory.class);
  private final RequestAuthenticator underTest = new RequestAuthenticatorImpl(jwtHttpHandler, basicAuthentication, userTokenAuthentication, httpHeadersAuthentication,
    githubWebhookAuthentication, sessionFactory);

  private final GithubWebhookUserSession githubWebhookMockUserSession = mock(GithubWebhookUserSession.class);

  @Before
  public void setUp() {
    when(sessionFactory.create(eq(A_USER), anyBoolean())).thenAnswer((Answer<UserSession>) invocation -> {
      MockUserSession mockUserSession = new MockUserSession(A_USER);
      Boolean isAuthenticatedBrowserSession = invocation.getArgument(1, Boolean.class);
      if (isAuthenticatedBrowserSession) {
        mockUserSession.flagAsBrowserSession();
      }
      return mockUserSession;
    })

      .thenReturn(new MockUserSession(A_USER));
    when(sessionFactory.create(A_USER, A_USER_TOKEN)).thenReturn(new MockUserSession(A_USER));
    when(sessionFactory.createAnonymous()).thenReturn(new AnonymousMockUserSession());
    when(sessionFactory.createGithubWebhookUserSession()).thenReturn(githubWebhookMockUserSession);
  }

  @Test
  public void authenticate_from_jwt_token() {
    when(httpHeadersAuthentication.authenticate(request, response)).thenReturn(Optional.empty());
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.of(A_USER));

    UserSession userSession = underTest.authenticate(request, response);
    assertThat(userSession.getUuid()).isEqualTo(A_USER.getUuid());
    assertThat(userSession.isAuthenticatedBrowserSession()).isTrue();

    verify(response, never()).setStatus(anyInt());
  }

  @Test
  public void authenticate_from_githubWebhook() {
    when(httpHeadersAuthentication.authenticate(request, response)).thenReturn(Optional.empty());
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.empty());
    when(githubWebhookAuthentication.authenticate(request)).thenReturn(Optional.of(UserAuthResult.withGithubWebhook()));

    UserSession userSession = underTest.authenticate(request, response);
    assertThat(userSession).isInstanceOf(GithubWebhookUserSession.class);
    assertThat(userSession.isAuthenticatedBrowserSession()).isFalse();
    verify(response, never()).setStatus(anyInt());
  }

  @Test
  public void authenticate_from_basic_header() {
    when(basicAuthentication.authenticate(request)).thenReturn(Optional.of(A_USER));
    when(httpHeadersAuthentication.authenticate(request, response)).thenReturn(Optional.empty());
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.empty());

    UserSession userSession = underTest.authenticate(request, response);
    assertThat(userSession.getUuid()).isEqualTo(A_USER.getUuid());
    assertThat(userSession.isAuthenticatedBrowserSession()).isFalse();

    verify(jwtHttpHandler).validateToken(request, response);
    verify(basicAuthentication).authenticate(request);
    verify(response, never()).setStatus(anyInt());
  }

  @Test
  public void authenticate_from_basic_token() {
    String token = "test-user-authentication-token";
    String basicAuthHeader = "Basic " + java.util.Base64.getEncoder().encodeToString((token + ":").getBytes());
    when(request.getHeader("Authorization")).thenReturn(basicAuthHeader);
    when(userTokenAuthentication.getUserToken(token)).thenReturn(A_USER_TOKEN);
    when(userTokenAuthentication.authenticate(request)).thenReturn(Optional.of(new UserAuthResult(A_USER, A_USER_TOKEN, UserAuthResult.AuthType.TOKEN)));
    when(httpHeadersAuthentication.authenticate(request, response)).thenReturn(Optional.empty());
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.empty());

    UserSession userSession = underTest.authenticate(request, response);
    assertThat(userSession.getUuid()).isEqualTo(A_USER.getUuid());
    assertThat(userSession.isAuthenticatedBrowserSession()).isFalse();

    verify(jwtHttpHandler).validateToken(request, response);
    verify(userTokenAuthentication).authenticate(request);
    verify(response, never()).setStatus(anyInt());
  }

  @Test
  public void authenticate_from_sso() {
    when(httpHeadersAuthentication.authenticate(request, response)).thenReturn(Optional.of(A_USER));
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.empty());

    UserSession userSession = underTest.authenticate(request, response);
    assertThat(userSession.getUuid()).isEqualTo(A_USER.getUuid());
    assertThat(userSession.isAuthenticatedBrowserSession()).isFalse();

    verify(httpHeadersAuthentication).authenticate(request, response);
    verify(jwtHttpHandler, never()).validateToken(request, response);
    verify(response, never()).setStatus(anyInt());
  }

  @Test
  public void return_empty_if_not_authenticated() {
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.empty());
    when(httpHeadersAuthentication.authenticate(request, response)).thenReturn(Optional.empty());
    when(basicAuthentication.authenticate(request)).thenReturn(Optional.empty());

    UserSession session = underTest.authenticate(request, response);
    assertThat(session.isLoggedIn()).isFalse();
    assertThat(session.getUuid()).isNull();
    assertThat(session.isAuthenticatedBrowserSession()).isFalse();

    verify(response, never()).setStatus(anyInt());
  }

  private static UserTokenDto mockUserTokenDto(UserDto userDto) {
    UserTokenDto userTokenDto = new UserTokenDto();
    userTokenDto.setType(USER_TOKEN.name());
    userTokenDto.setName("User Token");
    userTokenDto.setUserUuid(userDto.getUuid());
    return userTokenDto;
  }

}
