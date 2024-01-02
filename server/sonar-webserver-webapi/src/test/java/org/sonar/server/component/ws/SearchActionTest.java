/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.index.ComponentIndex;
import org.sonar.server.component.index.ComponentIndexer;
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
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.DIRECTORY;
import static org.sonar.api.resources.Qualifiers.FILE;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newPortfolio;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;

public class SearchActionTest {
  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public final EsTester es = EsTester.create();

  private final I18nRule i18n = new I18nRule();
  private final ResourceTypesRule resourceTypes = new ResourceTypesRule();
  private final ComponentIndexer indexer = new ComponentIndexer(db.getDbClient(), es.client());
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
        tuple("8.4", "The use of 'DIR','FIL','UTS' as values for parameter 'qualifiers' is no longer supported"),
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
    insertProjectsAuthorizedForUser(
      ComponentTesting.newPrivateProjectDto().setKey("project-_%-key"),
      ComponentTesting.newPrivateProjectDto().setKey("project-key-without-escaped-characters"));

    SearchWsResponse response = call(new SearchRequest().setQuery("project-_%-key").setQualifiers(singletonList(PROJECT)));

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("project-_%-key");
  }

  @Test
  public void search_with_pagination() {
    List<ComponentDto> componentDtoList = new ArrayList<>();
    for (int i = 1; i <= 9; i++) {
      componentDtoList.add(newPrivateProjectDto("project-uuid-" + i).setKey("project-key-" + i).setName("Project Name " + i));
    }
    insertProjectsAuthorizedForUser(componentDtoList.toArray(new ComponentDto[] {}));

    SearchWsResponse response = call(new SearchRequest().setPage(2).setPageSize(3).setQualifiers(singletonList(PROJECT)));

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsExactly("project-key-4", "project-key-5", "project-key-6");
  }

  @Test
  public void return_only_projects_on_which_user_has_browse_permission() {
    ComponentDto project1 = ComponentTesting.newPrivateProjectDto();
    ComponentDto project2 = ComponentTesting.newPrivateProjectDto();
    ComponentDto portfolio = ComponentTesting.newPortfolio();

    db.components().insertComponents(project1, project2, portfolio);
    setBrowsePermissionOnUserAndIndex(project1);

    SearchWsResponse response = call(new SearchRequest().setQualifiers(singletonList(PROJECT)));

    assertThat(response.getComponentsList()).extracting(Component::getKey)
      .containsExactlyInAnyOrder(project1.getKey());
    assertThat(response.getPaging().getTotal()).isOne();
  }

  @Test
  public void return_project_key() {
    ComponentDto project = ComponentTesting.newPublicProjectDto();
    ComponentDto module = ComponentTesting.newModuleDto(project);
    ComponentDto dir1 = newDirectory(module, "dir1").setKey("dir1");
    ComponentDto dir2 = newDirectory(module, "dir2").setKey("dir2");
    ComponentDto dir3 = newDirectory(project, "dir3").setKey("dir3");
    db.components().insertComponents(project, module, dir1, dir2, dir3);
    setBrowsePermissionOnUserAndIndex(project);

    SearchWsResponse response = call(new SearchRequest().setQualifiers(asList(PROJECT, APP)));

    assertThat(response.getComponentsList()).extracting(Component::getKey, Component::getProject)
      .containsOnly(tuple(project.getKey(), project.getKey()));
  }

  @Test
  public void does_not_return_branches() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto branch = db.components().insertProjectBranch(project);
    setBrowsePermissionOnUserAndIndex(project, branch);

    SearchWsResponse response = call(new SearchRequest().setQualifiers(asList(PROJECT)));

    assertThat(response.getComponentsList()).extracting(Component::getKey)
      .containsOnly(project.getKey());
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
    ComponentDto project = newPrivateProjectDto("project-uuid").setName("Project Name").setKey("project-key");
    ComponentDto module = newModuleDto("module-uuid", project).setName("Module Name").setKey("module-key");
    ComponentDto directory = newDirectory(module, "path/to/directoy").setUuid("directory-uuid").setKey("directory-key").setName("Directory Name");
    ComponentDto view = newPortfolio();
    db.components().insertComponents(project, module, directory, view);
    setBrowsePermissionOnUserAndIndex(project);

    String response = underTest.newRequest()
      .setMediaType(MediaTypes.JSON)
      .setParam(PARAM_QUALIFIERS, PROJECT)
      .execute().getInput();
    assertJson(response).isSimilarTo(underTest.getDef().responseExampleAsString());
  }

  private void insertProjectsAuthorizedForUser(ComponentDto... projects) {
    db.components().insertComponents(projects);
    setBrowsePermissionOnUserAndIndex(projects);
    db.commit();
  }

  private void setBrowsePermissionOnUserAndIndex(ComponentDto... projects) {
    index();
    Arrays.stream(projects).forEach(project -> authorizationIndexerTester.allowOnlyUser(project, user));
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
