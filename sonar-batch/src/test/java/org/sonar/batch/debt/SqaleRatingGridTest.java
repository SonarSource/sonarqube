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

package org.sonar.batch.debt;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class SqaleRatingGridTest {

  private SqaleRatingGrid ratingGrid;

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Before
  public void setUp() {
    double[] gridValues = new double[] {0.1, 0.2, 0.5, 1};
    ratingGrid = new SqaleRatingGrid(gridValues);
  }

  @Test
  public void return_grade_lower_bound() throws Exception {
    assertThat(ratingGrid.getGradeLowerBound(SqaleRatingGrid.SqaleRating.A)).isEqualTo(0);
    assertThat(ratingGrid.getGradeLowerBound(SqaleRatingGrid.SqaleRating.B)).isEqualTo(0.1);
    assertThat(ratingGrid.getGradeLowerBound(SqaleRatingGrid.SqaleRating.C)).isEqualTo(0.2);
    assertThat(ratingGrid.getGradeLowerBound(SqaleRatingGrid.SqaleRating.D)).isEqualTo(0.5);
    assertThat(ratingGrid.getGradeLowerBound(SqaleRatingGrid.SqaleRating.E)).isEqualTo(1);
  }

  @Test
  public void return_rating_matching_density() throws Exception {
    assertThat(ratingGrid.getRatingForDensity(0)).isEqualTo(1);
    assertThat(ratingGrid.getRatingForDensity(0.05)).isEqualTo(1);
    assertThat(ratingGrid.getRatingForDensity(0.1)).isEqualTo(2);
    assertThat(ratingGrid.getRatingForDensity(0.15)).isEqualTo(2);
    assertThat(ratingGrid.getRatingForDensity(0.2)).isEqualTo(3);
    assertThat(ratingGrid.getRatingForDensity(0.25)).isEqualTo(3);
    assertThat(ratingGrid.getRatingForDensity(0.5)).isEqualTo(4);
    assertThat(ratingGrid.getRatingForDensity(0.65)).isEqualTo(4);
    assertThat(ratingGrid.getRatingForDensity(1)).isEqualTo(5);
    assertThat(ratingGrid.getRatingForDensity(1.01)).isEqualTo(5);
  }

  @Test
  public void fail_on_invalid_density() throws Exception {
    throwable.expect(RuntimeException.class);

    ratingGrid.getRatingForDensity(-1);
  }
}
