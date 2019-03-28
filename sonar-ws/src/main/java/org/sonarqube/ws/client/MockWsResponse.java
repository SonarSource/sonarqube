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
package org.sonarqube.ws.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sonarqube.ws.MediaTypes;

import static java.util.Objects.requireNonNull;

public class MockWsResponse extends BaseResponse {

  private static final String CONTENT_TYPE_HEADER = "Content-Type";

  private int code = HttpURLConnection.HTTP_OK;
  private String requestUrl;
  private byte[] content;
  private final Map<String, String> headers = new HashMap<>();

  @Override
  public int code() {
    return code;
  }

  public MockWsResponse setCode(int code) {
    this.code = code;
    return this;
  }

  @Override
  public String contentType() {
    return requireNonNull(headers.get(CONTENT_TYPE_HEADER));
  }

  @Override
  public Optional<String> header(String name) {
    return Optional.ofNullable(headers.get(name));
  }

  public MockWsResponse setContentType(String contentType) {
    headers.put(CONTENT_TYPE_HEADER, contentType);
    return this;
  }

  public MockWsResponse setRequestUrl(String requestUrl) {
    this.requestUrl = requestUrl;
    return this;
  }

  public MockWsResponse setContent(byte[] b) {
    this.content = b;
    return this;
  }

  public MockWsResponse setContent(String s) {
    this.content = s.getBytes(StandardCharsets.UTF_8);
    return this;
  }

  @Override
  public boolean hasContent() {
    return content != null;
  }

  @Override
  public String requestUrl() {
    requireNonNull(requestUrl);
    return requestUrl;
  }

  @Override
  public InputStream contentStream() {
    requireNonNull(content);
    return new ByteArrayInputStream(content);
  }

  @Override
  public Reader contentReader() {
    requireNonNull(content);
    return new StringReader(new String(content, StandardCharsets.UTF_8));
  }

  @Override
  public String content() {
    requireNonNull(content);
    return new String(content, StandardCharsets.UTF_8);
  }

  public static MockWsResponse createJson(String json) {
    return new MockWsResponse()
      .setContentType(MediaTypes.JSON)
      .setContentType(json);
  }
}
