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
package org.sonar.server.security;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.data.Offset;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.server.measure.Rating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.measure.Rating.A;
import static org.sonar.server.measure.Rating.B;
import static org.sonar.server.measure.Rating.C;
import static org.sonar.server.measure.Rating.D;
import static org.sonar.server.measure.Rating.E;
import static org.sonar.server.security.SecurityReviewRating.computePercent;
import static org.sonar.server.security.SecurityReviewRating.computeRating;

@RunWith(DataProviderRunner.class)
public class SecurityReviewRatingTest {

  private static final Offset<Double> DOUBLE_OFFSET = Offset.offset(0.01d);

  @DataProvider
  public static Object[][] values() {
    List<Object[]> res = new ArrayList<>();
    res.add(new Object[] {100.0, A});
    res.add(new Object[] {90.0, A});
    res.add(new Object[] {80.0, A});
    res.add(new Object[] {75.0, B});
    res.add(new Object[] {70.0, B});
    res.add(new Object[] {60, C});
    res.add(new Object[] {50.0, C});
    res.add(new Object[] {40.0, D});
    res.add(new Object[] {30.0, D});
    res.add(new Object[] {29.9, E});
    return res.toArray(new Object[res.size()][2]);
  }

  @Test
  @UseDataProvider("values")
  public void compute_rating(double percent, Rating expectedRating) {
    assertThat(computeRating(percent)).isEqualTo(expectedRating);
  }

  @Test
  public void compute_percent() {
    assertThat(computePercent(0, 0)).isEmpty();
    assertThat(computePercent(0, 10)).contains(100.0);
    assertThat(computePercent(1, 3)).contains(75.0);
    assertThat(computePercent(3, 4).get()).isEqualTo(57.14, DOUBLE_OFFSET);
    assertThat(computePercent(10, 10)).contains(50.0);
    assertThat(computePercent(10, 0)).contains(0.0);
  }
}
