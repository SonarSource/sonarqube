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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import org.sonar.api.server.http.HttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.auth.saml.SamlAuthStatusPageGenerator.getSamlAuthStatusHtml;

public class SamlAuthStatusPageGeneratorTest {

  @Test
  public void getSamlAuthStatusHtml_whenCalled_shouldGeneratePageWithData() {
    SamlAuthenticationStatus samlAuthenticationStatus = mock(SamlAuthenticationStatus.class);
    HttpRequest request = mock(HttpRequest.class);

    when(samlAuthenticationStatus.getStatus()).thenReturn("success");
    when(samlAuthenticationStatus.getErrors()).thenReturn(new ArrayList<>());
    when(samlAuthenticationStatus.getWarnings()).thenReturn(new ArrayList<>());
    when(samlAuthenticationStatus.getAvailableAttributes()).thenReturn(new HashMap<>());
    when(samlAuthenticationStatus.getMappedAttributes()).thenReturn(new HashMap<>());
    when(samlAuthenticationStatus.isEncryptionEnabled()).thenReturn(false);
    when(samlAuthenticationStatus.isSignatureEnabled()).thenReturn(false);
    when(request.getContextPath()).thenReturn("context");

    String decodedDataResponse = getDecodedDataResponse(getSamlAuthStatusHtml(request, samlAuthenticationStatus));

    assertThat(decodedDataResponse).contains(
      "\"encryptionEnabled\":false",
      "\"signatureEnabled\":false",
      "\"errors\":[]",
      "\"warnings\":[]",
      "\"status\":\"success\"",
      "\"availableAttributes\":{}",
      "\"mappedAttributes\":{}");
  }

  private static String getDecodedDataResponse(String completeHtmlTemplate) {
    String pattern = "data-response=\"([^\"]+)\"";
    Pattern regex = Pattern.compile(pattern);
    Matcher matcher = regex.matcher(completeHtmlTemplate);
    if (matcher.find()) {
      String dataResponseValue = matcher.group(1);
      byte[] decoded = Base64.getDecoder().decode(dataResponseValue);
      return new String(decoded, StandardCharsets.UTF_8);
    }
    return "";
  }
}
