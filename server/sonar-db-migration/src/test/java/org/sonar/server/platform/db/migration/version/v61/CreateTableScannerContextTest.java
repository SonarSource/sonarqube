/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v61;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateTableScannerContextTest {
  private static final String TABLE_CE_SCANNER_CONTEXT = "ce_scanner_context";

  @Rule
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(CreateTableCeTaskInputTest.class, "empty.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CreateTableScannerContext underTest = new CreateTableScannerContext(dbTester.database());

  @Test
  public void creates_table_on_empty_db() throws SQLException {
    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_CE_SCANNER_CONTEXT)).isEqualTo(0);

    dbTester.assertColumnDefinition(TABLE_CE_SCANNER_CONTEXT, "task_uuid", Types.VARCHAR, 40, false);
    dbTester.assertColumnDefinition(TABLE_CE_SCANNER_CONTEXT, "context_data", Types.BLOB, null, false);
    dbTester.assertColumnDefinition(TABLE_CE_SCANNER_CONTEXT, "created_at", Types.BIGINT, null, false);
    dbTester.assertColumnDefinition(TABLE_CE_SCANNER_CONTEXT, "updated_at", Types.BIGINT, null, false);
    dbTester.assertPrimaryKey(TABLE_CE_SCANNER_CONTEXT, "pk_" + TABLE_CE_SCANNER_CONTEXT, "task_uuid");
  }

  @Test
  public void migration_is_not_reentrant() throws SQLException {
    underTest.execute();

    expectedException.expect(IllegalStateException.class);

    underTest.execute();
  }
}
