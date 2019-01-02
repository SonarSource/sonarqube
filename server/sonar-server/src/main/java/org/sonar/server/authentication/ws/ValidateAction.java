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

import com.google.common.io.Resources;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.ServletFilter;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.BasicAuthentication;
import org.sonar.server.authentication.JwtHttpHandler;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.ws.ServletFilterHandler;
import org.sonarqube.ws.MediaTypes;

import static org.sonar.api.CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY;
import static org.sonar.server.authentication.ws.AuthenticationWs.AUTHENTICATION_CONTROLLER;

public class ValidateAction extends ServletFilter implements AuthenticationWsAction {

  private static final String VALIDATE_ACTION = "validate";
  public static final String VALIDATE_URL = "/" + AUTHENTICATION_CONTROLLER + "/" + VALIDATE_ACTION;

  private final Configuration config;
  private final JwtHttpHandler jwtHttpHandler;
  private final BasicAuthentication basicAuthentication;

  public ValidateAction(Configuration config, BasicAuthentication basicAuthentication, JwtHttpHandler jwtHttpHandler) {
    this.config = config;
    this.basicAuthentication = basicAuthentication;
    this.jwtHttpHandler = jwtHttpHandler;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("validate")
      .setDescription("Check credentials.")
      .setSince("3.3")
      .setHandler(ServletFilterHandler.INSTANCE)
      .setResponseExample(Resources.getResource(this.getClass(), "example-validate.json"));
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.create(VALIDATE_URL);
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    boolean isAuthenticated = authenticate(request, response);
    response.setContentType(MediaTypes.JSON);

    try (JsonWriter jsonWriter = JsonWriter.of(response.getWriter())) {
      jsonWriter.beginObject();
      jsonWriter.prop("valid", isAuthenticated);
      jsonWriter.endObject();
    }
  }

  private boolean authenticate(HttpServletRequest request, HttpServletResponse response) {
    try {
      Optional<UserDto> user = jwtHttpHandler.validateToken(request, response);
      if (user.isPresent()) {
        return true;
      }
      user = basicAuthentication.authenticate(request);
      if (user.isPresent()) {
        return true;
      }
      return !config.getBoolean(CORE_FORCE_AUTHENTICATION_PROPERTY).orElse(false);
    } catch (AuthenticationException e) {
      return false;
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
