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
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class TechnicalDebtTest {

  @ClassRule
  public static Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;

  @Rule
  public DebtConfigurationRule debtConfiguration = DebtConfigurationRule.create(orchestrator);

  @Before
  public void deleteAnalysisData() {
    orchestrator.resetData();

    // Set hours in day property to 8
    debtConfiguration.updateHoursInDay(8);
  }

  /**
   * SONAR-4716
   */
  @Test
  public void technical_debt_on_issue() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/qualityModel/one-issue-per-line.xml"));
    orchestrator.getServer().provisionProject("sample", "sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");

    // Generate some issues
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));

    // All the issues should have a technical debt
    List<Issue> issues = orchestrator.getServer().wsClient().issueClient().find(IssueQuery.create()).list();
    assertThat(issues).isNotEmpty();
    for (Issue issue : issues) {
      assertThat(issue.debt()).isEqualTo("1min");
    }
  }

  @Test
  public void use_hours_in_day_property_to_display_debt() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/qualityModel/one-issue-per-file.xml"));
    orchestrator.getServer().provisionProject("sample", "sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-file");

    // One day -> 10 hours
    debtConfiguration.updateHoursInDay(10);

    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample"))
      // As OneIssuePerFile has a debt of 10 minutes, we multiply it by 72 to have 1 day and 2 hours of technical debt
      .setProperties("sonar.oneIssuePerFile.effortToFix", "72")
      );

    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    Issue issue = issueClient.find(IssueQuery.create()).list().get(0);

    assertThat(issue.debt()).isEqualTo("1d2h");
  }

  @Test
  public void use_hours_in_day_property_during_analysis_to_convert_debt() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/qualityModel/one-day-debt-per-file.xml"));
    orchestrator.getServer().provisionProject("sample", "sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-day-debt-per-file");

    // One day -> 10 hours : debt will be stored as 360.000 seconds (1 day * 10 hours per day * 60 * 60)
    debtConfiguration.updateHoursInDay(10);

    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));

    // Issue debt was 1 day during analysis but will be displayed as 1 day and 2 hours (hours in day property was set
    // to 10 during analysis but is now 8)
    debtConfiguration.updateHoursInDay(8);

    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    Issue issue = issueClient.find(IssueQuery.create()).list().get(0);
    assertThat(issue.debt()).isEqualTo("1d2h");
  }

}
