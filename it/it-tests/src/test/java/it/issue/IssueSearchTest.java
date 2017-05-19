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
package it.issue;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.time.DateUtils;
import org.assertj.core.api.Fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.base.Paging;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.client.issue.SearchWsRequest;
import util.ItUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonarqube.ws.Issues.SearchWsResponse;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperty;
import static util.ItUtils.toDate;
import static util.ItUtils.verifyHttpException;

public class IssueSearchTest extends AbstractIssueTest {

  private static final String PROJECT_KEY = "com.sonarsource.it.samples:multi-modules-sample";
  private static final String PROJECT_KEY2 = "com.sonarsource.it.samples:multi-modules-sample2";

  private static int DEFAULT_PAGINATED_RESULTS = 100;
  private static int TOTAL_NB_ISSUES = 272;

  @BeforeClass
  public static void prepareData() {
    ORCHESTRATOR.resetData();

    ItUtils.restoreProfile(ORCHESTRATOR, IssueSearchTest.class.getResource("/issue/with-many-rules.xml"));

    // Launch 2 analysis to have more than 100 issues in total
    ORCHESTRATOR.getServer().provisionProject(PROJECT_KEY, PROJECT_KEY);
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(PROJECT_KEY, "xoo", "with-many-rules");
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-multi-modules-sample");

    ORCHESTRATOR.getServer().provisionProject(PROJECT_KEY2, PROJECT_KEY2);
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(PROJECT_KEY2, "xoo", "with-many-rules");
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-multi-modules-sample", "sonar.projectKey", PROJECT_KEY2);

    // Assign a issue to test search by assignee
    adminIssueClient().assign(searchRandomIssue().key(), "admin");

    // Resolve a issue to test search by status and by resolution
    adminIssueClient().doTransition(searchRandomIssue().key(), "resolve");
  }

  @Before
  public void resetProperties() throws Exception {
    setServerProperty(ORCHESTRATOR, "sonar.forceAuthentication", "false");
  }

  @Test
  public void search_all_issues() {
    assertThat(search(IssueQuery.create()).list()).hasSize(DEFAULT_PAGINATED_RESULTS);
  }

  @Test
  public void search_issues_by_component_roots() {
    assertThat(search(IssueQuery.create().componentRoots("com.sonarsource.it.samples:multi-modules-sample")).list()).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertThat(search(IssueQuery.create().componentRoots("com.sonarsource.it.samples:multi-modules-sample:module_a")).list()).hasSize(82);
    assertThat(search(IssueQuery.create().componentRoots("com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1")).list()).hasSize(36);

    assertThat(search(IssueQuery.create().componentRoots("unknown")).list()).isEmpty();
  }

  @Test
  public void search_issues_by_components() {
    assertThat(
      search(IssueQuery.create().components("com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo")).list())
        .hasSize(34);
    assertThat(search(IssueQuery.create().components("unknown")).list()).isEmpty();
  }

  @Test
  public void search_issues_by_severities() {
    assertThat(search(IssueQuery.create().severities("BLOCKER")).list()).hasSize(8);
    assertThat(search(IssueQuery.create().severities("CRITICAL")).list()).hasSize(8);
    assertThat(search(IssueQuery.create().severities("MAJOR")).list()).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertThat(search(IssueQuery.create().severities("MINOR")).list()).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertThat(search(IssueQuery.create().severities("INFO")).list()).hasSize(4);
  }

  @Test
  public void search_issues_by_statuses() {
    assertThat(search(IssueQuery.create().statuses("OPEN")).list()).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertThat(search(IssueQuery.create().statuses("RESOLVED")).list()).hasSize(1);
    assertThat(search(IssueQuery.create().statuses("CLOSED")).list()).isEmpty();
  }

  @Test
  public void search_issues_by_resolutions() {
    assertThat(search(IssueQuery.create().resolutions("FIXED")).list()).hasSize(1);
    assertThat(search(IssueQuery.create().resolutions("FALSE-POSITIVE")).list()).isEmpty();
    assertThat(search(IssueQuery.create().resolved(true)).list()).hasSize(1);
    assertThat(search(IssueQuery.create().resolved(false)).paging().total()).isEqualTo(TOTAL_NB_ISSUES - 1);
  }

  @Test
  public void search_issues_by_assignees() {
    assertThat(search(IssueQuery.create().assignees("admin")).list()).hasSize(1);
    assertThat(search(IssueQuery.create().assignees("unknown")).list()).isEmpty();
    assertThat(search(IssueQuery.create().assigned(true)).list()).hasSize(1);
    assertThat(search(IssueQuery.create().assigned(false)).paging().total()).isEqualTo(TOTAL_NB_ISSUES - 1);
  }

  @Test
  public void search_issues_by_rules() {
    assertThat(search(IssueQuery.create().rules("xoo:OneIssuePerLine")).list()).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertThat(search(IssueQuery.create().rules("xoo:OneIssuePerFile")).list()).hasSize(8);

    try {
      assertThat(search(IssueQuery.create().rules("unknown")).list()).isEmpty();
      fail();
    } catch (Exception e) {
      verifyHttpException(e, 400);
    }
  }

