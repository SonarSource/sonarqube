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
package org.sonar.server.platform.db.migration.version.v104;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateRuleTagsTableIT {

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateRuleTagsTable.class);

  private final PopulateRuleTagsTable migration = new PopulateRuleTagsTable(db.database());

  @Test
  public void execute_whenTagsExist_shouldPopulateProperly() throws SQLException {
    insertRule("uuid-1", null, "tag_1,tag_2");
    insertRule("uuid-2", "systag_1,systag_2", null);
    insertRule("uuid-3", "systag_3,systag_4", "tag_3,tag_4");

    migration.execute();

    assertThat(db.select("select value, is_system_tag, rule_uuid from rule_tags"))
      .extracting(t -> t.get("value"), t -> t.get("is_system_tag"), t -> t.get("rule_uuid"))
      .containsExactlyInAnyOrder(
        tuple("systag_1", true, "uuid-2"),
        tuple("systag_2", true, "uuid-2"),
        tuple("tag_1", false, "uuid-1"),
        tuple("tag_2", false, "uuid-1"),
        tuple("systag_3", true, "uuid-3"),
        tuple("systag_4", true, "uuid-3"),
        tuple("tag_3", false, "uuid-3"),
        tuple("tag_4", false, "uuid-3")
      );
  }

  @Test
  public void execute_whenEmptyOrDuplicateTagsExist_shouldNotBeMigrated() throws SQLException {
    insertRule("uuid-1", null, "tag_1,,tag_2");
    insertRule("uuid-2", "systag_1,,systag_2,systag_2,", null);

    migration.execute();

    assertThat(db.select("select value, is_system_tag, rule_uuid from rule_tags"))
      .extracting(t -> t.get("value"), t -> t.get("is_system_tag"), t -> t.get("rule_uuid"))
      .containsExactlyInAnyOrder(
        tuple("systag_1", true, "uuid-2"),
        tuple("systag_2", true, "uuid-2"),
        tuple("tag_1", false, "uuid-1"),
        tuple("tag_2", false, "uuid-1")
      );
  }

  @Test
  public void execute_whenRunMoreThanOnce_shouldBeReentrant() throws SQLException {
    insertRule("uuid-3", "sys_tag", "tag");
    migration.execute();
    migration.execute();
    migration.execute();
    verifyMapping();
  }

  private void verifyMapping() {
    assertThat(db.select("select value, is_system_tag, rule_uuid from rule_tags"))
      .extracting(t -> t.get("value"), t -> t.get("is_system_tag"), t -> t.get("rule_uuid"))
      .containsExactly(
        tuple("sys_tag", true, "uuid-3"),
        tuple("tag", false, "uuid-3")
      );
  }

  private void insertRule(String uuid, @Nullable String systemTags, @Nullable String tags) {
    db.executeInsert("rules",
      "UUID", uuid,
      "PLUGIN_RULE_KEY", uuid,
      "PLUGIN_NAME", uuid,
      "SCOPE", "1",
      "IS_TEMPLATE", false,
      "IS_AD_HOC", false,
      "SYSTEM_TAGS", systemTags,
      "TAGS", tags,
      "IS_EXTERNAL", false);
  }

}
