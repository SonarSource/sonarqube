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

package org.sonar.server.platform.db.migration.version.v81;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static java.sql.Types.BIGINT;
import static java.sql.Types.VARCHAR;

public class CreateAlmSettingsTableTest {

  private static final String TABLE_NAME = "alm_settings";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createEmpty();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CreateAlmSettingsTable underTest = new CreateAlmSettingsTable(dbTester.database());

  @Test
  public void table_has_been_created() throws SQLException {
    underTest.execute();

    dbTester.assertTableExists(TABLE_NAME);
    dbTester.assertPrimaryKey(TABLE_NAME, "pk_alm_settings", "uuid");
    dbTester.assertUniqueIndex(TABLE_NAME, "uniq_alm_settings", "kee");

    dbTester.assertColumnDefinition(TABLE_NAME, "uuid", VARCHAR, 40, false);
    dbTester.assertColumnDefinition(TABLE_NAME, "kee", VARCHAR, 200, false);
    dbTester.assertColumnDefinition(TABLE_NAME, "alm_id", VARCHAR, 40, false);
    dbTester.assertColumnDefinition(TABLE_NAME, "url", VARCHAR, 2000, true);
    dbTester.assertColumnDefinition(TABLE_NAME, "app_id", VARCHAR, 80, true);
    dbTester.assertColumnDefinition(TABLE_NAME, "private_key", VARCHAR, 2000, true);
    dbTester.assertColumnDefinition(TABLE_NAME, "pat", VARCHAR, 2000, true);
    dbTester.assertColumnDefinition(TABLE_NAME, "updated_at", BIGINT, 20, false);
    dbTester.assertColumnDefinition(TABLE_NAME, "created_at", BIGINT, 20, false);

    // script should not fail if executed twice
    underTest.execute();
  }

}
