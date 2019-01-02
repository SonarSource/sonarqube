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

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;

/**
 * <p>Profile HTTP requests using platform profiling utility.</p>
 * <p>To avoid profiling of requests for static resources, the <code>staticDirs</code>
 * filter parameter can be set in the servlet context descriptor. This parameter should
 * contain a comma-separated list of paths, starting at the context root;
 * requests on subpaths of these paths will not be profiled.</p>
 *
 * @since 4.1
 */
public class RootFilter implements Filter {

  private static final org.sonar.api.utils.log.Logger LOGGER = Loggers.get(RootFilter.class);

  @Override
  public void init(FilterConfig filterConfig) {
    // nothing to do
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (request instanceof HttpServletRequest) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      try {
        chain.doFilter(new ServletRequestWrapper(httpRequest), httpResponse);
      } catch (Throwable e) {
        if (httpResponse.isCommitted()) {
          // Request has been aborted by the client, nothing can been done as Tomcat has committed the response
          LOGGER.debug(format("Processing of request %s failed", toUrl(httpRequest)), e);
          return;
        }
        LOGGER.error(format("Processing of request %s failed", toUrl(httpRequest)), e);
        httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    } else {
      // Not an HTTP request, not profiled
      chain.doFilter(request, response);
    }
  }

  private static String toUrl(HttpServletRequest request) {
    String requestURI = request.getRequestURI();
    String queryString = request.getQueryString();
    if (queryString == null) {
      return requestURI;
    }
    return requestURI + '?' + queryString;
  }

  @Override
  public void destroy() {
    // Nothing
  }

  @VisibleForTesting
  static class ServletRequestWrapper extends HttpServletRequestWrapper {

    ServletRequestWrapper(HttpServletRequest request) {
      super(request);
    }

    @Override
    public HttpSession getSession(boolean create) {
      if (!create) {
        return null;
      }
      throw notSupported();
    }

    @Override
    public HttpSession getSession() {
      throw notSupported();
    }

    private static UnsupportedOperationException notSupported() {
      return new UnsupportedOperationException("Sessions are disabled so that web server is stateless");
    }
  }
}
