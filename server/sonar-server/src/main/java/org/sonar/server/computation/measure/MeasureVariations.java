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
package org.sonar.server.computation.measure;

import com.google.common.base.Objects;
import java.util.Arrays;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.server.computation.period.Period;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;

@Immutable
public final class MeasureVariations {
  private static final String NAN_ERROR_MESSAGE = "NaN is not allowed in MeasureVariation";

  private final Double[] variations = new Double[5];

  public MeasureVariations(Double... variations) {
    checkArgument(variations.length <= 5, "There can not be more than 5 variations");
    checkArgument(!from(Arrays.asList(variations)).filter(notNull()).isEmpty(), "There must be at least one variation");
    for (Double variation : variations) {
      checkArgument(variation == null || !Double.isNaN(variation), NAN_ERROR_MESSAGE);
    }
    System.arraycopy(variations, 0, this.variations, 0, variations.length);
  }

  public static Builder newMeasureVariationsBuilder() {
    return new Builder();
  }

  public static final class Builder {
    private final Double[] variations = new Double[5];

    private Builder() {
      // prevents instantiation outside static method
    }

    public Builder setVariation(Period period, double variation) {
      int arrayIndex = period.getIndex() - 1;
      checkState(variations[arrayIndex] == null, String.format("Variation for Period %s has already been set", period.getIndex()));
      checkArgument(!Double.isNaN(variation), NAN_ERROR_MESSAGE);
      variations[arrayIndex] = variation;
      return this;
    }

    /**
     * Indicates whether any variation has been set in the builder.
     * This method can be used to know beforehand whether the {@link #build()} method will raise a
     * {@link IllegalArgumentException} because the constructor of {@link MeasureVariations} has been invoked with no
     * value.
     */
    public boolean isEmpty() {
      for (Double variation : variations) {
        if (variation != null) {
          return false;
        }
      }
      return true;
    }

    public MeasureVariations build() {
      return new MeasureVariations(variations);
    }
  }

  public boolean hasVariation1() {
    return hasVariation(1);
  }

  public boolean hasVariation2() {
    return hasVariation(2);
  }

  public boolean hasVariation3() {
    return hasVariation(3);
  }

  public boolean hasVariation4() {
    return hasVariation(4);
  }

  public boolean hasVariation5() {
    return hasVariation(5);
  }

  private void checkHasVariation(int periodIndex) {
    if (!hasVariation(periodIndex)) {
      throw new IllegalStateException(String.format("Variation %s has not been set", periodIndex));
    }
  }

  public boolean hasVariation(int periodIndex) {
    return variations[periodIndex - 1] != null;
  }

  public double getVariation1() {
    return getVariation(1);
  }

  public double getVariation2() {
    return getVariation(2);
  }

  public double getVariation3() {
    return getVariation(3);
  }

  public double getVariation4() {
    return getVariation(4);
  }

  public double getVariation5() {
    return getVariation(5);
  }

  public double getVariation(int i) {
    checkHasVariation(i);
    return variations[i - 1];
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("1", variations[0])
      .add("2", variations[1])
      .add("3", variations[2])
      .add("4", variations[3])
      .add("5", variations[4])
      .toString();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MeasureVariations that = (MeasureVariations) o;
    return Arrays.equals(variations, that.variations);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(variations);
  }
}
