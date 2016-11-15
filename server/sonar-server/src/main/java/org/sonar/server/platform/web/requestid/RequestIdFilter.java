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
package org.sonar.server.platform.web.requestid;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.slf4j.MDC;
import org.sonar.server.platform.Platform;

import static java.util.Objects.requireNonNull;

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
  public void init(FilterConfig filterConfig) throws ServletException {
    // nothing to do
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    RequestIdGenerator requestIdGenerator = platform.getContainer().getComponentByType(RequestIdGenerator.class);

    try (RequestIdMDCStorage mdcStorage = new RequestIdMDCStorage(requestIdGenerator.generate())) {
      chain.doFilter(request, response);
    }
  }

  @Override
  public void destroy() {
    // nothing to do
  }

  /**
   * Wraps MDC calls to store the HTTP request ID in the {@link MDC} into an {@link AutoCloseable}.
   */
  static class RequestIdMDCStorage implements AutoCloseable {
    private static final String HTTP_REQUEST_ID_MDC_KEY = "HTTP_REQUEST_ID";

    public RequestIdMDCStorage(String requestId) {
      MDC.put(HTTP_REQUEST_ID_MDC_KEY, requireNonNull(requestId, "Request ID can't be null"));
    }

    @Override
    public void close() {
      MDC.remove(HTTP_REQUEST_ID_MDC_KEY);
    }
  }
}
