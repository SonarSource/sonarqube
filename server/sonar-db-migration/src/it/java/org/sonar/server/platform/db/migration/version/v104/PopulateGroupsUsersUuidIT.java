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
package org.sonar.server.platform.db.migration.version.v104;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateGroupsUsersUuidIT {

  private static final String GROUPS_USERS_TABLE_NAME = "groups_users";
  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateGroupsUsersUuid.class);

  private final PopulateGroupsUsersUuid migration = new PopulateGroupsUsersUuid(db.database(), UuidFactoryFast.getInstance());

  @Test
  public void execute_whenTableIsEmpty_shouldPopulate() throws SQLException {
    insertRowsWithoutUuid();

    migration.execute();

    verifyUuidPresentAndUnique();
  }



  @Test
  public void execute_isReentrant() throws SQLException {
    insertRowsWithoutUuid();
    migration.execute();
    List<Tuple> existingUuids = getExistingUuids();

    migration.execute();
    verifyUuidsNotChanged(existingUuids);

    migration.execute();
    verifyUuidsNotChanged(existingUuids);
  }

  private void insertRowsWithoutUuid() {
    db.executeInsert(GROUPS_USERS_TABLE_NAME,
      "uuid", null,
      "group_uuid", "group1_uuid",
      "user_uuid", "user1_uuid");

    db.executeInsert(GROUPS_USERS_TABLE_NAME,
      "uuid", null,
      "group_uuid", "group2_uuid",
      "user_uuid", "user2_uuid");

    db.executeInsert(GROUPS_USERS_TABLE_NAME,
      "uuid", null,
      "group_uuid", "group3_uuid",
      "user_uuid", "user3_uuid");
  }

  private void verifyUuidPresentAndUnique() {
    List<Map<String, Object>> rows = db.select("select uuid, group_uuid, user_uuid from groups_users");
    rows
      .forEach(stringObjectMap -> assertThat(stringObjectMap.get("UUID")).isNotNull());
    long uniqueCount = rows.stream().map(row -> row.get("UUID")).distinct().count();
    assertThat(uniqueCount).isEqualTo(rows.size());

  }

  private List<Tuple> getExistingUuids() {
    return db.select("select uuid, group_uuid, user_uuid from groups_users")
      .stream()
      .map(stringObjectMap -> tuple(stringObjectMap.get("UUID"), stringObjectMap.get("GROUP_UUID"), stringObjectMap.get("USER_UUID")))
      .toList();
  }

  private void verifyUuidsNotChanged(List<Tuple> existingUuids) {
    assertThat(db.select("select uuid, group_uuid, user_uuid from groups_users"))
      .extracting(stringObjectMap -> tuple(stringObjectMap.get("UUID"), stringObjectMap.get("GROUP_UUID"), stringObjectMap.get("USER_UUID")))
      .containsExactlyInAnyOrderElementsOf(existingUuids);
  }



}
