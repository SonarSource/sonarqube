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

package org.sonar.core.i18n;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.BatchExtension;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.internal.WorkDuration;
import org.sonar.api.utils.internal.WorkDurationFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class WorkDurationFormatter implements ServerComponent, BatchExtension {

  private final WorkDurationFactory workDurationFactory;

  public WorkDurationFormatter(WorkDurationFactory workDurationFactory) {
    this.workDurationFactory = workDurationFactory;
  }

  public List<Result> format(long durationInMinutes) {
    if (durationInMinutes == 0) {
      return newArrayList(new Result("0", null));
    }
    boolean isNegative = durationInMinutes < 0;
    Long absDuration = Math.abs(durationInMinutes);
    return format(workDurationFactory.createFromMinutes(absDuration), isNegative);
  }

  private List<Result> format(WorkDuration workDuration, boolean isNegative) {
    List<Result> results = newArrayList();
    if (workDuration.days() > 0) {
      results.add(message("work_duration.x_days", isNegative ? -1 * workDuration.days() : workDuration.days()));
    }
    if (workDuration.hours() > 0 && workDuration.days() < 10) {
      addSpaceIfNeeded(results);
      results.add(message("work_duration.x_hours", isNegative && results.isEmpty() ? -1 * workDuration.hours() : workDuration.hours()));
    }
    if (workDuration.minutes() > 0 && workDuration.hours() < 10 && workDuration.days() == 0) {
      addSpaceIfNeeded(results);
      results.add(message("work_duration.x_minutes", isNegative && results.isEmpty() ? -1 * workDuration.minutes() : workDuration.minutes()));
    }
    return results;
  }

  private void addSpaceIfNeeded(List<Result> results){
    if (!results.isEmpty()) {
      results.add(new Result(" ", null));
    }
  }

  private Result message(String key, @CheckForNull Object parameter) {
    return new Result(key, parameter);
  }

  static class Result {
    private String key;
    private Object value;

    Result(String key, @Nullable Object value) {
      this.key = key;
      this.value = value;
    }

    String key() {
      return key;
    }

    @CheckForNull
    Object value() {
      return value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Result result = (Result) o;

      if (key != null ? !key.equals(result.key) : result.key != null) {
        return false;
      }
      if (value != null ? !value.equals(result.value) : result.value != null) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = key != null ? key.hashCode() : 0;
      result = 31 * result + (value != null ? value.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return new ReflectionToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).toString();
    }
  }
}
