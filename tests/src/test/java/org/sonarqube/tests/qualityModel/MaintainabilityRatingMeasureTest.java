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
package org.sonarqube.tests.qualityModel;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getMeasureAsDouble;
import static util.ItUtils.projectDir;

/**
 * SONAR-4715
 */
public class MaintainabilityRatingMeasureTest {

  private static final String PROJECT = "com.sonarsource.it.samples:multi-modules-sample";
  private static final String MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a";
  private static final String SUB_MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1";
  private static final String DIRECTORY = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1";
  private static final String FILE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo";

  @ClassRule
  public static Orchestrator orchestrator = QualityModelSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Before
  public void init() {
    // Set rating grid values to not depend from default value
    tester.qModel().updateRatingGrid(0.1d, 0.2d, 0.5d, 1d);
  }

  @Test
  public void sqale_rating_measures() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/qualityModel/with-many-rules.xml"));
    orchestrator.getServer().provisionProject(PROJECT, PROJECT);
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "with-many-rules");

    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample")));

    assertThat(getMeasureAsDouble(orchestrator, PROJECT, "sqale_rating")).isEqualTo(3);
    assertThat(getMeasureAsDouble(orchestrator, MODULE, "sqale_rating")).isEqualTo(3);
    assertThat(getMeasureAsDouble(orchestrator, SUB_MODULE, "sqale_rating")).isEqualTo(3);
    assertThat(getMeasureAsDouble(orchestrator, DIRECTORY, "sqale_rating")).isEqualTo(1);
    assertThat(getMeasureAsDouble(orchestrator, FILE, "sqale_rating")).isEqualTo(1);
  }

  @Test
  public void sqale_debt_ratio_measures() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/qualityModel/with-many-rules.xml"));
    orchestrator.getServer().provisionProject(PROJECT, PROJECT);
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "with-many-rules");

    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample")));

    assertThat(getMeasureAsDouble(orchestrator, PROJECT, "sqale_debt_ratio")).isEqualTo(29.1d);
    assertThat(getMeasureAsDouble(orchestrator, MODULE, "sqale_debt_ratio")).isEqualTo(28.5d);
    assertThat(getMeasureAsDouble(orchestrator, SUB_MODULE, "sqale_debt_ratio")).isEqualTo(31.4d);
    assertThat(getMeasureAsDouble(orchestrator, DIRECTORY, "sqale_debt_ratio")).isEqualTo(7.8d);
    assertThat(getMeasureAsDouble(orchestrator, FILE, "sqale_debt_ratio")).isEqualTo(7.8d);
  }

  @Test
  public void use_development_cost_parameter() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/qualityModel/one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject("sample", "sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));

    assertThat(getMeasureAsDouble(orchestrator, "sample", "sqale_rating")).isEqualTo(1);

    tester.qModel().updateDevelopmentCost(2);
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));

    assertThat(getMeasureAsDouble(orchestrator, "sample", "sqale_rating")).isEqualTo(4);
  }

  @Test
  public void use_language_specific_parameters() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/qualityModel/one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject(PROJECT, PROJECT);
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "one-issue-per-line");

    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample")));

    assertThat(getMeasureAsDouble(orchestrator, PROJECT, "sqale_rating")).isEqualTo(1);

    tester.qModel().updateLanguageDevelopmentCost("xoo", 1);
    orchestrator.executeBuild(
      SonarScanner.create(projectDir("shared/xoo-multi-modules-sample"))
        .setProfile("one-issue-per-line"));

    assertThat(getMeasureAsDouble(orchestrator, PROJECT, "sqale_rating")).isEqualTo(5);
  }

  @Test
  public void use_rating_grid_parameter() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/qualityModel/one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject("sample", "sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));

    assertThat(getMeasureAsDouble(orchestrator, "sample", "sqale_rating")).isEqualTo(1);

    tester.qModel().updateRatingGrid(0.001d, 0.005d, 0.01d, 0.015d);
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));

    assertThat(getMeasureAsDouble(orchestrator, "sample", "sqale_rating")).isEqualTo(5);
  }

  @Test
  public void effort_to_reach_maintainability_rating_a() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/qualityModel/with-many-rules.xml"));
    orchestrator.getServer().provisionProject(PROJECT, PROJECT);
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "with-many-rules");

    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample")));

    assertThat(getMeasureAsDouble(orchestrator, PROJECT, "sqale_rating")).isEqualTo(3);
    assertThat(getMeasureAsDouble(orchestrator, PROJECT, "effort_to_reach_maintainability_rating_a")).isEqualTo(292);

    assertThat(getMeasureAsDouble(orchestrator, MODULE, "sqale_rating")).isEqualTo(3);
    assertThat(getMeasureAsDouble(orchestrator, MODULE, "effort_to_reach_maintainability_rating_a")).isEqualTo(150);

    assertThat(getMeasureAsDouble(orchestrator, SUB_MODULE, "sqale_rating")).isEqualTo(3);
    assertThat(getMeasureAsDouble(orchestrator, SUB_MODULE, "effort_to_reach_maintainability_rating_a")).isEqualTo(77);

    assertThat(getMeasureAsDouble(orchestrator, DIRECTORY, "sqale_rating")).isEqualTo(1);
    assertThat(getMeasureAsDouble(orchestrator, DIRECTORY, "effort_to_reach_maintainability_rating_a")).isZero();

    assertThat(getMeasureAsDouble(orchestrator, FILE, "sqale_rating")).isEqualTo(1);
    assertThat(getMeasureAsDouble(orchestrator, FILE, "effort_to_reach_maintainability_rating_a")).isZero();
  }

}
