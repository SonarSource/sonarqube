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
package org.sonar.api.task;

import com.google.common.base.Preconditions;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;

/**
 * Register and describe a {@link TaskExtension}.
 *
 * @since 3.6
 * @deprecated since 7.6
 */
@ExtensionPoint
@ScannerSide
@InstantiationStrategy(InstantiationStrategy.PER_TASK)
@Deprecated
public class TaskDefinition implements Comparable<TaskDefinition> {
  static final String KEY_PATTERN = "[a-zA-Z0-9\\-\\_]+";

  private final String key;
  private final String description;
  private final Class<? extends Task> taskClass;

  private TaskDefinition(Builder builder) {
    this.key = builder.key;
    this.description = builder.description;
    this.taskClass = builder.taskClass;
  }

  public String description() {
    return description;
  }

  public String key() {
    return key;
  }

  public Class<? extends Task> taskClass() {
    return taskClass;
  }

  @Override
  public String toString() {
    return "Task " + key + "[class=" + taskClass.getName() + ", desc=" + description + "]";
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TaskDefinition that = (TaskDefinition) o;
    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public int compareTo(TaskDefinition o) {
    return key.compareTo(o.key);
  }

  public static class Builder {
    private String key;
    private String description;
    private Class<? extends Task> taskClass;

    private Builder() {
    }

    public Builder key(String key) {
      this.key = key;
      return this;
    }

    public Builder description(String s) {
      this.description = s;
      return this;
    }

    public Builder taskClass(Class<? extends Task> taskClass) {
      this.taskClass = taskClass;
      return this;
    }

    public TaskDefinition build() {
      Preconditions.checkArgument(!StringUtils.isEmpty(key), "Task key must be set");
      Preconditions.checkArgument(Pattern.matches(KEY_PATTERN, key), "Task key '" + key + "' must match " + KEY_PATTERN);
      Preconditions.checkArgument(!StringUtils.isEmpty(description), "Description must be set for task '" + key + "'");
      Preconditions.checkArgument(taskClass != null, "Class must be set for task '" + key + "'");
      return new TaskDefinition(this);
    }
  }
}
