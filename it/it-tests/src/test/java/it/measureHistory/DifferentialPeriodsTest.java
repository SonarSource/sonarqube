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
package it.measureHistory;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import it.Category1Suite;
import java.util.Date;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.WsClient;
import pageobjects.Navigation;

import static org.apache.commons.lang.time.DateUtils.addDays;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.formatDate;
import static util.ItUtils.getMeasure;
import static util.ItUtils.getMeasuresAsDoubleByMetricKey;
import static util.ItUtils.getPeriodMeasureValuesByIndex;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.resetPeriods;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperty;

public class DifferentialPeriodsTest {

  static final String PROJECT_KEY = "sample";
  static final String MULTI_MODULE_PROJECT_KEY = "com.sonarsource.it.samples:multi-modules-sample";

  static WsClient CLIENT;

  @ClassRule
  public static final Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @BeforeClass
  public static void createWsClient() throws Exception {
    CLIENT = newAdminWsClient(orchestrator);
  }

  @Before
  public void cleanUpAnalysisData() {
    orchestrator.resetData();
  }

  @After
  public void reset() throws Exception {
    resetPeriods(orchestrator);
  }

  /**
   * SONAR-6787
   */
  @Test
  public void ensure_differential_period_4_and_5_defined_at_project_level_is_taken_into_account() throws Exception {
    orchestrator.getServer().provisionProject(PROJECT_KEY, PROJECT_KEY);
    setServerProperty(orchestrator, "sonar.timemachine.period1", "previous_analysis");
    setServerProperty(orchestrator, "sonar.timemachine.period2", "previous_analysis");
    setServerProperty(orchestrator, "sonar.timemachine.period3", "previous_analysis");
    setServerProperty(orchestrator, PROJECT_KEY, "sonar.timemachine.period4", "30");
    setServerProperty(orchestrator, PROJECT_KEY, "sonar.timemachine.period5", "previous_analysis");

    // Execute an analysis 60 days ago to have a past snapshot without any issues
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT_KEY, "xoo", "empty");
    runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.projectDate", formatDate(addDays(new Date(), -60)));

