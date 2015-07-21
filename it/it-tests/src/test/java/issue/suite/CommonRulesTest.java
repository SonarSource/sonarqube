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
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class CommonRulesTest {

  public static final String FILE_KEY = "common-rules-project:src/Sample.xoo";
  public static final String TEST_FILE_KEY = "common-rules-project:test/SampleTest.xoo";

  @ClassRule
  public static Orchestrator orchestrator = IssueTestSuite.ORCHESTRATOR;

  @BeforeClass
  public static void setUp() {
    orchestrator.resetData();
    orchestrator.getServer().restoreProfile(FileLocation.ofClasspath("/issue/suite/CommonRulesTest/xoo-common-rules-profile.xml"));
    orchestrator.getServer().provisionProject("common-rules-project", "Sample");
    orchestrator.getServer().associateProjectToQualityProfile("common-rules-project", "xoo", "xoo-common-rules");
    SonarRunner analysis = SonarRunner.create(projectDir("issue/common-rules"),
      "sonar.cpd.xoo.minimumTokens", "2",
      "sonar.cpd.xoo.minimumLines", "2");
    orchestrator.executeBuild(analysis);
  }

  @Test
  public void test_rule_on_duplicated_blocks() {
    List<Issue> issues = findIssues(FILE_KEY, "common-xoo:DuplicatedBlocks");
    assertThat(issues).hasSize(1);
  }

  @Test
  public void test_rule_on_comments() {
    List<Issue> issues = findIssues(FILE_KEY, "common-xoo:InsufficientCommentDensity");
    assertThat(issues.size()).isEqualTo(1);
  }

  @Test
  public void test_rule_on_coverage() {
    List<Issue> issues = findIssues(FILE_KEY, "common-xoo:InsufficientBranchCoverage");
    assertThat(issues.size()).isEqualTo(1);

    issues = findIssues(FILE_KEY, "common-xoo:InsufficientLineCoverage");
    assertThat(issues.size()).isEqualTo(1);
  }

  @Test
  public void test_rule_on_skipped_tests() {
    List<Issue> issues = findIssues(TEST_FILE_KEY, "common-xoo:SkippedUnitTests");
    assertThat(issues.size()).isEqualTo(1);
  }

  @Test
  public void test_rule_on_test_errors() {
    List<Issue> issues = findIssues(TEST_FILE_KEY, "common-xoo:FailedUnitTests");
    assertThat(issues.size()).isEqualTo(1);
  }

  private List<Issue> findIssues(String componentKey, String ruleKey) {
    return orchestrator.getServer().wsClient().issueClient().find(IssueQuery.create().components(componentKey).rules(ruleKey)).list();
  }
}
