/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.ws;

import java.io.IOException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.sonar.server.http.JakartaHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonarqube.ws.MediaTypes.JSON;
import static org.sonarqube.ws.MediaTypes.XML;

public class ServletResponseTest {

  private final ServletOutputStream output = mock(ServletOutputStream.class);
  private final HttpServletResponse response = mock(HttpServletResponse.class);

  private final ServletResponse underTest = new ServletResponse(new JakartaHttpResponse(response));

  @Before
  public void setUp() throws Exception {
    when(response.getOutputStream()).thenReturn(output);
  }

  @Test
  public void test_default_header() {
    verify(response).setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
  }

  @Test
  public void set_header() {
    underTest.setHeader("header", "value");

    verify(response).setHeader("header", "value");
  }

  @Test
  public void get_header() {
    underTest.getHeader("header");

    verify(response).getHeader("header");
  }

  @Test
  public void get_header_names() {
    underTest.getHeaderNames();

    verify(response).getHeaderNames();
  }

  @Test
  public void test_default_status() {
    verify(response).setStatus(200);
  }

  @Test
  public void set_status() {
    underTest.stream().setStatus(404);

    verify(response).setStatus(404);
  }

  @Test
  public void setCharacterEncoding_encodingIsSet() {
    underTest.stream().setCharacterEncoding("UTF-8");

    verify(response).setCharacterEncoding("UTF-8");
  }

  @Test
  public void flushBuffer_bufferIsFlushed() throws IOException {
    underTest.stream().flushBuffer();

    verify(response).flushBuffer();
  }

  @Test
  public void test_output() {
    assertThat(underTest.stream().output()).isEqualTo(output);
  }


  @Test
  public void test_reset() {
    underTest.stream().reset();

    verify(response).reset();
  }

  @Test
  public void test_newJsonWriter() throws Exception {
    underTest.newJsonWriter();

    verify(response).setContentType(JSON);
    verify(response).getOutputStream();
  }

  @Test
  public void test_newXmlWriter() throws Exception {
    underTest.newXmlWriter();

    verify(response).setContentType(XML);
    verify(response).getOutputStream();
  }

  @Test
  public void test_noContent() throws Exception {
    underTest.noContent();

    verify(response).setStatus(204);
    verify(response, never()).getOutputStream();
  }
}
