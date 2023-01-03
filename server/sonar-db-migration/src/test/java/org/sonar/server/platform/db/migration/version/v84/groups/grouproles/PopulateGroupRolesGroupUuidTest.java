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
package org.sonar.server.platform.db.migration.version.v84.groups.grouproles;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateGroupRolesGroupUuidTest {
  private static final String TABLE_NAME = "group_roles";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateGroupRolesGroupUuidTest.class, "schema.sql");

  private DataChange underTest = new PopulateGroupRolesGroupUuid(db.database());

  @Test
  public void populate_uuids() throws SQLException {
    insertGroup(1L);
    insertGroup(2L);
    insertGroup(3L);

    insertGroupRole(4L, 1L);
    insertGroupRole(5L, 2L);
    insertGroupRole(6L, 3L);

    underTest.execute();

    assertThatTableContains(
      tuple("uuid4", 1L, "uuid1"),
      tuple("uuid5", 2L, "uuid2"),
      tuple("uuid6", 3L, "uuid3")
    );
  }

  @Test
  public void delete_orphan_rows() throws SQLException {
    insertGroup(1L);
    insertGroup(2L);
    insertGroup(3L);

    insertGroupRole(4L, 1L);
    insertGroupRole(5L, 2L);
    insertGroupRole(6L, 10L);
    insertGroupRole(7L, null);

    underTest.execute();

    assertThatTableContains(
      tuple("uuid4", 1L, "uuid1"),
      tuple("uuid5", 2L, "uuid2"),
      tuple("uuid7", null, null)
    );
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertGroup(1L);
    insertGroup(2L);
    insertGroup(3L);

    insertGroupRole(4L, 1L);
    insertGroupRole(5L, 2L);
    insertGroupRole(6L, 3L);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThatTableContains(
      tuple("uuid4", 1L, "uuid1"),
      tuple("uuid5", 2L, "uuid2"),
      tuple("uuid6", 3L, "uuid3")
    );
  }

  private void assertThatTableContains(Tuple... tuples) {
    List<Map<String, Object>> select = db.select("select uuid, group_id, group_uuid from " + TABLE_NAME);
    assertThat(select).extracting(m -> m.get("UUID"), m -> m.get("GROUP_ID"), m -> m.get("GROUP_UUID"))
      .containsExactlyInAnyOrder(tuples);
  }

  private void insertGroup(Long id) {
    db.executeInsert("groups",
      "id", id,
      "uuid", "uuid" + id,
      "organization_uuid", "org" + id,
      "name", "name" + id);
  }

  private void insertGroupRole(Long id, Long groupId) {
    db.executeInsert(TABLE_NAME,
      "uuid", "uuid" + id,
      "group_id", groupId,
      "role", "role" + id,
      "organization_uuid", "org" + id
    );
  }
}
