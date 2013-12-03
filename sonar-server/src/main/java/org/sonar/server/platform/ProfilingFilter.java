/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.platform;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.profiling.Profiling.Level;
import org.sonar.core.profiling.StopWatch;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Set;

/**
 * <p>Profile HTTP requests using platform profiling utility.</p>
 * <p>To avoid profiling of requests for static resources, the <code>staticDirs</code>
 * filter parameter can be set in the servlet context descriptor. This parameter should
 * contain a comma-separated list of paths, starting at the context root;
 * requests on subpaths of these paths will not be profiled.</p>
 * @since 4.1
 */
public class ProfilingFilter implements Filter {

  private static final Logger LOG = LoggerFactory.getLogger(ProfilingFilter.class);

  private static final String CONFIG_SEPARATOR = ",";
  private static final String URL_SEPARATOR = "/";

  private static final String MESSAGE_WITH_QUERY = "%s %s?%s";
  private static final String MESSAGE_WITHOUT_QUERY = "%s %s";

  private String contextRoot;
  private Set<String> staticResourceDirs;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    contextRoot = filterConfig.getServletContext().getContextPath();

    String staticResourcesConfig = filterConfig.getInitParameter("staticDirs");
    if (StringUtils.isNotBlank(staticResourcesConfig)) {
      staticResourceDirs = ImmutableSet.copyOf(staticResourcesConfig.split(CONFIG_SEPARATOR));
    } else {
      staticResourceDirs = ImmutableSet.of();
    }
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (request instanceof HttpServletRequest) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      String requestUri = httpRequest.getRequestURI();
      String rootDir = getRootDir(requestUri);

      if (staticResourceDirs.contains(rootDir)) {
        // Static resource, not profiled
        chain.doFilter(request, response);
      } else {
        StopWatch watch = getProfiling().start("http", Level.BASIC);
        try {
          chain.doFilter(request, response);
        } finally {
          String queryString = httpRequest.getQueryString();
          watch.stop(queryString == null ? MESSAGE_WITHOUT_QUERY : MESSAGE_WITH_QUERY, httpRequest.getMethod(), requestUri, queryString);
        }
      }
    } else {
      // Not an HTTP request, not profiled
      chain.doFilter(request, response);
    }
  }

  private String getRootDir(String requestUri) {
    String rootPath = "";
    String localPath = StringUtils.substringAfter(requestUri, contextRoot);
    if (localPath.startsWith(URL_SEPARATOR)) {
      int secondSlash = localPath.indexOf(URL_SEPARATOR, 1);
      if (secondSlash > 0) {
        rootPath = URL_SEPARATOR + localPath.substring(1, secondSlash);
      }
    }
    return rootPath;
  }

  @Override
  public void destroy() {
    // Nothing
  }

  @VisibleForTesting
  Profiling getProfiling() {
    try {
      return (Profiling) Platform.component(Profiling.class);
    } catch(Exception initException) {
      LOG.error("Could not initialize platform profiling", initException);
      return new Profiling(new Settings());
    }
  }
}
