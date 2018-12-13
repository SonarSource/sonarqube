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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Encryption;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.scanner.config.DefaultConfiguration;
import org.sonar.scanner.scan.ProjectConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericCoverageSensorTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void migrateOldProperties() {
    Map<String, String> settings = new HashMap<>();
    settings.put(GenericCoverageSensor.OLD_REPORT_PATH_PROPERTY_KEY, "old.xml");
    settings.put(GenericCoverageSensor.OLD_COVERAGE_REPORT_PATHS_PROPERTY_KEY, "old1.xml,old2.xml");
    settings.put(GenericCoverageSensor.OLD_IT_COVERAGE_REPORT_PATHS_PROPERTY_KEY, "old3.xml,old4.xml,old.xml");
    settings.put(GenericCoverageSensor.OLD_OVERALL_COVERAGE_REPORT_PATHS_PROPERTY_KEY, "old5.xml,old6.xml");

    PropertyDefinitions defs = new PropertyDefinitions(GenericCoverageSensor.properties());
    DefaultConfiguration config = new ProjectConfiguration(defs, new Encryption(null), settings);

    Set<String> reportPaths = new GenericCoverageSensor(config).loadReportPaths();

    assertThat(logTester.logs(LoggerLevel.WARN)).contains(
      "Property 'sonar.genericcoverage.reportPath' is deprecated. Please use 'sonar.coverageReportPaths' instead.",
      "Property 'sonar.genericcoverage.reportPaths' is deprecated. Please use 'sonar.coverageReportPaths' instead.",
      "Property 'sonar.genericcoverage.itReportPaths' is deprecated. Please use 'sonar.coverageReportPaths' instead.",
      "Property 'sonar.genericcoverage.overallReportPaths' is deprecated. Please use 'sonar.coverageReportPaths' instead.");

    assertThat(reportPaths).containsOnly(
      "old.xml", "old1.xml", "old2.xml", "old3.xml", "old4.xml", "old5.xml", "old6.xml");
  }
}
