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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.sonar.ce.log.CeLogging;
import org.sonar.process.LogbackHelper;
import org.sonar.process.Props;

/**
 * Configure logback for the Compute Engine process.
 * Logs are written to console, which is forwarded to file logs/sonar.log by the app master process.
 * In addition, CE writes activity to "logs/ce_activity.log".
 */
public class CeProcessLogging extends ServerProcessLogging {

  public CeProcessLogging() {
    super("ce");
  }

  @Override
  protected void configureExtraAppenders(LoggerContext ctx, LogbackHelper helper, Props props) {
    ctx.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(CeLogging.createPerTaskAppenderConfiguration(ctx, props));
    ctx.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(CeLogging.createCeActivityAppenderConfiguration(helper, ctx, props));
  }

}
