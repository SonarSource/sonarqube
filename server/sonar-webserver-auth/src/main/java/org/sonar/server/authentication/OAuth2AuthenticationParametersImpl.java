/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.api.server.http.Cookie;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;

import static java.net.URLDecoder.decode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static org.sonar.server.authentication.AuthenticationRedirection.encodeMessage;
import static org.sonar.server.authentication.Cookies.findCookie;
import static org.sonar.server.authentication.Cookies.newCookieBuilder;

public class OAuth2AuthenticationParametersImpl implements OAuth2AuthenticationParameters {

  private static final String AUTHENTICATION_COOKIE_NAME = "AUTH-PARAMS";
  private static final int FIVE_MINUTES_IN_SECONDS = 5 * 60;
  private static final Pattern VALID_RETURN_TO = Pattern.compile("^/\\w.*");

  /**
   * The HTTP parameter that contains the path where the user should be redirect to.
   * Please note that the web context is included.
   */
  private static final String RETURN_TO_PARAMETER = "return_to";

  private static final TypeToken<Map<String, String>> JSON_MAP_TYPE = new TypeToken<>() {
  };

  @Override
  public void init(HttpRequest request, HttpResponse response) {
    String returnTo = request.getParameter(RETURN_TO_PARAMETER);
    Map<String, String> parameters = new HashMap<>();
    Optional<String> sanitizeRedirectUrl = sanitizeRedirectUrl(returnTo);
    sanitizeRedirectUrl.ifPresent(s -> parameters.put(RETURN_TO_PARAMETER, s));
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
  public Optional<String> getReturnTo(HttpRequest request) {
    return getParameter(request, RETURN_TO_PARAMETER)
      .flatMap(OAuth2AuthenticationParametersImpl::sanitizeRedirectUrl);
  }

  private static Optional<String> getParameter(HttpRequest request, String parameterKey) {
    Optional<Cookie> cookie = findCookie(AUTHENTICATION_COOKIE_NAME, request);
    if (cookie.isEmpty()) {
      return empty();
    }

    Map<String, String> parameters = fromJson(cookie.get().getValue());
    if (parameters.isEmpty()) {
      return empty();
    }
    return Optional.ofNullable(parameters.get(parameterKey));
  }

  @Override
  public void delete(HttpRequest request, HttpResponse response) {
    response.addCookie(newCookieBuilder(request)
      .setName(AUTHENTICATION_COOKIE_NAME)
      .setValue(null)
      .setHttpOnly(true)
      .setExpiry(0)
      .build());
  }

  private static String toJson(Map<String, String> map) {
    Gson gson = new GsonBuilder().create();
    return encodeMessage(gson.toJson(map));
  }

  private static Map<String, String> fromJson(String json) {
    Gson gson = new GsonBuilder().create();
    try {
      return gson.fromJson(decode(json, UTF_8.name()), JSON_MAP_TYPE);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * This sanitization has been inspired by 'IsUrlLocalToHost()' method in
   * https://docs.microsoft.com/en-us/aspnet/mvc/overview/security/preventing-open-redirection-attacks
   */
  private static Optional<String> sanitizeRedirectUrl(@Nullable String url) {
    if (Strings.isNullOrEmpty(url)) {
      return empty();
    }

    String trimmedUrl = url.trim();
    boolean isValidUrl = VALID_RETURN_TO.matcher(trimmedUrl).matches();
    if (!isValidUrl) {
      return empty();
    }

    try {
      URI uri = new URI(trimmedUrl);
      String sanitizedPath = escapePathTraversalChars(uri.getPath());
      URI sanitizedUri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), sanitizedPath, uri.getQuery(), uri.getFragment());
      return Optional.of(sanitizedUri.toString());
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String escapePathTraversalChars(String path) {
    return Path.of(path).normalize().toString();
  }
}
