/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonarqube.ws.MediaTypes.JSON;
import static org.sonarqube.ws.MediaTypes.XML;

public class ServletResponseTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ServletOutputStream output = mock(ServletOutputStream.class);
  private HttpServletResponse response = mock(HttpServletResponse.class);

  private ServletResponse underTest = new ServletResponse(response);

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
