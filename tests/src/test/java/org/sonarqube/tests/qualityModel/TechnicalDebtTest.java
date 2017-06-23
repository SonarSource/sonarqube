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
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category2Suite;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import util.ItUtils;

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
  }

  /**
   * SONAR-4716
   */
  @Test
  public void technical_debt_on_issue() throws Exception {
    ItUtils.restoreProfile(orchestrator, getClass().getResource("/qualityModel/one-issue-per-line.xml"));
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

}
