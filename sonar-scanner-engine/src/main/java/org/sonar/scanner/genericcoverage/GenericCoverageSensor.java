/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.util.Arrays.asList;
import static org.sonar.api.CoreProperties.CATEGORY_CODE_COVERAGE;

public class GenericCoverageSensor implements Sensor {

  private static final Logger LOG = Loggers.get(GenericCoverageSensor.class);

  static final String REPORT_PATHS_PROPERTY_KEY = "sonar.coverageReportPaths";
  /**
   * @deprecated since 6.2
   */
  @Deprecated
  static final String OLD_REPORT_PATH_PROPERTY_KEY = "sonar.genericcoverage.reportPath";
  /**
   * @deprecated since 6.2
   */
  @Deprecated
  static final String OLD_COVERAGE_REPORT_PATHS_PROPERTY_KEY = "sonar.genericcoverage.reportPaths";
  /**
   * @deprecated since 6.2
   */
  @Deprecated
  static final String OLD_IT_COVERAGE_REPORT_PATHS_PROPERTY_KEY = "sonar.genericcoverage.itReportPaths";
  /**
   * @deprecated since 6.2
   */
  @Deprecated
  static final String OLD_OVERALL_COVERAGE_REPORT_PATHS_PROPERTY_KEY = "sonar.genericcoverage.overallReportPaths";

  private final Configuration config;

  public GenericCoverageSensor(Configuration config) {
    this.config = config;
  }

  public static ImmutableList<PropertyDefinition> properties() {
    return ImmutableList.of(

      PropertyDefinition.builder(REPORT_PATHS_PROPERTY_KEY)
        .name("Coverage report paths")
        .description("List of comma-separated paths (absolute or relative) containing coverage report.")
        .category(CATEGORY_CODE_COVERAGE)
        .onQualifiers(Qualifiers.PROJECT)
        .deprecatedKey(OLD_COVERAGE_REPORT_PATHS_PROPERTY_KEY)
        .build());

  }

  private void loadDeprecated(Set<String> reportPaths, String propertyKey) {
    if (config.hasKey(propertyKey)) {
      LOG.warn("Property '{}' is deprecated. Please use '{}' instead.", propertyKey, REPORT_PATHS_PROPERTY_KEY);
      reportPaths.addAll(Arrays.asList(config.getStringArray(propertyKey)));
    }
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Generic Coverage Report")
      .onlyWhenConfiguration(c -> asList(REPORT_PATHS_PROPERTY_KEY, OLD_REPORT_PATH_PROPERTY_KEY, OLD_COVERAGE_REPORT_PATHS_PROPERTY_KEY,
        OLD_IT_COVERAGE_REPORT_PATHS_PROPERTY_KEY, OLD_OVERALL_COVERAGE_REPORT_PATHS_PROPERTY_KEY)
          .stream()
          .anyMatch(c::hasKey));
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
    Set<String> reportPaths = new LinkedHashSet<>();
    reportPaths.addAll(Arrays.asList(config.getStringArray(REPORT_PATHS_PROPERTY_KEY)));
    loadDeprecated(reportPaths, OLD_REPORT_PATH_PROPERTY_KEY);
    loadDeprecated(reportPaths, OLD_COVERAGE_REPORT_PATHS_PROPERTY_KEY);
    loadDeprecated(reportPaths, OLD_IT_COVERAGE_REPORT_PATHS_PROPERTY_KEY);
    loadDeprecated(reportPaths, OLD_OVERALL_COVERAGE_REPORT_PATHS_PROPERTY_KEY);
    return reportPaths;
  }

}
