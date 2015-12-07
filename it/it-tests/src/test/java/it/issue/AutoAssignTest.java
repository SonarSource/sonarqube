package it.issue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsResponse;
import util.ProjectAnalysis;
import util.ProjectAnalysisRule;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.setServerProperty;

public class AutoAssignTest extends AbstractIssueTest {

  static final String SIMON_USER = "simon";

  @Rule
  public final ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(ORCHESTRATOR);

  ProjectAnalysis projectAnalysis;

  @Before
  public void setup() {
    ORCHESTRATOR.resetData();

    String qualityProfileKey = projectAnalysisRule.registerProfile("/issue/IssueActionTest/xoo-one-issue-per-line-profile.xml");
    String projectKey = projectAnalysisRule.registerProject("issue/xoo-with-scm");
    projectAnalysis = projectAnalysisRule.newProjectAnalysis(projectKey)
      .withQualityProfile(qualityProfileKey)
      .withProperties("sonar.scm.disabled", "false", "sonar.scm.provider", "xoo");
  }

  @After
  public void resetData() throws Exception {
    // Remove user simon
    newAdminWsClient(ORCHESTRATOR).wsConnector().call(
      new PostRequest("api/users/deactivate")
        .setParam("login", SIMON_USER));

    // Reset default assignee
    setServerProperty(ORCHESTRATOR, "sonar.issues.defaultAssigneeLogin", null);
  }

  @Test
  public void auto_assign_issues_to_user() throws Exception {
    createUser(SIMON_USER, SIMON_USER);
    projectAnalysis.run();

    // Simon has 3 issues
    assertThat(search(IssueQuery.create().assignees(SIMON_USER)).list()).hasSize(3);
    // Other issues are not assigned as no user have been created on their author
    assertThat(search(IssueQuery.create().assigned(false)).list()).hasSize(10);
  }

  @Test
  public void auto_assign_issues_to_default_assignee() throws Exception {
    createUser(SIMON_USER, SIMON_USER);
    setServerProperty(ORCHESTRATOR, "sonar.issues.defaultAssigneeLogin", SIMON_USER);
    projectAnalysis.run();

    // Simon has all issues
    assertThat(search(IssueQuery.create().assignees(SIMON_USER)).list()).hasSize(13);
    // No unassigned issues
    assertThat(search(IssueQuery.create().assigned(false)).list()).isEmpty();
  }

  private void createUser(String login, String password) {
    WsResponse response = newAdminWsClient(ORCHESTRATOR).wsConnector().call(
      new PostRequest("api/users/create")
        .setParam("login", login)
        .setParam("name", login)
        .setParam("password", password));
    assertThat(response.code()).isEqualTo(200);
  }
}
