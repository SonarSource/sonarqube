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
package it.measureHistory;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import it.Category1Suite;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang.time.DateUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;
import static util.ItUtils.setServerProperty;

public class SinceXDaysHistoryTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  private static final String PROJECT = "com.sonarsource.it.samples:multi-modules-sample";

  @BeforeClass
  public static void analyseProjectWithHistory() {
    initPeriods();

    orchestrator.resetData();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/measureHistory/one-issue-per-line-profile.xml"));
    orchestrator.getServer().provisionProject(PROJECT, PROJECT);
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "one-issue-per-line");

    // Execute a analysis in the past before since 30 days period -> 0 issue, 0 file
    analyzeProject("2013-01-01", "multi-modules-sample:module_b,multi-modules-sample:module_a");

    // Execute a analysis 20 days ago, after since 30 days period -> 16 issues, 1 file
    analyzeProject(getPastDate(20), "multi-modules-sample:module_b,multi-modules-sample:module_a:module_a2");

    // Execute a analysis 10 days ago, after since 30 days period -> 28 issues, 2 files
    analyzeProject(getPastDate(10), "multi-modules-sample:module_b");

    // Execute a analysis in the present with all modules -> 52 issues, 4 files
    analyzeProject();
  }

  public static void initPeriods() {
    setServerProperty(orchestrator, "sonar.timemachine.period1", "previous_analysis");
    setServerProperty(orchestrator, "sonar.timemachine.period2", "30");
    setServerProperty(orchestrator, "sonar.timemachine.period3", "previous_version");
  }

  @AfterClass
  public static void resetPeriods() throws Exception {
    ItUtils.resetPeriods(orchestrator);
  }

  @Test
  public void periods_are_well_defined() throws Exception {
    Resource project = getProject("files");

    assertThat(project.getPeriod1Mode()).isEqualTo("previous_analysis");

    assertThat(project.getPeriod2Mode()).isEqualTo("days");
    assertThat(project.getPeriod2Param()).isEqualTo("30");
  }

  @Test
  public void check_files_variation() throws Exception {
    checkMeasure("files", 2, 3);
  }

  @Test
  public void check_issues_variation() throws Exception {
    checkMeasure("violations", 24, 45);
  }

  @Test
  public void check_new_issues_measures() throws Exception {
    checkMeasure("new_violations", 24, 45);
  }

  private void checkMeasure(String measure, int variation1, int variation2){
    Resource project = getProject(measure);
    Measure newTechnicalDebt = project.getMeasure(measure);

    assertThat(newTechnicalDebt.getVariation1().intValue()).isEqualTo(variation1);
    assertThat(newTechnicalDebt.getVariation2().intValue()).isEqualTo(variation2);
  }

  private Resource getProject(String... metricKeys) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(PROJECT, metricKeys).setIncludeTrends(true));
  }

  private static void analyzeProject() {
    analyzeProject(null, null);
  }

  private static void analyzeProject(String date, String skippedModules) {
    SonarRunner runner = SonarRunner.create(projectDir("shared/xoo-multi-modules-sample"));
    if (date != null) {
      runner.setProperty("sonar.projectDate", date);
    }
    if (skippedModules != null) {
      runner.setProperties("sonar.skippedModules", skippedModules);
    }
    orchestrator.executeBuild(runner);
  }

  private static String getPastDate(int nbPastDays){
    return new SimpleDateFormat("yyyy-MM-dd").format(DateUtils.addDays(new Date(), nbPastDays * -1));
  }

}
