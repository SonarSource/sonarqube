/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.server.exceptions.UnauthorizedException;

import static java.lang.String.format;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.sonar.server.authentication.CookieUtils.createCookie;
import static org.sonar.server.authentication.CookieUtils.findCookie;

public class OAuthCsrfVerifier {

  private static final String CSRF_STATE_COOKIE = "OAUTHSTATE";

  public String generateState(HttpServletRequest request, HttpServletResponse response) {
    // Create a state token to prevent request forgery.
    // Store it in the session for later validation.
    String state = new BigInteger(130, new SecureRandom()).toString(32);
    response.addCookie(createCookie(CSRF_STATE_COOKIE, sha256Hex(state), true, -1, request));
    return state;
  }

  public void verifyState(HttpServletRequest request, HttpServletResponse response) {
    Cookie cookie = findCookie(CSRF_STATE_COOKIE, request).orElseThrow(() -> new UnauthorizedException(format("Cookie '%s' is missing", CSRF_STATE_COOKIE)));
    String hashInCookie = cookie.getValue();

    // remove cookie
    response.addCookie(createCookie(CSRF_STATE_COOKIE, null, true, 0, request));

    String stateInRequest = request.getParameter("state");
    if (isBlank(stateInRequest) || !sha256Hex(stateInRequest).equals(hashInCookie)) {
      throw new UnauthorizedException("CSRF state value is invalid");
    }
  }

}
