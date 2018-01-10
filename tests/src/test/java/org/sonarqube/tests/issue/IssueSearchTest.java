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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.assertj.core.api.Fail;
import org.assertj.core.api.ListAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.issues.SearchRequest;
import util.ItUtils;
import util.selenium.Consumer;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.time.DateUtils.addSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.wsclient.internal.EncodingUtils.toQueryParam;
import static org.sonarqube.ws.Issues.SearchWsResponse;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.setServerProperty;
import static util.ItUtils.toDate;

public class IssueSearchTest extends AbstractIssueTest {

  private static final String PROJECT_KEY = "com.sonarsource.it.samples:multi-modules-sample";
  private static final String PROJECT_KEY2 = "com.sonarsource.it.samples:multi-modules-sample2";

  private static int DEFAULT_PAGINATED_RESULTS = 100;
  private static int TOTAL_NB_ISSUES = 256;

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
  public void resetProperties() {
    setServerProperty(ORCHESTRATOR, "sonar.forceAuthentication", "false");
  }

  @Test
  public void search_all_issues() {
    assertSearch().hasSize(DEFAULT_PAGINATED_RESULTS);
  }

  @Test
  public void search_issues_by_component_roots() {
    assertSearch(r -> r.setComponentRoots("com.sonarsource.it.samples:multi-modules-sample")).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertSearch(r -> r.setComponentRoots("com.sonarsource.it.samples:multi-modules-sample:module_a")).hasSize(76);
    assertSearch(r -> r.setComponentRoots("com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1")).hasSize(35);

    assertThat(search(IssueQuery.create().componentRoots("unknown")).list()).isEmpty();
  }

  @Test
  public void search_issues_by_components() {
    assertSearch(r -> r.setComponents("com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1:src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo")).hasSize(33);
    assertSearch(r -> r.setComponents("unknown")).isEmpty();
  }

  @Test
  public void search_issues_by_severities() {
    assertSearch(r -> r.setSeverities(singletonList("BLOCKER"))).hasSize(8);
    assertSearch(r -> r.setSeverities(singletonList("CRITICAL"))).hasSize(8);
    assertSearch(r -> r.setSeverities(singletonList("MAJOR"))).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertSearch(r -> r.setSeverities(singletonList("MINOR"))).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertSearch(r -> r.setSeverities(singletonList("INFO"))).hasSize(4);
  }

  @Test
  public void search_issues_by_statuses() {
    assertSearch(r -> r.setStatuses(singletonList("OPEN"))).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertSearch(r -> r.setStatuses(singletonList("RESOLVED"))).hasSize(1);
    assertSearch(r -> r.setStatuses(singletonList("CLOSED"))).isEmpty();
  }

  @Test
  public void search_issues_by_resolutions() {
    assertSearch(r -> r.setResolutions(singletonList("FIXED"))).hasSize(1);
    assertSearch(r -> r.setResolutions(singletonList("FALSE-POSITIVE"))).isEmpty();
    assertSearch(r -> r.setResolved("true")).hasSize(1);
    assertThat(searchResponse(r -> r.setResolved("false")).getPaging().getTotal()).isEqualTo(TOTAL_NB_ISSUES - 1);
  }

  @Test
  public void search_issues_by_assignees() {
    assertSearch(r -> r.setAssignees(singletonList("admin"))).hasSize(1);
    assertSearch(r -> r.setAssignees(singletonList("unknown"))).isEmpty();
    assertSearch(r -> r.setAssigned("true")).hasSize(1);
    assertThat(searchResponse(r -> r.setAssigned("false")).getPaging().getTotal()).isEqualTo(TOTAL_NB_ISSUES - 1);
  }

