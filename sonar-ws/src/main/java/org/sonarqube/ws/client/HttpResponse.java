/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonarqube.ws.client;

import com.squareup.okhttp.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;

class HttpResponse extends BaseResponse {

  private final Response okResponse;

  HttpResponse(Response okResponse) {
    this.okResponse = okResponse;
  }

  @Override
  public int code() {
    return okResponse.code();
  }

  @Override
  public String requestUrl() {
    return okResponse.request().urlString();
  }

  @Override
  public boolean hasContent() {
    return okResponse.code() != HTTP_NO_CONTENT;
  }

  @Override
  public String contentType() {
    return okResponse.header("Content-Type");
  }

  /**
   * Get stream of bytes
   */
  @Override
  public InputStream contentStream() {
    try {
      return okResponse.body().byteStream();
    } catch (IOException e) {
      throw fail(e);
    }
  }

  /**
   * Get stream of characters, decoded with the charset
   * of the Content-Type header. If that header is either absent or lacks a
   * charset, this will attempt to decode the response body as UTF-8.
   */
  @Override
  public Reader contentReader() {
    try {
      return okResponse.body().charStream();
    } catch (IOException e) {
      throw fail(e);
    }
  }

  @Override
  public String content() {
    try {
      return okResponse.body().string();
    } catch (IOException e) {
      throw fail(e);
    }
  }

  private RuntimeException fail(Exception e) {
    throw new IllegalStateException("Fail to read response of " + requestUrl(), e);
  }
}
