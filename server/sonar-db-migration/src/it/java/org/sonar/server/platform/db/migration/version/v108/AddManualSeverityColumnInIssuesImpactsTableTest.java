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
package org.sonar.server.platform.db.migration.version.v108;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.junit.jupiter.api.Assertions.*;

class AddManualSeverityColumnInIssuesImpactsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddManualSeverityColumnInIssuesImpactsTable.class);
  private final AddManualSeverityColumnInIssuesImpactsTable underTest = new AddManualSeverityColumnInIssuesImpactsTable(db.database());

  @Test
  void execute_whenColumnDoesNotExist_shouldCreateColumn() throws Exception {
    db.assertColumnDoesNotExist(AddManualSeverityColumnInIssuesImpactsTable.TABLE_NAME, AddManualSeverityColumnInIssuesImpactsTable.MANUAL_SEVERITY);
    underTest.execute();
    db.assertColumnDefinition(AddManualSeverityColumnInIssuesImpactsTable.TABLE_NAME, AddManualSeverityColumnInIssuesImpactsTable.MANUAL_SEVERITY, java.sql.Types.BOOLEAN, null, false);
  }

  @Test
  void execute_whenColumnAlreadyExists_shouldNotFail() throws Exception {
    underTest.execute();
    assertDoesNotThrow(() -> underTest.execute());
  }

  @Test
  void execute_is_reentrant() throws Exception {
    underTest.execute();
    underTest.execute();
    db.assertColumnDefinition(AddManualSeverityColumnInIssuesImpactsTable.TABLE_NAME, AddManualSeverityColumnInIssuesImpactsTable.MANUAL_SEVERITY, java.sql.Types.BOOLEAN, null, false);
  }

}
