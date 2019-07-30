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
package org.sonar.ce.task.projectanalysis.measure;

import java.util.EnumMap;
import org.sonar.server.measure.Rating;

import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;

public class RatingMeasures {
  private static final EnumMap<Rating, Measure> ratingMeasureCache;

  static {
    ratingMeasureCache = new EnumMap<>(Rating.class);
    for (Rating r : Rating.values()) {
      ratingMeasureCache.put(r, newMeasureBuilder().create(r.getIndex(), r.name()));
    }
  }

  private RatingMeasures() {
    // static only
  }

  public static Measure get(Rating rating) {
    return ratingMeasureCache.get(rating);
  }
}
