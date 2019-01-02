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

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.tester.AnonymousMockUserSession;
import org.sonar.server.tester.MockUserSession;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserSessionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;

public class RequestAuthenticatorImplTest {

  private static final UserDto A_USER = newUserDto();

  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);
  private BasicAuthentication basicAuthentication = mock(BasicAuthentication.class);
  private HttpHeadersAuthentication httpHeadersAuthentication = mock(HttpHeadersAuthentication.class);
  private UserSessionFactory sessionFactory = mock(UserSessionFactory.class);
  private CustomAuthentication customAuthentication1 = mock(CustomAuthentication.class);
  private CustomAuthentication customAuthentication2 = mock(CustomAuthentication.class);
  private RequestAuthenticator underTest = new RequestAuthenticatorImpl(jwtHttpHandler, basicAuthentication, httpHeadersAuthentication, sessionFactory,
    new CustomAuthentication[]{customAuthentication1, customAuthentication2});

  @Before
  public void setUp() throws Exception {
    when(sessionFactory.create(A_USER)).thenReturn(new MockUserSession(A_USER));
    when(sessionFactory.createAnonymous()).thenReturn(new AnonymousMockUserSession());
  }

  @Test
  public void authenticate_from_jwt_token() {
    when(httpHeadersAuthentication.authenticate(request, response)).thenReturn(Optional.empty());
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.of(A_USER));

    assertThat(underTest.authenticate(request, response).getUuid()).isEqualTo(A_USER.getUuid());
    verify(response, never()).setStatus(anyInt());
  }

  @Test
  public void authenticate_from_basic_header() {
    when(basicAuthentication.authenticate(request)).thenReturn(Optional.of(A_USER));
    when(httpHeadersAuthentication.authenticate(request, response)).thenReturn(Optional.empty());
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.empty());

    assertThat(underTest.authenticate(request, response).getUuid()).isEqualTo(A_USER.getUuid());

    verify(jwtHttpHandler).validateToken(request, response);
    verify(basicAuthentication).authenticate(request);
    verify(response, never()).setStatus(anyInt());
  }

  @Test
  public void authenticate_from_sso() {
    when(httpHeadersAuthentication.authenticate(request, response)).thenReturn(Optional.of(A_USER));
    when(jwtHttpHandler.validateToken(request, response)).thenReturn(Optional.empty());

    assertThat(underTest.authenticate(request, response).getUuid()).isEqualTo(A_USER.getUuid());

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
    verify(response, never()).setStatus(anyInt());
  }

  @Test
  public void delegate_to_CustomAuthentication() {
    when(customAuthentication1.authenticate(request, response)).thenReturn(Optional.of(new MockUserSession("foo")));

    UserSession session = underTest.authenticate(request, response);

    assertThat(session.getLogin()).isEqualTo("foo");
  }

  @Test
  public void CustomAuthentication_has_priority_over_core_authentications() {
    // use-case: both custom and core authentications check the HTTP header "Authorization".
    // The custom authentication should be able to test the header because that the core authentication
    // throws an exception.
    when(customAuthentication1.authenticate(request, response)).thenReturn(Optional.of(new MockUserSession("foo")));
    when(basicAuthentication.authenticate(request)).thenThrow(AuthenticationException.newBuilder()
      .setSource(AuthenticationEvent.Source.sso())
      .setMessage("message")
      .build());

    UserSession session = underTest.authenticate(request, response);

    assertThat(session.getLogin()).isEqualTo("foo");
  }
}
