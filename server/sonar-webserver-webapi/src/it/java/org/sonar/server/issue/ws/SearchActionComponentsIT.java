/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.issue.ws;

import java.time.Clock;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.TestMetadataType;
import org.sonar.server.common.avatar.AvatarResolverImpl;
import org.sonar.server.es.EsTester;
import org.sonar.server.feature.JiraSonarQubeFeature;
import org.sonar.server.issue.FromSonarQubeUpdateFeature;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.TaintChecker;
import org.sonar.server.issue.TextRangeResponseFormatter;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.index.IssueQueryComplianceStandardService;
import org.sonar.server.issue.index.IssueQueryFactory;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflow;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowActionsFactory;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowDefinition;
import org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflow;
import org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflowActionsFactory;
import org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflowDefinition;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.view.index.ViewIndexer;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.Issues.Component;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.Issues.SearchWsResponse;
import org.sonarsource.compliancereports.reports.MetadataLoader;
import org.sonarsource.compliancereports.reports.MetadataRules;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.addDays;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.permission.ProjectPermission.USER;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_DIRECTORIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_FILES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_IN_NEW_CODE_PERIOD;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PROJECTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PULL_REQUEST;

class SearchActionComponentsIT {

  @RegisterExtension
  private final UserSessionRule userSession = UserSessionRule.standalone();
  @RegisterExtension
  private final DbTester db = DbTester.create();
  @RegisterExtension
  private final EsTester es = EsTester.create();

  private final Configuration config = mock(Configuration.class);

