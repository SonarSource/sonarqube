/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202604;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v202604.AddSignatureTypeToScaIrCveLocations.COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v202604.AddSignatureTypeToScaIrCveLocations.COLUMN_SIZE;
import static org.sonar.server.platform.db.migration.version.v202604.AddSignatureTypeToScaIrCveLocations.TABLE_NAME;

class AddSignatureTypeToScaIrCveLocationsTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddSignatureTypeToScaIrCveLocations.class);

  private final AddSignatureTypeToScaIrCveLocations underTest = new AddSignatureTypeToScaIrCveLocations(db.database());

  @Test
  void execute_shouldAddColumnBackfillAndMakeNotNullable() throws SQLException {
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_NAME);

    db.executeInsert(TABLE_NAME,
      "uuid", "cve-loc-1",
      "sca_issues_releases_uuid", "issue-release-1",
      "cve_id", "CVE-2026-2400",
      "signature", "com.example.Reachable.method()",
      "file_path", "/src/Main.java",
      "start_line", 10,
      "start_line_offset", 1,
      "end_line", 10,
      "end_line_offset", 20,
      "created_at", 0L,
      "updated_at", 0L);

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.VARCHAR, COLUMN_SIZE, false);
    var rows = db.select("SELECT signature_type FROM sca_ir_cve_locations WHERE uuid = 'cve-loc-1'");
    assertThat(rows).hasSize(1);
    assertThat(rows.getFirst()).containsEntry("SIGNATURE_TYPE", "VULNERABLE_FUNCTION");
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.VARCHAR, COLUMN_SIZE, false);
  }
}
