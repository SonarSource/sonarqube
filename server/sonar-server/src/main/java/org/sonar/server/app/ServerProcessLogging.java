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

import ch.qos.logback.classic.LoggerContext;
import org.sonar.process.LogbackHelper;
import org.sonar.process.Props;
import org.sonar.server.platform.ServerLogging;

import static org.sonar.process.LogbackHelper.RootLoggerConfig.newRootLoggerConfigBuilder;

public abstract class ServerProcessLogging {
  private final String processName;
  private final String threadIdFieldPattern;
  private final LogbackHelper helper = new LogbackHelper();

  protected ServerProcessLogging(String processName, String threadIdFieldPattern) {
    this.processName = processName;
    this.threadIdFieldPattern = threadIdFieldPattern;
  }

  public LoggerContext configure(Props props) {
    LoggerContext ctx = helper.getRootContext();
    ctx.reset();

    helper.enableJulChangePropagation(ctx);
    configureRootLogger(props);

    extendConfiguration(props);

    return ctx;
  }

  protected abstract void extendConfiguration(Props props);

  private void configureRootLogger(Props props) {
    LogbackHelper.RootLoggerConfig config = newRootLoggerConfigBuilder()
      .setProcessName(processName)
      .setThreadIdFieldPattern(threadIdFieldPattern)
      .setFileNamePrefix(processName)
      .build();
    String logPattern = helper.buildLogPattern(config);

    helper.configureGlobalFileLog(props, config, logPattern);
    helper.configureForSubprocessGobbler(props, logPattern);

    helper.configureRootLogLevel(props);
    ServerLogging.configureHardcodedLevels(helper);
  }

}
