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
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class DropUserManagedColumnFromMetricsTableTest {
  private static final String COLUMN_NAME = "user_managed";
  private static final String TABLE_NAME = "metrics";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(DropUserManagedColumnFromMetricsTableTest.class, "schema.sql");

  private final DdlChange underTest = new DropUserManagedColumnFromMetricsTable(db.database());

  @Test
  public void migration_should_drop_unique_index_on_manual_measures() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.BOOLEAN, null, true);
    underTest.execute();
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_NAME);
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.BOOLEAN, null, true);
    underTest.execute();
    // re-entrant
    underTest.execute();
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_NAME);
  }
}
