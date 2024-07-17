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
package org.sonar.telemetry.metrics;

import java.util.List;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbTester;
import org.sonar.telemetry.FakeServer;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.TelemetryDataProvider;
import org.sonar.telemetry.metrics.schema.BaseMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelemetryMetricsLoaderIT {

  private static final String SOME_UUID = "some-uuid";
  private static final Long NOW = 100_000_000L;
  private static final String SERVER_ID = "AU-TpxcB-iU5OvuD2FL7";
  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public DbTester db = DbTester.create(system2);
  private final FakeServer server = new FakeServer();
  private final UuidFactory uuidFactory = mock(UuidFactory.class);
  private final List<TelemetryDataProvider<?>> providers = List.of(new TestTelemetryBean(Dimension.INSTALLATION), new TestTelemetryBean(Dimension.USER));
  private final TelemetryMetricsLoader underTest = new TelemetryMetricsLoader(server, db.getDbClient(), uuidFactory, providers);

  @Test
  void sendTelemetryData() {
    when(uuidFactory.create()).thenReturn(SOME_UUID);

    server.setId(SERVER_ID);
    TelemetryMetricsLoader.Context context = underTest.loadData();

    assertThat(context.getMessages()).hasSize(2);

    assertThat(context.getMessages())
      .extracting(BaseMessage::getMessageUuid, BaseMessage::getInstallationId, BaseMessage::getDimension)
      .containsExactlyInAnyOrder(
        tuple(SOME_UUID, SERVER_ID, Dimension.INSTALLATION),
        tuple(SOME_UUID, SERVER_ID, Dimension.USER)
      );
  }

}
