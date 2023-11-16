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
package org.sonar.server.platform.db.migration.version.v102;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class PopulateCleanCodeAttributeColumnInRulesIT {

  private static final String TABLE_NAME = "rules";

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateCleanCodeAttributeColumnInRules.class);
  private final PopulateCleanCodeAttributeColumnInRules underTest = new PopulateCleanCodeAttributeColumnInRules(db.database());

  @Test
  public void execute_whenRulesDoNotExist_shouldNotFail() {
    assertThatCode(underTest::execute).doesNotThrowAnyException();
  }

  @Test
  public void execute_whenRuleWithUndefinedCleanCodeAttribute_shouldUpdate() throws SQLException {
    insertRule("1", null);
    underTest.execute();
    assertThat(db.select("select uuid, clean_code_attribute from rules"))
      .extracting(stringObjectMap -> stringObjectMap.get("clean_code_attribute"))
      .containsExactly(CleanCodeAttribute.CONVENTIONAL.name());
  }

  @Test
  public void execute_whenRuleWithUndefinedCleanCodeAttribute_shouldBeReentrant() throws SQLException {
    insertRule("1", null);
    underTest.execute();
    underTest.execute();
    assertThat(db.select("select uuid, clean_code_attribute from rules"))
      .extracting(stringObjectMap -> stringObjectMap.get("clean_code_attribute"))
      .containsExactly(CleanCodeAttribute.CONVENTIONAL.name());
  }

  @Test
  public void execute_whenRuleWithDefinedCleanCodeAttribute_shouldNotUpdate() throws SQLException {
    insertRule("1", CleanCodeAttribute.FOCUSED);
    underTest.execute();
    assertThat(db.select("select uuid, clean_code_attribute from rules"))
      .extracting(stringObjectMap -> stringObjectMap.get("clean_code_attribute"))
      .containsExactly(CleanCodeAttribute.FOCUSED.name());
  }

  @Test
  public void execute_whenRuleIsHotspot_shouldNotUpdate() throws SQLException {
    insertRule("1", RuleType.SECURITY_HOTSPOT, null, null);
    underTest.execute();
    assertThat(db.select("select uuid, clean_code_attribute from rules"))
      .extracting(stringObjectMap -> stringObjectMap.get("clean_code_attribute"))
      .containsOnlyNulls();
  }

  @Test
  public void execute_whenAdhocRuleIsHotspot_shouldNotUpdate() throws SQLException {
    insertRule("1", null, RuleType.SECURITY_HOTSPOT, null);
    underTest.execute();
    assertThat(db.select("select uuid, clean_code_attribute from rules"))
      .extracting(stringObjectMap -> stringObjectMap.get("clean_code_attribute"))
      .containsOnlyNulls();
  }


  private void insertRule(String uuid, @Nullable RuleType ruleType, @Nullable RuleType adhocRuleType, @Nullable CleanCodeAttribute cleanCodeAttribute) {
    db.executeInsert(TABLE_NAME,
      "UUID", uuid,
      "PLUGIN_RULE_KEY", "key",
      "PLUGIN_NAME", "name",
      "SCOPE", "1",
      "CLEAN_CODE_ATTRIBUTE", cleanCodeAttribute != null ? cleanCodeAttribute.name() : null,
      "IS_TEMPLATE", false,
      "RULE_TYPE", ruleType != null ? ruleType.getDbConstant() : null,
      "AD_HOC_TYPE", adhocRuleType != null ? adhocRuleType.getDbConstant() : null,
      "IS_AD_HOC", false,
      "IS_EXTERNAL", false);
  }

  private void insertRule(String uuid, @Nullable CleanCodeAttribute cleanCodeAttribute) {
    insertRule(uuid, RuleType.CODE_SMELL, null, cleanCodeAttribute);
  }
}
