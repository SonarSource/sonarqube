package issue.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class IssueWorkflowTest {

  @ClassRule
  public static Orchestrator orchestrator = IssueTestSuite.ORCHESTRATOR;

  @Before
  public void setUp() {
    orchestrator.resetData();
  }

  /**
   * Issue on a disabled rule (uninstalled plugin or rule deactivated from quality profile) must 
   * be CLOSED with resolution REMOVED
   */
  @Test
  public void issue_is_closed_as_removed_when_rule_is_disabled() throws Exception {
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/issue/suite/IssueWorkflowTest/xoo-one-issue-per-line-profile.xml"));
    orchestrator.getServer().provisionProject("workflow", "Workflow");
    orchestrator.getServer().associateProjectToQualityProfile("workflow", "xoo", "xoo-one-issue-per-line-profile");

    SonarRunner analysis = SonarRunner.create(projectDir("issue/workflow"));
    orchestrator.executeBuild(analysis);

    IssueClient issueClient = orchestrator.getServer().wsClient().issueClient();
    List<Issue> issues = issueClient.find(IssueQuery.create().rules("xoo:OneIssuePerLine")).list();
    assertThat(issues).isNotEmpty();

    // re-analyze with profile "empty". The rule is disabled so the issues must be closed
    orchestrator.getServer().associateProjectToQualityProfile("workflow", "xoo", "empty");
    analysis = SonarRunner.create(projectDir("issue/workflow"));
    orchestrator.executeBuild(analysis);
    issues = issueClient.find(IssueQuery.create().rules("xoo:OneIssuePerLine").componentRoots("workflow")).list();
    assertThat(issues).isNotEmpty();
    for (Issue issue : issues) {
      assertThat(issue.status()).isEqualTo("CLOSED");
      assertThat(issue.resolution()).isEqualTo("REMOVED");
    }
  }
}
