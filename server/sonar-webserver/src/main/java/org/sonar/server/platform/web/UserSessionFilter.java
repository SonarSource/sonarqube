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

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import javax.annotation.Nullable;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.db.DBSessions;
import org.sonar.server.authentication.UserSessionInitializer;
import org.sonar.server.http.JakartaHttpRequest;
import org.sonar.server.http.JakartaHttpResponse;
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
      }
    } finally {
      dbSessions.disableCaching();
    }
  }

  private static void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
    @Nullable UserSessionInitializer userSessionInitializer) throws IOException, ServletException {
    try {
      if (userSessionInitializer == null || userSessionInitializer.initUserSession(new JakartaHttpRequest(request), new JakartaHttpResponse(response))) {
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
}
