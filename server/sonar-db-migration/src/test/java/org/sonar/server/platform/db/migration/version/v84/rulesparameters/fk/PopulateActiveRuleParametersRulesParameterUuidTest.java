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
package org.sonar.server.platform.db.migration.version.v84.rulesparameters.fk;

import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class PopulateActiveRuleParametersRulesParameterUuidTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateActiveRuleParametersRulesParameterUuidTest.class, "schema.sql");

  private UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private DataChange underTest = new PopulateActiveRuleParametersRulesParameterUuid(db.database());

  @Test
  public void populate_rules_parameter_uuids() throws SQLException {
    String ruleParamUuid1 = uuidFactory.create();
    long ruleParamId1 = 1L;
    insertRuleParameter(ruleParamId1, ruleParamUuid1, 101L);
    String activeRuleParameter11 = insertActiveRuleParameter(101L, ruleParamId1);
    String activeRuleParameter12 = insertActiveRuleParameter(101L, ruleParamId1);
    String activeRuleParameter13 = insertActiveRuleParameter(101L, ruleParamId1);

    String ruleParamUuid2 = uuidFactory.create();
    long ruleParamId2 = 2L;
    insertRuleParameter(ruleParamId2, ruleParamUuid2, 101L);
    String activeRuleParameter21 = insertActiveRuleParameter(101L, ruleParamId2);
    String activeRuleParameter22 = insertActiveRuleParameter(101L, ruleParamId2);

    insertRuleParameter(3L, uuidFactory.create(), 101L);

    underTest.execute();

    assertRulesParameterUuidsAreNotNull();
    assertThatRulesParametersUuidAreSet(ruleParamUuid1, activeRuleParameter11, activeRuleParameter12, activeRuleParameter13);
    assertThatRulesParametersUuidAreSet(ruleParamUuid2, activeRuleParameter21, activeRuleParameter22);
  }

  @Test
  public void delete_orphan_rows() throws SQLException {
    String ruleParamUuid1 = uuidFactory.create();
    long ruleParamId1 = 1L;
    insertRuleParameter(ruleParamId1, ruleParamUuid1, 101L);
    String activeRuleParameter11 = insertActiveRuleParameter(101L, ruleParamId1);
    String activeRuleParameter12 = insertActiveRuleParameter(101L, ruleParamId1);
    String activeRuleParameter13 = insertActiveRuleParameter(101L, 2L);

    underTest.execute();

    assertRulesParameterUuidsAreNotNull();
    assertThatRulesParametersUuidAreSet(ruleParamUuid1, activeRuleParameter11, activeRuleParameter12);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    String ruleParamUuid1 = uuidFactory.create();
    long ruleParamId1 = 1L;
    insertRuleParameter(ruleParamId1, ruleParamUuid1, 101L);
    String activeRuleParameter11 = insertActiveRuleParameter(101L, ruleParamId1);
    String activeRuleParameter12 = insertActiveRuleParameter(101L, ruleParamId1);
    String activeRuleParameter13 = insertActiveRuleParameter(101L, ruleParamId1);

    underTest.execute();

    String ruleParamUuid2 = uuidFactory.create();
    long ruleParamId2 = 2L;
    insertRuleParameter(ruleParamId2, ruleParamUuid2, 101L);
    String activeRuleParameter21 = insertActiveRuleParameter(101L, ruleParamId2);
    String activeRuleParameter22 = insertActiveRuleParameter(101L, ruleParamId2);
    // re-entrant
    underTest.execute();

    assertRulesParameterUuidsAreNotNull();
    assertThatRulesParametersUuidAreSet(ruleParamUuid1, activeRuleParameter11, activeRuleParameter12, activeRuleParameter13);
    assertThatRulesParametersUuidAreSet(ruleParamUuid2, activeRuleParameter21, activeRuleParameter22);
  }

  private void assertThatRulesParametersUuidAreSet(String ruleParameterUuid, String... activeRuleParameters) {
    assertThat(db.select("select rules_parameter_uuid from active_rule_parameters where uuid in ("
      + Stream.of(activeRuleParameters).map(s -> format("'%s'", s)).collect(Collectors.joining(",")) + ")")
      .stream()
      .map(row -> row.get("RULES_PARAMETER_UUID"))
      .filter(o -> !Objects.equals(o, ruleParameterUuid))
      .collect(Collectors.toList())).isEmpty();
  }

  private void assertRulesParameterUuidsAreNotNull() {
    assertThat(db.select("select rules_parameter_uuid from active_rule_parameters")
      .stream()
      .map(row -> row.get("RULES_PARAMETER_UUID"))
      .filter(Objects::isNull)
      .collect(Collectors.toList())).isEmpty();
  }

  private void insertRuleParameter(Long id, String uuid, Long ruleId) {
    db.executeInsert("rules_parameters",
      "id", id,
      "uuid", uuid,
      "rule_id", ruleId,
      "name", uuidFactory.create(),
      "param_type", uuidFactory.create());
  }

  private String insertActiveRuleParameter(Long ruleId, Long rulesParameterId) {
    String uuid = uuidFactory.create();
    db.executeInsert("active_rule_parameters",
      "uuid", uuid,
      "active_rule_id", ruleId,
      "rules_parameter_id", rulesParameterId);
    return uuid;
  }

}
