/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.net.MalformedURLException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class WebPagesFilterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private HttpServletRequest request = mock(HttpServletRequest.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);
  private FilterChain chain = mock(FilterChain.class);
  private ServletContext servletContext = mock(ServletContext.class);
  private FilterConfig filterConfig = mock(FilterConfig.class);
  private StringOutputStream outputStream = new StringOutputStream();

  private WebPagesFilter underTest = new WebPagesFilter();

  @Before
  public void setUp() throws Exception {
    when(filterConfig.getServletContext()).thenReturn(servletContext);
    when(response.getOutputStream()).thenReturn(outputStream);
  }

  @Test
  public void verify_paths() throws Exception {
    mockIndexFile();
    verifyPathIsHandled("/");
    verifyPathIsHandled("/issues");
    verifyPathIsHandled("/foo");
  }

  @Test
  public void return_index_file_content() throws Exception {
    mockIndexFile();
    mockPath("/foo", "");
    underTest.init(filterConfig);
    underTest.doFilter(request, response, chain);

    assertThat(outputStream.toString()).contains("<head>");
    verify(response).setContentType("text/html");
    verify(response).setCharacterEncoding("utf-8");
    verify(response).setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    verify(response).getOutputStream();
    verifyNoMoreInteractions(response);
  }

  @Test
  public void return_index_file_content_with_default_web_context() throws Exception {
    mockIndexFile();
    mockPath("/foo", "");
    underTest.init(filterConfig);
    underTest.doFilter(request, response, chain);

    assertThat(outputStream.toString()).contains("href=\"/sonar.css\"");
    assertThat(outputStream.toString()).contains("<script src=\"/sonar.js\"></script>");
    assertThat(outputStream.toString()).doesNotContain("%WEB_CONTEXT%");
  }

  @Test
  public void return_index_file_content_with_web_context() throws Exception {
    mockIndexFile();
    mockPath("/foo", "/web");
    underTest.init(filterConfig);
    underTest.doFilter(request, response, chain);

    assertThat(outputStream.toString()).contains("href=\"/web/sonar.css\"");
    assertThat(outputStream.toString()).contains("<script src=\"/web/sonar.js\"></script>");
    assertThat(outputStream.toString()).doesNotContain("%WEB_CONTEXT%");
  }

  @Test
  public void fail_when_index_is_not_found() throws Exception {
    mockPath("/foo", "");
    when(servletContext.getResource("/index.html")).thenReturn(null);

    expectedException.expect(IllegalStateException.class);
    underTest.init(filterConfig);
  }

  private void mockIndexFile() throws MalformedURLException {
    when(servletContext.getResource("/index.html")).thenReturn(getClass().getResource("WebPagesFilterTest/index.html"));
  }

  private void mockPath(String path, String context) {
    when(request.getRequestURI()).thenReturn(path);
    when(request.getContextPath()).thenReturn(context);
    when(servletContext.getContextPath()).thenReturn(context);
  }

  private void verifyPathIsHandled(String path) throws Exception {
    mockPath(path, "");
    underTest.init(filterConfig);

    underTest.doFilter(request, response, chain);

    verify(response).getOutputStream();
    verify(response).setContentType(anyString());
    reset(response);
    when(response.getOutputStream()).thenReturn(outputStream);
  }

  private void verifyPthIsIgnored(String path) throws Exception {
    mockPath(path, "");
    underTest.init(filterConfig);

    underTest.doFilter(request, response, chain);

    verifyZeroInteractions(response);
    reset(response);
    when(response.getOutputStream()).thenReturn(outputStream);
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
