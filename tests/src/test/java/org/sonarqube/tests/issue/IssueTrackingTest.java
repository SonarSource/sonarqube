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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Issues.SearchWsResponse;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.issues.SearchRequest;
import util.ItUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.runProjectAnalysis;
import static util.ItUtils.toDate;

public class IssueTrackingTest extends AbstractIssueTest {

  private static final String SAMPLE_PROJECT_KEY = "sample";

  private static final String OLD_DATE = "2014-03-01";
  private static final Date NEW_DATE = new Date();
  private static final String NEW_DATE_STR = new SimpleDateFormat("yyyy-MM-dd").format(NEW_DATE);

  private static WsClient adminClient;

  @Before
  public void prepareData() {
    ORCHESTRATOR.resetData();
    ItUtils.restoreProfile(ORCHESTRATOR, getClass().getResource("/issue/issue-on-tag-foobar.xml"));
    ItUtils.restoreProfile(ORCHESTRATOR, getClass().getResource("/issue/IssueTrackingTest/one-issue-per-module-profile.xml"));
    ORCHESTRATOR.getServer().provisionProject(SAMPLE_PROJECT_KEY, SAMPLE_PROJECT_KEY);
    adminClient = newAdminWsClient(ORCHESTRATOR);
  }

  @Test
  public void close_issues_on_removed_components() {
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(SAMPLE_PROJECT_KEY, "xoo", "issue-on-tag-foobar");

    // version 1
    runProjectAnalysis(ORCHESTRATOR, "issue/xoo-tracking-v1",
      "sonar.projectDate", OLD_DATE);

    List<Issue> issues = searchUnresolvedIssuesByComponent("sample:src/main/xoo/sample/Sample.xoo");
    assertThat(issues).hasSize(1);

    // version 2
    runProjectAnalysis(ORCHESTRATOR, "issue/xoo-tracking-v1",
      "sonar.projectDate", NEW_DATE_STR,
      "sonar.exclusions", "**/*.xoo");

    issues = searchIssues(new SearchRequest().setProjects(singletonList("sample"))).getIssuesList();
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).getStatus()).isEqualTo("CLOSED");
    assertThat(issues.get(0).getResolution()).isEqualTo("FIXED");
  }

  /**
   * SONAR-3072
   */
  @Test
  public void track_issues_based_on_blocks_recognition() {
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(SAMPLE_PROJECT_KEY, "xoo", "issue-on-tag-foobar");

    // version 1
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(SAMPLE_PROJECT_KEY, "xoo", "issue-on-tag-foobar");
    runProjectAnalysis(ORCHESTRATOR, "issue/xoo-tracking-v1",
      "sonar.projectDate", OLD_DATE);

    List<Issue> issues = searchUnresolvedIssuesByComponent("sample:src/main/xoo/sample/Sample.xoo");
    assertThat(issues).hasSize(1);
    Date issueDate = toDate(issues.iterator().next().getCreationDate());

    // version 2
    runProjectAnalysis(ORCHESTRATOR, "issue/xoo-tracking-v2",
      "sonar.projectDate", NEW_DATE_STR);

    issues = searchUnresolvedIssuesByComponent("sample:src/main/xoo/sample/Sample.xoo");
    assertThat(issues).hasSize(3);

    // issue created during the first scan and moved during the second scan
    assertThat(toDate(getIssueOnLine(6, "xoo:HasTag", issues).getCreationDate())).isEqualTo(issueDate);

    // issues created during the second scan
    assertThat(toDate(getIssueOnLine(10, "xoo:HasTag", issues).getCreationDate())).isAfter(issueDate);
    assertThat(toDate(getIssueOnLine(14, "xoo:HasTag", issues).getCreationDate())).isAfter(issueDate);
  }

  /**
   * SONAR-4310
   */
  @Test
  public void track_existing_unchanged_issues_on_module() {
    // The custom rule on module is enabled

    ORCHESTRATOR.getServer().associateProjectToQualityProfile(SAMPLE_PROJECT_KEY, "xoo", "one-issue-per-module");
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample");

    // Only one issue is created
    assertThat(searchIssues(new SearchRequest()).getIssuesList()).hasSize(1);
    Issue issue = getRandomIssue();

    // Re analysis of the same project
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-sample");

    // No new issue should be created
    assertThat(searchIssues(new SearchRequest()).getIssuesList()).hasSize(1);

    // The issue on module should stay open and be the same from the first analysis
    Issue reloadIssue = getIssueByKey(issue.getKey());
    assertThat(reloadIssue.getCreationDate()).isEqualTo(issue.getCreationDate());
    assertThat(reloadIssue.getStatus()).isEqualTo("OPEN");
    assertThat(reloadIssue.hasResolution()).isFalse();
  }

  /**
   * SONAR-4310
   */
  @Test
  public void track_existing_unchanged_issues_on_multi_modules() {
    // The custom rule on module is enabled
    ORCHESTRATOR.getServer().provisionProject("com.sonarsource.it.samples:multi-modules-sample", "com.sonarsource.it.samples:multi-modules-sample");
    ORCHESTRATOR.getServer().associateProjectToQualityProfile("com.sonarsource.it.samples:multi-modules-sample", "xoo", "one-issue-per-module");
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-multi-modules-sample");

    // One issue by module are created
    List<Issue> issues = searchIssues(new SearchRequest()).getIssuesList();
    assertThat(issues).hasSize(4);

    // Re analysis of the same project
    runProjectAnalysis(ORCHESTRATOR, "shared/xoo-multi-modules-sample");

    // No new issue should be created
    assertThat(searchIssues(new SearchRequest()).getIssuesList()).hasSize(issues.size());

    // Issues on modules should stay open and be the same from the first analysis
    for (Issue issue : issues) {
      Issue reloadIssue = getIssueByKey(issue.getKey());
      assertThat(reloadIssue.getStatus()).isEqualTo("OPEN");
      assertThat(reloadIssue.hasResolution()).isFalse();
      assertThat(reloadIssue.getCreationDate()).isEqualTo(issue.getCreationDate());
      assertThat(reloadIssue.getUpdateDate()).isEqualTo(issue.getUpdateDate());
    }
  }

  @Test
  public void track_file_moves_based_on_identical_content() {
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(SAMPLE_PROJECT_KEY, "xoo", "issue-on-tag-foobar");

    // version 1
    runProjectAnalysis(ORCHESTRATOR, "issue/xoo-tracking-v1",
      "sonar.projectDate", OLD_DATE);

    List<Issue> issues = searchUnresolvedIssuesByComponent("sample:src/main/xoo/sample/Sample.xoo");
    assertThat(issues).hasSize(1);
    Issue issueOnSample = issues.iterator().next();

    // version 2
    runProjectAnalysis(ORCHESTRATOR, "issue/xoo-tracking-v3",
      "sonar.projectDate", NEW_DATE_STR);

    assertThat(searchUnresolvedIssuesByComponent("sample:src/main/xoo/sample/Sample.xoo")).isEmpty();

    issues = searchUnresolvedIssuesByComponent("sample:src/main/xoo/sample/Sample2.xoo");
    assertThat(issues).hasSize(1);
    Issue issueOnSample2 = issues.get(0);
    assertThat(issueOnSample2.getKey()).isEqualTo(issueOnSample.getKey());
    assertThat(issueOnSample2.getCreationDate()).isEqualTo(issueOnSample.getCreationDate());
    assertThat(issueOnSample2.getUpdateDate()).isNotEqualTo(issueOnSample.getUpdateDate());
    assertThat(issueOnSample2.getStatus()).isEqualTo("OPEN");
  }

  private Issue getIssueOnLine(int line, String rule, List<Issue> issues) {
    return issues.stream()
      .filter(issue -> issue.getRule().equals(rule))
      .filter(issue -> issue.getLine() == line)
      .findFirst().orElseThrow(IllegalArgumentException::new);
  }

  private List<Issue> searchUnresolvedIssuesByComponent(String componentKey) {
    return searchIssues(new SearchRequest().setComponentKeys(singletonList(componentKey)).setResolved("false")).getIssuesList();
  }

  private static Issue getRandomIssue() {
    return searchIssues(new SearchRequest()).getIssues(0);
  }

  private static Issue getIssueByKey(String issueKey) {
    SearchWsResponse search = searchIssues(new SearchRequest().setIssues(singletonList(issueKey)));
    assertThat(search.getTotal()).isEqualTo(1);
    return search.getIssues(0);
  }

  private static SearchWsResponse searchIssues(SearchRequest request) {
    return adminClient.issues().search(request);
  }

}
