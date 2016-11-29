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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.sonar.api.web.ServletFilter;
import org.sonarqube.ws.MediaTypes;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.web.ServletFilter.UrlPattern.Builder.staticResourcePatterns;

/**
 * This filter provide the HTML file that will be used to display every web pages.
 * The same file should be provided for any URLs except WS and static resources.
 */
public class WebPagesFilter implements Filter {

  private static final String CONTEXT_PLACEHOLDER = "%WEB_CONTEXT%";

  private static final ServletFilter.UrlPattern URL_PATTERN = ServletFilter.UrlPattern
    .builder()
    // This exclusion won't be needed anymore when static resources will be handled in a dedicated filter, executed before this one
    .excludes(staticResourcePatterns())
    // These exclusions won't be needed anymore when this filter will be last filter (no more rails filter)
    .excludes("/api/*", "/batch/*")
    .build();

  private String index;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    String path = httpServletRequest.getRequestURI().replaceFirst(httpServletRequest.getContextPath(), "");
    if (!URL_PATTERN.matches(path)) {
      chain.doFilter(request, response);
      return;
    }

    response.setContentType(MediaTypes.HTML);
    IOUtils.write(index, response.getOutputStream());
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    String context = filterConfig.getServletContext().getContextPath();
    String indexFile = readIndexFile(filterConfig.getServletContext());
    this.index = indexFile.replaceAll(CONTEXT_PLACEHOLDER, context);
  }

  private String readIndexFile(ServletContext servletContext) {
    try {
      return IOUtils.toString(requireNonNull(servletContext.getResource("/index.html")), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to provide index file", e);
    }
  }

  @Override
  public void destroy() {
    // Nothing to do
  }
}
