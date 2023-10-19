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

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.core.documentation.DocumentationLinkGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExternalIssueReportValidatorTest {

  private static final String DEPRECATED_REPORTS_LOCATION = "src/test/resources/org/sonar/scanner/externalissue/";
  private static final String REPORTS_LOCATION = "src/test/resources/org/sonar/scanner/externalissue/cct/";
  private static final String URL = "/analyzing-source-code/importing-external-issues/generic-issue-import-format/";
  private static final String TEST_URL = "test-url";

  private final Gson gson = new Gson();
  private final Path reportPath = Paths.get("report-path");
  private final DocumentationLinkGenerator documentationLinkGenerator = mock(DocumentationLinkGenerator.class);
  private final ExternalIssueReportValidator validator = new ExternalIssueReportValidator(documentationLinkGenerator);

  @Rule
  public LogTester logTester = new LogTester();

  @Before
  public void setUp() {
    when(documentationLinkGenerator.getDocumentationLink(URL)).thenReturn(TEST_URL);
  }

  @Test
  public void validate_whenInvalidReport_shouldThrowException() throws IOException {
    ExternalIssueReport report = readInvalidReport(DEPRECATED_REPORTS_LOCATION);
    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': invalid report detected.");
  }

  @Test
  public void validate_whenCorrect_shouldNotThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    assertThatCode(() -> validator.validate(report, reportPath)).doesNotThrowAnyException();
  }

  @Test
  public void validate_whenDuplicateRuleIdFound_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.rules[0].id = "rule2";

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': found duplicate rule ID 'rule2'.");
  }

  @Test
  public void validate_whenMissingMandatoryCleanCodeAttributeField_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.rules[0].cleanCodeAttribute = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'cleanCodeAttribute'.");
  }

  @Test
  public void validate_whenMissingEngineIdField_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.rules[0].engineId = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'engineId'.");
  }

  @Test
  public void validate_whenMissingFilepathFieldForPrimaryLocation_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.issues[0].primaryLocation.filePath = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'filePath' in the primary location of the issue.");
  }

  @Test
  public void validate_whenMissingImpactsField_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.rules[0].impacts = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'impacts'.");
  }

  @Test
  public void validate_whenMissingMessageFieldForPrimaryLocation_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.issues[0].primaryLocation.message = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'message' in the primary location of the issue.");
  }

  @Test
  public void validate_whenMissingStartLineFieldForPrimaryLocation_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.issues[0].primaryLocation.textRange.startLine = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'startLine of the text range' in the primary location of the issue.");
  }

  @Test
  public void validate_whenReportMissingFilePathForSecondaryLocation_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.issues[3].secondaryLocations[0].filePath = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'filePath' in a secondary location of the issue.");
  }

  @Test
  public void validate_whenReportMissingTextRangeForSecondaryLocation_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.issues[3].secondaryLocations[0].textRange = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'textRange' in a secondary location of the issue.");
  }

  @Test
  public void validate_whenReportMissingTextRangeStartLineForSecondaryLocation_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.issues[3].secondaryLocations[0].textRange.startLine = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'startLine of the text range' in a secondary location of the issue.");
  }

  @Test
  public void validate_whenMissingNameField_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.rules[0].name = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'name'.");
  }

  @Test
  public void validate_whenMissingPrimaryLocationField_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.issues[0].primaryLocation = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'primaryLocation'.");
  }

  @Test
  public void validate_whenMissingOrEmptyRuleIdField_shouldThrowException() throws IOException {
    String errorMessage = "Failed to parse report 'report-path': missing mandatory field 'id'.";

    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.rules[0].id = null;
    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(errorMessage);

    report.rules[0].id = "";
    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(errorMessage);
  }

  @Test
  public void validate_whenIssueContainsRuleIdNotPresentInReport_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.issues[0].ruleId = "rule-id-not-present";

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': rule with 'rule-id-not-present' not present.");
  }

  @Test
  public void validate_whenIssueRuleIdNotPresentInReport_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.issues[0].ruleId = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'ruleId'.");
  }

  @Test
  public void validate_whenContainsDeprecatedSeverityEntry_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.issues[0].severity = "MAJOR";

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Deprecated 'severity' field found in the following report: 'report-path'.");
  }

  @Test
  public void validate_whenContainsDeprecatedTypeEntry_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.issues[0].type = "BUG";

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Deprecated 'type' field found in the following report: 'report-path'.");
  }

  @Test
  public void validate_whenContainsEmptyImpacts_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(REPORTS_LOCATION);
    report.rules[0].impacts = new ExternalIssueReport.Impact[0];

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': mandatory array 'impacts' not populated.");
  }

  @Test
  public void validate_whenDeprecatedReportFormat_shouldValidateWithWarningLog() throws IOException {
    ExternalIssueReport report = read(DEPRECATED_REPORTS_LOCATION);
    assertThatCode(() -> validator.validate(report, reportPath)).doesNotThrowAnyException();
    assertWarningLog();
  }

  @Test
  public void validate_whenDeprecatedReportMissingEngineId_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(DEPRECATED_REPORTS_LOCATION);
    report.issues[0].engineId = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'engineId'.");
    assertWarningLog();
  }

  @Test
  public void validate_whenDeprecatedReportMissingFilepathForPrimaryLocation_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(DEPRECATED_REPORTS_LOCATION);
    report.issues[0].primaryLocation.filePath = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'filePath' in the primary location of the issue.");
    assertWarningLog();
  }

  @Test
  public void validate_whenDeprecatedReportMissingMessageForPrimaryLocation_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(DEPRECATED_REPORTS_LOCATION);
    report.issues[0].primaryLocation.message = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'message' in the primary location of the issue.");
    assertWarningLog();
  }

  @Test
  public void validate_whenDeprecatedReportMissingPrimaryLocation_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(DEPRECATED_REPORTS_LOCATION);
    report.issues[0].primaryLocation = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'primaryLocation'.");
    assertWarningLog();
  }

  @Test
  public void validate_whenDeprecatedReportMissingStartLineForPrimaryLocation_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(DEPRECATED_REPORTS_LOCATION);
    report.issues[0].primaryLocation.textRange.startLine = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'startLine of the text range' in the primary location of the issue.");
    assertWarningLog();
  }

  @Test
  public void validate_whenDeprecatedReportMissingFilePathForSecondaryLocation_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(DEPRECATED_REPORTS_LOCATION);
    report.issues[3].secondaryLocations[0].filePath = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'filePath' in a secondary location of the issue.");
    assertWarningLog();
  }

  @Test
  public void validate_whenDeprecatedReportMissingTextRangeForSecondaryLocation_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(DEPRECATED_REPORTS_LOCATION);
    report.issues[3].secondaryLocations[0].textRange = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'textRange' in a secondary location of the issue.");
    assertWarningLog();
  }

  @Test
  public void validate_whenDeprecatedReportMissingTextRangeStartLineForSecondaryLocation_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(DEPRECATED_REPORTS_LOCATION);
    report.issues[3].secondaryLocations[0].textRange.startLine = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'startLine of the text range' in a secondary location of the issue.");
    assertWarningLog();
  }

  @Test
  public void validate_whenDeprecatedReportMissingRuleId_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(DEPRECATED_REPORTS_LOCATION);
    report.issues[0].ruleId = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'ruleId'.");
    assertWarningLog();
  }

  @Test
  public void validate_whenDeprecatedReportMissingSeverity_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(DEPRECATED_REPORTS_LOCATION);
    report.issues[0].severity = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'severity'.");
    assertWarningLog();
  }

  @Test
  public void validate_whenDeprecatedReportMissingType_shouldThrowException() throws IOException {
    ExternalIssueReport report = read(DEPRECATED_REPORTS_LOCATION);
    report.issues[0].type = null;

    assertThatThrownBy(() -> validator.validate(report, reportPath))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to parse report 'report-path': missing mandatory field 'type'.");
    assertWarningLog();
  }

  private void assertWarningLog() {
    assertThat(logTester.getLogs(Level.WARN))
      .extracting(LogAndArguments::getFormattedMsg)
      .contains("External issues were imported with a deprecated format which will be removed soon. " +
        "Please switch to the newest format to fully benefit from Clean Code: " + TEST_URL);
  }

  private ExternalIssueReport read(String location) throws IOException {
    Reader reader = Files.newBufferedReader(Paths.get(location + "report.json"), StandardCharsets.UTF_8);
    return gson.fromJson(reader, ExternalIssueReport.class);
  }

  private ExternalIssueReport readInvalidReport(String location) throws IOException {
    Reader reader = Files.newBufferedReader(Paths.get(location + "invalid_report.json"), StandardCharsets.UTF_8);
    return gson.fromJson(reader, ExternalIssueReport.class);
  }

}
