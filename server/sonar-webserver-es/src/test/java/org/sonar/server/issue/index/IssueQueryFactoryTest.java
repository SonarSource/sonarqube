/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.rule.RuleDbTester;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.SearchRequest;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.ANALYSIS_FROM_SONARQUBE_9_4_KEY;
import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.api.utils.DateUtils.addDays;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.ComponentTesting.newSubPortfolio;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;
import static org.sonar.db.rule.RuleTesting.newRule;

public class IssueQueryFactoryTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public LogTester logTester = new LogTester();

  private final RuleDbTester ruleDbTester = new RuleDbTester(db);
  private final Clock clock = mock(Clock.class);
  private final IssueQueryFactory underTest = new IssueQueryFactory(db.getDbClient(), clock, userSession);

  @Test
  public void create_from_parameters() {
    String ruleAdHocName = "New Name";
    UserDto user = db.users().insertUser(u -> u.setLogin("joanna"));
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    RuleDto rule1 = ruleDbTester.insert(r -> r.setAdHocName(ruleAdHocName));
    RuleDto rule2 = ruleDbTester.insert(r -> r.setAdHocName(ruleAdHocName));
    newRule(RuleKey.of("findbugs", "NullReference"));
    SearchRequest request = new SearchRequest()
      .setIssues(asList("anIssueKey"))
      .setSeverities(asList("MAJOR", "MINOR"))
      .setStatuses(asList("CLOSED"))
      .setResolutions(asList("FALSE-POSITIVE"))
      .setResolved(true)
      .setProjectKeys(asList(project.getKey()))
      .setDirectories(asList("aDirPath"))
      .setFiles(asList(file.uuid()))
      .setAssigneesUuid(asList(user.getUuid()))
      .setScopes(asList("MAIN", "TEST"))
      .setLanguages(asList("xoo"))
      .setTags(asList("tag1", "tag2"))
      .setAssigned(true)
      .setCreatedAfter("2013-04-16T09:08:24+0200")
      .setCreatedBefore("2013-04-17T09:08:24+0200")
      .setRules(asList(rule1.getKey().toString(), rule2.getKey().toString()))
      .setSort("CREATION_DATE")
      .setAsc(true)
      .setCodeVariants(asList("variant1", "variant2"))
      .setPrioritizedRule(true);

    IssueQuery query = underTest.create(request);

    assertThat(query.issueKeys()).containsOnly("anIssueKey");
    assertThat(query.severities()).containsOnly("MAJOR", "MINOR");
    assertThat(query.statuses()).containsOnly("CLOSED");
    assertThat(query.resolutions()).containsOnly("FALSE-POSITIVE");
    assertThat(query.resolved()).isTrue();
    assertThat(query.projectUuids()).containsOnly(projectData.projectUuid());
    assertThat(query.files()).containsOnly(file.uuid());
    assertThat(query.assignees()).containsOnly(user.getUuid());
    assertThat(query.scopes()).containsOnly("TEST", "MAIN");
    assertThat(query.languages()).containsOnly("xoo");
    assertThat(query.tags()).containsOnly("tag1", "tag2");
    assertThat(query.onComponentOnly()).isFalse();
    assertThat(query.assigned()).isTrue();
    assertThat(query.rules()).hasSize(2);
    assertThat(query.ruleUuids()).hasSize(2);
    assertThat(query.directories()).containsOnly("aDirPath");
    assertThat(query.createdAfter().date()).isEqualTo(parseDateTime("2013-04-16T09:08:24+0200"));
    assertThat(query.createdAfter().inclusive()).isTrue();
    assertThat(query.createdBefore()).isEqualTo(parseDateTime("2013-04-17T09:08:24+0200"));
    assertThat(query.sort()).isEqualTo(IssueQuery.SORT_BY_CREATION_DATE);
    assertThat(query.asc()).isTrue();
    assertThat(query.codeVariants()).containsOnly("variant1", "variant2");
    assertThat(query.prioritizedRule()).isTrue();
  }

  @Test
  public void getIssuesFixedByPullRequest_returnIssuesFixedByThePullRequest() {
    String ruleAdHocName = "New Name";
    UserDto user = db.users().insertUser(u -> u.setLogin("joanna"));
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    RuleDto rule1 = ruleDbTester.insert(r -> r.setAdHocName(ruleAdHocName));
    RuleDto rule2 = ruleDbTester.insert(r -> r.setAdHocName(ruleAdHocName));
    newRule(RuleKey.of("findbugs", "NullReference"));
    SearchRequest request = new SearchRequest()
      .setIssues(asList("anIssueKey"))
      .setSeverities(asList("MAJOR", "MINOR"))
      .setStatuses(asList("CLOSED"))
      .setResolutions(asList("FALSE-POSITIVE"))
      .setResolved(true)
      .setProjectKeys(asList(project.getKey()))
      .setDirectories(asList("aDirPath"))
      .setFiles(asList(file.uuid()))
      .setAssigneesUuid(asList(user.getUuid()))
      .setScopes(asList("MAIN", "TEST"))
      .setLanguages(asList("xoo"))
      .setTags(asList("tag1", "tag2"))
      .setAssigned(true)
      .setCreatedAfter("2013-04-16T09:08:24+0200")
      .setCreatedBefore("2013-04-17T09:08:24+0200")
      .setRules(asList(rule1.getKey().toString(), rule2.getKey().toString()))
      .setSort("CREATION_DATE")
      .setAsc(true)
      .setCodeVariants(asList("variant1", "variant2"))
      .setPrioritizedRule(false);

    IssueQuery query = underTest.create(request);

    assertThat(query.issueKeys()).containsOnly("anIssueKey");
    assertThat(query.severities()).containsOnly("MAJOR", "MINOR");
    assertThat(query.statuses()).containsOnly("CLOSED");
    assertThat(query.resolutions()).containsOnly("FALSE-POSITIVE");
    assertThat(query.resolved()).isTrue();
    assertThat(query.projectUuids()).containsOnly(projectData.projectUuid());
    assertThat(query.files()).containsOnly(file.uuid());
    assertThat(query.assignees()).containsOnly(user.getUuid());
    assertThat(query.scopes()).containsOnly("TEST", "MAIN");
    assertThat(query.languages()).containsOnly("xoo");
    assertThat(query.tags()).containsOnly("tag1", "tag2");
    assertThat(query.onComponentOnly()).isFalse();
    assertThat(query.assigned()).isTrue();
    assertThat(query.rules()).hasSize(2);
    assertThat(query.ruleUuids()).hasSize(2);
    assertThat(query.directories()).containsOnly("aDirPath");
    assertThat(query.createdAfter().date()).isEqualTo(parseDateTime("2013-04-16T09:08:24+0200"));
    assertThat(query.createdAfter().inclusive()).isTrue();
    assertThat(query.createdBefore()).isEqualTo(parseDateTime("2013-04-17T09:08:24+0200"));
    assertThat(query.sort()).isEqualTo(IssueQuery.SORT_BY_CREATION_DATE);
    assertThat(query.asc()).isTrue();
    assertThat(query.codeVariants()).containsOnly("variant1", "variant2");
    assertThat(query.prioritizedRule()).isFalse();
  }

  @Test
  public void create_with_rule_key_that_does_not_exist_in_the_db() {
    db.users().insertUser(u -> u.setLogin("joanna"));
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertComponent(newFileDto(project));
    newRule(RuleKey.of("findbugs", "NullReference"));
    SearchRequest request = new SearchRequest()
      .setRules(asList("unknown:key1", "unknown:key2"));

    IssueQuery query = underTest.create(request);

    assertThat(query.rules()).isEmpty();
    assertThat(query.ruleUuids()).containsExactly("non-existing-uuid");
  }

  @Test
  public void in_new_code_period_start_date_is_exclusive() {
    long newCodePeriodStart = addDays(new Date(), -14).getTime();

    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    SnapshotDto analysis = db.components().insertSnapshot(project, s -> s.setPeriodDate(newCodePeriodStart));

    SearchRequest request = new SearchRequest()
      .setComponentUuids(Collections.singletonList(file.uuid()))
      .setOnComponentOnly(true)
      .setInNewCodePeriod(true);

    IssueQuery query = underTest.create(request);

    assertThat(query.componentUuids()).containsOnly(file.uuid());
    assertThat(query.createdAfter().date()).isEqualTo(new Date(newCodePeriodStart));
    assertThat(query.createdAfter().inclusive()).isFalse();
    assertThat(query.newCodeOnReference()).isNull();
  }

  @Test
  public void new_code_period_does_not_rely_on_date_for_reference_branch_with_analysis_after_sonarqube_94() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));

    db.components().insertSnapshot(project, s -> s.setPeriodMode(REFERENCE_BRANCH.name())
      .setPeriodParam("master"));

    MetricDto analysisMetric = db.measures().insertMetric(m -> m.setKey(ANALYSIS_FROM_SONARQUBE_9_4_KEY));
    db.measures().insertMeasure(project, measure -> measure.addValue(analysisMetric.getKey(), "true"));

    SearchRequest request = new SearchRequest()
      .setComponentUuids(Collections.singletonList(file.uuid()))
      .setOnComponentOnly(true)
      .setInNewCodePeriod(true);

    IssueQuery query = underTest.create(request);

    assertThat(query.componentUuids()).containsOnly(file.uuid());
    assertThat(query.newCodeOnReference()).isTrue();
    assertThat(query.createdAfter()).isNull();
  }

  @Test
  public void dates_are_inclusive() {
    when(clock.getZone()).thenReturn(ZoneId.of("Europe/Paris"));
    SearchRequest request = new SearchRequest()
      .setCreatedAfter("2013-04-16")
      .setCreatedBefore("2013-04-17");

    IssueQuery query = underTest.create(request);

    assertThat(query.createdAfter().date()).isEqualTo(parseDateTime("2013-04-16T00:00:00+0200"));
    assertThat(query.createdAfter().inclusive()).isTrue();
    assertThat(query.createdBefore()).isEqualTo(parseDateTime("2013-04-18T00:00:00+0200"));
  }

  @Test
  public void creation_date_support_localdate() {
    when(clock.getZone()).thenReturn(ZoneId.of("Europe/Paris"));
    SearchRequest request = new SearchRequest()
      .setCreatedAt("2013-04-16");

    IssueQuery query = underTest.create(request);

    assertThat(query.createdAt()).isEqualTo(parseDateTime("2013-04-16T00:00:00+0200"));
  }

  @Test
  public void use_provided_timezone_to_parse_createdAfter() {
    SearchRequest request = new SearchRequest()
      .setCreatedAfter("2020-04-16")
      .setTimeZone("Australia/Sydney");

    IssueQuery query = underTest.create(request);

    assertThat(query.createdAfter().date()).isEqualTo(parseDateTime("2020-04-16T00:00:00+1000"));
  }

  @Test
  public void use_provided_timezone_to_parse_createdBefore() {
    SearchRequest request = new SearchRequest()
      .setCreatedBefore("2020-04-16")
      .setTimeZone("Europe/Moscow");

    IssueQuery query = underTest.create(request);

    assertThat(query.createdBefore()).isEqualTo(parseDateTime("2020-04-17T00:00:00+0300"));
  }

  @Test
  public void creation_date_support_zoneddatetime() {
    SearchRequest request = new SearchRequest()
      .setCreatedAt("2013-04-16T09:08:24+0200");

    IssueQuery query = underTest.create(request);

    assertThat(query.createdAt()).isEqualTo(parseDateTime("2013-04-16T09:08:24+0200"));
  }

  @Test
  public void add_unknown_when_no_component_found() {
    SearchRequest request = new SearchRequest()
      .setComponentKeys(asList("does_not_exist"));

    IssueQuery query = underTest.create(request);

    assertThat(query.componentUuids()).containsOnly("<UNKNOWN>");
  }

  @Test
  public void query_without_any_parameter() {
    SearchRequest request = new SearchRequest();

    IssueQuery query = underTest.create(request);

    assertThat(query.componentUuids()).isEmpty();
    assertThat(query.projectUuids()).isEmpty();
    assertThat(query.directories()).isEmpty();
    assertThat(query.files()).isEmpty();
    assertThat(query.viewUuids()).isEmpty();
    assertThat(query.branchUuid()).isNull();
  }

  @Test
  public void fail_if_components_and_components_uuid_params_are_set_at_the_same_time() {
    SearchRequest request = new SearchRequest()
      .setComponentKeys(singletonList("foo"))
      .setComponentUuids(singletonList("bar"));

    assertThatThrownBy(() -> underTest.create(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("At most one of the following parameters can be provided: components and componentUuids");
  }

  @Test
  public void timeZone_ifZoneFromQueryIsUnknown_fallbacksToClockZone() {
    SearchRequest request = new SearchRequest().setTimeZone("Poitou-Charentes");
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    IssueQuery issueQuery = underTest.create(request);
    assertThat(issueQuery.timeZone()).isEqualTo(clock.getZone());
    assertThat(logTester.logs()).containsOnly("TimeZone 'Poitou-Charentes' cannot be parsed as a valid zone ID");
  }

  @Test
  public void param_componentUuids_enables_search_in_view_tree_if_user_has_permission_on_view() {
    ComponentDto view = db.components().insertPublicPortfolio();
    SearchRequest request = new SearchRequest()
      .setComponentUuids(singletonList(view.uuid()));
    userSession.registerPortfolios(view);

    IssueQuery query = underTest.create(request);

    assertThat(query.viewUuids()).containsOnly(view.uuid());
    assertThat(query.onComponentOnly()).isFalse();
  }

  @Test
  public void application_search_project_issues() {
    ProjectData projectData1 = db.components().insertPublicProject();
    ComponentDto project1 = projectData1.getMainBranchComponent();
    ProjectData projectData2 = db.components().insertPublicProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    ProjectData applicationData = db.components().insertPublicApplication();
    ComponentDto applicationMainBranch = applicationData.getMainBranchComponent();
    db.components().insertComponents(newProjectCopy("PC1", project1, applicationMainBranch));
    db.components().insertComponents(newProjectCopy("PC2", project2, applicationMainBranch));
    userSession.registerApplication(applicationData.getProjectDto())
      .registerProjects(projectData1.getProjectDto(), projectData2.getProjectDto())
      .registerBranches(applicationData.getMainBranchDto());

    IssueQuery result = underTest.create(new SearchRequest().setComponentUuids(singletonList(applicationMainBranch.uuid())));

    assertThat(result.viewUuids()).containsExactlyInAnyOrder(applicationMainBranch.uuid());
  }

  @Test
  public void application_search_project_issues_returns_empty_if_user_cannot_access_child_projects() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto application = db.components().insertPublicApplication().getMainBranchComponent();
    db.components().insertComponents(newProjectCopy("PC1", project1, application));
    db.components().insertComponents(newProjectCopy("PC2", project2, application));

    IssueQuery result = underTest.create(new SearchRequest().setComponentUuids(singletonList(application.uuid())));

    assertThat(result.viewUuids()).containsOnly("<UNKNOWN>");
  }

  @Test
  public void application_search_project_issues_in_new_code_with_and_without_analysis_after_sonarqube_94() {
    Date now = new Date();
    when(clock.millis()).thenReturn(now.getTime());
    ProjectData projectData1 = db.components().insertPublicProject();
    ComponentDto project1 = projectData1.getMainBranchComponent();
    SnapshotDto analysis1 = db.components().insertSnapshot(project1, s -> s.setPeriodDate(addDays(now, -14).getTime()));
    ProjectData projectData2 = db.components().insertPublicProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    db.components().insertSnapshot(project2, s -> s.setPeriodDate(null));
    ProjectData projectData3 = db.components().insertPublicProject();
    ComponentDto project3 = projectData3.getMainBranchComponent();
    ProjectData projectData4 = db.components().insertPublicProject();
    ComponentDto project4 = projectData4.getMainBranchComponent();
    SnapshotDto analysis2 = db.components().insertSnapshot(project4,
      s -> s.setPeriodMode(REFERENCE_BRANCH.name()).setPeriodParam("master"));
    ProjectData applicationData = db.components().insertPublicApplication();
    ComponentDto application = applicationData.getMainBranchComponent();
    MetricDto analysisMetric = db.measures().insertMetric(m -> m.setKey(ANALYSIS_FROM_SONARQUBE_9_4_KEY));
    db.measures().insertMeasure(project4, measure -> measure.addValue(analysisMetric.getKey(), "true"));
    db.components().insertComponents(newProjectCopy("PC1", project1, application));
    db.components().insertComponents(newProjectCopy("PC2", project2, application));
    db.components().insertComponents(newProjectCopy("PC3", project3, application));
    db.components().insertComponents(newProjectCopy("PC4", project4, application));
    userSession.registerApplication(applicationData.getProjectDto())
        .registerBranches(applicationData.getMainBranchDto());
    userSession.registerProjects(projectData1.getProjectDto(), projectData2.getProjectDto(), projectData3.getProjectDto(), projectData4.getProjectDto());

    IssueQuery result = underTest.create(new SearchRequest()
      .setComponentUuids(singletonList(application.uuid()))
      .setInNewCodePeriod(true));

    assertThat(result.createdAfterByProjectUuids()).hasSize(1);
    assertThat(result.createdAfterByProjectUuids().entrySet()).extracting(Map.Entry::getKey, e -> e.getValue().date(), e -> e.getValue().inclusive()).containsOnly(
      tuple(project1.uuid(), new Date(analysis1.getPeriodDate()), false));
    assertThat(result.newCodeOnReferenceByProjectUuids()).hasSize(1);
    assertThat(result.newCodeOnReferenceByProjectUuids()).containsOnly(project4.uuid());
    assertThat(result.viewUuids()).containsExactlyInAnyOrder(application.uuid());
  }

  @Test
  public void return_empty_results_if_not_allowed_to_search_for_subview() {
    ComponentDto view = db.components().insertPrivatePortfolio();
    ComponentDto subView = db.components().insertComponent(newSubPortfolio(view));
    SearchRequest request = new SearchRequest()
      .setComponentUuids(singletonList(subView.uuid()));

    IssueQuery query = underTest.create(request);

    assertThat(query.viewUuids()).containsOnly("<UNKNOWN>");
  }

  @Test
  public void param_componentUuids_enables_search_on_project_tree_by_default() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    SearchRequest request = new SearchRequest()
      .setComponentUuids(asList(mainBranch.uuid()));

    IssueQuery query = underTest.create(request);
    assertThat(query.projectUuids()).containsExactly(projectData.projectUuid());
    assertThat(query.onComponentOnly()).isFalse();
  }

  @Test
  public void onComponentOnly_restricts_search_to_specified_componentKeys() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    SearchRequest request = new SearchRequest()
      .setComponentKeys(asList(project.getKey()))
      .setOnComponentOnly(true);

    IssueQuery query = underTest.create(request);

    assertThat(query.projectUuids()).isEmpty();
    assertThat(query.componentUuids()).containsExactly(project.uuid());
    assertThat(query.onComponentOnly()).isTrue();
  }

  @Test
  public void param_componentUuids_enables_search_in_directory_tree() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto dir = db.components().insertComponent(newDirectory(project, "src/main/java/foo"));
    SearchRequest request = new SearchRequest()
      .setComponentUuids(asList(dir.uuid()));

    IssueQuery query = underTest.create(request);

    assertThat(query.directories()).containsOnly(dir.path());
    assertThat(query.onComponentOnly()).isFalse();
  }

  @Test
  public void param_componentUuids_enables_search_by_file() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    SearchRequest request = new SearchRequest()
      .setComponentUuids(asList(file.uuid()));

    IssueQuery query = underTest.create(request);

    assertThat(query.componentUuids()).containsExactly(file.uuid());
  }

  @Test
  public void param_componentUuids_enables_search_by_test_file() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project).setQualifier(ComponentQualifiers.UNIT_TEST_FILE));
    SearchRequest request = new SearchRequest()
      .setComponentUuids(asList(file.uuid()));

    IssueQuery query = underTest.create(request);

    assertThat(query.componentUuids()).containsExactly(file.uuid());
  }

  @Test
  public void search_issue_from_branch() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    String branchName = secure().nextAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(mainBranch, b -> b.setKey(branchName));

    assertThat(underTest.create(new SearchRequest()
      .setProjectKeys(singletonList(branch.getKey()))
      .setBranch(branchName)))
      .extracting(IssueQuery::branchUuid, query -> new ArrayList<>(query.projectUuids()), IssueQuery::isMainBranch)
      .containsOnly(branch.uuid(), singletonList(projectData.projectUuid()), false);

    assertThat(underTest.create(new SearchRequest()
      .setComponentKeys(singletonList(branch.getKey()))
      .setBranch(branchName)))
      .extracting(IssueQuery::branchUuid, query -> new ArrayList<>(query.projectUuids()), IssueQuery::isMainBranch)
      .containsOnly(branch.uuid(), singletonList(projectData.projectUuid()), false);
  }

  @Test
  public void search_file_issue_from_branch() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    String branchName = secure().nextAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey(branchName));
    ComponentDto file = db.components().insertComponent(newFileDto(branch, project.uuid()));

    assertThat(underTest.create(new SearchRequest()
      .setComponentKeys(singletonList(file.getKey()))
      .setBranch(branchName)))
      .extracting(IssueQuery::branchUuid, query -> new ArrayList<>(query.componentUuids()), IssueQuery::isMainBranch)
      .containsOnly(branch.uuid(), singletonList(file.uuid()), false);

    assertThat(underTest.create(new SearchRequest()
      .setComponentKeys(singletonList(branch.getKey()))
      .setFiles(singletonList(file.path()))
      .setBranch(branchName)))
      .extracting(IssueQuery::branchUuid, query -> new ArrayList<>(query.files()), IssueQuery::isMainBranch)
      .containsOnly(branch.uuid(), singletonList(file.path()), false);

    assertThat(underTest.create(new SearchRequest()
      .setProjectKeys(singletonList(branch.getKey()))
      .setFiles(singletonList(file.path()))
      .setBranch(branchName)))
      .extracting(IssueQuery::branchUuid, query -> new ArrayList<>(query.files()), IssueQuery::isMainBranch)
      .containsOnly(branch.uuid(), singletonList(file.path()), false);
  }

  @Test
  public void search_issue_on_component_only_from_branch() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    String branchName = secure().nextAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey(branchName));
    ComponentDto file = db.components().insertComponent(newFileDto(branch, project.uuid()));

    assertThat(underTest.create(new SearchRequest()
      .setComponentKeys(singletonList(file.getKey()))
      .setBranch(branchName)
      .setOnComponentOnly(true)))
      .extracting(IssueQuery::branchUuid, query -> new ArrayList<>(query.componentUuids()), IssueQuery::isMainBranch)
      .containsOnly(branch.uuid(), singletonList(file.uuid()), false);
  }

  @Test
  public void search_issues_from_main_branch() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    db.components().insertProjectBranch(mainBranch);

    assertThat(underTest.create(new SearchRequest()
      .setProjectKeys(singletonList(projectData.projectKey()))
      .setBranch(DEFAULT_MAIN_BRANCH_NAME)))
      .extracting(IssueQuery::branchUuid, query -> new ArrayList<>(query.projectUuids()), IssueQuery::isMainBranch)
      .containsOnly(mainBranch.uuid(), singletonList(projectData.projectUuid()), true);
    assertThat(underTest.create(new SearchRequest()
      .setComponentKeys(singletonList(mainBranch.getKey()))
      .setBranch(DEFAULT_MAIN_BRANCH_NAME)))
      .extracting(IssueQuery::branchUuid, query -> new ArrayList<>(query.projectUuids()), IssueQuery::isMainBranch)
      .containsOnly(mainBranch.uuid(), singletonList(projectData.projectUuid()), true);
  }

  @Test
  public void search_by_application_key() {
    ProjectData applicationData = db.components().insertPrivateApplication();
    ComponentDto application = applicationData.getMainBranchComponent();
    ProjectData projectData1 = db.components().insertPrivateProject();
    ComponentDto project1 = projectData1.getMainBranchComponent();
    ProjectData projectData2 = db.components().insertPrivateProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    db.components().insertComponents(newProjectCopy(project1, application));
    db.components().insertComponents(newProjectCopy(project2, application));
    userSession.registerApplication(applicationData.getProjectDto())
      .registerProjects(projectData1.getProjectDto(), projectData2.getProjectDto())
      .addProjectPermission(USER, applicationData.getProjectDto())
      .addProjectPermission(USER, projectData1.getProjectDto())
      .addProjectPermission(USER, projectData2.getProjectDto())
        .registerBranches(applicationData.getMainBranchDto());

    assertThat(underTest.create(new SearchRequest()
        .setComponentKeys(singletonList(application.getKey())))
      .viewUuids()).containsExactly(applicationData.getMainBranchComponent().uuid());
  }

  @Test
  public void search_by_application_key_and_branch() {
    ComponentDto application = db.components().insertPublicProject(c -> c.setQualifier(APP).setKey("app")).getMainBranchComponent();
    String branchName1 = "app-branch1";
    String branchName2 = "app-branch2";
    ComponentDto applicationBranch1 = db.components().insertProjectBranch(application, a -> a.setKey(branchName1));
    ComponentDto applicationBranch2 = db.components().insertProjectBranch(application, a -> a.setKey(branchName2));
    ProjectData projectData1 = db.components().insertPrivateProject(p -> p.setKey("prj1"));
    ComponentDto mainBranch1 = projectData1.getMainBranchComponent();
    ComponentDto project1Branch1 = db.components().insertProjectBranch(mainBranch1);
    db.components().insertComponent(newFileDto(project1Branch1, mainBranch1.uuid()));
    ComponentDto project1Branch2 = db.components().insertProjectBranch(mainBranch1);
    ComponentDto project2 = db.components().insertPrivateProject(p -> p.setKey("prj2")).getMainBranchComponent();
    db.components().insertComponents(newProjectCopy(project1Branch1, applicationBranch1));
    db.components().insertComponents(newProjectCopy(project2, applicationBranch1));
    db.components().insertComponents(newProjectCopy(project1Branch2, applicationBranch2));

    // Search on applicationBranch1
    assertThat(underTest.create(new SearchRequest()
      .setComponentKeys(singletonList(applicationBranch1.getKey()))
      .setBranch(branchName1)))
      .extracting(IssueQuery::branchUuid, query -> new ArrayList<>(query.projectUuids()), IssueQuery::isMainBranch)
      .containsOnly(applicationBranch1.uuid(), Collections.emptyList(), false);

    // Search on project1Branch1
    assertThat(underTest.create(new SearchRequest()
      .setComponentKeys(singletonList(applicationBranch1.getKey()))
      .setProjectKeys(singletonList(mainBranch1.getKey()))
      .setBranch(branchName1)))
      .extracting(IssueQuery::branchUuid, query -> new ArrayList<>(query.projectUuids()), IssueQuery::isMainBranch)
      .containsOnly(applicationBranch1.uuid(), singletonList(projectData1.projectUuid()), false);
  }

  @Test
  public void fail_if_created_after_and_created_since_are_both_set() {
    SearchRequest request = new SearchRequest()
      .setCreatedAfter("2013-07-25T07:35:00+0100")
      .setCreatedInLast("palap");

    try {
      underTest.create(request);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Parameters createdAfter and createdInLast cannot be set simultaneously");
    }
  }

  @Test
  public void set_created_after_from_created_since() {
    Date now = parseDateTime("2013-07-25T07:35:00+0100");
    when(clock.instant()).thenReturn(now.toInstant());
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    SearchRequest request = new SearchRequest()
      .setCreatedInLast("1y2m3w4d");
    assertThat(underTest.create(request).createdAfter().date()).isEqualTo(parseDateTime("2012-04-30T07:35:00+0100"));
    assertThat(underTest.create(request).createdAfter().inclusive()).isTrue();

  }

  @Test
  public void fail_if_in_new_code_period_and_created_after_set_at_the_same_time() {
    SearchRequest searchRequest = new SearchRequest()
      .setInNewCodePeriod(true)
      .setCreatedAfter("2013-07-25T07:35:00+0100");

    assertThatThrownBy(() -> underTest.create(searchRequest))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Parameters 'createdAfter' and 'inNewCodePeriod' cannot be set simultaneously");
  }

  @Test
  public void fail_if_in_new_code_period_and_created_in_last_set_at_the_same_time() {
    SearchRequest searchRequest = new SearchRequest()
      .setInNewCodePeriod(true)
      .setCreatedInLast("1y2m3w4d");

    assertThatThrownBy(() -> underTest.create(searchRequest))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Parameters 'createdInLast' and 'inNewCodePeriod' cannot be set simultaneously");
  }

  @Test
  public void fail_if_no_component_provided_with_since_leak_period() {
    assertThatThrownBy(() -> underTest.create(new SearchRequest().setInNewCodePeriod(true)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("One and only one component must be provided when searching in new code period");
  }

  @Test
  public void fail_if_no_component_provided_with_in_new_code_period() {
    SearchRequest searchRequest = new SearchRequest().setInNewCodePeriod(true);

    assertThatThrownBy(() -> underTest.create(searchRequest))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("One and only one component must be provided when searching in new code period");
  }

  @Test
  public void fail_if_several_components_provided_with_in_new_code_period() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();

    SearchRequest searchRequest = new SearchRequest()
      .setInNewCodePeriod(true)
      .setComponentKeys(asList(project1.getKey(), project2.getKey()));

    assertThatThrownBy(() -> underTest.create(searchRequest))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("One and only one component must be provided when searching in new code period");
  }

  @Test
  public void fail_if_date_is_not_formatted_correctly() {
    assertThatThrownBy(() -> underTest.create(new SearchRequest()
      .setCreatedAfter("unknown-date")))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("'unknown-date' cannot be parsed as either a date or date+time");
  }

}
