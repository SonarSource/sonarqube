/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v100;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.util.SequenceUuidFactory.UUID_1;

class RemoveOrphanRulesFromQualityProfilesIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(RemoveOrphanRulesFromQualityProfiles.class);
  private final System2 system2 = mock(System2.class);
  private final UuidFactory instance = new SequenceUuidFactory();
  private final DataChange underTest = new RemoveOrphanRulesFromQualityProfiles(db.database(), instance, system2);

  @BeforeEach
  public void before() {
    when(system2.now()).thenReturn(1L);
  }

  @Test
  void migration_should_remove_orphans() throws SQLException {
    insertData();

    underTest.execute();

    assertOrphanRuleRemoved();
    assertQualityProfileChanges();
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    insertData();

    // re-entrant
    underTest.execute();
    underTest.execute();

    assertOrphanRuleRemoved();
    assertQualityProfileChanges();
  }

  private void insertData() {
    insertRule("uuid-rule-1", "language-1", "rule1");
    insertRule("uuid-rule-2", "language-2", "rule2");
    insertProfile("uuid-profile-1", "language-1");
    insertProfile("uuid-profile-2", "language-2");
    activateRule("uuid-active-rule-1", "uuid-profile-1", "uuid-rule-1");
    activateRule("uuid-active-rule-2", "uuid-profile-1", "uuid-rule-2");
    activateRule("uuid-active-rule-3", "uuid-profile-2", "uuid-rule-2");
  }

  private void insertRule(String uuid, String language, String ruleKey) {
    Map<String, Object> rule = new HashMap<>();
    rule.put("uuid", uuid);
    rule.put("plugin_rule_key", language + ":" + ruleKey);
    rule.put("plugin_name", "plugin-name-1");
    rule.put("scope", "MAIN");
    rule.put("language", language);
    rule.put("is_template", false);
    rule.put("is_ad_hoc", false);
    rule.put("is_external", false);
    db.executeInsert("rules", rule);
  }

  private void insertProfile(String uuid, String language) {
    Map<String, Object> profile = new HashMap<>();
    profile.put("uuid", uuid);
    profile.put("name", "profile-name-1");
    profile.put("language", language);
    profile.put("is_built_in", false);
    db.executeInsert("rules_profiles", profile);
  }

  private void activateRule(String activeRuleUuid, String profileUuid, String ruleUuid) {
    Map<String, Object> active_rule = new HashMap<>();
    active_rule.put("uuid", activeRuleUuid);
    active_rule.put("failure_level", 3);
    active_rule.put("profile_uuid", profileUuid);
    active_rule.put("rule_uuid", ruleUuid);
    db.executeInsert("active_rules", active_rule);
  }

  private void assertOrphanRuleRemoved() {
    assertThat(db.select("SELECT * from active_rules"))
      .extracting(r -> r.get("UUID"))
      .containsExactly("uuid-active-rule-1", "uuid-active-rule-3");
  }

  private void assertQualityProfileChanges() {
    assertThat(db.select("SELECT * from qprofile_changes"))
      .extracting(r -> r.get("KEE"), r -> r.get("RULES_PROFILE_UUID"), r -> r.get("CHANGE_TYPE"), r -> r.get("USER_UUID"), r -> r.get("CHANGE_DATA"), r -> r.get("CREATED_AT"))
      .containsExactly(tuple(UUID_1, "uuid-profile-1", "DEACTIVATED", null, "ruleUuid=uuid-rule-2", 1L));
  }


}
