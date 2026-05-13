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
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConditionalSpringSecurityFilterTest {

  private static final String TARGET_BEAN_NAME = "springSecurityFilterChain";
  private static final String CONTEXT_ATTRIBUTE = "org.springframework.web.servlet.FrameworkServlet.CONTEXT.app";

  private ConditionalSpringSecurityFilter filter;
  private ServletContext servletContext;
  private FilterConfig filterConfig;
  private ServletRequest request;
  private ServletResponse response;
  private FilterChain chain;

  @BeforeEach
  void setUp() {
    servletContext = mock(ServletContext.class);
    filterConfig = mock(FilterConfig.class);
    request = mock(ServletRequest.class);
    response = mock(ServletResponse.class);
    chain = mock(FilterChain.class);

    when(filterConfig.getServletContext()).thenReturn(servletContext);

    filter = new ConditionalSpringSecurityFilter(TARGET_BEAN_NAME, CONTEXT_ATTRIBUTE);
  }

  @Test
  void init_shouldStoreServletContext() {
    filter.init(filterConfig);

    verify(filterConfig).getServletContext();
  }

  @Test
  void doFilter_whenContextNotInitialized_shouldPassThrough() throws Exception {
    filter.init(filterConfig);
    when(servletContext.getAttribute(CONTEXT_ATTRIBUTE)).thenReturn(null);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  void doFilter_whenContextExistsButNoBeanYet_shouldPassThrough() throws Exception {
    filter.init(filterConfig);
    WebApplicationContext context = mock(WebApplicationContext.class);
    when(servletContext.getAttribute(CONTEXT_ATTRIBUTE)).thenReturn(context);
    when(context.containsBean(TARGET_BEAN_NAME)).thenReturn(false);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verify(context).containsBean(TARGET_BEAN_NAME);
  }

  @Test
  void doFilter_whenContextThrowsException_shouldPassThrough() throws Exception {
    filter.init(filterConfig);
    when(servletContext.getAttribute(CONTEXT_ATTRIBUTE)).thenThrow(new RuntimeException("Context error"));

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  void doFilter_whenDelegateInitialized_shouldDelegateToSpringSecurityFilter() throws Exception {
    filter.init(filterConfig);

    // Set up Spring context with security filter bean
    WebApplicationContext context = mock(WebApplicationContext.class);
    Filter securityFilter = mock(Filter.class);
    when(servletContext.getAttribute(CONTEXT_ATTRIBUTE)).thenReturn(context);
    when(context.containsBean(TARGET_BEAN_NAME)).thenReturn(true);
    when(context.getBean(TARGET_BEAN_NAME, Filter.class)).thenReturn(securityFilter);

    filter.doFilter(request, response, chain);

    verify(securityFilter).doFilter(request, response, chain);
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  void doFilter_afterInitialization_shouldCacheDelegateAndNotReinitialize() throws Exception {
    filter.init(filterConfig);

    WebApplicationContext context = mock(WebApplicationContext.class);
    Filter securityFilter = mock(Filter.class);
    when(servletContext.getAttribute(CONTEXT_ATTRIBUTE)).thenReturn(context);
    when(context.containsBean(TARGET_BEAN_NAME)).thenReturn(true);
    when(context.getBean(TARGET_BEAN_NAME, Filter.class)).thenReturn(securityFilter);

    // First request - initializes delegate
    filter.doFilter(request, response, chain);

    // Second request - should use cached delegate
    filter.doFilter(request, response, chain);

    // Verify delegate was used twice but context was queried only once
    verify(securityFilter, times(2)).doFilter(request, response, chain);
    verify(context, times(1)).containsBean(TARGET_BEAN_NAME);
    verify(context, times(1)).getBean(TARGET_BEAN_NAME, Filter.class);
  }

  @Test
  void doFilter_whenBeanLookupThrowsException_shouldPassThrough() throws Exception {
    filter.init(filterConfig);

    WebApplicationContext context = mock(WebApplicationContext.class);
    when(servletContext.getAttribute(CONTEXT_ATTRIBUTE)).thenReturn(context);
    when(context.containsBean(TARGET_BEAN_NAME)).thenReturn(true);
    when(context.getBean(TARGET_BEAN_NAME, Filter.class)).thenThrow(new RuntimeException("Bean error"));

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  void doFilter_transitionFromSafeModeToLevel4_shouldStartDelegating() throws Exception {
    filter.init(filterConfig);

    // Initially no context (safe mode)
    when(servletContext.getAttribute(CONTEXT_ATTRIBUTE)).thenReturn(null);

    filter.doFilter(request, response, chain);
    verify(chain).doFilter(request, response);

    // Now context becomes available (level 4 initialized)
    WebApplicationContext context = mock(WebApplicationContext.class);
    Filter securityFilter = mock(Filter.class);
    when(servletContext.getAttribute(CONTEXT_ATTRIBUTE)).thenReturn(context);
    when(context.containsBean(TARGET_BEAN_NAME)).thenReturn(true);
    when(context.getBean(TARGET_BEAN_NAME, Filter.class)).thenReturn(securityFilter);

    filter.doFilter(request, response, chain);
    verify(securityFilter).doFilter(request, response, chain);
  }

  @Test
  void doFilter_whenContextReturnsNullForAttribute_shouldPassThrough() throws Exception {
    filter.init(filterConfig);
    when(servletContext.getAttribute(CONTEXT_ATTRIBUTE)).thenReturn(null);

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verify(servletContext).getAttribute(CONTEXT_ATTRIBUTE);
  }

  @Test
  void doFilter_whenContextIsNotWebApplicationContext_shouldPassThrough() throws Exception {
    filter.init(filterConfig);
    when(servletContext.getAttribute(CONTEXT_ATTRIBUTE)).thenReturn("NotAContext");

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  void destroy_shouldCompleteWithoutError() {
    assertThat(filter).isNotNull();

    assertDoesNotThrow(() -> filter.destroy());
  }
}
