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
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.CoreDbTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class DeleteOrphansFromRulesProfilesTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteOrphansFromRulesProfilesTest.class, "initial.sql");

  private DeleteOrphansFromRulesProfiles underTest = new DeleteOrphansFromRulesProfiles(db.database());


  @Test
  public void migration() throws SQLException {
    long rulesProfileId1 = insertRulesProfile("RP_UUID_1", "Sonar Way", "Java", 1_000L, true);
    long rulesProfileId2 = insertRulesProfile("RP_UUID_2", "Sonar Way", "JavaScript", 1_000L, false);
    insertRulesProfile("RP_UUID_3", "Sonar Way", "PL/SQL", 1_000L, false);
    insertRulesProfile("RP_UUID_4", "Sonar Way", "Cobol", 1_000L, false);
    insertRulesProfile("RP_UUID_5", "Sonar Way", "Cobol", 1_000L, false);
    insertRulesProfile("RP_UUID_6", "Sonar Way", "Cobol", 1_000L, true);

    insertOrgQProfile("ORG_QP_UUID_1", "ORG_UUID_1", "RP_UUID_1");
    insertOrgQProfile("ORG_QP_UUID_2", "ORG_UUID_1", "RP_UUID_2");

    long activeRuleId1 = insertActiveRule(rulesProfileId1, 1);
    long activeRuleId2 = insertActiveRule(rulesProfileId2, 1);
    insertActiveRule(-1, 1);
    insertActiveRule(-2, 1);

    long param1 = insertActiveRuleParameter(activeRuleId1, 1);
    long param2 = insertActiveRuleParameter(activeRuleId1, 2);
    long param3 = insertActiveRuleParameter(activeRuleId2, 2);
    insertActiveRuleParameter(-1, 1);
    insertActiveRuleParameter(-2, 1);

    insertQProfileChange("QPC_UUID1", "RP_UUID_1", "A");
    insertQProfileChange("QPC_UUID2", "RP_UUID_2", "B");
    insertQProfileChange("QPC_UUID3", "RP_UUID_3", "A");
    insertQProfileChange("QPC_UUID4", "RP_UUID_4", "A");

    underTest.execute();

    assertThat(selectRulesProfiles()).containsExactlyInAnyOrder("RP_UUID_1", "RP_UUID_2");
    assertThat(selectActiveRules()).containsExactlyInAnyOrder(activeRuleId1, activeRuleId2);
    assertThat(selectActiveRuleParameters()).containsExactlyInAnyOrder(param1, param2, param3);
    assertThat(selectQProfileChanges()).containsExactlyInAnyOrder("QPC_UUID1", "QPC_UUID2");
  }

  @Test
  public void delete_rules_profiles_without_reference_in_qprofiles() throws SQLException {
    insertRulesProfile("RP_UUID_1", "Sonar Way", "Java", 1_000L, true);
    insertRulesProfile("RP_UUID_2", "Sonar Way", "JavaScript", 1_000L, false);
    insertRulesProfile("RP_UUID_5", "Sonar Way", "Cobol", 1_000L, false);
    insertRulesProfile("RP_UUID_6", "Sonar Way", "Cobol", 1_000L, true);
    insertOrgQProfile("ORG_QP_UUID_1", "ORG_UUID_1", "RP_UUID_5");
    insertOrgQProfile("ORG_QP_UUID_2", "ORG_UUID_1", "RP_UUID_6");

    underTest.execute();

    assertThat(selectRulesProfiles()).containsExactlyInAnyOrder("RP_UUID_5", "RP_UUID_6");
  }

  @Test
  public void delete_active_rules_without_rules_profiles() throws SQLException {
    long rulesProfileId1 = insertRulesProfile("RP_UUID_1", "Sonar Way", "Java", 1_000L, true);
    long rulesProfileId2 = insertRulesProfile("RP_UUID_2", "Sonar Way", "JavaScript", 1_000L, false);

    insertOrgQProfile("ORG_QP_UUID_1", "ORG_UUID_1", "RP_UUID_1");
    insertOrgQProfile("ORG_QP_UUID_2", "ORG_UUID_1", "RP_UUID_2");

    long activeRule1 = insertActiveRule(rulesProfileId1, 1);
    long activeRule2 = insertActiveRule(rulesProfileId2, 1);
    insertActiveRule(-1, 1);
    insertActiveRule(-2, 1);

    underTest.execute();

    assertThat(selectActiveRules()).containsExactlyInAnyOrder(activeRule1, activeRule2);
  }

  @Test
  public void delete_active_rule_parameters_without_active_rules() throws SQLException {
    long rulesProfileId1 = insertRulesProfile("RP_UUID_1", "Sonar Way", "Java", 1_000L, true);
    long rulesProfileId2 = insertRulesProfile("RP_UUID_2", "Sonar Way", "JavaScript", 1_000L, false);

    insertOrgQProfile("ORG_QP_UUID_1", "ORG_UUID_1", "RP_UUID_1");
    insertOrgQProfile("ORG_QP_UUID_2", "ORG_UUID_1", "RP_UUID_2");

    long activeRuleId1 = insertActiveRule(rulesProfileId1, 1);
    long activeRuleId2 = insertActiveRule(rulesProfileId2, 1);

    long param1 = insertActiveRuleParameter(activeRuleId1, 1);
    long param2 = insertActiveRuleParameter(activeRuleId1, 2);
    long param3 = insertActiveRuleParameter(activeRuleId2, 2);

    insertActiveRuleParameter(-1, 1);
    insertActiveRuleParameter(-2, 1);

    underTest.execute();

    assertThat(selectActiveRuleParameters()).containsExactlyInAnyOrder(param1, param2, param3);
  }

  @Test
  public void delete_qprofile_changes_without_rules_profiles() throws SQLException {
    long rulesProfileId1 = insertRulesProfile("RP_UUID_1", "Sonar Way", "Java", 1_000L, true);
    long rulesProfileId2 = insertRulesProfile("RP_UUID_2", "Sonar Way", "JavaScript", 1_000L, false);

    insertOrgQProfile("ORG_QP_UUID_1", "ORG_UUID_1", "RP_UUID_1");
    insertOrgQProfile("ORG_QP_UUID_2", "ORG_UUID_1", "RP_UUID_2");

    insertQProfileChange("QPC_UUID1", "RP_UUID_1", "A");
    insertQProfileChange("QPC_UUID2", "RP_UUID_2", "B");
    insertQProfileChange("QPC_UUID3", "RP_UUID_3", "A");
    insertQProfileChange("QPC_UUID4", "RP_UUID_4", "A");

    underTest.execute();

    assertThat(selectQProfileChanges()).containsExactlyInAnyOrder("QPC_UUID1", "QPC_UUID2");
  }

  @Test
  public void reentrant_migration() throws SQLException {
    long rulesProfileId1 = insertRulesProfile("RP_UUID_1", "Sonar Way", "Java", 1_000L, true);
    long rulesProfileId2 = insertRulesProfile("RP_UUID_2", "Sonar Way", "JavaScript", 1_000L, false);
    insertRulesProfile("RP_UUID_3", "Sonar Way", "PL/SQL", 1_000L, false);
    insertRulesProfile("RP_UUID_4", "Sonar Way", "Cobol", 1_000L, false);
    insertRulesProfile("RP_UUID_5", "Sonar Way", "Cobol", 1_000L, false);
    insertRulesProfile("RP_UUID_6", "Sonar Way", "Cobol", 1_000L, true);

    insertOrgQProfile("ORG_QP_UUID_1", "ORG_UUID_1", "RP_UUID_1");
    insertOrgQProfile("ORG_QP_UUID_2", "ORG_UUID_1", "RP_UUID_2");

    long activeRuleId1 = insertActiveRule(rulesProfileId1, 1);
    long activeRuleId2 = insertActiveRule(rulesProfileId2, 1);
    insertActiveRule(-1, 1);
    insertActiveRule(-2, 1);

    insertActiveRuleParameter(activeRuleId1, 1);
    insertActiveRuleParameter(activeRuleId1, 2);
    insertActiveRuleParameter(activeRuleId2, 2);
    insertActiveRuleParameter(-1, 1);
    insertActiveRuleParameter(-2, 1);

    insertQProfileChange("QPC_UUID1", "RP_UUID_1", "A");
    insertQProfileChange("QPC_UUID2", "RP_UUID_2", "B");
    insertQProfileChange("QPC_UUID3", "RP_UUID_3", "A");
    insertQProfileChange("QPC_UUID4", "RP_UUID_4", "A");

    underTest.execute();
    underTest.execute();

    assertThat(countRows("rules_profiles")).isEqualTo(2);
    assertThat(countRows("active_rules")).isEqualTo(2);
    assertThat(countRows("active_rule_parameters")).isEqualTo(3);
    assertThat(countRows("qprofile_changes")).isEqualTo(2);
  }

  private int countRows(String table) {
    return db.countSql(format("select count(*) from %s", table));
  }

  private List<String> selectRulesProfiles() {
    return db.select("select kee as \"kee\" from rules_profiles")
      .stream()
      .map(row -> (String) row.get("kee"))
      .collect(MoreCollectors.toList());
  }

  private List<String> selectQProfileChanges() {
    return db.select("select kee as \"kee\" from qprofile_changes")
      .stream()
      .map(row -> (String) row.get("kee"))
      .collect(MoreCollectors.toList());
  }

  private List<Long> selectActiveRules() {
    return db.select("select id as \"id\" from active_rules")
      .stream()
      .map(row -> (Long) row.get("id"))
      .collect(MoreCollectors.toList());
  }

  private List<Long> selectActiveRuleParameters() {
    return db.select("select id as \"id\" from active_rule_parameters")
      .stream()
      .map(row -> (Long) row.get("id"))
      .collect(MoreCollectors.toList());
  }

  private long insertRulesProfile(String rulesProfileUuid, String name, String language, Long userUpdatedAt, boolean isBuiltIn) {
    db.executeInsert("RULES_PROFILES",
      "NAME", name,
      "LANGUAGE", language,
      "KEE", rulesProfileUuid,
      "USER_UPDATED_AT", userUpdatedAt,
      "IS_BUILT_IN", isBuiltIn,
      "LAST_USED", 1_000L,
      "CREATED_AT", new Date(1_000L),
      "UPDATED_AT", new Date(1_000L));

    return (Long) db.selectFirst(
      format("select id as \"id\" from rules_profiles where kee='%s'", rulesProfileUuid)).get("id");
  }

  private void insertOrgQProfile(String orgQProfileUuid, String orgUuid, String rulesProfileUuid) {
    db.executeInsert("ORG_QPROFILES",
      "UUID", orgQProfileUuid,
      "ORGANIZATION_UUID", orgUuid,
      "RULES_PROFILE_UUID", rulesProfileUuid,
      "CREATED_AT", 1_000L,
      "UPDATED_AT", 2_000L);
  }

  private long insertActiveRule(long profileId, long ruleId) {
    db.executeInsert("ACTIVE_RULES",
      "PROFILE_ID", profileId,
      "RULE_ID", ruleId,
      "FAILURE_LEVEL", 1,
      "INHERITANCE", "",
      "CREATED_AT", 1_000L,
      "UPDATED_AT", 2_000L);

    return (Long) db.selectFirst(
      format("select id as \"id\" from active_rules where profile_id = %d and rule_id = %d", profileId, ruleId)).get("id");
  }

  private long insertActiveRuleParameter(long activeRuleId, long rulesParameterId) {
    db.executeInsert("ACTIVE_RULE_PARAMETERS",
      "ACTIVE_RULE_ID", activeRuleId,
      "RULES_PARAMETER_ID", rulesParameterId,
      "RULES_PARAMETER_KEY", "",
      "VALUE", "");

    return (Long) db.selectFirst(
      format("select id as \"id\" from active_rule_parameters where active_rule_id=%d and rules_parameter_id=%d", activeRuleId, rulesParameterId)).get("id");
  }

  private void insertQProfileChange(String uuid, String rulesProfileUuid, String changeType) {
    db.executeInsert("QPROFILE_CHANGES",
      "KEE", uuid,
      "QPROFILE_KEY", rulesProfileUuid,
      "CHANGE_TYPE", changeType,
      "USER_LOGIN", "",
      "CHANGE_DATA", "",
      "CREATED_AT", 1_000L);
  }
}
