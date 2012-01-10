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

import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StaticResourcesServletTest {

  private StaticResourcesServlet servlet;
  private HttpServletRequest request;

  @Before
  public void setUp() throws Exception {
    servlet = new StaticResourcesServlet();
    request = mock(HttpServletRequest.class);
  }

  @Test
  public void shouldDeterminePluginKey() {
    when(request.getContextPath()).thenReturn("/");
    when(request.getServletPath()).thenReturn("static");
    when(request.getRequestURI()).thenReturn("/static/myplugin/image.png");
    assertThat(servlet.getPluginKey(request), is("myplugin"));

    when(request.getRequestURI()).thenReturn("/static/myplugin/images/image.png");
    assertThat(servlet.getPluginKey(request), is("myplugin"));

    when(request.getRequestURI()).thenReturn("/static/myplugin/");
    assertThat(servlet.getPluginKey(request), is("myplugin"));
  }

  @Test
  public void shouldDetermineResourcePath() {
    when(request.getContextPath()).thenReturn("/");
    when(request.getServletPath()).thenReturn("static");
    when(request.getRequestURI()).thenReturn("/static/myplugin/image.png");
    assertThat(servlet.getResourcePath(request), is("static/image.png"));

    when(request.getRequestURI()).thenReturn("/static/myplugin/images/image.png");
    assertThat(servlet.getResourcePath(request), is("static/images/image.png"));

    when(request.getRequestURI()).thenReturn("/static/myplugin/");
    assertThat(servlet.getResourcePath(request), is("static/"));
  }
}
