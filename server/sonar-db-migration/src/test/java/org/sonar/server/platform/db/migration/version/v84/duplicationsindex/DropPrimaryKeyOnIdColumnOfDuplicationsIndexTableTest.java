/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.duplicationsindex;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.sql.DbPrimaryKeyConstraintFinder;
import org.sonar.server.platform.db.migration.sql.DropPrimaryKeySqlGenerator;
import org.sonar.server.platform.db.migration.step.MigrationStep;

public class DropPrimaryKeyOnIdColumnOfDuplicationsIndexTableTest {

  private static final String TABLE_NAME = "duplications_index";
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DropPrimaryKeyOnIdColumnOfDuplicationsIndexTableTest.class, "schema.sql");

  private final DropPrimaryKeySqlGenerator dropPrimaryKeySqlGenerator = new DropPrimaryKeySqlGenerator(db.database(), new DbPrimaryKeyConstraintFinder(db.database()));

  private final MigrationStep underTest = new DropPrimaryKeyOnIdColumnOfDuplicationsIndexTable(db.database(), dropPrimaryKeySqlGenerator);

  @Test
  public void execute() throws SQLException {
    underTest.execute();

    db.assertNoPrimaryKey(TABLE_NAME);
  }

  @Test
  public void migration_is_re_entrant_but_fails_silently() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertNoPrimaryKey(TABLE_NAME);
  }

}
