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

public class FixLengthOfIssuesMessageOnOracleTest {

  Database db = mock(Database.class, RETURNS_DEEP_STUBS);
  DdlChange.Context context = mock(DdlChange.Context.class);
  FixLengthOfIssuesMessageOnOracle underTest = new FixLengthOfIssuesMessageOnOracle(db);

  @Test
  public void alter_column_if_upgrading_oracle() throws SQLException {
    prepare(new Oracle());

    underTest.execute(context);

    verify(context).execute("ALTER TABLE issues MODIFY (message VARCHAR (4000 CHAR))");
  }

  @Test
  public void do_not_alter_column_if_not_oracle() throws SQLException {
    prepare(new MySql());

    underTest.execute(context);

    verifyZeroInteractions(context);
  }

  private void prepare(Dialect dialect) {
    when(db.getDialect()).thenReturn(dialect);
  }
}
