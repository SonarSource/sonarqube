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
package org.sonar.ce.log;

import com.google.common.annotations.VisibleForTesting;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.ce.queue.CeTask;

import static java.lang.String.format;

public class LogFileRef {

  // restricted white-list for now
  private static final Pattern FILENAME_PATTERN = Pattern.compile("^[\\w\\-]*$");
  private final String taskType;
  private final String taskUuid;

  @CheckForNull
  private final String componentUuid;

  public LogFileRef(String taskType, String taskUuid, @Nullable String componentUuid) {
    this.taskType = requireValidFilename(taskType);
    this.taskUuid = requireValidFilename(taskUuid);
    this.componentUuid = requireValidFilename(componentUuid);
  }

  @VisibleForTesting
  @CheckForNull
  static String requireValidFilename(@Nullable String s) {
    if (s != null && !FILENAME_PATTERN.matcher(s).matches()) {
      throw new IllegalArgumentException(String.format("'%s' is not a valid filename for Compute Engine logs", s));
    }
    return s;
  }

  /**
   * Path relative to the CE logs directory
   */
  public String getRelativePath() {
    if (componentUuid == null) {
      return format("%s/%s.log", taskType, taskUuid);
    }
    return format("%s/%s/%s.log", taskType, componentUuid, taskUuid);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LogFileRef that = (LogFileRef) o;
    if (!taskType.equals(that.taskType)) {
      return false;
    }
    if (!taskUuid.equals(that.taskUuid)) {
      return false;
    }
    return componentUuid == null ? (that.componentUuid == null) : componentUuid.equals(that.componentUuid);

  }

  @Override
  public int hashCode() {
    int result = taskType.hashCode();
    result = 31 * result + taskUuid.hashCode();
    result = 31 * result + (componentUuid != null ? componentUuid.hashCode() : 0);
    return result;
  }

  public static LogFileRef from(CeActivityDto dto) {
    return new LogFileRef(dto.getTaskType(), dto.getUuid(), dto.getComponentUuid());
  }

  public static LogFileRef from(CeQueueDto dto) {
    return new LogFileRef(dto.getTaskType(), dto.getUuid(), dto.getComponentUuid());
  }

  public static LogFileRef from(CeTask task) {
    return new LogFileRef(task.getType(), task.getUuid(), task.getComponentUuid());
  }
}
