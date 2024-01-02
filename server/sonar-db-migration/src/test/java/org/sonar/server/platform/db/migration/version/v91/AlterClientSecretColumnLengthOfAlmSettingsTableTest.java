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
package org.sonar.server.platform.db.migration.version.v91;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.sql.Types.VARCHAR;

public class AlterClientSecretColumnLengthOfAlmSettingsTableTest {
  private final String TABLE = "alm_settings";
  private final String COLUMN = "client_secret";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AlterClientSecretColumnLengthOfAlmSettingsTableTest.class, "schema.sql");

  private DdlChange underTest = new AlterClientSecretColumnLengthOfAlmSettingsTable(db.database());

  @Test
  public void client_secret_column_is_not_null() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition(TABLE, COLUMN, VARCHAR, 160, true);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition(TABLE, COLUMN, VARCHAR, 160, true);
  }
}
