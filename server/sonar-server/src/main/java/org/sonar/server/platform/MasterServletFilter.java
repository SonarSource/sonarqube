/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.web.ServletFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Inspired by http://stackoverflow.com/a/7592883/229031
 */
public class MasterServletFilter implements Filter {

  public static volatile MasterServletFilter INSTANCE;
  private ServletFilter[] filters;
  private FilterConfig config;

  public MasterServletFilter() {
    if (INSTANCE != null) {
      throw new IllegalStateException("Servlet filter " + getClass().getName() + " is already instantiated");
    }
    INSTANCE = this;
  }

  @Override
  public void init(FilterConfig config) throws ServletException {
    // Filters are already available in picocontainer unless a database migration is required. See org.sonar.server.startup.RegisterServletFilters.
    init(config, Platform.getInstance().getContainer().getComponentsByType(ServletFilter.class));
  }

  void init(FilterConfig config, List<ServletFilter> filters) throws ServletException {
    this.config = config;
    initFilters(filters);
  }

  public void initFilters(List<ServletFilter> filterExtensions) throws ServletException {
    List<Filter> filterList = Lists.newArrayList();
    for (ServletFilter extension : filterExtensions) {
      try {
        Loggers.get(MasterServletFilter.class).info(String.format("Initializing servlet filter %s [pattern=%s]", extension, extension.doGetPattern()));
        extension.init(config);
        filterList.add(extension);
      } catch (Exception e) {
        throw new IllegalStateException("Fail to initialize servlet filter: " + extension + ". Message: " + e.getMessage(), e);
      }
    }
    filters = filterList.toArray(new ServletFilter[filterList.size()]);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
    HttpServletRequest hsr = (HttpServletRequest) request;
    if (filters.length == 0) {
      chain.doFilter(request, response);
    } else {
      String path = hsr.getRequestURI().replaceFirst(hsr.getContextPath(), "");
      GodFilterChain godChain = new GodFilterChain(chain);

      for (ServletFilter filter : filters) {
        if (filter.doGetPattern().matches(path)) {
          godChain.addFilter(filter);
        }
      }
      godChain.doFilter(request, response);
    }
  }

  @Override
  public void destroy() {
    for (ServletFilter filter : filters) {
      filter.destroy();
    }
  }

  @VisibleForTesting
  ServletFilter[] getFilters() {
    return filters;
  }

  private static final class GodFilterChain implements FilterChain {
    private FilterChain chain;
    private List<Filter> filters = Lists.newLinkedList();
    private Iterator<Filter> iterator;

    public GodFilterChain(FilterChain chain) {
      this.chain = chain;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
      if (iterator == null) {
        iterator = filters.iterator();
      }
      if (iterator.hasNext()) {
        iterator.next().doFilter(request, response, this);
      } else {
        chain.doFilter(request, response);
      }
    }

    public void addFilter(Filter filter) {
      Preconditions.checkState(iterator == null);
      filters.add(filter);
    }
  }
}
