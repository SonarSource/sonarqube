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
package org.sonar.server.computation.task.projectanalysis.formula.counter;

import com.google.common.base.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.server.computation.task.projectanalysis.formula.CounterInitializationContext;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;

import static java.util.Objects.requireNonNull;

/**
 * Simple counter that do the sum of an integer measure
 */
public class IntSumCounter implements SumCounter<Integer, IntSumCounter> {

  private final String metricKey;
  @CheckForNull
  private final Integer defaultInputValue;

  private int value = 0;
  private boolean initialized = false;

  public IntSumCounter(String metricKey) {
    this(metricKey, null);
  }

  public IntSumCounter(String metricKey, @Nullable Integer defaultInputValue) {
    this.metricKey = requireNonNull(metricKey, "metricKey can not be null");
    this.defaultInputValue = defaultInputValue;
  }

  @Override
  public void aggregate(IntSumCounter counter) {
    if (counter.getValue().isPresent()) {
      addValue(counter.getValue().get());
    }
  }

  @Override
  public void initialize(CounterInitializationContext context) {
    Optional<Measure> measureOptional = context.getMeasure(metricKey);
    if (measureOptional.isPresent()) {
      addValue(measureOptional.get().getIntValue());
    } else if (defaultInputValue != null) {
      addValue(defaultInputValue);
    }
  }

  private void addValue(int newValue) {
    initialized = true;
    value += newValue;
  }

  @Override
  public Optional<Integer> getValue() {
    if (initialized) {
      return Optional.of(value);
    }
    return Optional.absent();
  }
}
