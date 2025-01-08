/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.scanner.genericcoverage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.scanner.config.DefaultConfiguration;
import org.sonar.scanner.scan.ProjectConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GenericCoverageSensorTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void loadAllReportPaths() {
    Map<String, String> settings = new HashMap<>();
    settings.put(GenericCoverageSensor.REPORT_PATHS_PROPERTY_KEY, "report.xml,report2.xml");
    PropertyDefinitions defs = new PropertyDefinitions(System2.INSTANCE, GenericCoverageSensor.properties());
    DefaultConfiguration config = new ProjectConfiguration(defs, new Encryption(null), settings);

    Set<String> reportPaths = new GenericCoverageSensor(config).loadReportPaths();

    assertThat(reportPaths).containsOnly("report.xml", "report2.xml");
  }

  @Test
  public void should_log_info_message_when_unknown_files_exist() throws Exception {
    logTester.setLevel(Level.INFO);
    DefaultConfiguration config = mock(DefaultConfiguration.class);

    // Create a temporary file to simulate the report file
    Path tempReportFile = Files.createTempFile("report", ".xml");
    // Write valid content with version information to the temporary file
    Files.write(tempReportFile, "<coverage version=\"1\"><file path=\"unknownFile\"/></coverage>".getBytes());

    // Use the temporary file path in the configuration mock
    when(config.getStringArray(GenericCoverageSensor.REPORT_PATHS_PROPERTY_KEY)).thenReturn(new String[] {tempReportFile.toString()});

    GenericCoverageSensor sensor = new GenericCoverageSensor(config);
    SensorContextTester context = SensorContextTester.create(new File("."));
    DefaultFileSystem fileSystem = context.fileSystem();

    // Mock the input file instead of using DefaultInputFile
    InputFile inputFile = mock(InputFile.class);
    when(inputFile.language()).thenReturn("java");
    when(inputFile.type()).thenReturn(InputFile.Type.MAIN);
    when(inputFile.filename()).thenReturn("unknownFile");

    fileSystem.add(inputFile);

    sensor.execute(context);

    assertThat(logTester.logs(Level.INFO)).contains("Coverage data ignored for 1 unknown files, including:\nunknownFile");

    // Clean up the temporary file
    Files.delete(tempReportFile);
  }

}
