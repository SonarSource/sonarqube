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
package org.sonar.db.telemetry;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class TelemetryMetricsSentDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);
  private static final long NOW = 1L;
  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);
  private final DbSession dbSession = db.getSession();
  private final TelemetryMetricsSentDao underTest = new TelemetryMetricsSentDao(system2);

  @Test
  void selectAll_shouldReturnAllInsertedData() {
    List<TelemetryMetricsSentDto> dtos = IntStream.range(0, 10)
      .mapToObj(i -> TelemetryMetricsSentTesting.newTelemetryMetricsSentDto())
      .toList();
    dtos.forEach(metricDto -> db.getDbClient().telemetryMetricsSentDao().upsert(db.getSession(), metricDto));
    db.getSession().commit();

    assertThat(underTest.selectAll(dbSession))
      .extracting(TelemetryMetricsSentDto::getMetricKey, TelemetryMetricsSentDto::getDimension, TelemetryMetricsSentDto::getLastSent)
      .containsExactlyInAnyOrderElementsOf(
        dtos.stream()
          .map(metricDto -> tuple(
            metricDto.getMetricKey(),
            metricDto.getDimension(),
            metricDto.getLastSent()))
          .toList()
      );
  }

  @Test
  void upsert_shouldUpdateOnlyAfterSecondPersistence() {
    TelemetryMetricsSentDto dto = TelemetryMetricsSentTesting.newTelemetryMetricsSentDto();
    underTest.upsert(dbSession, dto);

    system2.setNow(NOW + 1);
    underTest.upsert(dbSession, dto);
    List<TelemetryMetricsSentDto> dtos = underTest.selectAll(dbSession);

    assertThat(dtos).hasSize(1);
    TelemetryMetricsSentDto result = dtos.get(0);
    assertThat(result)
      .extracting(TelemetryMetricsSentDto::getMetricKey, TelemetryMetricsSentDto::getDimension, TelemetryMetricsSentDto::getLastSent)
      .containsExactly(dto.getMetricKey(), dto.getDimension(), NOW + 1);
  }

}
