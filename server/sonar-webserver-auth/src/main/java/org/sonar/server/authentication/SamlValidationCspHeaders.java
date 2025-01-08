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
package org.sonar.server.authentication;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import org.sonar.api.server.http.HttpResponse;

public class SamlValidationCspHeaders {

  private SamlValidationCspHeaders() {
    throw new IllegalStateException("Utility class, cannot be instantiated");
  }

  public static String addCspHeadersWithNonceToResponse(HttpResponse httpResponse) {
    final String nonce = getNonce();

    List<String> cspPolicies = List.of(
      "default-src 'self'",
      "base-uri 'none'",
      "connect-src 'self' http: https:",
      "font-src 'self' data:;" +
      "img-src * data: blob:",
      "object-src 'none'",
      "script-src 'nonce-" + nonce + "'",
      "style-src 'self' 'unsafe-inline'",
      "worker-src 'none'");
    String policies = String.join("; ", cspPolicies).trim();

    List<String> cspHeaders = List.of("Content-Security-Policy", "X-Content-Security-Policy", "X-WebKit-CSP");
    cspHeaders.forEach(header -> httpResponse.setHeader(header, policies));
    return nonce;
  }

  private static String getNonce() {
    // this code is the same as in org.sonar.server.authentication.JwtCsrfVerifier.generateState
    return new BigInteger(130, new SecureRandom()).toString(32);
  }
}
