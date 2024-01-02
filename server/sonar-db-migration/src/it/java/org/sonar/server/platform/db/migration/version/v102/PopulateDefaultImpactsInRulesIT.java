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
package org.sonar.server.platform.db.migration.version.v102;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.RuleType;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.core.util.Uuids;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateDefaultImpactsInRulesIT {
  private static final String TABLE_NAME = "rules";

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateDefaultImpactsInRules.class);
  @Rule
  public LogTester logTester = new LogTester();

  private final PopulateDefaultImpactsInRules underTest = new PopulateDefaultImpactsInRules(db.database());

  @Test
  public void execute_whenRulesDoNotExist_shouldNotFail() {
    assertThatCode(underTest::execute).doesNotThrowAnyException();
  }

  @Test
  public void execute_whenRulesHasTypeAndSeverity_shouldCreateImpact() throws SQLException {
    insertRuleWithType("uuid", RuleType.CODE_SMELL, Severity.MAJOR);
    underTest.execute();

    assertThat(db.select("select software_quality, severity from rules_default_impacts"))
      .extracting(stringObjectMap -> stringObjectMap.get("software_quality"),
        stringObjectMap -> stringObjectMap.get("severity"))
      .containsExactly(tuple(SoftwareQuality.MAINTAINABILITY.name(), org.sonar.api.issue.impact.Severity.MEDIUM.name()));
  }

  @Test
  public void execute_shouldBeReentrant() throws SQLException {
    insertRuleWithType("uuid", RuleType.CODE_SMELL, Severity.MAJOR);
    underTest.execute();
    underTest.execute();

    assertThat(db.select("select software_quality, severity from rules_default_impacts"))
      .hasSize(1)
      .extracting(stringObjectMap -> stringObjectMap.get("software_quality"),
        stringObjectMap -> stringObjectMap.get("severity"))
      .containsExactly(tuple(SoftwareQuality.MAINTAINABILITY.name(), org.sonar.api.issue.impact.Severity.MEDIUM.name()));

  }

  @Test
  public void execute_whenAdhocRulesHasTypeAndSeverity_shouldCreateImpact() throws SQLException {
    insertRuleWithAdHocType("uuid", RuleType.CODE_SMELL, Severity.MAJOR);
    underTest.execute();

    assertThat(db.select("select software_quality, severity from rules_default_impacts"))
      .hasSize(1)
      .extracting(stringObjectMap -> stringObjectMap.get("software_quality"),
        stringObjectMap -> stringObjectMap.get("severity"))
      .containsExactly(tuple(SoftwareQuality.MAINTAINABILITY.name(), org.sonar.api.issue.impact.Severity.MEDIUM.name()));

  }

  @Test
  public void execute_whenAdhocRulesHasImpactAlready_shouldNotCreateImpact() throws SQLException {
    insertRuleWithAdHocType("uuid", RuleType.CODE_SMELL, Severity.MAJOR);
    insertImpact("uuid", SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.HIGH);
    underTest.execute();

    assertThat(db.select("select software_quality, severity from rules_default_impacts"))
      .hasSize(1)
      .extracting(stringObjectMap -> stringObjectMap.get("software_quality"),
        stringObjectMap -> stringObjectMap.get("severity"))
      .containsExactly(tuple(SoftwareQuality.SECURITY.name(), org.sonar.api.issue.impact.Severity.HIGH.name()));

  }

  @Test
  public void execute_whenNoTypeAndSeverityDefined_shouldNotCreateImpact() throws SQLException {
    insertRuleWithType("uuid", null, null);
    underTest.execute();

    assertThat(db.select("select software_quality, severity from rules_default_impacts"))
      .isEmpty();

  }

  @Test
  public void execute_whenInvalidValueDefined_shouldNotCreateImpactAndLog() throws SQLException {
    insertInvalidRule("uuid");
    underTest.execute();

    assertThat(db.select("select software_quality, severity from rules_default_impacts"))
      .isEmpty();
    assertThat(logTester.logs()).contains("Error while mapping type to impact for rule 'uuid'");

  }

  @Test
  public void execute_whenTypeIsHotspot_shouldNotCreateImpactAndLog() throws SQLException {
    insertRuleWithType("uuid", RuleType.SECURITY_HOTSPOT, Severity.MAJOR);
    underTest.execute();

    assertThat(db.select("select software_quality, severity from rules_default_impacts"))
      .isEmpty();
    assertThat(logTester.logs()).doesNotContain("Error while mapping type to impact for rule 'uuid'");
  }

  @Test
  public void execute_whenRuleHasEmptyFields_shouldCreateADefaultImpact() throws SQLException {
    insertPlaceholderAdhocRule("uuid");
    underTest.execute();

    assertThat(db.select("select software_quality, severity from rules_default_impacts"))
      .hasSize(1)
      .extracting(stringObjectMap -> stringObjectMap.get("software_quality"),
        stringObjectMap -> stringObjectMap.get("severity"))
      .containsExactly(tuple(SoftwareQuality.MAINTAINABILITY.name(), org.sonar.api.issue.impact.Severity.MEDIUM.name()));
  }

  @Test
  public void execute_whenStandardRuleHasBothAdhocAndStandardTypeAndSeverity_shouldCreateADefaultImpactWithAdhocTypes() throws SQLException {
    insertRule("uuid", RuleType.CODE_SMELL, Severity.CRITICAL, RuleType.VULNERABILITY, Severity.MINOR, true);
    underTest.execute();

    assertThat(db.select("select software_quality, severity from rules_default_impacts"))
      .hasSize(1)
      .extracting(stringObjectMap -> stringObjectMap.get("software_quality"),
        stringObjectMap -> stringObjectMap.get("severity"))
      .containsExactly(tuple(SoftwareQuality.SECURITY.name(), org.sonar.api.issue.impact.Severity.LOW.name()));
  }

  private void insertRuleWithType(String uuid, @Nullable RuleType ruleType, @Nullable Severity severity) {
    insertRule(uuid, ruleType, severity, null, null);
  }

  private void insertRuleWithAdHocType(String uuid, @Nullable RuleType adHocType, @Nullable Severity adHocseverity) {
    insertRule(uuid, null, null, adHocType, adHocseverity);
  }


  private void insertRule(String uuid, @Nullable RuleType ruleType, @Nullable Severity severity, @Nullable RuleType adHocType, @Nullable Severity adHocseverity) {
    insertRule(uuid, ruleType, severity, adHocType, adHocseverity, adHocType != null);
  }

  private void insertRule(String uuid, @Nullable RuleType ruleType, @Nullable Severity severity, @Nullable RuleType adHocType, @Nullable Severity adHocseverity, boolean isAdhoc) {
    db.executeInsert(TABLE_NAME,
      "UUID", uuid,
      "PLUGIN_RULE_KEY", "key",
      "PLUGIN_NAME", "name",
      "SCOPE", "1",
      "RULE_TYPE", ruleType != null ? ruleType.getDbConstant() : null,
      "PRIORITY", severity != null ? org.sonar.api.rule.Severity.ALL.indexOf(severity.name()) : null,
      "AD_HOC_TYPE", adHocType != null ? adHocType.getDbConstant() : null,
      "AD_HOC_SEVERITY", adHocseverity != null ? adHocseverity.name() : null,
      "IS_TEMPLATE", false,
      "IS_AD_HOC", isAdhoc,
      "IS_EXTERNAL", isAdhoc);
  }

  private void insertInvalidRule(String uuid) {
    db.executeInsert(TABLE_NAME,
      "UUID", uuid,
      "PLUGIN_RULE_KEY", "key",
      "PLUGIN_NAME", "name",
      "SCOPE", "1",
      "RULE_TYPE", 100,
      "PRIORITY", -1,
      "AD_HOC_TYPE", 100,
      "AD_HOC_SEVERITY", "-1",
      "IS_TEMPLATE", false,
      "IS_AD_HOC", false,
      "IS_EXTERNAL", false);
  }

  private void insertPlaceholderAdhocRule(String uuid) {
    db.executeInsert(TABLE_NAME,
      "UUID", uuid,
      "PLUGIN_RULE_KEY", "key",
      "PLUGIN_NAME", "name",
      "SCOPE", "1",
      "IS_TEMPLATE", false,
      "IS_AD_HOC", true,
      "IS_EXTERNAL", false);
  }

  private void insertImpact(String ruleUuid, SoftwareQuality softwareQuality, org.sonar.api.issue.impact.Severity severity) {
    db.executeInsert("RULES_DEFAULT_IMPACTS",
      "UUID", Uuids.create(),
      "RULE_UUID", ruleUuid,
      "SOFTWARE_QUALITY", softwareQuality.name(),
      "SEVERITY", severity.name());
  }

}
