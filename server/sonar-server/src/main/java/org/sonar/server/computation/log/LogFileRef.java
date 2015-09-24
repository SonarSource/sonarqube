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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;

import static java.lang.String.format;

public class LogFileRef {

  private final String taskUuid;

  @CheckForNull
  private final String componentUuid;

  public LogFileRef(String taskUuid, @Nullable String componentUuid) {
    this.taskUuid = taskUuid;
    this.componentUuid = componentUuid;
  }

  /**
   * Path relative to the CE logs directory
   */
  public String getRelativePath() {
    if (componentUuid == null) {
      return format("%s.log", taskUuid);
    }
    return format("%s/%s.log", componentUuid, taskUuid);
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

    if (!taskUuid.equals(that.taskUuid)) {
      return false;
    }
    return !(componentUuid != null ? !componentUuid.equals(that.componentUuid) : that.componentUuid != null);

  }

  @Override
  public int hashCode() {
    int result = taskUuid.hashCode();
    result = 31 * result + (componentUuid != null ? componentUuid.hashCode() : 0);
    return result;
  }

  public static LogFileRef from(CeActivityDto dto) {
    return new LogFileRef(dto.getUuid(), dto.getComponentUuid());
  }

  public static LogFileRef from(CeQueueDto dto) {
    return new LogFileRef(dto.getUuid(), dto.getComponentUuid());
  }
}
