/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.qualitymodel;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.server.measure.DebtRatingGrid;

import static org.sonar.api.CoreProperties.DEVELOPMENT_COST;

@ComputeEngineSide
public class RatingSettings {

  private final DebtRatingGrid ratingGrid;
  private final long defaultDevCost;

  public RatingSettings(Configuration config) {
    ratingGrid = new DebtRatingGrid(config);
    defaultDevCost = initDefaultDevelopmentCost(config);
  }

  public DebtRatingGrid getDebtRatingGrid() {
    return ratingGrid;
  }

  public long getDevCost() {
    return defaultDevCost;
  }

  private static long initDefaultDevelopmentCost(Configuration config) {
    try {
      return Long.parseLong(config.get(DEVELOPMENT_COST).get());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("The value of the development cost property '" + DEVELOPMENT_COST
        + "' is incorrect. Expected long but got '" + config.get(DEVELOPMENT_COST).get() + "'", e);
    }
  }

}
