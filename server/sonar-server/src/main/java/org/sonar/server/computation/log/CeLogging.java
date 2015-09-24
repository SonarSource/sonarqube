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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.log4j.MDC;
import org.sonar.api.config.Settings;
import org.sonar.process.ProcessProperties;
import org.sonar.server.computation.CeTask;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;

public class CeLogging {

  private final File logsDir;

  public CeLogging(Settings settings) {
    String dataDir = settings.getString(ProcessProperties.PATH_DATA);
    checkArgument(dataDir != null, "Property %s is not set", ProcessProperties.PATH_DATA);
    this.logsDir = CeFileAppenderFactory.logsDirFromDataDir(new File(dataDir));
  }

  @VisibleForTesting
  CeLogging(File logsDir) {
    this.logsDir = logsDir;
  }

  public Optional<File> getFile(LogFileRef ref) {
    File logFile = new File(logsDir, ref.getRelativePath());
    if (logFile.exists()) {
      return Optional.of(logFile);
    }
    return Optional.absent();
  }

  /**
   * Initialize logging of a Compute Engine task. Must be called
   * before first writing of log.
   */
  public void initTask(CeTask task) {
    LogFileRef ref = LogFileRef.from(task);
    MDC.put(CeFileAppenderFactory.MDC_LOG_PATH, ref.getRelativePath());
  }

  /**
   * Clean-up the logging of a task. Must be called after the last writing
   * of log.
   */
  public void clearTask(CeTask task) {
    MDC.clear();

    LogFileRef ref = LogFileRef.from(task);
    deleteSiblings(ref);
  }

  @VisibleForTesting
  void deleteSiblings(LogFileRef ref) {
    File parentDir = new File(logsDir, ref.getRelativePath()).getParentFile();
    List<File> logFiles = newArrayList(FileUtils.listFiles(parentDir, FileFilterUtils.fileFileFilter(), FileFilterUtils.falseFileFilter()));

    if (logFiles.size() > 10) {
      Collections.sort(logFiles, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
      logFiles = logFiles.subList(0, logFiles.size() - 10);
      for (File logFile : logFiles) {
        logFile.delete();
      }
    }
  }
}
