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

import ch.qos.logback.access.tomcat.LogbackValve;
import com.google.common.collect.ImmutableMap;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.sonar.core.config.Logback;
import org.sonar.core.profiling.Profiling;
import org.sonar.process.ProcessConstants;
import org.sonar.process.Props;

import java.io.File;
import java.util.Map;
import java.util.logging.LogManager;

class Logging {

  private static final String LOG_COMMON_PREFIX = "%d{yyyy.MM.dd HH:mm:ss} %-5level ";
  private static final String LOG_COMMON_SUFFIX = "%msg%n";

  private static final String LOG_LOGFILE_SPECIFIC_PART = "web[%logger{20}] %X ";
  private static final String LOG_FULL_SPECIFIC_PART = "%thread ";

  private static final String LOGFILE_STANDARD_LOGGING_FORMAT = LOG_COMMON_PREFIX + LOG_LOGFILE_SPECIFIC_PART + LOG_COMMON_SUFFIX;
  private static final String LOGFILE_FULL_LOGGING_FORMAT = LOG_COMMON_PREFIX + LOG_FULL_SPECIFIC_PART + LOG_LOGFILE_SPECIFIC_PART + LOG_COMMON_SUFFIX;

  static final String ACCESS_RELATIVE_PATH = "WEB-INF/config/logback-access.xml";
  static final String PROPERTY_ENABLE_ACCESS_LOGS = "sonar.web.accessLogs.enable";

  private Logging() {
  }

  static void init(Props props) {
    configureLogback(props);

    // Configure java.util.logging, used by Tomcat, in order to forward to slf4j
    LogManager.getLogManager().reset();
    SLF4JBridgeHandler.install();
  }

  /**
   * Configure Logback from classpath, with configuration from sonar.properties
   */
  private static void configureLogback(Props props) {
    String configProfilingLevel = props.value(Profiling.CONFIG_PROFILING_LEVEL, "NONE");
    Profiling.Level profilingLevel = Profiling.Level.fromConfigString(configProfilingLevel);
    Map<String, String> variables = ImmutableMap.of(
      ProcessConstants.PATH_LOGS, props.nonNullValue(ProcessConstants.PATH_LOGS),
      "LOGGING_FORMAT", profilingLevel == Profiling.Level.FULL ? LOGFILE_FULL_LOGGING_FORMAT : LOGFILE_STANDARD_LOGGING_FORMAT);
    Logback.configure("/org/sonar/server/platform/logback.xml", variables);
  }

  static void configure(Tomcat tomcat, Props props) {
    tomcat.setSilent(true);
    tomcat.getService().addLifecycleListener(new LifecycleLogger(LoggerFactory.getLogger(Logging.class)));
    configureLogbackAccess(tomcat, props);
  }

  private static void configureLogbackAccess(Tomcat tomcat, Props props) {
    if (props.valueAsBoolean(PROPERTY_ENABLE_ACCESS_LOGS, true)) {
      LogbackValve valve = new LogbackValve();
      valve.setQuiet(true);
      valve.setFilename(new File(props.nonNullValue(ProcessConstants.PATH_WEB), ACCESS_RELATIVE_PATH).getAbsolutePath());
      tomcat.getHost().getPipeline().addValve(valve);
    }
  }

  static class LifecycleLogger implements LifecycleListener {
    private Logger logger;

    LifecycleLogger(Logger logger) {
      this.logger = logger;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
      if ("after_start".equals(event.getType())) {
        logger.info("Web server is started");

      } else if ("after_destroy".equals(event.getType())) {
        logger.info("Web server is stopped");
      }
    }
  }

}
