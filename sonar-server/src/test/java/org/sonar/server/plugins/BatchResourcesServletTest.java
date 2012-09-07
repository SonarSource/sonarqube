/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.plugins;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class BatchResourcesServletTest {
  private BatchResourcesServlet servlet;
  private HttpServletRequest request;

  @Before
  public void setUp() throws Exception {
    servlet = new BatchResourcesServlet();
    request = mock(HttpServletRequest.class);
  }

  @Test
  public void test_filename() {
    when(request.getContextPath()).thenReturn("sonar");
    when(request.getServletPath()).thenReturn("/batch");

    when(request.getRequestURI()).thenReturn("/sonar/batch/sonar-core-2.6.jar");
    assertThat(servlet.filename(request)).isEqualTo("sonar-core-2.6.jar");

    when(request.getRequestURI()).thenReturn("/sonar/batch/");
    assertThat(servlet.filename(request)).isNull();

    when(request.getRequestURI()).thenReturn("/sonar/batch");
    assertThat(servlet.filename(request)).isNull();

    when(request.getRequestURI()).thenReturn("/sonar/batch.html");
    assertThat(servlet.filename(request)).isNull();

    when(request.getRequestURI()).thenReturn("/sonar/batch/index.html");
    assertThat(servlet.filename(request)).isNull();
  }

  @Test
  public void shouldDetermineListOfResources() {
    ServletContext servletContext = mock(ServletContext.class);
    servlet = spy(servlet);
    doReturn(servletContext).when(servlet).getServletContext();
    Set<String> libs = Sets.newHashSet();
    libs.add("/WEB-INF/lib/sonar-core-2.6.jar");
    libs.add("/WEB-INF/lib/treemap.rb");
    libs.add("/WEB-INF/lib/directory/");
    when(servletContext.getResourcePaths(anyString())).thenReturn(libs);

    assertThat(servlet.getLibs()).hasSize(1);
    assertThat(servlet.getLibs().get(0)).isEqualTo("sonar-core-2.6.jar");
  }

  @Test
  public void shouldIgnore() {
    assertThat(BatchResourcesServlet.isIgnored("sonar-batch-2.6-SNAPSHOT.jar")).isFalse();
    assertThat(BatchResourcesServlet.isIgnored("h2-1.3.166.jar")).isTrue();
    assertThat(BatchResourcesServlet.isIgnored("mysql-connector-java-5.1.13.jar")).isTrue();
    assertThat(BatchResourcesServlet.isIgnored("postgresql-9.0-801.jdbc3.jar")).isTrue();
    assertThat(BatchResourcesServlet.isIgnored("jtds-1.2.4.jar")).isTrue();
    assertThat(BatchResourcesServlet.isIgnored("jfreechart-1.0.9.jar")).isTrue();
    assertThat(BatchResourcesServlet.isIgnored("eastwood-1.1.0.jar")).isTrue();
    assertThat(BatchResourcesServlet.isIgnored("jetty-util-6.1.24.jar")).isTrue();
    assertThat(BatchResourcesServlet.isIgnored("jruby-complete-1.5.6.jar")).isTrue();
    assertThat(BatchResourcesServlet.isIgnored("jruby-rack-1.0.5.jar")).isTrue();
  }
}
