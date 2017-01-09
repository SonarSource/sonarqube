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
package org.sonar.db.metric;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import javax.annotation.Nonnull;

/**
 * Common functions on MetricDto
 */
public class MetricDtoFunctions {
  private MetricDtoFunctions() {
    // prevents instantiation
  }

  public static Function<MetricDto, Integer> toId() {
    return ToId.INSTANCE;
  }

  public static Function<MetricDto, String> toKey() {
    return ToKey.INSTANCE;
  }

  public static Predicate<MetricDto> isOptimizedForBestValue() {
    return IsMetricOptimizedForBestValue.INSTANCE;
  }

  private enum ToId implements Function<MetricDto, Integer> {
    INSTANCE;

    @Override
    public Integer apply(@Nonnull MetricDto input) {
      return input.getId();
    }
  }

  private enum ToKey implements Function<MetricDto, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull MetricDto input) {
      return input.getKey();
    }
  }

  private enum IsMetricOptimizedForBestValue implements Predicate<MetricDto> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull MetricDto input) {
      return input.isOptimizedBestValue() && input.getBestValue() != null;
    }
  }
}
