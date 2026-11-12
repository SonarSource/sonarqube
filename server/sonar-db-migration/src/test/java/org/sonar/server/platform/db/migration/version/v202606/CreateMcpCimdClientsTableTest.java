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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DropIndexChange;

import static java.sql.Types.BIGINT;
import static java.sql.Types.VARCHAR;
import static org.assertj.core.api.Assertions.assertThat;

class CreateMcpCimdClientsTableTest {

  @RegisterExtension
  final MigrationDbTester db = MigrationDbTester.createEmpty();

  private final CreateMcpCimdClientsTable underTest = new CreateMcpCimdClientsTable(db.database());

  @Test
  void creates_administration_table_with_required_columns_and_unique_registration_link() throws SQLException {
    db.assertTableDoesNotExist("mcp_cimd_clients");

    underTest.execute();

    db.assertPrimaryKey("mcp_cimd_clients", "pk_mcp_cimd_clients", "uuid");
    db.assertColumnDefinition("mcp_cimd_clients", "uuid", VARCHAR, 40, false);
    db.assertColumnDefinition("mcp_cimd_clients", "registered_client_id", VARCHAR, 100, false);
    db.assertColumnDefinition("mcp_cimd_clients", "metadata_source", VARCHAR, 20, false);
    db.assertColumnDefinition("mcp_cimd_clients", "created_at", BIGINT, null, false);
    db.assertColumnDefinition("mcp_cimd_clients", "updated_at", BIGINT, null, false);
    db.assertUniqueIndex("mcp_cimd_clients", "mcp_cimd_clients_reg_uniq", "registered_client_id");
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void is_reentrant_and_preserves_existing_clients(boolean missingUniqueIndex) throws SQLException {
    underTest.execute();
    db.executeInsert("mcp_cimd_clients",
      "uuid", "admin-uuid",
      "registered_client_id", "spring-id",
      "metadata_source", "REMOTE",
      "created_at", 1000L,
      "updated_at", 2000L);
    var existing = db.selectFirst("SELECT * FROM mcp_cimd_clients");

    if (missingUniqueIndex) {
      new DropIndexChange(db.database(), "mcp_cimd_clients_reg_uniq", "mcp_cimd_clients") {
      }.execute();
      db.assertIndexDoesNotExist("mcp_cimd_clients", "mcp_cimd_clients_reg_uniq");
    }

    underTest.execute();

    assertThat(db.selectFirst("SELECT * FROM mcp_cimd_clients")).isEqualTo(existing);
    db.assertUniqueIndex("mcp_cimd_clients", "mcp_cimd_clients_reg_uniq", "registered_client_id");
  }
}
