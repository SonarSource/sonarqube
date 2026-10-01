/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.web;

import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ws.LocalConnector;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.user.ThreadLocalUserSession;
import org.sonar.server.v2.security.UserSessionSnapshot;
import org.sonar.server.ws.V2LocalRequestDispatcher;
import org.sonar.server.ws.WebServiceEngine;
import org.sonarqube.ws.MediaTypes;

/**
 * Serves {@link LocalConnector} calls to /api/v2/* by running them through the same Spring Security filter and
 * {@link ApiV2Servlet} as HTTP requests. The user session of the caller is reused and restored once the call completes.
 */
public class ApiV2LocalRequestDispatcher implements V2LocalRequestDispatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiV2LocalRequestDispatcher.class);

  private final ThreadLocalUserSession threadLocalUserSession;
  private final AtomicReference<Target> target = new AtomicReference<>();

  public ApiV2LocalRequestDispatcher(ThreadLocalUserSession threadLocalUserSession) {
    this.threadLocalUserSession = threadLocalUserSession;
  }

  public void init(ServletContext servletContext, Servlet apiV2Servlet, Filter springSecurityFilter) {
    target.set(new Target(servletContext, apiV2Servlet, springSecurityFilter));
  }

  @Override
  public LocalConnector.LocalResponse dispatch(LocalConnector.LocalRequest request) {
    Target currentTarget = target.get();
    if (currentTarget == null) {
      return errorResponse(404, "Unknown url : " + request.getPath());
    }

    try (UserSessionSnapshot ignored = UserSessionSnapshot.take(threadLocalUserSession)) {
      LocalHttpServletResponse response = new LocalHttpServletResponse();
      LocalHttpServletRequest servletRequest = new LocalHttpServletRequest(request, currentTarget.servletContext());
      currentTarget.springSecurityFilter().doFilter(servletRequest, response, currentTarget.apiV2Servlet()::service);
      return response;
    } catch (Exception e) {
      LOGGER.error("Fail to process local request {}", request.getPath(), e);
      return errorResponse(500, "An error has occurred. Please contact your administrator");
    }
  }

  private static LocalConnector.LocalResponse errorResponse(int status, String message) {
    LocalHttpServletResponse response = new LocalHttpServletResponse();
    response.setStatus(status);
    response.setContentType(MediaTypes.JSON);
    try (JsonWriter json = JsonWriter.of(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
      json.beginObject();
      WebServiceEngine.writeErrors(json, List.of(message));
      json.endObject();
    }
    return response;
  }

  private record Target(ServletContext servletContext, Servlet apiV2Servlet, Filter springSecurityFilter) {
  }
}
