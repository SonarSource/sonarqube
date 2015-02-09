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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import org.assertj.core.api.Fail;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.core.component.ComponentDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.view.index.ViewDoc;
import org.sonar.server.view.index.ViewIndexer;

import javax.annotation.Nullable;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * As soon as IssueIndex take {@link org.sonar.server.es.EsClient} in its constructor, ServerTester should be replaced by EsTester, it will make this test going faster !
 */
public class IssueIndexMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  IssueIndex index;

  @Before
  public void setUp() throws Exception {
    tester.clearIndexes();
    index = tester.get(IssueIndex.class);
  }

  @Test
  public void get_by_key() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    IssueDoc issue = IssueTesting.newDoc("ISSUE1", file);
    indexIssues(issue);

    Issue loaded = index.getByKey(issue.key());
    assertThat(loaded).isNotNull();
  }

  @Test
  public void get_by_key_with_attributes() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    IssueDoc issue = IssueTesting.newDoc("ISSUE1", file).setAttributes((KeyValueFormat.format(ImmutableMap.of("jira-issue-key", "SONAR-1234"))));
    indexIssues(issue);

    Issue result = index.getByKey(issue.key());
    assertThat(result.attribute("jira-issue-key")).isEqualTo("SONAR-1234");
  }

  @Test(expected = IllegalStateException.class)
  public void comments_field_is_not_available() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    IssueDoc issue = IssueTesting.newDoc("ISSUE1", file);
    indexIssues(issue);

    Issue result = index.getByKey(issue.key());
    result.comments();
  }

  @Test(expected = IllegalStateException.class)
  public void is_new_field_is_not_available() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    IssueDoc issue = IssueTesting.newDoc("ISSUE1", file);
    indexIssues(issue);

    Issue result = index.getByKey(issue.key());
    result.isNew();
  }

  @Test(expected = NotFoundException.class)
  public void fail_to_get_unknown_key() throws Exception {
    index.getByKey("unknown");
  }

  @Test
  public void filter_by_keys() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();

    indexIssues(
      IssueTesting.newDoc("1", ComponentTesting.newFileDto(project)),
      IssueTesting.newDoc("2", ComponentTesting.newFileDto(project)));

    assertThat(index.search(IssueQuery.builder().issueKeys(newArrayList("1", "2")).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().issueKeys(newArrayList("1")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().issueKeys(newArrayList("3", "4")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void filter_by_projects() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto module = ComponentTesting.newModuleDto(project);
    ComponentDto subModule = ComponentTesting.newModuleDto(module);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", project),
      IssueTesting.newDoc("ISSUE2", ComponentTesting.newFileDto(project)),
      IssueTesting.newDoc("ISSUE3", module),
      IssueTesting.newDoc("ISSUE4", ComponentTesting.newFileDto(module)),
      IssueTesting.newDoc("ISSUE5", subModule),
      IssueTesting.newDoc("ISSUE6", ComponentTesting.newFileDto(subModule)));

    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList(project.uuid())).build(), new SearchOptions()).getDocs()).hasSize(6);
    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList("unknown")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facets_on_projects() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD");
    ComponentDto project2 = ComponentTesting.newProjectDto("EFGH");

    indexIssues(
      IssueTesting.newDoc("ISSUE1", ComponentTesting.newFileDto(project)),
      IssueTesting.newDoc("ISSUE2", ComponentTesting.newFileDto(project)),
      IssueTesting.newDoc("ISSUE3", ComponentTesting.newFileDto(project2)));

    SearchResult<IssueDoc> result = index.search(IssueQuery.builder().build(), new SearchOptions().addFacets(newArrayList("projectUuids")));
    assertThat(result.getFacets().getNames()).containsOnly("projectUuids");
    assertThat(result.getFacets().get("projectUuids")).containsOnly(entry("ABCD", 2L), entry("EFGH", 1L));
  }

  @Test
  public void filter_by_modules() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto module = ComponentTesting.newModuleDto(project);
    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    ComponentDto file = ComponentTesting.newFileDto(subModule);

    indexIssues(
      IssueTesting.newDoc("ISSUE3", module),
      IssueTesting.newDoc("ISSUE5", subModule),
      IssueTesting.newDoc("ISSUE2", file));

    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList(project.uuid()))
      .moduleUuids(newArrayList(file.uuid())).build(), new SearchOptions()).getDocs()).isEmpty();
    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList(project.uuid()))
      .moduleUuids(newArrayList(module.uuid())).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList(project.uuid()))
      .moduleUuids(newArrayList(subModule.uuid())).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList(project.uuid()))
      .moduleUuids(newArrayList(project.uuid())).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList(project.uuid()))
      .moduleUuids(newArrayList("unknown")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void filter_by_components_on_contextualized_search() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto module = ComponentTesting.newModuleDto(project);
    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    ComponentDto file1 = ComponentTesting.newFileDto(project);
    ComponentDto file2 = ComponentTesting.newFileDto(module);
    ComponentDto file3 = ComponentTesting.newFileDto(subModule);
    String view = "ABCD";
    indexView(view, newArrayList(project.uuid()));

    indexIssues(
      IssueTesting.newDoc("ISSUE1", project),
      IssueTesting.newDoc("ISSUE2", file1),
      IssueTesting.newDoc("ISSUE3", module),
      IssueTesting.newDoc("ISSUE4", file2),
      IssueTesting.newDoc("ISSUE5", subModule),
      IssueTesting.newDoc("ISSUE6", file3));

    assertThat(index.search(IssueQuery.builder().fileUuids(newArrayList(file1.uuid(), file2.uuid(), file3.uuid())).build(), new SearchOptions())
      .getDocs()).hasSize(3);
    assertThat(index.search(IssueQuery.builder().fileUuids(newArrayList(file1.uuid())).build(), new SearchOptions())
      .getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().moduleRootUuids(newArrayList(subModule.uuid())).build(), new SearchOptions())
      .getDocs()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().moduleRootUuids(newArrayList(module.uuid())).build(), new SearchOptions())
      .getDocs()).hasSize(4);
    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList(project.uuid())).build(), new SearchOptions())
      .getDocs()).hasSize(6);
    assertThat(index.search(IssueQuery.builder().viewUuids(newArrayList(view)).build(), new SearchOptions())
      .getDocs()).hasSize(6);
    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList("unknown")).build(), new SearchOptions())
      .getDocs()).isEmpty();
  }

  @Test
  public void filter_by_components_on_non_contextualized_search() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto("project");
    ComponentDto file1 = ComponentTesting.newFileDto(project, "file1");
    ComponentDto module = ComponentTesting.newModuleDto(project).setUuid("module");
    ComponentDto file2 = ComponentTesting.newFileDto(module, "file2");
    ComponentDto subModule = ComponentTesting.newModuleDto(module).setUuid("subModule");
    ComponentDto file3 = ComponentTesting.newFileDto(subModule, "file3");
    String view = "ABCD";
    indexView(view, newArrayList(project.uuid()));

    indexIssues(
      IssueTesting.newDoc("ISSUE1", project),
      IssueTesting.newDoc("ISSUE2", file1),
      IssueTesting.newDoc("ISSUE3", module),
      IssueTesting.newDoc("ISSUE4", file2),
      IssueTesting.newDoc("ISSUE5", subModule),
      IssueTesting.newDoc("ISSUE6", file3));

    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList("unknown")).build(), new SearchOptions()).getDocs()).isEmpty();
    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList(project.uuid())).build(), new SearchOptions()).getDocs()).hasSize(6);
    assertThat(index.search(IssueQuery.builder().viewUuids(newArrayList(view)).build(), new SearchOptions()).getDocs()).hasSize(6);
    assertThat(index.search(IssueQuery.builder().moduleUuids(newArrayList(module.uuid())).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().moduleUuids(newArrayList(subModule.uuid())).build(), new SearchOptions()).getDocs()).hasSize(1); // XXX
                                                                                                                                                  // Misleading
                                                                                                                                                  // !
    assertThat(index.search(IssueQuery.builder().fileUuids(newArrayList(file1.uuid())).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().fileUuids(newArrayList(file1.uuid(), file2.uuid(), file3.uuid())).build(), new SearchOptions()).getDocs()).hasSize(3);
  }

  @Test
  public void facets_on_components() throws Exception {
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

    SearchResult<IssueDoc> result = index.search(IssueQuery.builder().build(), new SearchOptions().addFacets(newArrayList("fileUuids")));
    assertThat(result.getFacets().getNames()).containsOnly("fileUuids");
    assertThat(result.getFacets().get("fileUuids"))
      .containsOnly(entry("A", 1L), entry("ABCD", 1L), entry("BCDE", 2L), entry("CDEF", 1L));
  }

  @Test
  public void filter_by_directories() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file1 = ComponentTesting.newFileDto(project).setPath("src/main/xoo/F1.xoo");
    ComponentDto file2 = ComponentTesting.newFileDto(project).setPath("F2.xoo");

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file1).setDirectoryPath("/src/main/xoo"),
      IssueTesting.newDoc("ISSUE2", file2).setDirectoryPath("/"));

    assertThat(index.search(IssueQuery.builder().directories(newArrayList("/src/main/xoo")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().directories(newArrayList("/")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().directories(newArrayList("unknown")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facets_on_directories() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file1 = ComponentTesting.newFileDto(project).setPath("src/main/xoo/F1.xoo");
    ComponentDto file2 = ComponentTesting.newFileDto(project).setPath("F2.xoo");

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file1).setDirectoryPath("/src/main/xoo"),
      IssueTesting.newDoc("ISSUE2", file2).setDirectoryPath("/"));

    SearchResult<IssueDoc> result = index.search(IssueQuery.builder().build(), new SearchOptions().addFacets(newArrayList("directories")));
    assertThat(result.getFacets().getNames()).containsOnly("directories");
    assertThat(result.getFacets().get("directories")).containsOnly(entry("/src/main/xoo", 1L), entry("/", 1L));
  }

  @Test
  public void filter_by_views() throws Exception {
    ComponentDto project1 = ComponentTesting.newProjectDto();
    ComponentDto file1 = ComponentTesting.newFileDto(project1);
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

    assertThat(index.search(IssueQuery.builder().viewUuids(newArrayList(view1)).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().viewUuids(newArrayList(view2)).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().viewUuids(newArrayList(view1, view2)).build(), new SearchOptions()).getDocs()).hasSize(3);
    assertThat(index.search(IssueQuery.builder().viewUuids(newArrayList("unknown")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void filter_by_severities() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setSeverity(Severity.INFO),
      IssueTesting.newDoc("ISSUE2", file).setSeverity(Severity.MAJOR));

    assertThat(index.search(IssueQuery.builder().severities(newArrayList(Severity.INFO, Severity.MAJOR)).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().severities(newArrayList(Severity.INFO)).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().severities(newArrayList(Severity.BLOCKER)).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facets_on_severities() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setSeverity(Severity.INFO),
      IssueTesting.newDoc("ISSUE2", file).setSeverity(Severity.INFO),
      IssueTesting.newDoc("ISSUE3", file).setSeverity(Severity.MAJOR));

    SearchResult<IssueDoc> result = index.search(IssueQuery.builder().build(), new SearchOptions().addFacets(newArrayList("severities")));
    assertThat(result.getFacets().getNames()).containsOnly("severities");
    assertThat(result.getFacets().get("severities")).containsOnly(entry("INFO", 2L), entry("MAJOR", 1L));
  }

  @Test
  public void filter_by_statuses() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setStatus(Issue.STATUS_CLOSED),
      IssueTesting.newDoc("ISSUE2", file).setStatus(Issue.STATUS_OPEN));

    assertThat(index.search(IssueQuery.builder().statuses(newArrayList(Issue.STATUS_CLOSED, Issue.STATUS_OPEN)).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().statuses(newArrayList(Issue.STATUS_CLOSED)).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().statuses(newArrayList(Issue.STATUS_CONFIRMED)).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facets_on_statuses() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setStatus(Issue.STATUS_CLOSED),
      IssueTesting.newDoc("ISSUE2", file).setStatus(Issue.STATUS_CLOSED),
      IssueTesting.newDoc("ISSUE3", file).setStatus(Issue.STATUS_OPEN));

    SearchResult<IssueDoc> result = index.search(IssueQuery.builder().build(), new SearchOptions().addFacets(newArrayList("statuses")));
    assertThat(result.getFacets().getNames()).containsOnly("statuses");
    assertThat(result.getFacets().get("statuses")).containsOnly(entry("CLOSED", 2L), entry("OPEN", 1L));
  }

  @Test
  public void filter_by_resolutions() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      IssueTesting.newDoc("ISSUE2", file).setResolution(Issue.RESOLUTION_FIXED));

    assertThat(index.search(IssueQuery.builder().resolutions(newArrayList(Issue.RESOLUTION_FALSE_POSITIVE, Issue.RESOLUTION_FIXED)).build(), new SearchOptions()).getDocs())
      .hasSize(2);
    assertThat(index.search(IssueQuery.builder().resolutions(newArrayList(Issue.RESOLUTION_FALSE_POSITIVE)).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().resolutions(newArrayList(Issue.RESOLUTION_REMOVED)).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facets_on_resolutions() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      IssueTesting.newDoc("ISSUE2", file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      IssueTesting.newDoc("ISSUE3", file).setResolution(Issue.RESOLUTION_FIXED));

    SearchResult<IssueDoc> result = index.search(IssueQuery.builder().build(), new SearchOptions().addFacets(newArrayList("resolutions")));
    assertThat(result.getFacets().getNames()).containsOnly("resolutions");
    assertThat(result.getFacets().get("resolutions")).containsOnly(entry("FALSE-POSITIVE", 2L), entry("FIXED", 1L));
  }

  @Test
  public void filter_by_resolved() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED),
      IssueTesting.newDoc("ISSUE2", file).setStatus(Issue.STATUS_OPEN).setResolution(null),
      IssueTesting.newDoc("ISSUE3", file).setStatus(Issue.STATUS_OPEN).setResolution(null));

    assertThat(index.search(IssueQuery.builder().resolved(true).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().resolved(false).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().resolved(null).build(), new SearchOptions()).getDocs()).hasSize(3);
  }

  @Test
  public void filter_by_action_plans() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setActionPlanKey("plan1"),
      IssueTesting.newDoc("ISSUE2", file).setActionPlanKey("plan2"));

    assertThat(index.search(IssueQuery.builder().actionPlans(newArrayList("plan1")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().actionPlans(newArrayList("plan1", "plan2")).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().actionPlans(newArrayList("unknown")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facets_on_action_plans() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setActionPlanKey("plan1"),
      IssueTesting.newDoc("ISSUE2", file).setActionPlanKey("plan2"));

    SearchResult<IssueDoc> result = index.search(IssueQuery.builder().build(), new SearchOptions().addFacets(newArrayList("actionPlans")));
    assertThat(result.getFacets().getNames()).containsOnly("actionPlans");
    assertThat(result.getFacets().get("actionPlans")).containsOnly(entry("plan1", 1L), entry("plan2", 1L));
  }

  @Test
  public void filter_by_planned() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setActionPlanKey("AP-KEY"),
      IssueTesting.newDoc("ISSUE2", file).setActionPlanKey(null),
      IssueTesting.newDoc("ISSUE3", file).setActionPlanKey(null));

    assertThat(index.search(IssueQuery.builder().planned(true).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().planned(false).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().planned(null).build(), new SearchOptions()).getDocs()).hasSize(3);
  }

  @Test
  public void filter_by_rules() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    RuleKey ruleKey = RuleKey.of("repo", "X1");

    indexIssues(IssueTesting.newDoc("ISSUE1", file).setRuleKey(ruleKey.toString()));

    assertThat(index.search(IssueQuery.builder().rules(newArrayList(ruleKey)).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().rules(newArrayList(RuleKey.of("rule", "without issue"))).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void filter_by_languages() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    RuleKey ruleKey = RuleKey.of("repo", "X1");

    indexIssues(IssueTesting.newDoc("ISSUE1", file).setRuleKey(ruleKey.toString()).setLanguage("xoo"));

    assertThat(index.search(IssueQuery.builder().languages(newArrayList("xoo")).build(),
      new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().languages(newArrayList("unknown")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facets_on_languages() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    RuleKey ruleKey = RuleKey.of("repo", "X1");

    indexIssues(IssueTesting.newDoc("ISSUE1", file).setRuleKey(ruleKey.toString()).setLanguage("xoo"));

    SearchResult<IssueDoc> result = index.search(IssueQuery.builder().build(), new SearchOptions().addFacets(newArrayList("languages")));
    assertThat(result.getFacets().getNames()).containsOnly("languages");
    assertThat(result.getFacets().get("languages")).containsOnly(entry("xoo", 1L));
  }

  @Test
  public void filter_by_assignees() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAssignee("steph"),
      IssueTesting.newDoc("ISSUE2", file).setAssignee("simon"),
      IssueTesting.newDoc("ISSUE3", file).setAssignee(null));

    assertThat(index.search(IssueQuery.builder().assignees(newArrayList("steph")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().assignees(newArrayList("steph", "simon")).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().assignees(newArrayList("unknown")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facets_on_assignees() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAssignee("steph"),
      IssueTesting.newDoc("ISSUE2", file).setAssignee("simon"),
      IssueTesting.newDoc("ISSUE3", file).setAssignee("simon"),
      IssueTesting.newDoc("ISSUE4", file).setAssignee(null));

    SearchResult<IssueDoc> result = index.search(IssueQuery.builder().build(), new SearchOptions().addFacets(newArrayList("assignees")));
    assertThat(result.getFacets().getNames()).containsOnly("assignees");
    assertThat(result.getFacets().get("assignees")).containsOnly(entry("steph", 1L), entry("simon", 2L), entry("", 1L));
  }

  @Test
  public void filter_by_assigned() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAssignee("steph"),
      IssueTesting.newDoc("ISSUE2", file).setAssignee(null),
      IssueTesting.newDoc("ISSUE3", file).setAssignee(null));

    assertThat(index.search(IssueQuery.builder().assigned(true).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().assigned(false).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().assigned(null).build(), new SearchOptions()).getDocs()).hasSize(3);
  }

  @Test
  public void filter_by_reporters() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setReporter("fabrice"),
      IssueTesting.newDoc("ISSUE2", file).setReporter("stephane"));

    assertThat(index.search(IssueQuery.builder().reporters(newArrayList("fabrice", "stephane")).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().reporters(newArrayList("fabrice")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().reporters(newArrayList("unknown")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void filter_by_authors() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAuthorLogin("steph"),
      IssueTesting.newDoc("ISSUE2", file).setAuthorLogin("simon"),
      IssueTesting.newDoc("ISSUE3", file).setAssignee(null));

    assertThat(index.search(IssueQuery.builder().authors(newArrayList("steph")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().authors(newArrayList("steph", "simon")).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().authors(newArrayList("unknown")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facets_on_authors() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAuthorLogin("steph"),
      IssueTesting.newDoc("ISSUE2", file).setAuthorLogin("simon"),
      IssueTesting.newDoc("ISSUE3", file).setAuthorLogin("simon"),
      IssueTesting.newDoc("ISSUE4", file).setAuthorLogin(null));

    SearchResult<IssueDoc> result = index.search(IssueQuery.builder().build(), new SearchOptions().addFacets(newArrayList("authors")));
    assertThat(result.getFacets().getNames()).containsOnly("authors");
    assertThat(result.getFacets().get("authors")).containsOnly(entry("steph", 1L), entry("simon", 2L));
  }

  @Test
  public void filter_by_created_after() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setFuncCreationDate(DateUtils.parseDate("2014-09-20")),
      IssueTesting.newDoc("ISSUE2", file).setFuncCreationDate(DateUtils.parseDate("2014-09-23")));

    assertThat(index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-19")).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-20")).build(), new SearchOptions()).getDocs()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-21")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-25")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void filter_by_created_before() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setFuncCreationDate(DateUtils.parseDate("2014-09-20")),
      IssueTesting.newDoc("ISSUE2", file).setFuncCreationDate(DateUtils.parseDate("2014-09-23")));

    assertThat(index.search(IssueQuery.builder().createdBefore(DateUtils.parseDate("2014-09-19")).build(), new SearchOptions()).getDocs()).isEmpty();
    assertThat(index.search(IssueQuery.builder().createdBefore(DateUtils.parseDate("2014-09-20")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().createdBefore(DateUtils.parseDate("2014-09-21")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().createdBefore(DateUtils.parseDate("2014-09-25")).build(), new SearchOptions()).getDocs()).hasSize(2);
  }

  @Test
  public void filter_by_created_before_must_be_lower_than_after() throws Exception {
    try {
      index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-20")).createdBefore(DateUtils.parseDate("2014-09-19")).build(), new SearchOptions());
      Fail.failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException exception) {
      assertThat(exception.getMessage()).isEqualTo("Start bound cannot be larger than end bound");
    }
  }

  @Test
  public void filter_by_created_after_must_not_be_in_future() throws Exception {
    try {
      index.search(IssueQuery.builder().createdAfter(new Date(Long.MAX_VALUE)).build(), new SearchOptions());
      Fail.failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException exception) {
      assertThat(exception.getMessage()).isEqualTo("Start bound cannot be in the future");
    }
  }

  @Test
  public void filter_by_created_at() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(IssueTesting.newDoc("ISSUE1", file).setFuncCreationDate(DateUtils.parseDate("2014-09-20")));

    assertThat(index.search(IssueQuery.builder().createdAt(DateUtils.parseDate("2014-09-20")).build(), new SearchOptions()).getDocs()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().createdAt(DateUtils.parseDate("2014-09-21")).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void facet_on_created_at_with_less_than_20_days() throws Exception {

    SearchOptions options = fixtureForCreatedAtFacet();

    IssueQuery query = IssueQuery.builder()
      .createdAfter(DateUtils.parseDate("2014-09-01"))
      .createdBefore(DateUtils.parseDate("2014-09-08"))
      .checkAuthorization(false)
      .build();
    SearchResult<IssueDoc> result = index.search(query, options);
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
  public void facet_on_created_at_with_less_than_20_weeks() throws Exception {

    SearchOptions SearchOptions = fixtureForCreatedAtFacet();

    Map<String, Long> createdAt = index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-01")).createdBefore(DateUtils.parseDate("2014-09-21")).build(),
      SearchOptions).getFacets().get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2014-08-25T01:00:00+0000", 0L),
      entry("2014-09-01T01:00:00+0000", 4L),
      entry("2014-09-08T01:00:00+0000", 0L),
      entry("2014-09-15T01:00:00+0000", 1L));
  }

  @Test
  public void facet_on_created_at_with_less_than_20_months() throws Exception {

    SearchOptions SearchOptions = fixtureForCreatedAtFacet();

    Map<String, Long> createdAt = index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-01")).createdBefore(DateUtils.parseDate("2015-01-19")).build(),
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
  public void facet_on_created_at_with_more_than_20_months() throws Exception {
    SearchOptions SearchOptions = fixtureForCreatedAtFacet();

    Map<String, Long> createdAt = index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2011-01-01")).createdBefore(DateUtils.parseDate("2016-01-01")).build(),
      SearchOptions).getFacets().get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2011-01-01T01:00:00+0000", 1L),
      entry("2012-01-01T01:00:00+0000", 0L),
      entry("2013-01-01T01:00:00+0000", 0L),
      entry("2014-01-01T01:00:00+0000", 5L),
      entry("2015-01-01T01:00:00+0000", 1L),
      entry("2016-01-01T01:00:00+0000", 0L));

  }

  @Test
  public void facet_on_created_at_with_bounds_outside_of_data() throws Exception {
    SearchOptions options = fixtureForCreatedAtFacet();

    Map<String, Long> createdAt = index.search(IssueQuery.builder()
      .createdAfter(DateUtils.parseDate("2009-01-01"))
      .createdBefore(DateUtils.parseDate("2016-01-01"))
      .build(), options).getFacets().get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2009-01-01T01:00:00+0000", 0L),
      entry("2010-01-01T01:00:00+0000", 0L),
      entry("2011-01-01T01:00:00+0000", 1L),
      entry("2012-01-01T01:00:00+0000", 0L),
      entry("2013-01-01T01:00:00+0000", 0L),
      entry("2014-01-01T01:00:00+0000", 5L),
      entry("2015-01-01T01:00:00+0000", 1L),
      entry("2016-01-01T01:00:00+0000", 0L));
  }

  @Test
  public void facet_on_created_at_without_start_bound() throws Exception {
    SearchOptions SearchOptions = fixtureForCreatedAtFacet();

    Map<String, Long> createdAt = index.search(IssueQuery.builder().createdBefore(DateUtils.parseDate("2016-01-01")).build(),
      SearchOptions).getFacets().get("createdAt");
    assertThat(createdAt).containsOnly(
      entry("2011-01-01T01:00:00+0000", 1L),
      entry("2012-01-01T01:00:00+0000", 0L),
      entry("2013-01-01T01:00:00+0000", 0L),
      entry("2014-01-01T01:00:00+0000", 5L),
      entry("2015-01-01T01:00:00+0000", 1L),
      entry("2016-01-01T01:00:00+0000", 0L));
  }

  @Test
  public void facet_on_created_at_without_issues() throws Exception {
    SearchOptions SearchOptions = new SearchOptions().addFacets("createdAt");

    Map<String, Long> createdAt = index.search(IssueQuery.builder().build(),
      SearchOptions).getFacets().get("createdAt");
    assertThat(createdAt).isEmpty();
  }

  protected SearchOptions fixtureForCreatedAtFacet() {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    IssueDoc issue0 = IssueTesting.newDoc("ISSUE0", file).setFuncCreationDate(DateUtils.parseDateTime("2011-04-25T01:05:13+0100"));
    IssueDoc issue1 = IssueTesting.newDoc("ISSUE1", file).setFuncCreationDate(DateUtils.parseDateTime("2014-09-01T12:34:56+0100"));
    IssueDoc issue2 = IssueTesting.newDoc("ISSUE2", file).setFuncCreationDate(DateUtils.parseDateTime("2014-09-01T23:45:60+0100"));
    IssueDoc issue3 = IssueTesting.newDoc("ISSUE3", file).setFuncCreationDate(DateUtils.parseDateTime("2014-09-02T12:34:56+0100"));
    IssueDoc issue4 = IssueTesting.newDoc("ISSUE4", file).setFuncCreationDate(DateUtils.parseDateTime("2014-09-05T12:34:56+0100"));
    IssueDoc issue5 = IssueTesting.newDoc("ISSUE5", file).setFuncCreationDate(DateUtils.parseDateTime("2014-09-20T12:34:56+0100"));
    IssueDoc issue6 = IssueTesting.newDoc("ISSUE6", file).setFuncCreationDate(DateUtils.parseDateTime("2015-01-18T12:34:56+0100"));

    indexIssues(issue0, issue1, issue2, issue3, issue4, issue5, issue6);

    return new SearchOptions().addFacets("createdAt");
  }

  @Test
  public void paging() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    for (int i = 0; i < 12; i++) {
      indexIssues(IssueTesting.newDoc("ISSUE" + i, file));
    }

    IssueQuery.Builder query = IssueQuery.builder();
    // There are 12 issues in total, with 10 issues per page, the page 2 should only contain 2 elements
    SearchResult<IssueDoc> result = index.search(query.build(), new SearchOptions().setPage(2, 10));
    assertThat(result.getDocs()).hasSize(2);
    assertThat(result.getTotal()).isEqualTo(12);

    result = index.search(IssueQuery.builder().build(), new SearchOptions().setOffset(0).setLimit(5));
    assertThat(result.getDocs()).hasSize(5);
    assertThat(result.getTotal()).isEqualTo(12);

    result = index.search(IssueQuery.builder().build(), new SearchOptions().setOffset(2).setLimit(0));
    assertThat(result.getDocs()).hasSize(10);
    assertThat(result.getTotal()).isEqualTo(12);
  }

  @Test
  public void search_with_max_limit() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    List<IssueDoc> issues = newArrayList();
    for (int i = 0; i < 500; i++) {
      String key = "ISSUE" + i;
      issues.add(IssueTesting.newDoc(key, file));
    }
    indexIssues(issues.toArray(new IssueDoc[] {}));

    IssueQuery.Builder query = IssueQuery.builder();
    SearchResult<IssueDoc> result = index.search(query.build(), new SearchOptions().setLimit(Integer.MAX_VALUE));
    assertThat(result.getDocs()).hasSize(SearchOptions.MAX_LIMIT);
  }

  @Test
  public void sort_by_status() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setStatus(Issue.STATUS_OPEN),
      IssueTesting.newDoc("ISSUE2", file).setStatus(Issue.STATUS_CLOSED),
      IssueTesting.newDoc("ISSUE3", file).setStatus(Issue.STATUS_REOPENED));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_STATUS).asc(true);
    SearchResult<IssueDoc> result = index.search(query.build(), new SearchOptions());
    assertThat(result.getDocs().get(0).status()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(result.getDocs().get(1).status()).isEqualTo(Issue.STATUS_OPEN);
    assertThat(result.getDocs().get(2).status()).isEqualTo(Issue.STATUS_REOPENED);

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_STATUS).asc(false);
    result = index.search(query.build(), new SearchOptions());
    assertThat(result.getDocs().get(0).status()).isEqualTo(Issue.STATUS_REOPENED);
    assertThat(result.getDocs().get(1).status()).isEqualTo(Issue.STATUS_OPEN);
    assertThat(result.getDocs().get(2).status()).isEqualTo(Issue.STATUS_CLOSED);
  }

  @Test
  public void sort_by_severity() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setSeverity(Severity.BLOCKER),
      IssueTesting.newDoc("ISSUE2", file).setSeverity(Severity.INFO),
      IssueTesting.newDoc("ISSUE3", file).setSeverity(Severity.MINOR),
      IssueTesting.newDoc("ISSUE4", file).setSeverity(Severity.CRITICAL),
      IssueTesting.newDoc("ISSUE5", file).setSeverity(Severity.MAJOR));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_SEVERITY).asc(true);
    SearchResult<IssueDoc> result = index.search(query.build(), new SearchOptions());
    assertThat(result.getDocs().get(0).severity()).isEqualTo(Severity.INFO);
    assertThat(result.getDocs().get(1).severity()).isEqualTo(Severity.MINOR);
    assertThat(result.getDocs().get(2).severity()).isEqualTo(Severity.MAJOR);
    assertThat(result.getDocs().get(3).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(result.getDocs().get(4).severity()).isEqualTo(Severity.BLOCKER);

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_SEVERITY).asc(false);
    result = index.search(query.build(), new SearchOptions());
    assertThat(result.getDocs().get(0).severity()).isEqualTo(Severity.BLOCKER);
    assertThat(result.getDocs().get(1).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(result.getDocs().get(2).severity()).isEqualTo(Severity.MAJOR);
    assertThat(result.getDocs().get(3).severity()).isEqualTo(Severity.MINOR);
    assertThat(result.getDocs().get(4).severity()).isEqualTo(Severity.INFO);
  }

  @Test
  public void sort_by_assignee() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAssignee("steph"),
      IssueTesting.newDoc("ISSUE2", file).setAssignee("simon"));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_ASSIGNEE).asc(true);
    SearchResult<IssueDoc> result = index.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(2);
    assertThat(result.getDocs().get(0).assignee()).isEqualTo("simon");
    assertThat(result.getDocs().get(1).assignee()).isEqualTo("steph");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_ASSIGNEE).asc(false);
    result = index.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(2);
    assertThat(result.getDocs().get(0).assignee()).isEqualTo("steph");
    assertThat(result.getDocs().get(1).assignee()).isEqualTo("simon");
  }

  @Test
  public void sort_by_creation_date() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setFuncCreationDate(DateUtils.parseDate("2014-09-23")),
      IssueTesting.newDoc("ISSUE2", file).setFuncCreationDate(DateUtils.parseDate("2014-09-24")));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CREATION_DATE).asc(true);
    SearchResult<IssueDoc> result = index.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(2);
    assertThat(result.getDocs().get(0).creationDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
    assertThat(result.getDocs().get(1).creationDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CREATION_DATE).asc(false);
    result = index.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(2);
    assertThat(result.getDocs().get(0).creationDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));
    assertThat(result.getDocs().get(1).creationDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
  }

  @Test
  public void sort_by_update_date() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setFuncUpdateDate(DateUtils.parseDate("2014-09-23")),
      IssueTesting.newDoc("ISSUE2", file).setFuncUpdateDate(DateUtils.parseDate("2014-09-24")));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_UPDATE_DATE).asc(true);
    SearchResult<IssueDoc> result = index.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(2);
    assertThat(result.getDocs().get(0).updateDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
    assertThat(result.getDocs().get(1).updateDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_UPDATE_DATE).asc(false);
    result = index.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(2);
    assertThat(result.getDocs().get(0).updateDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));
    assertThat(result.getDocs().get(1).updateDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
  }

  @Test
  public void sort_by_close_date() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setFuncCloseDate(DateUtils.parseDate("2014-09-23")),
      IssueTesting.newDoc("ISSUE2", file).setFuncCloseDate(DateUtils.parseDate("2014-09-24")),
      IssueTesting.newDoc("ISSUE3", file).setFuncCloseDate(null));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CLOSE_DATE).asc(true);
    SearchResult<IssueDoc> result = index.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(3);
    assertThat(result.getDocs().get(0).closeDate()).isNull();
    assertThat(result.getDocs().get(1).closeDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
    assertThat(result.getDocs().get(2).closeDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CLOSE_DATE).asc(false);
    result = index.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(3);
    assertThat(result.getDocs().get(0).closeDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));
    assertThat(result.getDocs().get(1).closeDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
    assertThat(result.getDocs().get(2).closeDate()).isNull();
  }

  @Test
  public void sort_by_file_and_line() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file1 = ComponentTesting.newFileDto(project, "F1").setPath("src/main/xoo/org/sonar/samples/File.xoo");
    ComponentDto file2 = ComponentTesting.newFileDto(project, "F2").setPath("src/main/xoo/org/sonar/samples/File2.xoo");

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
    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_FILE_LINE).asc(true);
    SearchResult<IssueDoc> result = index.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(6);
    assertThat(result.getDocs().get(0).key()).isEqualTo("F1_1");
    assertThat(result.getDocs().get(1).key()).isEqualTo("F1_2");
    assertThat(result.getDocs().get(2).key()).isEqualTo("F1_3");
    assertThat(result.getDocs().get(3).key()).isEqualTo("F2_1");
    assertThat(result.getDocs().get(4).key()).isEqualTo("F2_2");
    assertThat(result.getDocs().get(5).key()).isEqualTo("F2_3");

    // descending sort -> F2 then F1
    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_FILE_LINE).asc(false);
    result = index.search(query.build(), new SearchOptions());
    assertThat(result.getDocs()).hasSize(6);
    assertThat(result.getDocs().get(0).key()).isEqualTo("F2_3");
    assertThat(result.getDocs().get(1).key()).isEqualTo("F2_2");
    assertThat(result.getDocs().get(2).key()).isEqualTo("F2_1");
    assertThat(result.getDocs().get(3).key()).isEqualTo("F1_3");
    assertThat(result.getDocs().get(4).key()).isEqualTo("F1_2");
    assertThat(result.getDocs().get(5).key()).isEqualTo("F1_1");
  }

  @Test
  public void authorized_issues_on_groups() throws Exception {
    ComponentDto project1 = ComponentTesting.newProjectDto().setKey("project1");
    ComponentDto project2 = ComponentTesting.newProjectDto().setKey("project2");
    ComponentDto project3 = ComponentTesting.newProjectDto().setKey("project3");

    ComponentDto file1 = ComponentTesting.newFileDto(project1).setKey("file1");
    ComponentDto file2 = ComponentTesting.newFileDto(project2).setKey("file2");
    ComponentDto file3 = ComponentTesting.newFileDto(project3).setKey("file3");

    // project1 can be seen by sonar-users
    indexIssue(IssueTesting.newDoc("ISSUE1", file1), "sonar-users", null);
    // project2 can be seen by sonar-admins
    indexIssue(IssueTesting.newDoc("ISSUE2", file2), "sonar-admins", null);
    // project3 can be seen by nobody
    indexIssue(IssueTesting.newDoc("ISSUE3", file3), null, null);

    MockUserSession.set().setUserGroups("sonar-users");
    assertThat(index.search(IssueQuery.builder().build(), new SearchOptions()).getDocs()).hasSize(1);

    MockUserSession.set().setUserGroups("sonar-admins");
    assertThat(index.search(IssueQuery.builder().build(), new SearchOptions()).getDocs()).hasSize(1);

    MockUserSession.set().setUserGroups("sonar-users", "sonar-admins");
    assertThat(index.search(IssueQuery.builder().build(), new SearchOptions()).getDocs()).hasSize(2);

    MockUserSession.set().setUserGroups("another group");
    assertThat(index.search(IssueQuery.builder().build(), new SearchOptions()).getDocs()).isEmpty();

    MockUserSession.set().setUserGroups("sonar-users", "sonar-admins");
    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList(project3.uuid())).build(), new SearchOptions()).getDocs()).isEmpty();
  }

  @Test
  public void authorized_issues_on_user() throws Exception {
    ComponentDto project1 = ComponentTesting.newProjectDto().setKey("project1");
    ComponentDto project2 = ComponentTesting.newProjectDto().setKey("project2");
    ComponentDto project3 = ComponentTesting.newProjectDto().setKey("project3");

    ComponentDto file1 = ComponentTesting.newFileDto(project1).setKey("file1");
    ComponentDto file2 = ComponentTesting.newFileDto(project2).setKey("file2");
    ComponentDto file3 = ComponentTesting.newFileDto(project3).setKey("file3");

    // project1 can be seen by john, project2 by max, project3 cannot be seen by anyone
    indexIssue(IssueTesting.newDoc("ISSUE1", file1), null, "john");
    indexIssue(IssueTesting.newDoc("ISSUE2", file2), null, "max");
    indexIssue(IssueTesting.newDoc("ISSUE3", file3), null, null);

    MockUserSession.set().setLogin("john");

    assertThat(index.search(IssueQuery.builder().build(), new SearchOptions()).getDocs()).hasSize(1);

    MockUserSession.set().setLogin("max");
    assertThat(index.search(IssueQuery.builder().build(), new SearchOptions()).getDocs()).hasSize(1);

    MockUserSession.set().setLogin("another guy");
    assertThat(index.search(IssueQuery.builder().build(), new SearchOptions()).getDocs()).hasSize(0);

    MockUserSession.set().setLogin("john");
    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList(project3.key())).build(), new SearchOptions()).getDocs()).hasSize(0);
  }

  @Test
  public void authorized_issues_on_user_and_group() throws Exception {
    ComponentDto project1 = ComponentTesting.newProjectDto().setKey("project1");
    ComponentDto project2 = ComponentTesting.newProjectDto().setKey("project2");

    ComponentDto file1 = ComponentTesting.newFileDto(project1).setKey("file1");
    ComponentDto file2 = ComponentTesting.newFileDto(project2).setKey("file2");

    // project1 can be seen by john and by sonar-users
    indexIssue(IssueTesting.newDoc("ISSUE1", file1), "sonar-users", "john");
    indexIssue(IssueTesting.newDoc("ISSUE2", file2), null, "max");

    IssueQuery.Builder query = IssueQuery.builder();
    MockUserSession.set().setLogin("john").setUserGroups("sonar-users");
    assertThat(index.search(query.build(), new SearchOptions()).getDocs()).hasSize(1);
  }

  @Test
  public void list_assignees() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAssignee("steph").setStatus(Issue.STATUS_OPEN),
      IssueTesting.newDoc("ISSUE2", file).setAssignee("simon").setStatus(Issue.STATUS_OPEN),
      IssueTesting.newDoc("ISSUE3", file).setAssignee(null).setStatus(Issue.STATUS_OPEN),
      IssueTesting.newDoc("ISSUE4", file).setAssignee("steph").setStatus(Issue.STATUS_OPEN),
      // Issue assigned to julien should not be returned as the issue is closed
      IssueTesting.newDoc("ISSUE5", file).setAssignee("julien").setStatus(Issue.STATUS_CLOSED));

    LinkedHashMap<String, Long> results = index.searchForAssignees(IssueQuery.builder().statuses(newArrayList(Issue.STATUS_OPEN)).build());
    assertThat(results).hasSize(3);

    Iterator<Map.Entry<String, Long>> buckets = results.entrySet().iterator();

    Map.Entry<String, Long> bucket = buckets.next();
    assertThat(bucket.getKey()).isEqualTo("steph");
    assertThat(bucket.getValue()).isEqualTo(2L);

    bucket = buckets.next();
    assertThat(bucket.getKey()).isEqualTo("simon");
    assertThat(bucket.getValue()).isEqualTo(1L);

    bucket = buckets.next();
    assertThat(bucket.getKey()).isEqualTo("_notAssigned_");
    assertThat(bucket.getValue()).isEqualTo(1L);
  }

  @Test
  public void delete_closed_issues_from_one_project_older_than_specific_date() {
    // ARRANGE
    Date today = new Date();
    Date yesterday = org.apache.commons.lang.time.DateUtils.addDays(today, -1);
    Date beforeYesterday = org.apache.commons.lang.time.DateUtils.addDays(yesterday, -1);

    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setFuncCloseDate(today),
      IssueTesting.newDoc("ISSUE2", file).setFuncCloseDate(beforeYesterday),
      IssueTesting.newDoc("ISSUE3", file).setFuncCloseDate(null));

    // ACT
    index.deleteClosedIssuesOfProjectBefore(project.uuid(), yesterday);

    // ASSERT
    List<IssueDoc> issues = index.search(IssueQuery.builder().projectUuids(newArrayList(project.uuid())).build(), new SearchOptions()).getDocs();
    List<Date> dates = newArrayList();
    for (IssueDoc issue : issues) {
      dates.add(issue.closeDate());
    }

    assertThat(index.countAll()).isEqualTo(2);
    assertThat(dates).containsOnly(null, today);
  }

  private void indexIssues(IssueDoc... issues) {
    tester.get(IssueIndexer.class).index(Arrays.asList(issues).iterator());
    for (IssueDoc issue : issues) {
      addIssueAuthorization(issue.projectUuid(), DefaultGroups.ANYONE, null);
    }
  }

  private void indexIssue(IssueDoc issue, @Nullable String group, @Nullable String user) {
    tester.get(IssueIndexer.class).index(Iterators.singletonIterator(issue));
    addIssueAuthorization(issue.projectUuid(), group, user);
  }

  private void addIssueAuthorization(String projectUuid, @Nullable String group, @Nullable String user) {
    tester.get(IssueAuthorizationIndexer.class).index(newArrayList(new IssueAuthorizationDao.Dto(projectUuid, 1).addGroup(group).addUser(user)));
  }

  private void indexView(String viewUuid, List<String> projects) {
    tester.get(ViewIndexer.class).index(new ViewDoc().setUuid(viewUuid).setProjects(projects));
  }
}
