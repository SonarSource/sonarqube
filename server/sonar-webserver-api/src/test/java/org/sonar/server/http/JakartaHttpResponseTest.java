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
package org.sonar.server.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.sonar.api.server.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JakartaHttpResponseTest {

  @Test
  public void delegate_methods() throws IOException {
    HttpServletResponse responseMock = mock(HttpServletResponse.class);
    when(responseMock.getHeader("h1")).thenReturn("hvalue1");
    when(responseMock.getHeaders("h1")).thenReturn(List.of("hvalue1"));
    when(responseMock.getStatus()).thenReturn(200);
    ServletOutputStream outputStream = mock(ServletOutputStream.class);
    when(responseMock.getOutputStream()).thenReturn(outputStream);
    PrintWriter writer = mock(PrintWriter.class);
    when(responseMock.getWriter()).thenReturn(writer);

    JakartaHttpResponse underTest = new JakartaHttpResponse(responseMock);

    assertThat(underTest.getDelegate()).isSameAs(responseMock);
    assertThat(underTest.getHeader("h1")).isEqualTo("hvalue1");
    assertThat(underTest.getHeaders("h1")).containsExactly("hvalue1");
    assertThat(underTest.getStatus()).isEqualTo(200);
    assertThat(underTest.getWriter()).isEqualTo(writer);
    assertThat(underTest.getOutputStream()).isEqualTo(outputStream);

    underTest.addHeader("h2", "hvalue2");
    underTest.setHeader("h3", "hvalue3");
    underTest.setStatus(201);
    underTest.setContentType("text/plain");
    underTest.sendRedirect("http://redirect");
    underTest.setCharacterEncoding("UTF-8");

    Cookie cookie = mock(Cookie.class);
    when(cookie.getName()).thenReturn("name");
    when(cookie.getValue()).thenReturn("value");
    underTest.addCookie(cookie);
    verify(responseMock).addHeader("h2", "hvalue2");
    verify(responseMock).setHeader("h3", "hvalue3");
    verify(responseMock).setStatus(201);
    verify(responseMock).setContentType("text/plain");
    verify(responseMock).sendRedirect("http://redirect");
    verify(responseMock).setCharacterEncoding("UTF-8");
    verify(responseMock).addCookie(any(jakarta.servlet.http.Cookie.class));
  }
}
