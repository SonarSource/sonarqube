/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.rulesprofiles.fk.orgqprofiles;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.Uuids;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateOrgQProfilesRulesProfileUuidTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateOrgQProfilesRulesProfileUuidTest.class, "schema.sql");

  private DataChange underTest = new PopulateOrgQProfilesRulesProfileUuid(db.database());

  @Test
  public void populate_uuids() throws SQLException {
    long ruleProfileId_1 = 1L;
    String ruleProfileUuid_1 = "uuid-1";
    String ruleProfileKee_1 = "kee-1";
    insertRuleProfile(ruleProfileId_1, ruleProfileUuid_1, ruleProfileKee_1);

    long ruleProfileId_2 = 2L;
    String ruleProfileUuid_2 = "uuid-2";
    String ruleProfileKee_2 = "kee-2";
    insertRuleProfile(ruleProfileId_2, ruleProfileUuid_2, ruleProfileKee_2);

    long ruleProfileId_3 = 3L;
    String ruleProfileUuid_3 = "uuid-3";
    String ruleProfileKee_3 = "kee-3";
    insertRuleProfile(ruleProfileId_3, ruleProfileUuid_3, ruleProfileKee_3);

    String orgQProfileUuid_1 = "orgQProfileUuid_1";
    insertOrgQProfiles(orgQProfileUuid_1, ruleProfileKee_1);
    String orgQProfileUuid_2 = "orgQProfileUuid_2";
    insertOrgQProfiles(orgQProfileUuid_2, ruleProfileKee_2);
    String orgQProfileUuid_3 = "orgQProfileUuid_3";
    insertOrgQProfiles(orgQProfileUuid_3, ruleProfileKee_3);

    underTest.execute();

    assertThatOrgQprofileRulesProfileUuidIsEqualTo(orgQProfileUuid_1, ruleProfileUuid_1);
    assertThatOrgQprofileRulesProfileUuidIsEqualTo(orgQProfileUuid_2, ruleProfileUuid_2);
    assertThatOrgQprofileRulesProfileUuidIsEqualTo(orgQProfileUuid_3, ruleProfileUuid_3);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    long ruleProfileId_1 = 1L;
    String ruleProfileUuid_1 = "uuid-1";
    String ruleProfileKee_1 = "kee-1";
    insertRuleProfile(ruleProfileId_1, ruleProfileUuid_1, ruleProfileKee_1);

    long ruleProfileId_2 = 2L;
    String ruleProfileUuid_2 = "uuid-2";
    String ruleProfileKee_2 = "kee-2";
    insertRuleProfile(ruleProfileId_2, ruleProfileUuid_2, ruleProfileKee_2);

    long ruleProfileId_3 = 3L;
    String ruleProfileUuid_3 = "uuid-3";
    String ruleProfileKee_3 = "kee-3";
    insertRuleProfile(ruleProfileId_3, ruleProfileUuid_3, ruleProfileKee_3);

    String orgQProfileUuid_1 = "orgQProfileUuid_1";
    insertOrgQProfiles(orgQProfileUuid_1, ruleProfileKee_1);
    String orgQProfileUuid_2 = "orgQProfileUuid_2";
    insertOrgQProfiles(orgQProfileUuid_2, ruleProfileKee_2);
    String orgQProfileUuid_3 = "orgQProfileUuid_3";
    insertOrgQProfiles(orgQProfileUuid_3, ruleProfileKee_3);

    underTest.execute();

    long ruleProfileId_4 = 4L;
    String ruleProfileUuid_4 = "uuid-4";
    String ruleProfileKee_4 = "kee-4";
    insertRuleProfile(ruleProfileId_4, ruleProfileUuid_4, ruleProfileKee_4);

    String orgQProfileUuid_4 = "orgQProfileUuid_4";
    insertOrgQProfiles(orgQProfileUuid_4, ruleProfileKee_4);

    // re-entrant
    underTest.execute();

    assertThatOrgQprofileRulesProfileUuidIsEqualTo(orgQProfileUuid_1, ruleProfileUuid_1);
    assertThatOrgQprofileRulesProfileUuidIsEqualTo(orgQProfileUuid_2, ruleProfileUuid_2);
    assertThatOrgQprofileRulesProfileUuidIsEqualTo(orgQProfileUuid_3, ruleProfileUuid_3);
    assertThatOrgQprofileRulesProfileUuidIsEqualTo(orgQProfileUuid_4, ruleProfileUuid_4);
  }

  private void assertThatOrgQprofileRulesProfileUuidIsEqualTo(String orgQprofileUuid, String expectedUuid) {
    assertThat(db.select("select rules_profile_uuid from org_qprofiles where uuid = '" + orgQprofileUuid + "'")
      .stream()
      .map(row -> row.get("RULES_PROFILE_UUID"))
      .findFirst())
        .hasValue(expectedUuid);
  }

  private void insertOrgQProfiles(String uuid, String rulesProfileUuid) {
    db.executeInsert("org_qprofiles",
      "uuid", uuid,
      "organization_uuid", Uuids.createFast(),
      "rules_profile_uuid", rulesProfileUuid,
      "created_at", System.currentTimeMillis(),
      "updated_at", System.currentTimeMillis());
  }

  private void insertRuleProfile(Long id, String uuid, String kee) {
    db.executeInsert("rules_profiles",
      "id", id,
      "uuid", uuid,
      "name", "name" + id,
      "kee", kee,
      "is_built_in", false);
  }
}
