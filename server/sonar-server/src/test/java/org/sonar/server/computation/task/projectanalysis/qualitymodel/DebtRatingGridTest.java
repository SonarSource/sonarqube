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
package org.sonar.server.computation.task.projectanalysis.qualitymodel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.A;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.B;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.C;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.D;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.E;

public class DebtRatingGridTest {

  private DebtRatingGrid ratingGrid;

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Before
  public void setUp() {
    double[] gridValues = new double[] {0.1, 0.2, 0.5, 1};
    ratingGrid = new DebtRatingGrid(gridValues);
  }

  @Test
  public void return_rating_matching_density() {
    assertThat(ratingGrid.getRatingForDensity(0)).isEqualTo(A);
    assertThat(ratingGrid.getRatingForDensity(0.05)).isEqualTo(A);
    assertThat(ratingGrid.getRatingForDensity(0.09999999)).isEqualTo(A);
    assertThat(ratingGrid.getRatingForDensity(0.1)).isEqualTo(A);
    assertThat(ratingGrid.getRatingForDensity(0.15)).isEqualTo(B);
    assertThat(ratingGrid.getRatingForDensity(0.2)).isEqualTo(B);
    assertThat(ratingGrid.getRatingForDensity(0.25)).isEqualTo(C);
    assertThat(ratingGrid.getRatingForDensity(0.5)).isEqualTo(C);
    assertThat(ratingGrid.getRatingForDensity(0.65)).isEqualTo(D);
    assertThat(ratingGrid.getRatingForDensity(1)).isEqualTo(D);
    assertThat(ratingGrid.getRatingForDensity(1.01)).isEqualTo(E);
  }

  @Test
  public void density_matching_exact_grid_values() {
    assertThat(ratingGrid.getRatingForDensity(0.1)).isEqualTo(A);
    assertThat(ratingGrid.getRatingForDensity(0.2)).isEqualTo(B);
    assertThat(ratingGrid.getRatingForDensity(0.5)).isEqualTo(C);
    assertThat(ratingGrid.getRatingForDensity(1)).isEqualTo(D);
  }

  @Test
  public void convert_int_to_rating() {
    assertThat(Rating.valueOf(1)).isEqualTo(A);
    assertThat(Rating.valueOf(2)).isEqualTo(B);
    assertThat(Rating.valueOf(3)).isEqualTo(C);
    assertThat(Rating.valueOf(4)).isEqualTo(D);
    assertThat(Rating.valueOf(5)).isEqualTo(E);
  }

  @Test
  public void fail_on_invalid_density() {
    throwable.expect(IllegalArgumentException.class);
    throwable.expectMessage("Invalid value '-1.0'");

    ratingGrid.getRatingForDensity(-1);
  }

  @Test
  public void fail_to_concert_invalid_value() {
    throwable.expect(IllegalArgumentException.class);
    Rating.valueOf(10);
  }

  @Test
  public void fail_on_invalid_grid() {
    throwable.expect(IllegalStateException.class);
    throwable.expectMessage("Rating grid should contains 4 values");

    ratingGrid = new DebtRatingGrid(new double[] {0.1, 0.2, 0.5});
  }
}
