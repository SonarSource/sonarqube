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
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.commons.io.IOUtils;
import org.sonar.api.web.ServletFilter;
import org.sonarqube.ws.MediaTypes;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.web.ServletFilter.UrlPattern.Builder.staticResourcePatterns;

/**
 * This filter provide the HTML file that will be used to display every web pages.
 * The same file should be provided for any URLs except WS and static resources.
 */
public class WebPagesFilter extends ServletFilter {

  private static final String CONTEXT_PLACEHOLDER = "%WEB_CONTEXT%";

  private String index;

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern
      .builder()
      .excludes(staticResourcePatterns())
      .excludes("/api/*", "/batch/*")
      .build();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
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
