/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.profiling;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.sonar.api.utils.System2;

class LoggingWatch extends StopWatch {

  private Logger logger;
  private System2 system;
  private long startTimeInMillis;

  LoggingWatch(Logger LOGGER) {
    this(LOGGER, System2.INSTANCE);
  }

  @VisibleForTesting
  LoggingWatch(Logger LOGGER, System2 system) {
    this.system = system;
    this.logger = LOGGER;
    this.startTimeInMillis = system.now();
  }

  @Override
  public void stop(String message, Object... args) {
    long endTimeInMillis = system.now();
    String messageToDisplay = (args.length == 0) ? message : String.format(message, args);
    logger.info("{}ms {}", Long.valueOf(endTimeInMillis - startTimeInMillis), messageToDisplay);
  }

}
