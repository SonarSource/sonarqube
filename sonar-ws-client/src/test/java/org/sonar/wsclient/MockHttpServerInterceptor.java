/*
 * Copyright (C) 2011-2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
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

  public MockHttpServerInterceptor doReturnBody(String body) {
    server.doReturnBody(body);
    return this;
  }

  public MockHttpServerInterceptor doReturnStatus(int status) {
    server.doReturnStatus(status);
    return this;
  }

  public String requestedPath() {
    return server.requestPath();
  }

  public Map requestHeaders() {
    return server.requestHeaders();
  }

  public int port() {
    return server.getPort();
  }

  public String url() {
    return "http://localhost:" + port();
  }
}