/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.application;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

public final class StartServer {
  private static final String DEFAULT_WEB_HOST = "0.0.0.0";
  private static final int DEFAULT_WEB_PORT = 9000;
  private static final String DEFAULT_WEB_CONTEXT = "/";

  private StartServer() {
  }

  public static void main(String[] args) throws Exception {
    canCreateTemporaryFiles();
    configureHome();

    Properties configuration = getConfiguration();
    String host = configuration.getProperty("sonar.web.host", DEFAULT_WEB_HOST);
    int port = Integer.parseInt(configuration.getProperty("sonar.web.port", "" + DEFAULT_WEB_PORT));
    String context = configuration.getProperty("sonar.web.context", DEFAULT_WEB_CONTEXT);
    JettyEmbedder jetty = new JettyEmbedder(host, port, context, configuration);

    jetty.start();
    Thread.currentThread().join();
  }

  /**
   * This check is required in order to provide more meaningful message than JRuby - see SONAR-2715
   */
  private static void canCreateTemporaryFiles() {
    File file = null;
    try {
      file = File.createTempFile("sonar-check", "tmp");
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create file in temporary directory, please check existence of it and permissions: " + FileUtils.getTempDirectoryPath(), e);
    } finally {
      FileUtils.deleteQuietly(file);
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
