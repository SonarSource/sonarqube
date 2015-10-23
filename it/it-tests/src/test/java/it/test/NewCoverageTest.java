/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package it.test;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import it.Category2Suite;
import org.assertj.core.data.Offset;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class NewCoverageTest {

  @ClassRule
  public static Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;

  private static final String PROJECT_KEY = "sample-new-coverage";

  private static final Offset<Double> DEFAULT_OFFSET = Offset.offset(0.1d);

  private static final String[] ALL_NEW_COVERAGE_METRICS = new String[] {
    "new_coverage", "new_line_coverage", "new_branch_coverage",
    "new_it_coverage", "new_it_line_coverage", "new_it_branch_coverage",
    "new_overall_coverage", "new_overall_line_coverage", "new_overall_branch_coverage"
  };

  @BeforeClass
  public static void analyze_project() {
    orchestrator.resetData();

    orchestrator.executeBuilds(SonarRunner.create(projectDir("testing/xoo-sample-new-coverage-v1"))
      .setProperty("sonar.projectDate", "2015-02-01")
      .setProperty("sonar.scm.disabled", "false")
      );
    orchestrator.executeBuilds(SonarRunner.create(projectDir("testing/xoo-sample-new-coverage-v2"))
      .setProperty("sonar.scm.disabled", "false"));
  }

  @Test
  public void new_unit_test_coverage() throws Exception {
    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT_KEY, ALL_NEW_COVERAGE_METRICS).setIncludeTrends(true));
    assertThat(project.getMeasure("new_coverage").getVariation1()).isEqualTo(62.5d, DEFAULT_OFFSET);
    assertThat(project.getMeasure("new_line_coverage").getVariation1()).isEqualTo(80d, DEFAULT_OFFSET);
    assertThat(project.getMeasure("new_branch_coverage").getVariation1()).isEqualTo(33.3, DEFAULT_OFFSET);
  }

  @Test
  public void new_integration_test_coverage() throws Exception {
    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT_KEY, ALL_NEW_COVERAGE_METRICS).setIncludeTrends(true));
    assertThat(project.getMeasure("new_it_coverage").getVariation1()).isEqualTo(85.7, DEFAULT_OFFSET);
    assertThat(project.getMeasure("new_it_line_coverage").getVariation1()).isEqualTo(100d, DEFAULT_OFFSET);
    assertThat(project.getMeasure("new_it_branch_coverage").getVariation1()).isEqualTo(66.7, DEFAULT_OFFSET);
  }

  @Test
  public void new_overall_coverage() throws Exception {
    Resource project = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT_KEY, ALL_NEW_COVERAGE_METRICS).setIncludeTrends(true));
    assertThat(project.getMeasure("new_overall_coverage").getVariation1()).isEqualTo(44.4d, DEFAULT_OFFSET);
    assertThat(project.getMeasure("new_overall_line_coverage").getVariation1()).isEqualTo(50d, DEFAULT_OFFSET);
    assertThat(project.getMeasure("new_overall_branch_coverage").getVariation1()).isEqualTo(42.85, DEFAULT_OFFSET);
  }

}
