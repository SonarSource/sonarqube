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
package org.sonar.scanner.externalissue.sarif;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.scanner.sensor.ProjectSensor;
import org.sonar.api.utils.MessageException;
import org.sonar.core.sarif.Sarif210;
import org.sonar.core.sarif.SarifSerializer;

import static java.lang.String.format;

@ScannerSide
public class SarifIssuesImportSensor implements ProjectSensor {

  private static final Logger LOG = LoggerFactory.getLogger(SarifIssuesImportSensor.class);
  static final String SARIF_REPORT_PATHS_PROPERTY_KEY = "sonar.sarifReportPaths";

  private final SarifSerializer sarifSerializer;
  private final Sarif210Importer sarifImporter;
  private final Configuration config;

  public SarifIssuesImportSensor(SarifSerializer sarifSerializer, Sarif210Importer sarifImporter, Configuration config) {
    this.sarifSerializer = sarifSerializer;
    this.sarifImporter = sarifImporter;
    this.config = config;
  }

  public static List<PropertyDefinition> properties() {
    return Collections.singletonList(
      PropertyDefinition.builder(SARIF_REPORT_PATHS_PROPERTY_KEY)
        .name("SARIF report paths")
        .description("List of comma-separated paths (absolute or relative) containing a SARIF report with issues created by external rule engines.")
        .category(CoreProperties.CATEGORY_EXTERNAL_ISSUES)
        .onQualifiers(Qualifiers.PROJECT)
        .build());
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Import external issues report from SARIF file.")
      .onlyWhenConfiguration(c -> c.hasKey(SARIF_REPORT_PATHS_PROPERTY_KEY));
  }

  @Override
  public void execute(SensorContext context) {
    Set<String> reportPaths = loadReportPaths();
    Map<String, SarifImportResults> filePathToImportResults = new HashMap<>();

    for (String reportPath : reportPaths) {
      try {
        SarifImportResults sarifImportResults = processReport(context, reportPath);
        filePathToImportResults.put(reportPath, sarifImportResults);
      } catch (NoSuchFileException e) {
        throw MessageException.of(format("SARIF report file not found: %s", e.getFile()));
      } catch (Exception exception) {
        LOG.warn("Failed to process SARIF report from file '{}', error: '{}'", reportPath, exception.getMessage());
      }
    }
    filePathToImportResults.forEach(SarifIssuesImportSensor::displayResults);
  }

  private Set<String> loadReportPaths() {
    return Arrays.stream(config.getStringArray(SARIF_REPORT_PATHS_PROPERTY_KEY)).collect(Collectors.toSet());
  }

  private SarifImportResults processReport(SensorContext context, String reportPath) throws NoSuchFileException {
    LOG.debug("Importing SARIF issues from '{}'", reportPath);
    Path reportFilePath = context.fileSystem().resolvePath(reportPath).toPath();
    Sarif210 sarifReport = sarifSerializer.deserialize(reportFilePath);
    return sarifImporter.importSarif(sarifReport);
  }

  private static void displayResults(String filePath, SarifImportResults sarifImportResults) {
    if (sarifImportResults.getFailedRuns() > 0 && sarifImportResults.getSuccessFullyImportedRuns() > 0) {
      LOG.warn("File {}: {} run(s) could not be imported (see warning above) and {} run(s) successfully imported ({} vulnerabilities in total).",
        filePath, sarifImportResults.getFailedRuns(), sarifImportResults.getSuccessFullyImportedRuns(), sarifImportResults.getSuccessFullyImportedIssues());

    } else if (sarifImportResults.getFailedRuns() > 0) {
      LOG.warn("File {}: {} run(s) could not be imported (see warning above).",
        filePath, sarifImportResults.getFailedRuns());
    } else {
      LOG.info("File {}: {} run(s) successfully imported ({} vulnerabilities in total).",
        filePath, sarifImportResults.getSuccessFullyImportedRuns(), sarifImportResults.getSuccessFullyImportedIssues());
    }
  }
}
