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
package org.sonar.server.platform.db.migration.version.v95;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextBoolean;

public class PopulateRulesMetadataInRuleTableTest {

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(PopulateRulesMetadataInRuleTableTest.class, "schema.sql");

  private final DataChange underTest = new PopulateRulesMetadataInRuleTable(db.database());

  @Test
  public void validate_metadata_are_migrated_to_rules() throws SQLException {
    // create rules
    String ruleUuid1 = insertRule();
    String ruleUuid2 = insertRule();
    String ruleUuid3 = insertRule();

    // create metadata
    Map<String, Object> rule1Metadata = insertRuleMetadata(ruleUuid1);
    Map<String, Object> rule2Metadata = insertRuleMetadata(ruleUuid2);
    insertRuleMetadata("non-existing-rule");

    // migrate
    underTest.execute();

    // verify result
    verifyRuleMetadata(db, ruleUuid1, rule1Metadata);
    verifyRuleMetadata(db, ruleUuid2, rule2Metadata);
    verifyNoRuleMetadata(ruleUuid3);
  }

  private void verifyNoRuleMetadata(String ruleUuid) {
    Map<String, Object> mapRule = db.selectFirst(String.format("select * from rules where uuid='%s'", ruleUuid));
    List<String> metadataColumns = List.of("RULE_UUID", "NOTE_DATA", "NOTE_USER_UUID", "NOTE_CREATED_AT", "NOTE_UPDATED_AT", "REMEDIATION_FUNCTION", "REMEDIATION_GAP_MULT", "REMEDIATION_BASE_EFFORT", "TAGS", "AD_HOC_NAME", "AD_HOC_DESCRIPTION", "AD_HOC_SEVERITY", "AD_HOC_TYPE");
    for (String key : metadataColumns) {
      Assertions.assertThat(mapRule.get(key)).isNull();
    }
  }

  private void verifyRuleMetadata(CoreDbTester db, String ruleUuid, Map<String, Object> ruleMetadata) {
    // check rule metadata are here
    Map<String, Object> mapRule = db.selectFirst(String.format("select * from rules where uuid='%s'", ruleUuid));
    Map<String, Object> mapToVerify = new HashMap<>(ruleMetadata);
    mapToVerify.remove("RULE_UUID");
    Assertions.assertThat(mapRule)
      .containsAllEntriesOf(mapToVerify);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    // create rules
    String ruleUuid1 = insertRule();
    String ruleUuid2 = insertRule();
    String ruleUuid3 = insertRule();

    // create metadata
    Map<String, Object> rule1Metadata = insertRuleMetadata(ruleUuid1);
    Map<String, Object> rule2Metadata = insertRuleMetadata(ruleUuid2);
    insertRuleMetadata("non-existing-rule");

    underTest.execute();
    underTest.execute();

    // verify result
    verifyRuleMetadata(db, ruleUuid1, rule1Metadata);
    verifyRuleMetadata(db, ruleUuid2, rule2Metadata);
    verifyNoRuleMetadata(ruleUuid3);
  }

  private String insertRule() {
    String ruleUuid = RandomStringUtils.randomAlphanumeric(40);
    Map<String, Object> map = new HashMap<>();
    map.put("uuid", ruleUuid);
    map.put("plugin_rule_key", randomAlphanumeric(20));
    map.put("plugin_name", randomAlphanumeric(20));
    map.put("scope", randomAlphanumeric(20));
    map.put("is_template", nextBoolean());
    map.put("is_ad_hoc", nextBoolean());
    map.put("is_external", nextBoolean());
    db.executeInsert("rules", map);
    return ruleUuid;
  }

  private Map<String, Object> insertRuleMetadata(String ruleUuid) {
    Map<String, Object> map = new HashMap<>();
    map.put("RULE_UUID", ruleUuid);
    map.put("NOTE_DATA", randomAlphanumeric(20));
    map.put("NOTE_USER_UUID", randomAlphanumeric(20));
    map.put("NOTE_CREATED_AT", System.currentTimeMillis());
    map.put("NOTE_UPDATED_AT", System.currentTimeMillis());
    map.put("REMEDIATION_FUNCTION", randomAlphanumeric(20));
    map.put("REMEDIATION_GAP_MULT", randomAlphanumeric(20));
    map.put("REMEDIATION_BASE_EFFORT", randomAlphanumeric(20));
    map.put("TAGS", randomAlphanumeric(20));
    map.put("AD_HOC_NAME", randomAlphanumeric(20));
    map.put("AD_HOC_DESCRIPTION", randomAlphanumeric(1000000));
    map.put("AD_HOC_SEVERITY", randomAlphanumeric(10));
    map.put("AD_HOC_TYPE", (long) RandomUtils.nextInt(10));
    db.executeInsert("rules_metadata", map);
    return map;

  }



}
