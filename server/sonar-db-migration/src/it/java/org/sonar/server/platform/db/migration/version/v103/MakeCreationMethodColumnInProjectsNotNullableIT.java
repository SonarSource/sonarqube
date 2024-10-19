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
package org.sonar.server.platform.db.migration.version.v103;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static java.sql.Types.VARCHAR;
import static org.sonar.server.platform.db.migration.version.v103.AddCreationMethodColumnInProjectsTable.PROJECTS_CREATION_METHOD_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v103.AddCreationMethodColumnInProjectsTable.PROJECTS_TABLE_NAME;

class MakeCreationMethodColumnInProjectsNotNullableIT {
  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(MakeCreationMethodColumnInProjectsNotNullable.class);
  private final MakeCreationMethodColumnInProjectsNotNullable underTest = new MakeCreationMethodColumnInProjectsNotNullable(db.database());

  @Test
  void user_local_column_is_not_null() throws SQLException {
    db.assertColumnDefinition(PROJECTS_TABLE_NAME, PROJECTS_CREATION_METHOD_COLUMN_NAME, VARCHAR, null, true);
    underTest.execute();
    db.assertColumnDefinition(PROJECTS_TABLE_NAME, PROJECTS_CREATION_METHOD_COLUMN_NAME, VARCHAR, null, false);
  }

  @Test
  void migration_is_reentrant() throws SQLException {
    db.assertColumnDefinition(PROJECTS_TABLE_NAME, PROJECTS_CREATION_METHOD_COLUMN_NAME, VARCHAR, null, true);
    underTest.execute();
    underTest.execute();
    db.assertColumnDefinition(PROJECTS_TABLE_NAME, PROJECTS_CREATION_METHOD_COLUMN_NAME, VARCHAR, null, false);
  }
}
