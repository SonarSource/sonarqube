/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package issue.suite;

import com.sonar.orchestrator.Orchestrator;
import java.util.List;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  CommonRulesTest.class, IssueWorkflowTest.class, ManualRulesTest.class,
})
public class IssueTestSuite {

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .setSonarVersion("DEV")
    .addPlugin(ItUtils.xooPlugin())
    .build();

  static IssueClient adminIssueClient() {
    return ORCHESTRATOR.getServer().adminWsClient().issueClient();
  }

  static Issue searchRandomIssue() {
    List<Issue> issues = searchIssues(IssueQuery.create());
    assertThat(issues).isNotEmpty();
    return issues.get(0);
  }

  static List<Issue> searchIssues(IssueQuery issueQuery) {
    issueQuery.urlParams().put("additionalFields", "_all");
    return adminIssueClient().find(issueQuery).list();
  }

  static Issue searchIssueByKey(String issueKey) {
    IssueQuery query = IssueQuery.create().issues(issueKey);
    query.urlParams().put("additionalFields", "_all");
    List<Issue> issues = searchIssues(query);
    assertThat(issues).hasSize(1);
    return issues.get(0);
  }
}
