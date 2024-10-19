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
package org.sonar.server.platform.db.migration.version.v101;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v101.AddReportSchedulesTable.TABLE_NAME;

class AddReportSchedulesTableIT {
  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddReportSchedulesTable.class);

  private final DdlChange underTest = new AddReportSchedulesTable(db.database());

  @Test
  void migration_should_create_a_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertColumnDefinition(TABLE_NAME, "uuid", Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, "portfolio_uuid", Types.VARCHAR, UUID_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, "branch_uuid", Types.VARCHAR, UUID_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, "last_send_time_in_ms", Types.BIGINT, null, false);
    db.assertPrimaryKey(TABLE_NAME, "pk_report_schedules", "uuid");

  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    // re-entrant
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }


}
