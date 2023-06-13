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
package org.sonar.scanner.genericcoverage;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.scanner.sensor.ProjectSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.scanner.config.DefaultConfiguration;

import static org.sonar.api.CoreProperties.CATEGORY_CODE_COVERAGE;

public class GenericCoverageSensor implements ProjectSensor {

  private static final Logger LOG = LoggerFactory.getLogger(GenericCoverageSensor.class);

  static final String REPORT_PATHS_PROPERTY_KEY = "sonar.coverageReportPaths";
  private final DefaultConfiguration config;

  public GenericCoverageSensor(DefaultConfiguration config) {
    this.config = config;
  }

  public static List<PropertyDefinition> properties() {
    return Collections.singletonList(
      PropertyDefinition.builder(REPORT_PATHS_PROPERTY_KEY)
        .name("Coverage report paths")
        .description("List of comma-separated paths (absolute or relative) containing coverage report.")
        .category(CATEGORY_CODE_COVERAGE)
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .build());

  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Generic Coverage Report")
      .onlyWhenConfiguration(c -> c.hasKey(REPORT_PATHS_PROPERTY_KEY));
  }

  @Override
  public void execute(SensorContext context) {
    Set<String> reportPaths = loadReportPaths();

    for (String reportPath : reportPaths) {
      File reportFile = context.fileSystem().resolvePath(reportPath);
      LOG.info("Parsing {}", reportFile);
      GenericCoverageReportParser parser = new GenericCoverageReportParser();
      parser.parse(reportFile, context);
      LOG.info("Imported coverage data for {} files", parser.numberOfMatchedFiles());
      int numberOfUnknownFiles = parser.numberOfUnknownFiles();
      if (numberOfUnknownFiles > 0) {
        LOG.info("Coverage data ignored for " + numberOfUnknownFiles + " unknown files, including:\n" + parser.firstUnknownFiles().stream().collect(Collectors.joining("\n")));
      }
    }

  }

  Set<String> loadReportPaths() {
    return new LinkedHashSet<>(Arrays.asList(config.getStringArray(REPORT_PATHS_PROPERTY_KEY)));
  }

}
