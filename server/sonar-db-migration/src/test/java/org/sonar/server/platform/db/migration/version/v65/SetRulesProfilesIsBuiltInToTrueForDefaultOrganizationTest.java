/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class SetRulesProfilesIsBuiltInToTrueForDefaultOrganizationTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(SetRulesProfilesIsBuiltInToTrueForDefaultOrganizationTest.class, "initial.sql");

  private SetRulesProfilesIsBuiltInToTrueForDefaultOrganization underTest = new SetRulesProfilesIsBuiltInToTrueForDefaultOrganization(db.database());

  @Test
  public void has_no_effect_if_table_is_empty() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("rules_profiles")).isEqualTo(0);
  }

  @Test
  public void mark_rules_profiles_of_default_org_as_built_in() throws SQLException {
    enableOrganization();
    String defaultOrganizationUuid = "ORG_UUID_1";
    setDefaultOrganization(defaultOrganizationUuid);
    IntStream.rangeClosed(1, 3).forEach(i -> {
      insertProfile(defaultOrganizationUuid, "RP_UUID_" + i, false);
      insertProfile("ORG_UUID_404", "RP_UUID_404_" + i, false);
    });

    underTest.execute();

    assertThat(selectRulesProfiles(true)).containsExactlyInAnyOrder("RP_UUID_1", "RP_UUID_2", "RP_UUID_3");
    assertThat(selectRulesProfiles(false)).containsExactlyInAnyOrder("RP_UUID_404_1", "RP_UUID_404_2", "RP_UUID_404_3");
  }

  @Test
  public void do_nothing_if_org_disabled() throws SQLException {
    String defaultOrganizationUuid = "ORG_UUID_1";
    setDefaultOrganization(defaultOrganizationUuid);
    IntStream.rangeClosed(1, 3).forEach(i -> {
      insertProfile(defaultOrganizationUuid, "RP_UUID_" + i, false);
      insertProfile("ORG_UUID_404", "RP_UUID_404_" + i, false);
    });

    underTest.execute();

    assertThat(selectRulesProfiles(false)).containsExactlyInAnyOrder("RP_UUID_1", "RP_UUID_2", "RP_UUID_3", "RP_UUID_404_1", "RP_UUID_404_2", "RP_UUID_404_3");
  }

  @Test
  public void reentrant_migration() throws SQLException {
    enableOrganization();
    String defaultOrganizationUuid = "ORG_UUID_1";
    setDefaultOrganization(defaultOrganizationUuid);
    IntStream.rangeClosed(1, 3).forEach(i -> {
      insertProfile(defaultOrganizationUuid, "RP_UUID_" + i, false);
      insertProfile("ORG_UUID_404", "RP_UUID_404_" + i, false);
    });

    underTest.execute();
    underTest.execute();

    assertThat(selectRulesProfiles(true)).containsExactlyInAnyOrder("RP_UUID_1", "RP_UUID_2", "RP_UUID_3");
    assertThat(selectRulesProfiles(false)).containsExactlyInAnyOrder("RP_UUID_404_1", "RP_UUID_404_2", "RP_UUID_404_3");
  }

  @Test
  public void reentrant_of_crashed_migration() throws SQLException {
    enableOrganization();
    String defaultOrganizationUuid = "ORG_UUID_1";
    setDefaultOrganization(defaultOrganizationUuid);
    insertProfile(defaultOrganizationUuid, "RP_UUID_1", true);
    insertProfile(defaultOrganizationUuid, "RP_UUID_2", false);
    insertProfile(defaultOrganizationUuid, "RP_UUID_3", false);

    underTest.execute();

    assertThat(selectRulesProfiles(true)).containsExactlyInAnyOrder("RP_UUID_1", "RP_UUID_2", "RP_UUID_3");
  }

  @Test
  public void fail_if_no_default_org_and_org_activated() throws SQLException {
    enableOrganization();
    insertProfile("ORG_UUID_1", "RP_UUID_1", false);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Missing internal property: 'organization.default'");

    underTest.execute();
  }

  private Set<String> selectRulesProfiles(boolean isBuiltIn) {
    return db.select("select rp.kee as \"uuid\", rp.is_built_in as \"isBuiltIn\" from rules_profiles rp")
      .stream()
      .filter(row -> (boolean) row.get("isBuiltIn") == isBuiltIn)
      .map(row -> (String) row.get("uuid"))
      .collect(Collectors.toSet());
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

  private void insertProfile(String orgUuid, String rulesProfileUuid, boolean isBuiltIn) {
    db.executeInsert("ORG_QPROFILES",
      "ORGANIZATION_UUID", orgUuid,
      "UUID", "OQP_UUID_" + rulesProfileUuid,
      "RULES_PROFILE_UUID", rulesProfileUuid,
      "CREATED_AT", 1_000L,
      "UPDATED_AT", 2_000L);
    db.executeInsert("RULES_PROFILES",
      "NAME", "name_" + rulesProfileUuid,
      "KEE", rulesProfileUuid,
      "LANGUAGE", "LANG_" + rulesProfileUuid,
      "IS_BUILT_IN", isBuiltIn);
  }
}
