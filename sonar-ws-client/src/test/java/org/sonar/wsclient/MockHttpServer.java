/*
 * Copyright (C) 2011-2013 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.wsclient;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.io.IOUtils.write;

public class MockHttpServer {
  private Server server;
  private String responseBody;
  private int responseStatus = SC_OK;
  private String requestBody, requestPath;
  private Map requestHeaders = new HashMap();

  public void start() throws Exception {
    // 0 is random available port
    server = new Server(0);
    server.setHandler(getMockHandler());
    server.start();
  }

  public Handler getMockHandler() {
    Handler handler = new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest httpServletRequest, HttpServletResponse response) throws IOException, ServletException {
        requestPath = baseRequest.getUri().toString();
        requestBody = IOUtils.toString(baseRequest.getInputStream());
        requestHeaders.clear();
        Enumeration headerNames = baseRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
          String headerName = (String)headerNames.nextElement();
          requestHeaders.put(headerName, baseRequest.getHeader(headerName));
        }
        response.setStatus(responseStatus);
        response.setContentType("application/json;charset=utf-8");
        write(responseBody, response.getOutputStream());
        baseRequest.setHandled(true);
      }
    };
    return handler;
  }

  public void stop() {
    try {
      if (server != null) {
        server.stop();
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to stop HTTP server", e);
    }
  }

  public MockHttpServer doReturnBody(String responseBody) {
    this.responseBody = responseBody;
    return this;
  }

  public MockHttpServer doReturnStatus(int status) {
    this.responseStatus = status;
    return this;
  }

  public String requestBody() {
    return requestBody;
  }

  public String requestPath() {
    return requestPath;
  }

  public Map requestHeaders() {
    return requestHeaders;
  }

  public int getPort() {
    return server.getConnectors()[0].getLocalPort();
  }
}
