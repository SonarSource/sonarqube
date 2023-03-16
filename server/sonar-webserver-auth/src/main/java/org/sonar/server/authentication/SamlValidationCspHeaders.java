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

import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;

public class SamlValidationCspHeaders {

  private static final Pattern SCRIPT_PATTERN = Pattern.compile("(?<=<script>)(?s).*(?=</script>)");

  private SamlValidationCspHeaders() {
    throw new IllegalStateException("Utility class, cannot be instantiated");
  }

  public static void addCspHeadersToResponse(HttpServletResponse httpResponse, String hash) {
    List<String> cspPolicies = List.of(
      "default-src 'self'",
      "base-uri 'none'",
      "connect-src 'self' http: https:",
      "img-src * data: blob:",
      "object-src 'none'",
      "script-src 'self' '" + hash + "'",
      "style-src 'self' 'unsafe-inline'",
      "worker-src 'none'");
    String policies = String.join("; ", cspPolicies).trim();

    List<String> cspHeaders = List.of("Content-Security-Policy", "X-Content-Security-Policy", "X-WebKit-CSP");
    cspHeaders.forEach(header -> httpResponse.setHeader(header, policies));
  }

  public static String getHashForInlineScript(String html) {
    Matcher matcher = SCRIPT_PATTERN.matcher(html);
    if (matcher.find()) {
      return getBase64Sha256(matcher.group(0));
    }
    return "";
  }

  private static String getBase64Sha256(String string) {
    return "sha256-" + Base64.getEncoder().encodeToString(DigestUtils.sha256(string));
  }

}
