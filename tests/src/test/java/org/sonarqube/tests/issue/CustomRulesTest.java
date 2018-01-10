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
package org.sonarqube.tests.issue;

import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import util.ProjectAnalysis;
import util.ProjectAnalysisRule;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomRulesTest extends AbstractIssueTest {

  @Rule
  public final ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(ORCHESTRATOR);

  private ProjectAnalysis xooSampleAnalysis;

  @Before
  public void setup() {
    String profileKey = projectAnalysisRule.registerProfile("/issue/CustomRulesTest/custom.xml");
    String projectKey = projectAnalysisRule.registerProject("shared/xoo-sample");
    this.xooSampleAnalysis = projectAnalysisRule.newProjectAnalysis(projectKey)
      .withQualityProfile(profileKey);
  }

  @Test
  public void analyzeProjectWithCustomRules() {
    ORCHESTRATOR.getServer().adminWsClient().post("api/rules/create",
      "template_key", "xoo:TemplateRule",
      "custom_key", "MyCustomRule",
      "markdown_description", "My description",
      "name", "My custom rule",
      "severity", "BLOCKER",
      "params", "line=2");

    xooSampleAnalysis.run();

    List<Issue> issues = searchIssues();
    assertThat(issues).hasSize(1);

    Issue issue = issues.get(0);
    assertThat(issue.ruleKey()).isEqualTo("xoo:MyCustomRule");
    assertThat(issue.line()).isEqualTo(2);
    // Overridden in quality profile
    assertThat(issue.severity()).isEqualTo("CRITICAL");
  }
}
