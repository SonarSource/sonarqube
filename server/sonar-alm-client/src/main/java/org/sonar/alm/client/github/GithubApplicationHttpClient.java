/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.alm.client.github;

import java.io.IOException;
import java.util.Optional;
import org.sonar.alm.client.github.security.AccessToken;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

@ServerSide
@ComputeEngineSide
public interface GithubApplicationHttpClient {
  /**
   * Content of the response is populated if response's HTTP code is {@link java.net.HttpURLConnection#HTTP_OK OK}.
   */
  GetResponse get(String appUrl, AccessToken token, String endPoint) throws IOException;

  /**
   * Content of the response is populated if response's HTTP code is {@link java.net.HttpURLConnection#HTTP_OK OK} or
   * {@link java.net.HttpURLConnection#HTTP_CREATED CREATED}.
   */
  Response post(String appUrl, AccessToken token, String endPoint) throws IOException;

  /**
   * Content of the response is populated if response's HTTP code is {@link java.net.HttpURLConnection#HTTP_OK OK} or
   * {@link java.net.HttpURLConnection#HTTP_CREATED CREATED}.
   *
   * Content type will be application/json; charset=utf-8
   */
  Response post(String appUrl, AccessToken token, String endPoint, String json) throws IOException;

  /**
   * Content of the response is populated if response's HTTP code is {@link java.net.HttpURLConnection#HTTP_OK OK}.
   *
   * Content type will be application/json; charset=utf-8
   */
  Response patch(String appUrl, AccessToken token, String endPoint, String json) throws IOException;

  /**
   * Content of the response is populated if response's HTTP code is {@link java.net.HttpURLConnection#HTTP_OK OK}.
   *
   * Content type will be application/json; charset=utf-8
   *
   */
  Response delete(String appUrl, AccessToken token, String endPoint) throws IOException;

  interface Response {
    /**
     * @return the HTTP code of the response.
     */
    int getCode();

    /**
     * @return the content of the response if the response had an HTTP code for which we expect a content for the current
     *         HTTP method (see {@link #get(String, AccessToken, String)} and {@link #post(String, AccessToken, String)}).
     */
    Optional<String> getContent();
  }

  interface GetResponse extends Response {
    Optional<String> getNextEndPoint();
  }
}
