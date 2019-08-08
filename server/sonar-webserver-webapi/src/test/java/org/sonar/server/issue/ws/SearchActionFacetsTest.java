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
package org.sonar.server.issue.ws;

import com.google.common.collect.ImmutableMap;
import java.time.Clock;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.Languages;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Durations;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.StartupIndexer;
import org.sonar.server.issue.AvatarResolverImpl;
import org.sonar.server.issue.TransitionService;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.issue.index.IssueQueryFactory;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Common.FacetValue;
import org.sonarqube.ws.Issues.SearchWsResponse;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sonar.api.server.ws.WebService.Param.FACETS;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_MODE_EFFORT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_FILE_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_MODULE_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PROJECTS;

public class SearchActionFacetsTest {

  @Rule
  public UserSessionRule userSession = standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private IssueIndex issueIndex = new IssueIndex(es.client(), System2.INSTANCE, userSession, new WebAuthorizationTypeSupport(userSession));
  private IssueIndexer issueIndexer = new IssueIndexer(es.client(), db.getDbClient(), new IssueIteratorFactory(db.getDbClient()));
  private StartupIndexer permissionIndexer = new PermissionIndexer(db.getDbClient(), es.client(), issueIndexer);
  private IssueQueryFactory issueQueryFactory = new IssueQueryFactory(db.getDbClient(), Clock.systemUTC(), userSession);
  private SearchResponseLoader searchResponseLoader = new SearchResponseLoader(userSession, db.getDbClient(), new TransitionService(userSession, null));
  private Languages languages = new Languages();
  private SearchResponseFormat searchResponseFormat = new SearchResponseFormat(new Durations(), languages, new AvatarResolverImpl());

  private WsActionTester ws = new WsActionTester(
    new SearchAction(userSession, issueIndex, issueQueryFactory, searchResponseLoader, searchResponseFormat,
      new MapSettings().asConfig(), System2.INSTANCE, db.getDbClient()));

