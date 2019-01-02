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
package org.sonar.server.platform.web.requestid;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.sonar.server.platform.Platform;

/**
 * A {@link Filter} that puts and removes the HTTP request ID from the {@link org.slf4j.MDC}.
 */
public class RequestIdFilter implements Filter {

  private final Platform platform;

  public RequestIdFilter() {
    this(Platform.getInstance());
  }

  @VisibleForTesting
  RequestIdFilter(Platform platform) {
    this.platform = platform;
  }

  @Override
  public void init(FilterConfig filterConfig) {
    // nothing to do
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    RequestIdGenerator requestIdGenerator = platform.getContainer().getComponentByType(RequestIdGenerator.class);

    if (requestIdGenerator == null) {
      chain.doFilter(request, response);
    } else {
      String requestId = requestIdGenerator.generate();
      try (RequestIdMDCStorage mdcStorage = new RequestIdMDCStorage(requestId)) {
        request.setAttribute("ID", requestId);
        chain.doFilter(request, response);
      }
    }
  }

  @Override
  public void destroy() {
    // nothing to do
  }

}
