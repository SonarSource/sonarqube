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
package org.sonar.server.measure.live;

import java.util.List;
import org.junit.Test;
import org.sonar.db.issue.HotspotGroupDto;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class HotspotsCounterTest {
  @Test
  public void counts_hotspots() {
    HotspotGroupDto group1 = new HotspotGroupDto().setCount(3).setStatus("TO_REVIEW").setInLeak(false);
    HotspotGroupDto group2 = new HotspotGroupDto().setCount(2).setStatus("REVIEWED").setInLeak(false);
    HotspotGroupDto group3 = new HotspotGroupDto().setCount(1).setStatus("TO_REVIEW").setInLeak(true);
    HotspotGroupDto group4 = new HotspotGroupDto().setCount(1).setStatus("REVIEWED").setInLeak(true);

    HotspotsCounter counter = new HotspotsCounter(List.of(group1, group2, group3, group4));
    assertThat(counter.countHotspotsByStatus("TO_REVIEW", true)).isEqualTo(1);
    assertThat(counter.countHotspotsByStatus("REVIEWED", true)).isEqualTo(1);
    assertThat(counter.countHotspotsByStatus("TO_REVIEW", false)).isEqualTo(4);
    assertThat(counter.countHotspotsByStatus("REVIEWED", false)).isEqualTo(3);
  }

  @Test
  public void count_empty_hotspots() {
    HotspotsCounter counter = new HotspotsCounter(emptyList());
    assertThat(counter.countHotspotsByStatus("TO_REVIEW", true)).isZero();
  }
}
