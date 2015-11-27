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

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;
import static org.sonarqube.ws.client.WsRequest.Method.GET;
import static org.sonarqube.ws.client.WsRequest.Method.POST;

public class WsRequest {
  private final Map<String, Object> params = new HashMap<>();
  private Method method = Method.GET;
  private MediaType mimeType = MediaType.JSON;
  private String endpoint;

  private WsRequest(String endpoint) {
    this.endpoint = endpoint;
  }

  public static WsRequest newPostRequest(String endpoint) {
    return new WsRequest(endpoint)
      .setMethod(POST);
  }

  public static WsRequest newGetRequest(String endpoint) {
    return new WsRequest(endpoint)
      .setMethod(GET);
  }

  public Method getMethod() {
    return method;
  }

  private WsRequest setMethod(Method method) {
    this.method = method;
    return this;
  }

  public MediaType getMediaType() {
    return mimeType;
  }

  public WsRequest setMediaType(MediaType type) {
    requireNonNull(type);
    this.mimeType = type;
    return this;
  }

  public WsRequest setParam(String key, @Nullable Object value) {
    requireNonNull(key, "a WS parameter key cannot be null");
    if (value != null) {
      this.params.put(key, value);
    } else {
      this.params.remove(key);
    }

    return this;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public Map<String, Object> getParams() {
    return params;
  }

  public enum Method {
    GET, POST
  }

  public enum MediaType {
    PROTOBUF, JSON, TEXT
  }
}
