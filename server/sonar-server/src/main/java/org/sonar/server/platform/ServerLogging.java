/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import org.slf4j.LoggerFactory;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.ProcessProperties;
import org.sonar.process.logging.LogbackHelper;
import org.sonar.server.app.ServerProcessLogging;

@ServerSide
@ComputeEngineSide
public class ServerLogging {

  private final LogbackHelper helper;
  private final Configuration config;

  public ServerLogging(Configuration config) {
    this(new LogbackHelper(), config);
  }

  @VisibleForTesting
  ServerLogging(LogbackHelper helper, Configuration config) {
    this.helper = helper;
    this.config = config;
  }

  public void changeLevel(ServerProcessLogging serverProcessLogging, LoggerLevel level) {
    Level logbackLevel = Level.toLevel(level.name());
    helper.changeRoot(serverProcessLogging.getLogLevelConfig(), logbackLevel);
    LoggerFactory.getLogger(ServerLogging.class).info("Level of logs changed to {}", level);
  }

  public LoggerLevel getRootLoggerLevel() {
    return Loggers.get(Logger.ROOT_LOGGER_NAME).getLevel();
  }

  /**
   * The directory that contains log files. May not exist.
   */
  public File getLogsDir() {
    return new File(config.get(ProcessProperties.PATH_LOGS).get());
  }

}
