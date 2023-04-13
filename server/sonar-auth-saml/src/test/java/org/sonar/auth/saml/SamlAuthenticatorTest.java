/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.auth.saml;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.server.http.JavaxHttpRequest;
import org.sonar.server.http.JavaxHttpResponse;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SamlAuthenticatorTest {

  @Test
  public void authentication_status_with_errors_returned_when_init_fails() {
    SamlAuthenticator samlAuthenticator = new SamlAuthenticator(mock(SamlSettings.class), mock(SamlMessageIdChecker.class));
    HttpRequest request = new JavaxHttpRequest(mock(HttpServletRequest.class));
    HttpResponse response = new JavaxHttpResponse(mock(HttpServletResponse.class));
    when(request.getContextPath()).thenReturn("context");

    String authenticationStatus = samlAuthenticator.getAuthenticationStatusPage(request, response);

    assertFalse(authenticationStatus.isEmpty());
  }
}
