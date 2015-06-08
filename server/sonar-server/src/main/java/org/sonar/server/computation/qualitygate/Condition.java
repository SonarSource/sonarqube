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
package org.sonar.server.computation.qualitygate;

import com.google.common.base.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.server.computation.metric.Metric;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

@Immutable
public class Condition {

  private final Metric metric;
  @CheckForNull
  private final Integer period;
  private final String operator;
  @CheckForNull
  private final String warningThreshold;
  @CheckForNull
  private final String errorThreshold;

  public Condition(Metric metric, @Nullable Integer period,
    String operator, @Nullable String errorThreshold, @Nullable String warningThreshold) {
    this.metric = requireNonNull(metric);
    this.operator = requireNonNull(operator);
    this.period = period;
    this.errorThreshold = errorThreshold;
    this.warningThreshold = warningThreshold;
  }

  public Metric getMetric() {
    return metric;
  }

  @CheckForNull
  public Integer getPeriod() {
    return period;
  }

  public String getOperator() {
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
      && java.util.Objects.equals(period, that.period);
  }

  @Override
  public int hashCode() {
    return hash(metric, period);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("metric", metric)
      .add("period", period)
      .add("operator", operator)
      .add("warningThreshold", warningThreshold)
      .add("errorThreshold", errorThreshold)
      .toString();
  }
}
