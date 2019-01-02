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
package org.sonar.server.measure;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import org.sonar.api.config.Configuration;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.sonar.api.CoreProperties.RATING_GRID;
import static org.sonar.api.CoreProperties.RATING_GRID_DEF_VALUES;
import static org.sonar.server.measure.Rating.A;
import static org.sonar.server.measure.Rating.B;
import static org.sonar.server.measure.Rating.C;
import static org.sonar.server.measure.Rating.D;
import static org.sonar.server.measure.Rating.E;

public class DebtRatingGrid {

  private final double[] gridValues;
  private final EnumMap<Rating, Bounds> ratingBounds;

  public DebtRatingGrid(Configuration config) {
    try {
      String[] grades = config.getStringArray(RATING_GRID);
      gridValues = new double[4];
      for (int i = 0; i < 4; i++) {
        gridValues[i] = Double.parseDouble(grades[i]);
      }
      this.ratingBounds = buildRatingBounds(gridValues);
    } catch (Exception e) {
      throw new IllegalArgumentException("The rating grid is incorrect. Expected something similar to '"
        + RATING_GRID_DEF_VALUES + "' and got '" + config.get(RATING_GRID).orElse("") + "'", e);
    }
  }

  public DebtRatingGrid(double[] gridValues) {
    this.gridValues = Arrays.copyOf(gridValues, gridValues.length);
    this.ratingBounds = buildRatingBounds(gridValues);
  }

  private static EnumMap<Rating, Bounds> buildRatingBounds(double[] gridValues) {
    checkState(gridValues.length == 4, "Rating grid should contains 4 values");
    EnumMap<Rating, Bounds> ratingBounds = new EnumMap<>(Rating.class);
    ratingBounds.put(A, new Bounds(0d, gridValues[0]));
    ratingBounds.put(B, new Bounds(gridValues[0], gridValues[1]));
    ratingBounds.put(C, new Bounds(gridValues[1], gridValues[2]));
    ratingBounds.put(D, new Bounds(gridValues[2], gridValues[3]));
    ratingBounds.put(E, new Bounds(gridValues[3], Double.MAX_VALUE));
    return ratingBounds;
  }

  public Rating getRatingForDensity(double value) {
    return ratingBounds.entrySet().stream()
      .filter(e -> e.getValue().match(value))
      .map(Map.Entry::getKey)
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException(format("Invalid value '%s'", value)));
  }

  public double getGradeLowerBound(Rating rating) {
    if (rating.getIndex() > 1) {
      return gridValues[rating.getIndex() - 2];
    }
    return 0;
  }

  @VisibleForTesting
  public double[] getGridValues() {
    return gridValues;
  }

  private static class Bounds {
    private final double lowerBound;
    private final double higherBound;
    private final boolean isLowerBoundInclusive;

    private Bounds(double lowerBound, double higherBound) {
      this.lowerBound = lowerBound;
      this.higherBound = higherBound;
      this.isLowerBoundInclusive = lowerBound == 0;
    }

    boolean match(double value) {
      boolean lowerBoundMatch = isLowerBoundInclusive ? (value >= lowerBound) : (value > lowerBound);
      return lowerBoundMatch && value <= higherBound;
    }
  }

}
