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
package org.sonar.server.platform.db.migration.version.v202505;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v202505.AddFromSonarQubeUpdateColumnToIssuesTable.ISSUES_TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202505.AddFromSonarQubeUpdateColumnToIssuesTable.FROM_SONARQUBE_UPDATE_COLUMN_NAME;

class AddFromSonarQubeUpdateColumnToIssuesTableTest {
  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddFromSonarQubeUpdateColumnToIssuesTable.class);

  private final AddFromSonarQubeUpdateColumnToIssuesTable underTest = new AddFromSonarQubeUpdateColumnToIssuesTable(db.database());

  @Test
  void migration_should_add_issue_from_sonar_qube_update_column() throws SQLException {
    db.assertColumnDoesNotExist(ISSUES_TABLE_NAME, FROM_SONARQUBE_UPDATE_COLUMN_NAME);
    underTest.execute();
    db.assertColumnDefinition(ISSUES_TABLE_NAME, FROM_SONARQUBE_UPDATE_COLUMN_NAME, Types.BOOLEAN, null, false);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertColumnDoesNotExist(ISSUES_TABLE_NAME, FROM_SONARQUBE_UPDATE_COLUMN_NAME);
    underTest.execute();
    underTest.execute();
    db.assertColumnDefinition(ISSUES_TABLE_NAME, FROM_SONARQUBE_UPDATE_COLUMN_NAME, Types.BOOLEAN, null, false);
  }
}