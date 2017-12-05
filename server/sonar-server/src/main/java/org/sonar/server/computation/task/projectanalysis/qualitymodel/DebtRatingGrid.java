/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.qualitymodel;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.MessageException;

import static org.sonar.api.CoreProperties.RATING_GRID;
import static org.sonar.api.CoreProperties.RATING_GRID_DEF_VALUES;

public class DebtRatingGrid {

  private final double[] gridValues;

  public DebtRatingGrid(double[] gridValues) {
    this.gridValues = Arrays.copyOf(gridValues, gridValues.length);
  }

  public DebtRatingGrid(Configuration config) {
    try {
      String[] grades = config.getStringArray(RATING_GRID);
      gridValues = new double[4];
      for (int i = 0; i < 4; i++) {
        gridValues[i] = Double.parseDouble(grades[i]);
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("The rating grid is incorrect. Expected something similar to '"
        + RATING_GRID_DEF_VALUES + "' and got '" + config.get(RATING_GRID).orElse("") + "'", e);
    }
  }

  public Rating getRatingForDensity(double density) {
    for (Rating rating : Rating.values()) {
      double lowerBound = getGradeLowerBound(rating);
      if (density >= lowerBound) {
        return rating;
      }
    }
    throw MessageException.of("The rating density value should be between 0 and " + Double.MAX_VALUE + " and got " + density);
  }

  public double getGradeLowerBound(Rating rating) {
    if (rating.getIndex() > 1) {
      return gridValues[rating.getIndex() - 2];
    }
    return 0;
  }

  @VisibleForTesting
  double[] getGridValues() {
    return gridValues;
  }

}
