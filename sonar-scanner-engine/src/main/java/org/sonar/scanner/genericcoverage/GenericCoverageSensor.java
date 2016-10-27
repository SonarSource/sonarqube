/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.api.batch.Initializer;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static org.sonar.api.CoreProperties.CATEGORY_CODE_COVERAGE;

public class GenericCoverageSensor extends Initializer implements Sensor {

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

  private final Settings settings;

  public GenericCoverageSensor(Settings settings) {
    this.settings = settings;
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

  /**
   * Use an initializer to migrate old properties to the new one before Sensor phase so that
   * Sensor will not be executed if there is no report (thanks to SensorDescriptor.requireProperty(REPORT_PATH_PROPERTY_KEY))
   */
  @Override
  public void execute() {
    Set<String> reportPaths = new LinkedHashSet<>();
    reportPaths.addAll(Arrays.asList(settings.getStringArray(REPORT_PATHS_PROPERTY_KEY)));
    loadDeprecated(reportPaths, OLD_REPORT_PATH_PROPERTY_KEY);
    loadDeprecated(reportPaths, OLD_COVERAGE_REPORT_PATHS_PROPERTY_KEY);
    loadDeprecated(reportPaths, OLD_IT_COVERAGE_REPORT_PATHS_PROPERTY_KEY);
    loadDeprecated(reportPaths, OLD_OVERALL_COVERAGE_REPORT_PATHS_PROPERTY_KEY);
    if (!reportPaths.isEmpty()) {
      settings.setProperty(REPORT_PATHS_PROPERTY_KEY, reportPaths.stream().collect(Collectors.joining(",")));
    }
  }

  private void loadDeprecated(Set<String> reportPaths, String propertyKey) {
    if (settings.hasKey(propertyKey)) {
      LOG.warn("Property '{}' is deprecated. Please use '{}' instead.", propertyKey, REPORT_PATHS_PROPERTY_KEY);
      reportPaths.addAll(Arrays.asList(settings.getStringArray(propertyKey)));
    }
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Generic Coverage Report")
      .requireProperty(REPORT_PATHS_PROPERTY_KEY);
  }

  @Override
  public void execute(SensorContext context) {
    for (String reportPath : settings.getStringArray(REPORT_PATHS_PROPERTY_KEY)) {
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

}