  /**
   * SONAR-2981
   */
  @Test
  public void search_issues_by_dates() {
    // issues have been created today
    Date today = toDate(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
    Date past = toDate("2013-01-01");
    Date future = toDate("2020-12-31");

    // createdAfter in the future => bad request
    try {
      search(IssueQuery.create().createdAfter(future)).list();
      Fail.fail("Expecting 400 from issues search WS");
    } catch (HttpException exception) {
      assertThat(exception.getMessage()).contains("Start bound cannot be in the future");
    }

    // after date
    assertThat(search(IssueQuery.create().createdAfter(today)).list().size()).isGreaterThan(0);
    assertThat(search(IssueQuery.create().createdAfter(past)).list().size()).isGreaterThan(0);

    // before
    assertThat(search(IssueQuery.create().createdBefore(future)).list().size()).isGreaterThan(0);
    assertThat(search(IssueQuery.create().createdBefore(past)).list()).isEmpty();

    // before and after
    assertThat(search(IssueQuery.create().createdBefore(future).createdAfter(past)).list().size()).isGreaterThan(0);

    // createdAfter > createdBefore => bad request
    try {
      search(IssueQuery.create().createdBefore(past).createdAfter(today)).list();
      Fail.fail("Expecting 400 from issues search WS");
    } catch (HttpException exception) {
      assertThat(exception.getMessage()).contains("Start bound cannot be larger or equal to end bound");
    }

  }

  /**
   * SONAR-5132
   */
  @Test
  public void search_issues_by_languages() {
    assertThat(search(IssueQuery.create().languages("xoo")).list()).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertThat(search(IssueQuery.create().languages("foo")).list()).isEmpty();
  }

  @Test
  public void paginate_results() {
    Issues issues = search(IssueQuery.create().pageSize(20).pageIndex(2));

    assertThat(issues.list()).hasSize(20);
    Paging paging = issues.paging();
    assertThat(paging.pageIndex()).isEqualTo(2);
    assertThat(paging.pageSize()).isEqualTo(20);
    assertThat(paging.total()).isEqualTo(TOTAL_NB_ISSUES);

    // SONAR-3257
    // return max page size results when using negative page size value
    assertThat(search(IssueQuery.create().pageSize(0)).list()).hasSize(TOTAL_NB_ISSUES);
    assertThat(search(IssueQuery.create().pageSize(-1)).list()).hasSize(TOTAL_NB_ISSUES);
  }

  @Test
  public void sort_results() {
    List<Issue> issues = search(IssueQuery.create().sort("SEVERITY").asc(false)).list();
    assertThat(issues.get(0).severity()).isEqualTo("BLOCKER");
    assertThat(issues.get(8).severity()).isEqualTo("CRITICAL");
    assertThat(issues.get(17).severity()).isEqualTo("MAJOR");
  }

  /**
   * SONAR-4563
   */
  @Test
  public void search_by_exact_creation_date() {
    final Issue issue = search(IssueQuery.create()).list().get(0);
    assertThat(issue.creationDate()).isNotNull();

    // search the issue key with the same date
    assertThat(search(IssueQuery.create().issues().issues(issue.key()).createdAt(issue.creationDate())).list()).hasSize(1);

    // search issue key with 1 second more and less should return nothing
    assertThat(search(IssueQuery.create().issues().issues(issue.key()).createdAt(DateUtils.addSeconds(issue.creationDate(), 1))).size()).isEqualTo(0);
    assertThat(search(IssueQuery.create().issues().issues(issue.key()).createdAt(DateUtils.addSeconds(issue.creationDate(), -1))).size()).isEqualTo(0);

    // search with future and past dates that do not match any issues
    assertThat(search(IssueQuery.create().createdAt(toDate("2020-01-01"))).size()).isEqualTo(0);
    assertThat(search(IssueQuery.create().createdAt(toDate("2010-01-01"))).size()).isEqualTo(0);
  }

  @Test
  public void return_issue_type() throws Exception {
    List<org.sonarqube.ws.Issues.Issue> issues = searchByRuleKey("xoo:OneBugIssuePerLine");
    assertThat(issues).isNotEmpty();
    org.sonarqube.ws.Issues.Issue issue = issues.get(0);
    assertThat(issue.getType()).isEqualTo(Common.RuleType.BUG);

    issues = searchByRuleKey("xoo:OneVulnerabilityIssuePerModule");
    assertThat(issues).isNotEmpty();
    issue = issues.get(0);
    assertThat(issue.getType()).isEqualTo(Common.RuleType.VULNERABILITY);

    issues = searchByRuleKey("xoo:OneIssuePerLine");
    assertThat(issues).isNotEmpty();
    issue = issues.get(0);
    assertThat(issue.getType()).isEqualTo(Common.RuleType.CODE_SMELL);
  }

  @Test
  public void search_issues_by_types() throws IOException {
    assertThat(searchIssues(new SearchWsRequest().setTypes(singletonList("CODE_SMELL"))).getPaging().getTotal()).isEqualTo(142);
    assertThat(searchIssues(new SearchWsRequest().setTypes(singletonList("BUG"))).getPaging().getTotal()).isEqualTo(122);
    assertThat(searchIssues(new SearchWsRequest().setTypes(singletonList("VULNERABILITY"))).getPaging().getTotal()).isEqualTo(8);
  }

  private List<org.sonarqube.ws.Issues.Issue> searchByRuleKey(String... ruleKey) throws IOException {
    return searchIssues(new SearchWsRequest().setRules(asList(ruleKey))).getIssuesList();
  }

  private SearchWsResponse searchIssues(SearchWsRequest request) throws IOException {
    return newAdminWsClient(ORCHESTRATOR).issues().search(request);
  }

}
