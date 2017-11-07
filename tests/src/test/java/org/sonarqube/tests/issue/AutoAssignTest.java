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
package org.sonarqube.tests.issue;

import com.sonar.orchestrator.Orchestrator;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Category2Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.client.issue.SearchWsRequest;
import util.ProjectAnalysis;
import util.ProjectAnalysisRule;

import static com.google.common.base.Strings.nullToEmpty;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class AutoAssignTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;
  @Rule
  public Tester tester = new Tester(orchestrator);
  @Rule
  public final ProjectAnalysisRule projectAnalysisRule = ProjectAnalysisRule.from(orchestrator);

  private ProjectAnalysis projectAnalysis;

  @Before
  public void setup() {
    String qualityProfileKey = projectAnalysisRule.registerProfile("/issue/IssueActionTest/xoo-one-issue-per-line-profile.xml");
    String projectKey = projectAnalysisRule.registerProject("issue/AutoAssignTest");
    projectAnalysis = projectAnalysisRule.newProjectAnalysis(projectKey)
      .withQualityProfile(qualityProfileKey)
      .withProperties("sonar.scm.disabled", "false", "sonar.scm.provider", "xoo");
  }

  @Test
  public void auto_assign_issues_to_user() throws Exception {
    // verify that login matches, case-sensitive
    createUser("user1", "User 1", "user1@email.com");
    createUser("USER2", "User 2", "user2@email.com");
    // verify that name is not used to match, whatever the case
    createUser("user3", "User 3", "user3@email.com");
    createUser("user4", "USER 4", "user4@email.com");
    // verify that email matches, case-insensitive
    createUser("user5", "User 5", "user5@email.com");
    createUser("user6", "User 6", "USER6@email.COM");
    // verify that SCM account matches, case-insensitive
    createUser("user7", "User 7", "user7@email.com", "user7ScmAccount");
    createUser("user8", "User 8", "user8@email.com", "user8SCMaccOUNT");
    // SCM accounts longer than 255
    createUser("user9", "User 9", "user9@email.com", IntStream.range(0, 256).mapToObj(i -> "s").collect(Collectors.joining()));

    projectAnalysis.run();

    List<Issues.Issue> issues = tester.wsClient().issues().search(
      new SearchWsRequest().setComponentKeys(singletonList("AutoAssignTest:src/sample.xoo")).setSort("FILE_LINE")).getIssuesList();
    // login match, case-sensitive
    verifyIssueAssignee(issues, 1, "user1");
    verifyIssueAssignee(issues, 2, null);
    // user name is not used to match
    verifyIssueAssignee(issues, 3, null);
    verifyIssueAssignee(issues, 4, null);
    // email match, case-insensitive
    verifyIssueAssignee(issues, 5, "user5");
    verifyIssueAssignee(issues, 6, "user6");
    // SCM account match, case-insensitive
    verifyIssueAssignee(issues, 7, "user7");
    verifyIssueAssignee(issues, 8, "user8");
    // SCM accounts longer than 255 chars
    verifyIssueAssignee(issues, 10, "user9");
  }

  private static void verifyIssueAssignee(List<Issues.Issue> issues, int line, @Nullable String expectedAssignee) {
    assertThat(issues.get(line - 1).getAssignee()).isEqualTo(nullToEmpty(expectedAssignee));
  }

  @Test
  public void auto_assign_issues_to_default_assignee() throws Exception {
    createUser("user1", "User 1", "user1@email.com");
    createUser("user2", "User 2", "user2@email.com");
    tester.settings().setGlobalSetting("sonar.issues.defaultAssigneeLogin", "user2");
    projectAnalysis.run();

    // user1 is assigned to his issues. All other issues are assigned to the default assignee.
    assertThat(search(new SearchWsRequest().setAssignees(singletonList("user1")))).hasSize(1);
    assertThat(search(new SearchWsRequest().setAssignees(singletonList("user2")))).hasSize(9);
    assertThat(search(new SearchWsRequest().setAssigned(false))).isEmpty();
  }

  /**
   * SONAR-7098
   *
   * Given two versions of same project:
   * v1: issue, but no SCM data
   * v2: old issue and SCM data
   * Expected: all issues should be associated with authors
   */
  @Test
  public void update_author_and_assignee_when_scm_is_activated() {
    createUser("user1", "User 1", "user1@email.com");

    // Run a first analysis without SCM
    projectAnalysis.withProperties("sonar.scm.disabled", "true").run();
    List<Issues.Issue> issues = search(new SearchWsRequest());
    assertThat(issues).isNotEmpty();

    // No author and assignee are set
    assertThat(issues)
      .extracting(Issues.Issue::getLine, Issues.Issue::getAuthor)
      .containsExactlyInAnyOrder(
        tuple(1, ""),
        tuple(2, ""),
        tuple(3, ""),
        tuple(4, ""),
        tuple(5, ""),
        tuple(6, ""),
        tuple(7, ""),
        tuple(8, ""),
        tuple(9, ""),
        tuple(10, ""));
    assertThat(search(new SearchWsRequest().setAssigned(true))).isEmpty();

    // Run a second analysis with SCM
    projectAnalysis.run();
    issues = search(new SearchWsRequest());
    assertThat(issues).isNotEmpty();

    // Authors and assignees are set
    assertThat(issues)
      .extracting(Issues.Issue::getLine, Issues.Issue::getAuthor)
      .containsExactlyInAnyOrder(
        tuple(1, "user1"),
        tuple(2, "user2"),
        tuple(3, "user3name"),
        tuple(4, "user4name"),
        tuple(5, "user5@email.com"),
        tuple(6, "user6@email.com"),
        tuple(7, "user7scmaccount"),
        tuple(8, "user8scmaccount"),
        tuple(9, "user8scmaccount"),
        // SONAR-8727
        tuple(10, ""));
    assertThat(search(new SearchWsRequest().setAssignees(singletonList("user1")))).hasSize(1);
  }

  private List<Issues.Issue> search(SearchWsRequest request) {
    return tester.wsClient().issues().search(request).getIssuesList();
  }

  private void createUser(String login, String name, String email, String... scmAccounts) {
    tester.users().generate(u -> u.setLogin(login).setName(name).setEmail(email).setScmAccounts(Arrays.asList(scmAccounts)));
  }

}
