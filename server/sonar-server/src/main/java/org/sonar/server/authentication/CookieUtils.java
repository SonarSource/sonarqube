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

import java.util.Arrays;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class CookieUtils {

  private static final String HTTPS_HEADER = "X-Forwarded-Proto";
  private static final String HTTPS_VALUE = "https";

  private CookieUtils() {
    // Only static methods
  }

  public static Optional<Cookie> findCookie(String cookieName, HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }
    return Arrays.stream(cookies)
      .filter(cookie -> cookieName.equals(cookie.getName()))
      .findFirst();
  }

  public static Cookie createCookie(String name, @Nullable String value, boolean httpOnly, int expiry, HttpServletRequest request) {
    Cookie cookie = new Cookie(name, value);
    // Path is set "/" in order to allow rails to be able to remove cookies
    // TODO When logout when be implemented in Java (SONAR-7774), following line should be replaced by
    // cookie.setPath(request.getContextPath()"/");
    cookie.setPath("/");
    cookie.setSecure(isHttps(request));
    cookie.setHttpOnly(httpOnly);
    cookie.setMaxAge(expiry);
    return cookie;
  }

  private static boolean isHttps(HttpServletRequest request) {
    return HTTPS_VALUE.equalsIgnoreCase(request.getHeader(HTTPS_HEADER));
  }
}
