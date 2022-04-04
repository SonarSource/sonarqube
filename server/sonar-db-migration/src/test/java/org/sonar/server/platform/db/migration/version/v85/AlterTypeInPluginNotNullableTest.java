/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v85;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;

public class AlterTypeInPluginNotNullableTest {
  private static final String TABLE_NAME = "plugins";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AlterTypeInPluginNotNullableTest.class, "schema.sql");

  private MigrationStep underTest = new AlterTypeInPluginNotNullable(db.database());

  @Test
  public void add_column() throws SQLException {
    addPlugin("1");
    addPlugin("2");
    underTest.execute();
    db.assertColumnDefinition(TABLE_NAME, "type", Types.VARCHAR, 10, false);
    assertThat(db.countRowsOfTable(TABLE_NAME)).isEqualTo(2);
  }

  private void addPlugin(String id) {
    db.executeInsert(TABLE_NAME,
      "uuid", "uuid" + id,
      "kee", "kee" + id,
      "base_plugin_key", "base" + id,
      "file_hash", "hash" + id,
      "type", "BUNDLED",
      "created_at", 1L,
      "updated_at", 2L);
  }
}
