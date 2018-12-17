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
package org.sonar.server.component.ws;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.index.ComponentIndex;
import org.sonar.server.component.index.ComponentIndexer;
import org.sonar.server.component.ws.SearchAction.SearchRequest;
import org.sonar.server.es.EsTester;
import org.sonar.server.l18n.I18nRule;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.permission.index.PermissionIndexerTester;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.Components.SearchWsResponse;
import org.sonarqube.ws.MediaTypes;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.resources.Qualifiers.DIRECTORY;
import static org.sonar.api.resources.Qualifiers.FILE;
import static org.sonar.api.resources.Qualifiers.MODULE;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;

public class SearchActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public EsTester es = EsTester.create();

  private I18nRule i18n = new I18nRule();
  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private ResourceTypesRule resourceTypes = new ResourceTypesRule();
  private Languages languages = mock(Languages.class);
  private ComponentIndexer indexer = new ComponentIndexer(db.getDbClient(), es.client());
  private PermissionIndexerTester authorizationIndexerTester = new PermissionIndexerTester(es, indexer);
  private ComponentIndex index = new ComponentIndex(es.client(), new WebAuthorizationTypeSupport(userSession), System2.INSTANCE);

  private UserDto user;

  private WsActionTester ws;

  @Before
  public void setUp() {
    resourceTypes.setAllQualifiers(PROJECT, MODULE, DIRECTORY, FILE);
    when(languages.all()).thenReturn(javaLanguage());
    ws = new WsActionTester(new SearchAction(index, db.getDbClient(), resourceTypes, i18n, languages, defaultOrganizationProvider));

    user = db.users().insertUser("john");
    userSession.logIn(user);
  }

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();

    assertThat(action.since()).isEqualTo("6.3");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.changelog()).extracting(Change::getVersion, Change::getDescription).containsExactlyInAnyOrder(
      tuple("7.6", "The use of 'BRC' as value for parameter 'qualifiers' is deprecated"));
    assertThat(action.responseExampleAsString()).isNotEmpty();

    assertThat(action.params()).hasSize(6);

    WebService.Param pageSize = action.param("ps");
    assertThat(pageSize.isRequired()).isFalse();
    assertThat(pageSize.defaultValue()).isEqualTo("100");
    assertThat(pageSize.maximumValue()).isEqualTo(500);
    assertThat(pageSize.description()).isEqualTo("Page size. Must be greater than 0 and less or equal than 500");

    WebService.Param qualifiers = action.param("qualifiers");
    assertThat(qualifiers.isRequired()).isTrue();

    WebService.Param organization = action.param("organization");
    assertThat(organization.isRequired()).isFalse();
    assertThat(organization.description()).isEqualTo("Organization key");
    assertThat(organization.isInternal()).isTrue();
    assertThat(organization.exampleValue()).isEqualTo("my-org");
    assertThat(organization.since()).isEqualTo("6.3");
  }

  @Test
  public void search_by_key_query() {
    insertProjectsAuthorizedForUser(
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setDbKey("project-_%-key"),
      ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization()).setDbKey("project-key-without-escaped-characters"));

    SearchWsResponse response = call(new SearchRequest().setQuery("project-_%-key").setQualifiers(singletonList(PROJECT)));

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("project-_%-key");
  }

  @Test
  public void search_for_files() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    ComponentDto file1 = newFileDto(project).setDbKey("file1");
    ComponentDto file2 = newFileDto(project).setDbKey("file2");
    db.components().insertComponents(project, file1, file2);
    setBrowsePermissionOnUserAndIndex(project);

    SearchWsResponse response = call(new SearchRequest().setQuery(file1.getDbKey()).setQualifiers(singletonList(FILE)));

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly(file1.getDbKey());
  }

  @Test
  public void search_with_pagination() {
    OrganizationDto organizationDto = db.organizations().insert();
    List<ComponentDto> componentDtoList = new ArrayList<>();
    for (int i = 1; i <= 9; i++) {
      componentDtoList.add(newPrivateProjectDto(organizationDto, "project-uuid-" + i).setDbKey("project-key-" + i).setName("Project Name " + i));
    }
    insertProjectsAuthorizedForUser(componentDtoList.toArray(new ComponentDto[] {}));

    SearchWsResponse response = call(new SearchRequest().setOrganization(organizationDto.getKey()).setPage(2).setPageSize(3).setQualifiers(singletonList(PROJECT)));

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsExactly("project-key-4", "project-key-5", "project-key-6");
  }

  @Test
  public void search_with_language() {
    OrganizationDto organizationDto = db.organizations().insert();
    insertProjectsAuthorizedForUser(
      ComponentTesting.newPrivateProjectDto(organizationDto).setDbKey("java-project").setLanguage("java"),
      ComponentTesting.newPrivateProjectDto(organizationDto).setDbKey("cpp-project").setLanguage("cpp"));

    SearchWsResponse response = call(new SearchRequest().setOrganization(organizationDto.getKey()).setLanguage("java").setQualifiers(singletonList(PROJECT)));

    assertThat(response.getComponentsList()).extracting(Component::getKey).containsOnly("java-project");
  }

  @Test
  public void return_only_components_from_projects_on_which_user_has_browse_permission() {
    ComponentDto project1 = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    ComponentDto file1 = newFileDto(project1).setDbKey("file1");
    ComponentDto file2 = newFileDto(project1).setDbKey("file2");
    ComponentDto project2 = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    ComponentDto file3 = newFileDto(project2).setDbKey("file3");
    db.components().insertComponents(project1, file1, file2, project2, file3);
    setBrowsePermissionOnUserAndIndex(project1);

    SearchWsResponse response = call(new SearchRequest().setQualifiers(singletonList(FILE)));

    assertThat(response.getComponentsList()).extracting(Component::getKey)
      .containsExactlyInAnyOrder(file1.getDbKey(), file2.getDbKey());
    assertThat(response.getPaging().getTotal()).isEqualTo(2);
  }

  @Test
  public void return_project_key() {
    ComponentDto project = ComponentTesting.newPublicProjectDto(db.getDefaultOrganization());
    ComponentDto module = ComponentTesting.newModuleDto(project);
    ComponentDto file1 = newFileDto(module).setDbKey("file1");
    ComponentDto file2 = newFileDto(module).setDbKey("file2");
    ComponentDto file3 = newFileDto(project).setDbKey("file3");
    db.components().insertComponents(project, module, file1, file2, file3);
    setBrowsePermissionOnUserAndIndex(project);

    SearchWsResponse response = call(new SearchRequest().setQualifiers(asList(PROJECT, MODULE, FILE)));

    assertThat(response.getComponentsList()).extracting(Component::getKey, Component::getProject)
      .containsOnly(tuple(project.getDbKey(), project.getDbKey()),
        tuple(module.getDbKey(), project.getDbKey()),
        tuple(file1.getDbKey(), project.getDbKey()),
        tuple(file2.getDbKey(), project.getDbKey()),
        tuple(file3.getDbKey(), project.getDbKey()));
  }

  @Test
  public void does_not_return_branches() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    setBrowsePermissionOnUserAndIndex(project, branch);

    SearchWsResponse response = call(new SearchRequest().setQualifiers(asList(PROJECT, MODULE, FILE)));

    assertThat(response.getComponentsList()).extracting(Component::getKey)
      .containsOnly(project.getDbKey());
  }

  @Test
  public void fail_if_unknown_qualifier_provided() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of parameter 'qualifiers' (Unknown-Qualifier) must be one of: [BRC, DIR, FIL, TRK]");

    call(new SearchRequest().setQualifiers(singletonList("Unknown-Qualifier")));
  }

  @Test
  public void fail_when_no_qualifier_provided() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'qualifiers' parameter is missing");

    call(new SearchRequest());
  }

  @Test
  public void test_json_example() {
    OrganizationDto organizationDto = db.organizations().insertForKey("my-org-1");
    db.components().insertComponent(newView(organizationDto));
    ComponentDto project = newPrivateProjectDto(organizationDto, "project-uuid").setName("Project Name").setDbKey("project-key");
    ComponentDto module = newModuleDto("module-uuid", project).setName("Module Name").setDbKey("module-key");
    ComponentDto directory = newDirectory(module, "path/to/directoy").setUuid("directory-uuid").setDbKey("directory-key").setName("Directory Name");
    db.components().insertComponents(project, module, directory,
      newFileDto(module, directory, "file-uuid").setDbKey("file-key").setLanguage("java").setName("File Name"));
    setBrowsePermissionOnUserAndIndex(project);

    String response = ws.newRequest()
      .setMediaType(MediaTypes.JSON)
      .setParam(PARAM_ORGANIZATION, organizationDto.getKey())
      .setParam(PARAM_QUALIFIERS, Joiner.on(",").join(PROJECT, DIRECTORY, FILE))
      .execute().getInput();
    assertJson(response).isSimilarTo(ws.getDef().responseExampleAsString());
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
    TestRequest request = ws.newRequest();
    ofNullable(wsRequest.getOrganization()).ifPresent(p3 -> request.setParam(PARAM_ORGANIZATION, p3));
    ofNullable(wsRequest.getLanguage()).ifPresent(p2 -> request.setParam(PARAM_LANGUAGE, p2));
    ofNullable(wsRequest.getQualifiers()).ifPresent(p1 -> request.setParam(PARAM_QUALIFIERS, Joiner.on(",").join(p1)));
    ofNullable(wsRequest.getQuery()).ifPresent(p -> request.setParam(TEXT_QUERY, p));
    ofNullable(wsRequest.getPage()).ifPresent(page -> request.setParam(PAGE, String.valueOf(page)));
    ofNullable(wsRequest.getPageSize()).ifPresent(pageSize -> request.setParam(PAGE_SIZE, String.valueOf(pageSize)));
    return request.executeProtobuf(SearchWsResponse.class);
  }

  private void index() {
    indexer.indexOnStartup(emptySet());
  }

  private static Language[] javaLanguage() {
    return new Language[] {new Language() {
      @Override
      public String getKey() {
        return "java";
      }

      @Override
      public String getName() {
        return "Java";
      }

      @Override
      public String[] getFileSuffixes() {
        return new String[0];
      }
    }};
  }
}
