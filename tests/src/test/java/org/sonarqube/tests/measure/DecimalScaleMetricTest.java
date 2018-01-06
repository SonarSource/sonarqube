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
package org.sonarqube.tests.measure;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getMeasureAsDouble;

/**
 * SONAR-6939
 */
public class DecimalScaleMetricTest {

  /**
   * Requires the plugin "batch-plugin" 
   */
  @ClassRule
  public static Orchestrator orchestrator = MeasureSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void override_decimal_scale_of_numeric_metric() {
    String projectKey = "DecimalScaleMetricTest.override_decimal_scale_of_numeric_metric";
    // see DecimalScaleMetric
    String metricKey = "decimal_scale";
    ItUtils.runProjectAnalysis(orchestrator, "shared/xoo-sample",
      "sonar.projectKey", projectKey,
      "sonar.scanner.feedDecimalScaleMetric", String.valueOf(true));

    assertThat(getMeasureAsDouble(orchestrator, projectKey, metricKey)).isEqualTo(0.0001);
  }
}
