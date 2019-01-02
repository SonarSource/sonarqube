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
package org.sonar.db.schemamigration;

import java.sql.Statement;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class SchemaMigrationDaoTest {
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbSession dbSession = dbTester.getSession();
  private SchemaMigrationDao underTest = dbTester.getDbClient().schemaMigrationDao();

  @After
  public void tearDown() throws Exception {
    // schema_migration is not cleared by DbTester
    try(Statement statement = dbTester.getSession().getConnection().createStatement()) {
      statement.execute("truncate table schema_migrations");
    }
  }

  @Test
  public void insert_fails_with_NPE_if_argument_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("version can't be null");

    underTest.insert(dbSession, null);
  }

  @Test
  public void insert_fails_with_IAE_if_argument_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("version can't be empty");

    underTest.insert(dbSession, "");
  }

  @Test
  public void getVersions_returns_an_empty_list_if_table_is_empty() {
    assertThat(underTest.selectVersions(dbSession)).isEmpty();
  }

  @Test
  public void getVersions_returns_all_versions_in_table() {
    underTest.insert(dbSession, "22");
    underTest.insert(dbSession, "1");
    underTest.insert(dbSession, "3");
    dbSession.commit();

    assertThat(underTest.selectVersions(dbSession)).containsOnly(22, 1, 3);
  }
}
