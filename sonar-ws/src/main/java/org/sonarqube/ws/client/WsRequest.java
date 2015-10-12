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

import static com.google.common.base.Preconditions.checkNotNull;

public class WsRequest {
  private final Map<String, Object> params = new HashMap<>();
  private Method method = Method.GET;
  private MediaType mimeType = MediaType.JSON;
  private String url;

  public WsRequest(String url) {
    this.url = url;
  }

  public Method getMethod() {
    return method;
  }

  public WsRequest setMethod(Method method) {
    checkNotNull(method);
    this.method = method;
    return this;
  }

  public MediaType getMediaType() {
    return mimeType;
  }

  public WsRequest setMediaType(MediaType type) {
    checkNotNull(type);
    this.mimeType = type;
    return this;
  }

  public WsRequest setParam(String key, Object value) {
    checkNotNull(key);
    checkNotNull(value);
    this.params.put(key, value);
    return this;
  }

  public String getUrl() {
    return url;
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
