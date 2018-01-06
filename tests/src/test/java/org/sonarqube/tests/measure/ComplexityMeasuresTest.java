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
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sonarqube.qa.util.Tester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static util.ItUtils.getMeasuresAsDoubleByMetricKey;
import static util.ItUtils.projectDir;

// TODO complete the test with other complexity metrics
public class ComplexityMeasuresTest {

  private static final String PROJECT = "com.sonarsource.it.samples:multi-modules-sample";
  private static final String MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a";
  private static final String SUB_MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1";
  private static final String DIRECTORY = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1";
  private static final String FILE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo";
  private static final String COMPLEXITY_METRIC = "complexity";
  private static final String COGNITIVE_COMPLEXITY_METRIC = "cognitive_complexity";

  @ClassRule
  public static Orchestrator orchestrator = MeasureSuite.ORCHESTRATOR;

  private static Tester tester = new Tester(orchestrator);

  @ClassRule
  public static RuleChain ruleChain = RuleChain.outerRule(orchestrator).around(tester);

  @BeforeClass
  public static void inspectProject() {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample")));
  }

  @Test
  public void compute_complexity_metrics_on_file() {
    assertThat(getMeasuresAsDoubleByMetricKey(orchestrator, FILE, COMPLEXITY_METRIC, COGNITIVE_COMPLEXITY_METRIC)).containsOnly(
      entry(COMPLEXITY_METRIC, 3d),
      entry(COGNITIVE_COMPLEXITY_METRIC, 4d));
  }

  @Test
  public void compute_complexity_metrics_on_directory() {
    assertThat(getMeasuresAsDoubleByMetricKey(orchestrator, DIRECTORY, COMPLEXITY_METRIC, COGNITIVE_COMPLEXITY_METRIC)).containsOnly(
      entry(COMPLEXITY_METRIC, 3d),
      entry(COGNITIVE_COMPLEXITY_METRIC, 4d));
  }

  @Test
  public void compute_complexity_metrics_on_sub_module() {
    assertThat(getMeasuresAsDoubleByMetricKey(orchestrator, SUB_MODULE, COMPLEXITY_METRIC, COGNITIVE_COMPLEXITY_METRIC)).containsOnly(
      entry(COMPLEXITY_METRIC, 3d),
      entry(COGNITIVE_COMPLEXITY_METRIC, 4d));
  }

  @Test
  public void compute_complexity_metrics_on_module() {
    assertThat(getMeasuresAsDoubleByMetricKey(orchestrator, MODULE, COMPLEXITY_METRIC, COGNITIVE_COMPLEXITY_METRIC)).containsOnly(
      entry(COMPLEXITY_METRIC, 7d),
      entry(COGNITIVE_COMPLEXITY_METRIC, 9d));
  }

  @Test
  public void compute_complexity_metrics_on_project() {
    assertThat(getMeasuresAsDoubleByMetricKey(orchestrator, PROJECT, COMPLEXITY_METRIC, COGNITIVE_COMPLEXITY_METRIC)).containsOnly(
      entry(COMPLEXITY_METRIC, 13d),
      entry(COGNITIVE_COMPLEXITY_METRIC, 17d));
  }

}
