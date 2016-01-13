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
package org.sonar.server.plugins;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StaticResourcesServletTest {

  private StaticResourcesServlet servlet;
  private HttpServletRequest request;

  @Before
  public void setUp() {
    servlet = new StaticResourcesServlet();
    request = mock(HttpServletRequest.class);
  }

  @Test
  public void shouldDeterminePluginKey() {
    when(request.getContextPath()).thenReturn("/");
    when(request.getServletPath()).thenReturn("static");
    when(request.getRequestURI()).thenReturn("/static/myplugin/image.png");
    assertThat(servlet.getPluginKey(request)).isEqualTo("myplugin");

    when(request.getRequestURI()).thenReturn("/static/myplugin/images/image.png");
    assertThat(servlet.getPluginKey(request)).isEqualTo("myplugin");

    when(request.getRequestURI()).thenReturn("/static/myplugin/");
    assertThat(servlet.getPluginKey(request)).isEqualTo("myplugin");
  }

  @Test
  public void shouldDetermineResourcePath() {
    when(request.getContextPath()).thenReturn("/");
    when(request.getServletPath()).thenReturn("static");
    when(request.getRequestURI()).thenReturn("/static/myplugin/image.png");
    assertThat(servlet.getResourcePath(request)).isEqualTo("static/image.png");

    when(request.getRequestURI()).thenReturn("/static/myplugin/images/image.png");
    assertThat(servlet.getResourcePath(request)).isEqualTo("static/images/image.png");

    when(request.getRequestURI()).thenReturn("/static/myplugin/");
    assertThat(servlet.getResourcePath(request)).isEqualTo("static/");
  }

  @Test
  public void completeMimeType() {
    HttpServletResponse response = mock(HttpServletResponse.class);
    servlet.completeContentType(response, "static/sqale/sqale.css");
    verify(response).setContentType("text/css");
  }
}
