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
package org.sonar.application.es;

import ch.qos.logback.classic.Level;
import java.io.File;
import java.util.Properties;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;
import org.sonar.process.logging.Log4JPropertiesBuilder;
import org.sonar.process.logging.LogLevelConfig;
import org.sonar.process.logging.RootLoggerConfig;

import static org.sonar.process.logging.RootLoggerConfig.newRootLoggerConfigBuilder;

public class EsLogging {

  public Properties createProperties(Props props, File logDir) {
    Log4JPropertiesBuilder log4JPropertiesBuilder = new Log4JPropertiesBuilder(props);
    RootLoggerConfig config = newRootLoggerConfigBuilder().setProcessId(ProcessId.ELASTICSEARCH).build();
    String logPattern = log4JPropertiesBuilder.buildLogPattern(config);

    log4JPropertiesBuilder.internalLogLevel(Level.ERROR);
    log4JPropertiesBuilder.configureGlobalFileLog(config, logDir, logPattern);
    log4JPropertiesBuilder.apply(
      LogLevelConfig.newBuilder(log4JPropertiesBuilder.getRootLoggerName())
        .rootLevelFor(ProcessId.ELASTICSEARCH)
        .build());

    return log4JPropertiesBuilder.get();
  }

}
