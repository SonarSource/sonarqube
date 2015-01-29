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
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
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
import org.sonar.server.es.EsClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.view.index.ViewDoc;
import org.sonar.server.view.index.ViewIndexDefinition;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * As soon as IssueIndex take {@link org.sonar.server.es.EsClient} in its constructor, ServerTester should be replaced by EsTester, it will make this test going faster !
 */
public class IssueIndexMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().setProperty("sonar.log.profilingLevel", "FULL").setProperty("sonar.search.httpPort", "9999");

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

    assertThat(index.search(IssueQuery.builder().issueKeys(newArrayList("1", "2")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().issueKeys(newArrayList("1")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().issueKeys(newArrayList("3", "4")).build(), new QueryContext()).getHits()).isEmpty();
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

    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList(project.uuid())).build(), new QueryContext()).getHits()).hasSize(6);
    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void facets_on_projects() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD");
    ComponentDto project2 = ComponentTesting.newProjectDto("EFGH");

    indexIssues(
      IssueTesting.newDoc("ISSUE1", ComponentTesting.newFileDto(project)),
      IssueTesting.newDoc("ISSUE2", ComponentTesting.newFileDto(project)),
      IssueTesting.newDoc("ISSUE3", ComponentTesting.newFileDto(project2)));

    Result<Issue> result = index.search(IssueQuery.builder().build(), new QueryContext().addFacets(newArrayList("projectUuids")));
    assertThat(result.getFacets()).containsOnlyKeys("projectUuids");
    assertThat(result.getFacets().get("projectUuids")).containsOnly(new FacetValue("ABCD", 2), new FacetValue("EFGH", 1));
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
      .moduleUuids(newArrayList(file.uuid())).build(), new QueryContext()).getHits()).isEmpty();
    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList(project.uuid()))
      .moduleUuids(newArrayList(module.uuid())).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList(project.uuid()))
      .moduleUuids(newArrayList(subModule.uuid())).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList(project.uuid()))
      .moduleUuids(newArrayList(project.uuid())).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList(project.uuid()))
      .moduleUuids(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_components_on_contextualized_search() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto module = ComponentTesting.newModuleDto(project);
    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    ComponentDto file1 = ComponentTesting.newFileDto(project);
    ComponentDto file2 = ComponentTesting.newFileDto(module);
    ComponentDto file3 = ComponentTesting.newFileDto(subModule);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", project),
      IssueTesting.newDoc("ISSUE2", file1),
      IssueTesting.newDoc("ISSUE3", module),
      IssueTesting.newDoc("ISSUE4", file2),
      IssueTesting.newDoc("ISSUE5", subModule),
      IssueTesting.newDoc("ISSUE6", file3));

    assertThat(index.search(IssueQuery.builder().setContextualized(true).fileUuids(newArrayList(file1.uuid(), file2.uuid(), file3.uuid())).build(), new QueryContext())
      .getHits()).hasSize(3);
    assertThat(index.search(IssueQuery.builder().setContextualized(true).fileUuids(newArrayList(file1.uuid())).build(), new QueryContext())
      .getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().setContextualized(true).moduleRootUuids(newArrayList(subModule.uuid())).build(), new QueryContext())
      .getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().setContextualized(true).moduleRootUuids(newArrayList(module.uuid())).build(), new QueryContext())
      .getHits()).hasSize(4);
    assertThat(index.search(IssueQuery.builder().setContextualized(true).projectUuids(newArrayList(project.uuid())).build(), new QueryContext())
      .getHits()).hasSize(6);
    assertThat(index.search(IssueQuery.builder().setContextualized(true).projectUuids(newArrayList("unknown")).build(), new QueryContext())
      .getHits()).isEmpty();
  }

  @Test
  public void filter_by_components_on_non_contextualized_search() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto("project");
    ComponentDto file1 = ComponentTesting.newFileDto(project, "file1");
    ComponentDto module = ComponentTesting.newModuleDto(project).setUuid("module");
    ComponentDto file2 = ComponentTesting.newFileDto(module, "file2");
    ComponentDto subModule = ComponentTesting.newModuleDto(module).setUuid("subModule");
    ComponentDto file3 = ComponentTesting.newFileDto(subModule, "file3");

    indexIssues(
      IssueTesting.newDoc("ISSUE1", project),
      IssueTesting.newDoc("ISSUE2", file1),
      IssueTesting.newDoc("ISSUE3", module),
      IssueTesting.newDoc("ISSUE4", file2),
      IssueTesting.newDoc("ISSUE5", subModule),
      IssueTesting.newDoc("ISSUE6", file3));

    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
    assertThat(index.search(IssueQuery.builder().projectUuids(newArrayList(project.uuid())).build(), new QueryContext()).getHits()).hasSize(6);
    assertThat(index.search(IssueQuery.builder().moduleUuids(newArrayList(module.uuid())).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().moduleUuids(newArrayList(subModule.uuid())).build(), new QueryContext()).getHits()).hasSize(1); // XXX Misleading !
    assertThat(index.search(IssueQuery.builder().fileUuids(newArrayList(file1.uuid())).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().fileUuids(newArrayList(file1.uuid(), file2.uuid(), file3.uuid())).build(), new QueryContext()).getHits()).hasSize(3);
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

    Result<Issue> result = index.search(IssueQuery.builder().build(), new QueryContext().addFacets(newArrayList("fileUuids")));
    assertThat(result.getFacets()).containsOnlyKeys("fileUuids");
    assertThat(result.getFacets().get("fileUuids")).containsOnly(new FacetValue("A", 1), new FacetValue("ABCD", 1), new FacetValue("BCDE", 2), new FacetValue("CDEF", 1));
  }

  @Test
  public void filter_by_directories() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file1 = ComponentTesting.newFileDto(project).setPath("src/main/xoo/F1.xoo");
    ComponentDto file2 = ComponentTesting.newFileDto(project).setPath("F2.xoo");

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file1).setDirectoryPath("/src/main/xoo"),
      IssueTesting.newDoc("ISSUE2", file2).setDirectoryPath("/"));

    assertThat(index.search(IssueQuery.builder().directories(newArrayList("/src/main/xoo")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().directories(newArrayList("/")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().directories(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void facets_on_directories() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file1 = ComponentTesting.newFileDto(project).setPath("src/main/xoo/F1.xoo");
    ComponentDto file2 = ComponentTesting.newFileDto(project).setPath("F2.xoo");

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file1).setDirectoryPath("/src/main/xoo"),
      IssueTesting.newDoc("ISSUE2", file2).setDirectoryPath("/"));

    Result<Issue> result = index.search(IssueQuery.builder().build(), new QueryContext().addFacets(newArrayList("directories")));
    assertThat(result.getFacets()).containsOnlyKeys("directories");
    assertThat(result.getFacets().get("directories")).containsOnly(new FacetValue("/src/main/xoo", 1), new FacetValue("/", 1));
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

    assertThat(index.search(IssueQuery.builder().viewUuids(newArrayList(view1)).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().viewUuids(newArrayList(view2)).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().viewUuids(newArrayList(view1, view2)).build(), new QueryContext()).getHits()).hasSize(3);
    assertThat(index.search(IssueQuery.builder().viewUuids(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_severities() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setSeverity(Severity.INFO),
      IssueTesting.newDoc("ISSUE2", file).setSeverity(Severity.MAJOR));

    assertThat(index.search(IssueQuery.builder().severities(newArrayList(Severity.INFO, Severity.MAJOR)).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().severities(newArrayList(Severity.INFO)).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().severities(newArrayList(Severity.BLOCKER)).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void facets_on_severities() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setSeverity(Severity.INFO),
      IssueTesting.newDoc("ISSUE2", file).setSeverity(Severity.INFO),
      IssueTesting.newDoc("ISSUE3", file).setSeverity(Severity.MAJOR));

    Result<Issue> result = index.search(IssueQuery.builder().build(), new QueryContext().addFacets(newArrayList("severities")));
    assertThat(result.getFacets()).containsOnlyKeys("severities");
    assertThat(result.getFacets().get("severities")).containsOnly(new FacetValue("INFO", 2), new FacetValue("MAJOR", 1));
  }

  @Test
  public void filter_by_statuses() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setStatus(Issue.STATUS_CLOSED),
      IssueTesting.newDoc("ISSUE2", file).setStatus(Issue.STATUS_OPEN));

    assertThat(index.search(IssueQuery.builder().statuses(newArrayList(Issue.STATUS_CLOSED, Issue.STATUS_OPEN)).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().statuses(newArrayList(Issue.STATUS_CLOSED)).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().statuses(newArrayList(Issue.STATUS_CONFIRMED)).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void facets_on_statuses() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setStatus(Issue.STATUS_CLOSED),
      IssueTesting.newDoc("ISSUE2", file).setStatus(Issue.STATUS_CLOSED),
      IssueTesting.newDoc("ISSUE3", file).setStatus(Issue.STATUS_OPEN));

    Result<Issue> result = index.search(IssueQuery.builder().build(), new QueryContext().addFacets(newArrayList("statuses")));
    assertThat(result.getFacets()).containsOnlyKeys("statuses");
    assertThat(result.getFacets().get("statuses")).containsOnly(new FacetValue("CLOSED", 2), new FacetValue("OPEN", 1));
  }

  @Test
  public void filter_by_resolutions() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      IssueTesting.newDoc("ISSUE2", file).setResolution(Issue.RESOLUTION_FIXED));

    assertThat(index.search(IssueQuery.builder().resolutions(newArrayList(Issue.RESOLUTION_FALSE_POSITIVE, Issue.RESOLUTION_FIXED)).build(), new QueryContext()).getHits())
      .hasSize(2);
    assertThat(index.search(IssueQuery.builder().resolutions(newArrayList(Issue.RESOLUTION_FALSE_POSITIVE)).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().resolutions(newArrayList(Issue.RESOLUTION_REMOVED)).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void facets_on_resolutions() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      IssueTesting.newDoc("ISSUE2", file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      IssueTesting.newDoc("ISSUE3", file).setResolution(Issue.RESOLUTION_FIXED));

    Result<Issue> result = index.search(IssueQuery.builder().build(), new QueryContext().addFacets(newArrayList("resolutions")));
    assertThat(result.getFacets()).containsOnlyKeys("resolutions");
    assertThat(result.getFacets().get("resolutions")).containsOnly(new FacetValue("FALSE-POSITIVE", 2), new FacetValue("FIXED", 1));
  }

  @Test
  public void filter_by_resolved() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED),
      IssueTesting.newDoc("ISSUE2", file).setStatus(Issue.STATUS_OPEN).setResolution(null),
      IssueTesting.newDoc("ISSUE3", file).setStatus(Issue.STATUS_OPEN).setResolution(null));

    assertThat(index.search(IssueQuery.builder().resolved(true).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().resolved(false).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().resolved(null).build(), new QueryContext()).getHits()).hasSize(3);
  }

  @Test
  public void filter_by_action_plans() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setActionPlanKey("plan1"),
      IssueTesting.newDoc("ISSUE2", file).setActionPlanKey("plan2"));

    assertThat(index.search(IssueQuery.builder().actionPlans(newArrayList("plan1")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().actionPlans(newArrayList("plan1", "plan2")).build(), new
      QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().actionPlans(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void facets_on_action_plans() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setActionPlanKey("plan1"),
      IssueTesting.newDoc("ISSUE2", file).setActionPlanKey("plan2"));

    Result<Issue> result = index.search(IssueQuery.builder().build(), new QueryContext().addFacets(newArrayList("actionPlans")));
    assertThat(result.getFacets()).containsOnlyKeys("actionPlans");
    assertThat(result.getFacets().get("actionPlans")).containsOnly(new FacetValue("plan1", 1), new FacetValue("plan2", 1));
  }

  @Test
  public void filter_by_planned() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setActionPlanKey("AP-KEY"),
      IssueTesting.newDoc("ISSUE2", file).setActionPlanKey(null),
      IssueTesting.newDoc("ISSUE3", file).setActionPlanKey(null));

    assertThat(index.search(IssueQuery.builder().planned(true).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().planned(false).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().planned(null).build(), new QueryContext()).getHits()).hasSize(3);
  }

  @Test
  public void filter_by_rules() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    RuleKey ruleKey = RuleKey.of("repo", "X1");

    indexIssues(IssueTesting.newDoc("ISSUE1", file).setRuleKey(ruleKey.toString()));

    assertThat(index.search(IssueQuery.builder().rules(newArrayList(ruleKey)).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().rules(newArrayList(RuleKey.of("rule", "without issue"))).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_languages() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    RuleKey ruleKey = RuleKey.of("repo", "X1");

    indexIssues(IssueTesting.newDoc("ISSUE1", file).setRuleKey(ruleKey.toString()).setLanguage("xoo"));

    assertThat(index.search(IssueQuery.builder().languages(newArrayList("xoo")).build(), new
      QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().languages(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void facets_on_languages() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    RuleKey ruleKey = RuleKey.of("repo", "X1");

    indexIssues(IssueTesting.newDoc("ISSUE1", file).setRuleKey(ruleKey.toString()).setLanguage("xoo"));

    Result<Issue> result = index.search(IssueQuery.builder().build(), new QueryContext().addFacets(newArrayList("languages")));
    assertThat(result.getFacets()).containsOnlyKeys("languages");
    assertThat(result.getFacets().get("languages")).containsOnly(new FacetValue("xoo", 1));
  }

  @Test
  public void filter_by_assignees() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAssignee("steph"),
      IssueTesting.newDoc("ISSUE2", file).setAssignee("simon"),
      IssueTesting.newDoc("ISSUE3", file).setAssignee(null));

    assertThat(index.search(IssueQuery.builder().assignees(newArrayList("steph")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().assignees(newArrayList("steph", "simon")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().assignees(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
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

    Result<Issue> result = index.search(IssueQuery.builder().build(), new QueryContext().addFacets(newArrayList("assignees")));
    assertThat(result.getFacets()).containsOnlyKeys("assignees");
    assertThat(result.getFacets().get("assignees")).containsOnly(new FacetValue("steph", 1), new FacetValue("simon", 2), new FacetValue("", 1));
  }

  @Test
  public void filter_by_assigned() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAssignee("steph"),
      IssueTesting.newDoc("ISSUE2", file).setAssignee(null),
      IssueTesting.newDoc("ISSUE3", file).setAssignee(null));

    assertThat(index.search(IssueQuery.builder().assigned(true).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().assigned(false).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().assigned(null).build(), new QueryContext()).getHits()).hasSize(3);
  }

  @Test
  public void filter_by_reporters() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setReporter("fabrice"),
      IssueTesting.newDoc("ISSUE2", file).setReporter("stephane"));

    assertThat(index.search(IssueQuery.builder().reporters(newArrayList("fabrice", "stephane")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().reporters(newArrayList("fabrice")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().reporters(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_created_after() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setFuncCreationDate(DateUtils.parseDate("2014-09-20")),
      IssueTesting.newDoc("ISSUE2", file).setFuncCreationDate(DateUtils.parseDate("2014-09-23")));

    assertThat(index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-19")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-20")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-21")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-25")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_created_before() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setFuncCreationDate(DateUtils.parseDate("2014-09-20")),
      IssueTesting.newDoc("ISSUE2", file).setFuncCreationDate(DateUtils.parseDate("2014-09-23")));

    assertThat(index.search(IssueQuery.builder().createdBefore(DateUtils.parseDate("2014-09-19")).build(), new QueryContext()).getHits()).isEmpty();
    assertThat(index.search(IssueQuery.builder().createdBefore(DateUtils.parseDate("2014-09-20")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().createdBefore(DateUtils.parseDate("2014-09-21")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().createdBefore(DateUtils.parseDate("2014-09-25")).build(), new QueryContext()).getHits()).hasSize(2);
  }

  @Test
  public void filter_by_created_at() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(IssueTesting.newDoc("ISSUE1", file).setFuncCreationDate(DateUtils.parseDate("2014-09-20")));

    assertThat(index.search(IssueQuery.builder().createdAt(DateUtils.parseDate("2014-09-20")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().createdAt(DateUtils.parseDate("2014-09-21")).build(), new QueryContext()).getHits()).isEmpty();
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
    Result<Issue> result = index.search(query.build(), new QueryContext().setPage(2, 10));
    assertThat(result.getHits()).hasSize(2);
    assertThat(result.getTotal()).isEqualTo(12);

    result = index.search(IssueQuery.builder().build(), new QueryContext().setOffset(0).setLimit(5));
    assertThat(result.getHits()).hasSize(5);
    assertThat(result.getTotal()).isEqualTo(12);

    result = index.search(IssueQuery.builder().build(), new QueryContext().setOffset(2).setLimit(0));
    assertThat(result.getHits()).hasSize(0);
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
    Result<Issue> result = index.search(query.build(), new QueryContext().setMaxLimit());
    assertThat(result.getHits()).hasSize(500);
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
    Result<Issue> result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits().get(0).status()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(result.getHits().get(1).status()).isEqualTo(Issue.STATUS_OPEN);
    assertThat(result.getHits().get(2).status()).isEqualTo(Issue.STATUS_REOPENED);

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_STATUS).asc(false);
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits().get(0).status()).isEqualTo(Issue.STATUS_REOPENED);
    assertThat(result.getHits().get(1).status()).isEqualTo(Issue.STATUS_OPEN);
    assertThat(result.getHits().get(2).status()).isEqualTo(Issue.STATUS_CLOSED);
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
    Result<Issue> result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits().get(0).severity()).isEqualTo(Severity.INFO);
    assertThat(result.getHits().get(1).severity()).isEqualTo(Severity.MINOR);
    assertThat(result.getHits().get(2).severity()).isEqualTo(Severity.MAJOR);
    assertThat(result.getHits().get(3).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(result.getHits().get(4).severity()).isEqualTo(Severity.BLOCKER);

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_SEVERITY).asc(false);
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits().get(0).severity()).isEqualTo(Severity.BLOCKER);
    assertThat(result.getHits().get(1).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(result.getHits().get(2).severity()).isEqualTo(Severity.MAJOR);
    assertThat(result.getHits().get(3).severity()).isEqualTo(Severity.MINOR);
    assertThat(result.getHits().get(4).severity()).isEqualTo(Severity.INFO);
  }

  @Test
  public void sort_by_assignee() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setAssignee("steph"),
      IssueTesting.newDoc("ISSUE2", file).setAssignee("simon"));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_ASSIGNEE).asc(true);
    Result<Issue> result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(2);
    assertThat(result.getHits().get(0).assignee()).isEqualTo("simon");
    assertThat(result.getHits().get(1).assignee()).isEqualTo("steph");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_ASSIGNEE).asc(false);
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(2);
    assertThat(result.getHits().get(0).assignee()).isEqualTo("steph");
    assertThat(result.getHits().get(1).assignee()).isEqualTo("simon");
  }

  @Test
  public void sort_by_creation_date() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setFuncCreationDate(DateUtils.parseDate("2014-09-23")),
      IssueTesting.newDoc("ISSUE2", file).setFuncCreationDate(DateUtils.parseDate("2014-09-24")));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CREATION_DATE).asc(true);
    Result<Issue> result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(2);
    assertThat(result.getHits().get(0).creationDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
    assertThat(result.getHits().get(1).creationDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CREATION_DATE).asc(false);
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(2);
    assertThat(result.getHits().get(0).creationDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));
    assertThat(result.getHits().get(1).creationDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
  }

  @Test
  public void sort_by_update_date() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);

    indexIssues(
      IssueTesting.newDoc("ISSUE1", file).setFuncUpdateDate(DateUtils.parseDate("2014-09-23")),
      IssueTesting.newDoc("ISSUE2", file).setFuncUpdateDate(DateUtils.parseDate("2014-09-24")));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_UPDATE_DATE).asc(true);
    Result<Issue> result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(2);
    assertThat(result.getHits().get(0).updateDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
    assertThat(result.getHits().get(1).updateDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_UPDATE_DATE).asc(false);
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(2);
    assertThat(result.getHits().get(0).updateDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));
    assertThat(result.getHits().get(1).updateDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
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
    Result<Issue> result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(3);
    assertThat(result.getHits().get(0).closeDate()).isNull();
    assertThat(result.getHits().get(1).closeDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
    assertThat(result.getHits().get(2).closeDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CLOSE_DATE).asc(false);
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(3);
    assertThat(result.getHits().get(0).closeDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));
    assertThat(result.getHits().get(1).closeDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
    assertThat(result.getHits().get(2).closeDate()).isNull();
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
    Result<Issue> result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(6);
    assertThat(result.getHits().get(0).key()).isEqualTo("F1_1");
    assertThat(result.getHits().get(1).key()).isEqualTo("F1_2");
    assertThat(result.getHits().get(2).key()).isEqualTo("F1_3");
    assertThat(result.getHits().get(3).key()).isEqualTo("F2_1");
    assertThat(result.getHits().get(4).key()).isEqualTo("F2_2");
    assertThat(result.getHits().get(5).key()).isEqualTo("F2_3");

    // descending sort -> F2 then F1
    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_FILE_LINE).asc(false);
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(6);
    assertThat(result.getHits().get(0).key()).isEqualTo("F2_3");
    assertThat(result.getHits().get(1).key()).isEqualTo("F2_2");
    assertThat(result.getHits().get(2).key()).isEqualTo("F2_1");
    assertThat(result.getHits().get(3).key()).isEqualTo("F1_3");
    assertThat(result.getHits().get(4).key()).isEqualTo("F1_2");
    assertThat(result.getHits().get(5).key()).isEqualTo("F1_1");
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

    IssueQuery.Builder query = IssueQuery.builder();

    MockUserSession.set().setUserGroups("sonar-users");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);

    MockUserSession.set().setUserGroups("sonar-admins");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);

    MockUserSession.set().setUserGroups("sonar-users", "sonar-admins");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(2);

    MockUserSession.set().setUserGroups("another group");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).isEmpty();

    MockUserSession.set().setUserGroups("sonar-users", "sonar-admins");
    assertThat(index.search(query.projectUuids(newArrayList(project3.uuid())).build(), new QueryContext()).getHits()).isEmpty();
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

    IssueQuery.Builder query = IssueQuery.builder();

    MockUserSession.set().setLogin("john");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);

    MockUserSession.set().setLogin("max");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);

    MockUserSession.set().setLogin("another guy");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(0);

    MockUserSession.set().setLogin("john");
    assertThat(index.search(query.projectUuids(newArrayList(project3.key())).build(), new QueryContext()).getHits()).hasSize(0);
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
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);
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

    List<FacetValue> results = index.listAssignees(IssueQuery.builder().statuses(newArrayList(Issue.STATUS_OPEN)).build());

    assertThat(results).hasSize(3);
    assertThat(results.get(0).getKey()).isEqualTo("steph");
    assertThat(results.get(0).getValue()).isEqualTo(2);

    assertThat(results.get(1).getKey()).isEqualTo("simon");
    assertThat(results.get(1).getValue()).isEqualTo(1);

    assertThat(results.get(2).getKey()).isEqualTo("_notAssigned_");
    assertThat(results.get(2).getValue()).isEqualTo(1);
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
    List<Issue> issues = index.search(IssueQuery.builder().projectUuids(newArrayList(project.uuid())).build(), new QueryContext()).getHits();
    List<Date> dates = newArrayList();
    for (Issue issue : issues) {
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
    EsClient client = tester.get(EsClient.class);
    BulkRequestBuilder bulk = client.prepareBulk().setRefresh(true);
    bulk.add(new IndexRequest(ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW).source(new ViewDoc().setUuid(viewUuid).setProjects(projects).getFields()));
    bulk.get();
  }
}
