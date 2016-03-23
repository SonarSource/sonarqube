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
package org.sonar.server.computation.qualitymodel;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import org.sonar.api.utils.MessageException;

class RatingGrid {

  private final double[] gridValues;

  RatingGrid(double[] gridValues) {
    this.gridValues = Arrays.copyOf(gridValues, gridValues.length);
  }

  Rating getRatingForDensity(double density) {
    for (Rating rating : Rating.values()) {
      double lowerBound = getGradeLowerBound(rating);
      if (density >= lowerBound) {
        return rating;
      }
    }
    throw MessageException.of("The rating density value should be between 0 and " + Double.MAX_VALUE + " and got " + density);
  }

  double getGradeLowerBound(Rating rating) {
    if (rating.getIndex() > 1) {
      return gridValues[rating.getIndex() - 2];
    }
    return 0;
  }

  @VisibleForTesting
  double[] getGridValues() {
    return gridValues;
  }

  enum Rating {
    E(5),
    D(4),
    C(3),
    B(2),
    A(1);

    private final int index;

    Rating(int index) {
      this.index = index;
    }

    public int getIndex() {
      return index;
    }
  }

}
