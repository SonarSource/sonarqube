/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.springframework.web.context.WebApplicationContext;

/**
 * A conditional wrapper for Spring Security's filter chain.
 * This filter only delegates to Spring Security when the application context has been
 * fully initialized (level 4). During safe mode (DB migration), it acts as a pass-through.
 */
public class ConditionalSpringSecurityFilter implements Filter {

  private final String targetBeanName;
  private final String contextAttribute;
  private ServletContext servletContext;
  private final AtomicReference<Filter> delegateFilter = new AtomicReference<>();

  public ConditionalSpringSecurityFilter(String targetBeanName, String contextAttribute) {
    this.targetBeanName = targetBeanName;
    this.contextAttribute = contextAttribute;
  }

  @Override
  public void init(FilterConfig filterConfig) {
    this.servletContext = filterConfig.getServletContext();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {

    Filter delegate = delegateFilter.get();
    if (delegate == null) {
      Filter newDelegate = initializeDelegate();
      if (newDelegate != null) {
        delegateFilter.compareAndSet(null, newDelegate);
        delegate = delegateFilter.get();
      }
    }

    if (delegate != null) {
      // Level 4 is active - delegate to Spring Security
      delegate.doFilter(request, response, chain);
    } else {
      // Safe mode - pass through without Spring Security
      chain.doFilter(request, response);
    }
  }

  @Nullable
  private Filter initializeDelegate() {
    try {
      WebApplicationContext context = (WebApplicationContext) servletContext.getAttribute(contextAttribute);
      if (context != null && context.containsBean(targetBeanName)) {
        return context.getBean(targetBeanName, Filter.class);
      }
    } catch (Exception e) {
      return null;
    }

    return null;
  }

  @Override
  public void destroy() {
    // No cleanup needed
  }
}
