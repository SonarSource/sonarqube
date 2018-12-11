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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.i18n.I18n;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.Components;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.Components.TreeWsResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.FILE;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.UNIT_TEST_FILE;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentTesting.newChildComponent;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.ComponentTesting.newSubView;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_COMPONENT;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_PULL_REQUEST;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_STRATEGY;

public class TreeActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private ResourceTypesRule resourceTypes = new ResourceTypesRule()
    .setRootQualifiers(PROJECT)
    .setLeavesQualifiers(FILE, UNIT_TEST_FILE);
  private DbClient dbClient = db.getDbClient();

  private WsActionTester ws = new WsActionTester(new TreeAction(dbClient, new ComponentFinder(dbClient, resourceTypes), resourceTypes, userSession, Mockito.mock(I18n.class)));

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();

    assertThat(action.since()).isEqualTo("5.4");
    assertThat(action.description()).isNotNull();
    assertThat(action.responseExample()).isNotNull();
    assertThat(action.changelog()).extracting(Change::getVersion, Change::getDescription).containsExactlyInAnyOrder(
      tuple("7.6", "The use of 'BRC' as value for parameter 'qualifiers' is deprecated"),
      tuple("7.6", "The use of module keys in parameter 'component' is deprecated"),
      tuple("6.4", "The field 'id' is deprecated in the response"));
    assertThat(action.params()).extracting(Param::key).containsExactlyInAnyOrder("component", "componentId", "branch", "pullRequest", "qualifiers", "strategy",
      "q", "s", "p", "asc", "ps");

    Param componentId = action.param(PARAM_COMPONENT_ID);
    assertThat(componentId.isRequired()).isFalse();
    assertThat(componentId.description()).isNotNull();
    assertThat(componentId.exampleValue()).isNotNull();
    assertThat(componentId.deprecatedSince()).isEqualTo("6.4");
    assertThat(componentId.deprecatedKey()).isEqualTo("baseComponentId");
    assertThat(componentId.deprecatedKeySince()).isEqualTo("6.4");

    Param component = action.param(PARAM_COMPONENT);
    assertThat(component.isRequired()).isFalse();
    assertThat(component.description()).isNotNull();
    assertThat(component.exampleValue()).isNotNull();
    assertThat(component.deprecatedKey()).isEqualTo("baseComponentKey");
    assertThat(component.deprecatedKeySince()).isEqualTo("6.4");

    Param branch = action.param(PARAM_BRANCH);
    assertThat(branch.isInternal()).isTrue();
    assertThat(branch.isRequired()).isFalse();
    assertThat(branch.since()).isEqualTo("6.6");
  }

  @Test
  public void json_example() throws IOException {
    ComponentDto project = initJsonExampleComponents();
    logInWithBrowsePermission(project);

    String response = ws.newRequest()
      .setParam(PARAM_COMPONENT_ID, project.uuid())
      .execute().getInput();

    JsonAssert.assertJson(response)
      .withStrictArrayOrder()
      .isSimilarTo(getClass().getResource("tree-example.json"));
  }

  @Test
  public void return_children() {
    ComponentDto project = newPrivateProjectDto(db.organizations().insert(), "project-uuid");
    db.components().insertProjectAndSnapshot(project);
    ComponentDto module = newModuleDto("module-uuid-1", project);
    db.components().insertComponent(module);
    db.components().insertComponent(newFileDto(project, 1));
    for (int i = 2; i <= 9; i++) {
      db.components().insertComponent(newFileDto(module, i));
    }
    ComponentDto directory = newDirectory(module, "directory-path-1");
    db.components().insertComponent(directory);
    db.components().insertComponent(newFileDto(module, directory, 10));
    db.commit();
    logInWithBrowsePermission(project);

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_STRATEGY, "children")
      .setParam(PARAM_COMPONENT_ID, "module-uuid-1")
      .setParam(Param.PAGE, "2")
      .setParam(Param.PAGE_SIZE, "3")
      .setParam(Param.TEXT_QUERY, "file-name")
      .setParam(Param.ASCENDING, "false")
      .setParam(Param.SORT, "name").executeProtobuf(TreeWsResponse.class);

    assertThat(response.getComponentsCount()).isEqualTo(3);
    assertThat(response.getPaging().getTotal()).isEqualTo(8);
    assertThat(response.getComponentsList()).extracting("id").containsExactly("file-uuid-6", "file-uuid-5", "file-uuid-4");
  }

  @Test
  public void return_descendants() {
    ComponentDto project = newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid");
    SnapshotDto projectSnapshot = db.components().insertProjectAndSnapshot(project);
    ComponentDto module = newModuleDto("module-uuid-1", project);
    db.components().insertComponent(module);
    db.components().insertComponent(newFileDto(project, 10));
    for (int i = 2; i <= 9; i++) {
      db.components().insertComponent(newFileDto(module, i));
    }
    ComponentDto directory = newDirectory(module, "directory-path-1");
    db.components().insertComponent(directory);
    db.components().insertComponent(newFileDto(module, directory, 1));
    db.commit();
    logInWithBrowsePermission(project);

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_STRATEGY, "all")
      .setParam(PARAM_COMPONENT_ID, "module-uuid-1")
      .setParam(Param.PAGE, "2")
      .setParam(Param.PAGE_SIZE, "3")
      .setParam(Param.TEXT_QUERY, "file-name")
      .setParam(Param.ASCENDING, "true")
      .setParam(Param.SORT, "path").executeProtobuf(TreeWsResponse.class);

    assertThat(response.getComponentsCount()).isEqualTo(3);
    assertThat(response.getPaging().getTotal()).isEqualTo(9);
    assertThat(response.getComponentsList()).extracting("id").containsExactly("file-uuid-4", "file-uuid-5", "file-uuid-6");
  }

  @Test
  public void filter_descendants_by_qualifier() {
    ComponentDto project = newPrivateProjectDto(db.organizations().insert(), "project-uuid");
    db.components().insertProjectAndSnapshot(project);
    db.components().insertComponent(newFileDto(project, 1));
    db.components().insertComponent(newFileDto(project, 2));
    db.components().insertComponent(newModuleDto("module-uuid-1", project));
    db.commit();
    logInWithBrowsePermission(project);

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_STRATEGY, "all")
      .setParam(PARAM_QUALIFIERS, FILE)
      .setParam(PARAM_COMPONENT_ID, "project-uuid").executeProtobuf(TreeWsResponse.class);

    assertThat(response.getComponentsList()).extracting("id").containsExactly("file-uuid-1", "file-uuid-2");
  }

  @Test
  public void return_leaves() {
    ComponentDto project = newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid");
    db.components().insertProjectAndSnapshot(project);
    ComponentDto module = newModuleDto("module-uuid-1", project);
    db.components().insertComponent(module);
    db.components().insertComponent(newFileDto(project, 1));
    db.components().insertComponent(newFileDto(module, 2));
    ComponentDto directory = newDirectory(project, "directory-path-1");
    db.components().insertComponent(directory);
    db.components().insertComponent(newFileDto(module, directory, 3));
    db.commit();
    logInWithBrowsePermission(project);

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_STRATEGY, "leaves")
      .setParam(PARAM_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_QUALIFIERS, FILE).executeProtobuf(TreeWsResponse.class);

    assertThat(response.getComponentsCount()).isEqualTo(3);
    assertThat(response.getPaging().getTotal()).isEqualTo(3);
    assertThat(response.getComponentsList()).extracting("id").containsExactly("file-uuid-1", "file-uuid-2", "file-uuid-3");
  }

  @Test
  public void sort_descendants_by_qualifier() {
    ComponentDto project = newPrivateProjectDto(db.organizations().insert(), "project-uuid");
    db.components().insertProjectAndSnapshot(project);
    db.components().insertComponent(newFileDto(project, 1));
    db.components().insertComponent(newFileDto(project, 2));
    ComponentDto module = newModuleDto("module-uuid-1", project);
    db.components().insertComponent(module);
    db.components().insertComponent(newDirectory(project, "path/directory/", "directory-uuid-1"));
    db.commit();
    logInWithBrowsePermission(project);

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_STRATEGY, "all")
      .setParam(Param.SORT, "qualifier, name")
      .setParam(PARAM_COMPONENT_ID, "project-uuid").executeProtobuf(TreeWsResponse.class);

    assertThat(response.getComponentsList()).extracting("id").containsExactly("module-uuid-1", "path/directory/", "file-uuid-1", "file-uuid-2");
  }

  @Test
  public void project_reference_from_portfolio() {
    OrganizationDto organizationDto = db.organizations().insert();
    ComponentDto view = newView(organizationDto, "view-uuid");
    db.components().insertViewAndSnapshot(view);
    ComponentDto project = newPrivateProjectDto(organizationDto, "project-uuid-1").setName("project-name").setDbKey("project-key-1");
    db.components().insertProjectAndSnapshot(project);
    db.components().insertComponent(newProjectCopy("project-uuid-1-copy", project, view));
    db.components().insertComponent(newSubView(view, "sub-view-uuid", "sub-view-key").setName("sub-view-name"));
    db.commit();
    userSession.logIn()
      .registerComponents(view, project);

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_STRATEGY, "children")
      .setParam(PARAM_COMPONENT_ID, "view-uuid")
      .setParam(Param.TEXT_QUERY, "name").executeProtobuf(TreeWsResponse.class);

    assertThat(response.getComponentsList()).extracting("id").containsExactly("project-uuid-1-copy", "sub-view-uuid");
    assertThat(response.getComponentsList()).extracting("refId").containsExactly("project-uuid-1", "");
    assertThat(response.getComponentsList()).extracting("refKey").containsExactly("project-key-1", "");
  }

  @Test
  public void project_branch_reference_from_application_branch() {
    ComponentDto application = db.components().insertMainBranch(c -> c.setQualifier(APP).setDbKey("app-key"));
    ComponentDto applicationBranch = db.components().insertProjectBranch(application, a -> a.setKey("app-branch"));
    ComponentDto project = db.components().insertPrivateProject(p -> p.setDbKey("project-key"));
    ComponentDto projectBranch = db.components().insertProjectBranch(project, b -> b.setKey("project-branch"));
    ComponentDto techProjectBranch = db.components().insertComponent(newProjectCopy(projectBranch, applicationBranch)
      .setDbKey(applicationBranch.getKey() + applicationBranch.getBranch() + projectBranch.getDbKey()));
    logInWithBrowsePermission(application);

    TreeWsResponse result = ws.newRequest()
      .setParam(MeasuresWsParameters.PARAM_COMPONENT, applicationBranch.getKey())
      .setParam(MeasuresWsParameters.PARAM_BRANCH, applicationBranch.getBranch())
      .executeProtobuf(TreeWsResponse.class);

    assertThat(result.getBaseComponent())
      .extracting(Component::getKey, Component::getBranch)
      .containsExactlyInAnyOrder(applicationBranch.getKey(), applicationBranch.getBranch());
    assertThat(result.getComponentsList())
      .extracting(Component::getKey, Component::getBranch, Component::getRefId, Component::getRefKey)
      .containsExactlyInAnyOrder(tuple(techProjectBranch.getKey(), projectBranch.getBranch(), projectBranch.uuid(), project.getKey()));
  }

  @Test
  public void response_is_empty_on_provisioned_projects() {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid"));
    logInWithBrowsePermission(project);

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT_ID, "project-uuid").executeProtobuf(TreeWsResponse.class);

    assertThat(response.getBaseComponent().getId()).isEqualTo("project-uuid");
    assertThat(response.getComponentsList()).isEmpty();
    assertThat(response.getPaging().getTotal()).isEqualTo(0);
    assertThat(response.getPaging().getPageSize()).isEqualTo(100);
    assertThat(response.getPaging().getPageIndex()).isEqualTo(1);
  }

  @Test
  public void return_projects_composing_a_view() {
    ComponentDto project = newPrivateProjectDto(db.organizations().insert(), "project-uuid");
    db.components().insertProjectAndSnapshot(project);
    ComponentDto view = newView(db.getDefaultOrganization(), "view-uuid");
    db.components().insertViewAndSnapshot(view);
    db.components().insertComponent(newProjectCopy("project-copy-uuid", project, view));
    userSession.logIn()
      .registerComponents(project, view);

    TreeWsResponse response = ws.newRequest().setParam(PARAM_COMPONENT_ID, view.uuid()).executeProtobuf(TreeWsResponse.class);

    assertThat(response.getBaseComponent().getId()).isEqualTo(view.uuid());
    assertThat(response.getComponentsCount()).isEqualTo(1);
    assertThat(response.getComponents(0).getId()).isEqualTo("project-copy-uuid");
    assertThat(response.getComponents(0).getRefId()).isEqualTo("project-uuid");
  }

  @Test
  public void branch() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    String branchKey = "my_branch";
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey(branchKey));
    ComponentDto module = db.components().insertComponent(newModuleDto(branch));
    ComponentDto directory = db.components().insertComponent(newDirectory(module, "dir"));
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(directory));

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, module.getKey())
      .setParam(PARAM_BRANCH, branchKey)
      .executeProtobuf(TreeWsResponse.class);

    assertThat(response.getBaseComponent()).extracting(Component::getKey, Component::getBranch)
      .containsExactlyInAnyOrder(module.getKey(), branchKey);
    assertThat(response.getComponentsList()).extracting(Component::getKey, Component::getBranch)
      .containsExactlyInAnyOrder(
        tuple(directory.getKey(), branchKey),
        tuple(file.getKey(), branchKey));
  }

  @Test
  public void pull_request() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    String pullRequestId = "pr-123";
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey(pullRequestId).setBranchType(PULL_REQUEST));
    ComponentDto module = db.components().insertComponent(newModuleDto(branch));
    ComponentDto directory = db.components().insertComponent(newDirectory(module, "dir"));
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(directory));

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, module.getKey())
      .setParam(PARAM_PULL_REQUEST, pullRequestId)
      .executeProtobuf(TreeWsResponse.class);

    assertThat(response.getBaseComponent()).extracting(Component::getKey, Component::getPullRequest)
      .containsExactlyInAnyOrder(module.getKey(), pullRequestId);
    assertThat(response.getComponentsList()).extracting(Component::getKey, Component::getPullRequest)
      .containsExactlyInAnyOrder(
        tuple(directory.getKey(), pullRequestId),
        tuple(file.getKey(), pullRequestId));
  }

  @Test
  public void fail_when_using_branch_db_key() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.addProjectPermission(UserRole.USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(String.format("Component key '%s' not found", branch.getDbKey()));

    ws.newRequest()
      .setParam(PARAM_COMPONENT, branch.getDbKey())
      .executeProtobuf(Components.ShowWsResponse.class);
  }

  @Test
  public void fail_when_using_branch_uuid() {
    ComponentDto project = db.components().insertMainBranch();
    userSession.addProjectPermission(UserRole.USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(String.format("Component id '%s' not found", branch.uuid()));

    ws.newRequest()
      .setParam(PARAM_COMPONENT_ID, branch.uuid())
      .executeProtobuf(Components.ShowWsResponse.class);
  }

  @Test
  public void fail_when_not_enough_privileges() {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(db.organizations().insert(), "project-uuid"));
    userSession.logIn()
      .addProjectPermission(UserRole.CODEVIEWER, project);
    db.commit();

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam(PARAM_COMPONENT_ID, "project-uuid")
      .execute();
  }

  @Test
  public void fail_when_page_size_above_500() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'ps' value (501) must be less than 500");
    db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid"));
    db.commit();

    ws.newRequest()
      .setParam(PARAM_COMPONENT_ID, "project-uuid")
      .setParam(Param.PAGE_SIZE, "501")
      .execute();
  }

  @Test
  public void fail_when_search_query_has_less_than_3_characters() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'q' length (2) is shorter than the minimum authorized (3)");
    db.components().insertComponent(newPrivateProjectDto(db.organizations().insert(), "project-uuid"));
    db.commit();

    ws.newRequest()
      .setParam(PARAM_COMPONENT_ID, "project-uuid")
      .setParam(Param.TEXT_QUERY, "fi")
      .execute();
  }

  @Test
  public void fail_when_sort_is_unknown() {
    expectedException.expect(IllegalArgumentException.class);
    db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), "project-uuid"));
    db.commit();

    ws.newRequest()
      .setParam(PARAM_COMPONENT_ID, "project-uuid")
      .setParam(Param.SORT, "unknown-sort")
      .execute();
  }

  @Test
  public void fail_when_strategy_is_unknown() {
    expectedException.expect(IllegalArgumentException.class);
    db.components().insertComponent(newPrivateProjectDto(db.organizations().insert(), "project-uuid"));
    db.commit();

    ws.newRequest()
      .setParam(PARAM_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_STRATEGY, "unknown-strategy")
      .execute();
  }

  @Test
  public void fail_when_base_component_not_found() {
    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam(PARAM_COMPONENT_ID, "project-uuid")
      .execute();
  }

  @Test
  public void fail_when_base_component_is_removed() {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization()));
    db.components().insertComponent(ComponentTesting.newFileDto(project).setDbKey("file-key").setEnabled(false));
    logInWithBrowsePermission(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'file-key' not found");

    ws.newRequest()
      .setParam(PARAM_COMPONENT, "file-key")
      .execute();
  }

  @Test
  public void fail_when_no_base_component_parameter() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'componentId' or 'component' must be provided");

    ws.newRequest().execute();
  }

  @Test
  public void fail_when_componentId_and_branch_params_are_used_together() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Parameter 'componentId' cannot be used at the same time as 'branch' or 'pullRequest'");

    ws.newRequest()
      .setParam(PARAM_COMPONENT_ID, branch.uuid())
      .setParam(PARAM_BRANCH, "my_branch")
      .execute();
  }

  @Test
  public void fail_if_branch_does_not_exist() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(String.format("Component '%s' on branch '%s' not found", project.getKey(), "another_branch"));

    ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(PARAM_BRANCH, "another_branch")
      .execute();
  }

  private static ComponentDto newFileDto(ComponentDto moduleOrProject, @Nullable ComponentDto directory, int i) {
    return ComponentTesting.newFileDto(moduleOrProject, directory, "file-uuid-" + i)
      .setName("file-name-" + i)
      .setDbKey("file-key-" + i)
      .setPath("file-path-" + i);
  }

  private static ComponentDto newFileDto(ComponentDto moduleOrProject, int i) {
    return newFileDto(moduleOrProject, null, i);
  }

  private ComponentDto initJsonExampleComponents() throws IOException {
    OrganizationDto organizationDto = db.organizations().insertForKey("my-org-1");
    ComponentDto project = newPrivateProjectDto(organizationDto, "MY_PROJECT_ID")
      .setDbKey("MY_PROJECT_KEY")
      .setName("Project Name");
    db.components().insertProjectAndSnapshot(project);
    Date now = new Date();
    JsonParser jsonParser = new JsonParser();
    JsonElement jsonTree = jsonParser.parse(IOUtils.toString(getClass().getResource("tree-example.json"), UTF_8));
    JsonArray components = jsonTree.getAsJsonObject().getAsJsonArray("components");
    for (JsonElement componentAsJsonElement : components) {
      JsonObject componentAsJsonObject = componentAsJsonElement.getAsJsonObject();
      String uuid = getJsonField(componentAsJsonObject, "id");
      db.components().insertComponent(newChildComponent(uuid, project, project)
        .setDbKey(getJsonField(componentAsJsonObject, "key"))
        .setName(getJsonField(componentAsJsonObject, "name"))
        .setLanguage(getJsonField(componentAsJsonObject, "language"))
        .setPath(getJsonField(componentAsJsonObject, "path"))
        .setQualifier(getJsonField(componentAsJsonObject, "qualifier"))
        .setDescription(getJsonField(componentAsJsonObject, "description"))
        .setEnabled(true)
        .setCreatedAt(now));
    }
    db.commit();
    return project;
  }

  @CheckForNull
  private static String getJsonField(JsonObject jsonObject, String field) {
    JsonElement jsonElement = jsonObject.get(field);
    return jsonElement == null ? null : jsonElement.getAsString();
  }

  private void logInWithBrowsePermission(ComponentDto project) {
    userSession.logIn().addProjectPermission(UserRole.USER, project);
  }
}
