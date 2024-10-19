/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import ch.qos.logback.classic.ClassicConstants;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import javax.annotation.Nullable;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.sonar.db.DBSessions;
import org.sonar.server.authentication.UserSessionInitializer;
import org.sonar.server.http.JavaxHttpRequest;
import org.sonar.server.http.JavaxHttpResponse;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.PlatformImpl;
import org.sonar.server.setting.ThreadLocalSettings;

public class UserSessionFilter implements Filter {
  private static final Logger LOG = LoggerFactory.getLogger(UserSessionFilter.class);
  private final Platform platform;

  public UserSessionFilter() {
    this.platform = PlatformImpl.getInstance();
  }

  @VisibleForTesting
  UserSessionFilter(Platform platform) {
    this.platform = platform;
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;

    //Fix Me write new filter
    insertIntoMDC(servletRequest);

    DBSessions dbSessions = platform.getContainer().getComponentByType(DBSessions.class);
    ThreadLocalSettings settings = platform.getContainer().getComponentByType(ThreadLocalSettings.class);
    UserSessionInitializer userSessionInitializer = platform.getContainer().getOptionalComponentByType(UserSessionInitializer.class).orElse(null);

    LOG.trace("{} serves {}", Thread.currentThread(), request.getRequestURI());
    dbSessions.enableCaching();
    try {
      settings.load();
      try {
        doFilter(request, response, chain, userSessionInitializer);
      } finally {
        settings.unload();
        this.clearMDC();
      }
    } finally {
      dbSessions.disableCaching();
    }
  }

  private static void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
    @Nullable UserSessionInitializer userSessionInitializer) throws IOException, ServletException {
    try {
      if (userSessionInitializer == null || userSessionInitializer.initUserSession(new JavaxHttpRequest(request), new JavaxHttpResponse(response))) {
        chain.doFilter(request, response);
      }
    } finally {
      if (userSessionInitializer != null) {
        userSessionInitializer.removeUserSession();
      }
    }
  }

  @Override
  public void init(FilterConfig filterConfig) {
    // nothing to do
  }

  @Override
  public void destroy() {
    // nothing to do
  }

  private void insertIntoMDC(ServletRequest request) {
    MDC.put(ClassicConstants.REQUEST_REMOTE_HOST_MDC_KEY, request.getRemoteHost());

    if (request instanceof HttpServletRequest) {
      HttpServletRequest httpServletRequest = (HttpServletRequest) request;
      MDC.put(ClassicConstants.REQUEST_REQUEST_URI, httpServletRequest.getRequestURI());
      StringBuffer requestURL = httpServletRequest.getRequestURL();
      if (requestURL != null) {
        MDC.put(ClassicConstants.REQUEST_REQUEST_URL, requestURL.toString());
      }
      MDC.put(ClassicConstants.REQUEST_METHOD, httpServletRequest.getMethod());
      MDC.put(ClassicConstants.REQUEST_QUERY_STRING, httpServletRequest.getQueryString());
      MDC.put(ClassicConstants.REQUEST_USER_AGENT_MDC_KEY, httpServletRequest.getHeader("User-Agent"));
      MDC.put(ClassicConstants.REQUEST_X_FORWARDED_FOR, httpServletRequest.getHeader("X-Forwarded-For"));

    }
  }

  void clearMDC() {
    MDC.remove(ClassicConstants.REQUEST_REMOTE_HOST_MDC_KEY);
    MDC.remove(ClassicConstants.REQUEST_REQUEST_URI);
    MDC.remove(ClassicConstants.REQUEST_QUERY_STRING);
    // removing possibly inexistent item is OK
    MDC.remove(ClassicConstants.REQUEST_REQUEST_URL);
    MDC.remove(ClassicConstants.REQUEST_METHOD);
    MDC.remove(ClassicConstants.REQUEST_USER_AGENT_MDC_KEY);
    MDC.remove(ClassicConstants.REQUEST_X_FORWARDED_FOR);
  }
}
