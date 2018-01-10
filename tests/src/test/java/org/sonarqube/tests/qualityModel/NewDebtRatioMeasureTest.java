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
import java.util.Date;
import javax.annotation.Nullable;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import util.ItUtils;

import static org.apache.commons.lang.time.DateUtils.addDays;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static util.ItUtils.formatDate;
import static util.ItUtils.getLeakPeriodValue;
import static util.ItUtils.toDate;

/**
 * SONAR-5876
 */
public class NewDebtRatioMeasureTest {

  private static final String NEW_DEBT_RATIO_METRIC_KEY = "new_sqale_debt_ratio";
  private static final Date FIRST_COMMIT_DATE = toDate("2016-09-01");
  private static final Date SECOND_COMMIT_DATE = toDate("2016-09-17");
  private static final Date THIRD_COMMIT_DATE = toDate("2016-09-20");

  @ClassRule
  public static Orchestrator orchestrator = QualityModelSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void new_debt_ratio_is_computed_from_new_debt_and_new_ncloc_count_per_file() {
    tester.settings().setGlobalSettings("sonar.leak.period", "previous_version");

    // run analysis on the day of after the first commit, with 'one-issue-per-line' profile
    defineQualityProfile("one-issue-per-line");
    provisionSampleProject();
    setSampleProjectQualityProfile("one-issue-per-line");
    runSampleProjectAnalysis("v1", "sonar.projectDate", formatDate(addDays(FIRST_COMMIT_DATE, 1)));

    // first analysis, no previous snapshot => periods not resolved => no value
    assertNoNewDebtRatio();

    // run analysis on the day after of second commit 'one-issue-per-line' profile*
    // => 3 new issues will be created
    runSampleProjectAnalysis("v2", "sonar.projectDate", formatDate(addDays(SECOND_COMMIT_DATE, 1)));
    assertNewDebtRatio(4.44);

    // run analysis on the day after of third commit 'one-issue-per-line' profile*
    // => 4 new issues will be created
    runSampleProjectAnalysis("v3", "sonar.projectDate", formatDate(addDays(THIRD_COMMIT_DATE, 1)));
    assertNewDebtRatio(4.17);
  }

  @Test
  public void compute_new_debt_ratio_using_number_days_in_leak_period() {
    tester.settings().setGlobalSettings("sonar.leak.period", "30");

    // run analysis on the day of after the first commit, with 'one-issue-per-line' profile
    defineQualityProfile("one-issue-per-line");
    provisionSampleProject();
    setSampleProjectQualityProfile("one-issue-per-line");
    runSampleProjectAnalysis("v1", "sonar.projectDate", formatDate(addDays(FIRST_COMMIT_DATE, 1)));

    // first analysis, no previous snapshot => periods not resolved => no value
    assertNoNewDebtRatio();

    // run analysis on the day after of second commit 'one-issue-per-line' profile*
    // => 3 new issues will be created
    runSampleProjectAnalysis("v2", "sonar.projectDate", formatDate(addDays(SECOND_COMMIT_DATE, 1)));
    assertNewDebtRatio(4.44);

    // run analysis on the day after of third commit 'one-issue-per-line' profile*
    // => previous 3 issues plus 4 new issues will be taking into account
    runSampleProjectAnalysis("v3", "sonar.projectDate", formatDate(addDays(THIRD_COMMIT_DATE, 1)));
    assertNewDebtRatio(4.28);
  }

  private void assertNoNewDebtRatio() {
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", NEW_DEBT_RATIO_METRIC_KEY)).isZero();
  }

  private void assertNewDebtRatio(@Nullable Double valuePeriod) {
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", NEW_DEBT_RATIO_METRIC_KEY)).isEqualTo(valuePeriod, within(0.01));
  }

  private void setSampleProjectQualityProfile(String qualityProfileKey) {
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", qualityProfileKey);
  }

  private void provisionSampleProject() {
    orchestrator.getServer().provisionProject("sample", "sample");
  }

  private void defineQualityProfile(String qualityProfileKey) {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/measure/" + qualityProfileKey + ".xml"));
  }

  private void runSampleProjectAnalysis(String projectVersion, String... properties) {
    ItUtils.runVerboseProjectAnalysis(
      NewDebtRatioMeasureTest.orchestrator,
      "measure/xoo-new-debt-ratio-" + projectVersion,
      ItUtils.concat(properties,
        // disable standard scm support so that it does not interfere with Xoo Scm sensor
        "sonar.scm.disabled", "false",
        "sonar.projectVersion", projectVersion));
  }

}
