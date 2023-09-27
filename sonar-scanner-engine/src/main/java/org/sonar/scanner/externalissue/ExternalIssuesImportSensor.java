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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.scanner.ScannerSide;

@ScannerSide
public class ExternalIssuesImportSensor implements Sensor {
  private static final Logger LOG = LoggerFactory.getLogger(ExternalIssuesImportSensor.class);
  private static final String REPORT_PATHS_PROPERTY_KEY = "sonar.externalIssuesReportPaths";
  private final Configuration config;
  private final ExternalIssueReportParser externalIssueReportParser;

  public ExternalIssuesImportSensor(Configuration config, ExternalIssueReportParser externalIssueReportParser) {
    this.config = config;
    this.externalIssueReportParser = externalIssueReportParser;
  }

  public static List<PropertyDefinition> properties() {
    return Collections.singletonList(
      PropertyDefinition.builder(REPORT_PATHS_PROPERTY_KEY)
        .name("Issues report paths")
        .description("List of comma-separated paths (absolute or relative) containing report with issues created by external rule engines.")
        .category(CoreProperties.CATEGORY_EXTERNAL_ISSUES)
        .onQualifiers(Qualifiers.PROJECT)
        .build());
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Import external issues report")
      .onlyWhenConfiguration(c -> c.hasKey(REPORT_PATHS_PROPERTY_KEY));
  }

  @Override
  public void execute(SensorContext context) {
    Set<String> reportPaths = loadReportPaths();
    for (String reportPath : reportPaths) {
      LOG.debug("Importing issues from '{}'", reportPath);
      Path reportFilePath = context.fileSystem().resolvePath(reportPath).toPath();
      ExternalIssueReport report = externalIssueReportParser.parse(reportFilePath);
      ExternalIssueImporter issueImporter = new ExternalIssueImporter(context, report);
      issueImporter.execute();
    }
  }

  private Set<String> loadReportPaths() {
    return Arrays.stream(config.getStringArray(REPORT_PATHS_PROPERTY_KEY)).collect(Collectors.toSet());
  }

}
