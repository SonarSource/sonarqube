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
package org.sonarqube.tests.duplication;

import com.sonar.orchestrator.Orchestrator;
import java.util.Map;
import org.assertj.core.data.Offset;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Measures;

import static java.lang.Double.parseDouble;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getMeasuresWithVariationsByMetricKey;
import static util.ItUtils.runProjectAnalysis;

public class NewDuplicationsTest {

  private static final Offset<Double> DEFAULT_OFFSET = Offset.offset(0.1d);

  @ClassRule
  public static Orchestrator orchestrator = DuplicationSuite.ORCHESTRATOR;

  private static Tester tester = new Tester(orchestrator);

  @ClassRule
  public static RuleChain ruleChain = RuleChain.outerRule(orchestrator).around(tester);

  @BeforeClass
  public static void analyzeProjects() {
    runProjectAnalysis(orchestrator, "duplications/new-duplications-v1",
      "sonar.projectDate", "2015-02-01",
      "sonar.scm.disabled", "false");
    runProjectAnalysis(orchestrator, "duplications/new-duplications-v2",
      "sonar.scm.disabled", "false");
  }

  @Test
  public void new_duplications_on_project() {
    Map<String, Measures.Measure> measures = getMeasures("new-duplications");
    assertThat(parseDouble(measures.get("new_lines").getPeriods().getPeriodsValue(0).getValue())).isEqualTo(83d, DEFAULT_OFFSET);
    assertThat(parseDouble(measures.get("new_duplicated_lines").getPeriods().getPeriodsValue(0).getValue())).isEqualTo(71d, DEFAULT_OFFSET);
    assertThat(parseDouble(measures.get("new_duplicated_lines_density").getPeriods().getPeriodsValue(0).getValue())).isEqualTo(85.5d, DEFAULT_OFFSET);
    assertThat(parseDouble(measures.get("new_duplicated_blocks").getPeriods().getPeriodsValue(0).getValue())).isEqualTo(12d, DEFAULT_OFFSET);
  }

  @Test
  public void new_duplications_on_directory() {
    Map<String, Measures.Measure> measures = getMeasures("new-duplications:src/main/xoo/duplicated_lines_with_other_dir1");
    assertThat(parseDouble(measures.get("new_lines").getPeriods().getPeriodsValue(0).getValue())).isEqualTo(24d, DEFAULT_OFFSET);
    assertThat(parseDouble(measures.get("new_duplicated_lines").getPeriods().getPeriodsValue(0).getValue())).isEqualTo(24d, DEFAULT_OFFSET);
    assertThat(parseDouble(measures.get("new_duplicated_lines_density").getPeriods().getPeriodsValue(0).getValue())).isEqualTo(100d, DEFAULT_OFFSET);
    assertThat(parseDouble(measures.get("new_duplicated_blocks").getPeriods().getPeriodsValue(0).getValue())).isEqualTo(7d, DEFAULT_OFFSET);
  }

  @Test
  public void new_duplications_on_file() {
    Map<String, Measures.Measure> measures = getMeasures("new-duplications:src/main/xoo/duplicated_lines_within_same_file/DuplicatedLinesInSameFile.xoo");
    assertThat(parseDouble(measures.get("new_lines").getPeriods().getPeriodsValue(0).getValue())).isEqualTo(41d, DEFAULT_OFFSET);
    assertThat(parseDouble(measures.get("new_duplicated_lines").getPeriods().getPeriodsValue(0).getValue())).isEqualTo(29d, DEFAULT_OFFSET);
    assertThat(parseDouble(measures.get("new_duplicated_lines_density").getPeriods().getPeriodsValue(0).getValue())).isEqualTo(70.7d, DEFAULT_OFFSET);
    assertThat(parseDouble(measures.get("new_duplicated_blocks").getPeriods().getPeriodsValue(0).getValue())).isEqualTo(2d, DEFAULT_OFFSET);
  }

  private static Map<String, Measures.Measure> getMeasures(String key) {
    return getMeasuresWithVariationsByMetricKey(orchestrator, key, "new_lines", "new_duplicated_lines", "new_duplicated_lines_density", "new_duplicated_blocks");
  }
}
