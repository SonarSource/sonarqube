/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v85;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class DropUnusedPeriodsInSnapshotsTest {
  private static final String TABLE_NAME = "snapshots";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DropUnusedPeriodsInSnapshotsTest.class, "schema.sql");

  private MigrationStep underTest = new DropUnusedPeriodsInSnapshots(db.database());

  @Test
  public void drops_table() throws SQLException {
    insertData();
    db.assertColumnDefinition(TABLE_NAME, "period2_mode", Types.VARCHAR, 100, true);
    db.assertColumnDefinition(TABLE_NAME, "period3_mode", Types.VARCHAR, 100, true);
    db.assertColumnDefinition(TABLE_NAME, "period4_mode", Types.VARCHAR, 100, true);
    db.assertColumnDefinition(TABLE_NAME, "period5_mode", Types.VARCHAR, 100, true);

    db.assertColumnDefinition(TABLE_NAME, "period2_param", Types.VARCHAR, 100, true);
    db.assertColumnDefinition(TABLE_NAME, "period3_param", Types.VARCHAR, 100, true);
    db.assertColumnDefinition(TABLE_NAME, "period4_param", Types.VARCHAR, 100, true);
    db.assertColumnDefinition(TABLE_NAME, "period5_param", Types.VARCHAR, 100, true);

    db.assertColumnDefinition(TABLE_NAME, "period2_date", Types.BIGINT, null, true);
    db.assertColumnDefinition(TABLE_NAME, "period3_date", Types.BIGINT, null, true);
    db.assertColumnDefinition(TABLE_NAME, "period4_date", Types.BIGINT, null, true);
    db.assertColumnDefinition(TABLE_NAME, "period5_date", Types.BIGINT, null, true);

    underTest.execute();
    db.assertColumnDoesNotExist(TABLE_NAME, "period2_mode");
    db.assertColumnDoesNotExist(TABLE_NAME, "period3_mode");
    db.assertColumnDoesNotExist(TABLE_NAME, "period4_mode");
    db.assertColumnDoesNotExist(TABLE_NAME, "period5_mode");

    db.assertColumnDoesNotExist(TABLE_NAME, "period2_param");
    db.assertColumnDoesNotExist(TABLE_NAME, "period3_param");
    db.assertColumnDoesNotExist(TABLE_NAME, "period4_param");
    db.assertColumnDoesNotExist(TABLE_NAME, "period5_param");

    db.assertColumnDoesNotExist(TABLE_NAME, "period2_date");
    db.assertColumnDoesNotExist(TABLE_NAME, "period3_date");
    db.assertColumnDoesNotExist(TABLE_NAME, "period4_date");
    db.assertColumnDoesNotExist(TABLE_NAME, "period5_date");

    assertThat(db.selectFirst("select * from snapshots")).contains(entry("PERIOD1_MODE", "m1"));

  }

  private void insertData() {
    db.executeInsert(TABLE_NAME,
      "uuid", "uuid1",
      "component_uuid", "component1",

      "period1_mode", "m1",
      "period2_mode", "m2",
      "period3_mode", "m3",
      "period4_mode", "m4",
      "period5_mode", "m5",

      "period1_param", "p1",
      "period2_param", "p2",
      "period3_param", "p3",
      "period4_param", "p4",
      "period5_param", "p5",

      "period1_date", 1,
      "period2_date", 2,
      "period3_date", 3,
      "period4_date", 4,
      "period5_date", 5
    );
  }
}
