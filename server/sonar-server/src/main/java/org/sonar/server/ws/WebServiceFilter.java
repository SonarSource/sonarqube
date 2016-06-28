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
import java.util.ArrayList;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.web.ServletFilter;

/**
 * This filter is used to execute Java WS.
 *
 * If the url match a Java WS, the output of the WS is returned and no other filers are executed.
 * If the url doesn't match a Java WS, then it's calling remaining filters (for instance to execute Rails WS).
 */
public class WebServiceFilter extends ServletFilter {

  private final WebServiceEngine webServiceEngine;
  private final List<String> includeUrls = new ArrayList<>();
  private final List<String> excludeUrls = new ArrayList<>();

  public WebServiceFilter(WebServiceEngine webServiceEngine) {
    this.webServiceEngine = webServiceEngine;
    webServiceEngine.controllers().stream()
      .forEach(controller -> controller.actions().stream()
        .forEach(action -> {
          // Rails and servlet filter WS should not be executed by the web service engine
          if (!(action.handler() instanceof RailsHandler) && !(action.handler() instanceof ServletFilterHandler)) {
            includeUrls.add("/" + controller.path() + "/*");
          } else {
            excludeUrls.add("/" + action.path() + "*");
          }
        }));
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.builder()
      .includes(includeUrls)
      .excludes(excludeUrls)
      .build();
  }

  @Override
  public void doFilter(javax.servlet.ServletRequest servletRequest, javax.servlet.ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    ServletRequest wsRequest = new ServletRequest(request);
    ServletResponse wsResponse = new ServletResponse(response);
    webServiceEngine.execute(wsRequest, wsResponse);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Nothing to do
  }

  @Override
  public void destroy() {
    // Nothing to do
  }

  private static class WsUrl {
    private final String controller;
    private final String action;

    WsUrl(String controller, String action) {
      this.controller = controller;
      this.action = action;
    }

    String getController() {
      return controller;
    }

    String getAction() {
      return action;
    }
  }
}
