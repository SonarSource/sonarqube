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
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;

class Connectors {

  private static final int DISABLED_PORT = -1;
  static final String HTTP_PROTOCOL = "HTTP/1.1";

  static void configure(Tomcat tomcat, Props props) {
    configureShutdown(tomcat, props);
    configureConnectors(tomcat, props);
  }

  private static void configureConnectors(Tomcat tomcat, Props props) {
    List<Connector> connectors = new ArrayList<Connector>();
    connectors.addAll(Arrays.asList(newHttpConnector(props), newHttpsConnector(props)));
    connectors.removeAll(Collections.singleton(null));

    verify(connectors);

    tomcat.setConnector(connectors.get(0));
    for (Connector connector : connectors) {
      tomcat.getService().addConnector(connector);
    }
  }

  private static void verify(List<Connector> connectors) {
    if (connectors.isEmpty()) {
      throw new IllegalStateException("HTTP connectors are disabled");
    }
    Set<Integer> ports = new HashSet<Integer>();
    for (Connector connector : connectors) {
      int port = connector.getPort();
      if (ports.contains(port)) {
        throw new IllegalStateException(String.format("HTTP and HTTPS must not use the same port %d", port));
      }
      ports.add(port);
    }
  }

  private static void configureShutdown(Tomcat tomcat, Props props) {
    String shutdownToken = props.of("sonar.web.shutdown.token");
    Integer shutdownPort = props.intOf("sonar.web.shutdown.port");
    if (shutdownToken != null && !"".equals(shutdownToken) && shutdownPort != null) {
      tomcat.getServer().setPort(shutdownPort);
      tomcat.getServer().setShutdown(shutdownToken);
      info("Shutdown command is enabled on port " + shutdownPort);
    }
  }

  @Nullable
  private static Connector newHttpConnector(Props props) {
    Connector connector = null;
    // Not named "sonar.web.http.port" to keep backward-compatibility
    int port = props.intOf("sonar.web.port", 9000);
    if (port > DISABLED_PORT) {
      connector = newConnector(props, HTTP_PROTOCOL, "http");
      connector.setPort(port);
      info("HTTP connector is enabled on port " + port);
    }
    return connector;
  }

  @Nullable
  private static Connector newHttpsConnector(Props props) {
    Connector connector = null;
    int port = props.intOf("sonar.web.https.port", DISABLED_PORT);
    if (port > DISABLED_PORT) {
      connector = newConnector(props, HTTP_PROTOCOL, "https");
      connector.setPort(port);
      connector.setSecure(true);
      connector.setScheme("https");
      setConnectorAttribute(connector, "keyAlias", props.of("sonar.web.https.keyAlias"));
      String keyPassword = props.of("sonar.web.https.keyPass", "changeit");
      setConnectorAttribute(connector, "keyPass", keyPassword);
      setConnectorAttribute(connector, "keystorePass", props.of("sonar.web.https.keystorePass", keyPassword));
      setConnectorAttribute(connector, "keystoreFile", props.of("sonar.web.https.keystoreFile"));
      setConnectorAttribute(connector, "keystoreType", props.of("sonar.web.https.keystoreType", "JKS"));
      setConnectorAttribute(connector, "keystoreProvider", props.of("sonar.web.https.keystoreProvider"));
      setConnectorAttribute(connector, "clientAuth", false);
      setConnectorAttribute(connector, "sslProtocol", "TLS");
      setConnectorAttribute(connector, "SSLEnabled", true);
      info("HTTPS connector is enabled on port " + port);
    }
    return connector;
  }

  private static Connector newConnector(Props props, String protocol, String scheme) {
    Connector connector = new Connector(protocol);
    connector.setURIEncoding("UTF-8");
    connector.setProperty("address", props.of("sonar.web.host"));
    configurePool(props, connector, scheme);
    configureCompression(connector);
    return connector;
  }

  private static void configurePool(Props props, Connector connector, String scheme) {
    connector.setProperty("acceptorThreadCount", String.valueOf(2));
    connector.setProperty("minSpareThreads", String.valueOf(props.intOf("sonar.web." + scheme + ".minThreads", 5)));
    connector.setProperty("maxThreads", String.valueOf(props.intOf("sonar.web." + scheme + ".maxThreads", 50)));
    connector.setProperty("acceptCount", String.valueOf(props.intOf("sonar.web." + scheme + ".acceptCount", 25)));
  }

  private static void configureCompression(Connector connector) {
    connector.setProperty("compression", "on");
    connector.setProperty("compressionMinSize", "1024");
    connector.setProperty("compressableMimeType", "text/html,text/xml,text/plain,text/css,application/json,application/javascript");
  }

  private static void setConnectorAttribute(Connector c, String key, @Nullable Object value) {
    if (value != null) {
      c.setAttribute(key, value);
    }
  }

  private static void info(String message) {
    LoggerFactory.getLogger(Connectors.class).info(message);
  }
}
