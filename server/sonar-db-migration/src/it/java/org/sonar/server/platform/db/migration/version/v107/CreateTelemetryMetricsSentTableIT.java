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
package org.sonar.server.platform.db.migration.version.v107;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

class CreateTelemetryMetricsSentTableIT {

  private static final String EXPECTED_TABLE_NAME = "telemetry_metrics_sent";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateTelemetryMetricsSentTable.class);

  private final DdlChange underTest = new CreateTelemetryMetricsSentTable(db.database());

  @Test
  void migration_should_create_a_table() throws SQLException {
    db.assertTableDoesNotExist(EXPECTED_TABLE_NAME);

    underTest.execute();

    db.assertTableExists(EXPECTED_TABLE_NAME);
    db.assertColumnDefinition(EXPECTED_TABLE_NAME, "metric_key", Types.VARCHAR, 40, false);
    db.assertColumnDefinition(EXPECTED_TABLE_NAME, "dimension", Types.VARCHAR, 40, false);
    db.assertColumnDefinition(EXPECTED_TABLE_NAME, "last_sent", Types.BIGINT, null, false);
    db.assertPrimaryKey(EXPECTED_TABLE_NAME, null, "metric_key", "dimension");
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(EXPECTED_TABLE_NAME);

    underTest.execute();
    // re-entrant
    underTest.execute();

    db.assertTableExists(EXPECTED_TABLE_NAME);
  }
}
