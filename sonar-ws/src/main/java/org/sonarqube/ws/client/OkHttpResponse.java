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
package org.sonarqube.ws.client;

import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

class OkHttpResponse extends BaseResponse {

  private final Response okResponse;

  OkHttpResponse(Response okResponse) {
    this.okResponse = okResponse;
  }

  @Override
  public int code() {
    return okResponse.code();
  }

  @Override
  public String requestUrl() {
    return okResponse.request().url().toString();
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
    return okResponse.body().byteStream();
  }

  /**
   * Get stream of characters, decoded with the charset
   * of the Content-Type header. If that header is either absent or lacks a
   * charset, this will attempt to decode the response body as UTF-8.
   */
  @Override
  public Reader contentReader() {
    return okResponse.body().charStream();
  }

  /**
   * Get body content as a String. This response will be automatically closed.
   */
  @Override
  public String content() {
    ResponseBody body = okResponse.body();
    try {
      return body.string();
    } catch (IOException e) {
      throw fail(e);
    } finally {
      body.close();
    }
  }

  private RuntimeException fail(Exception e) {
    throw new IllegalStateException("Fail to read response of " + requestUrl(), e);
  }

  /**
   * Equivalent to closing contentReader or contentStream.
   */
  @Override
  public void close() {
    okResponse.close();
  }
}
