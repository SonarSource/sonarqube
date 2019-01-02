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

import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.sonar.server.authentication.event.AuthenticationException;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.sonar.server.authentication.Cookies.newCookieBuilder;
import static org.sonar.server.authentication.event.AuthenticationEvent.Method;
import static org.sonar.server.authentication.event.AuthenticationEvent.Source;

public class JwtCsrfVerifier {

  private static final String CSRF_STATE_COOKIE = "XSRF-TOKEN";
  private static final String CSRF_HEADER = "X-XSRF-TOKEN";

  private static final Set<String> UPDATE_METHODS = ImmutableSet.of("POST", "PUT", "DELETE");
  private static final String API_URL = "/api";

  public String generateState(HttpServletRequest request, HttpServletResponse response, int timeoutInSeconds) {
    // Create a state token to prevent request forgery.
    // Store it in the cookie for later validation.
    String state = new BigInteger(130, new SecureRandom()).toString(32);
    response.addCookie(newCookieBuilder(request).setName(CSRF_STATE_COOKIE).setValue(state).setHttpOnly(false).setExpiry(timeoutInSeconds).build());
    return state;
  }

  public void verifyState(HttpServletRequest request, @Nullable String csrfState, @Nullable String login) {
    if (!shouldRequestBeChecked(request)) {
      return;
    }

    String failureCause = checkCsrf(csrfState, request.getHeader(CSRF_HEADER));
    if (failureCause != null) {
      throw AuthenticationException.newBuilder()
        .setSource(Source.local(Method.JWT))
        .setLogin(login)
        .setMessage(failureCause)
        .build();
    }
  }

  @CheckForNull
  private static String checkCsrf(@Nullable String csrfState, @Nullable String stateInHeader) {
    if (isBlank(csrfState)) {
      return "Missing reference CSRF value";
    }
    if (!StringUtils.equals(csrfState, stateInHeader)) {
      return "Wrong CSFR in request";
    }
    return null;
  }

  public void refreshState(HttpServletRequest request, HttpServletResponse response, String csrfState, int timeoutInSeconds) {
    response.addCookie(newCookieBuilder(request).setName(CSRF_STATE_COOKIE).setValue(csrfState).setHttpOnly(false).setExpiry(timeoutInSeconds).build());
  }

  public void removeState(HttpServletRequest request, HttpServletResponse response) {
    response.addCookie(newCookieBuilder(request).setName(CSRF_STATE_COOKIE).setValue(null).setHttpOnly(false).setExpiry(0).build());
  }

  private static boolean shouldRequestBeChecked(HttpServletRequest request) {
    if (UPDATE_METHODS.contains(request.getMethod())) {
      String path = request.getRequestURI().replaceFirst(request.getContextPath(), "");
      return path.startsWith(API_URL);
    }
    return false;
  }

}
