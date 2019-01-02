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
package org.sonar.ce.task.projectanalysis.formula.counter;

import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.formula.CounterInitializationContext;
import org.sonar.ce.task.projectanalysis.measure.Measure;

import static java.util.Objects.requireNonNull;

/**
 * Simple counter that do the sum of an integer measure
 */
public class LongSumCounter implements SumCounter<Long, LongSumCounter> {

  private final String metricKey;
  @CheckForNull
  private final Long defaultInputValue;

  private long value = 0;
  private boolean initialized = false;

  public LongSumCounter(String metricKey) {
    this(metricKey, null);
  }

  public LongSumCounter(String metricKey, @Nullable Long defaultInputValue) {
    this.metricKey = requireNonNull(metricKey, "metricKey can not be null");
    this.defaultInputValue = defaultInputValue;
  }

  @Override
  public void aggregate(LongSumCounter counter) {
    if (counter.getValue().isPresent()) {
      addValue(counter.getValue().get());
    }
  }

  @Override
  public void initialize(CounterInitializationContext context) {
    Optional<Measure> measureOptional = context.getMeasure(metricKey);
    if (measureOptional.isPresent()) {
      addValue(measureOptional.get().getLongValue());
    } else if (defaultInputValue != null) {
      addValue(defaultInputValue);
    }
  }

  private void addValue(long newValue) {
    initialized = true;
    value += newValue;
  }

  @Override
  public Optional<Long> getValue() {
    if (initialized) {
      return Optional.of(value);
    }
    return Optional.empty();
  }
}
