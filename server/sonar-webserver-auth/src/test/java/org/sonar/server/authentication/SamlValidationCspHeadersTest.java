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
package org.sonar.server.authentication;

import javax.servlet.http.HttpServletResponse;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.server.authentication.SamlValidationCspHeaders.addCspHeadersToResponse;
import static org.sonar.server.authentication.SamlValidationCspHeaders.getHashForInlineScript;

public class SamlValidationCspHeadersTest {

  @Test
  public void CspHeaders_are_correctly_added_to_response() {
    HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);

    addCspHeadersToResponse(httpServletResponse, "hash");
    verify(httpServletResponse).setHeader("Content-Security-Policy", "default-src 'self'; base-uri 'none'; connect-src 'self' http: https:; img-src * data: blob:; object-src 'none'; script-src 'self' 'hash'; style-src 'self' 'unsafe-inline'; worker-src 'none'");
    verify(httpServletResponse).setHeader("X-Content-Security-Policy", "default-src 'self'; base-uri 'none'; connect-src 'self' http: https:; img-src * data: blob:; object-src 'none'; script-src 'self' 'hash'; style-src 'self' 'unsafe-inline'; worker-src 'none'");
    verify(httpServletResponse).setHeader("X-WebKit-CSP", "default-src 'self'; base-uri 'none'; connect-src 'self' http: https:; img-src * data: blob:; object-src 'none'; script-src 'self' 'hash'; style-src 'self' 'unsafe-inline'; worker-src 'none'");
  }

  @Test
  public void hash_is_properly_calculated_for_an_inline_script() {
    String hash = getHashForInlineScript(getBasicHtmlWithScript());
    assertEquals("sha256-jRoPhEx/vXxIUUkuTwJJ2ww4OPlo7B2ZK/wDVC4IXUs=", hash);
  }

  @Test
  public void hash_is_empty_when_no_inline_script_available() {
    String hash = getHashForInlineScript(getBasicHtmlWithoutScript());
    assertEquals("", hash);
  }

  private String getBasicHtmlWithScript() {
    return """
      <!DOCTYPE html>
      <html lang="en">
        <head>
          <title>SAML Authentication Test</title>
        </head>
        <body>
          <script>
            function createBox() {
              const box = document.createElement("div");
              box.className = "box";
              return box;
            });
          </script>
        </body>
      </html>
      """;
  }

  private String getBasicHtmlWithoutScript() {
    return """
      <!DOCTYPE html>
      <html lang="en">
        <head>
          <title>SAML Authentication Test</title>
        </head>  
        <body>
          <div id="content">
            <h1>SAML Authentication Test</h1>
            <div class="box">
              <div id="status"></div>
            </div>
            <div id="response" data-response="%SAML_AUTHENTICATION_STATUS%"></div>
          </div>
        </body>
      </html>
      """;
  }
}
