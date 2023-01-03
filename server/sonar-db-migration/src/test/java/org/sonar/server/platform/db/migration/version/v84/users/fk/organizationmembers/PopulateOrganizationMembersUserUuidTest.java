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
package org.sonar.server.platform.db.migration.version.v84.users.fk.organizationmembers;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateOrganizationMembersUserUuidTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateOrganizationMembersUserUuidTest.class, "schema.sql");

  private DataChange underTest = new PopulateOrganizationMembersUserUuid(db.database());

  @Test
  public void populate_uuids() throws SQLException {
    long userId_1 = 1L;
    String userUuid_1 = "uuid-1";
    insertUser(userId_1, userUuid_1);

    long userId_2 = 2L;
    String userUuid_2 = "uuid-2";
    insertUser(userId_2, userUuid_2);

    long userId_3 = 3L;
    String userUuid_3 = "uuid-3";
    insertUser(userId_3, userUuid_3);

    long userId_4 = 4L;
    String userUuid_4 = "uuid-4";
    insertUser(userId_4, userUuid_4);

    String organizationUuid_1 = Uuids.createFast();
    insertOrganizationMember(userId_1, organizationUuid_1);
    String organizationUuid_2 = Uuids.createFast();
    insertOrganizationMember(userId_2, organizationUuid_2);
    String organizationUuid_3 = Uuids.createFast();
    insertOrganizationMember(userId_3, organizationUuid_3);
    String organizationUuid_4 = Uuids.createFast();
    insertOrganizationMember(userId_4, organizationUuid_4);
    String organizationUuid_5 = Uuids.createFast();
    insertOrganizationMember(userId_1, organizationUuid_5);

    // orphan FK
    String organizationUuid_6 = Uuids.createFast();
    insertOrganizationMember(100L, organizationUuid_6);

    underTest.execute();

    assertThat(db.countRowsOfTable("organization_members")).isEqualTo(5);
    assertThatQProfileChangeRulesProfileUuidIsEqualTo(userId_1, organizationUuid_1, userUuid_1);
    assertThatQProfileChangeRulesProfileUuidIsEqualTo(userId_2, organizationUuid_2, userUuid_2);
    assertThatQProfileChangeRulesProfileUuidIsEqualTo(userId_3, organizationUuid_3, userUuid_3);
    assertThatQProfileChangeRulesProfileUuidIsEqualTo(userId_4, organizationUuid_4, userUuid_4);
    assertThatQProfileChangeRulesProfileUuidIsEqualTo(userId_1, organizationUuid_5, userUuid_1);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    long userId_1 = 1L;
    String userUuid_1 = "uuid-1";
    insertUser(userId_1, userUuid_1);

    long userId_2 = 2L;
    String userUuid_2 = "uuid-2";
    insertUser(userId_2, userUuid_2);

    long userId_3 = 3L;
    String userUuid_3 = "uuid-3";
    insertUser(userId_3, userUuid_3);

    long userId_4 = 4L;
    String userUuid_4 = "uuid-4";
    insertUser(userId_4, userUuid_4);

    String organizationUuid_1 = Uuids.createFast();
    insertOrganizationMember(userId_1, organizationUuid_1);
    String organizationUuid_2 = Uuids.createFast();
    insertOrganizationMember(userId_2, organizationUuid_2);
    String organizationUuid_3 = Uuids.createFast();
    insertOrganizationMember(userId_3, organizationUuid_3);

    underTest.execute();

    String organizationUuid_4 = Uuids.createFast();
    insertOrganizationMember(userId_3, organizationUuid_4);

    // re-entrant
    underTest.execute();

    assertThatQProfileChangeRulesProfileUuidIsEqualTo(userId_1, organizationUuid_1, userUuid_1);
    assertThatQProfileChangeRulesProfileUuidIsEqualTo(userId_2, organizationUuid_2, userUuid_2);
    assertThatQProfileChangeRulesProfileUuidIsEqualTo(userId_3, organizationUuid_3, userUuid_3);
    assertThatQProfileChangeRulesProfileUuidIsEqualTo(userId_3, organizationUuid_4, userUuid_3);
  }

  private void assertThatQProfileChangeRulesProfileUuidIsEqualTo(Long userId, String organizationUuid, String expectedUuid) {
    assertThat(db.select(String.format("select user_uuid from organization_members where user_id = %d and organization_uuid = '%s'", userId, organizationUuid))
      .stream()
      .map(row -> row.get("USER_UUID"))
      .findFirst())
        .hasValue(expectedUuid);
  }

  private void insertOrganizationMember(Long userId, String organizationUuid) {
    db.executeInsert("organization_members",
      "user_id", userId,
      "organization_uuid", organizationUuid);
  }

  private void insertUser(Long id, String uuid) {
    db.executeInsert("users",
      "id", id,
      "uuid", uuid,
      "login", "login" + id,
      "external_login", "ex-login" + id,
      "external_identity_provider", "ex-provider" + id,
      "external_id", id + 1,
      "is_root", false,
      "onboarded", false);
  }
}
