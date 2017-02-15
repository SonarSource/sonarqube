/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static java.lang.String.valueOf;

public class MakeUuidColumnsNotNullOnResourceIndexTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MakeUuidColumnsNotNullOnResourceIndexTest.class,
    "in_progress_resourceindex.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MakeUuidColumnsNotNullOnResourceIndex underTest = new MakeUuidColumnsNotNullOnResourceIndex(db.database());

  @Test
  public void migration_sets_uuid_columns_not_nullable_on_empty_table() throws SQLException {
    underTest.execute();

    verifyColumnDefinitions();
    verifyIndex();
  }

  @Test
  public void migration_sets_uuid_columns_not_nullable_on_populated_table() throws SQLException {
    insertResourceIndex(1, true, true);
    insertResourceIndex(2, true, true);

    underTest.execute();

    verifyColumnDefinitions();
    verifyIndex();
  }

  @Test
  public void migration_fails_if_some_uuid_columns_are_null() throws SQLException {
    insertResourceIndex(1, false, true);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to execute");

    underTest.execute();
  }

  private void verifyColumnDefinitions() {
    db.assertColumnDefinition("resource_index", "component_uuid", Types.VARCHAR, 50, false);
    db.assertColumnDefinition("resource_index", "root_component_uuid", Types.VARCHAR, 50, false);
  }

  private void verifyIndex() {
    db.assertIndex("resource_index", "resource_index_component", "component_uuid");
  }

  private void insertResourceIndex(long id, boolean hasComponentUiid, boolean hasRootComponentUuid) {
    db.executeInsert(
      "resource_index",
      "ID", valueOf(id),
      "KEE", "key_" + id,
      "POSITION", valueOf(id + 100),
      "NAME_SIZE", valueOf(id + 1000),
      "RESOURCE_ID", valueOf(id + 300),
      "ROOT_PROJECT_ID", valueOf(id + 4000),
      "QUALIFIER", "PROJECT");

    if (hasComponentUiid) {
      db.executeUpdateSql("update resource_index set COMPONENT_UUID=? where id=?", "uuid_" + id, valueOf(id));
    }
    if (hasRootComponentUuid) {
      db.executeUpdateSql("update resource_index set ROOT_COMPONENT_UUID=? where id=?", "root_uuid_" + id, valueOf(id));
    }
  }
}
