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
package org.sonar.server.platform.db.migration.version.v73;

import java.sql.SQLException;
import java.util.stream.Collectors;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class PopulateHotspotAdminPermissionOnUsersTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateHotspotAdminPermissionOnUsersTest.class, "user_roles.sql");

  private PopulateHotspotAdminPermissionOnUsers underTest = new PopulateHotspotAdminPermissionOnUsers(db.database());

  @Test
  public void insert_missing_permission() throws SQLException {
    insertUserRole("uuid", 1, 2, "noissueadmin");
    insertUserRole("uuid", 3, 4, "issueadmin");
    insertUserRole("uuid", 3, 4, "another");
    insertUserRole("uuid2", 5, 6, "securityhotspotadmin");

    underTest.execute();

    assertUserRoles(
      tuple("uuid", 1L, 2L, "noissueadmin"),
      tuple("uuid", 3L, 4L, "issueadmin"),
      tuple("uuid", 3L, 4L, "another"),
      tuple("uuid", 3L, 4L, "securityhotspotadmin"),
      tuple("uuid2", 5L, 6L, "securityhotspotadmin"));
  }

  private void insertUserRole(String orgaUuid, int userId, int resourceId, String role) {
    db.executeInsert(
      "USER_ROLES",
      "ORGANIZATION_UUID", orgaUuid,
      "USER_ID", userId,
      "RESOURCE_ID", resourceId,
      "ROLE", role);
  }

  private void assertUserRoles(Tuple... expectedTuples) {
    assertThat(db.select("SELECT ORGANIZATION_UUID, USER_ID, RESOURCE_ID, ROLE FROM USER_ROLES")
      .stream()
      .map(map -> new Tuple(map.get("ORGANIZATION_UUID"), map.get("USER_ID"), map.get("RESOURCE_ID"), map.get("ROLE")))
      .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

}
