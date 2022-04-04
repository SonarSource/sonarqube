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
package org.sonar.server.platform.db.migration.version.v84.activerules;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateActiveRuleParametersActiveRuleUuidTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateActiveRuleParametersActiveRuleUuidTest.class, "schema.sql");

  private DataChange underTest = new PopulateActiveRuleParametersActiveRuleUuid(db.database());

  @Test
  public void populate_uuids() throws SQLException {
    insertActiveRule(1L);
    insertActiveRule(2L);
    insertActiveRule(3L);

    insertActiveRuleParameter(4L, 1L);
    insertActiveRuleParameter(5L, 2L);
    insertActiveRuleParameter(6L, 3L);

    underTest.execute();

    assertThatTableContains(
      tuple("uuid4", 1L, "uuid1", "value4"),
      tuple("uuid5", 2L, "uuid2", "value5"),
      tuple("uuid6", 3L, "uuid3", "value6")
    );
  }

  @Test
  public void delete_orphan_rows() throws SQLException {
    insertActiveRule(1L);
    insertActiveRule(2L);
    insertActiveRule(3L);

    insertActiveRuleParameter(4L, 10L);
    insertActiveRuleParameter(5L, 2L);
    insertActiveRuleParameter(6L, 3L);

    underTest.execute();

    assertThatTableContains(
      tuple("uuid5", 2L, "uuid2", "value5"),
      tuple("uuid6", 3L, "uuid3", "value6")
    );
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertActiveRule(1L);
    insertActiveRule(2L);
    insertActiveRule(3L);

    insertActiveRuleParameter(4L, 1L);
    insertActiveRuleParameter(5L, 2L);
    insertActiveRuleParameter(6L, 3L);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThatTableContains(
      tuple("uuid4", 1L, "uuid1", "value4"),
      tuple("uuid5", 2L, "uuid2", "value5"),
      tuple("uuid6", 3L, "uuid3", "value6")
    );
  }

  private void assertThatTableContains(Tuple... tuples) {
    List<Map<String, Object>> select = db.select("select uuid, active_rule_id, active_rule_uuid, value from active_rule_parameters");
    assertThat(select).extracting(m -> m.get("UUID"), m -> m.get("ACTIVE_RULE_ID"), m -> m.get("ACTIVE_RULE_UUID"), m -> m.get("VALUE"))
      .containsExactlyInAnyOrder(tuples);
  }

  private void insertActiveRule(Long id) {
    db.executeInsert("active_rules",
      "uuid", "uuid" + id,
      "id", id,
      "profile_id", id + 1,
      "rule_id", id + 2,
      "failure_level", id + 3);
  }

  private void insertActiveRuleParameter(Long id, Long activeRuleId) {
    db.executeInsert("active_rule_parameters",
      "uuid", "uuid" + id,
      "rules_parameter_id", id,
      "value", "value" + id,
      "active_rule_id", activeRuleId);
  }
}
