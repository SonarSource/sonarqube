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
package org.sonar.server.issue.index;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.assertj.core.api.Fail;
import org.assertj.core.groups.Tuple;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.permission.index.PermissionIndexerDao;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.view.index.ViewDoc;
import org.sonar.server.view.index.ViewIndexDefinition;
import org.sonar.server.view.index.ViewIndexer;

import static com.google.common.collect.ImmutableSortedSet.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.api.utils.DateUtils.addDays;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.issue.IssueDocTesting.newDoc;

public class IssueIndexTest {

  private MapSettings settings = new MapSettings();

  @Rule
  public EsTester es = new EsTester(
    new IssueIndexDefinition(settings.asConfig()),
    new ViewIndexDefinition(settings.asConfig()),
    new RuleIndexDefinition(settings.asConfig()));
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private System2 system2 = new TestSystem2().setNow(1_500_000_000_000L).setDefaultTimeZone(TimeZone.getTimeZone("GMT-01:00"));
  @Rule
  public DbTester db = DbTester.create(system2);
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()));
  private ViewIndexer viewIndexer = new ViewIndexer(db.getDbClient(), es.client());
  private RuleIndexer ruleIndexer = new RuleIndexer(es.client(), db.getDbClient());
  private PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, issueIndexer);

  private IssueIndex underTest = new IssueIndex(es.client(), system2, userSessionRule, new AuthorizationTypeSupport(userSessionRule));

  @Test
  public void filter_by_keys() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());

    indexIssues(
      newDoc("I1", newFileDto(project, null)),
      newDoc("I2", newFileDto(project, null)));

    assertThatSearchReturnsOnly(IssueQuery.builder().issueKeys(asList("I1", "I2")), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().issueKeys(singletonList("I1")), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().issueKeys(asList("I3", "I4")));
  }

  @Test
  public void filter_by_projects() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto module = newModuleDto(project);
    ComponentDto subModule = newModuleDto(module);

    indexIssues(
      newDoc("I1", project),
      newDoc("I2", newFileDto(project, null)),
      newDoc("I3", module),
      newDoc("I4", newFileDto(module, null)),
      newDoc("I5", subModule),
      newDoc("I6", newFileDto(subModule, null)));

    assertThatSearchReturnsOnly(IssueQuery.builder().projectUuids(singletonList(project.uuid())), "I1", "I2", "I3", "I4", "I5", "I6");
    assertThatSearchReturnsEmpty(IssueQuery.builder().projectUuids(singletonList("unknown")));
  }

  @Test
  public void facet_on_projectUuids() {
    OrganizationDto organizationDto = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(organizationDto, "ABCD");
    ComponentDto project2 = newPrivateProjectDto(organizationDto, "EFGH");

    indexIssues(
      newDoc("I1", newFileDto(project, null)),
      newDoc("I2", newFileDto(project, null)),
      newDoc("I3", newFileDto(project2, null)));

    assertThatFacetHasExactly(IssueQuery.builder(), "projectUuids", entry("ABCD", 2L), entry("EFGH", 1L));
  }

  @Test
  public void filter_by_modules() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto module = newModuleDto(project);
    ComponentDto subModule = newModuleDto(module);
    ComponentDto file = newFileDto(subModule, null);

    indexIssues(
      newDoc("I3", module),
      newDoc("I5", subModule),
      newDoc("I2", file));

    assertThatSearchReturnsEmpty(IssueQuery.builder().projectUuids(singletonList(project.uuid())).moduleUuids(singletonList(file.uuid())));
    assertThatSearchReturnsOnly(IssueQuery.builder().projectUuids(singletonList(project.uuid())).moduleUuids(singletonList(module.uuid())), "I3");
    assertThatSearchReturnsOnly(IssueQuery.builder().projectUuids(singletonList(project.uuid())).moduleUuids(singletonList(subModule.uuid())), "I2", "I5");
    assertThatSearchReturnsEmpty(IssueQuery.builder().projectUuids(singletonList(project.uuid())).moduleUuids(singletonList(project.uuid())));
    assertThatSearchReturnsEmpty(IssueQuery.builder().projectUuids(singletonList(project.uuid())).moduleUuids(singletonList("unknown")));
  }

  @Test
  public void filter_by_components_on_contextualized_search() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto module = newModuleDto(project);
    ComponentDto subModule = newModuleDto(module);
    ComponentDto file1 = newFileDto(project, null);
    ComponentDto file2 = newFileDto(module, null);
    ComponentDto file3 = newFileDto(subModule, null);
    String view = "ABCD";
    indexView(view, asList(project.uuid()));

    indexIssues(
      newDoc("I1", project),
      newDoc("I2", file1),
      newDoc("I3", module),
      newDoc("I4", file2),
      newDoc("I5", subModule),
      newDoc("I6", file3));

    assertThatSearchReturnsOnly(IssueQuery.builder().fileUuids(asList(file1.uuid(), file2.uuid(), file3.uuid())), "I2", "I4", "I6");
    assertThatSearchReturnsOnly(IssueQuery.builder().fileUuids(singletonList(file1.uuid())), "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().moduleRootUuids(singletonList(subModule.uuid())), "I5", "I6");
    assertThatSearchReturnsOnly(IssueQuery.builder().moduleRootUuids(singletonList(module.uuid())), "I3", "I4", "I5", "I6");
    assertThatSearchReturnsOnly(IssueQuery.builder().projectUuids(singletonList(project.uuid())), "I1", "I2", "I3", "I4", "I5", "I6");
    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(view)), "I1", "I2", "I3", "I4", "I5", "I6");
    assertThatSearchReturnsEmpty(IssueQuery.builder().projectUuids(singletonList("unknown")));
  }

  @Test
  public void filter_by_components_on_non_contextualized_search() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto(), "project");
    ComponentDto file1 = newFileDto(project, null, "file1");
    ComponentDto module = newModuleDto(project).setUuid("module");
    ComponentDto file2 = newFileDto(module, null, "file2");
    ComponentDto subModule = newModuleDto(module).setUuid("subModule");
    ComponentDto file3 = newFileDto(subModule, null, "file3");
    String view = "ABCD";
    indexView(view, asList(project.uuid()));

    indexIssues(
      newDoc("I1", project),
      newDoc("I2", file1),
      newDoc("I3", module),
      newDoc("I4", file2),
      newDoc("I5", subModule),
      newDoc("I6", file3));

    assertThatSearchReturnsEmpty(IssueQuery.builder().projectUuids(singletonList("unknown")));
    assertThatSearchReturnsOnly(IssueQuery.builder().projectUuids(singletonList(project.uuid())), "I1", "I2", "I3", "I4", "I5", "I6");
    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(view)), "I1", "I2", "I3", "I4", "I5", "I6");
    assertThatSearchReturnsOnly(IssueQuery.builder().moduleUuids(singletonList(module.uuid())), "I3", "I4");
    assertThatSearchReturnsOnly(IssueQuery.builder().moduleUuids(singletonList(subModule.uuid())), "I5", "I6");
    assertThatSearchReturnsOnly(IssueQuery.builder().fileUuids(singletonList(file1.uuid())), "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().fileUuids(asList(file1.uuid(), file2.uuid(), file3.uuid())), "I2", "I4", "I6");
  }

  @Test
  public void facets_on_components() {
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
  public void filter_by_directories() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file1 = newFileDto(project, null).setPath("src/main/xoo/F1.xoo");
    ComponentDto file2 = newFileDto(project, null).setPath("F2.xoo");

    indexIssues(
      newDoc("I1", file1).setDirectoryPath("/src/main/xoo"),
      newDoc("I2", file2).setDirectoryPath("/"));

    assertThatSearchReturnsOnly(IssueQuery.builder().directories(singletonList("/src/main/xoo")), "I1");
    assertThatSearchReturnsOnly(IssueQuery.builder().directories(singletonList("/")), "I2");
    assertThatSearchReturnsEmpty(IssueQuery.builder().directories(singletonList("unknown")));
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
  public void filter_by_views() {
    OrganizationDto organizationDto = newOrganizationDto();
    ComponentDto project1 = newPrivateProjectDto(organizationDto);
    ComponentDto file1 = newFileDto(project1, null);
    ComponentDto project2 = newPrivateProjectDto(organizationDto);
    indexIssues(
      // Project1 has 2 issues (one on a file and one on the project itself)
      newDoc("I1", project1),
      newDoc("I2", file1),
      // Project2 has 1 issue
      newDoc("I3", project2));

    // The view1 is containing 2 issues from project1
    String view1 = "ABCD";
    indexView(view1, asList(project1.uuid()));

    // The view2 is containing 1 issue from project2
    String view2 = "CDEF";
    indexView(view2, asList(project2.uuid()));

    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(view1)), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(view2)), "I3");
    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(asList(view1, view2)), "I1", "I2", "I3");
    assertThatSearchReturnsEmpty(IssueQuery.builder().viewUuids(singletonList("unknown")));
  }

  @Test
  public void filter_by_views_not_having_projects() {
    OrganizationDto organizationDto = newOrganizationDto();
    ComponentDto project1 = newPrivateProjectDto(organizationDto);
    ComponentDto file1 = newFileDto(project1, null);
    indexIssues(newDoc("I2", file1));
    String view1 = "ABCD";
    indexView(view1, emptyList());

    assertThatSearchReturnsOnly(IssueQuery.builder().viewUuids(singletonList(view1)));
  }

  @Test
  public void filter_by_created_after_by_projects() {
    Date now = new Date();
    OrganizationDto organizationDto = newOrganizationDto();
    ComponentDto project1 = newPrivateProjectDto(organizationDto);
    IssueDoc project1Issue1 = newDoc(project1).setFuncCreationDate(addDays(now, -10));
    IssueDoc project1Issue2 = newDoc(project1).setFuncCreationDate(addDays(now, -20));
    ComponentDto project2 = newPrivateProjectDto(organizationDto);
    IssueDoc project2Issue1 = newDoc(project2).setFuncCreationDate(addDays(now, -15));
    IssueDoc project2Issue2 = newDoc(project2).setFuncCreationDate(addDays(now, -30));
    indexIssues(project1Issue1, project1Issue2, project2Issue1, project2Issue2);

    // Search for issues of project 1 having less than 15 days
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfterByProjectUuids(ImmutableMap.of(project1.uuid(), new IssueQuery.PeriodStart(addDays(now, -15), true))),
      project1Issue1.key());

    // Search for issues of project 1 having less than 14 days and project 2 having less then 25 days
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfterByProjectUuids(ImmutableMap.of(
        project1.uuid(), new IssueQuery.PeriodStart(addDays(now, -14), true),
        project2.uuid(), new IssueQuery.PeriodStart(addDays(now, -25), true))),
      project1Issue1.key(), project2Issue1.key());

    // Search for issues of project 1 having less than 30 days
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfterByProjectUuids(ImmutableMap.of(
        project1.uuid(), new IssueQuery.PeriodStart(addDays(now, -30), true))),
      project1Issue1.key(), project1Issue2.key());

    // Search for issues of project 1 and project 2 having less than 5 days
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfterByProjectUuids(ImmutableMap.of(
        project1.uuid(), new IssueQuery.PeriodStart(addDays(now, -5), true),
        project2.uuid(), new IssueQuery.PeriodStart(addDays(now, -5), true))));
  }

  @Test
  public void filter_one_issue_by_project_and_branch() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto anotherbBranch = db.components().insertProjectBranch(project);

    IssueDoc issueOnProject = newDoc(project);
    IssueDoc issueOnBranch = newDoc(branch);
    IssueDoc issueOnAnotherBranch = newDoc(anotherbBranch);
    indexIssues(issueOnProject, issueOnBranch, issueOnAnotherBranch);

    assertThatSearchReturnsOnly(IssueQuery.builder().branchUuid(branch.uuid()).mainBranch(false), issueOnBranch.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().componentUuids(singletonList(branch.uuid())).branchUuid(branch.uuid()).mainBranch(false), issueOnBranch.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().projectUuids(singletonList(project.uuid())).branchUuid(branch.uuid()).mainBranch(false), issueOnBranch.key());
    assertThatSearchReturnsOnly(
      IssueQuery.builder().componentUuids(singletonList(branch.uuid())).projectUuids(singletonList(project.uuid())).branchUuid(branch.uuid()).mainBranch(false),
      issueOnBranch.key());
    assertThatSearchReturnsEmpty(IssueQuery.builder().branchUuid("unknown"));
  }

  @Test
  public void issues_from_branch_component_children() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto projectModule = db.components().insertComponent(newModuleDto(project));
    ComponentDto projectFile = db.components().insertComponent(newFileDto(projectModule));
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));
    ComponentDto branchModule = db.components().insertComponent(newModuleDto(branch));
    ComponentDto branchFile = db.components().insertComponent(newFileDto(branchModule));

    indexIssues(
      newDoc("I1", project),
      newDoc("I2", projectFile),
      newDoc("I3", projectModule),
      newDoc("I4", branch),
      newDoc("I5", branchModule),
      newDoc("I6", branchFile));

    assertThatSearchReturnsOnly(IssueQuery.builder().branchUuid(branch.uuid()).mainBranch(false), "I4", "I5", "I6");
    assertThatSearchReturnsOnly(IssueQuery.builder().moduleUuids(singletonList(branchModule.uuid())).branchUuid(branch.uuid()).mainBranch(false), "I5", "I6");
    assertThatSearchReturnsOnly(IssueQuery.builder().fileUuids(singletonList(branchFile.uuid())).branchUuid(branch.uuid()).mainBranch(false), "I6");
    assertThatSearchReturnsEmpty(IssueQuery.builder().fileUuids(singletonList(branchFile.uuid())).mainBranch(false).branchUuid("unknown"));
  }

  @Test
  public void issues_from_main_branch() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project);

    IssueDoc issueOnProject = newDoc(project);
    IssueDoc issueOnBranch = newDoc(branch);
    indexIssues(issueOnProject, issueOnBranch);

    assertThatSearchReturnsOnly(IssueQuery.builder().branchUuid(project.uuid()).mainBranch(true), issueOnProject.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().componentUuids(singletonList(project.uuid())).branchUuid(project.uuid()).mainBranch(true), issueOnProject.key());
    assertThatSearchReturnsOnly(IssueQuery.builder().projectUuids(singletonList(project.uuid())).branchUuid(project.uuid()).mainBranch(true), issueOnProject.key());
    assertThatSearchReturnsOnly(
      IssueQuery.builder().componentUuids(singletonList(project.uuid())).projectUuids(singletonList(project.uuid())).branchUuid(project.uuid()).mainBranch(true),
      issueOnProject.key());
  }

  @Test
  public void branch_issues_are_ignored_when_no_branch_param() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));

    IssueDoc projectIssue = newDoc(project);
    IssueDoc branchIssue = newDoc(branch);
    indexIssues(projectIssue, branchIssue);

    assertThatSearchReturnsOnly(IssueQuery.builder(), projectIssue.key());
  }

  @Test
  public void filter_by_severities() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setSeverity(Severity.INFO),
      newDoc("I2", file).setSeverity(Severity.MAJOR));

    assertThatSearchReturnsOnly(IssueQuery.builder().severities(asList(Severity.INFO, Severity.MAJOR)), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().severities(singletonList(Severity.INFO)), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().severities(singletonList(Severity.BLOCKER)));
  }

  @Test
  public void facets_on_severities() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setSeverity(Severity.INFO),
      newDoc("I2", file).setSeverity(Severity.INFO),
      newDoc("I3", file).setSeverity(Severity.MAJOR));

    assertThatFacetHasOnly(IssueQuery.builder(), "severities", entry("INFO", 2L), entry("MAJOR", 1L));
  }

  @Test
  public void filter_by_statuses() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setStatus(Issue.STATUS_CLOSED),
      newDoc("I2", file).setStatus(Issue.STATUS_OPEN));

    assertThatSearchReturnsOnly(IssueQuery.builder().statuses(asList(Issue.STATUS_CLOSED, Issue.STATUS_OPEN)), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().statuses(singletonList(Issue.STATUS_CLOSED)), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().statuses(singletonList(Issue.STATUS_CONFIRMED)));
  }

  @Test
  public void facets_on_statuses() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setStatus(Issue.STATUS_CLOSED),
      newDoc("I2", file).setStatus(Issue.STATUS_CLOSED),
      newDoc("I3", file).setStatus(Issue.STATUS_OPEN));

    assertThatFacetHasOnly(IssueQuery.builder(), "statuses", entry("CLOSED", 2L), entry("OPEN", 1L));
  }

  @Test
  public void filter_by_resolutions() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      newDoc("I2", file).setResolution(Issue.RESOLUTION_FIXED));

    assertThatSearchReturnsOnly(IssueQuery.builder().resolutions(asList(Issue.RESOLUTION_FALSE_POSITIVE, Issue.RESOLUTION_FIXED)), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().resolutions(singletonList(Issue.RESOLUTION_FALSE_POSITIVE)), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().resolutions(singletonList(Issue.RESOLUTION_REMOVED)));
  }

  @Test
  public void facets_on_resolutions() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      newDoc("I2", file).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      newDoc("I3", file).setResolution(Issue.RESOLUTION_FIXED));

    assertThatFacetHasOnly(IssueQuery.builder(), "resolutions", entry("FALSE-POSITIVE", 2L), entry("FIXED", 1L));
  }

  @Test
  public void filter_by_resolved() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED),
      newDoc("I2", file).setStatus(Issue.STATUS_OPEN).setResolution(null),
      newDoc("I3", file).setStatus(Issue.STATUS_OPEN).setResolution(null));

    assertThatSearchReturnsOnly(IssueQuery.builder().resolved(true), "I1");
    assertThatSearchReturnsOnly(IssueQuery.builder().resolved(false), "I2", "I3");
    assertThatSearchReturnsOnly(IssueQuery.builder().resolved(null), "I1", "I2", "I3");
  }

  @Test
  public void filter_by_rules() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);
    RuleDefinitionDto ruleDefinitionDto = newRule();
    db.rules().insert(ruleDefinitionDto);

    indexIssues(newDoc("I1", file).setRuleId(ruleDefinitionDto.getId()));

    assertThatSearchReturnsOnly(IssueQuery.builder().rules(singletonList(ruleDefinitionDto)), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().rules(singletonList(new RuleDefinitionDto().setId(-1))));
  }

  @Test
  public void filter_by_languages() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);
    RuleDefinitionDto ruleDefinitionDto = newRule();
    db.rules().insert(ruleDefinitionDto);

    indexIssues(newDoc("I1", file).setRuleId(ruleDefinitionDto.getId()).setLanguage("xoo"));

    assertThatSearchReturnsOnly(IssueQuery.builder().languages(singletonList("xoo")), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().languages(singletonList("unknown")));
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
  public void filter_by_assignees() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setAssignee("steph"),
      newDoc("I2", file).setAssignee("marcel"),
      newDoc("I3", file).setAssignee(null));

    assertThatSearchReturnsOnly(IssueQuery.builder().assignees(singletonList("steph")), "I1");
    assertThatSearchReturnsOnly(IssueQuery.builder().assignees(asList("steph", "marcel")), "I1", "I2");
    assertThatSearchReturnsEmpty(IssueQuery.builder().assignees(singletonList("unknown")));
  }

  @Test
  public void facets_on_assignees() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setAssignee("steph"),
      newDoc("I2", file).setAssignee("marcel"),
      newDoc("I3", file).setAssignee("marcel"),
      newDoc("I4", file).setAssignee(null));

    assertThatFacetHasOnly(IssueQuery.builder(), "assignees", entry("steph", 1L), entry("marcel", 2L), entry("", 1L));
  }

  @Test
  public void facets_on_assignees_supports_dashes() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setAssignee("j-b"),
      newDoc("I2", file).setAssignee("marcel"),
      newDoc("I3", file).setAssignee("marcel"),
      newDoc("I4", file).setAssignee(null));

    assertThatFacetHasOnly(IssueQuery.builder().assignees(singletonList("j-b")), "assignees", entry("j-b", 1L), entry("marcel", 2L), entry("", 1L));
  }

  @Test
  public void filter_by_assigned() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setAssignee("steph"),
      newDoc("I2", file).setAssignee(null),
      newDoc("I3", file).setAssignee(null));

    assertThatSearchReturnsOnly(IssueQuery.builder().assigned(true), "I1");
    assertThatSearchReturnsOnly(IssueQuery.builder().assigned(false), "I2", "I3");
    assertThatSearchReturnsOnly(IssueQuery.builder().assigned(null), "I1", "I2", "I3");
  }

  @Test
  public void filter_by_authors() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setAuthorLogin("steph"),
      newDoc("I2", file).setAuthorLogin("marcel"),
      newDoc("I3", file).setAssignee(null));

    assertThatSearchReturnsOnly(IssueQuery.builder().authors(singletonList("steph")), "I1");
    assertThatSearchReturnsOnly(IssueQuery.builder().authors(asList("steph", "marcel")), "I1", "I2");
    assertThatSearchReturnsEmpty(IssueQuery.builder().authors(singletonList("unknown")));
  }

  @Test
  public void facets_on_authors() {
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
  public void filter_by_created_after() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setFuncCreationDate(parseDate("2014-09-20")),
      newDoc("I2", file).setFuncCreationDate(parseDate("2014-09-23")));

    assertThatSearchReturnsOnly(IssueQuery.builder().createdAfter(parseDate("2014-09-19")), "I1", "I2");
    // Lower bound is included
    assertThatSearchReturnsOnly(IssueQuery.builder().createdAfter(parseDate("2014-09-20")), "I1", "I2");
    assertThatSearchReturnsOnly(IssueQuery.builder().createdAfter(parseDate("2014-09-21")), "I2");
    assertThatSearchReturnsEmpty(IssueQuery.builder().createdAfter(parseDate("2014-09-25")));
  }

  @Test
  public void filter_by_created_before() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setFuncCreationDate(parseDate("2014-09-20")),
      newDoc("I2", file).setFuncCreationDate(parseDate("2014-09-23")));

    assertThatSearchReturnsEmpty(IssueQuery.builder().createdBefore(parseDate("2014-09-19")));
    // Upper bound is excluded
    assertThatSearchReturnsEmpty(IssueQuery.builder().createdBefore(parseDate("2014-09-20")));
    assertThatSearchReturnsOnly(IssueQuery.builder().createdBefore(parseDate("2014-09-21")), "I1");
    assertThatSearchReturnsOnly(IssueQuery.builder().createdBefore(parseDate("2014-09-25")), "I1", "I2");
  }

  @Test
  public void filter_by_created_after_and_before() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setFuncCreationDate(parseDate("2014-09-20")),
      newDoc("I2", file).setFuncCreationDate(parseDate("2014-09-23")));

    // 19 < createdAt < 25
    assertThatSearchReturnsOnly(IssueQuery.builder().createdAfter(parseDate("2014-09-19")).createdBefore(parseDate("2014-09-25")),
      "I1", "I2");

    // 20 < createdAt < 25: excludes first issue
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-20")).createdBefore(parseDate("2014-09-25")), "I1", "I2");

    // 21 < createdAt < 25
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-21")).createdBefore(parseDate("2014-09-25")), "I2");

    // 21 < createdAt < 24
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-21")).createdBefore(parseDate("2014-09-24")), "I2");

    // 21 < createdAt < 23: excludes second issue
    assertThatSearchReturnsEmpty(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-21")).createdBefore(parseDate("2014-09-23")));

    // 19 < createdAt < 21: only first issue
    assertThatSearchReturnsOnly(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-19")).createdBefore(parseDate("2014-09-21")), "I1");

    // 20 < createdAt < 20: exception
    expectedException.expect(IllegalArgumentException.class);
    underTest.search(IssueQuery.builder()
      .createdAfter(parseDate("2014-09-20")).createdBefore(parseDate("2014-09-20"))
      .build(), new SearchOptions());
  }

  @Test
  public void filter_by_create_after_and_before_take_into_account_timezone() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setFuncCreationDate(parseDateTime("2014-09-20T00:00:00+0100")),
      newDoc("I2", file).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")));

    assertThatSearchReturnsOnly(IssueQuery.builder().createdAfter(parseDateTime("2014-09-19T23:00:00+0000")).createdBefore(parseDateTime("2014-09-22T23:00:01+0000")),
      "I1", "I2");

    assertThatSearchReturnsEmpty(IssueQuery.builder().createdAfter(parseDateTime("2014-09-19T23:00:01+0000")).createdBefore(parseDateTime("2014-09-22T23:00:00+0000")));
  }

  @Test
  public void filter_by_created_before_must_be_lower_than_after() {
    try {
      underTest.search(IssueQuery.builder().createdAfter(parseDate("2014-09-20")).createdBefore(parseDate("2014-09-19")).build(),
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

    underTest.search(IssueQuery.builder().createdAfter(parseDate("2014-09-20")).createdBefore(parseDate("2014-09-20")).build(), new SearchOptions());
  }

  @Test
  public void filter_by_created_after_must_not_be_in_future() {
    try {
      underTest.search(IssueQuery.builder().createdAfter(new Date(Long.MAX_VALUE)).build(), new SearchOptions());
      Fail.failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException exception) {
      assertThat(exception.getMessage()).isEqualTo("Start bound cannot be in the future");
    }
  }

  @Test
  public void filter_by_created_at() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(newDoc("I1", file).setFuncCreationDate(parseDate("2014-09-20")));

    assertThatSearchReturnsOnly(IssueQuery.builder().createdAt(parseDate("2014-09-20")), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().createdAt(parseDate("2014-09-21")));
  }

  @Test
  public void facet_on_created_at_with_less_than_20_days() {
    SearchOptions options = fixtureForCreatedAtFacet();

    IssueQuery query = IssueQuery.builder()
      .createdAfter(parseDateTime("2014-09-01T00:00:00+0100"))
      .createdBefore(parseDateTime("2014-09-08T00:00:00+0100"))
      .checkAuthorization(false)
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

  @Test
  public void paging() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);
    for (int i = 0; i < 12; i++) {
      indexIssues(newDoc("I" + i, file));
    }

    IssueQuery.Builder query = IssueQuery.builder();
    // There are 12 issues in total, with 10 issues per page, the page 2 should only contain 2 elements
    SearchResponse result = underTest.search(query.build(), new SearchOptions().setPage(2, 10));
    assertThat(result.getHits().hits()).hasSize(2);
    assertThat(result.getHits().getTotalHits()).isEqualTo(12);

    result = underTest.search(IssueQuery.builder().build(), new SearchOptions().setOffset(0).setLimit(5));
    assertThat(result.getHits().hits()).hasSize(5);
    assertThat(result.getHits().getTotalHits()).isEqualTo(12);

    result = underTest.search(IssueQuery.builder().build(), new SearchOptions().setOffset(2).setLimit(0));
    assertThat(result.getHits().hits()).hasSize(10);
    assertThat(result.getHits().getTotalHits()).isEqualTo(12);
  }

  @Test
  public void search_with_max_limit() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);
    List<IssueDoc> issues = new ArrayList<>();
    for (int i = 0; i < 500; i++) {
      String key = "I" + i;
      issues.add(newDoc(key, file));
    }
    indexIssues(issues.toArray(new IssueDoc[] {}));

    IssueQuery.Builder query = IssueQuery.builder();
    SearchResponse result = underTest.search(query.build(), new SearchOptions().setLimit(Integer.MAX_VALUE));
    assertThat(result.getHits().hits()).hasSize(SearchOptions.MAX_LIMIT);
  }

  @Test
  public void sort_by_status() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setStatus(Issue.STATUS_OPEN),
      newDoc("I2", file).setStatus(Issue.STATUS_CLOSED),
      newDoc("I3", file).setStatus(Issue.STATUS_REOPENED));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_STATUS).asc(true);
    assertThatSearchReturnsOnly(query, "I2", "I1", "I3");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_STATUS).asc(false);
    assertThatSearchReturnsOnly(query, "I3", "I1", "I2");
  }

  @Test
  public void sort_by_severity() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setSeverity(Severity.BLOCKER),
      newDoc("I2", file).setSeverity(Severity.INFO),
      newDoc("I3", file).setSeverity(Severity.MINOR),
      newDoc("I4", file).setSeverity(Severity.CRITICAL),
      newDoc("I5", file).setSeverity(Severity.MAJOR));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_SEVERITY).asc(true);
    assertThatSearchReturnsOnly(query, "I2", "I3", "I5", "I4", "I1");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_SEVERITY).asc(false);
    assertThatSearchReturnsOnly(query, "I1", "I4", "I5", "I3", "I2");
  }

  @Test
  public void sort_by_assignee() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setAssignee("steph"),
      newDoc("I2", file).setAssignee("marcel"));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_ASSIGNEE).asc(true);
    assertThatSearchReturnsOnly(query, "I2", "I1");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_ASSIGNEE).asc(false);
    assertThatSearchReturnsOnly(query, "I1", "I2");
  }

  @Test
  public void sort_by_creation_date() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("I2", file).setFuncCreationDate(parseDateTime("2014-09-24T00:00:00+0100")));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CREATION_DATE).asc(true);
    SearchResponse result = underTest.search(query.build(), new SearchOptions());
    assertThatSearchReturnsOnly(query, "I1", "I2");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CREATION_DATE).asc(false);
    assertThatSearchReturnsOnly(query, "I2", "I1");
  }

  @Test
  public void sort_by_update_date() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setFuncUpdateDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("I2", file).setFuncUpdateDate(parseDateTime("2014-09-24T00:00:00+0100")));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_UPDATE_DATE).asc(true);
    SearchResponse result = underTest.search(query.build(), new SearchOptions());
    assertThatSearchReturnsOnly(query, "I1", "I2");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_UPDATE_DATE).asc(false);
    assertThatSearchReturnsOnly(query, "I2", "I1");
  }

  @Test
  public void sort_by_close_date() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);

    indexIssues(
      newDoc("I1", file).setFuncCloseDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("I2", file).setFuncCloseDate(parseDateTime("2014-09-24T00:00:00+0100")),
      newDoc("I3", file).setFuncCloseDate(null));

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CLOSE_DATE).asc(true);
    SearchResponse result = underTest.search(query.build(), new SearchOptions());
    assertThatSearchReturnsOnly(query, "I3", "I1", "I2");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CLOSE_DATE).asc(false);
    assertThatSearchReturnsOnly(query, "I2", "I1", "I3");
  }

  @Test
  public void sort_by_file_and_line() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file1 = newFileDto(project, null, "F1").setPath("src/main/xoo/org/sonar/samples/File.xoo");
    ComponentDto file2 = newFileDto(project, null, "F2").setPath("src/main/xoo/org/sonar/samples/File2.xoo");

    indexIssues(
      // file F1
      newDoc("F1_2", file1).setLine(20),
      newDoc("F1_1", file1).setLine(null),
      newDoc("F1_3", file1).setLine(25),

      // file F2
      newDoc("F2_1", file2).setLine(9),
      newDoc("F2_2", file2).setLine(109),
      // two issues on the same line -> sort by key
      newDoc("F2_3", file2).setLine(109));

    // ascending sort -> F1 then F2. Line "0" first.
    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_FILE_LINE).asc(true);
    assertThatSearchReturnsOnly(query, "F1_1", "F1_2", "F1_3", "F2_1", "F2_2", "F2_3");

    // descending sort -> F2 then F1
    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_FILE_LINE).asc(false);
    assertThatSearchReturnsOnly(query, "F2_3", "F2_2", "F2_1", "F1_3", "F1_2", "F1_1");
  }

  @Test
  public void default_sort_is_by_creation_date_then_project_then_file_then_line_then_issue_key() {
    OrganizationDto organizationDto = newOrganizationDto();
    ComponentDto project1 = newPrivateProjectDto(organizationDto, "P1");
    ComponentDto file1 = newFileDto(project1, null, "F1").setPath("src/main/xoo/org/sonar/samples/File.xoo");
    ComponentDto file2 = newFileDto(project1, null, "F2").setPath("src/main/xoo/org/sonar/samples/File2.xoo");

    ComponentDto project2 = newPrivateProjectDto(organizationDto, "P2");
    ComponentDto file3 = newFileDto(project2, null, "F3").setPath("src/main/xoo/org/sonar/samples/File3.xoo");

    indexIssues(
      // file F1 from project P1
      newDoc("F1_1", file1).setLine(20).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("F1_2", file1).setLine(null).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("F1_3", file1).setLine(25).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),

      // file F2 from project P1
      newDoc("F2_1", file2).setLine(9).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),
      newDoc("F2_2", file2).setLine(109).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),
      // two issues on the same line -> sort by key
      newDoc("F2_3", file2).setLine(109).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")),

      // file F3 from project P2
      newDoc("F3_1", file3).setLine(20).setFuncCreationDate(parseDateTime("2014-09-24T00:00:00+0100")),
      newDoc("F3_2", file3).setLine(20).setFuncCreationDate(parseDateTime("2014-09-23T00:00:00+0100")));

    assertThatSearchReturnsOnly(IssueQuery.builder(), "F3_1", "F1_2", "F1_1", "F1_3", "F2_1", "F2_2", "F2_3", "F3_2");
  }

  @Test
  public void authorized_issues_on_groups() {
    OrganizationDto org = newOrganizationDto();
    ComponentDto project1 = newPrivateProjectDto(org);
    ComponentDto project2 = newPrivateProjectDto(org);
    ComponentDto project3 = newPrivateProjectDto(org);
    ComponentDto file1 = newFileDto(project1, null);
    ComponentDto file2 = newFileDto(project2, null);
    ComponentDto file3 = newFileDto(project3, null);
    GroupDto group1 = newGroupDto();
    GroupDto group2 = newGroupDto();

    // project1 can be seen by group1
    indexIssue(newDoc("I1", file1));
    authorizationIndexerTester.allowOnlyGroup(project1, group1);
    // project2 can be seen by group2
    indexIssue(newDoc("I2", file2));
    authorizationIndexerTester.allowOnlyGroup(project2, group2);
    // project3 can be seen by nobody
    indexIssue(newDoc("I3", file3));

    userSessionRule.logIn().setGroups(group1);
    assertThatSearchReturnsOnly(IssueQuery.builder(), "I1");

    userSessionRule.logIn().setGroups(group2);
    assertThatSearchReturnsOnly(IssueQuery.builder(), "I2");

    userSessionRule.logIn().setGroups(group1, group2);
    assertThatSearchReturnsOnly(IssueQuery.builder(), "I1", "I2");

    GroupDto otherGroup = newGroupDto();
    userSessionRule.logIn().setGroups(otherGroup);
    assertThatSearchReturnsEmpty(IssueQuery.builder());

    userSessionRule.logIn().setGroups(group1, group2);
    assertThatSearchReturnsEmpty(IssueQuery.builder().projectUuids(singletonList(project3.uuid())));
  }

  @Test
  public void authorized_issues_on_user() {
    OrganizationDto org = newOrganizationDto();
    ComponentDto project1 = newPrivateProjectDto(org);
    ComponentDto project2 = newPrivateProjectDto(org);
    ComponentDto project3 = newPrivateProjectDto(org);
    ComponentDto file1 = newFileDto(project1, null);
    ComponentDto file2 = newFileDto(project2, null);
    ComponentDto file3 = newFileDto(project3, null);
    UserDto user1 = newUserDto();
    UserDto user2 = newUserDto();

    // project1 can be seen by john, project2 by max, project3 cannot be seen by anyone
    indexIssue(newDoc("I1", file1));
    authorizationIndexerTester.allowOnlyUser(project1, user1);
    indexIssue(newDoc("I2", file2));
    authorizationIndexerTester.allowOnlyUser(project2, user2);
    indexIssue(newDoc("I3", file3));

    userSessionRule.logIn(user1);
    assertThatSearchReturnsOnly(IssueQuery.builder(), "I1");
    assertThatSearchReturnsEmpty(IssueQuery.builder().projectUuids(asList(project3.getDbKey())));

    userSessionRule.logIn(user2);
    assertThatSearchReturnsOnly(IssueQuery.builder(), "I2");

    // another user
    userSessionRule.logIn(newUserDto());
    assertThatSearchReturnsEmpty(IssueQuery.builder());
  }

  @Test
  public void root_user_is_authorized_to_access_all_issues() {
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    indexIssue(newDoc("I1", project));
    userSessionRule.logIn().setRoot();

    assertThatSearchReturnsOnly(IssueQuery.builder(), "I1");
  }

  @Test
  public void list_tags() {
    RuleDefinitionDto r1 = db.rules().insert();
    RuleDefinitionDto r2 = db.rules().insert();
    ruleIndexer.commitAndIndex(db.getSession(), asList(r1.getId(), r2.getId()));

    OrganizationDto org = db.organizations().insert();
    OrganizationDto anotherOrg = db.organizations().insert();
    ComponentDto project = newPrivateProjectDto(newOrganizationDto());
    ComponentDto file = newFileDto(project, null);
    indexIssues(
      newDoc("I42", file).setOrganizationUuid(anotherOrg.getUuid()).setRuleId(r1.getId()).setTags(of("another")),
      newDoc("I1", file).setOrganizationUuid(org.getUuid()).setRuleId(r1.getId()).setTags(of("convention", "java8", "bug")),
      newDoc("I2", file).setOrganizationUuid(org.getUuid()).setRuleId(r1.getId()).setTags(of("convention", "bug")),
      newDoc("I3", file).setOrganizationUuid(org.getUuid()).setRuleId(r2.getId()),
      newDoc("I4", file).setOrganizationUuid(org.getUuid()).setRuleId(r1.getId()).setTags(of("convention")));

    assertThat(underTest.listTags(org, null, 100)).containsOnly("convention", "java8", "bug");
    assertThat(underTest.listTags(org, null, 2)).containsOnly("bug", "convention");
    assertThat(underTest.listTags(org, "vent", 100)).containsOnly("convention");
    assertThat(underTest.listTags(org, null, 1)).containsOnly("bug");
    assertThat(underTest.listTags(org, null, 100)).containsOnly("convention", "java8", "bug");
    assertThat(underTest.listTags(org, "invalidRegexp[", 100)).isEmpty();
    assertThat(underTest.listTags(null, null, 100)).containsExactlyInAnyOrder("another", "convention", "java8", "bug");
  }

  @Test
  public void fail_to_list_tags_when_size_greater_than_500() {
    OrganizationDto organization = db.organizations().insert();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Page size must be lower than or equals to 500");

    underTest.listTags(organization, null, 501);
  }

  @Test
  public void test_listAuthors() {
    OrganizationDto org = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(org);
    indexIssues(
      newDoc("issue1", project).setAuthorLogin("luke.skywalker"),
      newDoc("issue2", project).setAuthorLogin("luke@skywalker.name"),
      newDoc("issue3", project).setAuthorLogin(null),
      newDoc("issue4", project).setAuthorLogin("anakin@skywalker.name"));
    IssueQuery query = IssueQuery.builder()
      .checkAuthorization(false)
      .build();

    assertThat(underTest.listAuthors(query, null, 5)).containsExactly("anakin@skywalker.name", "luke.skywalker", "luke@skywalker.name");
    assertThat(underTest.listAuthors(query, null, 2)).containsExactly("anakin@skywalker.name", "luke.skywalker");
    assertThat(underTest.listAuthors(query, "uke", 5)).containsExactly("luke.skywalker", "luke@skywalker.name");
    assertThat(underTest.listAuthors(query, null, 1)).containsExactly("anakin@skywalker.name");
    assertThat(underTest.listAuthors(query, null, Integer.MAX_VALUE)).containsExactly("anakin@skywalker.name", "luke.skywalker", "luke@skywalker.name");
  }

  @Test
  public void listAuthors_escapes_regexp_special_characters() {
    OrganizationDto org = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(org);
    indexIssues(
      newDoc("issue1", project).setAuthorLogin("name++"));
    IssueQuery query = IssueQuery.builder()
      .checkAuthorization(false)
      .build();

    assertThat(underTest.listAuthors(query, "invalidRegexp[", 5)).isEmpty();
    assertThat(underTest.listAuthors(query, "nam+", 5)).isEmpty();
    assertThat(underTest.listAuthors(query, "name+", 5)).containsExactly("name++");
    assertThat(underTest.listAuthors(query, ".*", 5)).isEmpty();
  }

  @Test
  public void filter_by_organization() {
    OrganizationDto org1 = newOrganizationDto();
    ComponentDto projectInOrg1 = newPrivateProjectDto(org1);
    OrganizationDto org2 = newOrganizationDto();
    ComponentDto projectInOrg2 = newPrivateProjectDto(org2);

    indexIssues(newDoc("issueInOrg1", projectInOrg1), newDoc("issue1InOrg2", projectInOrg2), newDoc("issue2InOrg2", projectInOrg2));

    verifyOrganizationFilter(org1.getUuid(), "issueInOrg1");
    verifyOrganizationFilter(org2.getUuid(), "issue1InOrg2", "issue2InOrg2");
    verifyOrganizationFilter("does_not_exist");
  }

  @Test
  public void filter_by_organization_and_project() {
    OrganizationDto org1 = newOrganizationDto();
    ComponentDto projectInOrg1 = newPrivateProjectDto(org1);
    OrganizationDto org2 = newOrganizationDto();
    ComponentDto projectInOrg2 = newPrivateProjectDto(org2);

    indexIssues(newDoc("issueInOrg1", projectInOrg1), newDoc("issue1InOrg2", projectInOrg2), newDoc("issue2InOrg2", projectInOrg2));

    // no conflict
    IssueQuery.Builder query = IssueQuery.builder().organizationUuid(org1.getUuid()).projectUuids(singletonList(projectInOrg1.uuid()));
    assertThatSearchReturnsOnly(query, "issueInOrg1");

    // conflict
    query = IssueQuery.builder().organizationUuid(org1.getUuid()).projectUuids(singletonList(projectInOrg2.uuid()));
    assertThatSearchReturnsEmpty(query);
  }

  @Test
  public void countTags() {
    OrganizationDto org = newOrganizationDto();
    ComponentDto project = newPrivateProjectDto(org);
    indexIssues(
      newDoc("issue1", project).setTags(ImmutableSet.of("convention", "java8", "bug")),
      newDoc("issue2", project).setTags(ImmutableSet.of("convention", "bug")),
      newDoc("issue3", project).setTags(emptyList()),
      newDoc("issue4", project).setTags(ImmutableSet.of("convention", "java8", "bug")).setResolution(Issue.RESOLUTION_FIXED),
      newDoc("issue5", project).setTags(ImmutableSet.of("convention")));

    assertThat(underTest.countTags(projectQuery(project.uuid()), 5)).containsOnly(entry("convention", 3L), entry("bug", 2L), entry("java8", 1L));
    assertThat(underTest.countTags(projectQuery(project.uuid()), 2)).contains(entry("convention", 3L), entry("bug", 2L)).doesNotContainEntry("java8", 1L);
    assertThat(underTest.countTags(projectQuery("other"), 10)).isEmpty();
  }

  @Test
  public void searchBranchStatistics() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch1 = db.components().insertProjectBranch(project);
    ComponentDto branch2 = db.components().insertProjectBranch(project);
    ComponentDto branch3 = db.components().insertProjectBranch(project);
    ComponentDto fileOnBranch3 = db.components().insertComponent(newFileDto(branch3));
    indexIssues(newDoc(project),
      newDoc(branch1).setType(BUG).setResolution(null), newDoc(branch1).setType(VULNERABILITY).setResolution(null), newDoc(branch1).setType(CODE_SMELL).setResolution(null),
      newDoc(branch1).setType(CODE_SMELL).setResolution(RESOLUTION_FIXED),
      newDoc(branch3).setType(CODE_SMELL).setResolution(null), newDoc(branch3).setType(CODE_SMELL).setResolution(null),
      newDoc(fileOnBranch3).setType(CODE_SMELL).setResolution(null), newDoc(fileOnBranch3).setType(CODE_SMELL).setResolution(RESOLUTION_FIXED));

    List<BranchStatistics> branchStatistics = underTest.searchBranchStatistics(project.uuid(), asList(branch1.uuid(), branch2.uuid(), branch3.uuid()));

    assertThat(branchStatistics).extracting(BranchStatistics::getBranchUuid, BranchStatistics::getBugs, BranchStatistics::getVulnerabilities, BranchStatistics::getCodeSmells)
      .containsExactlyInAnyOrder(
        tuple(branch1.uuid(), 1L, 1L, 1L),
        tuple(branch3.uuid(), 0L, 0L, 3L));
  }

  @Test
  public void searchBranchStatistics_on_many_branches() {
    ComponentDto project = db.components().insertMainBranch();
    List<String> branchUuids = new ArrayList<>();
    List<Tuple> expectedResult = new ArrayList<>();
    IntStream.range(0, 15).forEach(i -> {
      ComponentDto branch = db.components().insertProjectBranch(project);
      addIssues(branch, 1 + i, 2 + i, 3 + i);
      expectedResult.add(tuple(branch.uuid(), 1L + i, 2L + i, 3L + i));
      branchUuids.add(branch.uuid());
    });

    List<BranchStatistics> branchStatistics = underTest.searchBranchStatistics(project.uuid(), branchUuids);

    assertThat(branchStatistics)
      .extracting(BranchStatistics::getBranchUuid, BranchStatistics::getBugs, BranchStatistics::getVulnerabilities, BranchStatistics::getCodeSmells)
      .hasSize(15)
      .containsAll(expectedResult);
  }

  @Test
  public void searchBranchStatistics_on_empty_list() {
    ComponentDto project = db.components().insertMainBranch();

    assertThat(underTest.searchBranchStatistics(project.uuid(), emptyList())).isEmpty();
    assertThat(underTest.searchBranchStatistics(project.uuid(), singletonList("unknown"))).isEmpty();
  }

  private void addIssues(ComponentDto component, int bugs, int vulnerabilities, int codeSmelles) {
    List<IssueDoc> issues = new ArrayList<>();
    IntStream.range(0, bugs).forEach(b -> issues.add(newDoc(component).setType(BUG).setResolution(null)));
    IntStream.range(0, vulnerabilities).forEach(v -> issues.add(newDoc(component).setType(VULNERABILITY).setResolution(null)));
    IntStream.range(0, codeSmelles).forEach(c -> issues.add(newDoc(component).setType(CODE_SMELL).setResolution(null)));
    indexIssues(issues.toArray(new IssueDoc[issues.size()]));
  }

  private IssueQuery projectQuery(String projectUuid) {
    return IssueQuery.builder().projectUuids(singletonList(projectUuid)).resolved(false).build();
  }

  private void verifyOrganizationFilter(String organizationUuid, String... expectedIssueKeys) {
    IssueQuery.Builder query = IssueQuery.builder().organizationUuid(organizationUuid);
    assertThatSearchReturnsOnly(query, expectedIssueKeys);
  }

  private void indexIssues(IssueDoc... issues) {
    issueIndexer.index(asList(issues).iterator());
    for (IssueDoc issue : issues) {
      PermissionIndexerDao.Dto access = new PermissionIndexerDao.Dto(issue.projectUuid(), "TRK");
      access.allowAnyone();
      authorizationIndexerTester.allow(access);
    }
  }

  private void indexIssue(IssueDoc issue) {
    issueIndexer.index(Iterators.singletonIterator(issue));
  }

  private void indexView(String viewUuid, List<String> projects) {
    viewIndexer.index(new ViewDoc().setUuid(viewUuid).setProjects(projects));
  }

  /**
   * Execute the search request and return the document ids of results.
   */
  private List<String> searchAndReturnKeys(IssueQuery.Builder query) {
    return Arrays.stream(underTest.search(query.build(), new SearchOptions()).getHits().getHits())
      .map(SearchHit::getId)
      .collect(Collectors.toList());
  }

  private void assertThatSearchReturnsOnly(IssueQuery.Builder query, String... expectedIssueKeys) {
    List<String> keys = searchAndReturnKeys(query);
    assertThat(keys).containsExactlyInAnyOrder(expectedIssueKeys);
  }

  private void assertThatSearchReturnsEmpty(IssueQuery.Builder query) {
    List<String> keys = searchAndReturnKeys(query);
    assertThat(keys).isEmpty();
  }

  private void assertThatFacetHasExactly(IssueQuery.Builder query, String facet, Map.Entry<String, Long>... expectedEntries) {
    SearchResponse result = underTest.search(query.build(), new SearchOptions().addFacets(singletonList(facet)));
    Facets facets = new Facets(result, system2.getDefaultTimeZone());
    assertThat(facets.getNames()).containsOnly(facet);
    assertThat(facets.get(facet)).containsExactly(expectedEntries);
  }

  private void assertThatFacetHasOnly(IssueQuery.Builder query, String facet, Map.Entry<String, Long>... expectedEntries) {
    SearchResponse result = underTest.search(query.build(), new SearchOptions().addFacets(singletonList(facet)));
    Facets facets = new Facets(result, system2.getDefaultTimeZone());
    assertThat(facets.getNames()).containsOnly(facet);
    assertThat(facets.get(facet)).containsOnly(expectedEntries);
  }
}
