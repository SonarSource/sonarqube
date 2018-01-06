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
public class MaintainabilityMeasureTest {

  private static final String PROJECT = "com.sonarsource.it.samples:multi-modules-sample";
  private static final String MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a";
  private static final String SUB_MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1";
  private static final String DIRECTORY = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1";
  private static final String FILE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo";

  private static final String CODE_SMELLS_METRIC = "code_smells";
  private static final String MAINTAINABILITY_REMEDIATION_EFFORT_METRIC = "sqale_index";
  private static final String MAINTAINABILITY_RATING_METRIC = "sqale_rating";
  private static final String EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_METRIC = "effort_to_reach_maintainability_rating_a";

  @ClassRule
  public static Orchestrator orchestrator = QualityModelSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Before
  public void init() {
    // Set rating grid values to not depend from default value
    tester.qModel().updateRatingGrid(0.1d, 0.2d, 0.5d, 1d);

    orchestrator.getServer().provisionProject(PROJECT, PROJECT);
  }

  @Test
  public void verify_maintainability_measures_when_code_smells_rules_activated() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/qualityModel/with-many-rules.xml"));
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "with-many-rules");
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample")));

    assertThat(getMeasureAsDouble(orchestrator, PROJECT, CODE_SMELLS_METRIC)).isEqualTo(71);
    assertThat(getMeasureAsDouble(orchestrator, PROJECT, MAINTAINABILITY_REMEDIATION_EFFORT_METRIC)).isEqualTo(445);
    assertThat(getMeasureAsDouble(orchestrator, PROJECT, MAINTAINABILITY_RATING_METRIC)).isEqualTo(3);
    assertThat(getMeasureAsDouble(orchestrator, PROJECT, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_METRIC)).isEqualTo(292);

    assertThat(getMeasureAsDouble(orchestrator, MODULE, CODE_SMELLS_METRIC)).isEqualTo(43);
    assertThat(getMeasureAsDouble(orchestrator, MODULE, MAINTAINABILITY_REMEDIATION_EFFORT_METRIC)).isEqualTo(231);
    assertThat(getMeasureAsDouble(orchestrator, MODULE, MAINTAINABILITY_RATING_METRIC)).isEqualTo(3);
    assertThat(getMeasureAsDouble(orchestrator, MODULE, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_METRIC)).isEqualTo(150);

    assertThat(getMeasureAsDouble(orchestrator, SUB_MODULE, CODE_SMELLS_METRIC)).isEqualTo(19);
    assertThat(getMeasureAsDouble(orchestrator, SUB_MODULE, MAINTAINABILITY_REMEDIATION_EFFORT_METRIC)).isEqualTo(113);
    assertThat(getMeasureAsDouble(orchestrator, SUB_MODULE, MAINTAINABILITY_RATING_METRIC)).isEqualTo(3);
    assertThat(getMeasureAsDouble(orchestrator, SUB_MODULE, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_METRIC)).isEqualTo(77);

    assertThat(getMeasureAsDouble(orchestrator, DIRECTORY, CODE_SMELLS_METRIC)).isEqualTo(18);
    assertThat(getMeasureAsDouble(orchestrator, DIRECTORY, MAINTAINABILITY_REMEDIATION_EFFORT_METRIC)).isEqualTo(28);
    assertThat(getMeasureAsDouble(orchestrator, DIRECTORY, MAINTAINABILITY_RATING_METRIC)).isEqualTo(1);
    assertThat(getMeasureAsDouble(orchestrator, DIRECTORY, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_METRIC)).isZero();

    assertThat(getMeasureAsDouble(orchestrator, FILE, CODE_SMELLS_METRIC)).isEqualTo(18);
    assertThat(getMeasureAsDouble(orchestrator, FILE, MAINTAINABILITY_REMEDIATION_EFFORT_METRIC)).isEqualTo(28);
    assertThat(getMeasureAsDouble(orchestrator, FILE, MAINTAINABILITY_RATING_METRIC)).isEqualTo(1);
    assertThat(getMeasureAsDouble(orchestrator, FILE, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_METRIC)).isZero();
  }

  @Test
  public void verify_reliability_measures_when_no_code_smells_rule() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/qualityModel/without-type-code-smells.xml"));
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "without-type-code-smells");
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample")));

    assertThat(getMeasureAsDouble(orchestrator, PROJECT, CODE_SMELLS_METRIC)).isZero();
    assertThat(getMeasureAsDouble(orchestrator, PROJECT, MAINTAINABILITY_REMEDIATION_EFFORT_METRIC)).isZero();
    assertThat(getMeasureAsDouble(orchestrator, PROJECT, MAINTAINABILITY_RATING_METRIC)).isEqualTo(1);
    assertThat(getMeasureAsDouble(orchestrator, PROJECT, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_METRIC)).isZero();
  }

}
