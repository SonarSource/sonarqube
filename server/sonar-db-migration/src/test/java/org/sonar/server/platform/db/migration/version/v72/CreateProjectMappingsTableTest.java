/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v72;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.sql.Types.VARCHAR;
import static org.assertj.core.api.Assertions.assertThat;

public class CreateProjectMappingsTableTest {

  private static final String TABLE = "project_mappings";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(CreateProjectMappingsTableTest.class, "empty.sql");

  private CreateProjectMappingsTable underTest = new CreateProjectMappingsTable(db.database());

  @Test
  public void creates_table_on_empty_db() throws SQLException {
    underTest.execute();

    checkTable();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    checkTable();
  }

  private void checkTable() {
    db.assertColumnDefinition(TABLE, "uuid", Types.VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE, "key_type", VARCHAR, 200, false);
    db.assertColumnDefinition(TABLE, "kee", VARCHAR, 4000, false);
    db.assertColumnDefinition(TABLE, "project_uuid", Types.VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE, "created_at", Types.BIGINT, 20, false);

    db.assertUniqueIndex(TABLE, "key_type_kee", "key_type", "kee");
    db.assertIndex(TABLE, "project_uuid", "project_uuid");
    assertThat(db.countRowsOfTable(TABLE)).isEqualTo(0);
  }
}
