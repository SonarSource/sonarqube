/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.plugins;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

}
