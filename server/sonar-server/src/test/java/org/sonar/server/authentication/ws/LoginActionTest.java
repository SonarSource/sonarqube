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
package org.sonar.server.authentication.ws;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTesting;
import org.sonar.server.authentication.Credentials;
import org.sonar.server.authentication.CredentialsAuthentication;
import org.sonar.server.authentication.JwtHttpHandler;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.user.TestUserSessionFactory;
import org.sonar.server.user.ThreadLocalUserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.FORM;

public class LoginActionTest {

  private static final String LOGIN = "LOGIN";
  private static final String PASSWORD = "PASSWORD";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();

  private DbSession dbSession = dbTester.getSession();

  private ThreadLocalUserSession threadLocalUserSession = new ThreadLocalUserSession();

  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private FilterChain chain = mock(FilterChain.class);

  private CredentialsAuthentication credentialsAuthentication = mock(CredentialsAuthentication.class);
  private JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);
  private AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);
  private TestUserSessionFactory userSessionFactory = TestUserSessionFactory.standalone();

  private UserDto user = UserTesting.newUserDto().setLogin(LOGIN);
  private LoginAction underTest = new LoginAction(credentialsAuthentication, jwtHttpHandler, threadLocalUserSession, authenticationEvent, userSessionFactory);

  @Before
  public void setUp() throws Exception {
    threadLocalUserSession.unload();
    dbClient.userDao().insert(dbSession, user);
    dbSession.commit();
  }

  @Test
  public void do_get_pattern() {
    assertThat(underTest.doGetPattern().matches("/api/authentication/login")).isTrue();
    assertThat(underTest.doGetPattern().matches("/api/authentication/logout")).isFalse();
    assertThat(underTest.doGetPattern().matches("/foo")).isFalse();
  }

  @Test
  public void do_authenticate() throws Exception {
    when(credentialsAuthentication.authenticate(new Credentials(LOGIN, PASSWORD), request, FORM)).thenReturn(user);

    executeRequest(LOGIN, PASSWORD);

    assertThat(threadLocalUserSession.isLoggedIn()).isTrue();
    verify(credentialsAuthentication).authenticate(new Credentials(LOGIN, PASSWORD), request, FORM);
    verify(jwtHttpHandler).generateToken(user, request, response);
    verifyZeroInteractions(chain);
    verifyZeroInteractions(authenticationEvent);
  }

  @Test
  public void ignore_get_request() {
    when(request.getMethod()).thenReturn("GET");

    underTest.doFilter(request, response, chain);

    verifyZeroInteractions(credentialsAuthentication, jwtHttpHandler, chain);
    verifyZeroInteractions(authenticationEvent);
  }

  @Test
  public void return_authorized_code_when_unauthorized_exception_is_thrown() throws Exception {
    doThrow(new UnauthorizedException("error !")).when(credentialsAuthentication).authenticate(new Credentials(LOGIN, PASSWORD), request, FORM);

    executeRequest(LOGIN, PASSWORD);

    verify(response).setStatus(401);
    assertThat(threadLocalUserSession.hasSession()).isFalse();
    verifyZeroInteractions(authenticationEvent);
  }

  @Test
  public void return_unauthorized_code_when_no_login() throws Exception {
    executeRequest(null, PASSWORD);
    verify(response).setStatus(401);
    verify(authenticationEvent).loginFailure(eq(request), any(AuthenticationException.class));
  }

  @Test
  public void return_unauthorized_code_when_empty_login() throws Exception {
    executeRequest("", PASSWORD);
    verify(response).setStatus(401);
    verify(authenticationEvent).loginFailure(eq(request), any(AuthenticationException.class));
  }

  @Test
  public void return_unauthorized_code_when_no_password() throws Exception {
    executeRequest(LOGIN, null);
    verify(response).setStatus(401);
    verify(authenticationEvent).loginFailure(eq(request), any(AuthenticationException.class));
  }

  @Test
  public void return_unauthorized_code_when_empty_password() throws Exception {
    executeRequest(LOGIN, "");
    verify(response).setStatus(401);
    verify(authenticationEvent).loginFailure(eq(request), any(AuthenticationException.class));
  }

  private void executeRequest(String login, String password) throws IOException, ServletException {
    when(request.getMethod()).thenReturn("POST");
    when(request.getParameter("login")).thenReturn(login);
    when(request.getParameter("password")).thenReturn(password);
    underTest.doFilter(request, response, chain);
  }
}
