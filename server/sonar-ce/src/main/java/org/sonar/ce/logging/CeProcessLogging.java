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
package org.sonar.ce.logging;

import ch.qos.logback.classic.Level;
import org.sonar.process.ProcessId;
import org.sonar.process.logging.LogDomain;
import org.sonar.process.logging.LogLevelConfig;
import org.sonar.server.log.ServerProcessLogging;

import static org.sonar.ce.task.log.CeTaskLogging.MDC_CE_TASK_UUID;

/**
 * Configure logback for the Compute Engine process. Logs are written to file "ce.log" in SQ's log directory.
 */
public class CeProcessLogging extends ServerProcessLogging {

  public CeProcessLogging() {
    super(ProcessId.COMPUTE_ENGINE, "%X{" + MDC_CE_TASK_UUID + "}");
  }

  @Override
  protected void extendLogLevelConfiguration(LogLevelConfig.Builder logLevelConfigBuilder) {
    logLevelConfigBuilder.levelByDomain("sql", ProcessId.COMPUTE_ENGINE, LogDomain.SQL);
    logLevelConfigBuilder.levelByDomain("es", ProcessId.COMPUTE_ENGINE, LogDomain.ES);
    JMX_RMI_LOGGER_NAMES.forEach(loggerName -> logLevelConfigBuilder.levelByDomain(loggerName, ProcessId.COMPUTE_ENGINE, LogDomain.JMX));
    LOGGER_NAMES_TO_TURN_OFF.forEach(loggerName -> logLevelConfigBuilder.immutableLevel(loggerName, Level.OFF));
  }

  @Override
  protected void extendConfigure() {
    // nothing to do
  }
}
