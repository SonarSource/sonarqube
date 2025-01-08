/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v102;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;

class PopulatePurgedColumnInSnapshotsIT {
  private static final String TABLE_NAME = "snapshots";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulatePurgedColumnInSnapshots.class);
  private final PopulatePurgedColumnInSnapshots underTest = new PopulatePurgedColumnInSnapshots(db.database());

  @Test
  void execute_whenSnapshotsDoesNotExist_shouldNotFail() {
    assertThatCode(underTest::execute)
      .doesNotThrowAnyException();
  }

  @Test
  void execute_whenSnapshotsExist_shouldPopulatePurgedColumn() throws SQLException {
    insertSnapshot("uuid-1", null);
    insertSnapshot("uuid-2", 1);
    insertSnapshot("uuid-3", 0);
    insertSnapshot("uuid-4", null);

    underTest.execute();

    assertThat(db.select("select uuid, purged from snapshots"))
      .extracting(stringObjectMap -> stringObjectMap.get("uuid"), stringObjectMap -> stringObjectMap.get("purged"))
      .containsExactlyInAnyOrder(
        tuple("uuid-1", false),
        tuple("uuid-2", true),
        tuple("uuid-3", false),
        tuple("uuid-4", false));
  }

  private void insertSnapshot(String uuid, @Nullable Integer status) {
    db.executeInsert(TABLE_NAME,
      "UUID", uuid,
      "ROOT_COMPONENT_UUID", "r_c_uuid",
      "STATUS", "s",
      "ISLAST", true,
      "PURGE_STATUS", status,
      "PURGED", null);
  }
}
