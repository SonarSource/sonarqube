/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.process.logging;

import javax.annotation.CheckForNull;
import org.sonar.process.ProcessId;

import static java.util.Objects.requireNonNull;

public final class RootLoggerConfig {
  private final ProcessId processId;
  private final String threadIdFieldPattern;

  private RootLoggerConfig(Builder builder) {
    this.processId = requireNonNull(builder.processId);
    this.threadIdFieldPattern = builder.threadIdFieldPattern;
  }

  public static Builder newRootLoggerConfigBuilder() {
    return new Builder();
  }

  ProcessId getProcessId() {
    return processId;
  }

  String getThreadIdFieldPattern() {
    return threadIdFieldPattern;
  }

  public static final class Builder {
    @CheckForNull
    private ProcessId processId;
    private String threadIdFieldPattern = "";

    private Builder() {
      // prevents instantiation outside RootLoggerConfig, use static factory method
    }

    public Builder setProcessId(ProcessId processId) {
      this.processId = processId;
      return this;
    }

    public Builder setThreadIdFieldPattern(String threadIdFieldPattern) {
      this.threadIdFieldPattern = requireNonNull(threadIdFieldPattern, "threadIdFieldPattern can't be null");
      return this;
    }

    public RootLoggerConfig build() {
      return new RootLoggerConfig(this);
    }
  }
}
