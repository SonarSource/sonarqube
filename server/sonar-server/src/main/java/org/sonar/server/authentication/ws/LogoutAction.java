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

import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.ServletFilter;
import org.sonar.server.authentication.JwtHttpHandler;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.ws.ServletFilterHandler;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static org.sonar.server.authentication.ws.AuthenticationWs.AUTHENTICATION_CONTROLLER;
import static org.sonarqube.ws.client.WsRequest.Method.POST;

public class LogoutAction extends ServletFilter implements AuthenticationWsAction {

  private static final String LOGOUT_ACTION = "logout";
  public static final String LOGOUT_URL = "/" + AUTHENTICATION_CONTROLLER + "/" + LOGOUT_ACTION;

  private final JwtHttpHandler jwtHttpHandler;
  private final AuthenticationEvent authenticationEvent;

  public LogoutAction(JwtHttpHandler jwtHttpHandler, AuthenticationEvent authenticationEvent) {
    this.jwtHttpHandler = jwtHttpHandler;
    this.authenticationEvent = authenticationEvent;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction(LOGOUT_ACTION)
      .setDescription("Logout a user.")
      .setSince("6.3")
      .setPost(true)
      .setHandler(ServletFilterHandler.INSTANCE);
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.create(LOGOUT_URL);
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    if (!request.getMethod().equals(POST.name())) {
      response.setStatus(HTTP_BAD_REQUEST);
      return;
    }
    logout(request, response);
  }

  private void logout(HttpServletRequest request, HttpServletResponse response) {
    generateAuthenticationEvent(request, response);
    jwtHttpHandler.removeToken(request, response);
  }

  /**
   * The generation of the authentication event should not prevent the removal of JWT cookie, that's why it's done in a separate method
   */
  private void generateAuthenticationEvent(HttpServletRequest request, HttpServletResponse response) {
    try {
      Optional<JwtHttpHandler.Token> token = jwtHttpHandler.getToken(request, response);
      String userLogin = token.isPresent() ? token.get().getUserDto().getLogin() : null;
      authenticationEvent.logoutSuccess(request, userLogin);
    } catch (AuthenticationException e) {
      authenticationEvent.logoutFailure(request, e.getMessage());
    }
  }

  @Override
  public void init(FilterConfig filterConfig) {
    // Nothing to do
  }

  @Override
  public void destroy() {
    // Nothing to do
  }
}
