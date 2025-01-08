/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Map;
import java.util.Set;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.web.FilterChain;
import org.sonar.api.web.HttpFilter;
import org.sonar.api.web.UrlPattern;
import org.sonar.server.ws.ServletRequest;
import org.sonar.server.ws.ServletResponse;
import org.sonar.server.ws.WebServiceEngine;

/**
 * This filter is used to execute renamed/moved web services
 */
public class WebServiceReroutingFilter extends HttpFilter {

  private static final Map<String, String> REDIRECTS = Map.of(
    "/api/components/bulk_update_key", "/api/projects/bulk_update_key",
    "/api/components/update_key", "/api/projects/update_key");
  static final Set<String> MOVED_WEB_SERVICES = REDIRECTS.keySet();

  private final WebServiceEngine webServiceEngine;

  public WebServiceReroutingFilter(WebServiceEngine webServiceEngine) {
    this.webServiceEngine = webServiceEngine;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.builder()
      .includes(MOVED_WEB_SERVICES)
      .build();
  }

  @Override
  public void doFilter(HttpRequest request, HttpResponse response, FilterChain filterChain) {
    RedirectionRequest wsRequest = new RedirectionRequest(request);
    ServletResponse wsResponse = new ServletResponse(response);

    webServiceEngine.execute(wsRequest, wsResponse);
  }

  @Override
  public void init() {
    // Nothing to do
  }

  @Override
  public void destroy() {
    // Nothing to do
  }

  private static class RedirectionRequest extends ServletRequest {
    private final String redirectedPath;

    public RedirectionRequest(HttpRequest source) {
      super(source);
      this.redirectedPath = REDIRECTS.getOrDefault(source.getServletPath(), source.getServletPath());
    }

    @Override
    public String getPath() {
      return redirectedPath;
    }
  }
}
