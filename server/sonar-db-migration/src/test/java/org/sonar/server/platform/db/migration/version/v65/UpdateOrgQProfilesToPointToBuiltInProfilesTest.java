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

import java.sql.SQLException;
import java.util.List;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class UpdateOrgQProfilesToPointToBuiltInProfilesTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(UpdateOrgQProfilesToPointToBuiltInProfilesTest.class, "initial.sql");

  private UpdateOrgQProfilesToPointToBuiltInProfiles underTest = new UpdateOrgQProfilesToPointToBuiltInProfiles(db.database());

  @Test
  public void has_no_effect_if_table_is_empty() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("rules_profiles")).isEqualTo(0);
  }

  @Test
  public void has_no_effect_if_organization_are_disabled() throws SQLException {
    String defaultOrgUuid = "DEFAULT_ORG_UUID";
    setDefaultOrganization(defaultOrgUuid);
    String sonarWayJava = "RP_UUID_1";
    String sonarWayJavascript = "RP_UUID_2";
    insertProfile(defaultOrgUuid, "OQP_UUID_1", sonarWayJava, "Sonar way", "Java", true, 1_000_000_000L);
    insertProfile(defaultOrgUuid, "OQP_UUID_2", sonarWayJavascript, "Sonar way", "Javascript", true, null);
    insertProfile(defaultOrgUuid, "OQP_UUID_3", "RP_UUID_3", "Sonar way", "Cobol", true, null);
    insertProfile(defaultOrgUuid, "OQP_UUID_4", "RP_UUID_4", "My Sonar way", "Java", false, null);

    underTest.execute();

    assertThat(selectRulesProfiles()).containsExactlyInAnyOrder(
      tuple("OQP_UUID_1", sonarWayJava),
      tuple("OQP_UUID_2", sonarWayJavascript),
      tuple("OQP_UUID_3", "RP_UUID_3"),
      tuple("OQP_UUID_4", "RP_UUID_4"));
  }

  @Test
  public void update_org_qprofiles_to_point_to_built_in_rules_profiles() throws SQLException {
    enableOrganization();
    String defaultOrgUuid = "DEFAULT_ORG_UUID";
    setDefaultOrganization(defaultOrgUuid);
    String sonarWayJava = "RP_UUID_1";
    String sonarWayJavascript = "RP_UUID_2";
    insertProfile(defaultOrgUuid, "OQP_UUID_1", sonarWayJava, "Sonar way", "Java", true, 1_000_000_000L);
    insertProfile(defaultOrgUuid, "OQP_UUID_2", sonarWayJavascript, "Sonar way", "Javascript", true, null);
    insertProfile(defaultOrgUuid, "OQP_UUID_3", "RP_UUID_3", "Sonar way", "Cobol", true, null);
    insertProfile("ORG_UUID_1", "OQP_UUID_4", "RP_UUID_4", "Sonar way", "Java", false, null);
    insertProfile("ORG_UUID_1", "OQP_UUID_5", "RP_UUID_5", "My Sonar way", "Java", false, null);
    insertProfile("ORG_UUID_2", "OQP_UUID_6", "RP_UUID_6", "Sonar way", "Javascript", false, null);
    insertProfile("ORG_UUID_2", "OQP_UUID_7", "RP_UUID_7", "Sonar way", "Python", false, null);
    insertProfile("ORG_UUID_2", "OQP_UUID_8", "RP_UUID_8", "Sonar way", "Java", false, 2_000_000_000L);

    underTest.execute();

    assertThat(selectRulesProfiles()).containsExactlyInAnyOrder(
      tuple("OQP_UUID_1", sonarWayJava),
      tuple("OQP_UUID_2", sonarWayJavascript),
      tuple("OQP_UUID_3", "RP_UUID_3"),
      tuple("OQP_UUID_4", sonarWayJava),
      tuple("OQP_UUID_5", "RP_UUID_5"),
      tuple("OQP_UUID_6", sonarWayJavascript),
      tuple("OQP_UUID_7", "RP_UUID_7"),
      tuple("OQP_UUID_8", "RP_UUID_8"));
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    enableOrganization();
    String defaultOrgUuid = "DEFAULT_ORG_UUID";
    setDefaultOrganization(defaultOrgUuid);
    String sonarWayJava = "RP_UUID_1";
    String sonarWayJavascript = "RP_UUID_2";
    insertProfile(defaultOrgUuid, "OQP_UUID_1", sonarWayJava, "Sonar way", "Java", true, 1_000_000_000L);
    insertProfile(defaultOrgUuid, "OQP_UUID_2", sonarWayJavascript, "Sonar way", "Javascript", true, null);
    insertProfile("ORG_UUID_1", "OQP_UUID_4", "RP_UUID_4", "Sonar way", "Java", false, null);
    insertProfile("ORG_UUID_1", "OQP_UUID_5", "RP_UUID_5", "My Sonar way", "Java", false, null);

    underTest.execute();
    underTest.execute();

    assertThat(selectRulesProfiles()).containsExactlyInAnyOrder(
      tuple("OQP_UUID_1", sonarWayJava),
      tuple("OQP_UUID_2", sonarWayJavascript),
      tuple("OQP_UUID_4", sonarWayJava),
      tuple("OQP_UUID_5", "RP_UUID_5"));
  }

  @Test
  public void crashed_migration_is_reentrant() throws SQLException {
    enableOrganization();
    String defaultOrgUuid = "DEFAULT_ORG_UUID";
    setDefaultOrganization(defaultOrgUuid);
    String sonarWayJava = "RP_UUID_1";
    String sonarWayJavascript = "RP_UUID_2";
    insertProfile(defaultOrgUuid, "OQP_UUID_1", sonarWayJava, "Sonar way", "Java", true, 1_000_000_000L);
    insertProfile(defaultOrgUuid, "OQP_UUID_2", sonarWayJavascript, "Sonar way", "Javascript", true, null);
    insertOrgQProfile("ORG_UUID_1", "OQP_UUID_3", sonarWayJava);
    insertProfile("ORG_UUID_1", "OQP_UUID_4", "RP_UUID_4", "Sonar way", "Javascript", false, null);
    insertProfile("ORG_UUID_1", "OQP_UUID_5", "RP_UUID_5", "My Sonar way", "Java", false, null);

    underTest.execute();

    assertThat(selectRulesProfiles()).containsExactlyInAnyOrder(
      tuple("OQP_UUID_1", sonarWayJava),
      tuple("OQP_UUID_2", sonarWayJavascript),
      tuple("OQP_UUID_3", sonarWayJava),
      tuple("OQP_UUID_4", sonarWayJavascript),
      tuple("OQP_UUID_5", "RP_UUID_5"));
  }

  @Test
  public void fail_if_no_default_org_and_org_activated() throws SQLException {
    enableOrganization();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Missing internal property: 'organization.default'");

    underTest.execute();
  }

  private List<Tuple> selectRulesProfiles() {
    return db.select("select oqp.uuid as \"uuid\", oqp.rules_profile_uuid as \"rulesProfileUuid\" from org_qprofiles oqp")
      .stream()
      .map(row -> tuple(row.get("uuid"), row.get("rulesProfileUuid")))
      .collect(MoreCollectors.toList());
  }

  private void enableOrganization() {
    db.executeInsert("INTERNAL_PROPERTIES",
      "KEE", "organization.enabled",
      "TEXT_VALUE", "true",
      "IS_EMPTY", false);
  }

  private void setDefaultOrganization(String uuid) {
    db.executeInsert("INTERNAL_PROPERTIES",
      "KEE", "organization.default",
      "TEXT_VALUE", uuid,
      "IS_EMPTY", false);
  }

  private void insertProfile(String orgUuid, String orgQProfileUuid, String rulesProfileUuid, String name, String language, boolean isBuiltIn, @Nullable Long userUpdatedAt) {
    db.executeInsert("ORG_QPROFILES",
      "ORGANIZATION_UUID", orgUuid,
      "UUID", orgQProfileUuid,
      "RULES_PROFILE_UUID", rulesProfileUuid,
      "CREATED_AT", 1_000L,
      "UPDATED_AT", 2_000L,
      "USER_UPDATED_AT", userUpdatedAt);
    db.executeInsert("RULES_PROFILES",
      "NAME", name,
      "KEE", rulesProfileUuid,
      "LANGUAGE", language,
      "IS_BUILT_IN", isBuiltIn);
  }

  private void insertOrgQProfile(String orgUuid, String orgQProfileUuid, String rulesProfileUuid) {
    db.executeInsert("ORG_QPROFILES",
      "ORGANIZATION_UUID", orgUuid,
      "UUID", orgQProfileUuid,
      "RULES_PROFILE_UUID", rulesProfileUuid,
      "CREATED_AT", 1_000L,
      "UPDATED_AT", 2_000L);
  }
}
