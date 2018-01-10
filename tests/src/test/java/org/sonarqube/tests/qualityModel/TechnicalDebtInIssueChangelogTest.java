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
import com.sonar.orchestrator.build.SonarScanner;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.client.issues.ChangelogRequest;
import org.sonarqube.ws.client.issues.SearchRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static util.ItUtils.projectDir;

/**
 * SONAR-4834
 */
public class TechnicalDebtInIssueChangelogTest {

  @ClassRule
  public static Orchestrator orchestrator = QualityModelSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void display_debt_in_issue_changelog() {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/qualityModel/one-issue-per-file.xml"));
    orchestrator.getServer().provisionProject("sample", "sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-file");

    // Execute a first analysis to have a past snapshot
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));

    // Second analysis, existing issues on OneIssuePerFile will have their technical debt updated with the effort to fix
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample"))
      .setProperties("sonar.oneIssuePerFile.effortToFix", "10"));

    Issues.Issue firstIssue = tester.wsClient().issues().search(new SearchRequest()).getIssues(0);

    List<Issues.ChangelogWsResponse.Changelog> changes = changelog(firstIssue.getKey()).getChangelogList();
    assertThat(changes).hasSize(1);
    assertThat(changes.get(0).getDiffsList())
      .extracting(Issues.ChangelogWsResponse.Changelog.Diff::getKey, Issues.ChangelogWsResponse.Changelog.Diff::getOldValue, Issues.ChangelogWsResponse.Changelog.Diff::getNewValue)
      .containsOnly(tuple("effort", "10", "100"));
  }

  private Issues.ChangelogWsResponse changelog(String issueKey) {
    return tester.wsClient().issues().changelog(new ChangelogRequest().setIssue(issueKey));
  }

}
