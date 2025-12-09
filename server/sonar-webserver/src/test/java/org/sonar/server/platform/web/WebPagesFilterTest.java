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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class WebPagesFilterTest {

  private static final String TEST_CONTEXT = "/sonarqube";

  private ServletContext servletContext = mock(ServletContext.class, RETURNS_MOCKS);
  private WebPagesCache webPagesCache = mock(WebPagesCache.class);

  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private FilterChain chain = mock(FilterChain.class);

  private WebPagesFilter underTest = new WebPagesFilter(webPagesCache);

  @Before
  public void setUp() {
    when(servletContext.getContextPath()).thenReturn(TEST_CONTEXT);
  }

  @Test
  public void return_web_page_content() throws Exception {
    String path = "/index.html";
    when(webPagesCache.getContent(path)).thenReturn("test");
    when(request.getRequestURI()).thenReturn(path);
    when(request.getContextPath()).thenReturn(TEST_CONTEXT);
    StringOutputStream outputStream = new StringOutputStream();
    when(response.getOutputStream()).thenReturn(outputStream);

    underTest.doFilter(request, response, chain);

    verify(response).setContentType("text/html");
    verify(response).setCharacterEncoding("utf-8");
    verify(response).setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    assertThat(outputStream).hasToString("test");
  }

  @Test
  public void does_nothing_when_static_resource() throws Exception{
    when(request.getRequestURI()).thenReturn("/static");
    when(request.getContextPath()).thenReturn(TEST_CONTEXT);

    underTest.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
    verifyNoInteractions(webPagesCache);
  }

  static class StringOutputStream extends ServletOutputStream {
    private final StringBuilder buf = new StringBuilder();

    StringOutputStream() {
    }

    @Override
    public boolean isReady() {
      return false;
    }

    @Override
    public void setWriteListener(WriteListener listener) {

    }

    public void write(byte[] b) {
      this.buf.append(new String(b));
    }

    public void write(byte[] b, int off, int len) {
      this.buf.append(new String(b, off, len));
    }

    public void write(int b) {
      byte[] bytes = new byte[] {(byte) b};
      this.buf.append(new String(bytes));
    }

    public String toString() {
      return this.buf.toString();
    }
  }
}
