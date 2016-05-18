/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.charset;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.db.Database;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.charset.DatabaseCharsetChecker.Flag.AUTO_REPAIR_COLLATION;
import static org.sonar.db.charset.DatabaseCharsetChecker.Flag.ENFORCE_UTF8;

public class DatabaseCharsetCheckerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  Database db = mock(Database.class, Mockito.RETURNS_MOCKS);
  CharsetHandler handler = mock(CharsetHandler.class);
  DatabaseCharsetChecker underTest = spy(new DatabaseCharsetChecker(db));

  @Test
  public void executes_handler() throws Exception {
    Oracle dialect = new Oracle();
    when(underTest.getHandler(dialect)).thenReturn(handler);
    when(db.getDialect()).thenReturn(dialect);

    underTest.check(ENFORCE_UTF8);
    verify(handler).handle(any(Connection.class), argThat(new TypeSafeMatcher<Set<DatabaseCharsetChecker.Flag>>() {
      @Override
      protected boolean matchesSafely(Set<DatabaseCharsetChecker.Flag> flags) {
        return flags.contains(ENFORCE_UTF8) && flags.size() == 1;
      }

      @Override
      public void describeTo(Description description) {
      }
    }));
  }

  @Test
  public void throws_ISE_if_handler_fails() throws Exception {
    Oracle dialect = new Oracle();
    when(underTest.getHandler(dialect)).thenReturn(handler);
    when(db.getDialect()).thenReturn(dialect);
    doThrow(new SQLException("failure")).when(handler).handle(any(Connection.class), anySet());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("failure");
    underTest.check(AUTO_REPAIR_COLLATION);
  }

  @Test
  public void does_nothing_if_h2() throws Exception {
    assertThat(underTest.getHandler(new H2())).isNull();
  }

  @Test
  public void getHandler_returns_MysqlCharsetHandler_if_mysql() throws Exception {
    assertThat(underTest.getHandler(new MySql())).isInstanceOf(MysqlCharsetHandler.class);
  }

  @Test
  public void getHandler_returns_MssqlCharsetHandler_if_mssql() throws Exception {
    assertThat(underTest.getHandler(new MsSql())).isInstanceOf(MssqlCharsetHandler.class);
  }

  @Test
  public void getHandler_returns_OracleCharsetHandler_if_oracle() throws Exception {
    assertThat(underTest.getHandler(new Oracle())).isInstanceOf(OracleCharsetHandler.class);
  }

  @Test
  public void getHandler_returns_PostgresCharsetHandler_if_postgres() throws Exception {
    assertThat(underTest.getHandler(new PostgreSql())).isInstanceOf(PostgresCharsetHandler.class);
  }

  @Test
  public void getHandler_throws_IAE_if_unsupported_db() throws Exception {
    Dialect unsupportedDialect = mock(Dialect.class);
    when(unsupportedDialect.getId()).thenReturn("foo");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Database not supported: foo");
    underTest.getHandler(unsupportedDialect);
  }
}
