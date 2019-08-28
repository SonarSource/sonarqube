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
import java.util.Collections;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.JwtHttpHandler;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.ws.ServletFilterHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source.sso;

public class LogoutActionTest {

  private static final UserDto USER = newUserDto().setLogin("john");

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private FilterChain chain = mock(FilterChain.class);

  private JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);
  private AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);

  private LogoutAction underTest = new LogoutAction(jwtHttpHandler, authenticationEvent);

  @Test
  public void verify_definition() {
    String controllerKey = "foo";
    WebService.Context context = new WebService.Context();
    WebService.NewController newController = context.createController(controllerKey);
    underTest.define(newController);
    newController.done();

    WebService.Action logout = context.controller(controllerKey).action("logout");
    assertThat(logout).isNotNull();
    assertThat(logout.handler()).isInstanceOf(ServletFilterHandler.class);
    assertThat(logout.isPost()).isTrue();
    assertThat(logout.params()).isEmpty();
  }

  @Test
  public void do_get_pattern() {
    assertThat(underTest.doGetPattern().matches("/api/authentication/logout")).isTrue();
    assertThat(underTest.doGetPattern().matches("/api/authentication/login")).isFalse();
    assertThat(underTest.doGetPattern().matches("/api/authentication/logou")).isFalse();
    assertThat(underTest.doGetPattern().matches("/api/authentication/logoutthing")).isFalse();
    assertThat(underTest.doGetPattern().matches("/foo")).isFalse();
  }

  @Test
  public void return_400_on_get_request() throws Exception {
    when(request.getMethod()).thenReturn("GET");

    underTest.doFilter(request, response, chain);

    verifyZeroInteractions(jwtHttpHandler, chain);
    verify(response).setStatus(400);
  }

  @Test
  public void logout_logged_user() throws Exception {
    setUser(USER);

    executeRequest();

    verify(jwtHttpHandler).removeToken(request, response);
    verifyZeroInteractions(chain);
    verify(authenticationEvent).logoutSuccess(request, "john");
  }

  @Test
  public void logout_unlogged_user() throws Exception {
    setNoUser();

    executeRequest();

    verify(jwtHttpHandler).removeToken(request, response);
    verifyZeroInteractions(chain);
    verify(authenticationEvent).logoutSuccess(request, null);
  }

  @Test
  public void generate_auth_event_on_failure() throws Exception {
    setUser(USER);
    AuthenticationException exception = AuthenticationException.newBuilder().setMessage("error!").setSource(sso()).build();
    doThrow(exception).when(jwtHttpHandler).getToken(any(HttpServletRequest.class), any(HttpServletResponse.class));

    executeRequest();

    verify(authenticationEvent).logoutFailure(request, "error!");
    verify(jwtHttpHandler).removeToken(any(HttpServletRequest.class), any(HttpServletResponse.class));
    verifyZeroInteractions(chain);
  }

  private void executeRequest() throws IOException, ServletException {
    when(request.getMethod()).thenReturn("POST");
    underTest.doFilter(request, response, chain);
  }

  private void setUser(UserDto user) {
    when(jwtHttpHandler.getToken(any(HttpServletRequest.class), any(HttpServletResponse.class)))
      .thenReturn(Optional.of(new JwtHttpHandler.Token(user, Collections.emptyMap())));
  }

  private void setNoUser() {
    when(jwtHttpHandler.getToken(any(HttpServletRequest.class), any(HttpServletResponse.class))).thenReturn(Optional.empty());
  }

}
