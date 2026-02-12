/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.scanner.externalissue.sarif;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.google.common.collect.MoreCollectors;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.event.Level;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.core.sarif.SarifDeserializationException;
import org.sonar.core.sarif.SarifDeserializationException.Category;
import org.sonar.core.sarif.SarifSerializer;
import org.sonar.sarif.pojo.SarifSchema210;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SarifIssuesImportSensorTest {

  private static final String FILE_1 = "path/to/sarif/file.sarif";
  private static final String FILE_2 = "path/to/sarif/file2.sarif";
  private static final String SARIF_REPORT_PATHS_PARAM = FILE_1 + "," + FILE_2;

  @Mock
  private SarifSerializer sarifSerializer;
  @Mock
  private Sarif210Importer sarifImporter;
  @Mock
  private AnalysisWarnings analysisWarnings;

  private MapSettings sensorSettings;

  @Before
  public void before() {
    sensorSettings = new MapSettings();
  }

  @Rule
  public final LogTester logTester = new LogTester();

  private final SensorContextTester sensorContext = SensorContextTester.create(Path.of("."));

  @Test
  public void execute_whenSingleFileIsSpecified_shouldImportResults() {
    sensorSettings.setProperty("sonar.sarifReportPaths", FILE_1);

    ReportAndResults reportAndResults = mockSuccessfulReportAndResults(FILE_1);

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig(), analysisWarnings);
    sensor.execute(sensorContext);

    verify(sarifImporter).importSarif(reportAndResults.getSarifReport());

    assertThat(logTester.logs(Level.INFO)).hasSize(1);
    assertSummaryIsCorrectlyDisplayedForSuccessfulFile(FILE_1, reportAndResults.getSarifImportResults());
  }

  @Test
  public void execute_whenMultipleFilesAreSpecified_shouldImportResults() {
    sensorSettings.setProperty("sonar.sarifReportPaths", SARIF_REPORT_PATHS_PARAM);
    ReportAndResults reportAndResults1 = mockSuccessfulReportAndResults(FILE_1);
    ReportAndResults reportAndResults2 = mockSuccessfulReportAndResults(FILE_2);

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig(), analysisWarnings);
    sensor.execute(sensorContext);

    verify(sarifImporter).importSarif(reportAndResults1.getSarifReport());
    verify(sarifImporter).importSarif(reportAndResults2.getSarifReport());

    assertSummaryIsCorrectlyDisplayedForSuccessfulFile(FILE_1, reportAndResults1.getSarifImportResults());
    assertSummaryIsCorrectlyDisplayedForSuccessfulFile(FILE_2, reportAndResults2.getSarifImportResults());
  }

  @Test
  public void execute_whenFileContainsOnlySuccessfulRuns_shouldLogCorrectMessage() {
    sensorSettings.setProperty("sonar.sarifReportPaths", FILE_1);
    ReportAndResults reportAndResults = mockSuccessfulReportAndResults(FILE_1);

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig(), analysisWarnings);
    sensor.execute(sensorContext);

    assertSummaryIsCorrectlyDisplayedForSuccessfulFile(FILE_1, reportAndResults.getSarifImportResults());
  }

  @Test
  public void execute_whenFileContainsOnlyFailedRuns_shouldLogCorrectMessage() {

    sensorSettings.setProperty("sonar.sarifReportPaths", FILE_1);
    ReportAndResults reportAndResults = mockFailedReportAndResults(FILE_1);

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig(), analysisWarnings);
    sensor.execute(sensorContext);

    assertSummaryIsCorrectlyDisplayedForFailedFile(FILE_1, reportAndResults.getSarifImportResults());
  }

  @Test
  public void execute_whenFileContainsFailedAndSuccessfulRuns_shouldLogCorrectMessage() {

    sensorSettings.setProperty("sonar.sarifReportPaths", FILE_1);

    ReportAndResults reportAndResults = mockMixedReportAndResults(FILE_1);

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig(), analysisWarnings);
    sensor.execute(sensorContext);

    verify(sarifImporter).importSarif(reportAndResults.getSarifReport());

    assertSummaryIsCorrectlyDisplayedForMixedFile(FILE_1, reportAndResults.getSarifImportResults());
  }

  @Test
  public void execute_whenImportFails_shouldSkipReport() {
    sensorSettings.setProperty("sonar.sarifReportPaths", SARIF_REPORT_PATHS_PARAM);

    ReportAndResults reportAndResults1 = mockFailedReportAndResults(FILE_1);
    ReportAndResults reportAndResults2 = mockSuccessfulReportAndResults(FILE_2);

    doThrow(new NullPointerException("import failed")).when(sarifImporter).importSarif(reportAndResults1.getSarifReport());

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig(), analysisWarnings);
    sensor.execute(sensorContext);

    verify(sarifImporter).importSarif(reportAndResults2.getSarifReport());
    assertThat(logTester.logs(Level.WARN))
      .contains("File path/to/sarif/file.sarif could not be parsed, 0 runs imported (import failed).");
    assertSummaryIsCorrectlyDisplayedForSuccessfulFile(FILE_2, reportAndResults2.getSarifImportResults());
  }

  @Test
  public void execute_whenDeserializationFails_shouldSkipReport() {
    sensorSettings.setProperty("sonar.sarifReportPaths", SARIF_REPORT_PATHS_PARAM);

    failDeserializingReport(FILE_1);
    ReportAndResults reportAndResults2 = mockSuccessfulReportAndResults(FILE_2);

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig(), analysisWarnings);
    sensor.execute(sensorContext);

    verify(sarifImporter).importSarif(reportAndResults2.getSarifReport());
    assertThat(logTester.logs(Level.WARN))
      .contains("File path/to/sarif/file.sarif could not be parsed, 0 runs imported (deserialization failed).");
    assertSummaryIsCorrectlyDisplayedForSuccessfulFile(FILE_2, reportAndResults2.getSarifImportResults());
  }

  @Test
  public void execute_whenDeserializationThrowsFileNotFound_shouldLogWarning() {
    sensorSettings.setProperty("sonar.sarifReportPaths", FILE_1);

    failDeserializingReportWithException(FILE_1, new SarifDeserializationException(Category.FILE_NOT_FOUND, "file not found", new RuntimeException("non-existent")));

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig(), analysisWarnings);
    sensor.execute(sensorContext);

    assertThat(logTester.logs(Level.WARN))
      .contains("File path/to/sarif/file.sarif could not be parsed, 0 runs imported (file not found).");
    verify(analysisWarnings).addUnique(SarifIssuesImportSensor.SARIF_IMPORT_ANALYSIS_WARNING);
  }

  @Test
  public void execute_whenSyntaxExceptionWithJsonLocation_shouldLogWithLineNumber() {
    sensorSettings.setProperty("sonar.sarifReportPaths", FILE_1);

    JsonLocation location = new JsonLocation(null, 0, 42, 5);
    JsonParseException jsonCause = new JsonParseException(null, "parse error", location);
    failDeserializingReportWithException(FILE_1, new SarifDeserializationException(Category.SYNTAX, "syntax error", jsonCause));

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig(), analysisWarnings);
    sensor.execute(sensorContext);

    assertThat(logTester.logs(Level.WARN))
      .contains("File path/to/sarif/file.sarif could not be parsed, 0 runs imported (invalid JSON syntax or non-UTF-8 encoding at line 42).");
    verify(analysisWarnings).addUnique(SarifIssuesImportSensor.SARIF_IMPORT_ANALYSIS_WARNING);
  }

  @Test
  public void execute_whenValueExceptionWithNullLocation_shouldLogWithoutLineNumber() {
    sensorSettings.setProperty("sonar.sarifReportPaths", FILE_1);

    JsonParseException jsonCause = new JsonParseException(null, "value overflow");
    failDeserializingReportWithException(FILE_1, new SarifDeserializationException(Category.VALUE, "value error", jsonCause));

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig(), analysisWarnings);
    sensor.execute(sensorContext);

    assertThat(logTester.logs(Level.WARN))
      .contains("File path/to/sarif/file.sarif could not be parsed, 0 runs imported (value overflow or type mismatch).");
    verify(analysisWarnings).addUnique(SarifIssuesImportSensor.SARIF_IMPORT_ANALYSIS_WARNING);
  }

  @Test
  public void execute_whenMappingExceptionWithNonJsonCause_shouldLogWithoutLineNumber() {
    sensorSettings.setProperty("sonar.sarifReportPaths", FILE_1);

    failDeserializingReportWithException(FILE_1, new SarifDeserializationException(Category.MAPPING, "mapping error", new RuntimeException("bad data")));

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig(), analysisWarnings);
    sensor.execute(sensorContext);

    assertThat(logTester.logs(Level.WARN))
      .contains("File path/to/sarif/file.sarif could not be parsed, 0 runs imported (cannot map to SARIF schema).");
    verify(analysisWarnings).addUnique(SarifIssuesImportSensor.SARIF_IMPORT_ANALYSIS_WARNING);
  }

  private void failDeserializingReport(String path) {
    Path reportFilePath = sensorContext.fileSystem().resolvePath(path).toPath();
    when(sarifSerializer.deserialize(reportFilePath)).thenThrow(new NullPointerException("deserialization failed"));
  }

  private void failDeserializingReportWithException(String path, Exception exception) {
    Path reportFilePath = sensorContext.fileSystem().resolvePath(path).toPath();
    when(sarifSerializer.deserialize(reportFilePath)).thenThrow(exception);
  }

  private ReportAndResults mockSuccessfulReportAndResults(String path) {
    SarifSchema210 report = mockSarifReport(path);

    SarifImportResults sarifImportResults = mock(SarifImportResults.class);
    when(sarifImportResults.getSuccessFullyImportedIssues()).thenReturn(10);
    when(sarifImportResults.getSuccessFullyImportedRuns()).thenReturn(3);
    when(sarifImportResults.getFailedRuns()).thenReturn(0);

    when(sarifImporter.importSarif(report)).thenReturn(sarifImportResults);
    return new ReportAndResults(report, sarifImportResults);
  }

  private SarifSchema210 mockSarifReport(String path) {
    SarifSchema210 report = mock(SarifSchema210.class);
    Path reportFilePath = sensorContext.fileSystem().resolvePath(path).toPath();
    when(sarifSerializer.deserialize(reportFilePath)).thenReturn(report);
    return report;
  }

  private ReportAndResults mockFailedReportAndResults(String path) {
    SarifSchema210 report = mockSarifReport(path);

    SarifImportResults sarifImportResults = mock(SarifImportResults.class);
    when(sarifImportResults.getSuccessFullyImportedRuns()).thenReturn(0);
    when(sarifImportResults.getFailedRuns()).thenReturn(1);

    when(sarifImporter.importSarif(report)).thenReturn(sarifImportResults);
    return new ReportAndResults(report, sarifImportResults);
  }

  private ReportAndResults mockMixedReportAndResults(String path) {
    SarifSchema210 report = mockSarifReport(path);

    SarifImportResults sarifImportResults = mock(SarifImportResults.class);
    when(sarifImportResults.getSuccessFullyImportedIssues()).thenReturn(10);
    when(sarifImportResults.getSuccessFullyImportedRuns()).thenReturn(3);
    when(sarifImportResults.getFailedRuns()).thenReturn(1);

    when(sarifImporter.importSarif(report)).thenReturn(sarifImportResults);
    return new ReportAndResults(report, sarifImportResults);
  }

  private void assertSummaryIsCorrectlyDisplayedForSuccessfulFile(String filePath, SarifImportResults sarifImportResults) {
    verifyLogContainsLine(Level.INFO, filePath, "File {}: {} run(s) successfully imported ({} vulnerabilities in total).",
      filePath, sarifImportResults.getSuccessFullyImportedRuns(), sarifImportResults.getSuccessFullyImportedIssues());
  }

  private void assertSummaryIsCorrectlyDisplayedForFailedFile(String filePath, SarifImportResults sarifImportResults) {
    verifyLogContainsLine(Level.WARN, filePath, "File {}: {} run(s) could not be imported (see warning above).",
      filePath, sarifImportResults.getFailedRuns());
  }

  private void assertSummaryIsCorrectlyDisplayedForMixedFile(String filePath, SarifImportResults sarifImportResults) {
    verifyLogContainsLine(Level.WARN, filePath,
      "File {}: {} run(s) could not be imported (see warning above) and {} run(s) successfully imported ({} vulnerabilities in total).",
      filePath, sarifImportResults.getFailedRuns(), sarifImportResults.getSuccessFullyImportedRuns(), sarifImportResults.getSuccessFullyImportedIssues());
  }

  private void verifyLogContainsLine(Level level, String filePath, String rawMsg, Object... arguments) {
    LogAndArguments logAndArguments = findLogEntry(level, filePath);
    assertThat(logAndArguments.getRawMsg())
      .isEqualTo(rawMsg);
    assertThat(logAndArguments.getArgs()).isPresent()
      .contains(arguments);
  }

  private LogAndArguments findLogEntry(Level level, String filePath) {
    Optional<LogAndArguments> optLogAndArguments = logTester.getLogs(level).stream()
      .filter(log -> log.getFormattedMsg().contains(filePath))
      .collect(MoreCollectors.toOptional());
    assertThat(optLogAndArguments).as("Log entry missing for file %s", filePath).isPresent();
    return optLogAndArguments.get();
  }

  private static class ReportAndResults {
    private final SarifSchema210 sarifReport;
    private final SarifImportResults sarifImportResults;

    private ReportAndResults(SarifSchema210 sarifReport, SarifImportResults sarifImportResults) {
      this.sarifReport = sarifReport;
      this.sarifImportResults = sarifImportResults;
    }

    private SarifSchema210 getSarifReport() {
      return sarifReport;
    }

    private SarifImportResults getSarifImportResults() {
      return sarifImportResults;
    }
  }
}
