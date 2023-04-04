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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.utils.System2;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.scanner.config.DefaultConfiguration;
import org.sonar.scanner.scan.ProjectConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

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
}
