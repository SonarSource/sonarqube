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
package org.sonar.server.platform;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.io.File;
import java.util.Set;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;
import org.sonar.process.LogbackHelper;
import org.sonar.process.ProcessProperties;

@ServerSide
public class ServerLogging {

  public static final Set<LoggerLevel> ALLOWED_ROOT_LOG_LEVELS = Sets.immutableEnumSet(LoggerLevel.TRACE, LoggerLevel.DEBUG, LoggerLevel.INFO);

  private final LogbackHelper helper = new LogbackHelper();
  private final Settings settings;

  public ServerLogging(Settings settings) {
    this.settings = settings;
  }

  public void changeLevel(LoggerLevel level) {
    configureLevels(helper, level);
    LoggerFactory.getLogger(ServerLogging.class).info("Level of logs changed to {}", level);
  }

  public LoggerLevel getRootLoggerLevel() {
    return Loggers.get(Logger.ROOT_LOGGER_NAME).getLevel();
  }

  public static void configureLevels(LogbackHelper helper, LoggerLevel level) {
    Preconditions.checkArgument(ALLOWED_ROOT_LOG_LEVELS.contains(level), "%s log level is not supported (allowed levels are %s)", level, ALLOWED_ROOT_LOG_LEVELS);
    helper.configureLogger(Logger.ROOT_LOGGER_NAME, Level.toLevel(level.name()));
    helper.configureLogger("rails", Level.WARN);
    helper.configureLogger("org.apache.ibatis", Level.WARN);
    helper.configureLogger("java.sql", Level.WARN);
    helper.configureLogger("java.sql.ResultSet", Level.WARN);
    helper.configureLogger("org.sonar.MEASURE_FILTER", Level.WARN);
    helper.configureLogger("org.elasticsearch", Level.INFO);
    helper.configureLogger("org.elasticsearch.node", Level.INFO);
    helper.configureLogger("org.elasticsearch.http", Level.INFO);
    helper.configureLogger("ch.qos.logback", Level.WARN);
    helper.configureLogger("org.apache.catalina", Level.INFO);
    helper.configureLogger("org.apache.coyote", Level.INFO);
    helper.configureLogger("org.apache.jasper", Level.INFO);
    helper.configureLogger("org.apache.tomcat", Level.INFO);
  }

  /**
   * The directory that contains log files. May not exist.
   */
  public File getLogsDir() {
    return new File(settings.getString(ProcessProperties.PATH_LOGS));
  }

  /**
   * The file sonar.log, may not exist.
   */
  public File getCurrentLogFile() {
    return new File(getLogsDir(), "sonar.log");
  }

}
