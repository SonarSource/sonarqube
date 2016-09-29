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
package org.sonar.server.computation.task.projectanalysis.formula.counter;

import java.util.Arrays;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.server.computation.task.projectanalysis.formula.Counter;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureVariations;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodsHolder;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.RatingGrid.Rating;

import static org.sonar.server.computation.task.projectanalysis.period.PeriodsHolder.MAX_NUMBER_OF_PERIODS;

/**
 * Convenience class wrapping a rating.
 * It also allows to compute the value of a MeasureVariation as a rating, and add a check to know if it is has ever been set.
 * <p>
 * Basically, this class will be used in a {@link Counter} implementation as an array property which can be easily
 * created using method {@link #newArray()}.
 * </p>
 */
public class RatingVariationValue {
  private boolean set = false;
  private Rating value = Rating.A;

  /**
   * @return the current RatingVariationValue so that chained calls on a specific RatingVariationValue instance can be done
   */
  public RatingVariationValue increment(Rating rating) {
    if (value.compareTo(rating) > 0) {
      value = rating;
    }
    this.set = true;
    return this;
  }

  /**
   * @return the current RatingVariationValue so that chained calls on a specific RatingVariationValue instance can be done
   */
  public RatingVariationValue increment(@Nullable RatingVariationValue value) {
    if (value != null && value.isSet()) {
      increment(value.value);
    }
    return this;
  }

  public boolean isSet() {
    return set;
  }

  public Rating getValue() {
    return value;
  }

  /**
   * Creates a new Array of {@link RatingVariationValue} of size {@link PeriodsHolder#MAX_NUMBER_OF_PERIODS},
   * initialized with newly creates {@link RatingVariationValue} instances.
   */
  public static Array newArray() {
    return new Array();
  }

  public static class Array {
    private final RatingVariationValue[] values;

    public Array() {
      this.values = new RatingVariationValue[MAX_NUMBER_OF_PERIODS];
      for (int i = 0; i < MAX_NUMBER_OF_PERIODS; i++) {
        this.values[i] = new RatingVariationValue();
      }
    }

    public RatingVariationValue get(Period period) {
      return values[period.getIndex() - 1];
    }

    /**
    * @return the current Array, so that chained calls on a specific Array instance can be done
    */
    public Array increment(Period period, Rating value) {
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
        return Optional.empty();
      }
      Double[] variations = new Double[values.length];
      for (int i = 0; i < values.length; i++) {
        if (values[i].isSet()) {
          variations[i] = (double) values[i].getValue().getIndex();
        }
      }
      return Optional.of(new MeasureVariations(variations));
    }

    private boolean isAnySet() {
      return Arrays.stream(values).anyMatch(RatingVariationValue::isSet);
    }
  }
}
