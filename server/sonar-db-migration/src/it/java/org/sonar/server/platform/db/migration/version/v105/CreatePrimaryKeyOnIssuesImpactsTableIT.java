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
package org.sonar.server.platform.db.migration.version.v105;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v105.CreatePrimaryKeyOnIssuesImpactsTable.ISSUE_KEY_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v105.CreatePrimaryKeyOnIssuesImpactsTable.PK_NAME;
import static org.sonar.server.platform.db.migration.version.v105.CreatePrimaryKeyOnIssuesImpactsTable.SOFTWARE_QUALITY_COLUMN;
import static org.sonar.server.platform.db.migration.version.v105.CreatePrimaryKeyOnIssuesImpactsTable.TABLE_NAME;

class CreatePrimaryKeyOnIssuesImpactsTableIT {
  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreatePrimaryKeyOnIssuesImpactsTable.class);

  private final CreatePrimaryKeyOnIssuesImpactsTable createIndex = new CreatePrimaryKeyOnIssuesImpactsTable(db.database());

  @Test
  void execute_whenPrimaryKeyDoesntExist_shouldCreatePrimaryKey() throws SQLException {
    db.assertNoPrimaryKey(TABLE_NAME);

    createIndex.execute();
    db.assertPrimaryKey(TABLE_NAME, PK_NAME, ISSUE_KEY_COLUMN_NAME, SOFTWARE_QUALITY_COLUMN);
  }

  @Test
  void execute_whenPrimaryKeyAlreadyExist_shouldKeepThePrimaryKeyAndNotFail() throws SQLException {
    createIndex.execute();
    createIndex.execute();

    db.assertPrimaryKey(TABLE_NAME, PK_NAME, ISSUE_KEY_COLUMN_NAME, SOFTWARE_QUALITY_COLUMN);
  }
}
