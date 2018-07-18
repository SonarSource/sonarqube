/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.tests.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Category3Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.WsProjects.CreateWsResponse.Project;
import org.sonarqube.ws.client.measure.ComponentWsRequest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonarqube.ws.WsMeasures.Measure;
import static util.ItUtils.projectDir;

public class ProjectMeasureTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void project_without_source_but_tests_related_measures() {
    Project project = tester.projects().generate(null);

    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample"),
      "sonar.projectKey", project.getKey(),
      // Exclude all file => no source code
      "sonar.exclusions", "**/*",
      "sonar.measure.valueByMetric", "tests=20;test_errors=1;test_failures=2;skipped_tests=3"));

    assertThat(tester.wsClient().measures().component(
      new ComponentWsRequest()
        .setComponent(project.getKey())
        .setMetricKeys(asList("tests", "test_errors", "test_failures", "skipped_tests")))
      .getComponent().getMeasuresList())
        .extracting(Measure::getMetric, Measure::getValue)
        .containsExactlyInAnyOrder(
          tuple("tests", "20"),
          tuple("test_errors", "1"),
          tuple("test_failures", "2"),
          tuple("skipped_tests", "3"));
  }

  @Test
  public void module_without_source_but_tests_related_measure() {
    Project project = tester.projects().generate(null);

    orchestrator.executeBuild(SonarScanner.create(projectDir("analysis/xoo-module-b-without-source"),
      "sonar.projectKey", project.getKey(),
      "sonar.measure.valueByMetric", "tests=20;test_errors=1;test_failures=2;skipped_tests=3"));

    String moduleBKey = project.getKey() + ":module_b";
    assertThat(tester.wsClient().measures().component(
      new ComponentWsRequest()
        .setComponent(moduleBKey)
        .setMetricKeys(asList("tests", "test_errors", "test_failures", "skipped_tests")))
      .getComponent().getMeasuresList())
        .extracting(Measure::getMetric, Measure::getValue)
        .containsExactlyInAnyOrder(
          tuple("tests", "20"),
          tuple("test_errors", "1"),
          tuple("test_failures", "2"),
          tuple("skipped_tests", "3"));

    assertThat(tester.wsClient().measures().component(
      new ComponentWsRequest()
        .setComponent(project.getKey())
        .setMetricKeys(asList("tests", "test_errors", "test_failures", "skipped_tests")))
      .getComponent().getMeasuresList())
        .extracting(Measure::getMetric, Measure::getValue)
        .containsExactlyInAnyOrder(
          tuple("tests", "20"),
          tuple("test_errors", "1"),
          tuple("test_failures", "2"),
          tuple("skipped_tests", "3"));
  }

  @Test
  public void ignore_measure_injected_at_project_level_when_measure_is_defined_on_file() {
    Project project = tester.projects().generate(null);

    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample-with-tests"),
      "sonar.projectKey", project.getKey(),
      "sonar.measure.valueByMetric", "tests=12"));

    assertThat(tester.wsClient().measures().component(
      new ComponentWsRequest()
        .setComponent(project.getKey())
        .setMetricKeys(singletonList("tests")))
      .getComponent().getMeasuresList())
        .extracting(Measure::getMetric, Measure::getValue)
        // Measure set by the sensor is ignored
        .containsExactlyInAnyOrder(tuple("tests", "2"));
  }
}
