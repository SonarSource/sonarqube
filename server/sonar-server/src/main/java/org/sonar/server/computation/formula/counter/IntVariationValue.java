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
package org.sonar.server.computation.formula.counter;

import com.google.common.base.Optional;
import javax.annotation.Nullable;
import org.sonar.server.computation.formula.Counter;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.period.PeriodsHolder;

import static org.sonar.server.computation.period.PeriodsHolder.MAX_NUMBER_OF_PERIODS;

/**
 * Convenience class wrapping a int to compute the value of a MeasureVariation as an int and know it is has ever been
 * set.
 * <p>
 * Basically, this class will be used in a {@link Counter} implementation as an array property which can be easily
 * creating using method {@link #newArray()}.
 * </p>
 */
public class IntVariationValue {
  private boolean set = false;
  private int value = 0;

  public void increment(int increment) {
    this.value += increment;
    this.set = true;
  }

  public void increment(@Nullable IntVariationValue value) {
    if (value != null) {
      increment(value.value);
    }
  }

  public boolean isSet() {
    return set;
  }

  public int getValue() {
    return value;
  }

  /**
   * Creates a new Array of {@link IntVariationValue} of size {@link PeriodsHolder#MAX_NUMBER_OF_PERIODS},
   * initialized with newly creates {@link IntVariationValue} instances.
   */
  public static Array newArray() {
    return new Array();
  }

  public static class Array {
    private final IntVariationValue[] values;

    public Array() {
      this.values = new IntVariationValue[MAX_NUMBER_OF_PERIODS];
      for (int i = 0; i < MAX_NUMBER_OF_PERIODS; i++) {
        this.values[i] = new IntVariationValue();
      }
    }

    public void increment(int index, int value) {
      this.values[index].increment(value);
    }

    public void incrementAll(Array source) {
      for (int i = 0; i < this.values.length; i++) {
        if (source.values[i].isSet()) {
          this.values[i].increment(source.values[i]);
        }
      }
    }

    /**
     * Creates a new MeasureVariations from the current array.
     *
     * @throws IllegalArgumentException if none of the {@link IntVariationValue} in the array is set
     */
    public Optional<MeasureVariations> toMeasureVariations() {
      if (!isAnySet()) {
        return Optional.absent();
      }
      Double[] variations = new Double[values.length];
      for (int i = 0; i < values.length; i++) {
        if (values[i].isSet()) {
          variations[i] = (double) values[i].getValue();
        }
      }
      return Optional.of(new MeasureVariations(variations));
    }

    private boolean isAnySet() {
      for (IntVariationValue value : values) {
        if (value.isSet()) {
          return true;
        }
      }
      return false;
    }
  }
}
