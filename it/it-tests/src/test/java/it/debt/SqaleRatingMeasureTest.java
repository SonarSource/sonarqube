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
package it.debt;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
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
public class SqaleRatingMeasureTest {

  private static final String PROJECT = "com.sonarsource.it.samples:multi-modules-sample";
  private static final String MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a";
  private static final String SUB_MODULE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1";
  private static final String DIRECTORY = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1";
  private static final String FILE = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo";

  @ClassRule
  public static Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;

  @Rule
  public DebtConfigurationRule debtConfiguration = DebtConfigurationRule.create(orchestrator);

  @Before
  public void init() {
    orchestrator.resetData();

    // Set rating grid values to not depend from default value
    debtConfiguration.updateRatingGrid(0.1d, 0.2d, 0.5d, 1d);
  }

  @Test
  public void sqale_rating_measures() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/debt/with-many-rules.xml"));
    orchestrator.getServer().provisionProject(PROJECT, PROJECT);
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "with-many-rules");

    orchestrator.executeBuild(SonarRunner.create(projectDir("shared/xoo-multi-modules-sample")));

    assertThat(getMeasure(PROJECT, "sqale_rating").getIntValue()).isEqualTo(3);
    assertThat(getMeasure(PROJECT, "sqale_rating").getData()).isEqualTo("C");

    assertThat(getMeasure(MODULE, "sqale_rating").getIntValue()).isEqualTo(3);
    assertThat(getMeasure(MODULE, "sqale_rating").getData()).isEqualTo("C");

    assertThat(getMeasure(SUB_MODULE, "sqale_rating").getIntValue()).isEqualTo(3);
    assertThat(getMeasure(SUB_MODULE, "sqale_rating").getData()).isEqualTo("C");

    assertThat(getMeasure(DIRECTORY, "sqale_rating").getIntValue()).isEqualTo(1);
    assertThat(getMeasure(DIRECTORY, "sqale_rating").getData()).isEqualTo("A");

    assertThat(getMeasure(FILE, "sqale_rating").getIntValue()).isEqualTo(1);
    assertThat(getMeasure(FILE, "sqale_rating").getData()).isEqualTo("A");
  }

  @Test
  public void sqale_debt_ratio_measures() {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/debt/with-many-rules.xml"));
    orchestrator.getServer().provisionProject(PROJECT, PROJECT);
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "with-many-rules");

    orchestrator.executeBuild(SonarRunner.create(projectDir("shared/xoo-multi-modules-sample")));

    assertThat(getMeasure(PROJECT, "sqale_debt_ratio").getValue()).isEqualTo(29.1d);
    assertThat(getMeasure(MODULE, "sqale_debt_ratio").getValue()).isEqualTo(28.5d);
    assertThat(getMeasure(SUB_MODULE, "sqale_debt_ratio").getValue()).isEqualTo(31.4d);
    assertThat(getMeasure(DIRECTORY, "sqale_debt_ratio").getValue()).isEqualTo(7.8d);
    assertThat(getMeasure(FILE, "sqale_debt_ratio").getValue()).isEqualTo(7.8d);
  }

  @Test
  public void use_development_cost_parameter() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/debt/one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject("sample", "sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    orchestrator.executeBuild(SonarRunner.create(projectDir("shared/xoo-sample")));

    Measure rating = getMeasure("sample", "sqale_rating");
    assertThat(rating.getIntValue()).isEqualTo(1);
    assertThat(rating.getData()).isEqualTo("A");

    debtConfiguration.updateDevelopmentCost(2);
    orchestrator.executeBuild(SonarRunner.create(projectDir("shared/xoo-sample")));

    rating = getMeasure("sample", "sqale_rating");
    assertThat(rating.getIntValue()).isEqualTo(4);
    assertThat(rating.getData()).isEqualTo("D");
  }

  @Test
  public void use_language_specific_parameters() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/debt/one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject(PROJECT, PROJECT);
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "one-issue-per-line");

    orchestrator.executeBuild(SonarRunner.create(projectDir("shared/xoo-multi-modules-sample")));

    Measure rating = getMeasure(PROJECT, "sqale_rating");
    assertThat(rating.getIntValue()).isEqualTo(1);
    assertThat(rating.getData()).isEqualTo("A");

    debtConfiguration.updateLanguageDevelopmentCost("xoo", 1);
    orchestrator.executeBuild(
      SonarRunner.create(projectDir("shared/xoo-multi-modules-sample"))
        .setProfile("one-issue-per-line"));

    rating = getMeasure(PROJECT, "sqale_rating");
    assertThat(rating.getIntValue()).isEqualTo(5);
    assertThat(rating.getData()).isEqualTo("E");
  }

  @Test
  public void use_rating_grid_parameter() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/debt/one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject("sample", "sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    orchestrator.executeBuild(SonarRunner.create(projectDir("shared/xoo-sample")));

    Measure rating = getMeasure("sample", "sqale_rating");
    assertThat(rating.getIntValue()).isEqualTo(1);
    assertThat(rating.getData()).isEqualTo("A");

    debtConfiguration.updateRatingGrid(0.001d, 0.005d, 0.01d, 0.015d);
    orchestrator.executeBuild(SonarRunner.create(projectDir("shared/xoo-sample")));

    rating = getMeasure("sample", "sqale_rating");
    assertThat(rating.getIntValue()).isEqualTo(5);
    assertThat(rating.getData()).isEqualTo("E");
  }

  private Measure getMeasure(String resource, String metricKey) {
    Resource res = orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(resource, metricKey));
    if (res == null) {
      return null;
    }
    return res.getMeasure(metricKey);
  }

}
