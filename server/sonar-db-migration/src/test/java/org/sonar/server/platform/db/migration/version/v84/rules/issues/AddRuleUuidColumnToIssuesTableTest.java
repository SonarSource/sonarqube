/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.rules.issues;

import java.sql.SQLException;
import java.sql.Types;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;

public class AddRuleUuidColumnToIssuesTableTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddRuleUuidColumnToIssuesTableTest.class, "schema.sql");

  private DdlChange underTest = new AddRuleUuidColumnToIssuesTable(db.database());

  @Before
  public void setup() {
    insertRule(1L, "uuid-rule-1");
    insertRule(2L, "uuid-rule-2");
    insertRule(3L, "uuid-rule-3");
    insertRule(4L, "uuid-rule-4");

    insertIssue("kee-iss-1", 1L);
    insertIssue("kee-iss-2", 1L);
    insertIssue("kee-iss-3", 2L);
    insertIssue("kee-iss-4", null);
    insertIssue("kee-iss-5", null);
  }

  @Test
  public void add_rule_uuid_column() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition("issues", "rule_uuid", Types.VARCHAR, 40, true);
    assertThat(db.countSql("select count(*) from issues"))
      .isEqualTo(5);
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

  private void insertIssue(String kee, @Nullable Long ruleId) {
    db.executeInsert("issues",
      "kee", kee,
      "rule_id", ruleId,
      "manual_severity", false);
  }

}
