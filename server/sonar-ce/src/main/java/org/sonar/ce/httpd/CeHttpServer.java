/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.httpd;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Properties;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.slf4j.LoggerFactory;
import org.sonar.api.Startable;
import org.sonar.process.sharedmemoryfile.DefaultProcessCommands;

import static java.lang.Integer.parseInt;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_PROCESS_INDEX;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_SHARED_PATH;

/**
 * This HTTP server exports data required for display of System Info page (and the related web service).
 * It listens on loopback address only, so it does not need to be secure (no HTTPS, no authentication).
 */
public class CeHttpServer implements Startable {
  private final Properties processProps;
  private final List<HttpAction> actions;
  private HttpServer httpServer;

  public CeHttpServer(Properties processProps, List<HttpAction> actions) {
    this.processProps = processProps;
    this.actions = actions;
  }

  @Override
  public void start() {
    try {
      this.httpServer = buildHttpServer();
      httpServer.start();
      registerServerUrl();
    } catch (IOException e) {
      throw new IllegalStateException("Can not start local HTTP server for System Info monitoring", e);
    }
  }

  private HttpServer buildHttpServer() {
    ServerBootstrap serverBootstrap = ServerBootstrap.bootstrap();
    serverBootstrap.setLocalAddress(InetAddress.getLoopbackAddress());
    actions.forEach(httpAction -> serverBootstrap.registerHandler(httpAction.getContextPath(), httpAction));
    serverBootstrap.registerHandler("/*", new NotFoundHttpRequestHandler());
    return serverBootstrap.create();
  }

  private void registerServerUrl() {
    int processNumber = parseInt(processProps.getProperty(PROPERTY_PROCESS_INDEX));
    File shareDir = new File(processProps.getProperty(PROPERTY_SHARED_PATH));
    try (DefaultProcessCommands commands = DefaultProcessCommands.secondary(shareDir, processNumber)) {
      String url = getUrl();
      commands.setHttpUrl(url);
      LoggerFactory.getLogger(getClass()).debug("System Info HTTP server listening at {}", url);
    }
  }

  @Override
  public void stop() {
    this.httpServer.stop();
  }

  // visible for testing
  String getUrl() {
    return "http://" + this.httpServer.getInetAddress().getHostAddress() + ":" + this.httpServer.getLocalPort();
  }

  private static class NotFoundHttpRequestHandler implements HttpRequestHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) {
      response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND);
    }
  }
}
