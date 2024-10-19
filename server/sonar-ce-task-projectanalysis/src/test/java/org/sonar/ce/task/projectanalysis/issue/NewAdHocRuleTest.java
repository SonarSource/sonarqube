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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Map;
import org.junit.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.RuleType;
import org.sonar.scanner.protocol.Constants;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NewAdHocRuleTest {

  @Test
  public void fail_if_engine_id_is_not_set() {
    assertThatThrownBy(() -> new NewAdHocRule(ScannerReport.ExternalIssue.newBuilder().build()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'engine id' not expected to be null for an ad hoc rule");
  }

  @Test
  public void test_equals_and_hashcode() {
    NewAdHocRule adHocRule1 = new NewAdHocRule(ScannerReport.ExternalIssue.newBuilder().setEngineId("eslint").setRuleId("no-cond-assign").build());
    NewAdHocRule adHocRule2 = new NewAdHocRule(ScannerReport.ExternalIssue.newBuilder().setEngineId("eslint").setRuleId("no-cond-assign").build());
    NewAdHocRule anotherAdHocRule = new NewAdHocRule(ScannerReport.ExternalIssue.newBuilder().setEngineId("eslint").setRuleId("another").build());

    assertThat(adHocRule1)
      .isEqualTo(adHocRule1)
      .isEqualTo(adHocRule2)
      .isNotNull()
      .isNotEqualTo(anotherAdHocRule)
      .hasSameHashCodeAs(adHocRule1)
      .hasSameHashCodeAs(adHocRule2);
    assertThat(adHocRule1.hashCode()).isNotEqualTo(anotherAdHocRule.hashCode());
  }

  @Test
  public void constructor_whenAdhocRuleHasProvidedImpact_shouldMapTypeAndSeverityAccordingly() {
    NewAdHocRule adHocRule = new NewAdHocRule(ScannerReport.AdHocRule.newBuilder()
      .setEngineId("eslint").setRuleId("no-cond-assign").setName("name")
      .addDefaultImpacts(ScannerReport.Impact.newBuilder().setSoftwareQuality(SoftwareQuality.MAINTAINABILITY.name()).setSeverity(Severity.LOW.name()).build())
      .build());

    assertThat(adHocRule.getRuleType()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(adHocRule.getSeverity()).isEqualTo(org.sonar.api.batch.rule.Severity.MINOR.name());
  }

  @Test
  public void constructor_whenAdhocRuleHasNoProvidedImpact_shouldMapDefaultImpactAccordingly() {
    NewAdHocRule adHocRule = new NewAdHocRule(ScannerReport.AdHocRule.newBuilder()
      .setEngineId("eslint").setRuleId("no-cond-assign").setName("name")
      .setType(ScannerReport.IssueType.CODE_SMELL)
      .setSeverity(Constants.Severity.MINOR)
      .build());

    assertThat(adHocRule.getDefaultImpacts())
      .containsExactlyEntriesOf(Map.of(SoftwareQuality.MAINTAINABILITY, Severity.LOW));
  }

  @Test
  public void constructor_whenAdhocRuleHasBothTypeAndSeverityAndProvidedImpact_shouldKeepSeverityAndTypeAndImpacts() {
    NewAdHocRule adHocRule = new NewAdHocRule(ScannerReport.AdHocRule.newBuilder()
      .setEngineId("eslint").setRuleId("no-cond-assign").setName("name")
      .setType(ScannerReport.IssueType.CODE_SMELL)
      .setSeverity(Constants.Severity.MINOR)
      .addDefaultImpacts(ScannerReport.Impact.newBuilder().setSoftwareQuality(SoftwareQuality.RELIABILITY.name())
        .setSeverity(Severity.HIGH.name()).build())
      .build());

    assertThat(adHocRule.getDefaultImpacts())
      .containsExactlyEntriesOf(Map.of(SoftwareQuality.RELIABILITY, Severity.HIGH));

    assertThat(adHocRule.getRuleType()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(adHocRule.getSeverity()).isEqualTo(org.sonar.api.batch.rule.Severity.MINOR.name());

  }

  @Test
  public void constructor_whenNoSeverityNorImpactsAreProvided_shouldThrowIllegalArgumentException() {
    ScannerReport.AdHocRule scannerReport = ScannerReport.AdHocRule.newBuilder()
      .setEngineId("eslint").setRuleId("no-cond-assign").setName("name")
      .build();

    assertThatThrownBy(() -> new NewAdHocRule(scannerReport))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'severity' not expected to be null for an ad hoc rule, or impacts should be provided instead");
  }

  @Test
  public void constructor_whenNoTypeNorImpactsAreProvided_shouldThrowIllegalArgumentException() {
    ScannerReport.AdHocRule scannerReport = ScannerReport.AdHocRule.newBuilder()
      .setSeverity(Constants.Severity.MINOR)
      .setEngineId("eslint").setRuleId("no-cond-assign").setName("name")
      .build();

    assertThatThrownBy(() -> new NewAdHocRule(scannerReport))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'issue type' not expected to be null for an ad hoc rule, or impacts should be provided instead");
  }

  @Test
  public void constructor_whenRuleHotspot_shouldNotPopulateImpactsNorAttribute() {
    NewAdHocRule adHocRule1 = new NewAdHocRule(ScannerReport.ExternalIssue.newBuilder()
      .setEngineId("eslint")
      .setType(ScannerReport.IssueType.SECURITY_HOTSPOT)
      .setSeverity(Constants.Severity.BLOCKER)
      .setRuleId("no-cond-assign").build());
    assertThat(adHocRule1.getDefaultImpacts()).isEmpty();
    assertThat(adHocRule1.getCleanCodeAttribute()).isNull();
  }

  @Test
  public void constructor_whenIssueHotspot_shouldNotPopulateImpactsNorAttribute() {
    NewAdHocRule adHocRule1 = new NewAdHocRule(ScannerReport.ExternalIssue.newBuilder()
      .setType(ScannerReport.IssueType.SECURITY_HOTSPOT)
      .setSeverity(Constants.Severity.BLOCKER)
      .setEngineId("eslint")
      .setRuleId("no-cond-assign").build());
    assertThat(adHocRule1.getDefaultImpacts()).isEmpty();
    assertThat(adHocRule1.getCleanCodeAttribute()).isNull();
  }

}
