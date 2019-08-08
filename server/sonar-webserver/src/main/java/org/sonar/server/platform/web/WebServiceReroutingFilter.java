/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.web.ServletFilter;
import org.sonar.server.ws.ServletRequest;
import org.sonar.server.ws.ServletResponse;
import org.sonar.server.ws.WebServiceEngine;

/**
 * This filter is used to execute renamed/moved web services
 */
public class WebServiceReroutingFilter extends ServletFilter {

  private static final Map<String, String> REDIRECTS = ImmutableMap.<String, String>builder()
    .put("/api/components/bulk_update_key", "/api/projects/bulk_update_key")
    .put("/api/components/update_key", "/api/projects/update_key")
    .build();
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
  public void doFilter(javax.servlet.ServletRequest servletRequest, javax.servlet.ServletResponse servletResponse, FilterChain chain) {
    HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
    RedirectionRequest wsRequest = new RedirectionRequest(httpRequest);
    ServletResponse wsResponse = new ServletResponse((HttpServletResponse) servletResponse);

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

  private static class RedirectionRequest extends ServletRequest {
    private final String redirectedPath;

    public RedirectionRequest(HttpServletRequest source) {
      super(source);
      this.redirectedPath = REDIRECTS.getOrDefault(source.getServletPath(), source.getServletPath());
    }

    @Override
    public String getPath() {
      return redirectedPath;
    }
  }
}
