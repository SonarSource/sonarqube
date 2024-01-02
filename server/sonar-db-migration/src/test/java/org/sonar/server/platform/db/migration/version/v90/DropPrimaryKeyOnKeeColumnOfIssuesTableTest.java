/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v90;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.db.Database;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.Oracle;
import org.sonar.server.platform.db.migration.sql.DbPrimaryKeyConstraintFinder;
import org.sonar.server.platform.db.migration.sql.DropPrimaryKeySqlGenerator;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DropPrimaryKeyOnKeeColumnOfIssuesTableTest {

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(DropPrimaryKeyOnKeeColumnOfIssuesTableTest.class, "schema.sql");

  private final DropPrimaryKeyOnKeeColumnOfIssuesTable underTest = new DropPrimaryKeyOnKeeColumnOfIssuesTable(
    db.database(),
    new DropPrimaryKeySqlGenerator(db.database(), new DbPrimaryKeyConstraintFinder(db.database())));

  @Test
  public void migration_should_have_no_effect_when_using_h2_database() throws SQLException {
    db.assertPrimaryKey("issues", "pk_issues", "kee");
    underTest.execute();
    db.assertPrimaryKey("issues", "pk_issues", "kee");
  }

  @Test
  public void migration_should_call_generator_when_using_oracle_database() throws SQLException {
    DropPrimaryKeySqlGenerator generator = mock(DropPrimaryKeySqlGenerator.class);
    Database mockedDb = mock(Database.class);
    DropPrimaryKeyOnKeeColumnOfIssuesTable underTest = new DropPrimaryKeyOnKeeColumnOfIssuesTable(
      mockedDb, generator);

    DdlChange.Context context = mock(DdlChange.Context.class);
    Dialect dialect = mock(Dialect.class);
    when(dialect.getId()).thenReturn(Oracle.ID);
    when(mockedDb.getDialect()).thenReturn(dialect);

    underTest.execute(context);

    verify(generator, only()).generate("issues", "kee", false);
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    db.assertPrimaryKey("issues", "pk_issues", "kee");
    underTest.execute();
    // re-entrant
    underTest.execute();
    db.assertPrimaryKey("issues", "pk_issues", "kee");
  }
}
