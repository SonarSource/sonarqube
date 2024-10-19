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

import com.google.common.io.Resources;
import java.io.IOException;
import java.util.Optional;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.FilterChain;
import org.sonar.api.web.HttpFilter;
import org.sonar.api.web.UrlPattern;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.BasicAuthentication;
import org.sonar.server.authentication.HttpHeadersAuthentication;
import org.sonar.server.authentication.JwtHttpHandler;
import org.sonar.server.authentication.UserAuthResult;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.usertoken.UserTokenAuthentication;
import org.sonar.server.ws.ServletFilterHandler;
import org.sonarqube.ws.MediaTypes;

import static org.sonar.api.CoreProperties.CORE_FORCE_AUTHENTICATION_DEFAULT_VALUE;
import static org.sonar.api.CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY;
import static org.sonar.server.authentication.ws.AuthenticationWs.AUTHENTICATION_CONTROLLER;

public class ValidateAction extends HttpFilter implements AuthenticationWsAction {

  private static final String VALIDATE_ACTION = "validate";
  public static final String VALIDATE_URL = "/" + AUTHENTICATION_CONTROLLER + "/" + VALIDATE_ACTION;

  private final Configuration config;
  private final JwtHttpHandler jwtHttpHandler;
  private final BasicAuthentication basicAuthentication;
  private final HttpHeadersAuthentication httpHeadersAuthentication;
  private final UserTokenAuthentication userTokenAuthentication;

  public ValidateAction(Configuration config, BasicAuthentication basicAuthentication, JwtHttpHandler jwtHttpHandler, HttpHeadersAuthentication httpHeadersAuthentication,
    UserTokenAuthentication userTokenAuthentication) {

    this.config = config;
    this.basicAuthentication = basicAuthentication;
    this.jwtHttpHandler = jwtHttpHandler;
    this.httpHeadersAuthentication = httpHeadersAuthentication;
    this.userTokenAuthentication = userTokenAuthentication;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction(VALIDATE_ACTION)
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
  public void doFilter(HttpRequest request, HttpResponse response, FilterChain filterChain) throws IOException {
    boolean isAuthenticated = authenticate(request, response);
    response.setContentType(MediaTypes.JSON);

    try (JsonWriter jsonWriter = JsonWriter.of(response.getWriter())) {
      jsonWriter.beginObject();
      jsonWriter.prop("valid", isAuthenticated);
      jsonWriter.endObject();
    }
  }

  private boolean authenticate(HttpRequest request, HttpResponse response) {
    try {
      Optional<UserDto> user = httpHeadersAuthentication.authenticate(request, response)
        .or(() -> jwtHttpHandler.validateToken(request, response))
        .or(() -> basicAuthentication.authenticate(request))
        .or(() -> userTokenAuthentication.authenticate(request).map(UserAuthResult::getUserDto));
      return user.isPresent() || !config.getBoolean(CORE_FORCE_AUTHENTICATION_PROPERTY).orElse(CORE_FORCE_AUTHENTICATION_DEFAULT_VALUE);
    } catch (AuthenticationException e) {
      return false;
    }
  }

  @Override
  public void init() {
    // Nothing to do
  }

  @Override
  public void destroy() {
    // Nothing to do
  }
}
