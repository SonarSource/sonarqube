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

package org.sonar.server.security;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.server.measure.Rating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.measure.Rating.A;
import static org.sonar.server.measure.Rating.B;
import static org.sonar.server.measure.Rating.C;
import static org.sonar.server.measure.Rating.D;
import static org.sonar.server.measure.Rating.E;

@RunWith(DataProviderRunner.class)
public class SecurityReviewRatingTest {

  @DataProvider
  public static Object[][] values() {
    List<Object[]> res = new ArrayList<>();
    res.add(new Object[] {1000, 0, A});
    res.add(new Object[] {1000, 3, A});
    res.add(new Object[] {1000, 4, B});
    res.add(new Object[] {1000, 10, B});
    res.add(new Object[] {1000, 11, C});
    res.add(new Object[] {1000, 15, C});
    res.add(new Object[] {1000, 16, D});
    res.add(new Object[] {1000, 25, D});
    res.add(new Object[] {1000, 26, E});
    res.add(new Object[] {1000, 900, E});

    res.add(new Object[] {0, 2, A});
    res.add(new Object[] {1001, 3, A});
    res.add(new Object[] {999, 3, B});
    res.add(new Object[] {Integer.MAX_VALUE, Integer.MAX_VALUE, E});
    return res.toArray(new Object[res.size()][3]);
  }

  @Test
  @UseDataProvider("values")
  public void compute_security_review_rating_on_project(int ncloc, int securityHotspots, Rating expectedRating) {
    assertThat(SecurityReviewRating.compute(ncloc, securityHotspots)).isEqualTo(expectedRating);
  }

}
