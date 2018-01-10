/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.ws;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.ServletFilter;
import org.sonar.core.util.stream.MoreCollectors;

import static java.util.stream.Stream.concat;
import static org.sonar.server.property.ws.PropertiesWs.CONTROLLER_PROPERTIES;
import static org.sonar.server.ws.WebServiceReroutingFilter.MOVED_WEB_SERVICES;

/**
 * This filter is used to execute Web Services.
 *
 * Every urls beginning with '/api' and every web service urls are taken into account, except :
 * <ul>
 *   <li>web services that directly implemented with servlet filter, see {@link ServletFilterHandler})</li>
 *   <li>deprecated '/api/properties' web service, see {@link DeprecatedPropertiesWsFilter}</li>
 * </ul>
 */
public class WebServiceFilter extends ServletFilter {

  private final WebServiceEngine webServiceEngine;
  private final Set<String> includeUrls;
  private final Set<String> excludeUrls;

  public WebServiceFilter(WebServiceEngine webServiceEngine) {
    this.webServiceEngine = webServiceEngine;
    this.includeUrls = concat(
      Stream.of("/api/*"),
      webServiceEngine.controllers().stream()
        .flatMap(controller -> controller.actions().stream())
        .map(toPath()))
          .collect(MoreCollectors.toSet());
    this.excludeUrls = concat(concat(
      Stream.of("/" + CONTROLLER_PROPERTIES + "*"),
      MOVED_WEB_SERVICES.stream()),
      webServiceEngine.controllers().stream()
        .flatMap(controller -> controller.actions().stream())
        .filter(action -> action.handler() instanceof ServletFilterHandler)
        .map(toPath()))
          .collect(MoreCollectors.toSet());
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.builder()
      .includes(includeUrls)
      .excludes(excludeUrls)
      .build();
  }

  @Override
  public void doFilter(javax.servlet.ServletRequest servletRequest, javax.servlet.ServletResponse servletResponse, FilterChain chain) {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    ServletRequest wsRequest = new ServletRequest(request);
    ServletResponse wsResponse = new ServletResponse(response);
    webServiceEngine.execute(wsRequest, wsResponse);
  }

  @Override
  public void init(FilterConfig filterConfig) {
    // Nothing to do
  }

  @Override
  public void destroy() {
    // Nothing to do
  }

  private static Function<WebService.Action, String> toPath() {
    return action -> "/" + action.path() + "/*";
  }

}
