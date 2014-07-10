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

package org.sonar.wsclient;

import org.junit.rules.ExternalResource;

import java.util.Map;

public final class MockHttpServerInterceptor extends ExternalResource {

  private MockHttpServer server;

  @Override
  protected final void before() throws Throwable {
    server = new MockHttpServer();
    server.start();
  }

  @Override
  protected void after() {
    server.stop();
  }

  public MockHttpServerInterceptor stubResponseBody(String body) {
    server.doReturnBody(body);
    return this;
  }

  public MockHttpServerInterceptor stubStatusCode(int status) {
    server.doReturnStatus(status);
    return this;
  }

  public String requestedPath() {
    return server.requestPath();
  }

  public Map requestHeaders() {
    return server.requestHeaders();
  }

  public Map requestParams() {
    return server.requestParams();
  }

  public int port() {
    return server.getPort();
  }

  public String url() {
    return "http://localhost:" + port();
  }
}
