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
package org.sonar.server.app;

import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.Props;

import static java.lang.String.format;
import static org.sonar.process.ProcessProperties.Property.WEB_HOST;
import static org.sonar.process.ProcessProperties.Property.WEB_HTTP_ACCEPT_COUNT;
import static org.sonar.process.ProcessProperties.Property.WEB_HTTP_KEEP_ALIVE_TIMEOUT;
import static org.sonar.process.ProcessProperties.Property.WEB_HTTP_MAX_THREADS;
import static org.sonar.process.ProcessProperties.Property.WEB_HTTP_MIN_THREADS;
import static org.sonar.process.ProcessProperties.Property.WEB_PORT;

public class TomcatHttpConnectorFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(TomcatHttpConnectorFactory.class);

  static final String HTTP_PROTOCOL = "HTTP/1.1";
  // Max HTTP headers size must be 48kb to accommodate the authentication token used for negotiate protocol of windows authentication.
  static final int MAX_HTTP_HEADER_SIZE_BYTES = 48 * 1024;
  private static final int MAX_POST_SIZE = -1;

  public Connector createConnector(Props props) {
    Connector connector = new Connector(HTTP_PROTOCOL);
    connector.setURIEncoding("UTF-8");
    connector.setProperty("address", props.value(WEB_HOST.getKey(), "0.0.0.0"));
    connector.setProperty("socket.soReuseAddress", "true");
    // See Tomcat configuration reference: https://tomcat.apache.org/tomcat-9.0-doc/config/http.html
    connector.setProperty("relaxedQueryChars", "\"<>[\\]^`{|}");
    connector.setProperty("maxHttpHeaderSize", String.valueOf(MAX_HTTP_HEADER_SIZE_BYTES));
    connector.setMaxPostSize(MAX_POST_SIZE);
    configurePort(connector, props);
    configurePool(props, connector);
    configureCompression(connector);
    return connector;
  }

  private static void configurePort(Connector connector, Props props) {
    int port = props.valueAsInt(WEB_PORT.getKey(), 9000);
    if (port < 0) {
      throw new IllegalStateException(format("HTTP port %s is invalid", port));
    }
    connector.setPort(port);
    LOGGER.info("Starting Tomcat on port {}", connector.getPort());
  }

  private static void configurePool(Props props, Connector connector) {
    connector.setProperty("minSpareThreads", String.valueOf(props.valueAsInt(WEB_HTTP_MIN_THREADS.getKey(), 5)));
    connector.setProperty("maxThreads", String.valueOf(props.valueAsInt(WEB_HTTP_MAX_THREADS.getKey(), 50)));
    connector.setProperty("acceptCount", String.valueOf(props.valueAsInt(WEB_HTTP_ACCEPT_COUNT.getKey(), 25)));
    connector.setProperty("keepAliveTimeout", String.valueOf(props.valueAsInt(WEB_HTTP_KEEP_ALIVE_TIMEOUT.getKey(), 60000)));
  }

  private static void configureCompression(Connector connector) {
    connector.setProperty("compression", "on");
    connector.setProperty("compressionMinSize", "1024");
    connector.setProperty("compressibleMimeType", "text/html,text/xml,text/plain,text/css,application/json,application/javascript,text/javascript");
  }

}
