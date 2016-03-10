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
package it.qualityModel;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import it.Category2Suite;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.assertj.core.api.Assertions.assertThat;
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
  public static Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;

  @Rule
  public DebtConfigurationRule debtConfiguration = DebtConfigurationRule.create(orchestrator);

  @Before
  public void init() {
    orchestrator.resetData();

    // Set rating grid values to not depend from default value
    debtConfiguration.updateRatingGrid(0.1d, 0.2d, 0.5d, 1d);

    orchestrator.getServer().provisionProject(PROJECT, PROJECT);
  }

  @Test
  public void verify_maintainability_measures_when_code_smells_rules_activated() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/qualityModel/with-many-rules.xml"));
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "with-many-rules");
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample")));

    assertThat(getMeasure(PROJECT, CODE_SMELLS_METRIC).getValue()).isEqualTo(71);
    assertThat(getMeasure(PROJECT, MAINTAINABILITY_REMEDIATION_EFFORT_METRIC).getValue()).isEqualTo(445);
    assertThat(getMeasure(PROJECT, MAINTAINABILITY_RATING_METRIC).getData()).isEqualTo("C");
    assertThat(getMeasure(PROJECT, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_METRIC).getValue()).isEqualTo(292);

    assertThat(getMeasure(MODULE, CODE_SMELLS_METRIC).getValue()).isEqualTo(43);
    assertThat(getMeasure(MODULE, MAINTAINABILITY_REMEDIATION_EFFORT_METRIC).getValue()).isEqualTo(231);
    assertThat(getMeasure(MODULE, MAINTAINABILITY_RATING_METRIC).getData()).isEqualTo("C");
    assertThat(getMeasure(MODULE, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_METRIC).getValue()).isEqualTo(150);

    assertThat(getMeasure(SUB_MODULE, CODE_SMELLS_METRIC).getValue()).isEqualTo(19);
    assertThat(getMeasure(SUB_MODULE, MAINTAINABILITY_REMEDIATION_EFFORT_METRIC).getValue()).isEqualTo(113);
    assertThat(getMeasure(SUB_MODULE, MAINTAINABILITY_RATING_METRIC).getData()).isEqualTo("C");
    assertThat(getMeasure(SUB_MODULE, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_METRIC).getValue()).isEqualTo(77);

    assertThat(getMeasure(DIRECTORY, CODE_SMELLS_METRIC).getValue()).isEqualTo(18);
    assertThat(getMeasure(DIRECTORY, MAINTAINABILITY_REMEDIATION_EFFORT_METRIC).getValue()).isEqualTo(28);
    assertThat(getMeasure(DIRECTORY, MAINTAINABILITY_RATING_METRIC).getData()).isEqualTo("A");
    assertThat(getMeasure(DIRECTORY, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_METRIC).getValue()).isEqualTo(0);

    assertThat(getMeasure(FILE, CODE_SMELLS_METRIC).getValue()).isEqualTo(18);
    assertThat(getMeasure(FILE, MAINTAINABILITY_REMEDIATION_EFFORT_METRIC).getValue()).isEqualTo(28);
    assertThat(getMeasure(FILE, MAINTAINABILITY_RATING_METRIC).getData()).isEqualTo("A");
    assertThat(getMeasure(FILE, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_METRIC)).isNull();
  }

  @Test
  public void verify_reliability_measures_when_no_code_smells_rule() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/qualityModel/without-type-code-smells.xml"));
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "without-type-code-smells");
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-multi-modules-sample")));

    assertThat(getMeasure(PROJECT, CODE_SMELLS_METRIC).getIntValue()).isEqualTo(0);
    assertThat(getMeasure(PROJECT, MAINTAINABILITY_REMEDIATION_EFFORT_METRIC).getIntValue()).isEqualTo(0);
    assertThat(getMeasure(PROJECT, MAINTAINABILITY_RATING_METRIC).getData()).isEqualTo("A");
    assertThat(getMeasure(PROJECT, EFFORT_TO_REACH_MAINTAINABILITY_RATING_A_METRIC).getIntValue()).isEqualTo(0);
  }

  private Measure getMeasure(String resource, String metricKey) {
    Resource res = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resource, metricKey));
    if (res == null) {
      return null;
    }
    return res.getMeasure(metricKey);
  }
}
