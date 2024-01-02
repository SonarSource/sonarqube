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
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class RemoveCleanCodeAttributeFromCustomHotspotRulesIT {

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(RemoveCleanCodeAttributeFromCustomHotspotRules.class);
  private final RemoveCleanCodeAttributeFromCustomHotspotRules underTest = new RemoveCleanCodeAttributeFromCustomHotspotRules(db.database());

  @Test
  public void execute_whenRulesTableIsEmpty_shouldDoNothing() throws SQLException {
    underTest.execute();

    assertThat(db.select("select clean_code_attribute from rules")).isEmpty();
  }

  @Test
  public void execute_whenCustomHotspotRuleExist_shouldRemoveCleanCodeAttributeOnlyFromHotspot() throws SQLException {
    insertRule("custom_hotspot_rule", 4, "CONVENTIONAL");
    insertRule("other_rule", 1, "ETHICAL");

    underTest.execute();

    List<Map<String, Object>> selectResult = db.select("select name, clean_code_attribute, updated_at from rules");

    assertThat(selectResult)
      .extracting(stringObjectMap -> stringObjectMap.get("name"), stringObjectMap -> stringObjectMap.get("clean_code_attribute"))
      .containsExactlyInAnyOrder(tuple("custom_hotspot_rule", null), tuple("other_rule", "ETHICAL"));

    Optional<Object> updatedAtForHotspotRule = selectResult.stream().filter(map -> map.containsValue("custom_hotspot_rule"))
      .map(map -> map.get("updated_at")).findFirst();
    assertThat(updatedAtForHotspotRule.get()).isNotEqualTo(0L);

    Optional<Object> updatedAtForOtherRule = selectResult.stream().filter(map -> map.containsValue("other_rule"))
      .map(map -> map.get("updated_at")).findFirst();
    assertThat(updatedAtForOtherRule).contains(0L);
  }

  @Test
  public void execute_whenCustomHotspotRuleExist_isReentrant() throws SQLException {
    insertRule("custom_hotspot_rule", 4, "CONVENTIONAL");
    insertRule("other_rule", 1, "ETHICAL");

    underTest.execute();
    underTest.execute();

    List<Map<String, Object>> selectResult = db.select("select name, clean_code_attribute from rules");

    assertThat(selectResult)
      .extracting(stringObjectMap -> stringObjectMap.get("name"), stringObjectMap -> stringObjectMap.get("clean_code_attribute"))
      .containsExactlyInAnyOrder(tuple("custom_hotspot_rule", null), tuple("other_rule", "ETHICAL"));
  }

  private void insertRule(String name, int ruleType, String cleanCodeAttribute) {
    db.executeInsert("rules",
      "PLUGIN_RULE_KEY", name,
      "PLUGIN_NAME", name,
      "SCOPE", name,
      "NAME", name,
      "IS_EXTERNAL", true,
      "IS_AD_HOC", false,
      "UUID", name,
      "RULE_TYPE", ruleType,
      "UPDATED_AT", 0L,
      "CLEAN_CODE_ATTRIBUTE", cleanCodeAttribute);
  }
}
