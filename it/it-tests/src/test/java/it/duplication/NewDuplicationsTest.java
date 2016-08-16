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
package it.duplication;

import com.sonar.orchestrator.Orchestrator;
import it.Category4Suite;
import org.assertj.core.data.Offset;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.runProjectAnalysis;

public class NewDuplicationsTest {

  static final Offset<Double> DEFAULT_OFFSET = Offset.offset(0.1d);

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @BeforeClass
  public static void analyzeProjects() {
    orchestrator.resetData();

    runProjectAnalysis(orchestrator, "duplications/new-duplications-v1",
      "sonar.projectDate", "2015-02-01",
      "sonar.scm.disabled", "false");
    runProjectAnalysis(orchestrator, "duplications/new-duplications-v2",
      "sonar.scm.disabled", "false");
  }

  @Test
  public void new_duplications_on_project() throws Exception {
    Resource project = getComponent("new-duplications");
    assertThat(project.getMeasure("new_lines").getVariation1()).isEqualTo(83d, DEFAULT_OFFSET);
    assertThat(project.getMeasure("new_duplicated_lines").getVariation1()).isEqualTo(71d, DEFAULT_OFFSET);
    assertThat(project.getMeasure("new_duplicated_lines_density").getVariation1()).isEqualTo(85.5d, DEFAULT_OFFSET);
    assertThat(project.getMeasure("new_duplicated_blocks").getVariation1()).isEqualTo(12d, DEFAULT_OFFSET);
  }

  @Test
  public void new_duplications_on_directory() throws Exception {
    Resource project = getComponent("new-duplications:src/main/xoo/duplicated_lines_with_other_dir1");
    assertThat(project.getMeasure("new_lines").getVariation1()).isEqualTo(24d, DEFAULT_OFFSET);
    assertThat(project.getMeasure("new_duplicated_lines").getVariation1()).isEqualTo(24d, DEFAULT_OFFSET);
    assertThat(project.getMeasure("new_duplicated_lines_density").getVariation1()).isEqualTo(100d, DEFAULT_OFFSET);
    assertThat(project.getMeasure("new_duplicated_blocks").getVariation1()).isEqualTo(7d, DEFAULT_OFFSET);
  }

  @Test
  public void new_duplications_on_file() throws Exception {
    Resource project = getComponent("new-duplications:src/main/xoo/duplicated_lines_within_same_file/DuplicatedLinesInSameFile.xoo");
    assertThat(project.getMeasure("new_lines").getVariation1()).isEqualTo(41d, DEFAULT_OFFSET);
    assertThat(project.getMeasure("new_duplicated_lines").getVariation1()).isEqualTo(29d, DEFAULT_OFFSET);
    assertThat(project.getMeasure("new_duplicated_lines_density").getVariation1()).isEqualTo(70.7d, DEFAULT_OFFSET);
    assertThat(project.getMeasure("new_duplicated_blocks").getVariation1()).isEqualTo(2d, DEFAULT_OFFSET);
  }

  private static Resource getComponent(String key) {
    Resource component = orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics(key, "new_lines", "new_duplicated_lines", "new_duplicated_lines_density", "new_duplicated_blocks").setIncludeTrends(true));
    assertThat(component).isNotNull();
    return component;
  }
}
