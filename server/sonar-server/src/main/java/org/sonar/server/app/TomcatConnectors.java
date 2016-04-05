/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.app;

import com.google.common.base.Predicates;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.sonar.process.Props;

import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;

/**
 * Configuration of Tomcat connectors
 */
class TomcatConnectors {

  public static final int DISABLED_PORT = -1;
  public static final String HTTP_PROTOCOL = "HTTP/1.1";
  public static final String AJP_PROTOCOL = "AJP/1.3";
  public static final int MAX_HTTP_HEADER_SIZE_BYTES = 48 * 1024;

  private TomcatConnectors() {
    // only static stuff
  }

  static void configure(Tomcat tomcat, Props props) {
    List<Connector> connectors = from(asList(newHttpConnector(props), newAjpConnector(props)))
      .filter(Predicates.notNull())
      .toList();

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
    Set<Integer> ports = new HashSet<>();
    for (Connector connector : connectors) {
      int port = connector.getPort();
      if (ports.contains(port)) {
        throw new IllegalStateException(String.format("HTTP and AJP must not use the same port %d", port));
      }
      ports.add(port);
    }
  }

  @CheckForNull
  private static Connector newHttpConnector(Props props) {
    Connector connector = null;
    // Not named "sonar.web.http.port" to keep backward-compatibility
    int port = props.valueAsInt("sonar.web.port", 9000);
    if (port > DISABLED_PORT) {
      connector = newConnector(props, HTTP_PROTOCOL, "http");
      configureMaxHttpHeaderSize(connector);
      connector.setPort(port);
    }
    return connector;
  }

  @CheckForNull
  private static Connector newAjpConnector(Props props) {
    int port = props.valueAsInt("sonar.ajp.port", DISABLED_PORT);
    if (port > DISABLED_PORT) {
      Connector connector = newConnector(props, AJP_PROTOCOL, "http");
      connector.setPort(port);
      return connector;
    }
    return null;
  }

  /**
   * HTTP header must be at least 48kb  to accommodate the authentication token used for
   * negotiate protocol of windows authentication.
   */
  private static void configureMaxHttpHeaderSize(Connector connector) {
    setConnectorAttribute(connector, "maxHttpHeaderSize", MAX_HTTP_HEADER_SIZE_BYTES);
  }

  private static Connector newConnector(Props props, String protocol, String scheme) {
    Connector connector = new Connector(protocol);
    connector.setURIEncoding("UTF-8");
    connector.setProperty("address", props.value("sonar.web.host", "0.0.0.0"));
    connector.setProperty("socket.soReuseAddress", "true");
    configurePool(props, connector, scheme);
    configureCompression(connector);
    return connector;
  }

  private static void configurePool(Props props, Connector connector, String scheme) {
    connector.setProperty("acceptorThreadCount", String.valueOf(2));
    connector.setProperty("minSpareThreads", String.valueOf(props.valueAsInt("sonar.web." + scheme + ".minThreads", 5)));
    connector.setProperty("maxThreads", String.valueOf(props.valueAsInt("sonar.web." + scheme + ".maxThreads", 50)));
    connector.setProperty("acceptCount", String.valueOf(props.valueAsInt("sonar.web." + scheme + ".acceptCount", 25)));
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
}
