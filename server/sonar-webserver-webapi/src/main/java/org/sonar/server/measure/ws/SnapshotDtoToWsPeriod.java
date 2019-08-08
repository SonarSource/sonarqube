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
package org.sonar.server.measure.ws;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.db.component.SnapshotDto;
import org.sonarqube.ws.Measures;

import static org.sonar.api.utils.DateUtils.formatDateTime;

class SnapshotDtoToWsPeriod {
  private SnapshotDtoToWsPeriod() {
    // prevent instantiation
  }

  static Optional<Measures.Period> snapshotToWsPeriods(@Nullable SnapshotDto snapshot) {
    if (snapshot == null) {
      return Optional.empty();
    }

    if (snapshot.getPeriodDate() != null) {
      return Optional.of(snapshotDtoToWsPeriod(snapshot));
    }

    return Optional.empty();
  }

  private static Measures.Period snapshotDtoToWsPeriod(SnapshotDto snapshot) {
    Measures.Period.Builder period = Measures.Period.newBuilder();
    period.setIndex(1);
    String periodMode = snapshot.getPeriodMode();
    if (periodMode != null) {
      period.setMode(periodMode);
    }
    String periodModeParameter = snapshot.getPeriodModeParameter();
    if (periodModeParameter != null) {
      period.setParameter(periodModeParameter);
    }
    Long periodDate = snapshot.getPeriodDate();
    if (periodDate != null) {
      period.setDate(formatDateTime(periodDate));
    }
    return period.build();
  }
}