    // Second analysis, 20 days ago, issues will be created
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/measureHistory/one-issue-per-line-profile.xml"));
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT_KEY, "xoo", "one-issue-per-line");
    runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.projectDate", formatDate(addDays(new Date(), -20)));

    // New technical debt only comes from new issues
    Map<Integer, Double> periodsMeasure = getPeriodMeasureValuesByIndex(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt");
    assertThat(periodsMeasure.get(4)).isEqualTo(17);
    assertThat(periodsMeasure.get(5)).isEqualTo(17);

    // Third analysis, today, with exactly the same profile -> no new issues so no new technical debt
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT_KEY, "xoo", "one-issue-per-line");
    runProjectAnalysis(orchestrator, "shared/xoo-sample");

    // No variation => measure is purged
    assertThat(getMeasure(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt")).isNull();
  }

  /**
   * SONAR-7093
   */
  @Test
  public void ensure_leak_period_defined_at_project_level_is_taken_into_account() throws Exception {
    orchestrator.getServer().provisionProject(PROJECT_KEY, PROJECT_KEY);

    // Set a global property and a project property to ensure project property is used
    setServerProperty(orchestrator, "sonar.timemachine.period1", "previous_analysis");
    setServerProperty(orchestrator, PROJECT_KEY, "sonar.timemachine.period1", "30");

    // Execute an analysis in the past to have a past snapshot without any issues
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT_KEY, "xoo", "empty");
    runProjectAnalysis(orchestrator, "shared/xoo-sample", "sonar.projectDate", formatDate(addDays(new Date(), -15)));

    // Second analysis -> issues will be created
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/measureHistory/one-issue-per-line-profile.xml"));
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT_KEY, "xoo", "one-issue-per-line");
    runProjectAnalysis(orchestrator, "shared/xoo-sample");

    // Third analysis -> There's no new issue from previous analysis
    runProjectAnalysis(orchestrator, "shared/xoo-sample");

    // Project should have 17 new issues for period 1
    Map<Integer, Double> periodsMeasure = getPeriodMeasureValuesByIndex(orchestrator, PROJECT_KEY, "violations");
    assertThat(periodsMeasure.get(1)).isEqualTo(17);

    // Check on ui that it's possible to define leak period on project
    Navigation.get(orchestrator).openHomepage().logIn().asAdmin().openSettings("sample")
      .assertSettingDisplayed("sonar.timemachine.period1");
  }

  /**
   * SONAR-7237
   */
  @Test
  public void ensure_differential_measures_are_computed_when_adding_new_component_after_period() throws Exception {
    orchestrator.getServer().provisionProject(MULTI_MODULE_PROJECT_KEY, MULTI_MODULE_PROJECT_KEY);
    setServerProperty(orchestrator, MULTI_MODULE_PROJECT_KEY, "sonar.timemachine.period1", "30");

    // Execute an analysis 60 days ago without module b
    orchestrator.getServer().associateProjectToQualityProfile(MULTI_MODULE_PROJECT_KEY, "xoo", "empty");
    runProjectAnalysis(orchestrator, "shared/xoo-multi-modules-sample",
      "sonar.projectDate", formatDate(addDays(new Date(), -60)),
      "sonar.modules", "module_a");

    // Second analysis, 20 days ago, issues will be created
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/measureHistory/one-issue-per-line-profile.xml"));
    orchestrator.getServer().associateProjectToQualityProfile(MULTI_MODULE_PROJECT_KEY, "xoo", "one-issue-per-line");
    runProjectAnalysis(orchestrator, "shared/xoo-multi-modules-sample",
      "sonar.projectDate", formatDate(addDays(new Date(), -20)),
      "sonar.modules", "module_a,module_b");

    // Variation on module b should exist
    Map<Integer, Double> periodsMeasure = getPeriodMeasureValuesByIndex(orchestrator, MULTI_MODULE_PROJECT_KEY + ":module_b", "ncloc");
    assertThat(periodsMeasure.get(1)).isEqualTo(24);
  }

  @Test
  public void compute_no_new_lines_measures_when_changes_but_no_scm() throws Exception {
    orchestrator.getServer().provisionProject(MULTI_MODULE_PROJECT_KEY, MULTI_MODULE_PROJECT_KEY);
    setServerProperty(orchestrator, MULTI_MODULE_PROJECT_KEY, "sonar.timemachine.period1", "previous_analysis");

    // Execute an analysis 60 days ago without module b
    orchestrator.getServer().associateProjectToQualityProfile(MULTI_MODULE_PROJECT_KEY, "xoo", "empty");
    runProjectAnalysis(orchestrator, "shared/xoo-multi-modules-sample",
      "sonar.projectDate", formatDate(addDays(new Date(), -60)),
      "sonar.modules", "module_a");

    // Second analysis, 20 days ago
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/measureHistory/one-issue-per-line-profile.xml"));
    orchestrator.getServer().associateProjectToQualityProfile(MULTI_MODULE_PROJECT_KEY, "xoo", "one-issue-per-line");
    runProjectAnalysis(orchestrator, "shared/xoo-multi-modules-sample",
      "sonar.projectDate", formatDate(addDays(new Date(), -20)),
      "sonar.modules", "module_a,module_b");

    // No new lines measure
    assertNoMeasures(MULTI_MODULE_PROJECT_KEY, "new_lines", "new_lines_to_cover");
  }

  @Test
  public void compute_zero_new_lines_measures_when_no_changes_and_scm_available() throws Exception {
    String projectKey = "sample-scm";
    orchestrator.getServer().provisionProject(projectKey, projectKey);
    setServerProperty(orchestrator, projectKey, "sonar.timemachine.period1", "previous_analysis");

    // Execute an analysis 60 days ago
    runProjectAnalysis(orchestrator, "scm/xoo-sample-with-scm", "sonar.projectDate", formatDate(addDays(new Date(), -60)),
      "sonar.scm.provider", "xoo", "sonar.scm.disabled", "false");

    // Second analysis, 20 days ago
    runProjectAnalysis(orchestrator, "scm/xoo-sample-with-scm", "sonar.projectDate", formatDate(addDays(new Date(), -20)),
      "sonar.scm.provider", "xoo", "sonar.scm.disabled", "false");

    // New lines measures is zero
    assertThat(getPeriodMeasureValuesByIndex(orchestrator, projectKey, "new_lines").get(1)).isEqualTo(0);
    assertThat(getPeriodMeasureValuesByIndex(orchestrator, projectKey, "new_lines_to_cover").get(1)).isEqualTo(0);
  }

  private void assertNoMeasures(String projectKey, String... metrics) {
    assertThat(getMeasuresAsDoubleByMetricKey(orchestrator, projectKey, metrics)).isEmpty();
  }
}
