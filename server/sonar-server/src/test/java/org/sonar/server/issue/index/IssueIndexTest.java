/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.annotation.Nullable;
import org.assertj.core.api.Fail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.System2;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.permission.index.AuthorizationIndexerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.view.index.ViewDoc;
import org.sonar.server.view.index.ViewIndexDefinition;
import org.sonar.server.view.index.ViewIndexer;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.api.utils.DateUtils.parseDateTime;

public class IssueIndexTest {

  @Rule
  public EsTester tester = new EsTester(new IssueIndexDefinition(new MapSettings()), new ViewIndexDefinition(new MapSettings()));

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  IssueIndex underTest;

  IssueIndexer issueIndexer;
  AuthorizationIndexerTester authorizationIndexerTester = new AuthorizationIndexerTester(tester);
  ViewIndexer viewIndexer;

  @Before
  public void setUp() {
    issueIndexer = new IssueIndexer(null, tester.client());
    viewIndexer = new ViewIndexer(null, tester.client());
    System2 system = mock(System2.class);
    when(system.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone("GMT-1:00"));
    when(system.now()).thenReturn(System.currentTimeMillis());

    underTest = new IssueIndex(tester.client(), system, userSessionRule);
  }

  @Test
  public void get_by_key() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);
    IssueDoc issue = IssueTesting.newDoc("ISSUE1", file)
      .setEffort(100L);
    indexIssues(issue);

    Issue loaded = underTest.getByKey(issue.key());
    assertThat(loaded).isNotNull();

    assertThat(loaded.key()).isEqualTo("ISSUE1");
    assertThat(loaded.effort()).isEqualTo(Duration.create(100L));
  }

  @Test
  public void get_by_key_with_attributes() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);
    IssueDoc issue = IssueTesting.newDoc("ISSUE1", file).setAttributes((KeyValueFormat.format(ImmutableMap.of("jira-issue-key", "SONAR-1234"))));
    indexIssues(issue);

    Issue result = underTest.getByKey(issue.key());
    assertThat(result.attribute("jira-issue-key")).isEqualTo("SONAR-1234");
  }

  @Test(expected = IllegalStateException.class)
  public void comments_field_is_not_available() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);
    IssueDoc issue = IssueTesting.newDoc("ISSUE1", file);
    indexIssues(issue);

    Issue result = underTest.getByKey(issue.key());
    result.comments();
  }

  @Test(expected = IllegalStateException.class)
  public void is_new_field_is_not_available() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);
    IssueDoc issue = IssueTesting.newDoc("ISSUE1", file);
    indexIssues(issue);

    Issue result = underTest.getByKey(issue.key());
    result.isNew();
  }

  @Test(expected = NotFoundException.class)
  public void fail_to_get_unknown_key() {
    underTest.getByKey("unknown");
  }

  @Test
  public void filter_by_keys() {
    ComponentDto project = ComponentTesting.newProjectDto();

    indexIssues(
      IssueTesting.newDoc("1", ComponentTesting.newFileDto(project, null)),
      IssueTesting.newDoc("2", ComponentTesting.newFileDto(project, null)));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).issueKeys(newArrayList("1", "2")).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).issueKeys(newArrayList("1")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).issueKeys(newArrayList("3", "4")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void filter_by_projects() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto module = ComponentTesting.newModuleDto(project);
    ComponentDto subModule = ComponentTesting.newModuleDto(module);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", project),
      IssueTesting.newDoc("ISSUE2", ComponentTesting.newFileDto(project, null)),
      IssueTesting.newDoc("ISSUE3", module),
      IssueTesting.newDoc("ISSUE4", ComponentTesting.newFileDto(module, null)),
      IssueTesting.newDoc("ISSUE5", subModule),
      IssueTesting.newDoc("ISSUE6", ComponentTesting.newFileDto(subModule, null)));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).projectUuids(newArrayList(project.uuid())).build(), new SearchOptions()).getDocs()).hasSize(6);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).projectUuids(newArrayList("unknown")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facets_on_projects() {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD");
    ComponentDto project2 = ComponentTesting.newProjectDto("EFGH");

    indexIssues(
      IssueTesting.newDoc("ISSUE1", ComponentTesting.newFileDto(project, null)),
      IssueTesting.newDoc("ISSUE2", ComponentTesting.newFileDto(project, null)),
      IssueTesting.newDoc("ISSUE3", ComponentTesting.newFileDto(project2, null)));

    SearchResult<IssueDoc> result = underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions().addFacets(newArrayList("projectUuids")));
    assertThat(result.getFacets().getNames()).containsOnly("projectUuids");
    assertThat(result.getFacets().get("projectUuids")).containsOnly(entry("ABCD", 2L), entry("EFGH", 1L));
  }

  @Test
  public void filter_by_modules() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto module = ComponentTesting.newModuleDto(project);
    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    ComponentDto file = ComponentTesting.newFileDto(subModule, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE3", module),
      IssueTesting.newDoc("ISSUE5", subModule),
      IssueTesting.newDoc("ISSUE2", file));

    assertThat(
      underTest.search(IssueQuery.builder(userSessionRule).projectUuids(newArrayList(project.uuid())).moduleUuids(newArrayList(file.uuid())).build(), new SearchOptions())
        .getDocs())
          .isEmpty();
    assertThat(
      underTest.search(IssueQuery.builder(userSessionRule).projectUuids(newArrayList(project.uuid())).moduleUuids(newArrayList(module.uuid())).build(), new SearchOptions())
        .getDocs())
          .hasSize(1);
    assertThat(
      underTest.search(IssueQuery.builder(userSessionRule).projectUuids(newArrayList(project.uuid())).moduleUuids(newArrayList(subModule.uuid())).build(), new SearchOptions())
        .getDocs())
          .hasSize(2);
    assertThat(
      underTest.search(IssueQuery.builder(userSessionRule).projectUuids(newArrayList(project.uuid())).moduleUuids(newArrayList(project.uuid())).build(), new SearchOptions())
        .getDocs())
          .isEmpty();
    assertThat(
      underTest.search(IssueQuery.builder(userSessionRule).projectUuids(newArrayList(project.uuid())).moduleUuids(newArrayList("unknown")).build(), new SearchOptions()).getDocs())
        .isEmpty();
  }

  @Test
  public void filter_by_components_on_contextualized_search() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto module = ComponentTesting.newModuleDto(project);
    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    ComponentDto file1 = ComponentTesting.newFileDto(project, null);
    ComponentDto file2 = ComponentTesting.newFileDto(module, null);
    ComponentDto file3 = ComponentTesting.newFileDto(subModule, null);
    String view = "ABCD";
    indexView(view, newArrayList(project.uuid()));

    indexIssues(
      IssueTesting.newDoc("ISSUE1", project),
      IssueTesting.newDoc("ISSUE2", file1),
      IssueTesting.newDoc("ISSUE3", module),
      IssueTesting.newDoc("ISSUE4", file2),
      IssueTesting.newDoc("ISSUE5", subModule),
      IssueTesting.newDoc("ISSUE6", file3));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).fileUuids(newArrayList(file1.uuid(), file2.uuid(), file3.uuid())).build(), new SearchOptions())
      .getDocs()).hasSize(3);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).fileUuids(newArrayList(file1.uuid())).build(), new SearchOptions())
      .getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).moduleRootUuids(newArrayList(subModule.uuid())).build(), new SearchOptions())
      .getDocs()).hasSize(2);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).moduleRootUuids(newArrayList(module.uuid())).build(), new SearchOptions())
      .getDocs()).hasSize(4);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).projectUuids(newArrayList(project.uuid())).build(), new SearchOptions())
      .getDocs()).hasSize(6);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).viewUuids(newArrayList(view)).build(), new SearchOptions())
      .getDocs()).hasSize(6);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).projectUuids(newArrayList("unknown")).build(), new SearchOptions())
      .getDocs()).isEmpty();
  }

  @Test
  public void filter_by_components_on_non_contextualized_search() {
    ComponentDto project = ComponentTesting.newProjectDto("project");
    ComponentDto file1 = ComponentTesting.newFileDto(project, null, "file1");
    ComponentDto module = ComponentTesting.newModuleDto(project).setUuid("module");
    ComponentDto file2 = ComponentTesting.newFileDto(module, null, "file2");
    ComponentDto subModule = ComponentTesting.newModuleDto(module).setUuid("subModule");
    ComponentDto file3 = ComponentTesting.newFileDto(subModule, null, "file3");
    String view = "ABCD";
    indexView(view, newArrayList(project.uuid()));

    indexIssues(
      IssueTesting.newDoc("ISSUE1", project),
      IssueTesting.newDoc("ISSUE2", file1),
      IssueTesting.newDoc("ISSUE3", module),
      IssueTesting.newDoc("ISSUE4", file2),
      IssueTesting.newDoc("ISSUE5", subModule),
      IssueTesting.newDoc("ISSUE6", file3));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).projectUuids(newArrayList("unknown")).build(), new SearchOptions()).getDocs()).isEmpty();
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).projectUuids(newArrayList(project.uuid())).build(), new SearchOptions()).getDocs()).hasSize(6);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).viewUuids(newArrayList(view)).build(), new SearchOptions()).getDocs()).hasSize(6);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).moduleUuids(newArrayList(module.uuid())).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).moduleUuids(newArrayList(subModule.uuid())).build(), new SearchOptions()).getDocs()).hasSize(2); // XXX
    // Misleading
    // !
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).fileUuids(newArrayList(file1.uuid())).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).fileUuids(newArrayList(file1.uuid(), file2.uuid(), file3.uuid())).build(), new SearchOptions()).getDocs())
      .hasSize(3);
  }

  @Test
  public void facets_on_components() {
    ComponentDto project = ComponentTesting.newProjectDto("A");
    ComponentDto file1 = ComponentTesting.newFileDto(project, null, "ABCD");
    ComponentDto file2 = ComponentTesting.newFileDto(project, null, "BCDE");
    ComponentDto file3 = ComponentTesting.newFileDto(project, null, "CDEF");

    indexIssues(
      IssueTesting.newDoc("ISSUE1", project),
      IssueTesting.newDoc("ISSUE2", file1),
      IssueTesting.newDoc("ISSUE3", file2),
      IssueTesting.newDoc("ISSUE4", file2),
      IssueTesting.newDoc("ISSUE5", file3));

    SearchResult<IssueDoc> result = underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions().addFacets(newArrayList("fileUuids")));
    assertThat(result.getFacets().getNames()).containsOnly("fileUuids");
    assertThat(result.getFacets().get("fileUuids"))
      .containsOnly(entry("A", 1L), entry("ABCD", 1L), entry("BCDE", 2L), entry("CDEF", 1L));
  }

  @Test
  public void filter_by_directories() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file1 = ComponentTesting.newFileDto(project, null).setPath("src/main/xoo/F1.xoo");
    ComponentDto file2 = ComponentTesting.newFileDto(project, null).setPath("F2.xoo");

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file1).setDirectoryPath("/src/main/xoo"),
      IssueTesting.newDoc("ISSUE2", file2).setDirectoryPath("/"));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).directories(newArrayList("/src/main/xoo")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).directories(newArrayList("/")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).directories(newArrayList("unknown")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facets_on_directories() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file1 = ComponentTesting.newFileDto(project, null).setPath("src/main/xoo/F1.xoo");
    ComponentDto file2 = ComponentTesting.newFileDto(project, null).setPath("F2.xoo");

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file1).setDirectoryPath("/src/main/xoo"),
      IssueTesting.newDoc("ISSUE2", file2).setDirectoryPath("/"));

    SearchResult<IssueDoc> result = underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions().addFacets(newArrayList("directories")));
    assertThat(result.getFacets().getNames()).containsOnly("directories");
    assertThat(result.getFacets().get("directories")).containsOnly(entry("/src/main/xoo", 1L), entry("/", 1L));
  }

  @Test
  public void filter_by_views() {
    ComponentDto project1 = ComponentTesting.newProjectDto();
    ComponentDto file1 = ComponentTesting.newFileDto(project1, null);
    ComponentDto project2 = ComponentTesting.newProjectDto();
    indexIssues(
      // Project1 has 2 issues (one on a file and one on the project itself)
      IssueTesting.newDoc("ISSUE1", project1),
      IssueTesting.newDoc("ISSUE2", file1),
      // Project2 has 1 issue
      IssueTesting.newDoc("ISSUE3", project2));

    // The view1 is containing 2 issues from project1
    String view1 = "ABCD";
    indexView(view1, newArrayList(project1.uuid()));

    // The view2 is containing 1 issue from project2
    String view2 = "CDEF";
    indexView(view2, newArrayList(project2.uuid()));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).viewUuids(newArrayList(view1)).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).viewUuids(newArrayList(view2)).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).viewUuids(newArrayList(view1, view2)).build(), new SearchOptions()).getDocs()).hasSize(3);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).viewUuids(newArrayList("unknown")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void filter_by_severities() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setSeverity(Severity.INFO),
      IssueTesting.newDoc("ISSUE2", file).setSeverity(Severity.MAJOR));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).severities(newArrayList(Severity.INFO, Severity.MAJOR)).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).severities(newArrayList(Severity.INFO)).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).severities(newArrayList(Severity.BLOCKER)).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facets_on_severities() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setSeverity(Severity.INFO),
      IssueTesting.newDoc("ISSUE2", file).setSeverity(Severity.INFO),
      IssueTesting.newDoc("ISSUE3", file).setSeverity(Severity.MAJOR));

    SearchResult<IssueDoc> result = underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions().addFacets(newArrayList("severities")));
    assertThat(result.getFacets().getNames()).containsOnly("severities");
    assertThat(result.getFacets().get("severities")).containsOnly(entry("INFO", 2L), entry("MAJOR", 1L));
  }

  @Test
  public void filter_by_statuses() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setStatus(Issue.STATUS_CLOSED),
      IssueTesting.newDoc("ISSUE2", file).setStatus(Issue.STATUS_OPEN));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).statuses(newArrayList(Issue.STATUS_CLOSED, Issue.STATUS_OPEN)).build(), new SearchOptions()).getDocs())
      .hasSize(2);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).statuses(newArrayList(Issue.STATUS_CLOSED)).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).statuses(newArrayList(Issue.STATUS_CONFIRMED)).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facets_on_statuses() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setStatus(Issue.STATUS_CLOSED),
      IssueTesting.newDoc("ISSUE2", file).setStatus(Issue.STATUS_CLOSED),
      IssueTesting.newDoc("ISSUE3", file).setStatus(Issue.STATUS_OPEN));

    SearchResult<IssueDoc> result = underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions().addFacets(newArrayList("statuses")));
    assertThat(result.getFacets().getNames()).containsOnly("statuses");
    assertThat(result.getFacets().get("statuses")).containsOnly(entry("CLOSED", 2L), entry("OPEN", 1L));
  }

  @Test
  public void filter_by_resolutions() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      IssueTesting.newDoc("ISSUE2", file).setResolution(Issue.RESOLUTION_FIXED));

    assertThat(
      underTest.search(IssueQuery.builder(userSessionRule).resolutions(newArrayList(Issue.RESOLUTION_FALSE_POSITIVE, Issue.RESOLUTION_FIXED)).build(), new SearchOptions())
        .getDocs())
          .hasSize(2);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).resolutions(newArrayList(Issue.RESOLUTION_FALSE_POSITIVE)).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).resolutions(newArrayList(Issue.RESOLUTION_REMOVED)).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facets_on_resolutions() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      IssueTesting.newDoc("ISSUE2", file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      IssueTesting.newDoc("ISSUE3", file).setResolution(Issue.RESOLUTION_FIXED));

    SearchResult<IssueDoc> result = underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions().addFacets(newArrayList("resolutions")));
    assertThat(result.getFacets().getNames()).containsOnly("resolutions");
    assertThat(result.getFacets().get("resolutions")).containsOnly(entry("FALSE-POSITIVE", 2L), entry("FIXED", 1L));
  }

  @Test
  public void filter_by_resolved() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED),
      IssueTesting.newDoc("ISSUE2", file).setStatus(Issue.STATUS_OPEN).setResolution(null),
      IssueTesting.newDoc("ISSUE3", file).setStatus(Issue.STATUS_OPEN).setResolution(null));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).resolved(true).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).resolved(false).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).resolved(null).build(), new SearchOptions()).getDocs()).hasSize(3);
  }

  @Test
  public void filter_by_rules() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);
    RuleKey ruleKey = RuleKey.of("repo", "X1");

    indexIssues(IssueTesting.newDoc("ISSUE1", file).setRuleKey(ruleKey.toString()));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).rules(newArrayList(ruleKey)).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).rules(newArrayList(RuleKey.of("rule", "without issue"))).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void filter_by_languages() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);
    RuleKey ruleKey = RuleKey.of("repo", "X1");

    indexIssues(IssueTesting.newDoc("ISSUE1", file).setRuleKey(ruleKey.toString()).setLanguage("xoo"));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).languages(newArrayList("xoo")).build(),
      new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).languages(newArrayList("unknown")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facets_on_languages() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);
    RuleKey ruleKey = RuleKey.of("repo", "X1");

    indexIssues(IssueTesting.newDoc("ISSUE1", file).setRuleKey(ruleKey.toString()).setLanguage("xoo"));

    SearchResult<IssueDoc> result = underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions().addFacets(newArrayList("languages")));
    assertThat(result.getFacets().getNames()).containsOnly("languages");
    assertThat(result.getFacets().get("languages")).containsOnly(entry("xoo", 1L));
  }

  @Test
  public void filter_by_assignees() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAssignee("steph"),
      IssueTesting.newDoc("ISSUE2", file).setAssignee("simon"),
      IssueTesting.newDoc("ISSUE3", file).setAssignee(null));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).assignees(newArrayList("steph")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).assignees(newArrayList("steph", "simon")).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).assignees(newArrayList("unknown")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facets_on_assignees() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAssignee("steph"),
      IssueTesting.newDoc("ISSUE2", file).setAssignee("simon"),
      IssueTesting.newDoc("ISSUE3", file).setAssignee("simon"),
      IssueTesting.newDoc("ISSUE4", file).setAssignee(null));

    SearchResult<IssueDoc> result = underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions().addFacets(newArrayList("assignees")));
    assertThat(result.getFacets().getNames()).containsOnly("assignees");
    assertThat(result.getFacets().get("assignees")).containsOnly(entry("steph", 1L), entry("simon", 2L), entry("", 1L));
  }

  @Test
  public void facets_on_assignees_supports_dashes() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAssignee("j-b"),
      IssueTesting.newDoc("ISSUE2", file).setAssignee("simon"),
      IssueTesting.newDoc("ISSUE3", file).setAssignee("simon"),
      IssueTesting.newDoc("ISSUE4", file).setAssignee(null));

    SearchResult<IssueDoc> result = underTest.search(IssueQuery.builder(userSessionRule).assignees(Arrays.asList("j-b")).build(),
      new SearchOptions().addFacets(newArrayList("assignees")));
    assertThat(result.getFacets().getNames()).containsOnly("assignees");
    assertThat(result.getFacets().get("assignees")).containsOnly(entry("j-b", 1L), entry("simon", 2L), entry("", 1L));
  }

  @Test
  public void filter_by_assigned() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAssignee("steph"),
      IssueTesting.newDoc("ISSUE2", file).setAssignee(null),
      IssueTesting.newDoc("ISSUE3", file).setAssignee(null));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).assigned(true).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).assigned(false).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).assigned(null).build(), new SearchOptions()).getDocs()).hasSize(3);
  }

  @Test
  public void filter_by_authors() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAuthorLogin("steph"),
      IssueTesting.newDoc("ISSUE2", file).setAuthorLogin("simon"),
      IssueTesting.newDoc("ISSUE3", file).setAssignee(null));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).authors(newArrayList("steph")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).authors(newArrayList("steph", "simon")).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).authors(newArrayList("unknown")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facets_on_authors() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAuthorLogin("steph"),
      IssueTesting.newDoc("ISSUE2", file).setAuthorLogin("simon"),
      IssueTesting.newDoc("ISSUE3", file).setAuthorLogin("simon"),
      IssueTesting.newDoc("ISSUE4", file).setAuthorLogin(null));

    SearchResult<IssueDoc> result = underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions().addFacets(newArrayList("authors")));
    assertThat(result.getFacets().getNames()).containsOnly("authors");
    assertThat(result.getFacets().get("authors")).containsOnly(entry("steph", 1L), entry("simon", 2L));
  }

  @Test
  public void filter_by_created_after() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setFuncCreationDate(parseDate("2014-09-20")),
      IssueTesting.newDoc("ISSUE2", file).setFuncCreationDate(parseDate("2014-09-23")));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).createdAfter(parseDate("2014-09-19")).build(), new SearchOptions()).getDocs()).hasSize(2);
    // Lower bound is included
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).createdAfter(parseDate("2014-09-20")).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).createdAfter(parseDate("2014-09-21")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).createdAfter(parseDate("2014-09-25")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void filter_by_created_before() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setFuncCreationDate(parseDate("2014-09-20")),
      IssueTesting.newDoc("ISSUE2", file).setFuncCreationDate(parseDate("2014-09-23")));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).createdBefore(parseDate("2014-09-19")).build(), new SearchOptions()).getDocs()).isEmpty();
    // Upper bound is excluded
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).createdBefore(parseDate("2014-09-20")).build(), new SearchOptions()).getDocs()).isEmpty();
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).createdBefore(parseDate("2014-09-21")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).createdBefore(parseDate("2014-09-25")).build(), new SearchOptions()).getDocs()).hasSize(2);
  }

  @Test
  public void filter_by_created_after_and_before() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setFuncCreationDate(parseDate("2014-09-20")),
      IssueTesting.newDoc("ISSUE2", file).setFuncCreationDate(parseDate("2014-09-23")));

    // 19 < createdAt < 25
    assertThat(underTest.search(IssueQuery.builder(userSessionRule)
      .createdAfter(parseDate("2014-09-19")).createdBefore(parseDate("2014-09-25"))
      .build(), new SearchOptions()).getDocs()).hasSize(2);

    // 20 < createdAt < 25: excludes first issue
    assertThat(underTest.search(IssueQuery.builder(userSessionRule)
      .createdAfter(parseDate("2014-09-20")).createdBefore(parseDate("2014-09-25"))
      .build(), new SearchOptions()).getDocs()).hasSize(2);

    // 21 < createdAt < 25
    assertThat(underTest.search(IssueQuery.builder(userSessionRule)
      .createdAfter(parseDate("2014-09-21")).createdBefore(parseDate("2014-09-25"))
      .build(), new SearchOptions()).getDocs()).hasSize(1);

    // 21 < createdAt < 24
    assertThat(underTest.search(IssueQuery.builder(userSessionRule)
      .createdAfter(parseDate("2014-09-21")).createdBefore(parseDate("2014-09-24"))
      .build(), new SearchOptions()).getDocs()).hasSize(1);

    // 21 < createdAt < 23: excludes second issue
    assertThat(underTest.search(IssueQuery.builder(userSessionRule)
      .createdAfter(parseDate("2014-09-21")).createdBefore(parseDate("2014-09-23"))
      .build(), new SearchOptions()).getDocs()).isEmpty();

    // 19 < createdAt < 21: only first issue
    assertThat(underTest.search(IssueQuery.builder(userSessionRule)
      .createdAfter(parseDate("2014-09-19")).createdBefore(parseDate("2014-09-21"))
      .build(), new SearchOptions()).getDocs()).hasSize(1);

    // 20 < createdAt < 20: exception
    expectedException.expect(IllegalArgumentException.class);
    underTest.search(IssueQuery.builder(userSessionRule)
      .createdAfter(parseDate("2014-09-20")).createdBefore(parseDate("2014-09-20"))
      .build(), new SearchOptions()).getDocs();
  }

  @Test
  public void filter_by_create_after_and_before_take_into_account_timezone() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setFuncCreationDate(parseDateTime("2014-09-20T00:00:00+0100")),
      IssueTesting.newDoc("ISSUE2", file).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule)
      .createdAfter(parseDateTime("2014-09-19T23:00:00+0000")).createdBefore(parseDateTime("2014-09-22T23:00:01+0000"))
      .build(), new SearchOptions()).getDocs()).hasSize(2);

    assertThat(underTest.search(IssueQuery.builder(userSessionRule)
      .createdAfter(parseDateTime("2014-09-19T23:00:01+0000")).createdBefore(parseDateTime("2014-09-22T23:00:00+0000"))
      .build(), new SearchOptions()).getDocs()).hasSize(0);
  }

  @Test
  public void filter_by_created_before_must_be_lower_than_after() {
    try {
      underTest.search(IssueQuery.builder(userSessionRule).createdAfter(parseDate("2014-09-20")).createdBefore(parseDate("2014-09-19")).build(),
        new SearchOptions());
      Fail.failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException exception) {
      assertThat(exception.getMessage()).isEqualTo("Start bound cannot be larger or equal to end bound");
    }
  }

  @Test
  public void fail_if_created_before_equals_created_after() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Start bound cannot be larger or equal to end bound");

    underTest.search(IssueQuery.builder(userSessionRule).createdAfter(parseDate("2014-09-20")).createdBefore(parseDate("2014-09-20")).build(), new SearchOptions());
  }

  @Test
  public void filter_by_created_after_must_not_be_in_future() {
    try {
      underTest.search(IssueQuery.builder(userSessionRule).createdAfter(new Date(Long.MAX_VALUE)).build(), new SearchOptions());
      Fail.failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException exception) {
      assertThat(exception.getMessage()).isEqualTo("Start bound cannot be in the future");
    }
  }

  @Test
  public void filter_by_created_at() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(IssueTesting.newDoc("ISSUE1", file).setFuncCreationDate(parseDate("2014-09-20")));

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).createdAt(parseDate("2014-09-20")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).createdAt(parseDate("2014-09-21")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facet_on_created_at_with_less_than_20_days() {

    SearchOptions options = fixtureForCreatedAtFacet();

    IssueQuery query = IssueQuery.builder(userSessionRule)
      .createdAfter(parseDateTime("2014-09-01T00:00:00+0100"))
      .createdBefore(parseDateTime("2014-09-08T00:00:00+0100"))
      .checkAuthorization(false)
      .build();
    SearchResult<IssueDoc> result = underTest.search(query, options);
    Map<String, Long> buckets = result.getFacets().get("createdAt");
    assertThat(buckets).containsOnly(
      entry("2014-08-31T01:00:00+0000", 0L),
      entry("2014-09-01T01:00:00+0000", 2L),
      entry("2014-09-02T01:00:00+0000", 1L),
      entry("2014-09-03T01:00:00+0000", 0L),
      entry("2014-09-04T01:00:00+0000", 0L),
      entry("2014-09-05T01:00:00+0000", 1L),
      entry("2014-09-06T01:00:00+0000", 0L),
      entry("2014-09-07T01:00:00+0000", 0L));
  }

  @Test
  public void facet_on_created_at_with_less_than_20_weeks() {

    SearchOptions SearchOptions = fixtureForCreatedAtFacet();

    Map<String, Long> createdAt = underTest.search(IssueQuery.builder(userSessionRule)
      .createdAfter(parseDateTime("2014-09-01T00:00:00+0100"))
      .createdBefore(parseDateTime("2014-09-21T00:00:00+0100")).build(),
      SearchOptions).getFacets().get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2014-08-25T01:00:00+0000", 0L),
      entry("2014-09-01T01:00:00+0000", 4L),
      entry("2014-09-08T01:00:00+0000", 0L),
      entry("2014-09-15T01:00:00+0000", 1L));
  }

  @Test
  public void facet_on_created_at_with_less_than_20_months() {

    SearchOptions SearchOptions = fixtureForCreatedAtFacet();

    Map<String, Long> createdAt = underTest.search(IssueQuery.builder(userSessionRule)
      .createdAfter(parseDateTime("2014-09-01T00:00:00+0100"))
      .createdBefore(parseDateTime("2015-01-19T00:00:00+0100")).build(),
      SearchOptions).getFacets().get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2014-08-01T01:00:00+0000", 0L),
      entry("2014-09-01T01:00:00+0000", 5L),
      entry("2014-10-01T01:00:00+0000", 0L),
      entry("2014-11-01T01:00:00+0000", 0L),
      entry("2014-12-01T01:00:00+0000", 0L),
      entry("2015-01-01T01:00:00+0000", 1L));
  }

  @Test
  public void facet_on_created_at_with_more_than_20_months() {
    SearchOptions SearchOptions = fixtureForCreatedAtFacet();

    Map<String, Long> createdAt = underTest.search(IssueQuery.builder(userSessionRule)
      .createdAfter(parseDateTime("2011-01-01T00:00:00+0100"))
      .createdBefore(parseDateTime("2016-01-01T00:00:00+0100")).build(),
      SearchOptions).getFacets().get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2010-01-01T01:00:00+0000", 0L),
      entry("2011-01-01T01:00:00+0000", 1L),
      entry("2012-01-01T01:00:00+0000", 0L),
      entry("2013-01-01T01:00:00+0000", 0L),
      entry("2014-01-01T01:00:00+0000", 5L),
      entry("2015-01-01T01:00:00+0000", 1L));

  }

  @Test
  public void facet_on_created_at_with_one_day() {
    SearchOptions SearchOptions = fixtureForCreatedAtFacet();

    Map<String, Long> createdAt = underTest.search(IssueQuery.builder(userSessionRule)
      .createdAfter(parseDateTime("2014-09-01T00:00:00-0100"))
      .createdBefore(parseDateTime("2014-09-02T00:00:00-0100")).build(),
      SearchOptions).getFacets().get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2014-09-01T01:00:00+0000", 2L));
  }

  @Test
  public void facet_on_created_at_with_bounds_outside_of_data() {
    SearchOptions options = fixtureForCreatedAtFacet();

    Map<String, Long> createdAt = underTest.search(IssueQuery.builder(userSessionRule)
      .createdAfter(parseDateTime("2009-01-01T00:00:00+0100"))
      .createdBefore(parseDateTime("2016-01-01T00:00:00+0100"))
      .build(), options).getFacets().get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2008-01-01T01:00:00+0000", 0L),
      entry("2009-01-01T01:00:00+0000", 0L),
      entry("2010-01-01T01:00:00+0000", 0L),
      entry("2011-01-01T01:00:00+0000", 1L),
      entry("2012-01-01T01:00:00+0000", 0L),
      entry("2013-01-01T01:00:00+0000", 0L),
      entry("2014-01-01T01:00:00+0000", 5L),
      entry("2015-01-01T01:00:00+0000", 1L));
  }

  @Test
  public void facet_on_created_at_without_start_bound() {
    SearchOptions SearchOptions = fixtureForCreatedAtFacet();

    Map<String, Long> createdAt = underTest.search(IssueQuery.builder(userSessionRule)
      .createdBefore(parseDateTime("2016-01-01T00:00:00+0100")).build(),
      SearchOptions).getFacets().get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2011-01-01T01:00:00+0000", 1L),
      entry("2012-01-01T01:00:00+0000", 0L),
      entry("2013-01-01T01:00:00+0000", 0L),
      entry("2014-01-01T01:00:00+0000", 5L),
      entry("2015-01-01T01:00:00+0000", 1L));
  }

  @Test
  public void facet_on_created_at_without_issues() {
    SearchOptions SearchOptions = new SearchOptions().addFacets("createdAt");

    Map<String, Long> createdAt = underTest.search(IssueQuery.builder(userSessionRule).build(),
      SearchOptions).getFacets().get("createdAt");
    assertThat(createdAt).isNull();
  }

  private SearchOptions fixtureForCreatedAtFacet() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    IssueDoc issue0 = IssueTesting.newDoc("ISSUE0", file).setFuncCreationDate(parseDateTime("2011-04-25T00:05:13+0000"));
    IssueDoc issue1 = IssueTesting.newDoc("ISSUE1", file).setFuncCreationDate(parseDateTime("2014-09-01T12:34:56+0100"));
    IssueDoc issue2 = IssueTesting.newDoc("ISSUE2", file).setFuncCreationDate(parseDateTime("2014-09-01T10:46:00-1200"));
    IssueDoc issue3 = IssueTesting.newDoc("ISSUE3", file).setFuncCreationDate(parseDateTime("2014-09-02T23:34:56+1200"));
    IssueDoc issue4 = IssueTesting.newDoc("ISSUE4", file).setFuncCreationDate(parseDateTime("2014-09-05T12:34:56+0100"));
    IssueDoc issue5 = IssueTesting.newDoc("ISSUE5", file).setFuncCreationDate(parseDateTime("2014-09-20T12:34:56+0100"));
    IssueDoc issue6 = IssueTesting.newDoc("ISSUE6", file).setFuncCreationDate(parseDateTime("2015-01-18T12:34:56+0100"));

    indexIssues(issue0, issue1, issue2, issue3, issue4, issue5, issue6);

    return new SearchOptions().addFacets("createdAt");
  }

  @Test
  public void paging() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);
    for (int i = 0; i < 12; i++) {
      indexIssues(IssueTesting.newDoc("ISSUE" + i, file));
    }

    IssueQuery.Builder query = IssueQuery.builder(userSessionRule);
    // There are 12 issues in total, with 10 issues per page, the page 2 should only contain 2 elements
    SearchResult<IssueDoc> result = underTest.search(query.build(), new SearchOptions().setPage(2, 10));
    assertThat(result.getDocs()).hasSize(2);
    assertThat(result.getTotal()).isEqualTo(12);

    result = underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions().setOffset(0).setLimit(5));
    assertThat(result.getDocs()).hasSize(5);
    assertThat(result.getTotal()).isEqualTo(12);

    result = underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions().setOffset(2).setLimit(0));
    assertThat(result.getDocs()).hasSize(10);
    assertThat(result.getTotal()).isEqualTo(12);
  }

  @Test
  public void search_with_max_limit() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);
    List<IssueDoc> issues = newArrayList();
    for (int i = 0; i < 500; i++) {
      String key = "ISSUE" + i;
      issues.add(IssueTesting.newDoc(key, file));
    }
    indexIssues(issues.toArray(new IssueDoc[] {}));

    IssueQuery.Builder query = IssueQuery.builder(userSessionRule);
    SearchResult<IssueDoc> result = underTest.search(query.build(), new SearchOptions().setLimit(Integer.MAX_VALUE));
    assertThat(result.getDocs()).hasSize(SearchOptions.MAX_LIMIT);
  }

  @Test
  public void sort_by_status() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setStatus(Issue.STATUS_OPEN),
      IssueTesting.newDoc("ISSUE2", file).setStatus(Issue.STATUS_CLOSED),
      IssueTesting.newDoc("ISSUE3", file).setStatus(Issue.STATUS_REOPENED));

    IssueQuery.Builder query = IssueQuery.builder(userSessionRule).sort(IssueQuery.SORT_BY_STATUS).asc(true);
    SearchResult<IssueDoc> result = underTest.search(query.build(), new SearchOptions());
    assertThat(result.getDocs().get(0).status()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(result.getDocs().get(1).status()).isEqualTo(Issue.STATUS_OPEN);
    assertThat(result.getDocs().get(2).status()).isEqualTo(Issue.STATUS_REOPENED);

    query = IssueQuery.builder(userSessionRule).sort(IssueQuery.SORT_BY_STATUS).asc(false);
    result = underTest.search(query.build(), new SearchOptions());
    assertThat(result.getDocs().get(0).status()).isEqualTo(Issue.STATUS_REOPENED);
    assertThat(result.getDocs().get(1).status()).isEqualTo(Issue.STATUS_OPEN);
    assertThat(result.getDocs().get(2).status()).isEqualTo(Issue.STATUS_CLOSED);
  }

  @Test
  public void sort_by_severity() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setSeverity(Severity.BLOCKER),
      IssueTesting.newDoc("ISSUE2", file).setSeverity(Severity.INFO),
      IssueTesting.newDoc("ISSUE3", file).setSeverity(Severity.MINOR),
      IssueTesting.newDoc("ISSUE4", file).setSeverity(Severity.CRITICAL),
      IssueTesting.newDoc("ISSUE5", file).setSeverity(Severity.MAJOR));

    IssueQuery.Builder query = IssueQuery.builder(userSessionRule).sort(IssueQuery.SORT_BY_SEVERITY).asc(true);
    SearchResult<IssueDoc> result = underTest.search(query.build(), new SearchOptions());
    assertThat(result.getDocs().get(0).severity()).isEqualTo(Severity.INFO);
    assertThat(result.getDocs().get(1).severity()).isEqualTo(Severity.MINOR);
    assertThat(result.getDocs().get(2).severity()).isEqualTo(Severity.MAJOR);
    assertThat(result.getDocs().get(3).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(result.getDocs().get(4).severity()).isEqualTo(Severity.BLOCKER);

    query = IssueQuery.builder(userSessionRule).sort(IssueQuery.SORT_BY_SEVERITY).asc(false);
    result = underTest.search(query.build(), new SearchOptions());
    assertThat(result.getDocs().get(0).severity()).isEqualTo(Severity.BLOCKER);
    assertThat(result.getDocs().get(1).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(result.getDocs().get(2).severity()).isEqualTo(Severity.MAJOR);
    assertThat(result.getDocs().get(3).severity()).isEqualTo(Severity.MINOR);
    assertThat(result.getDocs().get(4).severity()).isEqualTo(Severity.INFO);
  }

  @Test
  public void sort_by_assignee() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAssignee("steph"),
      IssueTesting.newDoc("ISSUE2", file).setAssignee("simon"));

    IssueQuery.Builder query = IssueQuery.builder(userSessionRule).sort(IssueQuery.SORT_BY_ASSIGNEE).asc(true);
    SearchResult<IssueDoc> result = underTest.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(2);
    assertThat(result.getDocs().get(0).assignee()).isEqualTo("simon");
    assertThat(result.getDocs().get(1).assignee()).isEqualTo("steph");

    query = IssueQuery.builder(userSessionRule).sort(IssueQuery.SORT_BY_ASSIGNEE).asc(false);
    result = underTest.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(2);
    assertThat(result.getDocs().get(0).assignee()).isEqualTo("steph");
    assertThat(result.getDocs().get(1).assignee()).isEqualTo("simon");
  }

  @Test
  public void sort_by_creation_date() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),
      IssueTesting.newDoc("ISSUE2", file).setFuncCreationDate(parseDateTime("2014-09-24T00:00:00+0100")));

    IssueQuery.Builder query = IssueQuery.builder(userSessionRule).sort(IssueQuery.SORT_BY_CREATION_DATE).asc(true);
    SearchResult<IssueDoc> result = underTest.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(2);
    assertThat(result.getDocs().get(0).creationDate()).isEqualTo(parseDateTime("2014-09-23T00:00:00+0100"));
    assertThat(result.getDocs().get(1).creationDate()).isEqualTo(parseDateTime("2014-09-24T00:00:00+0100"));

    query = IssueQuery.builder(userSessionRule).sort(IssueQuery.SORT_BY_CREATION_DATE).asc(false);
    result = underTest.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(2);
    assertThat(result.getDocs().get(0).creationDate()).isEqualTo(parseDateTime("2014-09-24T00:00:00+0100"));
    assertThat(result.getDocs().get(1).creationDate()).isEqualTo(parseDateTime("2014-09-23T00:00:00+0100"));
  }

  @Test
  public void sort_by_update_date() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setFuncUpdateDate(parseDateTime("2014-09-23T00:00:00+0100")),
      IssueTesting.newDoc("ISSUE2", file).setFuncUpdateDate(parseDateTime("2014-09-24T00:00:00+0100")));

    IssueQuery.Builder query = IssueQuery.builder(userSessionRule).sort(IssueQuery.SORT_BY_UPDATE_DATE).asc(true);
    SearchResult<IssueDoc> result = underTest.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(2);
    assertThat(result.getDocs().get(0).updateDate()).isEqualTo(parseDateTime("2014-09-23T00:00:00+0100"));
    assertThat(result.getDocs().get(1).updateDate()).isEqualTo(parseDateTime("2014-09-24T00:00:00+0100"));

    query = IssueQuery.builder(userSessionRule).sort(IssueQuery.SORT_BY_UPDATE_DATE).asc(false);
    result = underTest.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(2);
    assertThat(result.getDocs().get(0).updateDate()).isEqualTo(parseDateTime("2014-09-24T00:00:00+0100"));
    assertThat(result.getDocs().get(1).updateDate()).isEqualTo(parseDateTime("2014-09-23T00:00:00+0100"));
  }

  @Test
  public void sort_by_close_date() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setFuncCloseDate(parseDateTime("2014-09-23T00:00:00+0100")),
      IssueTesting.newDoc("ISSUE2", file).setFuncCloseDate(parseDateTime("2014-09-24T00:00:00+0100")),
      IssueTesting.newDoc("ISSUE3", file).setFuncCloseDate(null));

    IssueQuery.Builder query = IssueQuery.builder(userSessionRule).sort(IssueQuery.SORT_BY_CLOSE_DATE).asc(true);
    SearchResult<IssueDoc> result = underTest.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(3);
    assertThat(result.getDocs().get(0).closeDate()).isNull();
    assertThat(result.getDocs().get(1).closeDate()).isEqualTo(parseDateTime("2014-09-23T00:00:00+0100"));
    assertThat(result.getDocs().get(2).closeDate()).isEqualTo(parseDateTime("2014-09-24T00:00:00+0100"));

    query = IssueQuery.builder(userSessionRule).sort(IssueQuery.SORT_BY_CLOSE_DATE).asc(false);
    result = underTest.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(3);
    assertThat(result.getDocs().get(0).closeDate()).isEqualTo(parseDateTime("2014-09-24T00:00:00+0100"));
    assertThat(result.getDocs().get(1).closeDate()).isEqualTo(parseDateTime("2014-09-23T00:00:00+0100"));
    assertThat(result.getDocs().get(2).closeDate()).isNull();
  }

  @Test
  public void sort_by_file_and_line() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file1 = ComponentTesting.newFileDto(project, null, "F1").setPath("src/main/xoo/org/sonar/samples/File.xoo");
    ComponentDto file2 = ComponentTesting.newFileDto(project, null, "F2").setPath("src/main/xoo/org/sonar/samples/File2.xoo");

    indexIssues(
      // file F1
      IssueTesting.newDoc("F1_2", file1).setLine(20),
      IssueTesting.newDoc("F1_1", file1).setLine(null),
      IssueTesting.newDoc("F1_3", file1).setLine(25),

      // file F2
      IssueTesting.newDoc("F2_1", file2).setLine(9),
      IssueTesting.newDoc("F2_2", file2).setLine(109),
      // two issues on the same line -> sort by key
      IssueTesting.newDoc("F2_3", file2).setLine(109));

    // ascending sort -> F1 then F2. Line "0" first.
    IssueQuery.Builder query = IssueQuery.builder(userSessionRule).sort(IssueQuery.SORT_BY_FILE_LINE).asc(true);
    SearchResult<IssueDoc> result = underTest.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(6);
    assertThat(result.getDocs().get(0).key()).isEqualTo("F1_1");
    assertThat(result.getDocs().get(1).key()).isEqualTo("F1_2");
    assertThat(result.getDocs().get(2).key()).isEqualTo("F1_3");
    assertThat(result.getDocs().get(3).key()).isEqualTo("F2_1");
    assertThat(result.getDocs().get(4).key()).isEqualTo("F2_2");
    assertThat(result.getDocs().get(5).key()).isEqualTo("F2_3");

    // descending sort -> F2 then F1
    query = IssueQuery.builder(userSessionRule).sort(IssueQuery.SORT_BY_FILE_LINE).asc(false);
    result = underTest.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(6);
    assertThat(result.getDocs().get(0).key()).isEqualTo("F2_3");
    assertThat(result.getDocs().get(1).key()).isEqualTo("F2_2");
    assertThat(result.getDocs().get(2).key()).isEqualTo("F2_1");
    assertThat(result.getDocs().get(3).key()).isEqualTo("F1_3");
    assertThat(result.getDocs().get(4).key()).isEqualTo("F1_2");
    assertThat(result.getDocs().get(5).key()).isEqualTo("F1_1");
  }

  @Test
  public void authorized_issues_on_groups() {
    ComponentDto project1 = ComponentTesting.newProjectDto().setKey("project1");
    ComponentDto project2 = ComponentTesting.newProjectDto().setKey("project2");
    ComponentDto project3 = ComponentTesting.newProjectDto().setKey("project3");

    ComponentDto file1 = ComponentTesting.newFileDto(project1, null).setKey("file1");
    ComponentDto file2 = ComponentTesting.newFileDto(project2, null).setKey("file2");
    ComponentDto file3 = ComponentTesting.newFileDto(project3, null).setKey("file3");

    // project1 can be seen by sonar-users
    indexIssue(IssueTesting.newDoc("ISSUE1", file1), "sonar-users", null);
    // project2 can be seen by sonar-admins
    indexIssue(IssueTesting.newDoc("ISSUE2", file2), "sonar-admins", null);
    // project3 can be seen by nobody
    indexIssue(IssueTesting.newDoc("ISSUE3", file3), null, null);

    userSessionRule.login().setUserGroups("sonar-users");
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions()).getDocs()).hasSize(1);

    userSessionRule.login().setUserGroups("sonar-admins");
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions()).getDocs()).hasSize(1);

    userSessionRule.login().setUserGroups("sonar-users", "sonar-admins");
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions()).getDocs()).hasSize(2);

    userSessionRule.login().setUserGroups("another group");
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions()).getDocs()).isEmpty();

    userSessionRule.login().setUserGroups("sonar-users", "sonar-admins");
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).projectUuids(newArrayList(project3.uuid())).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void authorized_issues_on_user() {
    ComponentDto project1 = ComponentTesting.newProjectDto().setKey("project1");
    ComponentDto project2 = ComponentTesting.newProjectDto().setKey("project2");
    ComponentDto project3 = ComponentTesting.newProjectDto().setKey("project3");

    ComponentDto file1 = ComponentTesting.newFileDto(project1, null).setKey("file1");
    ComponentDto file2 = ComponentTesting.newFileDto(project2, null).setKey("file2");
    ComponentDto file3 = ComponentTesting.newFileDto(project3, null).setKey("file3");

    // project1 can be seen by john, project2 by max, project3 cannot be seen by anyone
    indexIssue(IssueTesting.newDoc("ISSUE1", file1), null, "john");
    indexIssue(IssueTesting.newDoc("ISSUE2", file2), null, "max");
    indexIssue(IssueTesting.newDoc("ISSUE3", file3), null, null);

    userSessionRule.login("john");

    assertThat(underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions()).getDocs()).hasSize(1);

    userSessionRule.login("max");
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions()).getDocs()).hasSize(1);

    userSessionRule.login("another guy");
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions()).getDocs()).hasSize(0);

    userSessionRule.login("john");
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).projectUuids(newArrayList(project3.key())).build(), new SearchOptions()).getDocs()).hasSize(0);
  }

  @Test
  public void authorized_issues_on_user_and_group() {
    ComponentDto project1 = ComponentTesting.newProjectDto().setKey("project1");
    ComponentDto project2 = ComponentTesting.newProjectDto().setKey("project2");

    ComponentDto file1 = ComponentTesting.newFileDto(project1, null).setKey("file1");
    ComponentDto file2 = ComponentTesting.newFileDto(project2, null).setKey("file2");

    // project1 can be seen by john and by sonar-users
    indexIssue(IssueTesting.newDoc("ISSUE1", file1), "sonar-users", "john");
    indexIssue(IssueTesting.newDoc("ISSUE2", file2), null, "max");

    userSessionRule.login("john").setUserGroups("sonar-users");
    assertThat(underTest.search(IssueQuery.builder(userSessionRule).build(), new SearchOptions()).getDocs()).hasSize(1);
  }

  @Test
  public void search_issues_for_batch_return_needed_fields() {
    ComponentDto project = ComponentTesting.newProjectDto("PROJECT");
    ComponentDto file = ComponentTesting.newFileDto(project, null).setPath("src/File.xoo");

    IssueDoc issue = IssueTesting.newDoc("ISSUE", file)
      .setRuleKey("squid:S001")
      .setChecksum("12345")
      .setAssignee("john")
      .setLine(11)
      .setMessage("the message")
      .setSeverity(Severity.BLOCKER)
      .setManualSeverity(true)
      .setStatus(Issue.STATUS_RESOLVED)
      .setResolution(Issue.RESOLUTION_FIXED)
      .setFuncCreationDate(new Date());
    indexIssues(issue);

    List<IssueDoc> issues = Lists.newArrayList(underTest.selectIssuesForBatch(file));
    assertThat(issues).hasSize(1);
    IssueDoc result = issues.get(0);
    assertThat(result.key()).isEqualTo("ISSUE");
    assertThat(result.moduleUuid()).isEqualTo("PROJECT");
    assertThat(result.filePath()).isEqualTo("src/File.xoo");
    assertThat(result.ruleKey()).isEqualTo(RuleKey.of("squid", "S001"));
    assertThat(result.checksum()).isEqualTo("12345");
    assertThat(result.assignee()).isEqualTo("john");
    assertThat(result.line()).isEqualTo(11);
    assertThat(result.message()).isEqualTo("the message");
    assertThat(result.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(result.isManualSeverity()).isTrue();
    assertThat(result.status()).isEqualTo(Issue.STATUS_RESOLVED);
    assertThat(result.resolution()).isEqualTo(Issue.RESOLUTION_FIXED);
    assertThat(result.creationDate()).isNotNull();
  }

  @Test
  public void search_issues_for_batch() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto module = ComponentTesting.newModuleDto(project);
    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    ComponentDto file = ComponentTesting.newFileDto(subModule, null);

    indexIssues(
      IssueTesting.newDoc("ISSUE3", module),
      IssueTesting.newDoc("ISSUE5", subModule),
      IssueTesting.newDoc("ISSUE2", file),
      // Close Issue, should never be returned
      IssueTesting.newDoc("CLOSE_ISSUE", file).setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED));

    assertThat(Lists.newArrayList(underTest.selectIssuesForBatch(project))).hasSize(3);
    assertThat(Lists.newArrayList(underTest.selectIssuesForBatch(module))).hasSize(3);
    assertThat(Lists.newArrayList(underTest.selectIssuesForBatch(subModule))).hasSize(2);
    assertThat(Lists.newArrayList(underTest.selectIssuesForBatch(file))).hasSize(1);
    assertThat(Lists.newArrayList(underTest.selectIssuesForBatch(ComponentTesting.newProjectDto()))).isEmpty();
  }

  @Test
  public void fail_to_search_issues_for_batch_on_not_allowed_scope() {
    try {
      underTest.selectIssuesForBatch(new ComponentDto().setScope(Scopes.DIRECTORY));
      failBecauseExceptionWasNotThrown(IllegalStateException.class);
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Component of scope 'DIR' is not allowed");
    }
  }

  @Test
  public void search_issues_for_batch_return_only_authorized_issues() {
    ComponentDto project1 = ComponentTesting.newProjectDto().setKey("project1");
    ComponentDto project2 = ComponentTesting.newProjectDto().setKey("project2");

    ComponentDto file1 = ComponentTesting.newFileDto(project1, null).setKey("file1");
    ComponentDto file2 = ComponentTesting.newFileDto(project2, null).setKey("file2");

    // project1 can be seen by sonar-users
    indexIssue(IssueTesting.newDoc("ISSUE1", file1), "sonar-users", null);
    // project3 can be seen by nobody
    indexIssue(IssueTesting.newDoc("ISSUE3", file2), null, null);

    userSessionRule.setUserGroups("sonar-users");
    assertThat(Lists.newArrayList(underTest.selectIssuesForBatch(project1))).hasSize(1);

    userSessionRule.setUserGroups("another group");
    assertThat(Lists.newArrayList(underTest.selectIssuesForBatch(project2))).isEmpty();
  }

  private void indexIssues(IssueDoc... issues) {
    issueIndexer.index(Arrays.asList(issues).iterator());
    for (IssueDoc issue : issues) {
      addIssueAuthorization(issue.projectUuid(), DefaultGroups.ANYONE, null);
    }
  }

  private void indexIssue(IssueDoc issue, @Nullable String group, @Nullable String user) {
    issueIndexer.index(Iterators.singletonIterator(issue));
    addIssueAuthorization(issue.projectUuid(), group, user);
  }

  private void addIssueAuthorization(String projectUuid, @Nullable String group, @Nullable String user) {
    authorizationIndexerTester.insertProjectAuthorization(projectUuid, singletonList(group), singletonList(user));
  }

  private void indexView(String viewUuid, List<String> projects) {
    viewIndexer.index(new ViewDoc().setUuid(viewUuid).setProjects(projects));
  }
}
