/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.pushapi;

import com.google.common.base.Throwables;
import java.util.Map;
import java.util.Optional;
import javax.servlet.AsyncContext;
import org.sonar.server.http.JavaxHttpRequest;
import org.sonar.server.ws.ServletRequest;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;

import static org.mockito.Mockito.mock;

public class TestPushRequest extends ServletRequest {

  private TestRequest testRequest = new TestRequest();

  public TestPushRequest() {
    super(mock(JavaxHttpRequest.class));
  }

  @Override
  public AsyncContext startAsync() {
    return mock(AsyncContext.class);
  }

  @Override
  public String method() {
    return testRequest.method();
  }

  @Override
  public boolean hasParam(String key) {
    return testRequest.hasParam(key);
  }

  @Override
  public Map<String, String[]> getParams() {
    return testRequest.getParams();
  }

  @Override
  public String readParam(String key) {
    return testRequest.readParam(key);
  }

  @Override
  public String getMediaType() {
    return testRequest.getMediaType();
  }

  public TestPushRequest setParam(String key, String value) {
    testRequest.setParam(key, value);
    return this;
  }

  @Override
  public Map<String, String> getHeaders() {
    return testRequest.getHeaders();
  }

  @Override
  public Optional<String> header(String name) {
    return testRequest.header(name);
  }

  public TestPushRequest setHeader(String name, String value) {
    testRequest.setHeader(name, value);
    return this;
  }

  public TestResponse execute() {
    try {
      DumbPushResponse response = new DumbPushResponse();
      action().handler().handle(this, response);
      return new TestResponse(response);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
