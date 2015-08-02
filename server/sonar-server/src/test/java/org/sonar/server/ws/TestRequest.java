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
package org.sonar.server.ws;

import com.google.common.base.Throwables;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.server.ws.internal.ValidatingRequest;

import static com.google.common.base.Preconditions.checkNotNull;

public class TestRequest extends ValidatingRequest {

  private final Map<String, String> params = new HashMap<>();
  private String method = "GET";
  private String mimeType = "application/octet-stream";

  @Override
  protected String readParam(String key) {
    return params.get(key);
  }

  @Override
  protected InputStream readInputStreamParam(String key) {
    throw new UnsupportedOperationException("Not supported in test yet");
  }

  @Override
  public String method() {
    return method;
  }

  @Override
  public boolean hasParam(String key) {
    return params.containsKey(key);
  }

  public TestRequest setMethod(String method) {
    checkNotNull(method);
    this.method = method;
    return this;
  }

  @Override
  public String getMediaType() {
    return mimeType;
  }

  public TestRequest setMediaType(String type) {
    checkNotNull(type);
    this.mimeType = type;
    return this;
  }

  public TestRequest setParam(String key, String value) {
    checkNotNull(key);
    checkNotNull(value);
    this.params.put(key, value);
    return this;
  }

  public TestResponse execute() {
    try {
      DumbResponse response = new DumbResponse();
      action().handler().handle(this, response);
      return new TestResponse(response);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

}
