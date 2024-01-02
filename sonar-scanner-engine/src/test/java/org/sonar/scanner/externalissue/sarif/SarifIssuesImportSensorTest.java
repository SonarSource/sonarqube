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
package org.sonar.scanner.externalissue.sarif;

import com.google.common.collect.MoreCollectors;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang.math.RandomUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogAndArguments;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.sarif.Sarif210;
import org.sonar.core.sarif.SarifSerializer;

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

  private MapSettings sensorSettings;

  @Before
  public void before() {
    sensorSettings = new MapSettings();
  }

  @Rule
  public final LogTester logTester = new LogTester();

  private final SensorContextTester sensorContext = SensorContextTester.create(Path.of("."));

  @Test
  public void execute_single_files() {
    sensorSettings.setProperty("sonar.sarifReportPaths", FILE_1);

    ReportAndResults reportAndResults = mockReportAndResults(FILE_1);

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig());
    sensor.execute(sensorContext);

    verify(sarifImporter).importSarif(reportAndResults.getSarifReport());

    assertThat(logTester.logs(LoggerLevel.INFO)).hasSize(1);
    assertSummaryIsCorrectlyDisplayed(FILE_1, reportAndResults.getSarifImportResults());
  }

  @Test
  public void execute_multiple_files() {

    sensorSettings.setProperty("sonar.sarifReportPaths", SARIF_REPORT_PATHS_PARAM);

    ReportAndResults reportAndResults1 = mockReportAndResults(FILE_1);
    ReportAndResults reportAndResults2 = mockReportAndResults(FILE_2);

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig());
    sensor.execute(sensorContext);

    verify(sarifImporter).importSarif(reportAndResults1.getSarifReport());
    verify(sarifImporter).importSarif(reportAndResults2.getSarifReport());

    assertSummaryIsCorrectlyDisplayed(FILE_1, reportAndResults1.getSarifImportResults());
    assertSummaryIsCorrectlyDisplayed(FILE_2, reportAndResults2.getSarifImportResults());
  }

  @Test
  public void skip_report_when_import_fails() {
    sensorSettings.setProperty("sonar.sarifReportPaths", SARIF_REPORT_PATHS_PARAM);

    ReportAndResults reportAndResults1 = mockReportAndResults(FILE_1);
    ReportAndResults reportAndResults2 = mockReportAndResults(FILE_2);

    doThrow(new NullPointerException("import failed")).when(sarifImporter).importSarif(reportAndResults1.getSarifReport());

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig());
    sensor.execute(sensorContext);

    verify(sarifImporter).importSarif(reportAndResults2.getSarifReport());
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Failed to process SARIF report from file 'path/to/sarif/file.sarif', error: 'import failed'");
    assertSummaryIsCorrectlyDisplayed(FILE_2, reportAndResults2.getSarifImportResults());
  }

  @Test
  public void skip_report_when_deserialization_fails() {
    sensorSettings.setProperty("sonar.sarifReportPaths", SARIF_REPORT_PATHS_PARAM);

    failDeserializingReport(FILE_1);
    ReportAndResults reportAndResults2 = mockReportAndResults(FILE_2);

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig());
    sensor.execute(sensorContext);

    verify(sarifImporter).importSarif(reportAndResults2.getSarifReport());
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Failed to process SARIF report from file 'path/to/sarif/file.sarif', error: 'deserialization failed'");
    assertSummaryIsCorrectlyDisplayed(FILE_2, reportAndResults2.getSarifImportResults());
  }

  private void failDeserializingReport(String path) {
    Path reportFilePath = sensorContext.fileSystem().resolvePath(path).toPath();
    when(sarifSerializer.deserialize(reportFilePath)).thenThrow(new NullPointerException("deserialization failed"));
  }

  private ReportAndResults mockReportAndResults(String path) {
    Sarif210 report = mock(Sarif210.class);
    Path reportFilePath = sensorContext.fileSystem().resolvePath(path).toPath();
    when(sarifSerializer.deserialize(reportFilePath)).thenReturn(report);

    SarifImportResults sarifImportResults = mock(SarifImportResults.class);
    when(sarifImportResults.getSuccessFullyImportedIssues()).thenReturn(RandomUtils.nextInt());
    when(sarifImportResults.getSuccessFullyImportedRuns()).thenReturn(RandomUtils.nextInt());
    when(sarifImportResults.getFailedRuns()).thenReturn(RandomUtils.nextInt());

    when(sarifImporter.importSarif(report)).thenReturn(sarifImportResults);
    return new ReportAndResults(report, sarifImportResults);
  }

  private void assertSummaryIsCorrectlyDisplayed(String filePath, SarifImportResults sarifImportResults) {
    LogAndArguments logAndArguments = findLogEntry(filePath);
    assertThat(logAndArguments.getRawMsg()).isEqualTo("File {}: successfully imported {} vulnerabilities spread in {} runs. {} failed run(s).");
    assertThat(logAndArguments.getArgs()).isPresent()
      .contains(new Object[] {filePath, sarifImportResults.getSuccessFullyImportedIssues(), sarifImportResults.getSuccessFullyImportedRuns(), sarifImportResults.getFailedRuns()});
  }

  private LogAndArguments findLogEntry(String filePath) {
    Optional<LogAndArguments> optLogAndArguments = logTester.getLogs(LoggerLevel.INFO).stream()
      .filter(log -> log.getFormattedMsg().contains(filePath))
      .collect(MoreCollectors.toOptional());
    assertThat(optLogAndArguments).as("Log entry missing for file %s", filePath).isPresent();
    return optLogAndArguments.get();
  }

  private static class ReportAndResults {
    private final Sarif210 sarifReport;
    private final SarifImportResults sarifImportResults;

    private ReportAndResults(Sarif210 sarifReport, SarifImportResults sarifImportResults) {
      this.sarifReport = sarifReport;
      this.sarifImportResults = sarifImportResults;
    }

    private Sarif210 getSarifReport() {
      return sarifReport;
    }

    private SarifImportResults getSarifImportResults() {
      return sarifImportResults;
    }
  }
}
