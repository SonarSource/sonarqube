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
package org.sonar.server.platform.db.migration.version.v70;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

public class CreateTempTableLiveMeasuresPTest {
  @Rule
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(CreateTempTableLiveMeasuresPTest.class, "empty.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CreateTempTableLiveMeasuresP underTest = new CreateTempTableLiveMeasuresP(dbTester.database());

  @Test
  public void create_table_live_measures_p() throws SQLException {
    underTest.execute();

    dbTester.assertColumnDefinition("live_measures_p", "project_uuid", Types.VARCHAR, 50, false);
    dbTester.assertPrimaryKey("live_measures_p", "pk_live_measures_p", "project_uuid");
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    underTest.execute();

    underTest.execute();
  }
}
