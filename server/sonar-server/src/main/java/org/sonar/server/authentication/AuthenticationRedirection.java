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
package org.sonar.server.authentication;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServletResponse;

import static java.lang.String.format;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;

public class AuthenticationRedirection {

  private AuthenticationRedirection() {
    // Only static methods
  }

  public static String encodeMessage(String message) {
    try {
      return encode(message, UTF_8.name())
        // In order for Javascript to be able to decode this message, + must be replaced by %20
        .replace("+", "%20");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(format("Fail to encode %s", message), e);
    }
  }

  public static void redirectTo(HttpServletResponse response, String url) {
    try {
      response.sendRedirect(url);
    } catch (IOException e) {
      throw new IllegalStateException(format("Fail to redirect to %s", url), e);
    }
  }
}
