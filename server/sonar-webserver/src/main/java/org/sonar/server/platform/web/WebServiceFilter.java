/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.FilterChain;
import org.sonar.api.web.HttpFilter;
import org.sonar.api.web.UrlPattern;
import org.sonar.server.ws.ServletFilterHandler;
import org.sonar.server.ws.ServletRequest;
import org.sonar.server.ws.ServletResponse;
import org.sonar.server.ws.WebServiceEngine;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Stream.concat;
import static org.sonar.server.platform.web.WebServiceReroutingFilter.MOVED_WEB_SERVICES;

/**
 * This filter is used to execute Web Services.
 *
 * Every urls beginning with '/api' and every web service urls are taken into account, except :
 * <ul>
 *   <li>web services that directly implemented with servlet filter, see {@link ServletFilterHandler})</li>
 * </ul>
 */
public class WebServiceFilter extends HttpFilter {

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
          .collect(Collectors.toSet());
    this.excludeUrls = concat(MOVED_WEB_SERVICES.stream(),
      webServiceEngine.controllers().stream()
        .flatMap(controller -> controller.actions().stream())
        .filter(action -> action.handler() instanceof ServletFilterHandler)
        .map(toPath())).collect(toCollection(HashSet::new));
    excludeUrls.add("/api/v2/*");
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.builder()
      .includes(includeUrls)
      .excludes(excludeUrls)
      .build();
  }

  @Override
  public void doFilter(HttpRequest request, HttpResponse response, FilterChain filterChain) {
    ServletRequest wsRequest = new ServletRequest(request);
    ServletResponse wsResponse = new ServletResponse(response);
    webServiceEngine.execute(wsRequest, wsResponse);
  }

  private static Function<WebService.Action, String> toPath() {
    return action -> "/" + action.path() + ".*";
  }

}
