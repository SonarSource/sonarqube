/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.wsclient.internal;

import com.github.kevinsawicki.http.HttpRequest;

import java.util.Map;

/**
 * Not an API, please do not directly use this class.
 */
public class HttpRequestFactory {

  static final int CONNECT_TIMEOUT_MILLISECONDS = 30000;
  static final int READ_TIMEOUT_MILLISECONDS = 60000;

  private final String baseURl;
  private final String login, password;

  public HttpRequestFactory(String baseURl, String login, String password) {
    this.baseURl = baseURl;
    this.login = login;
    this.password = password;
  }

  public HttpRequest get(String wsUrl, Map<String, Object> queryParams) {
    HttpRequest request = HttpRequest.get(baseURl + wsUrl, queryParams, true);
    return prepare(request);
  }

  public HttpRequest post(String wsUrl, Map<String, Object> queryParams) {
    HttpRequest request = HttpRequest.post(baseURl + wsUrl, queryParams, true);
    return prepare(request);
  }

  private HttpRequest prepare(HttpRequest request) {
    request
      .acceptGzipEncoding()
      .uncompress(true)
      .acceptJson()
      .acceptCharset(HttpRequest.CHARSET_UTF8)
      .connectTimeout(CONNECT_TIMEOUT_MILLISECONDS)
      .readTimeout(READ_TIMEOUT_MILLISECONDS)
      .trustAllCerts()
      .trustAllCerts();
    if (login != null) {
      request.basic(login, password);
    }
    return request;
  }
}
