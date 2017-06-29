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
package org.sonar.server.issue.index;

import java.util.Map;
import java.util.TimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.issue.IssueDocTesting;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.IssueQuery.Builder;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.permission.index.PermissionIndexerDao;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.view.index.ViewIndexDefinition;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.DEPRECATED_FACET_MODE_DEBT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_MODE_EFFORT;

public class IssueIndexDebtTest {

  @Rule
  public EsTester tester = new EsTester(new IssueIndexDefinition(new MapSettings().asConfig()), new ViewIndexDefinition(new MapSettings().asConfig()));

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private System2 system2 = System2.INSTANCE;
  private IssueIndex index;
  private IssueIndexer issueIndexer = new IssueIndexer(tester.client(), new IssueIteratorFactory(null));
  private PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(tester, issueIndexer);

  @Before
  public void setUp() {
    System2 system = mock(System2.class);
    when(system.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone("+01:00"));
    when(system.now()).thenReturn(System.currentTimeMillis());
    index = new IssueIndex(tester.client(), system, userSessionRule, new AuthorizationTypeSupport(userSessionRule));
  }

  @Test
  public void facets_on_projects() {
    OrganizationDto organizationDto = newOrganizationDto();
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto, "ABCD");
    ComponentDto project2 = ComponentTesting.newPrivateProjectDto(organizationDto, "EFGH");