  @Test
  public void search_issues_by_rules() {
    assertSearch(r -> r.setRules(singletonList("xoo:OneIssuePerLine"))).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertSearch(r -> r.setRules(singletonList("xoo:OneIssuePerFile"))).hasSize(8);

    try {
      searchResponse(r -> r.setRules(singletonList("unknown")));
      Assert.fail();
    } catch (HttpException e) {
      assertThat(e.getMessage()).contains("Invalid rule key: unknown");
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
      searchResponse(r -> r.setCreatedAfter(toDateString(future)));
      Fail.fail("Expecting 400 from issues search WS");
    } catch (HttpException exception) {
      assertThat(exception.getMessage()).contains("Start bound cannot be in the future");
    }

    // after date
    assertSearch(r -> r.setCreatedAfter(toDateString(today))).isNotEmpty();
    assertSearch(r -> r.setCreatedAfter(toDateString(past))).isNotEmpty();

    // before
    assertSearch(r -> r.setCreatedBefore(toDateString(future))).isNotEmpty();
    assertSearch(r -> r.setCreatedBefore(toDateString(past))).isEmpty();

    // before and after
    assertSearch(r -> r.setCreatedBefore(toDateString(future)).setCreatedAfter(toDateString(past))).isNotEmpty();

    // createdAfter > createdBefore => bad request
    try {
      searchResponse(r -> r.setCreatedBefore(toDateString(past)).setCreatedAfter(toDateString(today)));
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
    assertSearch(r -> r.setLanguages(singletonList("xoo"))).hasSize(DEFAULT_PAGINATED_RESULTS);
    assertSearch(r -> r.setLanguages(singletonList("foo"))).isEmpty();
  }

  @Test
  public void paginate_results() {
    SearchWsResponse issues = searchResponse(r -> r.setPs("20").setP("2"));

    assertThat(issues.getIssuesList()).hasSize(20);
    Common.Paging paging = issues.getPaging();
    assertThat(paging.getPageIndex()).isEqualTo(2);
    assertThat(paging.getPageSize()).isEqualTo(20);
    assertThat(paging.getTotal()).isEqualTo(TOTAL_NB_ISSUES);

    // SONAR-3257
    // return max page size results when using negative page size value
    assertSearch(r -> r.setPs("0")).hasSize(TOTAL_NB_ISSUES);
    assertSearch(r -> r.setPs("-1")).hasSize(TOTAL_NB_ISSUES);
  }

  @Test
  public void sort_results() {
    List<Issues.Issue> issues = searchResponse(r -> r.setS("SEVERITY").setAsc("false")).getIssuesList();
    assertThat(issues.get(0).getSeverity()).isEqualTo(Common.Severity.BLOCKER);
    assertThat(issues.get(8).getSeverity()).isEqualTo(Common.Severity.CRITICAL);
    assertThat(issues.get(17).getSeverity()).isEqualTo(Common.Severity.MAJOR);
  }

  /**
   * SONAR-4563
   */
  @Test
  public void search_by_exact_creation_date() {
    Issues.Issue issue = searchResponse().getIssues(0);
    assertThat(issue.getCreationDate()).isNotNull();

    // search the issue key with the same date
    assertSearch(r -> r.setIssues(singletonList(issue.getKey())).setCreatedAt(issue.getCreationDate())).hasSize(1);

    // search issue key with 1 second more and less should return nothing
    assertSearch(r -> r.setIssues(singletonList(issue.getKey())).setCreatedAt(toDateString(addSeconds(parse(issue.getCreationDate()), 1)))).isEmpty();
    assertSearch(r -> r.setIssues(singletonList(issue.getKey())).setCreatedAt(toDateString(addSeconds(parse(issue.getCreationDate()), -1)))).isEmpty();

    // search with future and past dates that do not match any issues
    assertSearch(r -> r.setCreatedAt(toDateString(toDate("2020-01-01")))).isEmpty();
    assertSearch(r -> r.setCreatedAt(toDateString(toDate("2010-01-01")))).isEmpty();
  }

  @Test
  public void return_issue_type() {
    SearchWsResponse issues = searchResponse(r -> r.setRules(singletonList("xoo:OneBugIssuePerLine")));
    assertThat(issues.getIssuesList()).isNotEmpty();
    org.sonarqube.ws.Issues.Issue issue = issues.getIssues(0);
    assertThat(issue.getType()).isEqualTo(Common.RuleType.BUG);

    issues = searchResponse(r -> r.setRules(singletonList("xoo:OneVulnerabilityIssuePerModule")));
    assertThat(issues.getIssuesList()).isNotEmpty();
    issue = issues.getIssues(0);
    assertThat(issue.getType()).isEqualTo(Common.RuleType.VULNERABILITY);

    issues = searchResponse(r -> r.setRules(singletonList("xoo:OneIssuePerLine")));
    assertThat(issues.getIssuesList()).isNotEmpty();
    issue = issues.getIssues(0);
    assertThat(issue.getType()).isEqualTo(Common.RuleType.CODE_SMELL);
  }

  @Test
  public void search_issues_by_types() {
    assertThat(searchResponse(r -> r.setTypes(singletonList("CODE_SMELL"))).getPaging().getTotal()).isEqualTo(142);
    assertThat(searchResponse(r -> r.setTypes(singletonList("BUG"))).getPaging().getTotal()).isEqualTo(106);
    assertThat(searchResponse(r -> r.setTypes(singletonList("VULNERABILITY"))).getPaging().getTotal()).isEqualTo(8);
  }

  private Date parse(String date) {
    try {
      return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(date);
    } catch (ParseException e) {
      throw new IllegalStateException(e);
    }
  }

  private String toDateString(Date future) {
    return toQueryParam(future, true);
  }

  @SafeVarargs
  private final ListAssert<org.sonarqube.ws.Issues.Issue> assertSearch(Consumer<SearchRequest>... consumers) {
    return assertThat(searchResponse(consumers).getIssuesList());
  }

  @SafeVarargs
  private final SearchWsResponse searchResponse(Consumer<SearchRequest>... consumers) {
    SearchRequest request = new SearchRequest();
    Arrays.stream(consumers).forEach(c -> c.accept(request));
    return newAdminWsClient(ORCHESTRATOR).issues().search(request);
  }
}
