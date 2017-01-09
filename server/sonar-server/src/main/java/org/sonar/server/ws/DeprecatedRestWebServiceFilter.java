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

package org.sonar.server.ws;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.web.ServletFilter;
import org.sonar.server.property.ws.IndexAction;

import static org.sonar.server.property.ws.PropertiesWs.CONTROLLER_PROPERTIES;

/**
 * This filter is used to execute some deprecated Java WS, that were using REST
 */
public class DeprecatedRestWebServiceFilter extends ServletFilter {

  private final WebServiceEngine webServiceEngine;

  public DeprecatedRestWebServiceFilter(WebServiceEngine webServiceEngine) {
    this.webServiceEngine = webServiceEngine;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.builder()
      .includes("/" + CONTROLLER_PROPERTIES + "/*")
      .build();
  }

  @Override
  public void doFilter(javax.servlet.ServletRequest servletRequest, javax.servlet.ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    String path = request.getRequestURI().replaceFirst(request.getContextPath(), "");
    Map<String, String> additionalParams = new HashMap<>();
    Optional<String> key = getKey(path);
    key.ifPresent(s -> additionalParams.put(IndexAction.PARAM_KEY, s));
    ServletRequest wsRequest = new ServletRequest(request, CONTROLLER_PROPERTIES + "/index", additionalParams);
    ServletResponse wsResponse = new ServletResponse(response);
    webServiceEngine.execute(wsRequest, wsResponse);
  }

  private static Optional<String> getKey(String path) {
    if (path.equals("/" + CONTROLLER_PROPERTIES)) {
      return Optional.empty();
    }
    String key = path.replace("/" + CONTROLLER_PROPERTIES + "/", "");
    if (key.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(key);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Nothing to do
  }

  @Override
  public void destroy() {
    // Nothing to do
  }

  static class ServletRequest extends org.sonar.server.ws.ServletRequest {

    private final String path;
    private final Map<String, String> additionalParams;

    public ServletRequest(HttpServletRequest source, String path, Map<String, String> additionalParams) {
      super(source);
      this.path = path;
      this.additionalParams = additionalParams;
    }

    @Override
    public String getPath() {
      return path;
    }

    @Override
    public boolean hasParam(String key) {
      return additionalParams.containsKey(key) || super.hasParam(key);
    }

    @Override
    protected String readParam(String key) {
      String param = additionalParams.get(key);
      if (param != null) {
        return param;
      }
      return super.readParam(key);
    }
  }

}