    indexIssues(
      IssueDocTesting.newDoc("ISSUE1", ComponentTesting.newFileDto(project, null)).setEffort(10L),
      IssueDocTesting.newDoc("ISSUE2", ComponentTesting.newFileDto(project, null)).setEffort(10L),
      IssueDocTesting.newDoc("ISSUE3", ComponentTesting.newFileDto(project2, null)).setEffort(10L));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(),
      new SearchOptions().addFacets(newArrayList("projectUuids")));
    assertThat(result.getFacets().getNames()).containsOnly("projectUuids", FACET_MODE_EFFORT);
    assertThat(result.getFacets().get("projectUuids")).containsOnly(entry("ABCD", 20L), entry("EFGH", 10L));
    assertThat(result.getFacets().get(FACET_MODE_EFFORT)).containsOnly(entry("total", 30L));
  }

  @Test
  public void facets_on_components() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(newOrganizationDto(), "A");
    ComponentDto file1 = ComponentTesting.newFileDto(project, null, "ABCD");
    ComponentDto file2 = ComponentTesting.newFileDto(project, null, "BCDE");
    ComponentDto file3 = ComponentTesting.newFileDto(project, null, "CDEF");

    indexIssues(
      IssueDocTesting.newDoc("ISSUE1", project),
      IssueDocTesting.newDoc("ISSUE2", file1),
      IssueDocTesting.newDoc("ISSUE3", file2),
      IssueDocTesting.newDoc("ISSUE4", file2),
      IssueDocTesting.newDoc("ISSUE5", file3));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("fileUuids")));
    assertThat(result.getFacets().getNames()).containsOnly("fileUuids", FACET_MODE_EFFORT);
    assertThat(result.getFacets().get("fileUuids"))
      .containsOnly(entry("A", 10L), entry("ABCD", 10L), entry("BCDE", 20L), entry("CDEF", 10L));
    assertThat(result.getFacets().get(FACET_MODE_EFFORT)).containsOnly(entry("total", 50L));
  }

  @Test
  public void facets_on_directories() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(newOrganizationDto());
    ComponentDto file1 = ComponentTesting.newFileDto(project, null).setPath("src/main/xoo/F1.xoo");
    ComponentDto file2 = ComponentTesting.newFileDto(project, null).setPath("F2.xoo");

    indexIssues(
      IssueDocTesting.newDoc("ISSUE1", file1).setDirectoryPath("/src/main/xoo"),
      IssueDocTesting.newDoc("ISSUE2", file2).setDirectoryPath("/"));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("directories")));
    assertThat(result.getFacets().getNames()).containsOnly("directories", FACET_MODE_EFFORT);
    assertThat(result.getFacets().get("directories")).containsOnly(entry("/src/main/xoo", 10L), entry("/", 10L));
    assertThat(result.getFacets().get(FACET_MODE_EFFORT)).containsOnly(entry("total", 20L));
  }

  @Test
  public void facets_on_severities() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueDocTesting.newDoc("ISSUE1", file).setSeverity(Severity.INFO),
      IssueDocTesting.newDoc("ISSUE2", file).setSeverity(Severity.INFO),
      IssueDocTesting.newDoc("ISSUE3", file).setSeverity(Severity.MAJOR));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("severities")));
    assertThat(result.getFacets().getNames()).containsOnly("severities", FACET_MODE_EFFORT);
    assertThat(result.getFacets().get("severities")).containsOnly(entry("INFO", 20L), entry("MAJOR", 10L));
    assertThat(result.getFacets().get(FACET_MODE_EFFORT)).containsOnly(entry("total", 30L));
  }

  @Test
  public void facets_on_statuses() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueDocTesting.newDoc("ISSUE1", file).setStatus(Issue.STATUS_CLOSED),
      IssueDocTesting.newDoc("ISSUE2", file).setStatus(Issue.STATUS_CLOSED),
      IssueDocTesting.newDoc("ISSUE3", file).setStatus(Issue.STATUS_OPEN));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("statuses")));
    assertThat(result.getFacets().getNames()).containsOnly("statuses", FACET_MODE_EFFORT);
    assertThat(result.getFacets().get("statuses")).containsOnly(entry("CLOSED", 20L), entry("OPEN", 10L));
    assertThat(result.getFacets().get(FACET_MODE_EFFORT)).containsOnly(entry("total", 30L));
  }

  @Test
  public void facets_on_resolutions() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueDocTesting.newDoc("ISSUE1", file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      IssueDocTesting.newDoc("ISSUE2", file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      IssueDocTesting.newDoc("ISSUE3", file).setResolution(Issue.RESOLUTION_FIXED));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("resolutions")));
    assertThat(result.getFacets().getNames()).containsOnly("resolutions", FACET_MODE_EFFORT);
    assertThat(result.getFacets().get("resolutions")).containsOnly(entry("FALSE-POSITIVE", 20L), entry("FIXED", 10L));
    assertThat(result.getFacets().get(FACET_MODE_EFFORT)).containsOnly(entry("total", 30L));
  }

  @Test
  public void facets_on_languages() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = ComponentTesting.newFileDto(project, null);
    RuleKey ruleKey = RuleKey.of("repo", "X1");

    indexIssues(IssueDocTesting.newDoc("ISSUE1", file).setRuleKey(ruleKey.toString()).setLanguage("xoo").setEffort(10L));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("languages")));
    assertThat(result.getFacets().getNames()).containsOnly("languages", FACET_MODE_EFFORT);
    assertThat(result.getFacets().get("languages")).containsOnly(entry("xoo", 10L));
    assertThat(result.getFacets().get(FACET_MODE_EFFORT)).containsOnly(entry("total", 10L));
  }

  @Test
  public void facets_on_assignees() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueDocTesting.newDoc("ISSUE1", file).setAssignee("steph"),
      IssueDocTesting.newDoc("ISSUE2", file).setAssignee("simon"),
      IssueDocTesting.newDoc("ISSUE3", file).setAssignee("simon"),
      IssueDocTesting.newDoc("ISSUE4", file).setAssignee(null));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("assignees")));
    assertThat(result.getFacets().getNames()).containsOnly("assignees", FACET_MODE_EFFORT);
    assertThat(result.getFacets().get("assignees")).containsOnly(entry("steph", 10L), entry("simon", 20L), entry("", 10L));
    assertThat(result.getFacets().get(FACET_MODE_EFFORT)).containsOnly(entry("total", 40L));
  }

  @Test
  public void facets_on_authors() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueDocTesting.newDoc("ISSUE1", file).setAuthorLogin("steph"),
      IssueDocTesting.newDoc("ISSUE2", file).setAuthorLogin("simon"),
      IssueDocTesting.newDoc("ISSUE3", file).setAuthorLogin("simon"),
      IssueDocTesting.newDoc("ISSUE4", file).setAuthorLogin(null));

    SearchResult<IssueDoc> result = index.search(newQueryBuilder().build(), new SearchOptions().addFacets(newArrayList("authors")));
    assertThat(result.getFacets().getNames()).containsOnly("authors", FACET_MODE_EFFORT);
    assertThat(result.getFacets().get("authors")).containsOnly(entry("steph", 10L), entry("simon", 20L));
    assertThat(result.getFacets().get(FACET_MODE_EFFORT)).containsOnly(entry("total", 40L));
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

  @Test
  public void deprecated_debt_facets() {
    OrganizationDto organizationDto = newOrganizationDto();
    ComponentDto project = ComponentTesting.newPrivateProjectDto(organizationDto, "ABCD");
    ComponentDto project2 = ComponentTesting.newPrivateProjectDto(organizationDto, "EFGH");

    indexIssues(
      IssueDocTesting.newDoc("ISSUE1", ComponentTesting.newFileDto(project, null)).setEffort(10L),
      IssueDocTesting.newDoc("ISSUE2", ComponentTesting.newFileDto(project, null)).setEffort(10L),
      IssueDocTesting.newDoc("ISSUE3", ComponentTesting.newFileDto(project2, null)).setEffort(10L));

    SearchResult<IssueDoc> result = index.search(IssueQuery.builder().facetMode(DEPRECATED_FACET_MODE_DEBT).build(),
      new SearchOptions().addFacets(newArrayList("projectUuids")));
    assertThat(result.getFacets().getNames()).containsOnly("projectUuids", FACET_MODE_EFFORT);
    assertThat(result.getFacets().get("projectUuids")).containsOnly(entry("ABCD", 20L), entry("EFGH", 10L));
    assertThat(result.getFacets().get(FACET_MODE_EFFORT)).containsOnly(entry("total", 30L));
  }

  protected SearchOptions fixtureForCreatedAtFacet() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    IssueDoc issue0 = IssueDocTesting.newDoc("ISSUE0", file).setFuncCreationDate(DateUtils.parseDateTime("2011-04-25T01:05:13+0100"));
    IssueDoc issue1 = IssueDocTesting.newDoc("ISSUE1", file).setFuncCreationDate(DateUtils.parseDateTime("2014-09-01T12:34:56+0100"));
    IssueDoc issue2 = IssueDocTesting.newDoc("ISSUE2", file).setFuncCreationDate(DateUtils.parseDateTime("2014-09-01T23:46:00+0100"));
    IssueDoc issue3 = IssueDocTesting.newDoc("ISSUE3", file).setFuncCreationDate(DateUtils.parseDateTime("2014-09-02T12:34:56+0100"));
    IssueDoc issue4 = IssueDocTesting.newDoc("ISSUE4", file).setFuncCreationDate(DateUtils.parseDateTime("2014-09-05T12:34:56+0100"));
    IssueDoc issue5 = IssueDocTesting.newDoc("ISSUE5", file).setFuncCreationDate(DateUtils.parseDateTime("2014-09-20T12:34:56+0100"));
    IssueDoc issue6 = IssueDocTesting.newDoc("ISSUE6", file).setFuncCreationDate(DateUtils.parseDateTime("2015-01-18T12:34:56+0100"));

    indexIssues(issue0, issue1, issue2, issue3, issue4, issue5, issue6);

    return new SearchOptions().addFacets("createdAt");
  }

  private void indexIssues(IssueDoc... issues) {
    issueIndexer.index(asList(issues).iterator());
    for (IssueDoc issue : issues) {
      addIssueAuthorization(issue.projectUuid());
    }
  }

  private void addIssueAuthorization(String projectUuid) {
    PermissionIndexerDao.Dto access = new PermissionIndexerDao.Dto(projectUuid, system2.now(), Qualifiers.PROJECT);
    access.allowAnyone();
    authorizationIndexerTester.allow(access);
  }

  private Builder newQueryBuilder() {
    return IssueQuery.builder().facetMode(FACET_MODE_EFFORT);
  }
}
