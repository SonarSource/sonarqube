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
package org.sonar.server.platform.web;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.sonar.api.web.ServletFilter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.IOUtils.write;
import static org.sonar.api.web.ServletFilter.UrlPattern.Builder.staticResourcePatterns;
import static org.sonarqube.ws.MediaTypes.HTML;

/**
 * This filter provide the HTML file that will be used to display every web pages.
 * The same file should be provided for any URLs except WS and static resources.
 */
public class WebPagesFilter implements Filter {

  private static final String CACHE_CONTROL_HEADER = "Cache-Control";
  private static final String CACHE_CONTROL_VALUE = "no-cache, no-store, must-revalidate";
  private static final String WEB_CONTEXT_PLACEHOLDER = "%WEB_CONTEXT%";
  private static final String DEFAULT_HTML_PATH = "/index.html";
  // all the html files to be loaded from disk
  private static final Set<String> HTML_PATHS = ImmutableSet.of(DEFAULT_HTML_PATH, "/integration/vsts/index.html");
  private static final Map<String, String> HTML_CONTENTS_BY_PATH = new HashMap<>();

  private static final ServletFilter.UrlPattern URL_PATTERN = ServletFilter.UrlPattern
    .builder()
    .excludes(staticResourcePatterns())
    .build();

  @Override
  public void init(FilterConfig filterConfig) {
    HTML_PATHS.forEach(path -> {
      ServletContext servletContext = filterConfig.getServletContext();
      HTML_CONTENTS_BY_PATH.put(path, loadHtmlFile(servletContext, path));
    });
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;
    String path = httpServletRequest.getRequestURI().replaceFirst(httpServletRequest.getContextPath(), "");
    if (!URL_PATTERN.matches(path)) {
      chain.doFilter(request, response);
      return;
    }
    httpServletResponse.setContentType(HTML);
    httpServletResponse.setCharacterEncoding(UTF_8.name().toLowerCase(ENGLISH));
    httpServletResponse.setHeader(CACHE_CONTROL_HEADER, CACHE_CONTROL_VALUE);
    String htmlPath = HTML_PATHS.contains(path) ? path : DEFAULT_HTML_PATH;
    String htmlContent = requireNonNull(HTML_CONTENTS_BY_PATH.get(htmlPath));
    write(htmlContent, httpServletResponse.getOutputStream(), UTF_8);
  }

  private static String loadHtmlFile(ServletContext context, String path) {
    try (InputStream input = context.getResourceAsStream(path)) {
      String template = IOUtils.toString(requireNonNull(input), UTF_8);
      return template.replaceAll(WEB_CONTEXT_PLACEHOLDER, context.getContextPath());
    } catch (Exception e) {
      throw new IllegalStateException("Fail to load file " + path, e);
    }
  }

  @Override
  public void destroy() {
    // Nothing to do
  }
}
