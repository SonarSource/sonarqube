/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Collections;
import java.util.Optional;
import org.junit.Test;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.FilterChain;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source.sso;

public class LogoutActionTest {

  private static final UserDto USER = newUserDto().setLogin("john");

  private final HttpRequest request = mock(HttpRequest.class);
  private final HttpResponse response = mock(HttpResponse.class);
  private final FilterChain chain = mock(FilterChain.class);

  private final JwtHttpHandler jwtHttpHandler = mock(JwtHttpHandler.class);
  private final AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);

  private final LogoutAction underTest = new LogoutAction(jwtHttpHandler, authenticationEvent);

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
  public void return_400_on_get_request() {
    when(request.getMethod()).thenReturn("GET");

    underTest.doFilter(request, response, chain);

    verifyNoInteractions(jwtHttpHandler, chain);
    verify(response).setStatus(400);
  }

  @Test
  public void logout_logged_user() {
    setUser(USER);

    executeRequest();

    verify(jwtHttpHandler).removeToken(request, response);
    verifyNoInteractions(chain);
    verify(authenticationEvent).logoutSuccess(request, "john");
  }

  @Test
  public void logout_unlogged_user() {
    setNoUser();

    executeRequest();

    verify(jwtHttpHandler).removeToken(request, response);
    verifyNoInteractions(chain);
    verify(authenticationEvent).logoutSuccess(request, null);
  }

  @Test
  public void generate_auth_event_on_failure() {
    setUser(USER);
    AuthenticationException exception = AuthenticationException.newBuilder().setMessage("error!").setSource(sso()).build();
    doThrow(exception).when(jwtHttpHandler).getToken(any(HttpRequest.class), any(HttpResponse.class));

    executeRequest();

    verify(authenticationEvent).logoutFailure(request, "error!");
    verify(jwtHttpHandler).removeToken(any(HttpRequest.class), any(HttpResponse.class));
    verifyNoInteractions(chain);
  }

  private void executeRequest() {
    when(request.getMethod()).thenReturn("POST");
    underTest.doFilter(request, response, chain);
  }

  private void setUser(UserDto user) {
    when(jwtHttpHandler.getToken(any(HttpRequest.class), any(HttpResponse.class)))
      .thenReturn(Optional.of(new JwtHttpHandler.Token(user, Collections.emptyMap())));
  }

  private void setNoUser() {
    when(jwtHttpHandler.getToken(any(HttpRequest.class), any(HttpResponse.class))).thenReturn(Optional.empty());
  }

}
