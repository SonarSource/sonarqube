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
package it.issue;

import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;
import static util.ItUtils.setServerProperty;

/**
 * SONAR-4564
 */
public class NewIssuesMeasureTest extends AbstractIssueTest {

  @BeforeClass
  public static void preparePeriodsAndQProfiles() {
    setServerProperty(ORCHESTRATOR, "sonar.timemachine.period1", "previous_analysis");
    setServerProperty(ORCHESTRATOR, "sonar.timemachine.period2", "30");
    setServerProperty(ORCHESTRATOR, "sonar.timemachine.period3", "previous_analysis");
  }

  @AfterClass
  public static void resetPeriods() {
    ItUtils.resetPeriods(ORCHESTRATOR);
  }

  @Before
  public void cleanUpAnalysisData() {
    ORCHESTRATOR.resetData();
  }

  @Test
  public void new_issues_measures() throws Exception {
    ORCHESTRATOR.getServer().provisionProject("sample", "Sample");

    // Execute an analysis in the past with no issue to have a past snapshot
    ORCHESTRATOR.getServer().associateProjectToQualityProfile("sample", "xoo", "empty");
    ORCHESTRATOR.executeBuild(SonarRunner.create(projectDir("shared/xoo-sample")).setProperty("sonar.projectDate", "2013-01-01"));

    // Execute a analysis now with some issues
    ORCHESTRATOR.getServer().restoreProfile(FileLocation.ofClasspath("/issue/one-issue-per-line-profile.xml"));
    ORCHESTRATOR.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line-profile");
    ORCHESTRATOR.executeBuild(SonarRunner.create(projectDir("shared/xoo-sample")));

    assertThat(ORCHESTRATOR.getServer().wsClient().issueClient().find(IssueQuery.create()).list()).isNotEmpty();
    Resource newIssues = ORCHESTRATOR.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", "new_violations").setIncludeTrends(true));
    List<Measure> measures = newIssues.getMeasures();
    assertThat(measures.get(0).getVariation1().intValue()).isEqualTo(17);
    assertThat(measures.get(0).getVariation2().intValue()).isEqualTo(17);

    // second analysis, with exactly the same profile -> no new issues
    ORCHESTRATOR.executeBuild(SonarRunner.create(projectDir("shared/xoo-sample")));

    assertThat(ORCHESTRATOR.getServer().wsClient().issueClient().find(IssueQuery.create()).list()).isNotEmpty();
    newIssues = ORCHESTRATOR.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample:src/main/xoo/sample/Sample.xoo", "new_violations").setIncludeTrends(true));
    // No variation => measure is purged
    assertThat(newIssues).isNull();
  }

  @Test
  public void new_issues_measures_should_be_zero_on_project_when_no_new_issues_since_x_days() throws Exception {
    ORCHESTRATOR.getServer().provisionProject("sample", "Sample");
    ORCHESTRATOR.getServer().restoreProfile(FileLocation.ofClasspath("/issue/one-issue-per-line-profile.xml"));
    ORCHESTRATOR.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line-profile");

    ORCHESTRATOR.executeBuild(SonarRunner.create(projectDir("shared/xoo-sample"))
      // Analyse a project in the past, with a date older than 30 last days (second period)
      .setProperty("sonar.projectDate", "2013-01-01"));
    ORCHESTRATOR.executeBuild(SonarRunner.create(projectDir("shared/xoo-sample")));

    // new issues measures should be to 0 on project on 2 periods as new issues has been created
    Resource file = ORCHESTRATOR.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample", "new_violations").setIncludeTrends(true));
    List<Measure> measures = file.getMeasures();
    Measure newIssues = find(measures, "new_violations");
    assertThat(newIssues.getVariation1().intValue()).isEqualTo(0);
    assertThat(newIssues.getVariation2().intValue()).isEqualTo(0);
  }

  /**
   * SONAR-3647
   */
  @Test
  public void new_issues_measures_consistent_with_variations() throws Exception {
    ORCHESTRATOR.getServer().provisionProject("sample", "Sample");
    ORCHESTRATOR.getServer().restoreProfile(FileLocation.ofClasspath("/issue/one-issue-per-line-profile.xml"));
    ORCHESTRATOR.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line-profile");

    // Execute an analysis in the past to have a past snapshot
    // version 1
    ORCHESTRATOR.executeBuilds(SonarRunner.create(projectDir("shared/xoo-history-v1")));

    // version 2 with 2 new violations and 3 more ncloc
    ORCHESTRATOR.executeBuilds(SonarRunner.create(projectDir("shared/xoo-history-v2")));

    assertThat(ORCHESTRATOR.getServer().wsClient().issueClient().find(IssueQuery.create()).list()).isNotEmpty();
    Resource file = ORCHESTRATOR.getServer().getWsClient().find(ResourceQuery.createForMetrics("sample", "new_violations", "violations", "ncloc").setIncludeTrends(true));
    List<Measure> measures = file.getMeasures();
    Measure newIssues = find(measures, "new_violations");
    assertThat(newIssues.getVariation1().intValue()).isEqualTo(17);
    assertThat(newIssues.getVariation2().intValue()).isEqualTo(17);

    Measure violations = find(measures, "violations");
    assertThat(violations.getValue().intValue()).isEqualTo(43);
    assertThat(violations.getVariation1().intValue()).isEqualTo(17);
    assertThat(violations.getVariation2().intValue()).isEqualTo(17);

    Measure ncloc = find(measures, "ncloc");
    assertThat(ncloc.getValue().intValue()).isEqualTo(40);
    assertThat(ncloc.getVariation1().intValue()).isEqualTo(16);
    assertThat(ncloc.getVariation2().intValue()).isEqualTo(16);
  }

  @Test
  public void new_issues_measures_should_be_correctly_calculated_when_adding_a_new_module() throws Exception {
    ORCHESTRATOR.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "com.sonarsource.it.samples:multi-modules-sample");

    // First analysis without module b
    ORCHESTRATOR.getServer().restoreProfile(FileLocation.ofClasspath("/issue/NewIssuesMeasureTest/profile1.xml"));
    ORCHESTRATOR.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "profile1");
    ORCHESTRATOR.executeBuild(SonarRunner.create(projectDir("shared/xoo-multi-modules-sample"))
      .setProperties("sonar.modules", "module_a"));

    // Second analysis with module b and with a new rule activated to have new issues on module a since last analysis
    ORCHESTRATOR.getServer().restoreProfile(FileLocation.ofClasspath("/issue/NewIssuesMeasureTest/profile2.xml"));
    ORCHESTRATOR.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "profile2");
    ORCHESTRATOR.executeBuild(SonarRunner.create(projectDir("shared/xoo-multi-modules-sample")));

    Resource project = ORCHESTRATOR.getServer().getWsClient()
      .find(ResourceQuery.createForMetrics("com.sonarsource.it.samples:multi-modules-sample", "new_violations", "violations").setIncludeTrends(true));
    List<Measure> measures = project.getMeasures();
    Measure newIssues = find(measures, "new_violations");
    assertThat(newIssues.getVariation1().intValue()).isEqualTo(65);
  }

  private Measure find(List<Measure> measures, String metricKey) {
    for (Measure measure : measures) {
      if (measure.getMetricKey().equals(metricKey)) {
        return measure;
      }
    }
    return null;
  }

}
