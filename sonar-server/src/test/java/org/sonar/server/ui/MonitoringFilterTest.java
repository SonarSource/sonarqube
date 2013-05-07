/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.ui;

import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MonitoringFilterTest {

  @Test
  public void shouldHaveNoImpactWhenDisabled() throws IOException, ServletException {
    MonitoringFilter filter = new MonitoringFilter() {
      @Override
      boolean isJmxMonitoringActive() {
        return false;
      }
    };
    FilterConfig config = mock(FilterConfig.class);
    ServletRequest request = mock(ServletRequest.class);
    ServletResponse response = mock(ServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    filter.init(config);
    filter.doFilter(request, response, chain);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldRegisterMetricsAndHaveNoImpactOnRequestsWhenActive() throws IOException, ServletException {
    MonitoringFilter filter = new MonitoringFilter() {
      @Override
      boolean isJmxMonitoringActive() {
        return true;
      }
    };
    FilterConfig config = mock(FilterConfig.class);
    ServletRequest request = mock(HttpServletRequest.class);
    ServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    ServletContext ctx = mock(ServletContext.class);
    when(config.getServletContext()).thenReturn(ctx);
    filter.init(config);
    filter.doFilter(request, response, chain);
    verify(chain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    String[] domains = ManagementFactory.getPlatformMBeanServer().getDomains();
    assertThat(domains).contains("sonar");
  }

}
