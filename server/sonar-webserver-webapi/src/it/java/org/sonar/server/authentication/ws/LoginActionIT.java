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
package org.sonar.server.authentication.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.FilterChain;
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
import org.sonar.server.ws.ServletFilterHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method.FORM;

public class LoginActionIT {

  private static final String LOGIN = "LOGIN";
  private static final String PASSWORD = "PASSWORD";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = dbTester.getDbClient();

  private final DbSession dbSession = dbTester.getSession();

  private final ThreadLocalUserSession threadLocalUserSession = new ThreadLocalUserSession();

  private final HttpRequest request = mock(HttpRequest.class);
  private final HttpResponse response = mock(HttpResponse.class);
  private final FilterChain chain = mock(FilterChain.class);

  private final CredentialsAuthentication credentialsAuthentication = mock(CredentialsAuthentication.class);
  private final JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);
  private final AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);
  private final TestUserSessionFactory userSessionFactory = TestUserSessionFactory.standalone();

  private final UserDto user = UserTesting.newUserDto().setLogin(LOGIN);
  private final LoginAction underTest = new LoginAction(credentialsAuthentication, jwtHttpHandler, threadLocalUserSession, authenticationEvent, userSessionFactory);

  @Before
  public void setUp() {
    threadLocalUserSession.unload();
    dbClient.userDao().insert(dbSession, user);
    dbSession.commit();
  }

  @Test
  public void verify_definition() {
    String controllerKey = "foo";
    WebService.Context context = new WebService.Context();
    WebService.NewController newController = context.createController(controllerKey);
    underTest.define(newController);
    newController.done();

    WebService.Action login = context.controller(controllerKey).action("login");
    assertThat(login).isNotNull();
    assertThat(login.handler()).isInstanceOf(ServletFilterHandler.class);
    assertThat(login.isPost()).isTrue();
    assertThat(login.params()).hasSize(2);
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
    verifyNoInteractions(chain);
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void ignore_get_request() {
    when(request.getMethod()).thenReturn("GET");

    underTest.doFilter(request, response, chain);

    verifyNoInteractions(credentialsAuthentication, jwtHttpHandler, chain);
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void return_authorized_code_when_unauthorized_exception_is_thrown() {
    doThrow(new UnauthorizedException("error !")).when(credentialsAuthentication).authenticate(new Credentials(LOGIN, PASSWORD), request, FORM);

    executeRequest(LOGIN, PASSWORD);

    verify(response).setStatus(401);
    assertThat(threadLocalUserSession.hasSession()).isFalse();
    verifyNoInteractions(authenticationEvent);
  }

  @Test
  public void return_unauthorized_code_when_no_login() {
    executeRequest(null, PASSWORD);
    verify(response).setStatus(401);
    verify(authenticationEvent).loginFailure(eq(request), any(AuthenticationException.class));
  }

  @Test
  public void return_unauthorized_code_when_empty_login() {
    executeRequest("", PASSWORD);
    verify(response).setStatus(401);
    verify(authenticationEvent).loginFailure(eq(request), any(AuthenticationException.class));
  }

  @Test
  public void return_unauthorized_code_when_no_password() {
    executeRequest(LOGIN, null);
    verify(response).setStatus(401);
    verify(authenticationEvent).loginFailure(eq(request), any(AuthenticationException.class));
  }

  @Test
  public void return_unauthorized_code_when_empty_password() {
    executeRequest(LOGIN, "");
    verify(response).setStatus(401);
    verify(authenticationEvent).loginFailure(eq(request), any(AuthenticationException.class));
  }

  private void executeRequest(String login, String password) {
    when(request.getMethod()).thenReturn("POST");
    when(request.getParameter("login")).thenReturn(login);
    when(request.getParameter("password")).thenReturn(password);
    underTest.doFilter(request, response, chain);
  }
}
