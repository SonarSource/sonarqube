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
package org.sonar.server.app;

import ch.qos.logback.classic.Level;
import org.sonar.process.ProcessId;
import org.sonar.process.logging.LogDomain;
import org.sonar.process.logging.LogLevelConfig;

import static org.sonar.server.platform.web.requestid.RequestIdMDCStorage.HTTP_REQUEST_ID_MDC_KEY;

/**
 * Configure logback for the Web Server process. Logs are written to file "web.log" in SQ's log directory.
 */
public class WebServerProcessLogging extends ServerProcessLogging {

  public WebServerProcessLogging() {
    super(ProcessId.WEB_SERVER, "%X{" + HTTP_REQUEST_ID_MDC_KEY + "}");
  }

  @Override
  protected void extendLogLevelConfiguration(LogLevelConfig.Builder logLevelConfigBuilder) {
    logLevelConfigBuilder.levelByDomain("sql", ProcessId.WEB_SERVER, LogDomain.SQL);
    logLevelConfigBuilder.levelByDomain("es", ProcessId.WEB_SERVER, LogDomain.ES);
    logLevelConfigBuilder.levelByDomain("auth.event", ProcessId.WEB_SERVER, LogDomain.AUTH_EVENT);
    JMX_RMI_LOGGER_NAMES.forEach(loggerName -> logLevelConfigBuilder.levelByDomain(loggerName, ProcessId.WEB_SERVER, LogDomain.JMX));

    logLevelConfigBuilder.offUnlessTrace("org.apache.catalina.core.ContainerBase");
    logLevelConfigBuilder.offUnlessTrace("org.apache.catalina.core.StandardContext");
    logLevelConfigBuilder.offUnlessTrace("org.apache.catalina.core.StandardService");

    LOGGER_NAMES_TO_TURN_OFF.forEach(loggerName -> logLevelConfigBuilder.immutableLevel(loggerName, Level.OFF));
  }

  @Override
  protected void extendConfigure() {
    // No extension needed
  }
}
