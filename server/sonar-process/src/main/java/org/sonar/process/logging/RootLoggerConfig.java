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
package org.sonar.process.logging;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.process.ProcessId;

import static java.util.Objects.requireNonNull;

public final class RootLoggerConfig {
  private final ProcessId processId;
  private final String threadIdFieldPattern;
  private final String nodeNameField;
  private final List<String> excludedFields;

  private RootLoggerConfig(Builder builder) {
    this.processId = requireNonNull(builder.processId);
    this.threadIdFieldPattern = builder.threadIdFieldPattern;
    this.nodeNameField = Optional.ofNullable(builder.nodeNameField).orElse("");
    this.excludedFields = Optional.ofNullable(builder.excludedFields).orElse(List.of());
  }

  public static Builder newRootLoggerConfigBuilder() {
    return new Builder();
  }

  public String getNodeNameField() {
    return nodeNameField;
  }

  public ProcessId getProcessId() {
    return processId;
  }

  public String getThreadIdFieldPattern() {
    return threadIdFieldPattern;
  }

  public List<String> getExcludedFields() {
    return excludedFields;
  }

  public static final class Builder {
    @CheckForNull
    private ProcessId processId;
    private String threadIdFieldPattern = "";
    private String nodeNameField;
    private List<String> excludedFields;

    private Builder() {
      // prevents instantiation outside RootLoggerConfig, use static factory method
    }

    public Builder setNodeNameField(@Nullable String nodeNameField) {
      this.nodeNameField = nodeNameField;
      return this;
    }

    public Builder setProcessId(ProcessId processId) {
      this.processId = processId;
      return this;
    }

    public Builder setThreadIdFieldPattern(String threadIdFieldPattern) {
      this.threadIdFieldPattern = requireNonNull(threadIdFieldPattern, "threadIdFieldPattern can't be null");
      return this;
    }

    public Builder setExcludedFields(Collection<String> excludedFields) {
      this.excludedFields = excludedFields.stream().toList();
      return this;
    }

    public RootLoggerConfig build() {
      return new RootLoggerConfig(this);
    }
  }
}
