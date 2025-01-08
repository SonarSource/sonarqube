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

import java.util.Arrays;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.http.Cookie;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.server.http.JakartaHttpRequest.JakartaCookie;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Helper class to create a {@link jakarta.servlet.http.Cookie}.
 *
 * The {@link jakarta.servlet.http.Cookie#setSecure(boolean)} will automatically be set to true.
 */
public class Cookies {
  public static final String SET_COOKIE = "Set-Cookie";
  public static final String SAMESITE_LAX = "Lax";

  private static final String HTTPS_HEADER = "X-Forwarded-Proto";
  private static final String HTTPS_VALUE = "https";

  private Cookies() {
    // Only static methods
  }

  public static Optional<Cookie> findCookie(String cookieName, HttpRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }
    return Arrays.stream(cookies)
      .filter(cookie -> cookieName.equals(cookie.getName()))
      .findFirst();
  }

  public static CookieBuilder newCookieBuilder(HttpRequest request) {
    return new CookieBuilder(request);
  }

  public static class CookieBuilder {

    private final HttpRequest request;

    private String name;
    private String value;
    private boolean httpOnly;
    private int expiry;

    private String sameSite;

    CookieBuilder(HttpRequest request) {
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
     * SameSite attribute, only work for toString()
     */
    public CookieBuilder setSameSite(@Nullable String sameSite) {
      this.sameSite = sameSite;
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
      jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(requireNonNull(name), value);
      cookie.setPath(getContextPath(request));
      cookie.setSecure(isHttps(request));
      cookie.setHttpOnly(httpOnly);
      cookie.setMaxAge(expiry);
      return new JakartaCookie(cookie);
    }

    public String toValueString() {
      String output = String.format("%s=%s; Path=%s; SameSite=%s; Max-Age=%d", name, value, getContextPath(request), sameSite, expiry);
      if (httpOnly) {
        output += "; HttpOnly";
      }
      if (isHttps(request)) {
        output += "; Secure";
      }
      return output;
    }

    private static boolean isHttps(HttpRequest request) {
      return HTTPS_VALUE.equalsIgnoreCase(request.getHeader(HTTPS_HEADER));
    }

    private static String getContextPath(HttpRequest request) {
      String path = request.getContextPath();
      return isNullOrEmpty(path) ? "/" : path;
    }
  }
}
