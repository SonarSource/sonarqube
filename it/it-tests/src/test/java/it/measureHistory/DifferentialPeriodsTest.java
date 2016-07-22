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
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.ItUtils;
import util.selenium.SeleneseTest;

import static org.apache.commons.lang.time.DateUtils.addDays;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.formatDate;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperty;

public class DifferentialPeriodsTest {

  static final String PROJECT_KEY = "sample";
  static final String MULTI_MODULE_PROJECT_KEY = "com.sonarsource.it.samples:multi-modules-sample";

  @ClassRule
  public static final Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Before
  public void cleanUpAnalysisData() {
    orchestrator.resetData();
  }

  @Before
  public void initPeriods() throws Exception {
    setServerProperty(orchestrator, "sonar.timemachine.period1", "previous_analysis");
    setServerProperty(orchestrator, "sonar.timemachine.period2", "previous_analysis");
    setServerProperty(orchestrator, "sonar.timemachine.period3", "previous_analysis");
  }

  @After
  public void resetPeriods() throws Exception {
    ItUtils.resetPeriods(orchestrator);
  }

  /**
   * SONAR-6787
   */
  @Test
  public void ensure_differential_period_4_and_5_defined_at_project_level_is_taken_into_account() throws Exception {
    orchestrator.getServer().provisionProject(PROJECT_KEY, PROJECT_KEY);
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
    Resource newTechnicalDebt = orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt").setIncludeTrends(true));
    List<Measure> measures = newTechnicalDebt.getMeasures();
    assertThat(measures.get(0).getVariation4()).isEqualTo(17);
    assertThat(measures.get(0).getVariation5()).isEqualTo(17);

    // Third analysis, today, with exactly the same profile -> no new issues so no new technical debt
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT_KEY, "xoo", "one-issue-per-line");
    runProjectAnalysis(orchestrator, "shared/xoo-sample");

    newTechnicalDebt = orchestrator.getServer().getWsClient().find(
      ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt").setIncludeTrends(true)
      );

    // No variation => measure is purged
    assertThat(newTechnicalDebt).isNull();
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
    Resource newTechnicalDebt = orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics(PROJECT_KEY, "violations").setIncludeTrends(true));
    List<Measure> measures = newTechnicalDebt.getMeasures();
    assertThat(measures.get(0).getVariation1()).isEqualTo(17);

    // Check on ui that it's possible to define leak period on project
    new SeleneseTest(Selenese.builder().setHtmlTestsInClasspath("define-leak-period-on-project",
      "/measureHistory/DifferentialPeriodsTest/define-leak-period-on-project.html"
    ).build()).runOn(orchestrator);
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
      "sonar.modules", "module_a"
    );

    // Second analysis, 20 days ago, issues will be created
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/measureHistory/one-issue-per-line-profile.xml"));
    orchestrator.getServer().associateProjectToQualityProfile(MULTI_MODULE_PROJECT_KEY, "xoo", "one-issue-per-line");
    runProjectAnalysis(orchestrator, "shared/xoo-multi-modules-sample",
      "sonar.projectDate", formatDate(addDays(new Date(), -20)),
      "sonar.modules", "module_a,module_b"
    );

    // Variation on module b should exist
    Resource ncloc = orchestrator.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics(MULTI_MODULE_PROJECT_KEY + ":module_b", "ncloc").setIncludeTrends(true));
    List<Measure> measures = ncloc.getMeasures();
    assertThat(measures.get(0).getVariation1()).isEqualTo(24);
  }

}
