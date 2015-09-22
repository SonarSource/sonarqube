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
package org.sonar.server.computation.log;

import com.google.common.base.Optional;
import java.io.File;
import org.apache.log4j.MDC;
import org.sonar.api.config.Settings;
import org.sonar.process.ProcessProperties;

public class CeLogging {

  private final File logsDir;

  public CeLogging(Settings settings) {
    this.logsDir = CeFileAppenderFactory.logsDirFromDataDir(new File(settings.getString(ProcessProperties.PATH_DATA)));
  }

  public Optional<File> fileForTaskUuid(String taskUuid) {
    File logFile = new File(logsDir, CeFileAppenderFactory.logFilenameForTaskUuid(taskUuid));
    if (logFile.exists()) {
      return Optional.of(logFile);
    }
    return Optional.absent();
  }

  /**
   * Initialize logging of a Compute Engine task. Must be called
   * before first writing of log.
   */
  public void initTask(String taskUuid) {
    MDC.put(CeFileAppenderFactory.MDC_TASK_UUID, taskUuid);
  }

  /**
   * Clean-up the logging of a task. Must be called after the last writing
   * of log.
   */
  public void clearTask() {
    MDC.clear();
  }
}
