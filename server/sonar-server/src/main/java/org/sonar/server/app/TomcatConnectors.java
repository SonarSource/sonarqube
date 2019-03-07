/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.app;

import javax.annotation.Nullable;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.sonar.process.Props;

import static java.lang.String.format;

/**
 * Configuration of Tomcat connectors
 */
class TomcatConnectors {

  static final String HTTP_PROTOCOL = "HTTP/1.1";
  static final int MAX_HTTP_HEADER_SIZE_BYTES = 48 * 1024;
  private static final int MAX_POST_SIZE = -1;

  private TomcatConnectors() {
    // only static stuff
  }

  static void configure(Tomcat tomcat, Props props) {
    Connector httpConnector = newHttpConnector(props);
    tomcat.getService().addConnector(httpConnector);
  }

  private static Connector newHttpConnector(Props props) {
    // Not named "sonar.web.http.port" to keep backward-compatibility
    int port = props.valueAsInt("sonar.web.port", 9000);
    if (port < 0) {
      throw new IllegalStateException(format("HTTP port '%s' is invalid", port));
    }

    Connector connector = new Connector(HTTP_PROTOCOL);
    connector.setURIEncoding("UTF-8");
    connector.setProperty("address", props.value("sonar.web.host", "0.0.0.0"));
    connector.setProperty("socket.soReuseAddress", "true");
    // see https://tomcat.apache.org/tomcat-8.5-doc/config/http.html
    connector.setProperty("relaxedQueryChars", "\"<>[\\]^`{|}");
    configurePool(props, connector);
    configureCompression(connector);
    configureMaxHttpHeaderSize(connector);
    connector.setPort(port);
    connector.setMaxPostSize(MAX_POST_SIZE);
    return connector;
  }

  /**
   * HTTP header must be at least 48kb  to accommodate the authentication token used for
   * negotiate protocol of windows authentication.
   */
  private static void configureMaxHttpHeaderSize(Connector connector) {
    setConnectorAttribute(connector, "maxHttpHeaderSize", MAX_HTTP_HEADER_SIZE_BYTES);
  }

  private static void configurePool(Props props, Connector connector) {
    connector.setProperty("acceptorThreadCount", String.valueOf(2));
    connector.setProperty("minSpareThreads", String.valueOf(props.valueAsInt("sonar.web.http.minThreads", 5)));
    connector.setProperty("maxThreads", String.valueOf(props.valueAsInt("sonar.web.http.maxThreads", 50)));
    connector.setProperty("acceptCount", String.valueOf(props.valueAsInt("sonar.web.http.acceptCount", 25)));
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
