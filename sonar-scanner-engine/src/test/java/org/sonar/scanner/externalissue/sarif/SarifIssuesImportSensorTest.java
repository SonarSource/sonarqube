/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.sarif.Sarif210;
import org.sonar.core.sarif.SarifSerializer;

import static org.mockito.Mockito.doThrow;
import static org.assertj.core.api.Assertions.assertThat;
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
  public LogTester logTester = new LogTester();

  private SensorContextTester sensorContext = SensorContextTester.create(Path.of("."));

  @Test
  public void execute_single_files() {
    sensorSettings.setProperty("sonar.sarifReportPaths", FILE_1);

    Sarif210 sarifReport = mockReport(FILE_1);

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig());
    sensor.execute(sensorContext);

    verify(sarifImporter).importSarif(sarifReport);
  }

  @Test
  public void execute_multiple_files() {

    sensorSettings.setProperty("sonar.sarifReportPaths", SARIF_REPORT_PATHS_PARAM);

    Sarif210 sarifReport1 = mockReport(FILE_1);
    Sarif210 sarifReport2 = mockReport(FILE_2);

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig());
    sensor.execute(sensorContext);

    verify(sarifImporter).importSarif(sarifReport1);
    verify(sarifImporter).importSarif(sarifReport2);
  }

  @Test
  public void skip_report_when_import_fails() {
    sensorSettings.setProperty("sonar.sarifReportPaths", SARIF_REPORT_PATHS_PARAM);

    Sarif210 sarifReport1 = mockReport(FILE_1);
    Sarif210 sarifReport2 = mockReport(FILE_2);

    doThrow(new NullPointerException("import failed")).when(sarifImporter).importSarif(sarifReport1);

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig());
    sensor.execute(sensorContext);

    verify(sarifImporter).importSarif(sarifReport2);
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Failed to process SARIF report from file 'path/to/sarif/file.sarif', error 'import failed'");
  }

  @Test
  public void skip_report_when_deserialization_fails() {
    sensorSettings.setProperty("sonar.sarifReportPaths", SARIF_REPORT_PATHS_PARAM);

    failDeserializingReport(FILE_1);
    Sarif210 sarifReport2 = mockReport(FILE_2);

    SarifIssuesImportSensor sensor = new SarifIssuesImportSensor(sarifSerializer, sarifImporter, sensorSettings.asConfig());
    sensor.execute(sensorContext);

    verify(sarifImporter).importSarif(sarifReport2);
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Failed to process SARIF report from file 'path/to/sarif/file.sarif', error 'deserialization failed'");

  }

  private Sarif210 mockReport(String path) {
    Sarif210 report = mock(Sarif210.class);
    Path reportFilePath = sensorContext.fileSystem().resolvePath(path).toPath();
    when(sarifSerializer.deserialize(reportFilePath)).thenReturn(report);
    return report;
  }

  private void failDeserializingReport(String path) {
    Path reportFilePath = sensorContext.fileSystem().resolvePath(path).toPath();
    when(sarifSerializer.deserialize(reportFilePath)).thenThrow(new NullPointerException("deserialization failed"));
  }
}
