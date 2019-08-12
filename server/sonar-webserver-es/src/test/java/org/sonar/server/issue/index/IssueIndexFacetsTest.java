/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.elasticsearch.action.search.SearchResponse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.permission.index.IndexPermissions;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.TimeZone.getTimeZone;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.rules.ExpectedException.none;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_REMOVED;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.INFO;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.server.issue.IssueDocTesting.newDoc;

public class IssueIndexFacetsTest {

  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = none();
  private System2 system2 = new TestSystem2().setNow(1_500_000_000_000L).setDefaultTimeZone(getTimeZone("GMT-01:00"));
  @Rule
  public DbTester db = DbTester.create(system2);

  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()));
  private PermissionIndexerTester authorizationIndexer = new PermissionIndexerTester(es, issueIndexer);

  private IssueIndex underTest = new IssueIndex(es.client(), system2, userSessionRule, new WebAuthorizationTypeSupport(userSessionRule));

  @Test
  public void facet_on_projectUuids() {
    OrganizationDto organizationDto = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(organizationDto, "ABCD");
    ComponentDto project2 = newPrivateProjectDto(organizationDto, "EFGH");

    indexIssues(
      newDoc("I1", newFileDto(project, null)),
      newDoc("I2", newFileDto(project, null)),
      newDoc("I3", newFileDto(project2, null)));

    assertThatFacetHasExactly(IssueQuery.builder(), "projects", entry("ABCD", 2L), entry("EFGH", 1L));
  }

  @Test
  public void facet_on_projectUuids_return_100_entries_plus_selected_values() {
    OrganizationDto organizationDto = newOrganizationDto();
    indexIssues(rangeClosed(1, 110).mapToObj(i -> newDoc(newPrivateProjectDto(organizationDto, "a" + i))).toArray(IssueDoc[]::new));
    IssueDoc issue1 = newDoc(newPrivateProjectDto(organizationDto, "project1"));
    IssueDoc issue2 = newDoc(newPrivateProjectDto(organizationDto, "project2"));
    indexIssues(issue1, issue2);

    assertThatFacetHasSize(IssueQuery.builder().build(), "projects", 100);
    assertThatFacetHasSize(IssueQuery.builder().projectUuids(asList(issue1.projectUuid(), issue2.projectUuid())).build(), "projects", 102);
  }

  @Test
  public void facets_on_files() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto(), "A");
    ComponentDto file1 = newFileDto(project, null, "ABCD");
    ComponentDto file2 = newFileDto(project, null, "BCDE");
    ComponentDto file3 = newFileDto(project, null, "CDEF");

    indexIssues(
      newDoc("I1", project),
      newDoc("I2", file1),
      newDoc("I3", file2),
      newDoc("I4", file2),
      newDoc("I5", file3));

    assertThatFacetHasOnly(IssueQuery.builder(), "fileUuids", entry("A", 1L), entry("ABCD", 1L), entry("BCDE", 2L), entry("CDEF", 1L));
  }

  @Test
  public void facet_on_files_return_100_entries_plus_selected_values() {
    OrganizationDto organizationDto = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(organizationDto);
    indexIssues(rangeClosed(1, 110).mapToObj(i -> newDoc(newFileDto(project, null, "a" + i))).toArray(IssueDoc[]::new));
    IssueDoc issue1 = newDoc(newFileDto(project, null, "file1"));
    IssueDoc issue2 = newDoc(newFileDto(project, null, "file2"));
    indexIssues(issue1, issue2);

    assertThatFacetHasSize(IssueQuery.builder().build(), "fileUuids", 100);
    assertThatFacetHasSize(IssueQuery.builder().fileUuids(asList(issue1.componentUuid(), issue2.componentUuid())).build(), "fileUuids", 102);
  }

  @Test
  public void facets_on_directories() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file1 = newFileDto(project, null).setPath("src/main/xoo/F1.xoo");
    ComponentDto file2 = newFileDto(project, null).setPath("F2.xoo");

    indexIssues(
      newDoc("I1", file1).setDirectoryPath("/src/main/xoo"),
      newDoc("I2", file2).setDirectoryPath("/"));

    assertThatFacetHasOnly(IssueQuery.builder(), "directories", entry("/src/main/xoo", 1L), entry("/", 1L));
  }

  @Test
  public void facet_on_directories_return_100_entries_plus_selected_values() {
    OrganizationDto organizationDto = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(organizationDto);
    indexIssues(rangeClosed(1, 110).mapToObj(i -> newDoc(newFileDto(project, newDirectory(project, "dir" + i))).setDirectoryPath("a" + i)).toArray(IssueDoc[]::new));
    IssueDoc issue1 = newDoc(newFileDto(project, newDirectory(project, "path1"))).setDirectoryPath("directory1");
    IssueDoc issue2 = newDoc(newFileDto(project, newDirectory(project, "path2"))).setDirectoryPath("directory2");
    indexIssues(issue1, issue2);

    assertThatFacetHasSize(IssueQuery.builder().build(), "directories", 100);
    assertThatFacetHasSize(IssueQuery.builder().directories(asList(issue1.directoryPath(), issue2.directoryPath())).build(), "directories", 102);
  }

  @Test
  public void facets_on_severities() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setSeverity(INFO),
      newDoc("I2", file).setSeverity(INFO),
      newDoc("I3", file).setSeverity(MAJOR));

    assertThatFacetHasOnly(IssueQuery.builder(), "severities", entry("INFO", 2L), entry("MAJOR", 1L));
  }

  @Test
  public void facet_on_severities_return_5_entries_max() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I2", file).setSeverity(INFO),
      newDoc("I1", file).setSeverity(MINOR),
      newDoc("I3", file).setSeverity(MAJOR),
      newDoc("I4", file).setSeverity(CRITICAL),
      newDoc("I5", file).setSeverity(BLOCKER),
      newDoc("I6", file).setSeverity(MAJOR));

    assertThatFacetHasSize(IssueQuery.builder().build(), "severities", 5);
  }

  @Test
  public void facets_on_statuses() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setStatus(STATUS_CLOSED),
      newDoc("I2", file).setStatus(STATUS_CLOSED),
      newDoc("I3", file).setStatus(STATUS_OPEN));

    assertThatFacetHasOnly(IssueQuery.builder(), "statuses", entry("CLOSED", 2L), entry("OPEN", 1L));
  }

  @Test
  public void facet_on_statuses_return_5_entries_max() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setStatus(STATUS_OPEN),
      newDoc("I2", file).setStatus(STATUS_CONFIRMED),
      newDoc("I3", file).setStatus(STATUS_REOPENED),
      newDoc("I4", file).setStatus(STATUS_RESOLVED),
      newDoc("I5", file).setStatus(STATUS_CLOSED),
      newDoc("I6", file).setStatus(STATUS_OPEN));

    assertThatFacetHasSize(IssueQuery.builder().build(), "statuses", 5);
  }

  @Test
  public void facets_on_resolutions() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setResolution(RESOLUTION_FALSE_POSITIVE),
      newDoc("I2", file).setResolution(RESOLUTION_FALSE_POSITIVE),
      newDoc("I3", file).setResolution(RESOLUTION_FIXED));

    assertThatFacetHasOnly(IssueQuery.builder(), "resolutions", entry("FALSE-POSITIVE", 2L), entry("FIXED", 1L));
  }

  @Test
  public void facets_on_resolutions_return_5_entries_max() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setResolution(RESOLUTION_FIXED),
      newDoc("I2", file).setResolution(RESOLUTION_FALSE_POSITIVE),
      newDoc("I3", file).setResolution(RESOLUTION_REMOVED),
      newDoc("I4", file).setResolution(RESOLUTION_WONT_FIX),
      newDoc("I5", file).setResolution(null));

    assertThatFacetHasSize(IssueQuery.builder().build(), "resolutions", 5);
  }

  @Test
  public void facets_on_languages() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);
    RuleDefinitionDto ruleDefinitionDto = newRule();
    db.rules().insert(ruleDefinitionDto);

    indexIssues(newDoc("I1", file).setRuleId(ruleDefinitionDto.getId()).setLanguage("xoo"));

    assertThatFacetHasOnly(IssueQuery.builder(), "languages", entry("xoo", 1L));
  }

  @Test
  public void facets_on_languages_return_100_entries_plus_selected_values() {
    OrganizationDto organizationDto = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(organizationDto);
    indexIssues(rangeClosed(1, 100).mapToObj(i -> newDoc(newFileDto(project, null)).setLanguage("a" + i)).toArray(IssueDoc[]::new));
    IssueDoc issue1 = newDoc(newFileDto(project, null)).setLanguage("language1");
    IssueDoc issue2 = newDoc(newFileDto(project, null)).setLanguage("language2");
    indexIssues(issue1, issue2);

    assertThatFacetHasSize(IssueQuery.builder().build(), "languages", 100);
    assertThatFacetHasSize(IssueQuery.builder().languages(asList(issue1.language(), issue2.language())).build(), "languages", 102);
  }

  @Test
  public void facets_on_assignees() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setAssigneeUuid("steph-uuid"),
      newDoc("I2", file).setAssigneeUuid("marcel-uuid"),
      newDoc("I3", file).setAssigneeUuid("marcel-uuid"),
      newDoc("I4", file).setAssigneeUuid(null));

    assertThatFacetHasOnly(IssueQuery.builder(), "assignees", entry("steph-uuid", 1L), entry("marcel-uuid", 2L), entry("", 1L));
  }

  @Test
  public void facets_on_assignees_return_only_100_entries_plus_selected_values() {
    OrganizationDto organizationDto = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(organizationDto);
    indexIssues(rangeClosed(1, 110).mapToObj(i -> newDoc(newFileDto(project, null)).setAssigneeUuid("a" + i)).toArray(IssueDoc[]::new));
    IssueDoc issue1 = newDoc(newFileDto(project, null)).setAssigneeUuid("user1");
    IssueDoc issue2 = newDoc(newFileDto(project, null)).setAssigneeUuid("user2");
    indexIssues(issue1, issue2);

    assertThatFacetHasSize(IssueQuery.builder().build(), "assignees", 100);
    assertThatFacetHasSize(IssueQuery.builder().assigneeUuids(asList(issue1.assigneeUuid(), issue2.assigneeUuid())).build(), "assignees", 102);
  }

  @Test
  public void facets_on_assignees_supports_dashes() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setAssigneeUuid("j-b-uuid"),
      newDoc("I2", file).setAssigneeUuid("marcel-uuid"),
      newDoc("I3", file).setAssigneeUuid("marcel-uuid"),
      newDoc("I4", file).setAssigneeUuid(null));

    assertThatFacetHasOnly(IssueQuery.builder().assigneeUuids(singletonList("j-b")),
      "assignees", entry("j-b-uuid", 1L), entry("marcel-uuid", 2L), entry("", 1L));
  }

  @Test
  public void facets_on_author() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setAuthorLogin("steph"),
      newDoc("I2", file).setAuthorLogin("marcel"),
      newDoc("I3", file).setAuthorLogin("marcel"),
      newDoc("I4", file).setAuthorLogin(null));

    assertThatFacetHasOnly(IssueQuery.builder(), "author", entry("steph", 1L), entry("marcel", 2L));
  }

  @Test
  public void facets_on_deprecated_authors() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setAuthorLogin("steph"),
      newDoc("I2", file).setAuthorLogin("marcel"),
      newDoc("I3", file).setAuthorLogin("marcel"),
      newDoc("I4", file).setAuthorLogin(null));

    assertThatFacetHasOnly(IssueQuery.builder(), "authors", entry("steph", 1L), entry("marcel", 2L));
  }

  @Test
  public void facets_on_authors_return_100_entries_plus_selected_values() {
    OrganizationDto organizationDto = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(organizationDto);
    indexIssues(rangeClosed(1, 110).mapToObj(i -> newDoc(newFileDto(project, null)).setAuthorLogin("a" + i)).toArray(IssueDoc[]::new));
    IssueDoc issue1 = newDoc(newFileDto(project, null)).setAuthorLogin("user1");
    IssueDoc issue2 = newDoc(newFileDto(project, null)).setAuthorLogin("user2");
    indexIssues(issue1, issue2);

    assertThatFacetHasSize(IssueQuery.builder().build(), "authors", 100);
    assertThatFacetHasSize(IssueQuery.builder().authors(asList(issue1.authorLogin(), issue2.authorLogin())).build(), "authors", 102);
  }

  @Test
  public void facet_on_created_at_with_less_than_20_days() {
    SearchOptions options = fixtureForCreatedAtFacet();

    IssueQuery query = IssueQuery.builder()
      .createdAfter(parseDateTime("2014-09-01T00:00:00+0100"))
      .createdBefore(parseDateTime("2014-09-08T00:00:00+0100"))
      .build();
    SearchResponse result = underTest.search(query, options);
    Map<String, Long> buckets = new Facets(result, system2.getDefaultTimeZone()).get("createdAt");
    assertThat(buckets).containsOnly(
      entry("2014-08-31", 0L),
      entry("2014-09-01", 2L),
      entry("2014-09-02", 1L),
      entry("2014-09-03", 0L),
      entry("2014-09-04", 0L),
      entry("2014-09-05", 1L),
      entry("2014-09-06", 0L),
      entry("2014-09-07", 0L));
  }

  @Test
  public void facet_on_created_at_with_less_than_20_weeks() {
    SearchOptions options = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
      .createdAfter(parseDateTime("2014-09-01T00:00:00+0100"))
      .createdBefore(parseDateTime("2014-09-21T00:00:00+0100")).build(),
      options);
    Map<String, Long> createdAt = new Facets(result, system2.getDefaultTimeZone()).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2014-08-25", 0L),
      entry("2014-09-01", 4L),
      entry("2014-09-08", 0L),
      entry("2014-09-15", 1L));
  }

  @Test
  public void facet_on_created_at_with_less_than_20_months() {
    SearchOptions options = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
      .createdAfter(parseDateTime("2014-09-01T00:00:00+0100"))
      .createdBefore(parseDateTime("2015-01-19T00:00:00+0100")).build(),
      options);
    Map<String, Long> createdAt = new Facets(result, system2.getDefaultTimeZone()).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2014-08-01", 0L),
      entry("2014-09-01", 5L),
      entry("2014-10-01", 0L),
      entry("2014-11-01", 0L),
      entry("2014-12-01", 0L),
      entry("2015-01-01", 1L));
  }

  @Test
  public void facet_on_created_at_with_more_than_20_months() {
    SearchOptions options = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
      .createdAfter(parseDateTime("2011-01-01T00:00:00+0100"))
      .createdBefore(parseDateTime("2016-01-01T00:00:00+0100")).build(),
      options);
    Map<String, Long> createdAt = new Facets(result, system2.getDefaultTimeZone()).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2010-01-01", 0L),
      entry("2011-01-01", 1L),
      entry("2012-01-01", 0L),
      entry("2013-01-01", 0L),
      entry("2014-01-01", 5L),
      entry("2015-01-01", 1L));
  }

  @Test
  public void facet_on_created_at_with_one_day() {
    SearchOptions options = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
      .createdAfter(parseDateTime("2014-09-01T00:00:00-0100"))
      .createdBefore(parseDateTime("2014-09-02T00:00:00-0100")).build(),
      options);
    Map<String, Long> createdAt = new Facets(result, system2.getDefaultTimeZone()).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2014-09-01", 2L));
  }

  @Test
  public void facet_on_created_at_with_bounds_outside_of_data() {
    SearchOptions options = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
      .createdAfter(parseDateTime("2009-01-01T00:00:00+0100"))
      .createdBefore(parseDateTime("2016-01-01T00:00:00+0100"))
      .build(), options);
    Map<String, Long> createdAt = new Facets(result, system2.getDefaultTimeZone()).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2008-01-01", 0L),
      entry("2009-01-01", 0L),
      entry("2010-01-01", 0L),
      entry("2011-01-01", 1L),
      entry("2012-01-01", 0L),
      entry("2013-01-01", 0L),
      entry("2014-01-01", 5L),
      entry("2015-01-01", 1L));
  }

  @Test
  public void facet_on_created_at_without_start_bound() {
    SearchOptions searchOptions = fixtureForCreatedAtFacet();

    SearchResponse result = underTest.search(IssueQuery.builder()
      .createdBefore(parseDateTime("2016-01-01T00:00:00+0100")).build(),
      searchOptions);
    Map<String, Long> createdAt = new Facets(result, system2.getDefaultTimeZone()).get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2011-01-01", 1L),
      entry("2012-01-01", 0L),
      entry("2013-01-01", 0L),
      entry("2014-01-01", 5L),
      entry("2015-01-01", 1L));
  }

  @Test
  public void facet_on_created_at_without_issues() {
    SearchOptions searchOptions = new SearchOptions().addFacets("createdAt");

    SearchResponse result = underTest.search(IssueQuery.builder().build(), searchOptions);
    Map<String, Long> createdAt = new Facets(result, system2.getDefaultTimeZone()).get("createdAt");
    assertThat(createdAt).isNull();
  }

  private SearchOptions fixtureForCreatedAtFacet() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    IssueDoc issue0 = newDoc("ISSUE0", file).setFuncCreationDate(parseDateTime("2011-04-25T00:05:13+0000"));
    IssueDoc issue1 = newDoc("I1", file).setFuncCreationDate(parseDateTime("2014-09-01T12:34:56+0100"));
    IssueDoc issue2 = newDoc("I2", file).setFuncCreationDate(parseDateTime("2014-09-01T10:46:00-1200"));
    IssueDoc issue3 = newDoc("I3", file).setFuncCreationDate(parseDateTime("2014-09-02T23:34:56+1200"));
    IssueDoc issue4 = newDoc("I4", file).setFuncCreationDate(parseDateTime("2014-09-05T12:34:56+0100"));
    IssueDoc issue5 = newDoc("I5", file).setFuncCreationDate(parseDateTime("2014-09-20T12:34:56+0100"));
    IssueDoc issue6 = newDoc("I6", file).setFuncCreationDate(parseDateTime("2015-01-18T12:34:56+0100"));

    indexIssues(issue0, issue1, issue2, issue3, issue4, issue5, issue6);

    return new SearchOptions().addFacets("createdAt");
  }

  private void indexIssues(IssueDoc... issues) {
    issueIndexer.index(asList(issues).iterator());
    authorizationIndexer.allow(stream(issues).map(issue -> new IndexPermissions(issue.projectUuid(), PROJECT).allowAnyone()).collect(toList()));
  }

  @SafeVarargs
  private final void assertThatFacetHasExactly(IssueQuery.Builder query, String facet, Map.Entry<String, Long>... expectedEntries) {
    SearchResponse result = underTest.search(query.build(), new SearchOptions().addFacets(singletonList(facet)));
    Facets facets = new Facets(result, system2.getDefaultTimeZone());
    assertThat(facets.getNames()).containsOnly(facet, "effort");
    assertThat(facets.get(facet)).containsExactly(expectedEntries);
  }

  @SafeVarargs
  private final void assertThatFacetHasOnly(IssueQuery.Builder query, String facet, Map.Entry<String, Long>... expectedEntries) {
    SearchResponse result = underTest.search(query.build(), new SearchOptions().addFacets(singletonList(facet)));
    Facets facets = new Facets(result, system2.getDefaultTimeZone());
    assertThat(facets.getNames()).containsOnly(facet, "effort");
    assertThat(facets.get(facet)).containsOnly(expectedEntries);
  }

  private void assertThatFacetHasSize(IssueQuery issueQuery, String facet, int expectedSize) {
    SearchResponse result = underTest.search(issueQuery, new SearchOptions().addFacets(singletonList(facet)));
    Facets facets = new Facets(result, system2.getDefaultTimeZone());
    assertThat(facets.get(facet)).hasSize(expectedSize);
  }
}
