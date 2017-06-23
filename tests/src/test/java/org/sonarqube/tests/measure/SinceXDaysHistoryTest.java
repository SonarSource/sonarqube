/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.tests.measure;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category1Suite;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Nullable;
import org.apache.commons.lang.time.DateUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.WsMeasures;
import util.ItUtils;

import static java.lang.Integer.parseInt;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getMeasureWithVariation;
import static util.ItUtils.projectDir;
import static util.ItUtils.setServerProperty;

public class SinceXDaysHistoryTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  private static final String PROJECT = "multi-files-sample";

  @BeforeClass
  public static void analyseProjectWithHistory() {
    initPeriod();

    orchestrator.resetData();
    ItUtils.restoreProfile(orchestrator, SinceXDaysHistoryTest.class.getResource("/measure/one-issue-per-line-profile.xml"));
    orchestrator.getServer().provisionProject(PROJECT, PROJECT);
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT, "xoo", "one-issue-per-line");

    // Execute a analysis in the past before since 30 days period -> 0 issue, 0 file
    analyzeProject("2013-01-01", "**/File1*,**/File2*,**/File3*,**/File4*");

    // Execute a analysis 20 days ago, after since 30 days period -> 16 issues, 1 file
    analyzeProject(getPastDate(20), "**/File2*,**/File3*,**/File4*");

    // Execute a analysis 10 days ago, after since 30 days period -> 28 issues, 2 files
    analyzeProject(getPastDate(10), "**/File3*,**/File4*");

    // Execute a analysis in the present with all modules -> 52 issues, 4 files
    analyzeProject();
  }

  private static void initPeriod() {
    setServerProperty(orchestrator, "sonar.leak.period", "30");
  }

  @AfterClass
  public static void resetPeriods() throws Exception {
    ItUtils.resetPeriod(orchestrator);
  }

  @Test
  public void check_files_variation() throws Exception {
    checkMeasure("files", 3);
  }

  @Test
  public void check_issues_variation() throws Exception {
    checkMeasure("violations", 45);
  }

  @Test
  public void check_new_issues_measures() throws Exception {
    checkMeasure("new_violations", 45);
  }

  private void checkMeasure(String metric, int variation) {
    WsMeasures.Measure measure = getMeasureWithVariation(orchestrator, PROJECT, metric);
    assertThat(measure.getPeriods().getPeriodsValueList()).extracting(periodValue -> parseInt(periodValue.getValue())).containsOnly(variation);
  }

  private static void analyzeProject() {
    analyzeProject(null, null);
  }

  private static void analyzeProject(@Nullable String date, @Nullable String exclusions) {
    SonarScanner runner = SonarScanner.create(projectDir("measureHistory/xoo-multi-files-sample"));
    if (date != null) {
      runner.setProperty("sonar.projectDate", date);
    }
    if (exclusions != null) {
      runner.setProperties("sonar.exclusions", exclusions);
    }
    orchestrator.executeBuild(runner);
  }

  private static String getPastDate(int nbPastDays) {
    return new SimpleDateFormat("yyyy-MM-dd").format(DateUtils.addDays(new Date(), nbPastDays * -1));
  }

}
