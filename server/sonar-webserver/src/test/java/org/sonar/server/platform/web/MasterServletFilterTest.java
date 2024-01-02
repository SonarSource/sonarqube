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

import java.io.IOException;
import java.util.Collections;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.web.ServletFilter;
import org.sonar.api.web.ServletFilter.UrlPattern;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MasterServletFilterTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Before
  public void resetSingleton() {
    MasterServletFilter.setInstance(null);
  }

  @Test
  public void should_init_and_destroy_filters() throws Exception {
    ServletFilter filter = createMockFilter();
    FilterConfig config = mock(FilterConfig.class);
    MasterServletFilter master = new MasterServletFilter();
    master.init(config, singletonList(filter));

    assertThat(master.getFilters()).containsOnly(filter);
    verify(filter).init(config);

    master.destroy();
    verify(filter).destroy();
  }

  @Test
  public void servlet_container_should_instantiate_only_a_single_master_instance() {
    new MasterServletFilter();

    assertThatThrownBy(() -> new MasterServletFilter())
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Servlet filter org.sonar.server.platform.web.MasterServletFilter is already instantiated");
  }

  @Test
  public void should_propagate_initialization_failure() throws Exception {
    ServletFilter filter = createMockFilter();
    doThrow(new IllegalStateException("foo")).when(filter).init(any(FilterConfig.class));

    FilterConfig config = mock(FilterConfig.class);
    MasterServletFilter filters = new MasterServletFilter();

    assertThatThrownBy(() -> filters.init(config, singletonList(filter)))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("foo");
  }

  @Test
  public void filters_should_be_optional() throws Exception {
    FilterConfig config = mock(FilterConfig.class);
    MasterServletFilter filters = new MasterServletFilter();
    filters.init(config, Collections.emptyList());

    ServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    filters.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  public void should_add_scim_filter_first_for_scim_request() throws Exception {
    String scimPath = "/api/scim/v2/Groups";
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn(scimPath);
    when(request.getContextPath()).thenReturn("");
    ServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);

    ServletFilter filter1 = mockFilter(ServletFilter.class, request, response);
    ServletFilter filter2 = mockFilter(ServletFilter.class, request, response);
    ServletFilter filter3 = mockFilter(WebServiceFilter.class, request, response);
    when(filter3.doGetPattern()).thenReturn(UrlPattern.builder().includes(scimPath).build());

    MasterServletFilter filters = new MasterServletFilter();
    filters.init(mock(FilterConfig.class), asList(filter3, filter1, filter2));

    filters.doFilter(request, response, chain);

    InOrder inOrder = Mockito.inOrder(filter1, filter2, filter3);
    inOrder.verify(filter3).doFilter(any(), any(), any());
    inOrder.verify(filter1).doFilter(any(), any(), any());
    inOrder.verify(filter2).doFilter(any(), any(), any());
  }

  private ServletFilter mockFilter(Class<? extends ServletFilter> filterClazz, HttpServletRequest request, ServletResponse response) throws IOException, ServletException {
    ServletFilter filter = mock(filterClazz);
    when(filter.doGetPattern()).thenReturn(UrlPattern.builder().build());
    doAnswer(invocation -> {
      FilterChain argument = invocation.getArgument(2, FilterChain.class);
      argument.doFilter(request, response);
      return null;
    }).when(filter).doFilter(any(), any(), any());
    return filter;
  }

  @Test
  public void display_servlet_filter_patterns_in_INFO_log() {
    ServletFilter filter = new PatternFilter(UrlPattern.builder().includes("/api/issues").excludes("/batch/projects").build());
    FilterConfig config = mock(FilterConfig.class);
    MasterServletFilter master = new MasterServletFilter();

    master.init(config, singletonList(filter));

    assertThat(logTester.logs(LoggerLevel.INFO)).containsOnly("Initializing servlet filter PatternFilter [pattern=UrlPattern{inclusions=[/api/issues], exclusions=[/batch/projects]}]");
  }

  private static ServletFilter createMockFilter() {
    ServletFilter filter = mock(ServletFilter.class);
    when(filter.doGetPattern()).thenReturn(UrlPattern.builder().build());
    return filter;
  }

  private static class PatternFilter extends ServletFilter {

    private final UrlPattern urlPattern;

    PatternFilter(UrlPattern urlPattern) {
      this.urlPattern = urlPattern;
    }

    @Override
    public UrlPattern doGetPattern() {
      return urlPattern;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
      // Nothing to do
    }

    @Override
    public void init(FilterConfig filterConfig) {
      // Nothing to do
    }

    @Override
    public void destroy() {
      // Nothing to do
    }

    @Override
    public String toString() {
      return "PatternFilter";
    }
  }

}
