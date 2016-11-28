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

import java.util.logging.LogManager;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.sonar.process.logging.LogLevelConfig;
import org.sonar.process.ProcessId;

import org.sonar.process.logging.LogDomain;
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
  }

  @Override
  protected void extendConfigure() {
    // Configure java.util.logging, used by Tomcat, in order to forward to slf4j
    LogManager.getLogManager().reset();
    SLF4JBridgeHandler.install();
  }
}
