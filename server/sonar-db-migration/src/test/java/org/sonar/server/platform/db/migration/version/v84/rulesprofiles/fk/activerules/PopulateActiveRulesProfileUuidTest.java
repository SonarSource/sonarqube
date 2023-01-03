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
package org.sonar.server.platform.db.migration.version.v84.rulesprofiles.fk.activerules;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;

public class PopulateActiveRulesProfileUuidTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateActiveRulesProfileUuidTest.class, "schema.sql");

  private DataChange underTest = new PopulateActiveRulesProfileUuid(db.database());

  @Test
  public void populate_uuids() throws SQLException {
    long ruleProfileId_1 = 1L;
    String ruleProfileUuid_1 = "uuid-1";
    insertRuleProfile(ruleProfileId_1, ruleProfileUuid_1);

    long ruleProfileId_2 = 2L;
    String ruleProfileUuid_2 = "uuid-2";
    insertRuleProfile(ruleProfileId_2, ruleProfileUuid_2);

    long ruleProfileId_3 = 3L;
    String ruleProfileUuid_3 = "uuid-3";
    insertRuleProfile(ruleProfileId_3, ruleProfileUuid_3);

    insertActiveRule("4", ruleProfileId_1);
    insertActiveRule("5", ruleProfileId_2);
    insertActiveRule("6", ruleProfileId_3);

    underTest.execute();

    assertThatActiveRulesProfileUuidIsEqualTo("4", ruleProfileUuid_1);
    assertThatActiveRulesProfileUuidIsEqualTo("5", ruleProfileUuid_2);
    assertThatActiveRulesProfileUuidIsEqualTo("6", ruleProfileUuid_3);
  }

  @Test
  public void delete_orphan_rows() throws SQLException {
    long ruleProfileId_1 = 1L;
    String ruleProfileUuid_1 = "uuid-1";
    insertRuleProfile(ruleProfileId_1, ruleProfileUuid_1);

    long ruleProfileId_2 = 2L;
    String ruleProfileUuid_2 = "uuid-2";
    insertRuleProfile(ruleProfileId_2, ruleProfileUuid_2);

    long ruleProfileId_3 = 3L;
    String ruleProfileUuid_3 = "uuid-3";
    insertRuleProfile(ruleProfileId_3, ruleProfileUuid_3);

    insertActiveRule("4", ruleProfileId_1);
    insertActiveRule("5", ruleProfileId_2);
    insertActiveRule("6", 10L);

    assertThat(db.countRowsOfTable("active_rules")).isEqualTo(3);

    underTest.execute();

    assertThatActiveRulesProfileUuidIsEqualTo("4", ruleProfileUuid_1);
    assertThatActiveRulesProfileUuidIsEqualTo("5", ruleProfileUuid_2);
    assertThat(db.countRowsOfTable("active_rules")).isEqualTo(2);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    long ruleProfileId_1 = 1L;
    String ruleProfileUuid_1 = "uuid-1";
    insertRuleProfile(ruleProfileId_1, ruleProfileUuid_1);

    long ruleProfileId_2 = 2L;
    String ruleProfileUuid_2 = "uuid-2";
    insertRuleProfile(ruleProfileId_2, ruleProfileUuid_2);

    long ruleProfileId_3 = 3L;
    String ruleProfileUuid_3 = "uuid-3";
    insertRuleProfile(ruleProfileId_3, ruleProfileUuid_3);

    insertActiveRule("4", ruleProfileId_1);
    insertActiveRule("5", ruleProfileId_2);
    insertActiveRule("6", ruleProfileId_3);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThatActiveRulesProfileUuidIsEqualTo("4", ruleProfileUuid_1);
    assertThatActiveRulesProfileUuidIsEqualTo("5", ruleProfileUuid_2);
    assertThatActiveRulesProfileUuidIsEqualTo("6", ruleProfileUuid_3);
  }

  private void assertThatActiveRulesProfileUuidIsEqualTo(String uuid, String expectedUuid) {
    assertThat(db.select("select profile_uuid from active_rules where uuid = '" + uuid + "'")
      .stream()
      .map(row -> row.get("PROFILE_UUID"))
      .findFirst())
        .hasValue(expectedUuid);
  }

  private void insertActiveRule(String uuid, Long profileId) {
    db.executeInsert("active_rules",
      "uuid", uuid,
      "profile_id", profileId,
      "rule_id", 1,
      "failure_level", 2);
  }

  private void insertRuleProfile(Long id, String uuid) {
    db.executeInsert("rules_profiles",
      "id", id,
      "uuid", uuid,
      "name", "name" + id,
      "kee", "kee" + id,
      "is_built_in", false);
  }
}
