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

package org.sonar.server.platform.web;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebPagesFilterTest {

  HttpServletRequest request = mock(HttpServletRequest.class);
  HttpServletResponse response = mock(HttpServletResponse.class);
  FilterChain chain = mock(FilterChain.class);
  ServletContext servletContext = mock(ServletContext.class);
  FilterConfig filterConfig = mock(FilterConfig.class);
  StringOutputStream outputStream = new StringOutputStream();

  WebPagesFilter underTest = new WebPagesFilter();

  @Before
  public void setUp() throws Exception {
    when(filterConfig.getServletContext()).thenReturn(servletContext);
    when(response.getOutputStream()).thenReturn(outputStream);
  }

  @Test
  public void do_get_pattern() throws Exception {
    assertThat(underTest.doGetPattern().matches("/")).isTrue();
    assertThat(underTest.doGetPattern().matches("/issues")).isTrue();
    assertThat(underTest.doGetPattern().matches("/foo")).isTrue();
    assertThat(underTest.doGetPattern().matches("/api/issues/search")).isFalse();
    assertThat(underTest.doGetPattern().matches("/batch/index")).isFalse();
  }

  @Test
  public void return_index_file_with_default_web_context() throws Exception {
    when(servletContext.getResource("/index.html")).thenReturn(getClass().getResource("WebPagesFilterTest/index.html"));
    when(servletContext.getContextPath()).thenReturn("");
    underTest.init(filterConfig);
    underTest.doFilter(request, response, chain);

    assertThat(outputStream.toString()).contains("href=\"/sonar.css\"");
    assertThat(outputStream.toString()).contains("<script src=\"/sonar.js\"></script>");
    assertThat(outputStream.toString()).doesNotContain("%WEB_CONTEXT%");
  }

  @Test
  public void return_index_file_with_web_context() throws Exception {
    when(servletContext.getResource("/index.html")).thenReturn(getClass().getResource("WebPagesFilterTest/index.html"));
    when(servletContext.getContextPath()).thenReturn("/web");
    underTest.init(filterConfig);
    underTest.doFilter(request, response, chain);

    assertThat(outputStream.toString()).contains("href=\"/web/sonar.css\"");
    assertThat(outputStream.toString()).contains("<script src=\"/web/sonar.js\"></script>");
    assertThat(outputStream.toString()).doesNotContain("%WEB_CONTEXT%");
  }

  class StringOutputStream extends ServletOutputStream {
    private StringBuffer buf = new StringBuffer();

    StringOutputStream() {
    }

    @Override
    public boolean isReady() {
      return false;
    }

    @Override
    public void setWriteListener(WriteListener listener) {

    }

    public void write(byte[] b) throws IOException {
      this.buf.append(new String(b));
    }

    public void write(byte[] b, int off, int len) throws IOException {
      this.buf.append(new String(b, off, len));
    }

    public void write(int b) throws IOException {
      byte[] bytes = new byte[] {(byte) b};
      this.buf.append(new String(bytes));
    }

    public String toString() {
      return this.buf.toString();
    }
  }
}
