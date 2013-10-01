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

import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

class Connectors {

  static final String PROPERTY_SHUTDOWN_TOKEN = "sonar.web.shutdown.token";
  static final String PROPERTY_SHUTDOWN_PORT = "sonar.web.shutdown.port";
  static final String PROPERTY_MIN_THREADS = "sonar.web.connections.minThreads";
  static final String PROPERTY_MAX_THREADS = "sonar.web.connections.maxThreads";
  static final String PROPERTY_ACCEPT_COUNT = "sonar.web.connections.acceptCount";

  static void configure(Tomcat tomcat, Props props) {
    tomcat.getServer().setAddress(props.of("sonar.web.host", "0.0.0.0"));
    configureShutdown(tomcat, props);

    Connector connector = new Connector("HTTP/1.1");
    connector.setPort(props.intOf("sonar.web.port", 9000));
    connector.setURIEncoding("UTF-8");
    configurePool(props, connector);
    configureCompression(connector);
    tomcat.setConnector(connector);
    tomcat.getService().addConnector(connector);
  }

  private static void configureShutdown(Tomcat tomcat, Props props) {
    String shutdownToken = props.of(PROPERTY_SHUTDOWN_TOKEN);
    Integer shutdownPort = props.intOf(PROPERTY_SHUTDOWN_PORT);
    if (shutdownToken != null && !"".equals(shutdownToken) && shutdownPort != null) {
      tomcat.getServer().setPort(shutdownPort);
      tomcat.getServer().setShutdown(shutdownToken);
    }
  }

  private static void configurePool(Props props, Connector connector) {
    connector.setProperty("acceptorThreadCount", String.valueOf(2));
    connector.setProperty("minSpareThreads", String.valueOf(props.intOf(PROPERTY_MIN_THREADS, 5)));
    connector.setProperty("maxThreads", String.valueOf(props.intOf(PROPERTY_MAX_THREADS, 50)));
    connector.setProperty("acceptCount", String.valueOf(props.intOf(PROPERTY_ACCEPT_COUNT, 25)));
  }

  private static void configureCompression(Connector connector) {
    connector.setProperty("compression", "on");
    connector.setProperty("compressionMinSize", "1024");
    connector.setProperty("compressableMimeType", "text/html,text/xml,text/plain,text/css,application/json,application/javascript");
  }
}
