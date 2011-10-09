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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

public final class StartServer {
  private static final String DEFAULT_WEB_HOST = "0.0.0.0";
  private static final int DEFAULT_WEB_PORT = 9000;
  private static final String DEFAULT_WEB_CONTEXT = "/";
  private static final int DEFAULT_AJP13_PORT = -1;

  private StartServer() {
  }

  public static void main(String[] args) throws Exception {
    configureHome();

    Properties configuration = getConfiguration();
    String host = configuration.getProperty("sonar.web.host", DEFAULT_WEB_HOST);
    int port = Integer.parseInt(configuration.getProperty("sonar.web.port", "" + DEFAULT_WEB_PORT));
    String context = configuration.getProperty("sonar.web.context", DEFAULT_WEB_CONTEXT);
    int ajp13Port = Integer.parseInt(configuration.getProperty("sonar.ajp13.port", "" + DEFAULT_AJP13_PORT));
    JettyEmbedder jetty = new JettyEmbedder(host, port, context, ajp13Port, StartServer.class.getResource("/jetty.xml"));
    configureRequestLogs(jetty, configuration);

    jetty.start();
    Thread.currentThread().join();
  }

  private static void configureRequestLogs(JettyEmbedder jetty, Properties configuration) {
    String filenamePattern = configuration.getProperty("sonar.web.jettyRequestLogs");
    if (filenamePattern != null) {
      jetty.configureRequestLogs(filenamePattern);
    }
  }

  private static Properties getConfiguration() throws IOException {
    Properties properties = new Properties();
    properties.load(StartServer.class.getResourceAsStream("/conf/sonar.properties"));
    return properties;
  }

  private static void configureHome() throws URISyntaxException {
    File confFile = new File(StartServer.class.getResource("/conf/sonar.properties").toURI());
    System.setProperty("SONAR_HOME" /* see constant org.sonar.server.platform.SonarHome.PROPERTY */,
        confFile.getParentFile().getParentFile().getAbsolutePath());
  }
}
