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
package org.sonar.server.platform.db.migration.version.v84.rules.deprecatedrulekeys;

import java.sql.SQLException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateDeprecatedRuleKeysRuleUuidColumnTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateDeprecatedRuleKeysRuleUuidColumnTest.class, "schema.sql");

  private DataChange underTest = new PopulateDeprecatedRuleKeysRuleUuidColumn(db.database());

  @Before
  public void setup() {
    insertRule(1L, "uuid-rule-1");
    insertRule(2L, "uuid-rule-2");
    insertRule(3L, "uuid-rule-3");
    insertRule(4L, "uuid-rule-4");

    insertDeprecatedRuleKeyRow("uuid-drk-1", 1L);
    insertDeprecatedRuleKeyRow("uuid-drk-2", 1L);
    insertDeprecatedRuleKeyRow("uuid-drk-3", 2L);
    // orphan FK
    insertDeprecatedRuleKeyRow("uuid-drk-4", 10L);
  }

  @Test
  public void add_rule_uuid_column() throws SQLException {
    underTest.execute();

    assertThat(db.countSql("select count(*) from deprecated_rule_keys"))
      .isEqualTo(3);
    assertThat(db.select("select uuid, rule_id, rule_uuid from deprecated_rule_keys"))
      .extracting(m -> m.get("UUID"), m -> m.get("RULE_ID"), m -> m.get("RULE_UUID"))
      .containsExactlyInAnyOrder(
        tuple("uuid-drk-1", 1L, "uuid-rule-1"),
        tuple("uuid-drk-2", 1L, "uuid-rule-1"),
        tuple("uuid-drk-3", 2L, "uuid-rule-2"));
  }

  private void insertRule(long id, String uuid) {
    db.executeInsert("rules",
      "id", id,
      "uuid", uuid,
      "plugin_rule_key", "rk" + id,
      "plugin_name", "rn" + id,
      "scope", "MAIN",
      "is_ad_hoc", false,
      "is_external", false);
  }

  private void insertDeprecatedRuleKeyRow(String uuid, long ruleId) {
    db.executeInsert("deprecated_rule_keys",
      "uuid", uuid,
      "rule_id", ruleId,
      "old_repository_key", "old-repo-key" + uuid,
      "old_rule_key", "old-rule-key" + uuid,
      "created_at", System2.INSTANCE.now());
  }

}
