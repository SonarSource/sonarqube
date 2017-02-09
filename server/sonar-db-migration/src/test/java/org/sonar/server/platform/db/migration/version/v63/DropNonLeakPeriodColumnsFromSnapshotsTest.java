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

package org.sonar.server.platform.db.migration.version.v63;

import java.sql.SQLException;
import org.junit.Test;
import org.sonar.db.Database;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DropNonLeakPeriodColumnsFromSnapshotsTest {

  Database database = mock(Database.class);

  DropNonLeakPeriodColumnsFromSnapshots underTest = new DropNonLeakPeriodColumnsFromSnapshots(database);

  @Test
  public void verify_generated_sql_on_postgresql() throws SQLException {
    when(database.getDialect()).thenReturn(new PostgreSql());

    DdlChange.Context context = mock(DdlChange.Context.class);
    underTest.execute(context);

    verify(context).execute("ALTER TABLE snapshots " +
      "DROP COLUMN period2_mode, DROP COLUMN period2_param, DROP COLUMN period2_date, " +
      "DROP COLUMN period3_mode, DROP COLUMN period3_param, DROP COLUMN period3_date, " +
      "DROP COLUMN period4_mode, DROP COLUMN period4_param, DROP COLUMN period4_date, " +
      "DROP COLUMN period5_mode, DROP COLUMN period5_param, DROP COLUMN period5_date");
  }

}
