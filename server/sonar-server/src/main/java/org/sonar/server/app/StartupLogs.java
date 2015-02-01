/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.app;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang.StringUtils;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11JsseProtocol;
import org.slf4j.Logger;
import org.sonar.process.Props;

class StartupLogs {

  private final Logger log;

  StartupLogs(Logger log) {
    this.log = log;
  }

  void log(Props props, Tomcat tomcat) {
    Connector[] connectors = tomcat.getService().findConnectors();
    for (Connector connector : connectors) {
      if (StringUtils.containsIgnoreCase(connector.getProtocol(), "AJP")) {
        logAjp(connector);
      } else if (StringUtils.equalsIgnoreCase(connector.getScheme(), "https")) {
        logHttps(connector, props);
      } else if (StringUtils.equalsIgnoreCase(connector.getScheme(), "http")) {
        logHttp(connector);
      } else {
        throw new IllegalArgumentException("Unsupported connector: " + connector);
      }
    }
  }

  private void logAjp(Connector connector) {
    log.info(String.format("%s connector enabled on port %d", connector.getProtocol(), connector.getPort()));
  }

  private void logHttp(Connector connector) {
    log.info(String.format("HTTP connector enabled on port %d", connector.getPort()));
  }

  private void logHttps(Connector connector, Props props) {
    StringBuilder additional = new StringBuilder();
    ProtocolHandler protocol = connector.getProtocolHandler();
    if (protocol instanceof AbstractHttp11JsseProtocol) {
      additional.append("| ciphers=");
      if (StringUtils.isBlank(props.value(Connectors.PROP_HTTPS_CIPHERS))) {
        additional.append("JVM defaults");
      } else {
        String[] ciphers = ((AbstractHttp11JsseProtocol) protocol).getCiphersUsed();
        additional.append(StringUtils.join(ciphers, ","));
      }
    }
    log.info(String.format("HTTPS connector enabled on port %d %s", connector.getPort(), additional));
  }
}
