/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.telemetry;

import java.util.List;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class TelemetryMetricsSentDao implements Dao {

  private final System2 system2;

  public TelemetryMetricsSentDao(System2 system2) {
    this.system2 = system2;
  }

  public List<TelemetryMetricsSentDto> selectAll(DbSession session) {
    return mapper(session).selectAll();
  }

  public void upsert(DbSession dbSession, TelemetryMetricsSentDto telemetryMetricsSentDto) {
    long now = system2.now();
    telemetryMetricsSentDto.setLastSent(now);
    mapper(dbSession).upsert(telemetryMetricsSentDto);
  }

  private static TelemetryMetricsSentMapper mapper(DbSession session) {
    return session.getMapper(TelemetryMetricsSentMapper.class);
  }


}