  private final DbClient dbClient = db.getDbClient();
  private final IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession, new WebAuthorizationTypeSupport(userSession), config);
  private final IssueIndexer issueIndexer = new IssueIndexer(es.client(), dbClient, new IssueIteratorFactory(dbClient), null);
  private final ViewIndexer viewIndexer = new ViewIndexer(dbClient, es.client());
  private final MetadataLoader metadataLoader = new MetadataLoader(Set.of(new TestMetadataType()));
  private final MetadataRules metadataRules = new MetadataRules(metadataLoader);
  private final IssueQueryComplianceStandardService complianceStandardService = new IssueQueryComplianceStandardService(metadataRules,
    db.getDbClient());
  private final IssueQueryFactory issueQueryFactory = new IssueQueryFactory(dbClient, Clock.systemUTC(), userSession, complianceStandardService);
  private final IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  private final IssueWorkflow issueWorkflow = new IssueWorkflow(
    new CodeQualityIssueWorkflow(new CodeQualityIssueWorkflowActionsFactory(issueFieldsSetter), new CodeQualityIssueWorkflowDefinition(), mock(TaintChecker.class)),
    new SecurityHotspotWorkflow(new SecurityHotspotWorkflowActionsFactory(issueFieldsSetter), new SecurityHotspotWorkflowDefinition()));
  private final SearchResponseLoader searchResponseLoader = new SearchResponseLoader(userSession, dbClient, new TransitionService(userSession, issueWorkflow));
  private final Languages languages = new Languages();
  private final UserResponseFormatter userFormatter = new UserResponseFormatter(new AvatarResolverImpl());
  private final SearchResponseFormat searchResponseFormat = new SearchResponseFormat(new Durations(), languages, new TextRangeResponseFormatter(), userFormatter);
  private final PermissionIndexerTester permissionIndexer = new PermissionIndexerTester(es, issueIndexer);

  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker = new IssueIndexSyncProgressChecker(db.getDbClient());
  private final FromSonarQubeUpdateFeature fromSonarQubeUpdateFeature = mock(FromSonarQubeUpdateFeature.class);
  private final JiraSonarQubeFeature jiraSonarQubeFeature = mock(JiraSonarQubeFeature.class);

  {
    when(fromSonarQubeUpdateFeature.isAvailable()).thenReturn(true);
  }

  private final WsActionTester ws = new WsActionTester(
    new SearchAction(
      userSession,
      issueIndex,
      issueQueryFactory,
      issueIndexSyncProgressChecker,
      searchResponseLoader,
      searchResponseFormat,
      System2.INSTANCE,
      dbClient,
      fromSonarQubeUpdateFeature,
      jiraSonarQubeFeature,
      metadataLoader,
      metadataRules
    )
  );

  @Test
  void search_all_issues_when_no_parameter() {
    RuleDto rule = db.rules().insertIssueRule();
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project));
    IssueDto issue = db.issues().insertIssue(rule, project, projectFile);
    allowAnyoneOnProjects(projectData.getProjectDto());
    indexIssues();

    SearchWsResponse result = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issues.Issue::getKey)
      .containsExactlyInAnyOrder(issue.getKey());
  }

  @Test
  void issues_on_different_projects() {
    RuleDto rule = db.rules().insertIssueRule(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    ProjectData projectData1 = db.components().insertPublicProject();
    ComponentDto project = projectData1.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto issue1 = db.issues().insertIssue(rule, project, file);
    ProjectData projectData2 = db.components().insertPublicProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    ComponentDto file2 = db.components().insertComponent(newFileDto(project2));
    IssueDto issue2 = db.issues().insertIssue(rule, project2, file2);
    allowAnyoneOnProjects(projectData1.getProjectDto(), projectData2.getProjectDto());
    indexIssues();

    SearchWsResponse response = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(response.getIssuesList())
      .extracting(Issue::getKey, Issue::getComponent, Issue::getProject)
      .containsExactlyInAnyOrder(
        tuple(issue1.getKey(), file.getKey(), project.getKey()),
        tuple(issue2.getKey(), file2.getKey(), project2.getKey()));
    assertThat(response.getComponentsList())
      .extracting(Component::getKey, Component::getEnabled)
      .containsExactlyInAnyOrder(tuple(project.getKey(), true), tuple(file.getKey(), true), tuple(project2.getKey(), true), tuple(file2.getKey(), true));
  }

  @Test
  void search_since_in_new_code_period_on_project() {
    ProjectData projectData = db.components().insertPublicProject(p -> p.setKey("PK1"));
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setKey("FK1"));
    db.components().insertSnapshot(project, a -> a.setPeriodDate(parseDateTime("2015-09-03T00:00:00+0100").getTime()));
    RuleDto rule = db.rules().insertIssueRule(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    IssueDto issueAfterLeak = db.issues().insertIssue(rule, project, file, i -> i.setKee(UUID_EXAMPLE_01)
      .setIssueCreationDate(parseDateTime("2015-09-04T00:00:00+0100"))
      .setIssueUpdateDate(parseDateTime("2015-10-04T00:00:00+0100")));
    IssueDto issueBeforeLeak = db.issues().insertIssue(rule, project, file, i -> i.setKee(UUID_EXAMPLE_02)
      .setIssueCreationDate(parseDateTime("2014-09-04T00:00:00+0100"))
      .setIssueUpdateDate(parseDateTime("2015-10-04T00:00:00+0100")));
    allowAnyoneOnProjects(projectData.getProjectDto());
    indexIssues();

    ws.newRequest()
      .setParam(PARAM_COMPONENTS, project.getKey())
      .setParam(PARAM_IN_NEW_CODE_PERIOD, "true")
      .execute()
      .assertJson(this.getClass(), "search_since_leak_period.json");
  }

  @Test
  void search_by_file_uuid() {
    ProjectData projectData = db.components().insertPublicProject(p -> p.setKey("PK1"));
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setKey("FK1"));
    RuleDto rule = db.rules().insertIssueRule(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    IssueDto issue = db.issues().insertIssue(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    allowAnyoneOnProjects(projectData.getProjectDto());
    indexIssues();

    ws.newRequest()
      .setParam(PARAM_FILES, file.path())
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json");

    ws.newRequest()
      .setParam(PARAM_FILES, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  void search_by_file_key() {
    ProjectData projectData = db.components().insertPublicProject(p -> p.setKey("PK1"));
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setKey("FK1"));
    ComponentDto unitTest = db.components().insertComponent(newFileDto(project, null, "F2").setQualifier(ComponentQualifiers.UNIT_TEST_FILE).setKey("FK2"));
    RuleDto rule = db.rules().insertIssueRule(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    IssueDto issueOnFile = db.issues().insertIssue(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    IssueDto issueOnTest = db.issues().insertIssue(rule, project, unitTest, i -> i.setKee("2bd4eac2-b650-4037-80bc-7b1182fd47d4"));
    allowAnyoneOnProjects(projectData.getProjectDto());
    indexIssues();

    ws.newRequest()
      .setParam(PARAM_COMPONENTS, file.getKey())
      .execute()
      .assertJson(this.getClass(), "search_by_file_key.json");

    ws.newRequest()
      .setParam(PARAM_COMPONENTS, unitTest.getKey())
      .execute()
      .assertJson(this.getClass(), "search_by_test_key.json");
  }

  @Test
  void search_by_directory_path() {
    ProjectData projectData = db.components().insertPublicProject(p -> p.setKey("PK1"));
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "D1", "src/main/java/dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setKey("FK1").setPath(directory.path() + "/MyComponent.java"));
    RuleDto rule = db.rules().insertIssueRule(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insertIssue(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    allowAnyoneOnProjects(projectData.getProjectDto());
    indexIssues();

    ws.newRequest()
      .setParam(PARAM_COMPONENTS, directory.getKey())
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json");

    ws.newRequest()
      .setParam(PARAM_DIRECTORIES, "unknown")
      .execute()
      .assertJson(this.getClass(), "no_issue.json");

    ws.newRequest()
      .setParam(PARAM_DIRECTORIES, "src/main/java/dir")
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json");

    ws.newRequest()
      .setParam(PARAM_DIRECTORIES, "src/main/java")
      .execute()
      .assertJson(this.getClass(), "search_by_file_uuid.json");
  }

  @Test
  void search_by_view_uuid() {
    ProjectData projectData = db.components().insertPublicProject(p -> p.setKey("PK1"));
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setKey("FK1"));
    ComponentDto view = db.components().insertComponent(ComponentTesting.newPortfolio("V1").setKey("MyView"));
    db.components().insertComponent(newProjectCopy(project, view));
    RuleDto rule = db.rules().insertIssueRule(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insertIssue(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    allowAnyoneOnProjects(projectData.getProjectDto());
    allowAnyoneOnPortfolios(view);

    indexIssuesAndViews();

    ws.newRequest()
      .setParam(PARAM_COMPONENTS, view.getKey())
      .execute()
      .assertJson(this.getClass(), "search_by_view_uuid.json");
  }

  @Test
  void search_by_sub_view_uuid() {
    ProjectData projectData = db.components().insertPublicProject(p -> p.setKey("PK1"));
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setKey("FK1"));
    RuleDto rule = db.rules().insertIssueRule(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insertIssue(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    ComponentDto view = db.components().insertComponent(ComponentTesting.newPortfolio("V1").setKey("MyView"));
    ComponentDto subView = db.components().insertComponent(ComponentTesting.newSubPortfolio(view, "SV1", "MySubView"));
    db.components().insertComponent(newProjectCopy(project, subView));
    allowAnyoneOnProjects(projectData.getProjectDto());
    allowAnyoneOnPortfolios(view);
    indexIssuesAndViews();

    ws.newRequest()
      .setParam(PARAM_COMPONENTS, subView.getKey())
      .execute()
      .assertJson(this.getClass(), "search_by_view_uuid.json");
  }

  @Test
  void search_by_sub_view_uuid_return_only_authorized_view() {
    ProjectData projectData = db.components().insertPublicProject(p -> p.setKey("PK1"));
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project, null, "F1").setKey("FK1"));
    RuleDto rule = db.rules().insertIssueRule(r -> r.setRuleKey(RuleKey.of("xoo", "x1")));
    db.issues().insertIssue(rule, project, file, i -> i.setKee("82fd47d4-b650-4037-80bc-7b112bd4eac2"));
    ComponentDto view = db.components().insertComponent(ComponentTesting.newPortfolio("V1").setKey("MyView"));
    ComponentDto subView = db.components().insertComponent(ComponentTesting.newSubPortfolio(view, "SV1", "MySubView"));
    db.components().insertComponent(newProjectCopy(project, subView));
    // User has no permission on the view, no issue will be returned
    allowAnyoneOnProjects(projectData.getProjectDto());
    indexIssuesAndViews();

    ws.newRequest()
      .setParam(PARAM_COMPONENTS, subView.getKey())
      .execute()
      .assertJson(this.getClass(), "no_issue.json");
  }

  @Test
  void search_by_application_key() {
    ProjectData applicationData = db.components().insertPrivateApplication();
    ComponentDto application = applicationData.getMainBranchComponent();
    ProjectData projectData1 = db.components().insertPrivateProject();
    ComponentDto project1 = projectData1.getMainBranchComponent();
    ProjectData projectData2 = db.components().insertPrivateProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    db.components().insertComponents(newProjectCopy(project1, application));
    db.components().insertComponents(newProjectCopy(project2, application));
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue1 = db.issues().insertIssue(rule, project1, project1);
    IssueDto issue2 = db.issues().insertIssue(rule, project2, project2);
    allowAnyoneOnApplication(applicationData.getProjectDto(), projectData1.getProjectDto(), projectData2.getProjectDto());
    userSession.addProjectPermission(USER, applicationData.getProjectDto());
    userSession.addProjectBranchMapping(applicationData.projectUuid(), application);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENTS, application.getKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issue1.getKey(), issue2.getKey());
  }

  @Test
  void search_by_application_key_and_branch() {
    ProjectData applicatioData = db.components().insertPrivateProject(c -> c.setQualifier(APP).setKey("app"));
    ComponentDto application = applicatioData.getMainBranchComponent();
    String appBranch1 = "app-branch1";
    String appBranch2 = "app-branch2";
    String proj1branch1 = "proj1branch1";
    String proj1branch2 = "proj1branch2";
    ComponentDto applicationBranch1 = db.components().insertProjectBranch(application, a -> a.setKey(appBranch1));
    ComponentDto applicationBranch2 = db.components().insertProjectBranch(application, a -> a.setKey(appBranch2));
    ProjectData projectData1 = db.components().insertPrivateProject(p -> p.setKey("prj1"));
    ComponentDto project1 = projectData1.getMainBranchComponent();
    ComponentDto project1Branch1 = db.components().insertProjectBranch(project1, b -> b.setKey(proj1branch1));
    ComponentDto fileOnProject1Branch1 = db.components().insertComponent(newFileDto(project1Branch1, project1.uuid()));
    ComponentDto project1Branch2 = db.components().insertProjectBranch(project1, b -> b.setKey(proj1branch2));
    ProjectData projectData2 = db.components().insertPrivateProject(p -> p.setKey("prj2"));
    ComponentDto project2 = projectData2.getMainBranchComponent();
    db.components().insertComponents(newProjectCopy(project1Branch1, applicationBranch1));
    db.components().insertComponents(newProjectCopy(project2, applicationBranch1));
    db.components().insertComponents(newProjectCopy(project1Branch2, applicationBranch2));

    RuleDto issueRule = db.rules().insertIssueRule();
    RuleDto hotspotRule = db.rules().insertHotspotRule();
    IssueDto issueOnProject1 = db.issues().insertIssue(issueRule, project1, project1);
    IssueDto issueOnProject1Branch1 = db.issues().insertIssue(issueRule, project1Branch1, project1Branch1);
    db.issues().insertHotspot(hotspotRule, project1Branch1, project1Branch1);
    IssueDto issueOnFileOnProject1Branch1 = db.issues().insertIssue(issueRule, project1Branch1, fileOnProject1Branch1);
    IssueDto issueOnProject1Branch2 = db.issues().insertIssue(issueRule, project1Branch2, project1Branch2);
    IssueDto issueOnProject2 = db.issues().insertIssue(issueRule, project2, project2);
    db.issues().insertHotspot(hotspotRule, project2, project2);
    allowAnyoneOnProjects(projectData1.getProjectDto(), projectData2.getProjectDto(), applicatioData.getProjectDto());
    userSession.addProjectPermission(USER, applicatioData.getProjectDto());
    indexIssuesAndViews();

    // All issues on applicationBranch1
    assertThat(ws.newRequest()
      .setParam(PARAM_COMPONENTS, applicationBranch1.getKey())
      .setParam(PARAM_BRANCH, appBranch1)
      .executeProtobuf(SearchWsResponse.class).getIssuesList())
        .extracting(Issue::getKey, Issue::getComponent, Issue::getProject, Issue::getBranch, Issue::hasBranch)
        .containsExactlyInAnyOrder(
          tuple(issueOnProject1Branch1.getKey(), project1Branch1.getKey(), project1Branch1.getKey(), proj1branch1, true),
          tuple(issueOnFileOnProject1Branch1.getKey(), fileOnProject1Branch1.getKey(), project1Branch1.getKey(), proj1branch1, true),
          tuple(issueOnProject2.getKey(), project2.getKey(), project2.getKey(), "", false));

    // Issues on project1Branch1
    assertThat(ws.newRequest()
      .setParam(PARAM_COMPONENTS, applicationBranch1.getKey())
      .setParam(PARAM_PROJECTS, project1.getKey())
      .setParam(PARAM_BRANCH, appBranch1)
      .executeProtobuf(SearchWsResponse.class).getIssuesList())
        .extracting(Issue::getKey, Issue::getComponent, Issue::getBranch)
        .containsExactlyInAnyOrder(
          tuple(issueOnProject1Branch1.getKey(), project1Branch1.getKey(), proj1branch1),
          tuple(issueOnFileOnProject1Branch1.getKey(), fileOnProject1Branch1.getKey(), proj1branch1));
  }

  @Test
  void ignore_application_without_browse_permission() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto application = db.components().insertPublicApplication().getMainBranchComponent();
    db.components().insertComponents(newProjectCopy("PC1", project, application));
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project, project);
    allowAnyoneOnProjects(projectData.getProjectDto());
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENTS, application.getKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).isEmpty();
  }

  @Test
  void search_application_without_projects() {
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ProjectData applicationData = db.components().insertPublicApplication();
    ComponentDto application = applicationData.getMainBranchComponent();
    RuleDto rule = db.rules().insertIssueRule();
    db.issues().insertIssue(rule, project, project);
    allowAnyoneOnProjects(projectData.getProjectDto(), applicationData.getProjectDto());
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENTS, application.getKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).isEmpty();
  }

  @Test
  void search_by_application_and_by_new_code_period() {
    Date now = new Date();
    RuleDto rule = db.rules().insertIssueRule();
    ProjectData applicationData = db.components().insertPublicApplication();
    ComponentDto application = applicationData.getMainBranchComponent();
    // Project 1
    ProjectData projectData1 = db.components().insertPublicProject();
    ComponentDto project1 = projectData1.getMainBranchComponent();
    db.components().insertSnapshot(project1, s -> s.setPeriodDate(addDays(now, -14).getTime()));
    db.components().insertComponents(newProjectCopy("PC1", project1, application));
    IssueDto project1Issue1 = db.issues().insertIssue(rule, project1, project1, i -> i.setIssueCreationDate(addDays(now, -10)));
    IssueDto project1Issue2 = db.issues().insertIssue(rule, project1, project1, i -> i.setIssueCreationDate(addDays(now, -20)));
    // Project 2
    ProjectData projectData2 = db.components().insertPublicProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    db.components().insertSnapshot(project2, s -> s.setPeriodDate(addDays(now, -25).getTime()));
    db.components().insertComponents(newProjectCopy("PC2", project2, application));
    IssueDto project2Issue1 = db.issues().insertIssue(rule, project2, project2, i -> i.setIssueCreationDate(addDays(now, -15)));
    IssueDto project2Issue2 = db.issues().insertIssue(rule, project2, project2, i -> i.setIssueCreationDate(addDays(now, -30)));
    // Permissions and index
    allowAnyoneOnApplication(applicationData.getProjectDto(), projectData1.getProjectDto(), projectData2.getProjectDto());
    userSession.addProjectBranchMapping(applicationData.projectUuid(), application);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENTS, application.getKey())
      .setParam(PARAM_IN_NEW_CODE_PERIOD, "true")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issue::getKey)
      .containsExactlyInAnyOrder(project1Issue1.getKey(), project2Issue1.getKey())
      .doesNotContain(project1Issue2.getKey(), project2Issue2.getKey());
  }

  @Test
  void search_by_application_and_project() {
    ProjectData projectData1 = db.components().insertPublicProject();
    ComponentDto project1 = projectData1.getMainBranchComponent();
    ProjectData projectData2 = db.components().insertPublicProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    ProjectData applicationData = db.components().insertPublicApplication();
    ComponentDto application = applicationData.getMainBranchComponent();
    db.components().insertComponents(newProjectCopy("PC1", project1, application));
    db.components().insertComponents(newProjectCopy("PC2", project2, application));
    RuleDto rule = db.rules().insertIssueRule();
    IssueDto issue1 = db.issues().insertIssue(rule, project1, project1);
    IssueDto issue2 = db.issues().insertIssue(rule, project2, project2);
    allowAnyoneOnApplication(applicationData.getProjectDto(), projectData1.getProjectDto(), projectData2.getProjectDto());
    userSession.addProjectBranchMapping(applicationData.getProjectDto().getUuid(), application);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENTS, application.getKey())
      .setParam(PARAM_PROJECTS, project1.getKey())
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issue::getKey)
      .containsExactlyInAnyOrder(issue1.getKey())
      .doesNotContain(issue2.getKey());
  }

  @Test
  void search_by_application_and_project_and_new_code_period() {
    Date now = new Date();
    RuleDto rule = db.rules().insertIssueRule();
    ProjectData applicationData = db.components().insertPublicApplication();
    ComponentDto application = applicationData.getMainBranchComponent();
    // Project 1
    ProjectData projectData1 = db.components().insertPublicProject();
    ComponentDto project1 = projectData1.getMainBranchComponent();
    db.components().insertSnapshot(project1, s -> s.setPeriodDate(addDays(now, -14).getTime()));
    db.components().insertComponents(newProjectCopy("PC1", project1, application));
    IssueDto project1Issue1 = db.issues().insertIssue(rule, project1, project1, i -> i.setIssueCreationDate(addDays(now, -10)));
    IssueDto project1Issue2 = db.issues().insertIssue(rule, project1, project1, i -> i.setIssueCreationDate(addDays(now, -20)));
    // Project 2
    ProjectData projectData2 = db.components().insertPublicProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    db.components().insertSnapshot(project2, s -> s.setPeriodDate(addDays(now, -25).getTime()));
    db.components().insertComponents(newProjectCopy("PC2", project2, application));
    IssueDto project2Issue1 = db.issues().insertIssue(rule, project2, project2, i -> i.setIssueCreationDate(addDays(now, -15)));
    IssueDto project2Issue2 = db.issues().insertIssue(rule, project2, project2, i -> i.setIssueCreationDate(addDays(now, -30)));
    // Permissions and index
    allowAnyoneOnApplication(applicationData.getProjectDto(), projectData1.getProjectDto(), projectData2.getProjectDto());
    userSession.addProjectBranchMapping(applicationData.projectUuid(), application);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENTS, application.getKey())
      .setParam(PARAM_PROJECTS, project1.getKey())
      .setParam(PARAM_IN_NEW_CODE_PERIOD, "true")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issue::getKey)
      .containsExactlyInAnyOrder(project1Issue1.getKey())
      .doesNotContain(project1Issue2.getKey(), project2Issue1.getKey(), project2Issue2.getKey());
  }

  @Test
  void search_by_application_and_by_new_code_period_when_one_project_has_no_leak() {
    Date now = new Date();
    RuleDto rule = db.rules().insertIssueRule();
    ProjectData applicationData = db.components().insertPublicApplication();
    ComponentDto application = applicationData.getMainBranchComponent();
    // Project 1
    ProjectData projectData1 = db.components().insertPublicProject();
    ComponentDto project1 = projectData1.getMainBranchComponent();
    db.components().insertSnapshot(project1, s -> s.setPeriodDate(addDays(now, -14).getTime()));
    db.components().insertComponents(newProjectCopy("PC1", project1, application));
    IssueDto project1Issue1 = db.issues().insertIssue(rule, project1, project1, i -> i.setIssueCreationDate(addDays(now, -10)));
    IssueDto project1Issue2 = db.issues().insertIssue(rule, project1, project1, i -> i.setIssueCreationDate(addDays(now, -20)));
    // Project 2, without leak => no issue form it should be returned
    ProjectData projectData2 = db.components().insertPublicProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    db.components().insertSnapshot(project2, s -> s.setPeriodDate(null));
    db.components().insertComponents(newProjectCopy("PC2", project2, application));
    IssueDto project2Issue1 = db.issues().insertIssue(rule, project2, project2, i -> i.setIssueCreationDate(addDays(now, -15)));
    IssueDto project2Issue2 = db.issues().insertIssue(rule, project2, project2, i -> i.setIssueCreationDate(addDays(now, -30)));
    // Permissions and index
    allowAnyoneOnApplication(applicationData.getProjectDto(), projectData1.getProjectDto(), projectData2.getProjectDto());
    userSession.addProjectBranchMapping(applicationData.projectUuid(), application);
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENTS, application.getKey())
      .setParam(PARAM_IN_NEW_CODE_PERIOD, "true")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issue::getKey)
      .containsExactlyInAnyOrder(project1Issue1.getKey())
      .doesNotContain(project1Issue2.getKey(), project2Issue1.getKey(), project2Issue2.getKey());
  }

  @Test
  void search_by_branch() {
    RuleDto rule = db.rules().insertIssueRule();
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    IssueDto issue = db.issues().insertIssue(rule, project, file);

    String branchName = secure().nextAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH).setKey(branchName));
    ComponentDto branchFile = db.components().insertComponent(newFileDto(branch, project.uuid()));
    IssueDto branchIssue = db.issues().insertIssue(rule, branch, branchFile);
    allowAnyoneOnProjects(projectData.getProjectDto());
    indexIssuesAndViews();

    // On component key + branch
    assertThat(ws.newRequest()
      .setParam(PARAM_COMPONENTS, project.getKey())
      .setParam(PARAM_BRANCH, branchName)
      .executeProtobuf(SearchWsResponse.class).getIssuesList())
        .extracting(Issue::getKey, Issue::getComponent, Issue::getBranch)
        .containsExactlyInAnyOrder(tuple(branchIssue.getKey(), branchFile.getKey(), branchName));

    // On project key + branch
    assertThat(ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_BRANCH, branchName)
      .executeProtobuf(SearchWsResponse.class).getIssuesList())
        .extracting(Issue::getKey, Issue::getComponent, Issue::getBranch)
        .containsExactlyInAnyOrder(tuple(branchIssue.getKey(), branchFile.getKey(), branchName));
    // On file key + branch
    assertThat(ws.newRequest()
      .setParam(PARAM_COMPONENTS, branchFile.getKey())
      .setParam(PARAM_BRANCH, branchName)
      .executeProtobuf(SearchWsResponse.class).getIssuesList())
        .extracting(Issue::getKey, Issue::getComponent, Issue::getBranch)
        .containsExactlyInAnyOrder(tuple(branchIssue.getKey(), branchFile.getKey(), branchName));
  }

  @Test
  void return_branch_in_component_list() {
    RuleDto rule = db.rules().insertIssueRule();
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project));
    IssueDto projectIssue = db.issues().insertIssue(rule, project, projectFile);
    String branchName = secure().nextAlphanumeric(248);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setBranchType(BRANCH).setKey(branchName));
    ComponentDto branchFile = db.components().insertComponent(newFileDto(branch, project.uuid()));
    IssueDto branchIssue = db.issues().insertIssue(rule, branch, branchFile);
    allowAnyoneOnProjects(projectData.getProjectDto());
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENTS, branch.getKey())
      .setParam(PARAM_BRANCH, branchName)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getComponentsList())
      .extracting(Issues.Component::getKey, Issues.Component::getBranch)
      .containsExactlyInAnyOrder(
        tuple(branchFile.getKey(), branchName),
        tuple(branch.getKey(), branchName));
  }

  @Test
  void search_by_pull_request() {
    RuleDto rule = db.rules().insertIssueRule();
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project));
    IssueDto projectIssue = db.issues().insertIssue(rule, project, projectFile);

    String pullRequestKey = secure().nextAlphanumeric(100);
    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> b.setBranchType(PULL_REQUEST).setKey(pullRequestKey));
    ComponentDto pullRequestFile = db.components().insertComponent(newFileDto(pullRequest, project.uuid()));
    IssueDto pullRequestIssue = db.issues().insertIssue(rule, pullRequest, pullRequestFile);
    allowAnyoneOnProjects(projectData.getProjectDto());
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENTS, pullRequest.getKey())
      .setParam(PARAM_PULL_REQUEST, pullRequestKey)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey, Issue::getComponent, Issue::getPullRequest)
      .containsExactlyInAnyOrder(tuple(pullRequestIssue.getKey(), pullRequestFile.getKey(), pullRequestKey));
    assertThat(result.getComponentsList())
      .extracting(Issues.Component::getKey, Issues.Component::getPullRequest)
      .containsExactlyInAnyOrder(
        tuple(pullRequestFile.getKey(), pullRequestKey),
        tuple(pullRequest.getKey(), pullRequestKey));
  }

  @Test
  void search_using_main_branch_name() {
    RuleDto rule = db.rules().insertIssueRule();
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project));
    IssueDto projectIssue = db.issues().insertIssue(rule, project, projectFile);
    allowAnyoneOnProjects(projectData.getProjectDto());
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest()
      .setParam(PARAM_COMPONENTS, project.getKey())
      .setParam(PARAM_BRANCH, DEFAULT_MAIN_BRANCH_NAME)
      .executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList())
      .extracting(Issue::getKey, Issue::getComponent, Issue::hasBranch)
      .containsExactlyInAnyOrder(tuple(projectIssue.getKey(), projectFile.getKey(), false));
    assertThat(result.getComponentsList())
      .extracting(Issues.Component::getKey, Issues.Component::hasBranch)
      .containsExactlyInAnyOrder(
        tuple(projectFile.getKey(), false),
        tuple(project.getKey(), false));
  }

  @Test
  void does_not_return_branch_issues_on_not_contextualized_search() {
    RuleDto rule = db.rules().insertIssueRule();
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto project = projectData.getMainBranchComponent();
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project));
    IssueDto projectIssue = db.issues().insertIssue(rule, project, projectFile);
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto branchFile = db.components().insertComponent(newFileDto(branch, project.uuid()));
    IssueDto branchIssue = db.issues().insertIssue(rule, branch, branchFile);
    allowAnyoneOnProjects(projectData.getProjectDto());
    indexIssuesAndViews();

    SearchWsResponse result = ws.newRequest().executeProtobuf(SearchWsResponse.class);

    assertThat(result.getIssuesList()).extracting(Issue::getKey)
      .containsExactlyInAnyOrder(projectIssue.getKey())
      .doesNotContain(branchIssue.getKey());
  }

  private void allowAnyoneOnProjects(ProjectDto... projects) {
    userSession.registerProjects(projects);
    Arrays.stream(projects).forEach(permissionIndexer::allowOnlyAnyone);
  }

  private void allowAnyoneOnPortfolios(ComponentDto... portfolios) {
    userSession.registerPortfolios(portfolios);
    Arrays.stream(portfolios).forEach(permissionIndexer::allowOnlyAnyone);
  }

  private void allowAnyoneOnApplication(ProjectDto application, ProjectDto... projects) {
    userSession.registerApplication(application);
    Arrays.stream(projects).forEach(permissionIndexer::allowOnlyAnyone);
  }

  private void indexIssues() {
    issueIndexer.indexAllIssues();
  }

  private void indexIssuesAndViews() {
    indexIssues();
    viewIndexer.indexAll();
  }
}
