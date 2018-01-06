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
package org.sonar.server.computation.task.projectanalysis.qualitygate;

import com.google.common.base.MoreObjects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

@Immutable
public class Condition {

  public enum Operator {
    EQUALS("EQ"), NOT_EQUALS("NE"), GREATER_THAN("GT"), LESS_THAN("LT");

    private final String dbValue;

    Operator(String dbValue) {
      this.dbValue = dbValue;
    }

    public String getDbValue() {
      return dbValue;
    }
  }

  private final Metric metric;
  private final Operator operator;
  @CheckForNull
  private final String warningThreshold;
  @CheckForNull
  private final String errorThreshold;
  private final boolean hasPeriod;

  public Condition(Metric metric, String operator,
    @Nullable String errorThreshold, @Nullable String warningThreshold,
    boolean hasPeriod) {
    this.metric = requireNonNull(metric);
    this.operator = parseFromDbValue(requireNonNull(operator));
    this.hasPeriod = hasPeriod;
    this.errorThreshold = errorThreshold;
    this.warningThreshold = warningThreshold;
  }

  private static Operator parseFromDbValue(String str) {
    for (Operator operator : Operator.values()) {
      if (operator.dbValue.equals(str)) {
        return operator;
      }
    }
    throw new IllegalArgumentException(String.format("Unsupported operator value: '%s'", str));
  }

  public Metric getMetric() {
    return metric;
  }

  public boolean hasPeriod() {
    return hasPeriod;
  }

  public Operator getOperator() {
    return operator;
  }

  @CheckForNull
  public String getWarningThreshold() {
    return warningThreshold;
  }

  @CheckForNull
  public String getErrorThreshold() {
    return errorThreshold;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Condition that = (Condition) o;
    return java.util.Objects.equals(metric, that.metric)
      && java.util.Objects.equals(hasPeriod, that.hasPeriod);
  }

  @Override
  public int hashCode() {
    return hash(metric, hasPeriod);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("metric", metric)
      .add("hasPeriod", hasPeriod)
      .add("operator", operator)
      .add("warningThreshold", warningThreshold)
      .add("errorThreshold", errorThreshold)
      .toString();
  }
}
