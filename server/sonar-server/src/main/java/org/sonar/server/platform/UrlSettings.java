/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.sonar.api.CoreProperties.SERVER_BASE_URL;
import static org.sonar.server.app.TomcatContexts.PROPERTY_CONTEXT;

@ComputeEngineSide
@ServerSide
public class UrlSettings {

  private static final int DEFAULT_PORT = 9000;
  private static final int DEFAULT_HTTP_PORT = 80;
  private static final String ALL_IPS_HOST = "0.0.0.0";

  private final Configuration config;
  // cached, so can't change at runtime
  private final String contextPath;

  public UrlSettings(Configuration config) {
    this.config = config;
    this.contextPath = config.get(PROPERTY_CONTEXT).orElse("")
      // Remove trailing slashes
      .replaceFirst("(\\/+)$", "");
  }

  public String getBaseUrl() {
    String url = config.get(SERVER_BASE_URL).orElse("");
    if (isEmpty(url)) {
      url = computeBaseUrl();
    }
    return url;
  }

  public String getContextPath() {
    return contextPath;
  }

  public boolean isSecured() {
    return getBaseUrl().startsWith("https://");
  }

  private String computeBaseUrl() {
    String host = config.get("sonar.web.host").orElse("");
    int port = config.getInt("sonar.web.port").orElse(0);
    String context = config.get("sonar.web.context").orElse("");

    StringBuilder res = new StringBuilder();
    res.append("http://");
    appendHost(host, res);
    appendPort(port, res);
    appendContext(context, res);

    return res.toString();
  }

  private static void appendHost(String host, StringBuilder res) {
    if (isEmpty(host) || ALL_IPS_HOST.equals(host)) {
      res.append("localhost");
    } else {
      res.append(host);
    }
  }

  private static void appendPort(int port, StringBuilder res) {
    if (port < 1) {
      res.append(':').append(DEFAULT_PORT);
    } else if (port != DEFAULT_HTTP_PORT) {
      res.append(':').append(port);
    }
  }

  private static void appendContext(String context, StringBuilder res) {
    if (isNotEmpty(context)) {
      res.append(context);
    }
  }
}
