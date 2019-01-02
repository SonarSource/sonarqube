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

import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.log.Logger;

class TomcatStartupLogs {

  private final Logger log;

  TomcatStartupLogs(Logger log) {
    this.log = log;
  }

  void log(Tomcat tomcat) {
    Connector[] connectors = tomcat.getService().findConnectors();
    for (Connector connector : connectors) {
      if (StringUtils.equalsIgnoreCase(connector.getScheme(), "http")) {
        logHttp(connector);
      } else {
        throw new IllegalArgumentException("Unsupported connector: " + connector);
      }
    }
  }

  private void logHttp(Connector connector) {
    log.info(String.format("HTTP connector enabled on port %d", connector.getPort()));
  }

}
