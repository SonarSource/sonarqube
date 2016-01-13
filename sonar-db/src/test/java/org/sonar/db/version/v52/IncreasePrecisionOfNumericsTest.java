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
package org.sonar.db.version.v52;

import org.junit.Test;
import org.sonar.db.Database;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.db.version.DdlChange;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * It's not possible to test the change of type to NUMERIC (38,20) with a H2 db because on H2 numeric type is a DOUBLE, without precision and scale.
 * That's why mocks are used in this test.
 */
public class IncreasePrecisionOfNumericsTest {

  DdlChange.Context context = mock(DdlChange.Context.class);
  Database db = mock(Database.class);

  IncreasePrecisionOfNumerics underTest = new IncreasePrecisionOfNumerics(db);

  @Test
  public void update_column_types() throws Exception {
    when(db.getDialect()).thenReturn(new PostgreSql());

    underTest.execute(context);

    verify(context).execute("ALTER TABLE metrics ALTER COLUMN worst_value TYPE NUMERIC (38,20), ALTER COLUMN best_value TYPE NUMERIC (38,20)");
    verify(context).execute("ALTER TABLE project_measures " +
      "ALTER COLUMN value TYPE NUMERIC (38,20), " +
      "ALTER COLUMN variation_value_1 TYPE NUMERIC (38,20), " +
      "ALTER COLUMN variation_value_2 TYPE NUMERIC (38,20), " +
      "ALTER COLUMN variation_value_3 TYPE NUMERIC (38,20), " +
      "ALTER COLUMN variation_value_4 TYPE NUMERIC (38,20), " +
      "ALTER COLUMN variation_value_5 TYPE NUMERIC (38,20)");
    verify(context).execute("ALTER TABLE manual_measures ALTER COLUMN value TYPE NUMERIC (38,20)");
  }

}
