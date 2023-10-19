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
package org.sonar.scanner.externalissue;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ExternalIssueReportParserTest {
  private static final String DEPRECATED_REPORTS_LOCATION = "src/test/resources/org/sonar/scanner/externalissue/";
  private static final String REPORTS_LOCATION = "src/test/resources/org/sonar/scanner/externalissue/cct/";
  private final ExternalIssueReportValidator validator = mock(ExternalIssueReportValidator.class);
  private final ExternalIssueReportParser externalIssueReportParser = new ExternalIssueReportParser(validator);
  private Path reportPath;

  @Test
  public void parse_whenCorrectCctFormat_shouldParseCorrectly() {
    reportPath = Paths.get(REPORTS_LOCATION + "report.json");

    ExternalIssueReport report = externalIssueReportParser.parse(reportPath);

    verify(validator).validate(report, reportPath);
    assertCctReport(report);
  }

  @Test
  public void parse_whenCorrectDeprecatedFormat_shouldParseCorrectly() {
    reportPath = Paths.get(DEPRECATED_REPORTS_LOCATION + "report.json");

    ExternalIssueReport report = externalIssueReportParser.parse(reportPath);

    verify(validator).validate(report, reportPath);
    assertDeprecatedReport(report);
  }

  @Test
  public void parse_whenDoesntExist_shouldFail() {
    reportPath = Paths.get("unknown.json");

    assertThatThrownBy(() -> externalIssueReportParser.parse(reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to read external issues report 'unknown.json'");
  }

  @Test
  public void parse_whenInvalidDeprecatedFormat_shouldFail() {
    reportPath = Paths.get(DEPRECATED_REPORTS_LOCATION + "report_invalid_json.json");

    assertThatThrownBy(() -> externalIssueReportParser.parse(reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to read external issues report 'src/test/resources/org/sonar/scanner/externalissue/report_invalid_json.json': " +
        "invalid JSON syntax");
  }

  @Test
  public void parse_whenValidatorThrowsException_shouldPropagate() {
    reportPath = Paths.get(DEPRECATED_REPORTS_LOCATION + "report.json");

    doThrow(new IllegalStateException("Just a dummy exception")).when(validator).validate(any(), eq(reportPath));

    assertThatThrownBy(() -> externalIssueReportParser.parse(reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Just a dummy exception");
  }

  private static void assertDeprecatedReport(ExternalIssueReport report) {
    assertThat(report.issues).hasSize(4);

    ExternalIssueReport.Issue issue1 = report.issues[0];
    assertThat(issue1.engineId).isEqualTo("eslint");
    assertThat(issue1.ruleId).isEqualTo("rule1");
    assertThat(issue1.severity).isEqualTo("MAJOR");
    assertThat(issue1.effortMinutes).isEqualTo(40);
    assertThat(issue1.type).isEqualTo("CODE_SMELL");
    assertThat(issue1.primaryLocation.filePath).isEqualTo("file1.js");
    assertThat(issue1.primaryLocation.message).isEqualTo("fix the issue here");
    assertThat(issue1.primaryLocation.textRange.startColumn).isEqualTo(2);
    assertThat(issue1.primaryLocation.textRange.startLine).isEqualTo(1);
    assertThat(issue1.primaryLocation.textRange.endColumn).isEqualTo(4);
    assertThat(issue1.primaryLocation.textRange.endLine).isEqualTo(3);
    assertThat(issue1.secondaryLocations).isNull();

    ExternalIssueReport.Issue issue2 = report.issues[3];
    assertThat(issue2.engineId).isEqualTo("eslint");
    assertThat(issue2.ruleId).isEqualTo("rule3");
    assertThat(issue2.severity).isEqualTo("MAJOR");
    assertThat(issue2.effortMinutes).isNull();
    assertThat(issue2.type).isEqualTo("BUG");
    assertThat(issue2.secondaryLocations).hasSize(2);
    assertThat(issue2.secondaryLocations[0].filePath).isEqualTo("file1.js");
    assertThat(issue2.secondaryLocations[0].message).isEqualTo("fix the bug here");
    assertThat(issue2.secondaryLocations[0].textRange.startLine).isEqualTo(1);
    assertThat(issue2.secondaryLocations[1].filePath).isEqualTo("file2.js");
    assertThat(issue2.secondaryLocations[1].message).isNull();
    assertThat(issue2.secondaryLocations[1].textRange.startLine).isEqualTo(2);
  }

  private static void assertCctReport(ExternalIssueReport report) {
    assertThat(report.rules).hasSize(2);

    ExternalIssueReport.Rule rule = report.rules[0];
    assertThat(rule.id).isEqualTo("rule1");
    assertThat(rule.engineId).isEqualTo("test");
    assertThat(rule.name).isEqualTo("just_some_rule_name");
    assertThat(rule.description).isEqualTo("just_some_description");
    assertThat(rule.cleanCodeAttribute).isEqualTo("FORMATTED");
    assertThat(rule.impacts).hasSize(2);
    assertThat(rule.impacts[0].severity).isEqualTo("HIGH");
    assertThat(rule.impacts[0].softwareQuality).isEqualTo("MAINTAINABILITY");
    assertThat(rule.impacts[1].severity).isEqualTo("LOW");
    assertThat(rule.impacts[1].softwareQuality).isEqualTo("SECURITY");

    assertThat(report.issues).hasSize(8);

    ExternalIssueReport.Issue issue1 = report.issues[0];
    assertThat(issue1.engineId).isNull();
    assertThat(issue1.ruleId).isEqualTo("rule1");
    assertThat(issue1.severity).isNull();
    assertThat(issue1.effortMinutes).isEqualTo(40);
    assertThat(issue1.type).isNull();
    assertThat(issue1.primaryLocation.filePath).isEqualTo("file1.js");
    assertThat(issue1.primaryLocation.message).isEqualTo("fix the issue here");
    assertThat(issue1.primaryLocation.textRange.startColumn).isEqualTo(2);
    assertThat(issue1.primaryLocation.textRange.startLine).isEqualTo(1);
    assertThat(issue1.primaryLocation.textRange.endColumn).isEqualTo(4);
    assertThat(issue1.primaryLocation.textRange.endLine).isEqualTo(3);
    assertThat(issue1.secondaryLocations).isNull();

    ExternalIssueReport.Issue issue2 = report.issues[7];
    assertThat(issue2.engineId).isNull();
    assertThat(issue2.ruleId).isEqualTo("rule2");
    assertThat(issue2.severity).isNull();
    assertThat(issue2.effortMinutes).isNull();
    assertThat(issue2.type).isNull();
    assertThat(issue2.secondaryLocations).hasSize(2);
    assertThat(issue2.secondaryLocations[0].filePath).isEqualTo("file1.js");
    assertThat(issue2.secondaryLocations[0].message).isEqualTo("fix the bug here");
    assertThat(issue2.secondaryLocations[0].textRange.startLine).isEqualTo(1);
    assertThat(issue2.secondaryLocations[1].filePath).isEqualTo("file2.js");
    assertThat(issue2.secondaryLocations[1].message).isNull();
    assertThat(issue2.secondaryLocations[1].textRange.startLine).isEqualTo(2);
  }

}
