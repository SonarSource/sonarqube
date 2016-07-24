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
import org.sonar.wsclient.issue.IssueChange;
import org.sonar.wsclient.issue.IssueChangeDiff;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

/**
 * SONAR-4834
 */
public class TechnicalDebtInIssueChangelogTest {

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

  @Test
  public void display_debt_in_issue_changelog() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/qualityModel/one-issue-per-file.xml"));
    orchestrator.getServer().provisionProject("sample", "sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-file");

    // Execute a first analysis to have a past snapshot
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));

    // Second analysis, existing issues on OneIssuePerFile will have their technical debt updated with the effort to fix
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample"))
      .setProperties("sonar.oneIssuePerFile.effortToFix", "10"));

    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    Issue issue = issueClient.find(IssueQuery.create()).list().get(0);
    List<IssueChange> changes = issueClient.changes(issue.key());

    assertThat(changes).hasSize(1);
    IssueChange change = changes.get(0);

    assertThat(change.diffs()).hasSize(1);
    IssueChangeDiff changeDiff = change.diffs().get(0);
    assertThat(changeDiff.key()).isEqualTo("effort");
    assertThat(changeDiff.oldValue()).isEqualTo("10min");
    assertThat(changeDiff.newValue()).isEqualTo("1h40min");
  }

  @Test
  public void use_hours_in_day_property_to_display_debt_in_issue_changelog() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/qualityModel/one-issue-per-file.xml"));
    orchestrator.getServer().provisionProject("sample", "sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-file");

    // Execute a first analysis to have a past snapshot
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));

    // One day -> 10 hours
    debtConfiguration.updateHoursInDay(10);

    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample"))
      // As OneIssuePerFile has a debt of 10 minutes, we multiply it by 72 to have 1 day and 2 hours of technical debtn
      .setProperties("sonar.oneIssuePerFile.effortToFix", "72")
      );

    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    Issue issue = issueClient.find(IssueQuery.create()).list().get(0);
    List<IssueChange> changes = issueClient.changes(issue.key());

    assertThat(changes).hasSize(1);
    IssueChange change = changes.get(0);

    assertThat(change.diffs()).hasSize(1);
    IssueChangeDiff changeDiff = change.diffs().get(0);
    assertThat(changeDiff.key()).isEqualTo("effort");
    assertThat(changeDiff.oldValue()).isEqualTo("10min");
    assertThat(changeDiff.newValue()).isEqualTo("1d2h");
  }

}
