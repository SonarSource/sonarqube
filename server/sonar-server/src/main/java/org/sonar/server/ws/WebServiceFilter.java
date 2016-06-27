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

import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
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
  private final Map<String, WsUrl> wsUrls = new HashMap<>();
  private final List<String> includeUrls = new ArrayList<>();

  public WebServiceFilter(WebServiceEngine webServiceEngine) {
    this.webServiceEngine = webServiceEngine;
    webServiceEngine.controllers().stream()
      .forEach(controller -> controller.actions().stream()
        .filter(action -> !(action.handler() instanceof RailsHandler) && !(action.handler() instanceof ServletFilterHandler))
        .forEach(action -> {
          String url = "/" + action.path();
          wsUrls.put(url, new WsUrl(controller.path(), action.key()));
          includeUrls.add(url + "*");
        }));
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.builder()
      .includes(includeUrls)
      .build();
  }

  @Override
  public void doFilter(javax.servlet.ServletRequest servletRequest, javax.servlet.ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    String path = request.getRequestURI().replaceFirst(request.getContextPath(), "");

    String[] pathWithExtension = getPathWithExtension(path);
    WsUrl url = wsUrls.get(pathWithExtension[0]);
    if (url == null) {
      throw new IllegalStateException(format("Unknown path : %s", path));
    }
    ServletRequest wsRequest = new ServletRequest(request);
    ServletResponse wsResponse = new ServletResponse();
    webServiceEngine.execute(wsRequest, wsResponse, url.getController(), url.getAction(), pathWithExtension[1]);
    writeResponse(wsResponse, response);
  }

  private static void writeResponse(ServletResponse wsResponse, HttpServletResponse response) throws IOException {
    // SONAR-6964 WS should not be cached by browser
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    for (String header : wsResponse.getHeaderNames()) {
      response.setHeader(header, wsResponse.getHeader(header));
    }

    response.setContentType(wsResponse.stream().mediaType());
    response.setStatus(wsResponse.stream().httpStatus());

    OutputStream responseOutput = response.getOutputStream();
    ByteArrayOutputStream wsOutputStream = (ByteArrayOutputStream) wsResponse.stream().output();
    IOUtils.write(wsOutputStream.toByteArray(), responseOutput);
    responseOutput.flush();
    responseOutput.close();
  }

  private static String[] getPathWithExtension(String fullPath) {
    String path = fullPath;
    String extension = null;
    int semiColonPos = fullPath.lastIndexOf('.');
    if (semiColonPos > 0) {
      path = fullPath.substring(0, semiColonPos);
      extension = fullPath.substring(semiColonPos + 1);
    }
    return new String[] {path, extension};
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
