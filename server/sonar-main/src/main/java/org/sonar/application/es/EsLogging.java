/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.application.es;

import ch.qos.logback.classic.Level;
import java.io.File;
import java.util.Properties;
import javax.annotation.CheckForNull;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;
import org.sonar.process.logging.Log4JPropertiesBuilder;
import org.sonar.process.logging.LogLevelConfig;
import org.sonar.process.logging.RootLoggerConfig;

import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_NODE_NAME;
import static org.sonar.process.ProcessProperties.Property.LOG_CONSOLE;
import static org.sonar.process.ProcessProperties.Property.LOG_JSON_OUTPUT;
import static org.sonar.process.logging.RootLoggerConfig.newRootLoggerConfigBuilder;

public class EsLogging {

  public Properties createProperties(Props props, File logDir) {
    Log4JPropertiesBuilder log4JPropertiesBuilder = new Log4JPropertiesBuilder(props);
    RootLoggerConfig config = newRootLoggerConfigBuilder()
      .setNodeNameField(getNodeNameWhenCluster(props))
      .setProcessId(ProcessId.ELASTICSEARCH)
      .build();

    String logPattern = log4JPropertiesBuilder.buildLogPattern(config);

    return log4JPropertiesBuilder.internalLogLevel(Level.ERROR)
      .rootLoggerConfig(config)
      .logPattern(logPattern)
      .enableAllLogsToConsole(isAllLogsToConsoleEnabled(props))
      .jsonOutput(isJsonOutput(props))
      .logDir(logDir)
      .logLevelConfig(
        LogLevelConfig.newBuilder(log4JPropertiesBuilder.getRootLoggerName())
          .rootLevelFor(ProcessId.ELASTICSEARCH)
          .build())
      .build();
  }

  private static boolean isJsonOutput(Props props) {
    return props.valueAsBoolean(LOG_JSON_OUTPUT.getKey(),
      Boolean.parseBoolean(LOG_JSON_OUTPUT.getDefaultValue()));
  }

  @CheckForNull
  private static String getNodeNameWhenCluster(Props props) {
    boolean clusterEnabled = props.valueAsBoolean(CLUSTER_ENABLED.getKey(),
      Boolean.parseBoolean(CLUSTER_ENABLED.getDefaultValue()));
    return clusterEnabled ? props.value(CLUSTER_NODE_NAME.getKey(), CLUSTER_NODE_NAME.getDefaultValue()) : null;
  }

  /**
   * Finds out whether we are in testing environment (usually ITs) and logs of all processes must be forward to
   * App's System.out. This is specified by the value of property {@link ProcessProperties.Property#LOG_CONSOLE}.
   */
  private static boolean isAllLogsToConsoleEnabled(Props props) {
    return props.valueAsBoolean(LOG_CONSOLE.getKey(), false);
  }
}
