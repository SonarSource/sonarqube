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
package org.sonar.server.platform.db.migration.version.v84.rules;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateRulesTemplateUuidTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateRulesTemplateUuidTest.class, "schema.sql");

  private DataChange underTest = new PopulateRulesTemplateUuid(db.database());

  @Before
  public void setup() {
    insertRule(1L, "uuid-1", 1L);
    insertRule(2L, "uuid-2", 1L);
    insertRule(3L, "uuid-3", null);
    insertRule(4L, "uuid-4", 2L);
  }

  @Test
  public void add_rule_uuid_column() throws SQLException {
    underTest.execute();

    assertThat(db.countSql("select count(*) from rules"))
      .isEqualTo(4);
    assertThat(db.select("select uuid, template_id, template_uuid from rules"))
      .extracting(m -> m.get("UUID"), m -> m.get("TEMPLATE_ID"), m -> m.get("TEMPLATE_UUID"))
      .containsExactlyInAnyOrder(
        tuple("uuid-1", 1L, "uuid-1"),
        tuple("uuid-2", 1L, "uuid-1"),
        tuple("uuid-3", null, null),
        tuple("uuid-4", 2L, "uuid-2"));
  }

  private void insertRule(long id, String uuid, @Nullable Long templateId) {
    db.executeInsert("rules",
      "id", id,
      "uuid", uuid,
      "template_id", templateId,
      "plugin_rule_key", "rk" + id,
      "plugin_name", "rn" + id,
      "scope", "MAIN",
      "is_ad_hoc", false,
      "is_external", false);
  }
}
