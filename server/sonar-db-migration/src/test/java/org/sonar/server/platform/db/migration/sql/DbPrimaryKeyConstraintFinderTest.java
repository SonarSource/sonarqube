/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.sql;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.db.Database;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DbPrimaryKeyConstraintFinderTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DbPrimaryKeyConstraintFinderTest.class, "schema.sql");

  private final Database dbMock = mock(Database.class);
  private final DbPrimaryKeyConstraintFinder underTest = new DbPrimaryKeyConstraintFinder(dbMock);

  private static final PostgreSql POSTGRESQL = new PostgreSql();
  private static final MsSql MS_SQL = new MsSql();
  private static final Oracle ORACLE = new Oracle();
  private static final org.sonar.db.dialect.H2 H2 = new H2();

  @Test
  public void findConstraintName_constraint_exists() throws SQLException {
    DbPrimaryKeyConstraintFinder underTest = new DbPrimaryKeyConstraintFinder(db.database());
    String constraintName = underTest.findConstraintName("TEST_PRIMARY_KEY");
    assertThat(constraintName).isEqualTo("PK_TEST_PRIMARY_KEY");
  }

  @Test
  public void findConstraintName_constraint_not_exist() {
    DbPrimaryKeyConstraintFinder underTest = new DbPrimaryKeyConstraintFinder(db.database());
    assertThatThrownBy(() -> underTest.findConstraintName("NOT_EXISTING_TABLE"))
      .hasMessage("Cannot find constraint for table 'NOT_EXISTING_TABLE'")
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void getDbVendorSpecificQuery_mssql() {
    when(dbMock.getDialect()).thenReturn(MS_SQL);

    assertThat(underTest.getDbVendorSpecificQuery("my_table"))
      .isEqualTo("SELECT name FROM sys.key_constraints WHERE type = 'PK' AND OBJECT_NAME(parent_object_id) = 'my_table'");
  }

  @Test
  public void getDbVendorSpecificQuery_postgresql() {
    when(dbMock.getDialect()).thenReturn(POSTGRESQL);

    assertThat(underTest.getDbVendorSpecificQuery("my_table"))
      .isEqualTo("SELECT conname FROM pg_constraint WHERE conrelid =     (SELECT oid     FROM pg_class     WHERE relname LIKE 'my_table')");
  }

  @Test
  public void getDbVendorSpecificQuery_oracle() {
    when(dbMock.getDialect()).thenReturn(ORACLE);

    assertThat(underTest.getDbVendorSpecificQuery("my_table"))
      .isEqualTo("SELECT constraint_name FROM user_constraints WHERE table_name = UPPER('my_table') AND constraint_type='P'");
  }

  @Test
  public void getDbVendorSpecificQuery_h2() {
    when(dbMock.getDialect()).thenReturn(H2);

    assertThat(underTest.getDbVendorSpecificQuery("my_table"))
      .isEqualTo("SELECT constraint_name FROM information_schema.constraints WHERE table_name = 'MY_TABLE' and constraint_type = 'PRIMARY KEY'");
  }
}
