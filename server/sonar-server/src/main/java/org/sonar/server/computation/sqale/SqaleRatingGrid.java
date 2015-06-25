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

package org.sonar.server.computation.sqale;

import java.util.Arrays;
import org.sonar.api.utils.MessageException;

public class SqaleRatingGrid {

  private final double[] gridValues;

  public SqaleRatingGrid(double[] gridValues) {
    this.gridValues = Arrays.copyOf(gridValues, gridValues.length);
  }

  public int getRatingForDensity(double density) {
    for (SqaleRating sqaleRating : SqaleRating.values()) {
      double lowerBound = getGradeLowerBound(sqaleRating);
      if (density >= lowerBound) {
        return sqaleRating.getIndex();
      }
    }
    throw MessageException.of("The SQALE density value should be between 0 and " + Double.MAX_VALUE + " and got " + density);
  }

  private double getGradeLowerBound(SqaleRating rating) {
    if (rating.getIndex() > 1) {
      return gridValues[rating.getIndex() - 2];
    }
    return 0;
  }

  public enum SqaleRating {
    E(5),
    D(4),
    C(3),
    B(2),
    A(1);

    private final int index;

    SqaleRating(int index) {
      this.index = index;
    }

    public int getIndex() {
      return index;
    }

    public static SqaleRating createForIndex(int index) {
      for (SqaleRating rating : values()) {
        if (rating.getIndex() == index) {
          return rating;
        }
      }
      throw new IllegalArgumentException("A SQALE rating must be in the range [1..5].");
    }
  }

}
