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

import com.google.common.collect.ImmutableMap;
import org.slf4j.LoggerFactory;
import org.sonar.core.config.Logback;
import org.sonar.core.profiling.Profiling;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import java.util.Map;

import static org.apache.commons.lang.StringUtils.defaultIfEmpty;

public final class PlatformLifecycleListener implements ServletContextListener {

  private static final String CONFIG_LOG_CONSOLE = "sonar.log.console";

  private static final String LOG_COMMON_PREFIX = "%d{yyyy.MM.dd HH:mm:ss} %-5level ";
  private static final String LOG_COMMON_SUFFIX = "%msg%n";

  private static final String LOG_LOGFILE_SPECIFIC_PART = "[%logger{20}] %X ";
  private static final String LOG_FULL_SPECIFIC_PART = "%thread ";

  private static final String LOGFILE_STANDARD_LOGGING_FORMAT = LOG_COMMON_PREFIX + LOG_LOGFILE_SPECIFIC_PART + LOG_COMMON_SUFFIX;
  private static final String LOGFILE_FULL_LOGGING_FORMAT = LOG_COMMON_PREFIX + LOG_FULL_SPECIFIC_PART + LOG_LOGFILE_SPECIFIC_PART + LOG_COMMON_SUFFIX;

  private static final String CONSOLE_STANDARD_LOGGING_FORMAT = LOG_COMMON_PREFIX + LOG_COMMON_SUFFIX;
  private static final String CONSOLE_FULL_LOGGING_FORMAT = LOG_COMMON_PREFIX + LOG_FULL_SPECIFIC_PART + LOG_COMMON_SUFFIX;

  public void contextInitialized(ServletContextEvent event) {
    try {
      configureLogback(event);
      Platform.getInstance().init(event.getServletContext());
      Platform.getInstance().doStart();
    } catch (Throwable t) {
      // Tomcat 7 "limitations":
      // - server does not stop if webapp fails at startup
      // - the second listener for jruby on rails is started even if this listener fails. It generates
      // unexpected errors
      LoggerFactory.getLogger(getClass()).error("Fail to start server", t);
      stopQuietly();
      System.exit(1);
    }
  }

  private void stopQuietly() {
    try {
      Platform.getInstance().doStop();
    } catch (Exception e) {
      // ignored, but an error during startup generally prevents pico to be correctly stopped
    }
  }

  public void contextDestroyed(ServletContextEvent event) {
    Platform.getInstance().doStop();
  }

  /**
   * Configure Logback from classpath, with configuration from sonar.properties
   */
  private void configureLogback(ServletContextEvent event) {
    String configProfilingLevel = defaultIfEmpty(
        event.getServletContext().getInitParameter(Profiling.CONFIG_PROFILING_LEVEL),
        System.getProperty(Profiling.CONFIG_PROFILING_LEVEL));
    Profiling.Level profilingLevel = Profiling.Level.fromConfigString(configProfilingLevel);
    String consoleEnabled = defaultIfEmpty(defaultIfEmpty(
        event.getServletContext().getInitParameter(CONFIG_LOG_CONSOLE),
        System.getProperty(CONFIG_LOG_CONSOLE)),
        // Line below used in last resort
        "false");
    Map<String, String> variables = ImmutableMap.of(
        "RAILS_LOGGER_LEVEL", profilingLevel == Profiling.Level.FULL ? "DEBUG" : "WARN",
        "LOGFILE_LOGGING_FORMAT", profilingLevel == Profiling.Level.FULL ? LOGFILE_FULL_LOGGING_FORMAT : LOGFILE_STANDARD_LOGGING_FORMAT,
        "CONSOLE_LOGGING_FORMAT", profilingLevel == Profiling.Level.FULL ? CONSOLE_FULL_LOGGING_FORMAT : CONSOLE_STANDARD_LOGGING_FORMAT,
        "CONSOLE_ENABLED", consoleEnabled);
    Logback.configure("/org/sonar/server/platform/logback.xml", variables);
  }
}
