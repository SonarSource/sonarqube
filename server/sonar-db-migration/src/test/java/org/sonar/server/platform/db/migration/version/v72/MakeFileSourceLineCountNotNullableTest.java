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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static java.sql.Types.INTEGER;

public class MakeFileSourceLineCountNotNullableTest {
  private static final String TABLE_NAME = "file_sources";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(MakeFileSourceLineCountNotNullableTest.class, "file_sources.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MakeFileSourceLineCountNotNullable underTest = new MakeFileSourceLineCountNotNullable(dbTester.database());

  @Test
  public void column_is_made_not_nullable() throws SQLException {
    underTest.execute();

    dbTester.assertColumnDefinition(TABLE_NAME, "line_count", INTEGER, null, false);
  }

  @Test
  public void migration_does_not_fix_null_values_in_line_count() throws SQLException {
    dbTester.executeInsert(
      TABLE_NAME,
      "PROJECT_UUID", "foo_prj",
      "FILE_UUID", "foo_file",
      "CREATED_AT", 123456,
      "UPDATED_AT", 987654
    );

    expectedException.expect(IllegalStateException.class);

    underTest.execute();
  }
}
