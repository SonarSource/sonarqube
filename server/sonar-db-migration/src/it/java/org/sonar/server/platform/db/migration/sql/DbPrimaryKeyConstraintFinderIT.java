/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.CoreDbTester;
import org.sonar.db.Database;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DbPrimaryKeyConstraintFinderIT {

  @RegisterExtension
  public final CoreDbTester db = CoreDbTester.createForSchema(DbPrimaryKeyConstraintFinderIT.class, "schema.sql");

  private final Database dbMock = mock(Database.class);
  private final DbPrimaryKeyConstraintFinder underTest = new DbPrimaryKeyConstraintFinder(dbMock);

  private static final PostgreSql POSTGRESQL = new PostgreSql();
  private static final MsSql MS_SQL = new MsSql();
  private static final Oracle ORACLE = new Oracle();
  private static final org.sonar.db.dialect.H2 H2 = new H2();

  @Test
  void findConstraintName_constraint_exists() throws SQLException {
    DbPrimaryKeyConstraintFinder underTest = new DbPrimaryKeyConstraintFinder(db.database());
    Optional<String> constraintName = underTest.findConstraintName("TEST_PRIMARY_KEY");
    assertThat(constraintName).isPresent();
    assertThat(constraintName.get()).contains("PK_TEST_PRIMARY_KEY");
  }

  @Test
  void findConstraintName_constraint_not_exist_fails_silently() throws SQLException {
    DbPrimaryKeyConstraintFinder underTest = new DbPrimaryKeyConstraintFinder(db.database());
    assertThat(underTest.findConstraintName("NOT_EXISTING_TABLE")).isNotPresent();
  }

  @Test
  void getDbVendorSpecificQuery_mssql() {
    when(dbMock.getDialect()).thenReturn(MS_SQL);

    assertThat(underTest.getDbVendorSpecificQuery("my_table"))
      .isEqualTo("SELECT name FROM sys.key_constraints WHERE type = 'PK' AND OBJECT_NAME(parent_object_id) = 'my_table'");
  }

  @Test
  void getDbVendorSpecificQuery_postgresql() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getSchema()).thenReturn("SonarQube");
    when(dbMock.getDialect()).thenReturn(POSTGRESQL);
    when(dbMock.getDataSource()).thenReturn(dataSource);

    assertThat(underTest.getDbVendorSpecificQuery("my_table"))
      .isEqualTo("SELECT conname FROM pg_constraint c JOIN pg_namespace n on c.connamespace = n.oid JOIN pg_class cls on c.conrelid = cls.oid WHERE cls.relname = 'my_table' AND n.nspname = 'SonarQube'");
  }

  @Test
  void getDbVendorSpecificQuery_oracle() {
    when(dbMock.getDialect()).thenReturn(ORACLE);

    assertThat(underTest.getDbVendorSpecificQuery("my_table"))
      .isEqualTo("SELECT constraint_name FROM user_constraints WHERE table_name = UPPER('my_table') AND constraint_type='P'");
  }

  @Test
  void getDbVendorSpecificQuery_h2() {
    when(dbMock.getDialect()).thenReturn(H2);

    assertThat(underTest.getDbVendorSpecificQuery("my_table"))
      .isEqualTo("SELECT constraint_name FROM information_schema.table_constraints WHERE table_name = 'MY_TABLE' and constraint_type = 'PRIMARY KEY'");
  }
}
