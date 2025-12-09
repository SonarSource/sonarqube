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
import com.google.common.base.Preconditions;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.web.HttpFilter;
import org.sonar.server.http.JakartaHttpRequest;
import org.sonar.server.http.JakartaHttpResponse;
import org.sonar.server.platform.PlatformImpl;

/**
 * Inspired by http://stackoverflow.com/a/7592883/229031
 */
public class MasterServletFilter implements Filter {

  private static final String SCIM_FILTER_PATH = "/api/scim/v2/";
  private static final Logger LOG = LoggerFactory.getLogger(MasterServletFilter.class);
  private static volatile MasterServletFilter instance;

  private HttpFilter[] httpFilters;

  public MasterServletFilter() {
    if (instance != null) {
      throw new IllegalStateException("Servlet filter " + getClass().getName() + " is already instantiated");
    }
    instance = this;
  }

  @Override
  public void init(FilterConfig config) {
    // Filters are already available in the container unless a database migration is required. See
    // org.sonar.server.startup.RegisterServletFilters.
    List<HttpFilter> httpFilterList = PlatformImpl.getInstance().getContainer().getComponentsByType(HttpFilter.class);
    init(httpFilterList);
  }

  @CheckForNull
  public static MasterServletFilter getInstance() {
    return instance;
  }

  @VisibleForTesting
  static void setInstance(@Nullable MasterServletFilter instance) {
    MasterServletFilter.instance = instance;
  }

  void init(List<HttpFilter> httpFilters) {
    initHttpFilters(httpFilters);
  }

  public void initHttpFilters(List<HttpFilter> filterExtensions) {
    LinkedList<HttpFilter> filterList = new LinkedList<>();
    for (HttpFilter extension : filterExtensions) {
      try {
        LOG.atInfo()
          .addArgument(extension)
          .addArgument(() -> extension.doGetPattern().label())
          .log("Initializing servlet filter {} [pattern={}]");
        extension.init();
        // As for scim we need to intercept traffic to URLs with path parameters
        // and that use is not properly handled when dealing with inclusions/exclusions of the WebServiceFilter,
        // we need to make sure the Scim filters are invoked before the WebserviceFilter
        if (isScimFilter(extension)) {
          filterList.addFirst(extension);
        } else {
          filterList.addLast(extension);
        }
      } catch (Exception e) {
        throw new IllegalStateException("Fail to initialize servlet filter: " + extension + ". Message: " + e.getMessage(), e);
      }
    }
    httpFilters = filterList.toArray(new HttpFilter[0]);
  }

  private static boolean isScimFilter(HttpFilter extension) {
    return extension.doGetPattern().getInclusions().stream()
      .anyMatch(s -> s.startsWith(SCIM_FILTER_PATH));
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
    HttpServletRequest hsr = (HttpServletRequest) request;
    if (httpFilters.length == 0) {
      chain.doFilter(hsr, response);
    } else {
      String path = hsr.getRequestURI().replaceFirst(hsr.getContextPath(), "");
      GodFilterChain godChain = new GodFilterChain(chain);
      buildGodchain(path, godChain);
      godChain.doFilter(hsr, response);
    }
  }

  private void buildGodchain(String path, GodFilterChain godChain) {
    Arrays.stream(httpFilters)
      .filter(filter -> filter.doGetPattern().matches(path))
      .forEachOrdered(godChain::addFilter);
  }

  @Override
  public void destroy() {
    for (HttpFilter filter : httpFilters) {
      filter.destroy();
    }
  }

  @VisibleForTesting
  HttpFilter[] getHttpFilters() {
    return httpFilters;
  }

  private static final class GodFilterChain implements FilterChain {
    private final FilterChain chain;
    private final List<Filter> filters = new LinkedList<>();
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
        Filter next = iterator.next();
        next.doFilter(request, response, this);
      } else {
        chain.doFilter(request, response);
      }
    }

    @Deprecated(forRemoval = true)
    public void addFilter(Filter filter) {
      Preconditions.checkState(iterator == null);
      filters.add(filter);
    }

    public void addFilter(HttpFilter filter) {
      Preconditions.checkState(iterator == null);
      filters.add(new JavaxFilterAdapter(filter));
    }
  }

  private static class JavaxFilterAdapter implements Filter {
    private final HttpFilter httpFilter;

    JavaxFilterAdapter(HttpFilter httpFilter) {
      this.httpFilter = httpFilter;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
      HttpRequest javaxHttpRequest = new JakartaHttpRequest((HttpServletRequest) servletRequest);
      HttpResponse javaxHttpResponse = new JakartaHttpResponse((HttpServletResponse) servletResponse);
      httpFilter.doFilter(javaxHttpRequest, javaxHttpResponse, new HttpFilterChainAdapter(chain));
    }
  }

  private static class HttpFilterChainAdapter implements org.sonar.api.web.FilterChain {
    private final FilterChain filterChain;

    HttpFilterChainAdapter(FilterChain filterChain) {
      this.filterChain = filterChain;
    }

    @Override
    public void doFilter(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
      try {
        filterChain.doFilter(((JakartaHttpRequest) httpRequest).getDelegate(), ((JakartaHttpResponse) httpResponse).getDelegate());
      } catch (ServletException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
