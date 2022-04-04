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
package org.sonar.server.platform.db.migration.version.v84.rulesprofiles.fk.activerules;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;

public class AddProfileUuidColumnToActiveRulesTableTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddProfileUuidColumnToActiveRulesTableTest.class, "schema.sql");

  private DdlChange underTest = new AddProfileUuidColumnToActiveRulesTable(db.database());

  @Before
  public void setup() {
    insertActiveRule("1", 1L, 2L);
    insertActiveRule("2", 3L, 4L);
    insertActiveRule("3", 5L, 6L);
  }

  @Test
  public void add_uuid_column_to_rules_profiles() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition("active_rules", "profile_uuid", Types.VARCHAR, 40, true);

    assertThat(db.countRowsOfTable("active_rules"))
      .isEqualTo(3);
  }

  private void insertActiveRule(String uuid, long profileId, long rule_id) {
    db.executeInsert("active_rules",
      "uuid", uuid,
      "profile_id", profileId,
      "rule_id", rule_id,
      "failure_level", 3);
  }
}
