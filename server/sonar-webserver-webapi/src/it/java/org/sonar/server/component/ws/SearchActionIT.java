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
package org.sonar.server.component.ws;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.index.ComponentIndex;
import org.sonar.server.component.index.EntityDefinitionIndexer;
import org.sonar.server.component.ws.SearchAction.SearchRequest;
import org.sonar.server.es.EsTester;
import org.sonar.server.l18n.I18nRule;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.Components.SearchWsResponse;
import org.sonarqube.ws.MediaTypes;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.db.component.ComponentQualifiers.DIRECTORY;
import static org.sonar.db.component.ComponentQualifiers.FILE;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newPortfolio;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;

public class SearchActionIT {
  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public final EsTester es = EsTester.create();

  private final I18nRule i18n = new I18nRule();
  private final ComponentTypesRule resourceTypes = new ComponentTypesRule();
  private final EntityDefinitionIndexer indexer = new EntityDefinitionIndexer(db.getDbClient(), es.client());
  private final PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, indexer);
  private final ComponentIndex index = new ComponentIndex(es.client(), new WebAuthorizationTypeSupport(userSession), System2.INSTANCE);

  private UserDto user;
  private WsActionTester underTest;

  @Before
  public void setUp() {
    resourceTypes.setAllQualifiers(APP, PROJECT, DIRECTORY, FILE);
    underTest = new WsActionTester(new SearchAction(index, db.getDbClient(), resourceTypes, i18n));

    user = db.users().insertUser("john");
    userSession.logIn(user);
  }

  @Test
  public void verify_definition() {
    WebService.Action action = underTest.getDef();

    assertThat(action.since()).isEqualTo("6.3");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.changelog())
      .extracting(Change::getVersion, Change::getDescription)
      .containsExactlyInAnyOrder(
        tuple("8.4", "Param 'language' has been removed"),
        tuple("8.4", "The use of 'DIR','FIL','UTS' and 'BRC' as values for parameter 'qualifiers' is no longer supported"),
        tuple("8.0", "Field 'id' from response has been removed"),
        tuple("7.6", "The use of 'BRC' as value for parameter 'qualifiers' is deprecated"));
    assertThat(action.responseExampleAsString()).isNotEmpty();

    assertThat(action.params()).hasSize(4);

    WebService.Param pageSize = action.param("ps");
    assertThat(pageSize.isRequired()).isFalse();
    assertThat(pageSize.defaultValue()).isEqualTo("100");
    assertThat(pageSize.maximumValue()).isEqualTo(500);
    assertThat(pageSize.description()).isEqualTo("Page size. Must be greater than 0 and less or equal than 500");

    WebService.Param qualifiers = action.param("qualifiers");
    assertThat(qualifiers.isRequired()).isTrue();
  }

  @Test
  public void search_by_key_query() {
    ProjectDto p1 = db.components().insertPrivateProject(p -> p.setKey("project-_%-key")).getProjectDto();
    ProjectDto p2 = db.components().insertPrivateProject(p -> p.setKey("project-key-without-escaped-characters")).getProjectDto();

    insertProjectsAuthorizedForUser(List.of(p1, p2));
    SearchWsResponse response = call(new SearchRequest().setQuery("project-_%-key").setQualifiers(singletonList(PROJECT)));

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("project-_%-key");
  }

  @Test
  public void search_with_pagination() {
    List<ProjectDto> projectList = new ArrayList<>();
    for (int i = 1; i <= 9; i++) {
      int j = i;
      projectList.add(db.components().insertPrivateProject("project-uuid-" + j, p -> p.setKey("project-key-" + j).setName("Project Name " + j)).getProjectDto());
    }
    insertProjectsAuthorizedForUser(projectList);

    SearchWsResponse response = call(new SearchRequest().setPage(2).setPageSize(3).setQualifiers(singletonList(PROJECT)));

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsExactly("project-key-4", "project-key-5", "project-key-6");
  }

  @Test
  public void return_only_projects_on_which_user_has_browse_permission() {
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    ComponentDto portfolio = db.components().insertPrivatePortfolio();

    setBrowsePermissionOnUserAndIndex(List.of(project1));

    SearchWsResponse response = call(new SearchRequest().setQualifiers(singletonList(PROJECT)));

    assertThat(response.getComponentsList()).extracting(Component::getKey)
      .containsExactlyInAnyOrder(project1.getKey());
    assertThat(response.getPaging().getTotal()).isOne();
  }

  @Test
  public void return_project_key() {
    ProjectData project = db.components().insertPublicProject();
    ComponentDto dir1 = newDirectory(project.getMainBranchComponent(), "dir1").setKey("dir1");
    ComponentDto dir2 = newDirectory(project.getMainBranchComponent(), "dir2").setKey("dir2");
    ComponentDto dir3 = newDirectory(project.getMainBranchComponent(), "dir3").setKey("dir3");
    db.components().insertComponents(dir1, dir2, dir3);
    setBrowsePermissionOnUserAndIndex(List.of(project.getProjectDto()));

    SearchWsResponse response = call(new SearchRequest().setQualifiers(asList(PROJECT, APP)));

    assertThat(response.getComponentsList()).extracting(Component::getKey, Component::getProject)
      .containsOnly(tuple(project.getProjectDto().getKey(), project.getProjectDto().getKey()));
  }

  @Test
  public void does_not_return_branches() {
    ProjectData project = db.components().insertPublicProject();
    ComponentDto branch = db.components().insertProjectBranch(project.getMainBranchComponent());
    setBrowsePermissionOnUserAndIndex(List.of(project.getProjectDto()));

    SearchWsResponse response = call(new SearchRequest().setQualifiers(asList(PROJECT)));

    assertThat(response.getComponentsList()).extracting(Component::getKey)
      .containsOnly(project.getProjectDto().getKey());
  }

  @Test
  public void fail_if_unknown_qualifier_provided() {
    SearchRequest searchRequest = new SearchRequest().setQualifiers(singletonList("Unknown-Qualifier"));
    assertThatThrownBy(() -> call(searchRequest))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'qualifiers' (Unknown-Qualifier) must be one of: [APP, TRK]");
  }

  @Test
  public void fail_when_no_qualifier_provided() {
    SearchRequest searchRequest = new SearchRequest();
    assertThatThrownBy(() -> call(searchRequest))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'qualifiers' parameter is missing");
  }

  @Test
  public void test_json_example() {
    db.components().insertComponent(newPortfolio());
    ProjectData project = db.components().insertPrivateProject("project-uuid", p -> p.setName("Project Name").setKey("project-key"));
    ComponentDto directory = newDirectory(project.getMainBranchComponent(), "path/to/directoy").setUuid("directory-uuid").setKey("directory-key").setName("Directory Name");
    ComponentDto view = newPortfolio();
    db.components().insertComponents(directory, view);
    setBrowsePermissionOnUserAndIndex(List.of(project.getProjectDto()));

    String response = underTest.newRequest()
      .setMediaType(MediaTypes.JSON)
      .setParam(PARAM_QUALIFIERS, PROJECT)
      .execute().getInput();
    assertJson(response).isSimilarTo(underTest.getDef().responseExampleAsString());
  }

  private void insertProjectsAuthorizedForUser(List<ProjectDto> projects) {
    setBrowsePermissionOnUserAndIndex(projects);
    db.commit();
  }

  private void setBrowsePermissionOnUserAndIndex(List<ProjectDto> projects) {
    index();
    projects.forEach(project -> authorizationIndexerTester.allowOnlyUser(project, user));
  }

  private SearchWsResponse call(SearchRequest wsRequest) {
    TestRequest request = underTest.newRequest();
    ofNullable(wsRequest.getQualifiers()).ifPresent(p1 -> request.setParam(PARAM_QUALIFIERS, Joiner.on(",").join(p1)));
    ofNullable(wsRequest.getQuery()).ifPresent(p -> request.setParam(TEXT_QUERY, p));
    ofNullable(wsRequest.getPage()).ifPresent(page -> request.setParam(PAGE, String.valueOf(page)));
    ofNullable(wsRequest.getPageSize()).ifPresent(pageSize -> request.setParam(PAGE_SIZE, String.valueOf(pageSize)));
    return request.executeProtobuf(SearchWsResponse.class);
  }

  private void index() {
    indexer.indexAll();
  }

}
