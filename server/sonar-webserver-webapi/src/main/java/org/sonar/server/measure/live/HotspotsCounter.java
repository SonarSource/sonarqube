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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.db.issue.HotspotGroupDto;

public class HotspotsCounter {
  private final Map<String, Count> hotspotsByStatus = new HashMap<>();

  HotspotsCounter(Collection<HotspotGroupDto> groups) {
    for (HotspotGroupDto group : groups) {
      if (group.getStatus() != null) {
        hotspotsByStatus
          .computeIfAbsent(group.getStatus(), k -> new Count())
          .add(group);
      }
    }
  }

  public long countHotspotsByStatus(String status, boolean onlyInLeak) {
    return value(hotspotsByStatus.get(status), onlyInLeak);
  }

  private static long value(@Nullable Count count, boolean onlyInLeak) {
    if (count == null) {
      return 0;
    }
    return onlyInLeak ? count.leak : count.absolute;
  }

  private static class Count {
    private long absolute = 0L;
    private long leak = 0L;

    void add(HotspotGroupDto group) {
      absolute += group.getCount();
      if (group.isInLeak()) {
        leak += group.getCount();
      }
    }
  }
}
