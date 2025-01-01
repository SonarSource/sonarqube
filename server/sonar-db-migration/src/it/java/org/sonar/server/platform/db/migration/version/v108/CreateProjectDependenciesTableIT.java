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

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.sql.Types.BIGINT;
import static java.sql.Types.CLOB;
import static java.sql.Types.VARCHAR;
import static org.sonar.db.MigrationDbTester.createForMigrationStep;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

class CreateProjectDependenciesTableIT {

  private static final String TABLE_NAME = "project_dependencies";

  @RegisterExtension
  public final MigrationDbTester db = createForMigrationStep(CreateProjectDependenciesTable.class);

  private final DdlChange underTest = new CreateProjectDependenciesTable(db.database());

  @Test
  void execute_shouldCreateTable() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_project_dependencies", "uuid");
    db.assertColumnDefinition(TABLE_NAME, "uuid", VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, "version", CLOB, null, true);
    db.assertColumnDefinition(TABLE_NAME, "include_paths", CLOB, null, true);
    db.assertColumnDefinition(TABLE_NAME, "package_manager", VARCHAR, 50, true);
    db.assertColumnDefinition(TABLE_NAME, "created_at", BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, "updated_at", BIGINT, null, false);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);
    underTest.execute();

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }

}
