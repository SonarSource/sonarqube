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
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.LocalConnector;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Function.identity;

class LocalWsConnector implements WsConnector {

  private final LocalConnector localConnector;

  LocalWsConnector(LocalConnector localConnector) {
    this.localConnector = localConnector;
  }

  LocalConnector localConnector() {
    return localConnector;
  }

  @Override
  public String baseUrl() {
    return "/";
  }

  @Override
  public WsResponse call(WsRequest wsRequest) {
    DefaultLocalRequest localRequest = new DefaultLocalRequest(wsRequest);
    LocalConnector.LocalResponse localResponse = localConnector.call(localRequest);
    return new ByteArrayResponse(wsRequest.getPath(), localResponse);
  }

  private static class DefaultLocalRequest implements LocalConnector.LocalRequest {
    private final WsRequest wsRequest;

    public DefaultLocalRequest(WsRequest wsRequest) {
      this.wsRequest = wsRequest;
    }

    @Override
    public String getPath() {
      return wsRequest.getPath();
    }

    @Override
    public String getMediaType() {
      return wsRequest.getMediaType();
    }

    @Override
    public String getMethod() {
      return wsRequest.getMethod().name();
    }

    @Override
    public boolean hasParam(String key) {
      return !wsRequest.getParameters().getValues(key).isEmpty();
    }

    @Override
    public String getParam(String key) {
      return wsRequest.getParameters().getValue(key);
    }

    @Override
    public List<String> getMultiParam(String key) {
      return wsRequest.getParameters().getValues(key);
    }

    @Override
    public Optional<String> getHeader(String name) {
      return wsRequest.getHeaders().getValue(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
      return wsRequest.getParameters()
          .getKeys()
          .stream()
          .collect(Collectors.toMap(
              identity(),
              k -> wsRequest.getParameters().getValues(k).toArray(new String[0])));
    }
  }

  private static class ByteArrayResponse extends BaseResponse {
    private final String path;
    private final byte[] bytes;
    private final String contentType;
    private final int code;

    ByteArrayResponse(String path, LocalConnector.LocalResponse localResponse) {
      this.path = path;
      this.bytes = localResponse.getBytes();
      this.contentType = localResponse.getMediaType();
      this.code = localResponse.getStatus();
    }

    @Override
    public String requestUrl() {
      return path;
    }

    @Override
    public int code() {
      return code;
    }

    @Override
    public String contentType() {
      return contentType;
    }

    @Override
    public InputStream contentStream() {
      return new ByteArrayInputStream(bytes);
    }

    @Override
    public Reader contentReader() {
      return new InputStreamReader(new ByteArrayInputStream(bytes), UTF_8);
    }

    @Override
    public String content() {
      return new String(bytes, UTF_8);
    }

    /**
     * Not implemented yet
     */
    @Override
    public Optional<String> header(String name) {
      return Optional.empty();
    }
  }
}
