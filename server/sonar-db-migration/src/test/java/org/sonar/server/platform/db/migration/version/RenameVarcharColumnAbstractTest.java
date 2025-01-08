/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version;

import java.sql.SQLException;
import org.sonar.db.AbstractDbTester;
import org.sonar.db.TestDb;
import org.sonar.server.platform.db.migration.step.RenameVarcharColumnChange;

import static java.sql.Types.VARCHAR;

public abstract class RenameVarcharColumnAbstractTest {

  private final String tableName;
  private final String columnName;
  private final boolean isNullable;

  public RenameVarcharColumnAbstractTest(String tableName, String columnName, boolean isNullable) {
    this.tableName = tableName;
    this.columnName = columnName;
    this.isNullable = isNullable;
  }

  protected void verifyMigrationIsReentrant() throws SQLException {
    getDatabase().assertColumnDoesNotExist(tableName, columnName);
    getClassUnderTest().execute();
    getClassUnderTest().execute();
    getDatabase().assertColumnDefinition(tableName, columnName, VARCHAR, 40, isNullable);
  }

  protected void verifyColumnIsRenamed() throws SQLException {
    getDatabase().assertColumnDoesNotExist(tableName, columnName);
    getClassUnderTest().execute();
    getDatabase().assertColumnDefinition(tableName, columnName, VARCHAR, 40, isNullable);
  }

  protected abstract RenameVarcharColumnChange getClassUnderTest();

  protected abstract AbstractDbTester<? extends TestDb> getDatabase();
}
