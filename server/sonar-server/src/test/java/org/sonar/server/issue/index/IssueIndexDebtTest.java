/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue.index;

import java.util.Arrays;
import java.util.Map;
import java.util.TimeZone;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.IssueQuery.Builder;
import org.sonar.server.issue.IssueTesting;
import org.sonarqube.ws.client.issue.IssueFilterParameters;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.view.index.ViewIndexDefinition;
import org.sonar.server.view.index.ViewIndexer;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssueIndexDebtTest {

  @ClassRule
  public static EsTester tester = new EsTester().addDefinitions(new IssueIndexDefinition(new Settings()), new ViewIndexDefinition(new Settings()));
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  IssueIndex index;

  IssueIndexer issueIndexer;
  IssueAuthorizationIndexer issueAuthorizationIndexer;
  ViewIndexer viewIndexer;

  @Before
  public void setUp() {
    tester.truncateIndices();
    issueIndexer = new IssueIndexer(null, tester.client());
    issueAuthorizationIndexer = new IssueAuthorizationIndexer(null, tester.client());
    viewIndexer = new ViewIndexer(null, tester.client());
    System2 system = mock(System2.class);
    when(system.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone("+01:00"));
    when(system.now()).thenReturn(System.currentTimeMillis());

    index = new IssueIndex(tester.client(), system, userSessionRule);

  }

  @Test
  public void facets_on_projects() {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD");
    ComponentDto project2 = ComponentTesting.newProjectDto("EFGH");

    indexIssues(
      IssueTesting.newDoc("ISSUE1", ComponentTesting.newFileDto(project)),
      IssueTesting.newDoc("ISSUE2", ComponentTesting.newFileDto(project)),
      IssueTesting.newDoc("ISSUE3", ComponentTesting.newFileDto(project2)));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("projectUuids")));
    assertThat(result.getFacets().getNames()).containsOnly("projectUuids", "debt");
    assertThat(result.getFacets().get("projectUuids")).containsOnly(entry("ABCD", 20L), entry("EFGH", 10L));
    assertThat(result.getFacets().get("debt")).containsOnly(entry("total", 30L));
  }

  @Test
  public void facets_on_components() {
    ComponentDto project = ComponentTesting.newProjectDto("A");
    ComponentDto file1 = ComponentTesting.newFileDto(project, "ABCD");
    ComponentDto file2 = ComponentTesting.newFileDto(project, "BCDE");
    ComponentDto file3 = ComponentTesting.newFileDto(project, "CDEF");

    indexIssues(
      IssueTesting.newDoc("ISSUE1", project),
      IssueTesting.newDoc("ISSUE2", file1),
      IssueTesting.newDoc("ISSUE3", file2),
      IssueTesting.newDoc("ISSUE4", file2),
      IssueTesting.newDoc("ISSUE5", file3));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("fileUuids")));
    assertThat(result.getFacets().getNames()).containsOnly("fileUuids", "debt");
    assertThat(result.getFacets().get("fileUuids"))
      .containsOnly(entry("A", 10L), entry("ABCD", 10L), entry("BCDE", 20L), entry("CDEF", 10L));
    assertThat(result.getFacets().get("debt")).containsOnly(entry("total", 50L));
  }

  @Test
  public void facets_on_directories() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file1 = ComponentTesting.newFileDto(project).setPath("src/main/xoo/F1.xoo");
    ComponentDto file2 = ComponentTesting.newFileDto(project).setPath("F2.xoo");

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file1).setDirectoryPath("/src/main/xoo"),
      IssueTesting.newDoc("ISSUE2", file2).setDirectoryPath("/"));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("directories")));
    assertThat(result.getFacets().getNames()).containsOnly("directories", "debt");
    assertThat(result.getFacets().get("directories")).containsOnly(entry("/src/main/xoo", 10L), entry("/", 10L));
    assertThat(result.getFacets().get("debt")).containsOnly(entry("total", 20L));
  }

  @Test
  public void facets_on_severities() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setSeverity(Severity.INFO),
      IssueTesting.newDoc("ISSUE2", file).setSeverity(Severity.INFO),
      IssueTesting.newDoc("ISSUE3", file).setSeverity(Severity.MAJOR));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("severities")));
    assertThat(result.getFacets().getNames()).containsOnly("severities", "debt");
    assertThat(result.getFacets().get("severities")).containsOnly(entry("INFO", 20L), entry("MAJOR", 10L));
    assertThat(result.getFacets().get("debt")).containsOnly(entry("total", 30L));
  }

  @Test
  public void facets_on_statuses() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setStatus(Issue.STATUS_CLOSED),
      IssueTesting.newDoc("ISSUE2", file).setStatus(Issue.STATUS_CLOSED),
      IssueTesting.newDoc("ISSUE3", file).setStatus(Issue.STATUS_OPEN));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("statuses")));
    assertThat(result.getFacets().getNames()).containsOnly("statuses", "debt");
    assertThat(result.getFacets().get("statuses")).containsOnly(entry("CLOSED", 20L), entry("OPEN", 10L));
    assertThat(result.getFacets().get("debt")).containsOnly(entry("total", 30L));
  }

  @Test
  public void facets_on_resolutions() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      IssueTesting.newDoc("ISSUE2", file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      IssueTesting.newDoc("ISSUE3", file).setResolution(Issue.RESOLUTION_FIXED));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("resolutions")));
    assertThat(result.getFacets().getNames()).containsOnly("resolutions", "debt");
    assertThat(result.getFacets().get("resolutions")).containsOnly(entry("FALSE-POSITIVE", 20L), entry("FIXED", 10L));
    assertThat(result.getFacets().get("debt")).containsOnly(entry("total", 30L));
  }

  @Test
  public void facets_on_action_plans() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setActionPlanKey("plan1"),
      IssueTesting.newDoc("ISSUE2", file).setActionPlanKey("plan2"));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("actionPlans")));
    assertThat(result.getFacets().getNames()).containsOnly("actionPlans", "debt");
    assertThat(result.getFacets().get("actionPlans")).containsOnly(entry("plan1", 10L), entry("plan2", 10L));
    assertThat(result.getFacets().get("debt")).containsOnly(entry("total", 20L));
  }

  @Test
  public void facets_on_languages() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    RuleKey ruleKey = RuleKey.of("repo", "X1");

    indexIssues(IssueTesting.newDoc("ISSUE1", file).setRuleKey(ruleKey.toString()).setLanguage("xoo"));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("languages")));
    assertThat(result.getFacets().getNames()).containsOnly("languages", "debt");
    assertThat(result.getFacets().get("languages")).containsOnly(entry("xoo", 10L));
    assertThat(result.getFacets().get("debt")).containsOnly(entry("total", 10L));
  }

  @Test
  public void facets_on_assignees() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAssignee("steph"),
      IssueTesting.newDoc("ISSUE2", file).setAssignee("simon"),
      IssueTesting.newDoc("ISSUE3", file).setAssignee("simon"),
      IssueTesting.newDoc("ISSUE4", file).setAssignee(null));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("assignees")));
    assertThat(result.getFacets().getNames()).containsOnly("assignees", "debt");
    assertThat(result.getFacets().get("assignees")).containsOnly(entry("steph", 10L), entry("simon", 20L), entry("", 10L));
    assertThat(result.getFacets().get("debt")).containsOnly(entry("total", 40L));
  }

  @Test
  public void facets_on_authors() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAuthorLogin("steph"),
      IssueTesting.newDoc("ISSUE2", file).setAuthorLogin("simon"),
      IssueTesting.newDoc("ISSUE3", file).setAuthorLogin("simon"),
      IssueTesting.newDoc("ISSUE4", file).setAuthorLogin(null));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("authors")));
    assertThat(result.getFacets().getNames()).containsOnly("authors", "debt");
    assertThat(result.getFacets().get("authors")).containsOnly(entry("steph", 10L), entry("simon", 20L));
    assertThat(result.getFacets().get("debt")).containsOnly(entry("total", 40L));
  }

  @Test
  public void facet_on_created_at() {
    SearchOptions SearchOptions = fixtureForCreatedAtFacet();

    Map<String, Long> createdAt = index.search(newQueryBuilder()
      .createdBefore(DateUtils.parseDateTime("2016-01-01T00:00:00+0100")).build(),
      SearchOptions).getFacets().get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2011-01-01T00:00:00+0000", 10L),
      entry("2012-01-01T00:00:00+0000", 0L),
      entry("2013-01-01T00:00:00+0000", 0L),
      entry("2014-01-01T00:00:00+0000", 50L),
      entry("2015-01-01T00:00:00+0000", 10L));
  }

  protected SearchOptions fixtureForCreatedAtFacet() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    IssueDoc issue0 = IssueTesting.newDoc("ISSUE0", file).setFuncCreationDate(DateUtils.parseDateTime("2011-04-25T01:05:13+0100"));
    IssueDoc issue1 = IssueTesting.newDoc("ISSUE1", file).setFuncCreationDate(DateUtils.parseDateTime("2014-09-01T12:34:56+0100"));
    IssueDoc issue2 = IssueTesting.newDoc("ISSUE2", file).setFuncCreationDate(DateUtils.parseDateTime("2014-09-01T23:46:00+0100"));
    IssueDoc issue3 = IssueTesting.newDoc("ISSUE3", file).setFuncCreationDate(DateUtils.parseDateTime("2014-09-02T12:34:56+0100"));
    IssueDoc issue4 = IssueTesting.newDoc("ISSUE4", file).setFuncCreationDate(DateUtils.parseDateTime("2014-09-05T12:34:56+0100"));
    IssueDoc issue5 = IssueTesting.newDoc("ISSUE5", file).setFuncCreationDate(DateUtils.parseDateTime("2014-09-20T12:34:56+0100"));
    IssueDoc issue6 = IssueTesting.newDoc("ISSUE6", file).setFuncCreationDate(DateUtils.parseDateTime("2015-01-18T12:34:56+0100"));

    indexIssues(issue0, issue1, issue2, issue3, issue4, issue5, issue6);

    return new SearchOptions().addFacets("createdAt");
  }

  private void indexIssues(IssueDoc... issues) {
    issueIndexer.index(Arrays.asList(issues).iterator());
    for (IssueDoc issue : issues) {
      addIssueAuthorization(issue.projectUuid(), DefaultGroups.ANYONE, null);
    }
  }

  private void addIssueAuthorization(String projectUuid, @Nullable String group, @Nullable String user) {
    issueAuthorizationIndexer.index(newArrayList(new IssueAuthorizationDao.Dto(projectUuid, 1).addGroup(group).addUser(user)));
  }

  private Builder newQueryBuilder() {
    return IssueQuery.builder(userSessionRule).facetMode(IssueFilterParameters.FACET_MODE_DEBT);
  }
}
