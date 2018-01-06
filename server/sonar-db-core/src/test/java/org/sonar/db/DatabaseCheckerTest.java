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
package org.sonar.db;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.utils.MessageException;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseCheckerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void requires_oracle_driver_11_2() throws Exception {
    Database db = mockDb(new Oracle(), 11, 2, "11.2.0.0.1");
    new DatabaseChecker(db).start();
    // no error

    db = mockDb(new Oracle(), 11, 2, "11.3.1");
    new DatabaseChecker(db).start();
    // no error

    db = mockDb(new Oracle(), 11, 2, "12.0.2");
    new DatabaseChecker(db).start();
    // no error

    db = mockDb(new Oracle(), 11, 2, "11.1.0.2");
    try {
      new DatabaseChecker(db).start();
      fail();
    } catch (MessageException e) {
      assertThat(e).hasMessage("Unsupported Oracle driver version: 11.1.0.2. Minimal supported version is 11.2.");
    }
  }

  @Test
  public void requires_oracle_11g_or_greater() throws Exception {
    // oracle 11.0 is ok
    Database db = mockDb(new Oracle(), 11, 0, "11.2.0.0.1");
    new DatabaseChecker(db).start();

    // oracle 11.1 is ok
    db = mockDb(new Oracle(), 11, 1, "11.2.0.0.1");
    new DatabaseChecker(db).start();

    // oracle 11.2 is ok
    db = mockDb(new Oracle(), 11, 2, "11.2.0.0.1");
    new DatabaseChecker(db).start();

    // oracle 12 is ok
    db = mockDb(new Oracle(), 12, 0, "11.2.0.0.1");
    new DatabaseChecker(db).start();

    // oracle 10 is not supported
    db = mockDb(new Oracle(), 10, 2, "11.2.0.0.1");
    try {
      new DatabaseChecker(db).start();
      fail();
    } catch (MessageException e) {
      assertThat(e).hasMessage("Unsupported oracle version: 10.2. Minimal supported version is 11.0.");
    }
  }

  @Test
  public void log_warning_if_h2() throws Exception {
    Database db = mockDb(new H2(), 13, 4, "13.4");
    DatabaseChecker checker = new DatabaseChecker(db);
    checker.start();
    checker.stop();
    // TODO test log
  }

  @Test
  public void test_mysql() throws Exception {
    Database db = mockDb(new MySql(), 5, 7, "5.7");
    new DatabaseChecker(db).start();
    // no error
  }

  @Test
  public void mssql_2012_is_not_supported() throws Exception {
    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Unsupported mssql version: 11.0. Minimal supported version is 12.0.");

    Database db = mockDb(new MsSql(), 11, 0, "6.1");
    new DatabaseChecker(db).start();
    // no error
  }

  @Test
  public void mssql_2014_is_supported() throws Exception {
    Database db = mockDb(new MsSql(), 12, 0, "6.1");
    new DatabaseChecker(db).start();
    // no error
  }

  @Test
  public void fail_if_mysql_less_than_5_6() throws Exception {
    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Unsupported mysql version: 5.5. Minimal supported version is 5.6.");

    Database db = mockDb(new MySql(), 5, 5, "5.6");
    new DatabaseChecker(db).start();
  }

  @Test
  public void fail_if_cant_get_db_version() throws Exception {
    SQLException sqlException = new SQLException();
    Database db = mock(Database.class, Mockito.RETURNS_DEEP_STUBS);
    when(db.getDialect()).thenReturn(new MySql());
    when(db.getDataSource().getConnection().getMetaData()).thenThrow(sqlException);

    try {
      new DatabaseChecker(db).start();
      fail();
    } catch (RuntimeException e) {
      assertThat(e.getCause()).isSameAs(sqlException);
    }
  }

  private Database mockDb(Dialect dialect, int dbMajorVersion, int dbMinorVersion, String driverVersion) throws SQLException {
    Database db = mock(Database.class, Mockito.RETURNS_DEEP_STUBS);
    when(db.getDialect()).thenReturn(dialect);
    when(db.getDataSource().getConnection().getMetaData().getDatabaseMajorVersion()).thenReturn(dbMajorVersion);
    when(db.getDataSource().getConnection().getMetaData().getDatabaseMinorVersion()).thenReturn(dbMinorVersion);
    when(db.getDataSource().getConnection().getMetaData().getDriverVersion()).thenReturn(driverVersion);
    return db;
  }
}
