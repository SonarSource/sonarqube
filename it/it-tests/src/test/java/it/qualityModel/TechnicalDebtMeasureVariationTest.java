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
import com.sonar.orchestrator.locator.FileLocation;
import it.Category2Suite;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static util.ItUtils.getPeriodMeasureValuesByIndex;
import static util.ItUtils.setServerProperty;

/**
 * SONAR-4776
 */
public class TechnicalDebtMeasureVariationTest {

  @ClassRule
  public static Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;

  @BeforeClass
  public static void initPeriods() throws Exception {
    setServerProperty(orchestrator, "sonar.timemachine.period1", "previous_analysis");
    setServerProperty(orchestrator, "sonar.timemachine.period2", "30");
    setServerProperty(orchestrator, "sonar.timemachine.period3", "previous_analysis");
  }

  @AfterClass
  public static void resetPeriods() throws Exception {
    ItUtils.resetPeriods(orchestrator);
  }

  @Before
  public void cleanUpAnalysisData() {
    orchestrator.resetData();
  }

  @Test
  public void new_technical_debt_measures_from_new_issues() throws Exception {
    // This test assumes that period 1 is "since previous analysis" and 2 is "over x days"

    // Execute an analysis in the past to have a past snapshot without any issues
    provisionSampleProject();
    setSampleProjectQualityProfile("empty");
    runSampleProjectAnalysis("sonar.projectDate", "2013-01-01");

    // Second analysis -> issues will be created
    defineQualityProfile("one-issue-per-line");
    setSampleProjectQualityProfile("one-issue-per-line");
    runSampleProjectAnalysis();

    // New technical debt only comes from new issues
    assertThat(getPeriodMeasureValuesByIndex(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt"))
      .contains(entry(1, 17d), entry(2, 17d));

    // Third analysis, with exactly the same profile -> no new issues so no new technical debt
    runSampleProjectAnalysis();
    // No variation => measure is 0 (best value)
    assertThat(getPeriodMeasureValuesByIndex(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt"))
      .contains(entry(1, 0d), entry(2, 0d));
  }

  @Test
  public void new_technical_debt_measures_from_technical_debt_update_since_previous_analysis() throws Exception {
    // This test assumes that period 1 is "since previous analysis"

    // Execute twice analysis
    defineQualityProfile("one-issue-per-file");
    provisionSampleProject();
    setSampleProjectQualityProfile("one-issue-per-file");
    runSampleProjectAnalysis();
    runSampleProjectAnalysis();

    // Third analysis, existing issues on OneIssuePerFile will have their technical debt updated with the effort to fix
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "10");
    assertThat(getPeriodMeasureValuesByIndex(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt")
      .get(1)).isEqualTo(90);

    // Fourth analysis, with exactly the same profile -> no new issues so no new technical debt since previous analysis
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "10");
    assertThat(getPeriodMeasureValuesByIndex(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt")
      .get(1)).isZero();
  }

  @Test
  public void new_technical_debt_measures_from_technical_debt_update_since_30_days() throws Exception {
    // This test assumes that period 2 is "over x days"

    // Execute an analysis in the past to have a past snapshot without any issues
    provisionSampleProject();
    setSampleProjectQualityProfile("empty");
    runSampleProjectAnalysis("sonar.projectDate", "2013-01-01");

    // Second analysis -> issues will be created
    String profileXmlFile = "one-issue-per-file";
    defineQualityProfile(profileXmlFile);
    setSampleProjectQualityProfile("one-issue-per-file");
    runSampleProjectAnalysis();

    // Third analysis, existing issues on OneIssuePerFile will have their technical debt updated with the effort to fix
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "10");
    assertThat(getPeriodMeasureValuesByIndex(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt")
      .get(2)).isEqualTo(90);

    // Fourth analysis, with exactly the same profile -> no new issues so no new technical debt since previous analysis but still since 30
    // days
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "10");
    assertThat(getPeriodMeasureValuesByIndex(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt")
      .get(2)).isEqualTo(90);
  }

  /**
   * SONAR-5059
   */
  @Test
  public void new_technical_debt_measures_should_never_be_negative() throws Exception {
    // This test assumes that period 1 is "since previous analysis" and 2 is "over x days"

    // Execute an analysis with a big effort to fix
    defineQualityProfile("one-issue-per-file");
    provisionSampleProject();
    setSampleProjectQualityProfile("one-issue-per-file");
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "100");

    // Execute a second analysis with a smaller effort to fix -> Added technical debt should be 0, not negative
    runSampleProjectAnalysis("sonar.oneIssuePerFile.effortToFix", "10");
    assertThat(getPeriodMeasureValuesByIndex(orchestrator, "sample:src/main/xoo/sample/Sample.xoo", "new_technical_debt"))
      .contains(entry(1, 0d), entry(2, 0d));
  }

  private void setSampleProjectQualityProfile(String qualityProfileKey) {
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", qualityProfileKey);
  }

  private void provisionSampleProject() {
    orchestrator.getServer().provisionProject("sample", "sample");
  }

  private void defineQualityProfile(String qualityProfileKey) {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/measure/" + qualityProfileKey + ".xml"));
  }

  private void runSampleProjectAnalysis(String... properties) {
    ItUtils.runVerboseProjectAnalysis(TechnicalDebtMeasureVariationTest.orchestrator, "shared/xoo-sample", properties);
  }

}
