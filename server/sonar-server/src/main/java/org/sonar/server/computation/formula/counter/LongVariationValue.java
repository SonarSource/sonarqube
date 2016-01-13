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
package org.sonar.server.computation.formula.counter;

import com.google.common.base.Optional;
import javax.annotation.Nullable;
import org.sonar.server.computation.formula.Counter;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolder;

import static org.sonar.server.computation.period.PeriodsHolder.MAX_NUMBER_OF_PERIODS;

/**
 * Convenience class wrapping a long to compute the value of a MeasureVariation as an long and know it is has ever been
 * set.
 * <p>
 * Basically, this class will be used in a {@link Counter} implementation as an array property which can be easily
 * creating using method {@link #newArray()}.
 * </p>
 */
public class LongVariationValue {
  private boolean set = false;
  private long value = 0L;

  /**
   * @return the current LongVariationValue so that chained calls on a specific LongVariationValue instance can be done
   */
  public LongVariationValue increment(long increment) {
    this.value += increment;
    this.set = true;
    return this;
  }

  /**
   * @return the current LongVariationValue so that chained calls on a specific LongVariationValue instance can be done
   */
  public LongVariationValue increment(@Nullable LongVariationValue value) {
    if (value != null && value.isSet()) {
      increment(value.value);
    }
    return this;
  }

  public boolean isSet() {
    return set;
  }

  public long getValue() {
    return value;
  }

  /**
   * Creates a new Array of {@link LongVariationValue} of size {@link PeriodsHolder#MAX_NUMBER_OF_PERIODS},
   * initialized with newly creates {@link LongVariationValue} instances.
   */
  public static Array newArray() {
    return new Array();
  }

  public static class Array {
    private final LongVariationValue[] values;

    public Array() {
      this.values = new LongVariationValue[MAX_NUMBER_OF_PERIODS];
      for (int i = 0; i < MAX_NUMBER_OF_PERIODS; i++) {
        this.values[i] = new LongVariationValue();
      }
    }

    public LongVariationValue get(Period period) {
      return values[period.getIndex() - 1];
    }

    /**
     * @return the current Array, so that chained calls on a specific Array instance can be done
     */
    public Array increment(Period period, long value) {
      this.values[period.getIndex() - 1].increment(value);
      return this;
    }

    /**
     * @return the current Array, so that chained calls on a specific Array instance can be done
     */
    public Array incrementAll(Array source) {
      for (int i = 0; i < this.values.length; i++) {
        if (source.values[i].isSet()) {
          this.values[i].increment(source.values[i]);
        }
      }
      return this;
    }

    /**
     * Creates a new MeasureVariations from the current array.
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
      for (LongVariationValue variationValue : values) {
        if (variationValue.isSet()) {
          return true;
        }
      }
      return false;
    }
  }
}
