/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package issue.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomRulesTest {

  @ClassRule
  public static Orchestrator orchestrator = IssueTestSuite.ORCHESTRATOR;

  @Before
  public void deleteData() {
    orchestrator.resetData();
  }

  @Test
  public void analyzeProjectWithCustomRules() throws Exception {

    orchestrator.getServer().adminWsClient().post("api/rules/create",
      "template_key", "xoo:TemplateRule",
      "custom_key", "MyCustomRule",
      "markdown_description", "My description",
      "name", "My custom rule",
      "severity", "BLOCKER",
      "params", "line=2");

    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/issue/suite/CustomRulesTest/custom.xml"));

    orchestrator.getServer().provisionProject("sample", "Sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "Custom");

    orchestrator.executeBuild(SonarRunner.create().setProjectDir(ItUtils.projectDir("shared/xoo-sample")));

    List<Issue> issues = orchestrator.getServer().adminWsClient().issueClient().find(IssueQuery.create()).list();
    assertThat(issues).hasSize(1);

    Issue issue = issues.get(0);
    assertThat(issue.ruleKey()).isEqualTo("xoo:MyCustomRule");
    assertThat(issue.line()).isEqualTo(2);
    // Overriden in quality profile
    assertThat(issue.severity()).isEqualTo("CRITICAL");
  }
}
