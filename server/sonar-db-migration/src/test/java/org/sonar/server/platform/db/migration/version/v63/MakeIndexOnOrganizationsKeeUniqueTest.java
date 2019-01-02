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
package org.sonar.server.platform.db.migration.version.v63;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

public class MakeIndexOnOrganizationsKeeUniqueTest {
  private static final String TABLE_ORGANIZATIONS = "organizations";
  private static final String INDEX_NAME = "organization_key";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(MakeIndexOnOrganizationsKeeUniqueTest.class, "organizations_with_non_unique_index.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MakeIndexOnOrganizationsKeeUnique underTest = new MakeIndexOnOrganizationsKeeUnique(dbTester.database());

  @Test
  public void execute_makes_index_unique_on_empty_table() throws SQLException {
    dbTester.assertIndex(TABLE_ORGANIZATIONS, INDEX_NAME, "kee");

    underTest.execute();

    dbTester.assertUniqueIndex(TABLE_ORGANIZATIONS, INDEX_NAME, "kee");
  }

  @Test
  public void execute_makes_index_unique_on_non_empty_table_without_duplicates() throws SQLException {
    dbTester.assertIndex(TABLE_ORGANIZATIONS, INDEX_NAME, "kee");
    insert("1", "kee_1");
    insert("2", "kee_2");

    underTest.execute();

    dbTester.assertUniqueIndex(TABLE_ORGANIZATIONS, INDEX_NAME, "kee");
  }

  @Test
  public void execute_fails_non_empty_table_with_duplicates() throws SQLException {
    dbTester.assertIndex(TABLE_ORGANIZATIONS, INDEX_NAME, "kee");
    insert("1", "kee_1");
    insert("2", "kee_1");

    expectedException.expect(IllegalStateException.class);

    underTest.execute();
  }

  private void insert(String uuid, String kee) {
    dbTester.executeInsert(TABLE_ORGANIZATIONS,
      "UUID", uuid,
      "KEE", kee,
      "NAME", "name",
      "GUARDED", "false",
      "CREATED_AT", "1000",
      "UPDATED_AT", "2000");
  }
}
