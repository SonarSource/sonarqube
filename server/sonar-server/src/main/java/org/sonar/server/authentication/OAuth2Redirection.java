/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.Optional;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.elasticsearch.common.Nullable;

import static org.elasticsearch.common.Strings.isNullOrEmpty;
import static org.sonar.server.authentication.Cookies.findCookie;
import static org.sonar.server.authentication.Cookies.newCookieBuilder;

public class OAuth2Redirection {

  private static final String REDIRECT_TO_COOKIE = "REDIRECT_TO";

  /**
   * The HTTP parameter that contains the path where the user should be redirect to.
   * Please note that the web context is included.
   */
  private static final String RETURN_TO_PARAMETER = "return_to";

  public void create(HttpServletRequest request, HttpServletResponse response) {
    Optional<String> redirectTo = sanitizeRedirectUrl(request.getParameter(RETURN_TO_PARAMETER));
    if (!redirectTo.isPresent()) {
      return;
    }
    response.addCookie(newCookieBuilder(request)
      .setName(REDIRECT_TO_COOKIE)
      .setValue(redirectTo.get())
      .setHttpOnly(true)
      .setExpiry(-1)
      .build());
  }

  public Optional<String> getAndDelete(HttpServletRequest request, HttpServletResponse response) {
    Optional<Cookie> cookie = findCookie(REDIRECT_TO_COOKIE, request);
    if (!cookie.isPresent()) {
      return Optional.empty();
    }

    delete(request, response);

    String redirectTo = cookie.get().getValue();
    if (isNullOrEmpty(redirectTo)) {
      return Optional.empty();
    }
    return Optional.of(redirectTo);
  }

  public void delete(HttpServletRequest request, HttpServletResponse response) {
    response.addCookie(newCookieBuilder(request)
      .setName(REDIRECT_TO_COOKIE)
      .setValue(null)
      .setHttpOnly(true)
      .setExpiry(0)
      .build());
  }

  private static Optional<String> sanitizeRedirectUrl(@Nullable String url){
    if (isNullOrEmpty(url)){
      return Optional.empty();
    }
    if (url.startsWith("//") || url.startsWith("/\\")){
      return Optional.empty();
    }
    if (!url.startsWith("/")){
      return Optional.empty();
    }
    return Optional.of(url);
  }

}
