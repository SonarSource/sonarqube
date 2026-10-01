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

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.MappingMatch;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.LocalConnector;

import static java.util.Objects.requireNonNullElse;
import static org.apache.commons.lang3.StringUtils.removeStart;

/**
 * Minimal {@link HttpServletRequest} built from a {@link LocalConnector.LocalRequest}, so that a local call can be served
 * by the Web API v2 servlet. It has no body, no session and no cookies.
 */
class LocalHttpServletRequest extends AbstractLocalHttpServletRequest {

  static final String API_V2_SERVLET_PATH = "/api/v2";
  private static final String API_V2_SERVLET_NAME = "app";
  private static final String ACCEPT_HEADER = "Accept";

  private final LocalConnector.LocalRequest localRequest;
  private final ServletContext servletContext;
  private final String contextPath;
  private final String pathInfo;
  private final Map<String, String[]> parameters;
  private final Map<String, Object> attributes = new HashMap<>();

  LocalHttpServletRequest(LocalConnector.LocalRequest localRequest, ServletContext servletContext) {
    this.localRequest = localRequest;
    this.servletContext = servletContext;
    this.contextPath = requireNonNullElse(servletContext.getContextPath(), "");
    String path = '/' + removeStart(localRequest.getPath(), '/');
    this.pathInfo = stripTrailingSlash(path.substring(API_V2_SERVLET_PATH.length()));
    this.parameters = Collections.unmodifiableMap(new LinkedHashMap<>(requireNonNullElse(localRequest.getParameterMap(), Map.of())));
  }

  private static String stripTrailingSlash(String path) {
    return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
  }

  @Override
  public String getMethod() {
    return localRequest.getMethod();
  }

  @Override
  public ServletContext getServletContext() {
    return servletContext;
  }

  @Override
  public String getContextPath() {
    return contextPath;
  }

  @Override
  public String getServletPath() {
    return API_V2_SERVLET_PATH;
  }

  @Override
  public String getPathInfo() {
    return pathInfo;
  }

  @Override
  public String getRequestURI() {
    return contextPath + API_V2_SERVLET_PATH + pathInfo;
  }

  @Override
  public StringBuffer getRequestURL() {
    return new StringBuffer(getScheme()).append("://").append(LOCALHOST).append(getRequestURI());
  }

  @Override
  public HttpServletMapping getHttpServletMapping() {
    return new HttpServletMapping() {
      @Override
      public String getMatchValue() {
        return pathInfo.substring(1);
      }

      @Override
      public String getPattern() {
        return API_V2_SERVLET_PATH + "/*";
      }

      @Override
      public String getServletName() {
        return API_V2_SERVLET_NAME;
      }

      @Override
      public MappingMatch getMappingMatch() {
        return MappingMatch.PATH;
      }
    };
  }

  @CheckForNull
  @Override
  public String getQueryString() {
    if (parameters.isEmpty()) {
      return null;
    }
    List<String> pairs = new ArrayList<>();
    parameters.forEach((key, values) -> {
      for (String value : values) {
        pairs.add(URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
      }
    });
    return String.join("&", pairs);
  }

  @CheckForNull
  @Override
  public String getParameter(String name) {
    String[] values = parameters.get(name);
    return values == null || values.length == 0 ? null : values[0];
  }

  @Override
  public Enumeration<String> getParameterNames() {
    return Collections.enumeration(parameters.keySet());
  }

  @CheckForNull
  @Override
  public String[] getParameterValues(String name) {
    return parameters.get(name);
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    return parameters;
  }

  @CheckForNull
  @Override
  public String getHeader(String name) {
    if (ACCEPT_HEADER.equalsIgnoreCase(name)) {
      return localRequest.getHeader(ACCEPT_HEADER).orElse(localRequest.getMediaType());
    }
    return localRequest.getHeader(name).orElse(null);
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    String value = getHeader(name);
    return value == null ? Collections.emptyEnumeration() : Collections.enumeration(List.of(value));
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    return getHeader(ACCEPT_HEADER) == null ? Collections.emptyEnumeration() : Collections.enumeration(List.of(ACCEPT_HEADER));
  }

  @Override
  public long getDateHeader(String name) {
    return -1L;
  }

  @Override
  public int getIntHeader(String name) {
    String value = getHeader(name);
    return value == null ? -1 : Integer.parseInt(value);
  }

  @CheckForNull
  @Override
  public Object getAttribute(String name) {
    return attributes.get(name);
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return Collections.enumeration(new ArrayList<>(attributes.keySet()));
  }

  @Override
  public void setAttribute(String name, @Nullable Object o) {
    if (o == null) {
      attributes.remove(name);
    } else {
      attributes.put(name, o);
    }
  }

  @Override
  public void removeAttribute(String name) {
    attributes.remove(name);
  }
}
