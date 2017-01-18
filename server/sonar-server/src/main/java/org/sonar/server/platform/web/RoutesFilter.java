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
package org.sonar.server.platform.web;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.core.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

public class RoutesFilter implements Filter {

  private static final String EMPTY = "";

  private static final List<Route> ROUTES = ImmutableList.of(
    new BatchRoute(),
    new BatchBootstrapRoute(),
    new ApiSourcesRoute(),
    new ProfilesExportRoute());

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    String path = extractPath(request);
    List<Route> routes = ROUTES.stream()
      .filter(route -> route.test(path))
      .collect(Collectors.toList());
    checkState(routes.isEmpty() || routes.size() == 1, "Multiple routes have been found for '%s'", path);
    if (routes.isEmpty()) {
      chain.doFilter(request, response);
      return;
    }
    response.sendRedirect(routes.get(0).apply(request));
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Nothing
  }

  @Override
  public void destroy() {
    // Nothing
  }

  private interface Route extends Predicate<String>, Function<HttpServletRequest, String> {
    @Override
    boolean test(String path);

    @Override
    String apply(HttpServletRequest request);
  }

  /**
   * Old scanners were using /batch/file.jar url (see SCANNERAPI-167)
   */
  private static class BatchRoute implements Route {

    private static final String BATCH_WS = "/batch";

    @Override
    public boolean test(String path) {
      return path.startsWith(BATCH_WS + "/") && path.endsWith(".jar");
    }

    @Override
    public String apply(HttpServletRequest request) {
      String path = extractPath(request);
      return format("%s%s/file?name=%s", request.getContextPath(), BATCH_WS, path.replace(BATCH_WS + "/", EMPTY));
    }
  }

  /**
   * Old scanners were using /batch_bootstrap url (see SCANNERAPI-167)
   */
  private static class BatchBootstrapRoute implements Route {

    @Override
    public boolean test(String path) {
      return "/batch_bootstrap/index".equals(path);
    }

    @Override
    public String apply(HttpServletRequest request) {
      return format("%s%s/index", request.getContextPath(), "/batch");
    }
  }

  /**
   * SONAR-7852 /api/sources?resource url is still used
   */
  private static class ApiSourcesRoute implements Route {

    private static final String API_SOURCES_WS = "/api/sources";

    @Override
    public boolean test(String path) {
      return API_SOURCES_WS.equals(path);
    }

    @Override
    public String apply(HttpServletRequest request) {
      return format("%s%s/index?%s", request.getContextPath(), API_SOURCES_WS, request.getQueryString());
    }
  }

  /**
   * Old scanners were using /profiles/export url (see SVS-130)
   */
  private static class ProfilesExportRoute implements Route {

    private static final String PROFILES_EXPORT = "/profiles/export";
    private static final String API_QUALITY_PROFILE_EXPORT = "/api/qualityprofiles/export";

    @Override
    public boolean test(String path) {
      return PROFILES_EXPORT.equals(path);
    }

    @Override
    public String apply(HttpServletRequest request) {
      return format("%s%s?%s", request.getContextPath(), API_QUALITY_PROFILE_EXPORT, request.getQueryString());
    }
  }

  private static String extractPath(HttpServletRequest request) {
    return request.getRequestURI().replaceFirst(request.getContextPath(), EMPTY);
  }
}
