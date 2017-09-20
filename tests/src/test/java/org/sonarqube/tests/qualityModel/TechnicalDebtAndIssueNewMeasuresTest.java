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
package org.sonarqube.tests.qualityModel;

import com.sonar.orchestrator.Orchestrator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.tests.Category2Suite;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.getLeakPeriodValue;
import static util.ItUtils.setServerProperty;

/**
 * SONAR-4776
 */
public class TechnicalDebtAndIssueNewMeasuresTest {

  private static final String DATE_31_DAYS_AGO = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now().minusDays(31));// "2013-01-01";

  @ClassRule
  public static Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;

  @AfterClass
  public static void resetPeriod() throws Exception {
    ItUtils.resetPeriod(orchestrator);
  }

  @Before
  public void cleanUpAnalysisData() {
    orchestrator.resetData();
  }

  @Test
  public void new_technical_debt_measures_from_new_issues() throws Exception {
    setServerProperty(orchestrator, "sonar.leak.period", "previous_analysis");

    // Execute an analysis in the past to have a past snapshot without any issues
    provisionSampleProject();
    setSampleProjectQualityProfile("empty");
    runSampleProjectAnalysis("sonar.projectDate", DATE_31_DAYS_AGO);

    // Second analysis -> issues will be created
    defineQualityProfile("one-issue-per-line");
    setSampleProjectQualityProfile("one-issue-per-line");

    // New technical debt only comes from new issues
    runSampleProjectAnalysis();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_technical_debt")).isEqualTo(17d);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_violations")).isEqualTo(17);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_code_smells")).isEqualTo(17);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_minor_violations")).isEqualTo(17);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_info_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt")).isEqualTo(17d);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_violations")).isEqualTo(17);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_code_smells")).isEqualTo(17);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_minor_violations")).isEqualTo(17);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_info_violations")).isZero();

    // Third analysis, with exactly the same profile -> no new issues so no new technical debt
    runSampleProjectAnalysis();
    // No variation => measure is 0 (best value)
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_technical_debt")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_code_smells")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_minor_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_info_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_code_smells")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_minor_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_info_violations")).isZero();
  }

  @Test
  public void new_technical_debt_measures_from_technical_debt_update_since_previous_analysis() throws Exception {
    setServerProperty(orchestrator, "sonar.leak.period", "previous_analysis");

    // Execute twice analysis
    defineQualityProfile("one-issue-per-file");
    provisionSampleProject();
    setSampleProjectQualityProfile("one-issue-per-file");
    runSampleProjectAnalysis();
    runSampleProjectAnalysis();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_technical_debt")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_code_smells")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_minor_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_info_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_code_smells")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_minor_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_info_violations")).isZero();

    // Third analysis, existing issues on OneIssuePerFile will have their technical debt updated with the effort to fix
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "10");
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_technical_debt")).isEqualTo(90);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_code_smells")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_minor_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_info_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt")).isEqualTo(90);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_code_smells")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_minor_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_info_violations")).isZero();

    // Fourth analysis, with exactly the same profile -> no new issues so no new technical debt since previous analysis
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "10");
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_technical_debt")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_code_smells")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_minor_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_info_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_code_smells")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_minor_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_info_violations")).isZero();
  }

  @Test
  public void new_technical_debt_measures_from_technical_debt_update_since_30_days_with_constant_effort() throws Exception {
    setServerProperty(orchestrator, "sonar.leak.period", "30");

    // Execute an analysis in the past to have a past snapshot without any issues
    provisionSampleProject();
    setSampleProjectQualityProfile("empty");
    runSampleProjectAnalysis("sonar.projectDate", DATE_31_DAYS_AGO);

    // Second analysis -> issues will be created
    String profileXmlFile = "one-issue-per-file";
    defineQualityProfile(profileXmlFile);
    setSampleProjectQualityProfile("one-issue-per-file");
    runSampleProjectAnalysis();

    // no new issue and no change in debt but still one issue on the leak period (30d)
    runSampleProjectAnalysis();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_technical_debt")).isEqualTo(10);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_violations")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_code_smells")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_minor_violations")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_info_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt")).isEqualTo(10);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_violations")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_code_smells")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_minor_violations")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_info_violations")).isZero();

    // Fourth analysis, with exactly the same profile -> no new issues so no new technical debt since previous analysis but still since 30
    // days
    runSampleProjectAnalysis();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_technical_debt")).isEqualTo(10);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_violations")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_code_smells")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_minor_violations")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_info_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt")).isEqualTo(10);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_violations")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_code_smells")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_minor_violations")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_info_violations")).isZero();
  }

  @Test
  public void new_technical_debt_measures_from_technical_debt_update_since_30_days_with_effort_change() throws Exception {
    setServerProperty(orchestrator, "sonar.leak.period", "30");

    // Execute an analysis in the past to have a past snapshot without any issues
    provisionSampleProject();
    setSampleProjectQualityProfile("empty");
    runSampleProjectAnalysis("sonar.projectDate", DATE_31_DAYS_AGO);

    // Second analysis -> issues will be created
    String profileXmlFile = "one-issue-per-file";
    defineQualityProfile(profileXmlFile);
    setSampleProjectQualityProfile("one-issue-per-file");
    runSampleProjectAnalysis();

    // Third analysis, existing issues on OneIssuePerFile will have their technical debt updated with the effort to fix
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "10");
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_technical_debt")).isEqualTo(100);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_violations")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_code_smells")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_minor_violations")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_info_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt")).isEqualTo(100);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_violations")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_code_smells")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_minor_violations")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_info_violations")).isZero();

    // Fourth analysis, with exactly the same profile -> no new issues so no new technical debt since previous analysis but still since 30
    // days
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "10");
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_technical_debt")).isEqualTo(100);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_violations")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_code_smells")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_minor_violations")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample", "new_info_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt")).isEqualTo(100);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_violations")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_code_smells")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_bugs")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_vulnerabilities")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_blocker_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_critical_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_major_violations")).isZero();
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_minor_violations")).isEqualTo(1);
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_info_violations")).isZero();
  }

  /**
   * SONAR-5059
   */
  @Test
  public void new_technical_debt_measures_should_never_be_negative() throws Exception {
    setServerProperty(orchestrator, "sonar.leak.period", "previous_analysis");

    // Execute an analysis with a big effort to fix
    defineQualityProfile("one-issue-per-file");
    provisionSampleProject();
    setSampleProjectQualityProfile("one-issue-per-file");
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "100");

    // Execute a second analysis with a smaller effort to fix -> Added technical debt should be 0, not negative
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "10");
    assertThat(getLeakPeriodValue(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt")).isZero();
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

  private void runSampleProjectAnalysis(String... properties) {
    ItUtils.runVerboseProjectAnalysis(TechnicalDebtAndIssueNewMeasuresTest.orchestrator, "shared/xoo-sample", properties);
  }

}
