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
package org.sonarqube.tests.webhook;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.rules.ExternalResource;

import static java.util.Collections.synchronizedList;

/**
 * This web server listens to requests sent by webhooks
 */
class ExternalServer extends ExternalResource {
  private final Server jetty;
  private final List<PayloadRequest> payloads = synchronizedList(new ArrayList<>());

  ExternalServer() {
    jetty = new Server(0);
    jetty.setHandler(new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
        throws IOException {

        if ("POST".equalsIgnoreCase(request.getMethod())) {
          String json = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
          Map<String, String> httpHeaders = new HashMap<>();
          Enumeration<String> headerNames = request.getHeaderNames();
          while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            httpHeaders.put(key, request.getHeader(key));
          }
          payloads.add(new PayloadRequest(target, httpHeaders, json));
        }

        response.setStatus(target.equals("/fail") ? 500 : 200);
        baseRequest.setHandled(true);
      }
    });
  }

  @Override
  protected void before() throws Throwable {
    jetty.start();
  }

  @Override
  protected void after() {
    try {
      jetty.stop();
    } catch (Exception e) {
      throw new IllegalStateException("Cannot stop Jetty");
    }
  }

  List<PayloadRequest> getPayloadRequests() {
    return payloads;
  }

  List<PayloadRequest> getPayloadRequestsOnPath(String path) {
    return payloads.stream().filter(p -> p.getPath().equals(path)).collect(Collectors.toList());
  }

  String urlFor(String path) {
    return jetty.getURI().resolve(path).toString();
  }

  void clear() {
    payloads.clear();
  }
}
