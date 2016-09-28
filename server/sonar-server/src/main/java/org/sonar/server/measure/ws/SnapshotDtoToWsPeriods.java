/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.measure.ws;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.db.component.SnapshotDto;
import org.sonarqube.ws.WsMeasures;

import static java.util.Collections.emptyList;
import static org.sonar.api.utils.DateUtils.formatDateTime;

class SnapshotDtoToWsPeriods {
  private SnapshotDtoToWsPeriods() {
    // prevent instantiation
  }

  static List<WsMeasures.Period> snapshotToWsPeriods(@Nullable SnapshotDto snapshot) {
    if (snapshot == null) {
      return emptyList();
    }

    List<WsMeasures.Period> periods = new ArrayList<>();
    for (int periodIndex = 1; periodIndex <= 3; periodIndex++) {
      if (snapshot.getPeriodDate(periodIndex) != null) {
        periods.add(snapshotDtoToWsPeriod(snapshot, periodIndex));
      }
    }

    return periods;
  }

  private static WsMeasures.Period snapshotDtoToWsPeriod(SnapshotDto snapshot, int periodIndex) {
    WsMeasures.Period.Builder period = WsMeasures.Period.newBuilder();
    period.setIndex(periodIndex);
    if (snapshot.getPeriodMode(periodIndex) != null) {
      period.setMode(snapshot.getPeriodMode(periodIndex));
    }
    if (snapshot.getPeriodModeParameter(periodIndex) != null) {
      period.setParameter(snapshot.getPeriodModeParameter(periodIndex));
    }
    if (snapshot.getPeriodDate(periodIndex) != null) {
      period.setDate(formatDateTime(snapshot.getPeriodDate(periodIndex)));
    }

    return period.build();
  }
}
