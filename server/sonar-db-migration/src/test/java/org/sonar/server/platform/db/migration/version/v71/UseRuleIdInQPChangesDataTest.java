/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v71;

import com.google.common.base.Preconditions;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.db.CoreDbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class UseRuleIdInQPChangesDataTest {

  public static final String RULE_ID_DATA_FIELD = "ruleId";
  public static final String RULE_KEY_DATA_FIELD = "ruleKey";
  @Rule
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(UseRuleIdInQPChangesDataTest.class, "rules_and_qprofile_changes.sql");

  private UseRuleIdInQPChangesData underTest = new UseRuleIdInQPChangesData(dbTester.database());

  @Test
  public void no_effect_if_tables_are_empty() throws SQLException {
    underTest.execute();
  }

  @Test
  public void qpChange_without_ruleKey_is_unchanged() throws SQLException {
    String c1Data = insertQPChange("c1");
    String c2Data = insertQPChange("c2", "random", "data", "for", "value");

    underTest.execute();

    assertThat(reaQPChangeData("c1")).isEqualTo(c1Data);
    assertThat(reaQPChangeData("c2")).isEqualTo(c2Data);
  }

  @Test
  public void qpChange_with_ruleKey_of_other_case_is_unchanged() throws SQLException {
    int ruleId1 = insertRule("foo", "bar");
    String c1Data = insertQPChange("c1", "RULEKEY", "notARuleKey");
    String c2Data = insertQPChange("c2", "RULeKey", "foo:bar");

    underTest.execute();

    assertThat(reaQPChangeData("c1")).isEqualTo(c1Data);
    assertThat(reaQPChangeData("c2")).isEqualTo(c2Data);
  }

  @Test
  @UseDataProvider("nonExistingRuleKey")
  public void qpChange_with_ruleKey_of_non_existing_rule_has_ruleKey_removed(String ruleKey) throws SQLException {
    insertRule("foo", "bar");
    insertQPChange("c1", RULE_KEY_DATA_FIELD, ruleKey);
    insertQPChange("c2", "otherDataKey", "otherDataData", RULE_KEY_DATA_FIELD, ruleKey);

    underTest.execute();

    assertThat(reaQPChangeData("c1")).isEmpty();
    assertThat(reaQPChangeData("c2")).contains("otherDataKey=otherDataData");
  }

  @DataProvider
  public static Object[][] nonExistingRuleKey() {
    return new Object[][] {
      {"notARuleKey"},
      {"bar:foo"},
      {"foo:bar2"},
    };
  }

  @Test
  public void qpChange_with_ruleKey_of_existing_rule_is_replaced_with_ruleId() throws SQLException {
    int ruleId1 = insertRule("foo", "bar");
    int ruleId2 = insertRule("foo2", "bar");
    int ruleId3 = insertRule("foo", "bar2");
    insertQPChange("c1", RULE_KEY_DATA_FIELD, "foo:bar");
    insertQPChange("c2", "otherDataKey", "otherDataData", RULE_KEY_DATA_FIELD, "foo2:bar");
    insertQPChange("c3", RULE_KEY_DATA_FIELD, "foo:bar2", "otherDataKey2", "otherDataData2");

    underTest.execute();

    assertThat(reaQPChangeData("c1"))
      .doesNotContain(RULE_KEY_DATA_FIELD)
      .contains("ruleId=" + ruleId1);
    assertThat(reaQPChangeData("c2"))
      .contains("otherDataKey=otherDataData")
      .doesNotContain(RULE_KEY_DATA_FIELD)
      .contains("ruleId=" + ruleId2);
    assertThat(reaQPChangeData("c3"))
      .contains("otherDataKey2=otherDataData2")
      .doesNotContain(RULE_KEY_DATA_FIELD)
      .contains("ruleId=" + ruleId3);
  }

  @Test
  public void qpChange_with_ruleId_is_unchanged() throws SQLException {
    int ruleId1 = insertRule("foo", "bar");
    String c1Data = insertQPChange("c1", RULE_ID_DATA_FIELD, "notAnIntButIgnored");
    String c2Data = insertQPChange("c2", RULE_ID_DATA_FIELD, String.valueOf(ruleId1));

    underTest.execute();

    assertThat(reaQPChangeData("c1")).isEqualTo(c1Data);
    assertThat(reaQPChangeData("c2")).isEqualTo(c2Data);
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    int ruleId1 = insertRule("foo", "bar");
    insertQPChange("c1", RULE_KEY_DATA_FIELD, "foo:bar");
    String c2Data = insertQPChange("c2", "RULEKEY", "notARuleKey");
    insertQPChange("c3", RULE_KEY_DATA_FIELD, "nonExistingRuleKey");

    underTest.execute();

    assertThat(reaQPChangeData("c1"))
      .doesNotContain(RULE_KEY_DATA_FIELD)
      .contains("ruleId=" + ruleId1);
    assertThat(reaQPChangeData("c2")).isEqualTo(c2Data);
    assertThat(reaQPChangeData("c3")).isEmpty();

    underTest.execute();

    assertThat(reaQPChangeData("c1"))
      .doesNotContain(RULE_KEY_DATA_FIELD)
      .contains("ruleId=" + ruleId1);
    assertThat(reaQPChangeData("c2")).isEqualTo(c2Data);
    assertThat(reaQPChangeData("c3")).isEmpty();
  }

  private String reaQPChangeData(String qpChangeKey) {
    return (String) dbTester.selectFirst("select change_data as \"DATA\"from qprofile_changes where kee='" + qpChangeKey + "'")
      .get("DATA");
  }

  private int insertRule(String repo, String key) {
    dbTester.executeInsert(
      "RULES",
      "PLUGIN_RULE_KEY", key,
      "PLUGIN_NAME", repo);

    Map<String, Object> row = dbTester.selectFirst("select id as \"ID\" from rules where plugin_rule_key='" + key + "' and plugin_name='" + repo + "'");
    return ((Long) row.get("ID")).intValue();
  }

  private String insertQPChange(String kee, String... keysAndValues) {
    String data = keysAndValues.length == 0 ? null : KeyValueFormat.format(toMap(keysAndValues));
    dbTester.executeInsert(
      "QPROFILE_CHANGES",
      "KEE", kee,
      "RULES_PROFILE_UUID", randomAlphanumeric(5),
      "CHANGE_TYPE", randomAlphanumeric(6),
      "CREATED_AT", new Random().nextInt(10_000),
      "CHANGE_DATA", data);

    return data;
  }

  private static Map<String, String> toMap(String[] keysAndValues) {
    Preconditions.checkArgument(keysAndValues.length % 2 == 0);
    Map<String, String> res = new HashMap<>();
    for (int i = 0; i < keysAndValues.length; i++) {
      res.put(keysAndValues[i++], keysAndValues[i]);
    }
    return res;
  }
}
