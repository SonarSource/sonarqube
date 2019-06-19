/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.config.DefaultConfiguration;
import org.sonar.scanner.deprecated.test.TestPlanBuilder;

import static org.sonar.api.CoreProperties.CATEGORY_CODE_COVERAGE;

public class GenericTestExecutionSensor implements Sensor {

  private static final Logger LOG = Loggers.get(GenericTestExecutionSensor.class);

  static final String REPORT_PATHS_PROPERTY_KEY = "sonar.testExecutionReportPaths";
  /**
   * @deprecated since 6.2
   */
  @Deprecated
  static final String OLD_UNIT_TEST_REPORT_PATHS_PROPERTY_KEY = "sonar.genericcoverage.unitTestReportPaths";

  private final TestPlanBuilder testPlanBuilder;
  private final DefaultConfiguration configuration;

  public GenericTestExecutionSensor(TestPlanBuilder testPlanBuilder, DefaultConfiguration configuration) {
    this.testPlanBuilder = testPlanBuilder;
    this.configuration = configuration;
  }

  public static List<PropertyDefinition> properties() {
    return Collections.singletonList(

      PropertyDefinition.builder(REPORT_PATHS_PROPERTY_KEY)
        .name("Unit tests results report paths")
        .description("List of comma-separated paths (absolute or relative) containing unit tests results report.")
        .category(CATEGORY_CODE_COVERAGE)
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .deprecatedKey(OLD_UNIT_TEST_REPORT_PATHS_PROPERTY_KEY)
        .build());
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("Generic Test Executions Report")
      .global()
      .onlyWhenConfiguration(conf -> conf.hasKey(REPORT_PATHS_PROPERTY_KEY));
  }

  @Override
  public void execute(SensorContext context) {
    if (configuration.getOriginalProperties().containsKey(OLD_UNIT_TEST_REPORT_PATHS_PROPERTY_KEY)) {
      LOG.warn("Property '{}' is deprecated. Please use '{}' instead.", OLD_UNIT_TEST_REPORT_PATHS_PROPERTY_KEY, REPORT_PATHS_PROPERTY_KEY);
    }

    for (String reportPath : configuration.getStringArray(REPORT_PATHS_PROPERTY_KEY)) {
      File reportFile = context.fileSystem().resolvePath(reportPath);
      LOG.info("Parsing {}", reportFile);
      GenericTestExecutionReportParser parser = new GenericTestExecutionReportParser(testPlanBuilder);
      parser.parse(reportFile, context);
      LOG.info("Imported test execution data for {} files", parser.numberOfMatchedFiles());
      int numberOfUnknownFiles = parser.numberOfUnknownFiles();
      if (numberOfUnknownFiles > 0) {
        LOG.info("Test execution data ignored for {} unknown files, including:\n{}", numberOfUnknownFiles, parser.firstUnknownFiles().stream().collect(Collectors.joining("\n")));
      }
    }

  }

}
