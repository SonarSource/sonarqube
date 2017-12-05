/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.formula.counter;

import javax.annotation.Nullable;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating;

/**
 * Convenience class wrapping a rating to compute the value and know it is has ever been set.
 */
public class RatingValue {
  private boolean set = false;
  private Rating value = Rating.A;

  /**
   * @return the current {@link RatingValue}  so that chained calls on a specific {@link RatingValue} instance can be done
   */
  public RatingValue increment(Rating rating) {
    if (value.compareTo(rating) > 0) {
      value = rating;
    }
    this.set = true;
    return this;
  }

  /**
   * @return the current {@link RatingValue} so that chained calls on a specific {@link RatingValue} instance can be done
   */
  public RatingValue increment(@Nullable RatingValue value) {
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

}
