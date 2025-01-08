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
package org.sonar.auth.saml;

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.json.JSONObject;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.http.HttpRequest;

@ServerSide
class SamlAuthStatusPageGenerator {

  private static final String WEB_CONTEXT = "WEB_CONTEXT";
  private static final String SAML_AUTHENTICATION_STATUS = "%SAML_AUTHENTICATION_STATUS%";
  private static final String HTML_TEMPLATE_NAME = "samlAuthResult.html";

  public String getSamlAuthStatusHtml(HttpRequest request, SamlAuthenticationStatus samlAuthenticationStatus) {
    Map<String, String> substitutionsMap = getSubstitutionsMap(request, samlAuthenticationStatus);
    String htmlTemplate = getPlainTemplate();

    return substitutionsMap
      .keySet()
      .stream()
      .reduce(htmlTemplate, (accumulator, pattern) -> accumulator.replace(pattern, substitutionsMap.get(pattern)));
  }

  private static Map<String, String> getSubstitutionsMap(HttpRequest request, SamlAuthenticationStatus samlAuthenticationStatus) {
    return Map.of(
      WEB_CONTEXT, request.getContextPath(),
      SAML_AUTHENTICATION_STATUS, getBase64EncodedStatus(samlAuthenticationStatus));
  }

  private static String getBase64EncodedStatus(SamlAuthenticationStatus samlAuthenticationStatus) {
    byte[] bytes = new JSONObject(samlAuthenticationStatus).toString().getBytes(StandardCharsets.UTF_8);
    return String.format("%s", Base64.getEncoder().encodeToString(bytes));
  }

  private static String getPlainTemplate() {
    URL url = Resources.getResource(HTML_TEMPLATE_NAME);
    try {
      return Resources.toString(url, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read the template " + HTML_TEMPLATE_NAME);
    }
  }

}
