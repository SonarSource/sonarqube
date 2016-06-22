/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import static org.sonar.api.CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY;

import java.io.IOException;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.ServletFilter;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.BasicAuthenticator;
import org.sonar.server.authentication.JwtHttpHandler;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonarqube.ws.MediaTypes;

public class ValidateAction extends ServletFilter {

  public static final String AUTH_VALIDATE_URL = "/api/authentication/validate";

  private final Settings settings;
  private final JwtHttpHandler jwtHttpHandler;
  private final BasicAuthenticator basicAuthenticator;

  public ValidateAction(Settings settings, BasicAuthenticator basicAuthenticator, JwtHttpHandler jwtHttpHandler) {
    this.settings = settings;
    this.basicAuthenticator = basicAuthenticator;
    this.jwtHttpHandler = jwtHttpHandler;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.create(AUTH_VALIDATE_URL);
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    boolean isAuthenticated = authenticate(request, response);
    response.setContentType(MediaTypes.JSON);

    JsonWriter jsonWriter = JsonWriter.of(response.getWriter());
    jsonWriter.beginObject();
    jsonWriter.prop("valid", isAuthenticated);
    jsonWriter.endObject();
  }

  private boolean authenticate(HttpServletRequest request, HttpServletResponse response) {
    try {
      Optional<UserDto> user = jwtHttpHandler.validateToken(request, response);
      if (user.isPresent()) {
        return true;
      }
      user = basicAuthenticator.authenticate(request);
      if (user.isPresent()) {
        return true;
      }
      return !settings.getBoolean(CORE_FORCE_AUTHENTICATION_PROPERTY);
    } catch (UnauthorizedException e) {
      return false;
    }
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Nothing to do
  }

  @Override
  public void destroy() {
    // Nothing to do
  }
}
