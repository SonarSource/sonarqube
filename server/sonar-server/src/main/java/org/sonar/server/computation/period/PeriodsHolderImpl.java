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

package org.sonar.server.computation.period;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.computation.period.Period.isValidPeriodIndex;

public class PeriodsHolderImpl implements PeriodsHolder {

  @CheckForNull
  private Period[] periods = null;

  /**
   * Initializes the periods in the holder.
   *
   * @throws NullPointerException if the specified Iterable is {@code null}
   * @throws NullPointerException if the specified Iterable contains a {@code null}
   * @throws IllegalArgumentException if the specified Iterable has more than 5 elements
   * @throws IllegalStateException if the holder has already been initialized
   * @throws IllegalStateException if two Periods have the same index
   */
  public void setPeriods(Iterable<Period> periods) {
    requireNonNull(periods, "Periods cannot be null");
    checkArgument(Iterables.size(periods) <= MAX_NUMBER_OF_PERIODS, String.format("There can not be more than %d periods", MAX_NUMBER_OF_PERIODS));
    checkState(this.periods == null, "Periods have already been initialized");

    Period[] newPeriods = new Period[MAX_NUMBER_OF_PERIODS];
    for (Period period : from(periods).filter(CheckNotNull.INSTANCE)) {
      int arrayIndex = period.getIndex() - 1;
      checkArgument(newPeriods[arrayIndex] == null, "More than one period has the index " + period.getIndex());
      newPeriods[arrayIndex] = period;
    }
    this.periods = newPeriods;
  }

  @Override
  public List<Period> getPeriods() {
    checkHolderIsInitialized();
    return from(Arrays.asList(periods)).filter(Predicates.notNull()).toList();
  }

  @Override
  public boolean hasPeriod(int i) {
    checkHolderIsInitialized();
    if (!isValidPeriodIndex(i)) {
      throw new IndexOutOfBoundsException(String.format("Invalid Period index (%s), must be 0 < x < 6", i));
    }
    return periods[i - 1] != null;
  }

  @Override
  public Period getPeriod(int i) {
    checkState(hasPeriod(i), "Holder has no Period for index " + i);
    return this.periods[i - 1];
  }

  private void checkHolderIsInitialized() {
    checkState(this.periods != null, "Periods have not been initialized yet");
  }

  private enum CheckNotNull implements Predicate<Period> {
    INSTANCE;

    @Override
    public boolean apply(@Nullable Period input) {
      requireNonNull(input, "No null Period can be added to the holder");
      return true;
    }
  }

}
