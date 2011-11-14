/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.application;

import org.apache.commons.io.FileUtils;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.NCSARequestLog;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.ajp.Ajp13SocketConnector;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.thread.QueuedThreadPool;
import org.mortbay.xml.XmlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JettyEmbedder {

  private Server server;
  private String host;
  private int port;
  private String contextPath;
  private int ajp13Port = -1;

  public JettyEmbedder(String host, int port, String contextPath, int ajp13Port, URL configurationURL) throws Exception {
    this.host = host.trim();
    this.port = port;
    this.contextPath = contextPath;
    this.ajp13Port = ajp13Port;
    server = new Server();

    if (configurationURL == null) {
      configureProgrammatically();

    } else {
      System.setProperty("jetty.host", this.host);
      System.setProperty("jetty.port", String.valueOf(port));
      System.setProperty("jetty.context", contextPath);
      if (ajp13Port > 0) {
        System.setProperty("jetty.ajp13Port", String.valueOf(ajp13Port));
      }
      XmlConfiguration configuration = new XmlConfiguration(configurationURL);
      configuration.configure(server);
    }
  }

  /**
   * for tests
   */
  JettyEmbedder(String host, int port) throws Exception {
    this(host, port, null, 0, null);
  }

  public void start() throws Exception {
    server.start();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          server.stop();
        } catch (Exception e) {
          System.err.println("Can not stop the Jetty server");
          e.printStackTrace();
        }
      }
    });
  }

  private Server configureProgrammatically() throws URISyntaxException, IOException {
    configureServer();
    WebAppContext context = new WebAppContext(getPath("/war/sonar-server"), contextPath);
    server.addHandler(context);
    return server;
  }

  public void configureRequestLogs(String filenamePattern) {
    RequestLogHandler requestLogHandler = new RequestLogHandler();
    NCSARequestLog requestLog = new NCSARequestLog(filenamePattern);
    requestLog.setRetainDays(7);
    requestLog.setAppend(true);
    requestLog.setExtended(true);
    requestLog.setLogTimeZone("GMT");
    requestLogHandler.setRequestLog(requestLog);
    server.addHandler(requestLogHandler);
  }

  private void configureServer() throws URISyntaxException {
    QueuedThreadPool threadPool = new QueuedThreadPool();
    threadPool.setMinThreads(5);
    threadPool.setMaxThreads(50);
    threadPool.setLowThreads(10);
    server.setThreadPool(threadPool);
    SelectChannelConnector connector = new SelectChannelConnector();
    connector.setHost(host);
    connector.setPort(port);
    connector.setStatsOn(false);
    connector.setAcceptors(2);
    connector.setConfidentialPort(8443);
    if (ajp13Port > 0) {
      System.out.println("AJP13 connector is on port " + ajp13Port);
      Connector ajpConnector = new Ajp13SocketConnector();
      ajpConnector.setPort(ajp13Port);
      server.addConnector(ajpConnector);
    }
    server.addConnector(connector);
    server.setStopAtShutdown(true);
    server.setSendServerVersion(false);
    server.setSendDateHeader(true);
    server.setGracefulShutdown(1000);

  }

  final String getPluginsClasspath(String pluginsPathFromClassloader) throws URISyntaxException, IOException {
    final URL resource = getClass().getResource(pluginsPathFromClassloader);
    if (resource != null) {
      File pluginsDir = new File(resource.toURI());
      List<String> paths = new ArrayList<String>();
      paths.add(pluginsDir.getCanonicalPath() + System.getProperty("file.separator"));

      Collection<File> files = FileUtils.listFiles(pluginsDir, new String[]{"jar"}, false);
      if (files != null) {
        for (File file : files) {
          paths.add(file.getCanonicalPath());
        }
      }
      return join(paths, ",");
    }
    return null;
  }

  private String join(List<String> paths, String separator) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (String path : paths) {
      if (!first) {
        sb.append(separator);
      }
      sb.append(path);
      first = false;
    }
    return sb.toString();
  }

  private String getPath(String resourcePath) throws URISyntaxException {
    URL resource = getClass().getResource(resourcePath);
    if (resource != null) {
      return resource.toURI().toString();
    }
    return null;
  }

  Server getServer() {
    return server;
  }

  @Override
  public String toString() {
    return new StringBuilder().append("http://").append(host).append(":").append(port).append(contextPath).toString();
  }
}
