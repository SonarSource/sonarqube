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
package org.sonar.server.platform.db.migration.version.v76;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static java.sql.Types.BIGINT;
import static java.sql.Types.VARCHAR;

public class CreateUserPropertiesTableTest {

  private static final String TABLE = "user_properties";

  @Rule
  public final CoreDbTester db = CoreDbTester.createEmpty();

  private CreateUserPropertiesTable underTest = new CreateUserPropertiesTable(db.database());

  @Test
  public void creates_table() throws SQLException {
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
    db.assertPrimaryKey(TABLE, "pk_user_properties", "uuid");
    db.assertColumnDefinition(TABLE, "uuid", VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE, "user_uuid", VARCHAR, 255, false);
    db.assertColumnDefinition(TABLE, "kee", VARCHAR, 100, false);
    db.assertColumnDefinition(TABLE, "text_value", VARCHAR, 4000, false);
    db.assertColumnDefinition(TABLE, "created_at", BIGINT, null, false);
    db.assertColumnDefinition(TABLE, "updated_at", BIGINT, null, false);
  }
}
