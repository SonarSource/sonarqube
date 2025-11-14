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
package org.sonar.server.platform.db.migration.version.v202506;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.sql.Types.BIGINT;
import static java.sql.Types.CLOB;
import static java.sql.Types.VARCHAR;
import static org.sonar.db.MigrationDbTester.createForMigrationStep;

class CreateJiraOrganizationBindingsPendingTableIT {
  private static final String TABLE_NAME = "jira_org_bindings_pending";

  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(CreateJiraOrganizationBindingsPendingTable.class);
  private final DdlChange underTest = new CreateJiraOrganizationBindingsPendingTable(db.database());

  @Test
  void execute_shouldCreateTable() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);
    underTest.execute();
    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_jira_org_bindings_pending", "id");
    db.assertColumnDefinition(TABLE_NAME, "id", VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE_NAME, "created_at", BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, "updated_at", BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, "sonar_organization_uuid", VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE_NAME, "jira_access_token", CLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, "jira_access_token_expires_at", BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, "jira_refresh_token", CLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, "jira_refresh_token_created_at", BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, "jira_refresh_token_updated_at", BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, "updated_by", VARCHAR, 40, false);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);
    underTest.execute();
    underTest.execute();
    db.assertTableExists(TABLE_NAME);
  }
}
