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

import java.util.Arrays;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Helper class to create a {@link javax.servlet.http.Cookie}.
 *
 * The {@link javax.servlet.http.Cookie#secure} will automatically be set.
 */
public class Cookies {

  private static final String HTTPS_HEADER = "X-Forwarded-Proto";
  private static final String HTTPS_VALUE = "https";

  private Cookies() {
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

  public static CookieBuilder newCookieBuilder(HttpServletRequest request) {
    return new CookieBuilder(request);
  }

  public static class CookieBuilder {

    private final HttpServletRequest request;

    private String name;
    private String value;
    private boolean httpOnly;
    private int expiry;

    public CookieBuilder(HttpServletRequest request) {
      this.request = request;
    }

    /**
     * Name of the cookie
     */
    public CookieBuilder setName(String name) {
      this.name = requireNonNull(name);
      return this;
    }

    /**
     * Name of the cookie
     */
    public CookieBuilder setValue(@Nullable String value) {
      this.value = value;
      return this;
    }

    /**
     * Sets the flag that controls if this cookie will be hidden from scripts on the client side.
     */
    public CookieBuilder setHttpOnly(boolean httpOnly) {
      this.httpOnly = httpOnly;
      return this;
    }

    /**
     * Sets the maximum age of the cookie in seconds.
     */
    public CookieBuilder setExpiry(int expiry) {
      this.expiry = expiry;
      return this;
    }

    public Cookie build() {
      Cookie cookie = new Cookie(requireNonNull(name), value);
      cookie.setPath(getContextPath(request));
      cookie.setSecure(isHttps(request));
      cookie.setHttpOnly(httpOnly);
      cookie.setMaxAge(expiry);
      return cookie;
    }

    private static boolean isHttps(HttpServletRequest request) {
      return HTTPS_VALUE.equalsIgnoreCase(request.getHeader(HTTPS_HEADER));
    }

    private static String getContextPath(HttpServletRequest request) {
      String path = request.getContextPath();
      return isNullOrEmpty(path) ? "/" : path;
    }
  }
}
