/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static java.net.URLDecoder.decode;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonar.server.authentication.Cookies.findCookie;
import static org.sonar.server.authentication.Cookies.newCookieBuilder;

public class OAuth2AuthenticationParametersImpl implements OAuth2AuthenticationParameters {

  private static final String AUTHENTICATION_COOKIE_NAME = "AUTH-PARAMS";
  private static final int FIVE_MINUTES_IN_SECONDS = 5 * 60;

  /**
   * The HTTP parameter that contains the path where the user should be redirect to.
   * Please note that the web context is included.
   */
  private static final String RETURN_TO_PARAMETER = "return_to";

  /**
   * This parameter is used to allow the shift of email from an existing user to the authenticating user
   */
  private static final String ALLOW_EMAIL_SHIFT_PARAMETER = "allowEmailShift";

  private static final Type JSON_MAP_TYPE = new TypeToken<HashMap<String, String>>() {
  }.getType();

  @Override
  public void init(HttpServletRequest request, HttpServletResponse response) {
    String returnTo = request.getParameter(RETURN_TO_PARAMETER);
    String allowEmailShift = request.getParameter(ALLOW_EMAIL_SHIFT_PARAMETER);
    Map<String, String> parameters = new HashMap<>();
    if (isNotBlank(returnTo)) {
      parameters.put(RETURN_TO_PARAMETER, returnTo);
    }
    if (isNotBlank(allowEmailShift)) {
      parameters.put(ALLOW_EMAIL_SHIFT_PARAMETER, allowEmailShift);
    }
    if (parameters.isEmpty()) {
      return;
    }
    response.addCookie(newCookieBuilder(request)
      .setName(AUTHENTICATION_COOKIE_NAME)
      .setValue(toJson(parameters))
      .setHttpOnly(true)
      .setExpiry(FIVE_MINUTES_IN_SECONDS)
      .build());
  }

  @Override
  public Optional<String> getReturnTo(HttpServletRequest request) {
    return getParameter(request, RETURN_TO_PARAMETER);
  }

  @Override
  public Optional<Boolean> getAllowEmailShift(HttpServletRequest request) {
    Optional<String> parameter = getParameter(request, ALLOW_EMAIL_SHIFT_PARAMETER);
    return parameter.map(Boolean::parseBoolean);
  }

  private static Optional<String> getParameter(HttpServletRequest request, String parameterKey) {
    Optional<javax.servlet.http.Cookie> cookie = findCookie(AUTHENTICATION_COOKIE_NAME, request);
    if (!cookie.isPresent()) {
      return Optional.empty();
    }

    Map<String, String> parameters = fromJson(cookie.get().getValue());
    if (parameters.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(parameters.get(parameterKey));
  }

  @Override
  public void delete(HttpServletRequest request, HttpServletResponse response) {
    response.addCookie(newCookieBuilder(request)
      .setName(AUTHENTICATION_COOKIE_NAME)
      .setValue(null)
      .setHttpOnly(true)
      .setExpiry(0)
      .build());
  }

  private static String toJson(Map<String, String> map) {
    Gson gson = new GsonBuilder().create();
    try {
      return encode(gson.toJson(map), UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Map<String, String> fromJson(String json) {
    Gson gson = new GsonBuilder().create();
    try {
      return gson.fromJson(decode(json, UTF_8.name()), JSON_MAP_TYPE);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }
}