  @Test
  public void display_all_facets() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto file = db.components().insertComponent(newFileDto(module));
    RuleDefinitionDto rule = db.rules().insert();
    UserDto user = db.users().insertUser();
    db.issues().insert(rule, project, file, i -> i
      .setSeverity("MAJOR")
      .setStatus("OPEN")
      .setType(RuleType.CODE_SMELL)
      .setEffort(10L)
      .setAssigneeUuid(user.getUuid()));
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, project.getKey())
      .setParam(FACETS, "severities,statuses,resolutions,rules,types,languages,projects,moduleUuids,fileUuids,assignees")
      .executeProtobuf(SearchWsResponse.class);

    Map<String, Number> expectedStatuses = ImmutableMap.<String, Number>builder().put("OPEN", 1L).put("CONFIRMED", 0L)
      .put("REOPENED", 0L).put("RESOLVED", 0L).put("CLOSED", 0L).put("IN_REVIEW", 0L).put("TO_REVIEW", 0L).put("REVIEWED", 0L).build();

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(
        tuple("severities", of("INFO", 0L, "MINOR", 0L, "MAJOR", 1L, "CRITICAL", 0L, "BLOCKER", 0L)),
        tuple("statuses", expectedStatuses),
        tuple("resolutions", of("", 1L, "FALSE-POSITIVE", 0L, "FIXED", 0L, "REMOVED", 0L, "WONTFIX", 0L)),
        tuple("rules", of(rule.getKey().toString(), 1L)),
        tuple("types", of("CODE_SMELL", 1L, "BUG", 0L, "VULNERABILITY", 0L, "SECURITY_HOTSPOT", 0L)),
        tuple("languages", of(rule.getLanguage(), 1L)),
        tuple("projects", of(project.getKey(), 1L)),
        tuple("moduleUuids", of(module.uuid(), 1L)),
        tuple("fileUuids", of(file.uuid(), 1L)),
        tuple("assignees", of("", 0L, user.getLogin(), 1L)));
  }

  @Test
  public void display_facets_in_effort_mode() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, file, i -> i
      .setSeverity("MAJOR")
      .setStatus("OPEN")
      .setType(RuleType.CODE_SMELL)
      .setEffort(10L)
      .setAssigneeUuid(null));
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, project.getKey())
      .setParam(FACETS, "severities,statuses,resolutions,rules,types,languages,projects,fileUuids,assignees")
      .setParam("facetMode", FACET_MODE_EFFORT)
      .executeProtobuf(SearchWsResponse.class);

    Map<String, Number> expectedStatuses = ImmutableMap.<String, Number>builder().put("OPEN", 10L).put("CONFIRMED", 0L)
      .put("REOPENED", 0L).put("RESOLVED", 0L).put("CLOSED", 0L).put("IN_REVIEW", 0L).put("TO_REVIEW", 0L).put("REVIEWED", 0L).build();

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(
        tuple("severities", of("INFO", 0L, "MINOR", 0L, "MAJOR", 10L, "CRITICAL", 0L, "BLOCKER", 0L)),
        tuple("statuses", expectedStatuses),
        tuple("resolutions", of("", 10L, "FALSE-POSITIVE", 0L, "FIXED", 0L, "REMOVED", 0L, "WONTFIX", 0L)),
        tuple("rules", of(rule.getKey().toString(), 10L)),
        tuple("types", of("CODE_SMELL", 10L, "BUG", 0L, "VULNERABILITY", 0L, "SECURITY_HOTSPOT", 0L)),
        tuple("languages", of(rule.getLanguage(), 10L)),
        tuple("projects", of(project.getKey(), 10L)),
        tuple("fileUuids", of(file.uuid(), 10L)),
        tuple("assignees", of("", 10L)));
  }

  @Test
  public void display_projects_facet() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, file);
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(WebService.Param.FACETS, "projects")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(tuple("projects", of(project.getKey(), 1L)));
  }

  @Test
  public void projects_facet_is_sticky() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    OrganizationDto organization3 = db.organizations().insert();
    ComponentDto project1 = db.components().insertPublicProject(organization1);
    ComponentDto project2 = db.components().insertPublicProject(organization2);
    ComponentDto project3 = db.components().insertPublicProject(organization3);
    ComponentDto file1 = db.components().insertComponent(newFileDto(project1));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project2));
    ComponentDto file3 = db.components().insertComponent(newFileDto(project3));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project1, file1);
    db.issues().insert(rule, project2, file2);
    db.issues().insert(rule, project3, file3);
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_PROJECTS, project1.getKey())
      .setParam(WebService.Param.FACETS, "projects")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(tuple("projects", of(project1.getKey(), 1L, project2.getKey(), 1L, project3.getKey(), 1L)));
  }

  @Test
  public void display_moduleUuids_facet_using_project() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto subModule1 = db.components().insertComponent(newModuleDto(module));
    ComponentDto subModule2 = db.components().insertComponent(newModuleDto(module));
    ComponentDto subModule3 = db.components().insertComponent(newModuleDto(module));
    ComponentDto file1 = db.components().insertComponent(newFileDto(subModule1));
    ComponentDto file2 = db.components().insertComponent(newFileDto(subModule2));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, file1);
    db.issues().insert(rule, project, file2);
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_PROJECTS, project.getKey())
      .setParam(PARAM_COMPONENT_UUIDS, module.uuid())
      .setParam(PARAM_MODULE_UUIDS, subModule1.uuid())
      .setParam(WebService.Param.FACETS, "moduleUuids")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(tuple("moduleUuids", of(subModule1.uuid(), 1L, subModule2.uuid(), 1L)));
  }

  @Test
  public void display_module_facet_using_organization() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto subModule1 = db.components().insertComponent(newModuleDto(module));
    ComponentDto subModule2 = db.components().insertComponent(newModuleDto(module));
    ComponentDto subModule3 = db.components().insertComponent(newModuleDto(module));
    ComponentDto file1 = db.components().insertComponent(newFileDto(subModule1));
    ComponentDto file2 = db.components().insertComponent(newFileDto(subModule2));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, file1);
    db.issues().insert(rule, project, file2);
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_COMPONENT_UUIDS, module.uuid())
      .setParam(PARAM_MODULE_UUIDS, subModule1.uuid() + "," + subModule2.uuid())
      .setParam(WebService.Param.FACETS, "moduleUuids")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(tuple("moduleUuids", of(subModule1.uuid(), 1L, subModule2.uuid(), 1L)));
  }

  @Test
  public void fail_to_display_module_facet_when_no_organization_or_project_is_set() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto file = db.components().insertComponent(newFileDto(module, null));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, file);
    indexPermissions();
    indexIssues();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Facet(s) 'moduleUuids' require to also filter by project or organization");

    ws.newRequest()
      .setParam(PARAM_COMPONENT_UUIDS, module.uuid())
      .setParam(WebService.Param.FACETS, "moduleUuids")
      .execute();
  }

  @Test
  public void display_directory_facet_using_project() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "src/main/java/dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, directory));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, file);
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam("resolved", "false")
      .setParam(PARAM_COMPONENT_KEYS, project.getKey())
      .setParam(WebService.Param.FACETS, "directories")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(tuple("directories", of(directory.path(), 1L)));
  }

  @Test
  public void display_directory_facet_using_organization() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "src/main/java/dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, directory));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, file);
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam("resolved", "false")
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(WebService.Param.FACETS, "directories")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(tuple("directories", of(directory.path(), 1L)));
  }

  @Test
  public void fail_to_display_directory_facet_when_no_organization_or_project_is_set() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto directory = db.components().insertComponent(newDirectory(project, "src"));
    ComponentDto file = db.components().insertComponent(newFileDto(project, directory));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, file);
    indexPermissions();
    indexIssues();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Facet(s) 'directories' require to also filter by project or organization");

    ws.newRequest()
      .setParam(WebService.Param.FACETS, "directories")
      .execute();
  }

  @Test
  public void display_fileUuids_facet_with_project() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    ComponentDto file1 = db.components().insertComponent(newFileDto(project));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project));
    ComponentDto file3 = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, file1);
    db.issues().insert(rule, project, file2);
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, project.getKey())
      .setParam(PARAM_FILE_UUIDS, file1.uuid())
      .setParam(WebService.Param.FACETS, "fileUuids")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(tuple("fileUuids", of(file1.uuid(), 1L, file2.uuid(), 1L)));
  }

  @Test
  public void display_fileUuids_facet_with_organization() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    ComponentDto file1 = db.components().insertComponent(newFileDto(project));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project));
    ComponentDto file3 = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, file1);
    db.issues().insert(rule, project, file2);
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_FILE_UUIDS, file1.uuid())
      .setParam(WebService.Param.FACETS, "fileUuids")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(tuple("fileUuids", of(file1.uuid(), 1L, file2.uuid(), 1L)));
  }

  @Test
  public void fail_to_display_fileUuids_facet_when_no_organization_or_project_is_set() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    db.issues().insert(rule, project, file);
    indexPermissions();
    indexIssues();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Facet(s) 'fileUuids' require to also filter by project or organization");

    ws.newRequest()
      .setParam(PARAM_FILE_UUIDS, file.uuid())
      .setParam(WebService.Param.FACETS, "fileUuids")
      .execute();
  }

  @Test
  public void check_facets_max_size() {
    ComponentDto project = db.components().insertPublicProject();
    IntStream.rangeClosed(1, 110)
      .forEach(index -> {
        RuleDefinitionDto rule = db.rules().insert();
        UserDto user = db.users().insertUser();
        ComponentDto module = db.components().insertComponent(newModuleDto(project));
        ComponentDto directory = db.components().insertComponent(newDirectory(module, "dir" + index));
        ComponentDto file = db.components().insertComponent(newFileDto(directory));
        db.issues().insert(rule, project, file, i -> i.setAssigneeUuid(user.getUuid()));
      });
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT_KEYS, project.getKey())
      .setParam(FACETS, "fileUuids,directories,moduleUuids,statuses,resolutions,severities,types,rules,languages,assignees")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, Common.Facet::getValuesCount)
      .containsExactlyInAnyOrder(
        tuple("fileUuids", 100),
        tuple("directories", 100),
        tuple("moduleUuids", 100),
        tuple("rules", 100),
        tuple("languages", 100),
        // Assignees contains one additional element : it's the empty string that will return number of unassigned issues
        tuple("assignees", 101),
        // Following facets returned fixed number of elements
        tuple("statuses", 8),
        tuple("resolutions", 5),
        tuple("severities", 5),
        tuple("types", 4));
  }

  @Test
  public void check_projects_facet_max_size() {
    RuleDefinitionDto rule = db.rules().insert();
    IntStream.rangeClosed(1, 110)
      .forEach(i -> {
        ComponentDto project = db.components().insertPublicProject();
        db.issues().insert(rule, project, project);
      });
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(FACETS, "projects")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getPaging().getTotal()).isEqualTo(110);
    assertThat(response.getFacets().getFacets(0).getValuesCount()).isEqualTo(100);
  }

  @Test
  public void display_zero_valued_facets_for_selected_items_having_no_issue() {
    ComponentDto project1 = db.components().insertPublicProject();
    ComponentDto module1 = db.components().insertComponent(newModuleDto(project1));
    ComponentDto module2 = db.components().insertComponent(newModuleDto(project1));
    ComponentDto project2 = db.components().insertPublicProject();
    ComponentDto file1 = db.components().insertComponent(newFileDto(module1));
    ComponentDto file2 = db.components().insertComponent(newFileDto(module1));
    RuleDefinitionDto rule1 = db.rules().insert();
    RuleDefinitionDto rule2 = db.rules().insert();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.issues().insert(rule1, project1, file1, i -> i
      .setSeverity("MAJOR")
      .setStatus("OPEN")
      .setResolution(null)
      .setType(RuleType.CODE_SMELL)
      .setEffort(10L)
      .setAssigneeUuid(user1.getUuid()));
    indexPermissions();
    indexIssues();

    SearchWsResponse response = ws.newRequest()
      .setParam(PARAM_PROJECTS, project1.getKey() + "," + project2.getKey())
      .setParam(PARAM_MODULE_UUIDS, module1.uuid() + "," + module2.uuid())
      .setParam(PARAM_FILE_UUIDS, file1.uuid() + "," + file2.uuid())
      .setParam("rules", rule1.getKey().toString() + "," + rule2.getKey().toString())
      .setParam("severities", "MAJOR,MINOR")
      .setParam("languages", rule1.getLanguage() + "," + rule2.getLanguage())
      .setParam("assignees", user1.getLogin() + "," + user2.getLogin())
      .setParam(FACETS, "severities,statuses,resolutions,rules,types,languages,projects,moduleUuids,fileUuids,assignees")
      .executeProtobuf(SearchWsResponse.class);

    Map<String, Number> expectedStatuses = ImmutableMap.<String, Number>builder().put("OPEN", 1L).put("CONFIRMED", 0L)
      .put("REOPENED", 0L).put("RESOLVED", 0L).put("CLOSED", 0L).put("IN_REVIEW", 0L).put("TO_REVIEW", 0L).put("REVIEWED", 0L).build();

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(
        tuple("severities", of("INFO", 0L, "MINOR", 0L, "MAJOR", 1L, "CRITICAL", 0L, "BLOCKER", 0L)),
        tuple("statuses", expectedStatuses),
        tuple("resolutions", of("", 1L, "FALSE-POSITIVE", 0L, "FIXED", 0L, "REMOVED", 0L, "WONTFIX", 0L)),
        tuple("rules", of(rule1.getKey().toString(), 1L, rule2.getKey().toString(), 0L)),
        tuple("types", of("CODE_SMELL", 1L, "BUG", 0L, "VULNERABILITY", 0L, "SECURITY_HOTSPOT", 0L)),
        tuple("languages", of(rule1.getLanguage(), 1L, rule2.getLanguage(), 0L)),
        tuple("projects", of(project1.getKey(), 1L, project2.getKey(), 0L)),
        tuple("moduleUuids", of(module1.uuid(), 1L, module2.uuid(), 0L)),
        tuple("fileUuids", of(file1.uuid(), 1L, file2.uuid(), 0L)),
        tuple("assignees", of("", 0L, user1.getLogin(), 1L, user2.getLogin(), 0L)));
  }

  @Test
  public void assignedToMe_facet_must_escape_login_of_authenticated_user() {
    // login looks like an invalid regexp
    UserDto user = db.users().insertUser(u -> u.setLogin("foo["));
    userSession.logIn(user);

    // should not fail
    SearchWsResponse response = ws.newRequest()
      .setParam(FACETS, "assigned_to_me")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(
        tuple("assigned_to_me", of("foo[", 0L)));
  }

  @Test
  public void assigned_to_me_facet_is_sticky_relative_to_assignees() {
    ComponentDto project = db.components().insertPublicProject();
    indexPermissions();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    UserDto john = db.users().insertUser();
    UserDto alice = db.users().insertUser();
    db.issues().insert(rule, project, file, i -> i.setAssigneeUuid(john.getUuid()));
    db.issues().insert(rule, project, file, i -> i.setAssigneeUuid(alice.getUuid()));
    db.issues().insert(rule, project, file, i -> i.setAssigneeUuid(null));
    indexIssues();
    userSession.logIn(john);

    SearchWsResponse response = ws.newRequest()
      .setParam("resolved", "false")
      .setParam("assignees", alice.getLogin())
      .setParam(FACETS, "assignees,assigned_to_me")
      .executeProtobuf(SearchWsResponse.class);

    assertThat(response.getFacets().getFacetsList())
      .extracting(Common.Facet::getProperty, facet -> facet.getValuesList().stream().collect(toMap(FacetValue::getVal, FacetValue::getCount)))
      .containsExactlyInAnyOrder(
        tuple("assignees", of(john.getLogin(), 1L, alice.getLogin(), 1L, "", 1L)),
        tuple("assigned_to_me", of(john.getLogin(), 1L)));
  }

  private void indexPermissions() {
    permissionIndexer.indexOnStartup(permissionIndexer.getIndexTypes());
  }

  private void indexIssues() {
    issueIndexer.indexOnStartup(issueIndexer.getIndexTypes());
  }

}
