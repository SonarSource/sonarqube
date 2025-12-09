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
package org.sonar.server.app;

import java.io.IOException;
import jakarta.servlet.ServletException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SecurityErrorReportValveTest {

  SecureErrorReportValve underTest = spy(SecureErrorReportValve.class);

  @Test
  public void add_security_headers() throws ServletException, IOException {
    var request = mock(Request.class);
    var response = mock(Response.class);

    underTest.setNext(new ValveBase() {
      @Override
      public void invoke(Request request, Response response) {
      }
    });

    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/");
    when(request.getContextPath()).thenReturn("");
    when(request.getHeader("x-forwarded-proto")).thenReturn("https");

    underTest.invoke(request, response);

    verify(response).setHeader("X-Frame-Options", "SAMEORIGIN");
    verify(response).setHeader("X-XSS-Protection", "0");
    verify(response).setHeader("X-Content-Type-Options", "nosniff");
    verify(response).setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains;");
  }
}
