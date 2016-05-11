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
package org.sonar.db.version.v56;

import java.sql.SQLException;
import org.junit.Test;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.db.Database;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.version.DdlChange;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class FixTypeOfRuleTypeOnMysqlTest {

  private static final int FRESH_MIGRATION_VERSION = -1;
  private static final int A_MIGRATION_VERSION_IN_5_5 = 1105;
  private static final int A_MIGRATION_VERSION_BEFORE_5_5 = 200;

  Database db = mock(Database.class, RETURNS_DEEP_STUBS);
  ServerUpgradeStatus dbVersion = mock(ServerUpgradeStatus.class);
  DdlChange.Context context = mock(DdlChange.Context.class);
  FixTypeOfRuleTypeOnMysql underTest = new FixTypeOfRuleTypeOnMysql(db, dbVersion);

  @Test
  public void alter_columns_if_upgrading_mysql_from_version_5_5() throws SQLException {
    prepare(A_MIGRATION_VERSION_IN_5_5, new MySql());

    underTest.execute(context);

    verify(context).execute("ALTER TABLE rules MODIFY COLUMN rule_type TINYINT (2)");
    verify(context).execute("ALTER TABLE issues MODIFY COLUMN issue_type TINYINT (2)");
  }

  @Test
  public void do_not_alter_columns_if_fresh_mysql_install() throws SQLException {
    prepare(FRESH_MIGRATION_VERSION, new MySql());

    underTest.execute(context);

    verifyZeroInteractions(context);
  }

  @Test
  public void do_not_alter_columns_if_upgrading_mysql_from_before_5_5() throws SQLException {
    prepare(A_MIGRATION_VERSION_BEFORE_5_5, new MySql());

    underTest.execute(context);

    verifyZeroInteractions(context);
  }

  @Test
  public void do_not_alter_columns_if_upgrading_from_5_5_but_not_mysql() throws SQLException {
    prepare(A_MIGRATION_VERSION_IN_5_5, new Oracle());

    underTest.execute(context);

    verifyZeroInteractions(context);
  }

  private void prepare(int initialVersion, Dialect dialect) {
    when(dbVersion.getInitialDbVersion()).thenReturn(initialVersion);
    when(db.getDialect()).thenReturn(dialect);
  }
}
