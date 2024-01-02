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
import org.sonar.api.resources.ResourceTypeTree;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.DefaultResourceTypes;
import org.sonar.core.i18n.I18n;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.Components.Component;
import org.sonarqube.ws.Components.TreeWsResponse;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.FILE;
import static org.sonar.api.resources.Qualifiers.UNIT_TEST_FILE;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.ComponentTesting.newChildComponent;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newDirectoryOnBranch;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newProjectBranchCopy;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_COMPONENT;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_PULL_REQUEST;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_STRATEGY;

public class TreeActionIT {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private final DbClient dbClient = db.getDbClient();
  private final ResourceTypes defaultResourceTypes = new ResourceTypes(new ResourceTypeTree[]{DefaultResourceTypes.get()});
  private final ResourceTypesRule resourceTypes = new ResourceTypesRule()
    .setRootQualifiers(defaultResourceTypes.getRoots())
    .setAllQualifiers(defaultResourceTypes.getAll())
    .setLeavesQualifiers(FILE, UNIT_TEST_FILE);
  private final WsActionTester ws = new WsActionTester(new TreeAction(dbClient, new ComponentFinder(dbClient, resourceTypes), resourceTypes, userSession,
    mock(I18n.class)));

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();

    assertThat(action.since()).isEqualTo("5.4");
    assertThat(action.description()).isNotNull();
    assertThat(action.responseExample()).isNotNull();
    assertThat(action.changelog()).extracting(Change::getVersion, Change::getDescription).containsExactlyInAnyOrder(
      tuple("10.1", "The use of module keys in parameter 'component' is removed"),
      tuple("10.1", "The use of 'BRC' as value for parameter 'qualifiers' is removed"),
      tuple("7.6", "The use of 'BRC' as value for parameter 'qualifiers' is deprecated"),
      tuple("7.6", "The use of module keys in parameter 'component' is deprecated"));
    assertThat(action.params()).extracting(Param::key).containsExactlyInAnyOrder("component", "branch", "pullRequest", "qualifiers", "strategy",
      "q", "s", "p", "asc", "ps");

    Param component = action.param(PARAM_COMPONENT);
    assertThat(component.isRequired()).isTrue();
    assertThat(component.description()).isNotNull();
    assertThat(component.exampleValue()).isNotNull();

    Param branch = action.param(PARAM_BRANCH);
    assertThat(branch.isInternal()).isFalse();
    assertThat(branch.isRequired()).isFalse();
    assertThat(branch.since()).isEqualTo("6.6");
  }

  @Test
  public void json_example() throws IOException {
    ProjectData project = initJsonExampleComponents();
    logInWithBrowsePermission(project);

    String response = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.projectKey())
      .execute()
      .getInput();

    JsonAssert.assertJson(response)
      .withStrictArrayOrder()
      .isSimilarTo(getClass().getResource("tree-example.json"));
  }

  @Test
  public void return_children() {
    ProjectData projectData = db.components().insertPrivateProject(p->p.setUuid("project-uuid").setBranchUuid("project-uuid"));
    ComponentDto projectMainBranch = projectData.getMainBranchComponent();
    db.components().insertSnapshot(projectMainBranch);
    ComponentDto dir = newDirectory(projectMainBranch, "dir");
    db.components().insertComponent(dir);
    db.components().insertComponent(newFileDto(projectMainBranch, 1));
    for (int i = 2; i <= 9; i++) {
      db.components().insertComponent(newFileDto(dir, i));
    }
    ComponentDto directory = newDirectory(dir, "directory-path-1");
    db.components().insertComponent(directory);
    db.components().insertComponent(newFileDto(projectMainBranch, directory, 10));
    db.commit();
    logInWithBrowsePermission(projectData);

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_STRATEGY, "children")
      .setParam(PARAM_COMPONENT, dir.getKey())
      .setParam(Param.PAGE, "2")
      .setParam(Param.PAGE_SIZE, "3")
      .setParam(Param.TEXT_QUERY, "file-name")
      .setParam(Param.ASCENDING, "false")
      .setParam(Param.SORT, "name").executeProtobuf(TreeWsResponse.class);

    assertThat(response.getComponentsCount()).isEqualTo(3);
    assertThat(response.getPaging().getTotal()).isEqualTo(8);
    assertThat(response.getComponentsList()).extracting("key").containsExactly("file-key-6", "file-key-5", "file-key-4");
  }

  @Test
  public void return_descendants() {
    ProjectData projectData = db.components()
      .insertPrivateProject(p->p.setUuid("project-uuid").setBranchUuid("project-uuid"));
    ComponentDto projectMainBranch = projectData.getMainBranchComponent();
    db.components().insertSnapshot(projectMainBranch);
    ComponentDto module = newDirectory(projectMainBranch, "path");
    db.components().insertComponent(module);
    db.components().insertComponent(newFileDto(projectMainBranch, 10));
    for (int i = 2; i <= 9; i++) {
      db.components().insertComponent(newFileDto(module, i));
    }
    ComponentDto directory = newDirectory(module, "directory-path-1");
    db.components().insertComponent(directory);
    db.components().insertComponent(newFileDto(module, directory, 1));
    db.commit();
    logInWithBrowsePermission(projectData);

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_STRATEGY, "all")
      .setParam(PARAM_COMPONENT, module.getKey())
      .setParam(Param.PAGE, "2")
      .setParam(Param.PAGE_SIZE, "3")
      .setParam(Param.TEXT_QUERY, "file-name")
      .setParam(Param.ASCENDING, "true")
      .setParam(Param.SORT, "path").executeProtobuf(TreeWsResponse.class);

    assertThat(response.getComponentsCount()).isEqualTo(3);
    assertThat(response.getPaging().getTotal()).isEqualTo(9);
    assertThat(response.getComponentsList()).extracting("key").containsExactly("file-key-4", "file-key-5", "file-key-6");
  }

  @Test
  public void filter_descendants_by_qualifier() {
    ProjectData projectData = db.components()
      .insertPrivateProject(p->p.setUuid("project-uuid").setBranchUuid("project-uuid"));
    ComponentDto projectMainBranch = projectData.getMainBranchComponent();
    db.components().insertSnapshot(projectMainBranch);
    db.components().insertComponent(newFileDto(projectMainBranch, 1));
    db.components().insertComponent(newFileDto(projectMainBranch, 2));
    db.commit();
    logInWithBrowsePermission(projectData);

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_STRATEGY, "all")
      .setParam(PARAM_QUALIFIERS, FILE)
      .setParam(PARAM_COMPONENT, projectMainBranch.getKey()).executeProtobuf(TreeWsResponse.class);

    assertThat(response.getComponentsList()).extracting("key").containsExactly("file-key-1", "file-key-2");
  }

  @Test
  public void return_leaves() {
    ProjectData projectData = db.components()
      .insertPrivateProject(p->p.setUuid("mainBranch-uuid").setBranchUuid("mainBranch-uuid"));
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    db.components().insertSnapshot(mainBranch);
    db.components().insertComponent(newFileDto(mainBranch, 1));
    db.components().insertComponent(newFileDto(mainBranch, 2));
    ComponentDto directory = newDirectory(mainBranch, "directory-path-1");
    db.components().insertComponent(directory);
    db.components().insertComponent(newFileDto(mainBranch, directory, 3));
    db.commit();
    logInWithBrowsePermission(projectData);

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_STRATEGY, "leaves")
      .setParam(PARAM_COMPONENT, mainBranch.getKey())
      .setParam(PARAM_QUALIFIERS, FILE).executeProtobuf(TreeWsResponse.class);

    assertThat(response.getComponentsCount()).isEqualTo(3);
    assertThat(response.getPaging().getTotal()).isEqualTo(3);
    assertThat(response.getComponentsList()).extracting("key").containsExactly("file-key-1", "file-key-2", "file-key-3");
  }

  @Test
  public void sort_descendants_by_qualifier() {
    ProjectData projectData = db.components()
      .insertPrivateProject(p->p.setUuid("project-uuid").setBranchUuid("project-uuid").setKey("project-key"));
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    db.components().insertSnapshot(mainBranch);
    db.components().insertComponent(newFileDto(mainBranch, 1));
    db.components().insertComponent(newFileDto(mainBranch, 2));
    db.components().insertComponent(newDirectory(mainBranch, "path/directory/", "directory-uuid-1"));
    db.commit();
    logInWithBrowsePermission(projectData);

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_STRATEGY, "all")
      .setParam(Param.SORT, "qualifier, name")
      .setParam(PARAM_COMPONENT, mainBranch.getKey()).executeProtobuf(TreeWsResponse.class);

    assertThat(response.getComponentsList()).extracting("key").containsExactly("project-key:directory-uuid-1", "file-key-1", "file-key-2");
  }

  @Test
  public void project_reference_from_portfolio() {
    ComponentDto view = ComponentTesting.newPortfolio("view-uuid");
    db.components().insertPortfolioAndSnapshot(view);
    ProjectData project = db.components().insertPrivateProject(p->p.setUuid("project-uuid-1").setBranchUuid("project-uuid-1").setName("project-name").setKey("project-key-1"));
    db.components().insertSnapshot(project.getMainBranchComponent());
    db.components().insertComponent(newProjectCopy("project-uuid-1-copy", project.getMainBranchComponent(), view));
    db.components().insertComponent(ComponentTesting.newSubPortfolio(view, "sub-view-uuid", "sub-view-key").setName("sub-view-name"));
    db.commit();
    userSession.logIn()
      .registerPortfolios(view)
      .registerProjects(project.getProjectDto());

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_STRATEGY, "children")
      .setParam(PARAM_COMPONENT, view.getKey())
      .setParam(Param.TEXT_QUERY, "name").executeProtobuf(TreeWsResponse.class);

    assertThat(response.getComponentsList()).extracting("key").containsExactly("KEY_view-uuidproject-key-1", "sub-view-key");
    assertThat(response.getComponentsList()).extracting("refId").containsExactly("project-uuid-1", "");
    assertThat(response.getComponentsList()).extracting("refKey").containsExactly("project-key-1", "");
  }

  @Test
  public void project_branch_reference_from_portfolio() {
    ComponentDto view = ComponentTesting.newPortfolio("view-uuid");
    db.components().insertPortfolioAndSnapshot(view);
    ProjectData projectData = db.components().insertPrivateProject(p->p.setUuid("project-uuid-1").setBranchUuid("project-uuid-1").setName("project-name").setKey("project-key-1"));
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    db.components().insertSnapshot(mainBranch);
    db.components().insertComponent(newProjectBranchCopy("project-uuid-1-copy", mainBranch, view, "branch1"));
    db.components().insertComponent(ComponentTesting.newSubPortfolio(view, "sub-view-uuid", "sub-view-key").setName("sub-view-name"));
    db.commit();
    userSession.logIn()
      .registerPortfolios(view)
      .registerProjects(projectData.getProjectDto());

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_STRATEGY, "children")
      .setParam(PARAM_COMPONENT, view.getKey())
      .setParam(Param.TEXT_QUERY, "name").executeProtobuf(TreeWsResponse.class);

    assertThat(response.getComponentsList()).extracting("key").containsExactly("KEY_view-uuidproject-key-1", "sub-view-key");
    assertThat(response.getComponentsList()).extracting("refId").containsExactly("project-uuid-1", "");
    assertThat(response.getComponentsList()).extracting("refKey").containsExactly("project-key-1", "");
  }

  @Test
  public void project_branch_reference_from_application_branch() {
    String appBranchName = "app-branch";
    String projectBranchName = "project-branch";

    ProjectData applicationData = db.components().insertPrivateProject(c -> c.setQualifier(APP).setKey("app-key"));
    ProjectDto application = applicationData.getProjectDto();
    ComponentDto applicationBranch = db.components().insertProjectBranch(applicationData.getMainBranchComponent(), a -> a.setKey(appBranchName));

    ComponentDto project = db.components().insertPrivateProject(p -> p.setKey("project-key")).getMainBranchComponent();
    ComponentDto projectBranch = db.components().insertProjectBranch(project, b -> b.setKey(projectBranchName));
    ComponentDto techProjectBranch = db.components().insertComponent(newProjectCopy(projectBranch, applicationBranch)
      .setKey(applicationBranch.getKey() + project.getKey()));

    logInWithBrowsePermission(applicationData);
    userSession.addProjectBranchMapping(application.getUuid(), applicationBranch);

    TreeWsResponse result = ws.newRequest()
      .setParam(MeasuresWsParameters.PARAM_COMPONENT, applicationBranch.getKey())
      .setParam(MeasuresWsParameters.PARAM_BRANCH, appBranchName)
      .executeProtobuf(TreeWsResponse.class);

    assertThat(result.getBaseComponent())
      .extracting(Component::getKey, Component::getBranch)
      .containsExactlyInAnyOrder(applicationBranch.getKey(), appBranchName);
    assertThat(result.getComponentsList())
      .extracting(Component::getKey, Component::getBranch, Component::getRefId, Component::getRefKey)
      .containsExactlyInAnyOrder(tuple(techProjectBranch.getKey(), projectBranchName, projectBranch.uuid(), project.getKey()));
  }

  @Test
  public void response_is_empty_on_provisioned_projects() {
    ProjectData projectData = db.components().insertPrivateProject("project-uuid");
    ProjectDto project = projectData.getProjectDto();
    logInWithBrowsePermission(projectData);

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey()).executeProtobuf(TreeWsResponse.class);

    assertThat(response.getBaseComponent().getKey()).isEqualTo(project.getKey());
    assertThat(response.getComponentsList()).isEmpty();
    assertThat(response.getPaging().getTotal()).isZero();
    assertThat(response.getPaging().getPageSize()).isEqualTo(100);
    assertThat(response.getPaging().getPageIndex()).isOne();
  }

  @Test
  public void return_projects_composing_a_view() {
    ProjectData projectData = db.components().insertPrivateProject(p -> p.setUuid("project-uuid").setBranchUuid("project-uuid"));
    ComponentDto project = projectData.getMainBranchComponent();
    db.components().insertSnapshot(project);
    ComponentDto view = ComponentTesting.newPortfolio("view-uuid");
    db.components().insertPortfolioAndSnapshot(view);
    ComponentDto projectCopy = db.components().insertComponent(newProjectCopy("project-copy-uuid", project, view));
    userSession.logIn()
      .registerProjects(projectData.getProjectDto())
      .registerPortfolios(view);

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, view.getKey())
      .executeProtobuf(TreeWsResponse.class);

    assertThat(response.getBaseComponent().getKey()).isEqualTo(view.getKey());
    assertThat(response.getComponentsCount()).isOne();
    assertThat(response.getComponents(0).getKey()).isEqualTo(projectCopy.getKey());
    assertThat(response.getComponents(0).getRefKey()).isEqualTo(project.getKey());
  }

  @Test
  public void branch() {
    ProjectData project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project.getProjectDto());
    String branchKey = "my_branch";
    ComponentDto branch = db.components().insertProjectBranch(project.getMainBranchComponent(), b -> b.setKey(branchKey));
    userSession.addProjectBranchMapping(project.projectUuid(), branch);
    ComponentDto directory = db.components().insertComponent(newDirectoryOnBranch(branch, "dir", project.mainBranchUuid()));
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(directory, project.mainBranchUuid()));

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, branch.getKey())
      .setParam(PARAM_BRANCH, branchKey)
      .executeProtobuf(TreeWsResponse.class);

    assertThat(response.getBaseComponent()).extracting(Component::getKey, Component::getBranch)
      .containsExactlyInAnyOrder(branch.getKey(), branchKey);
    assertThat(response.getComponentsList()).extracting(Component::getKey, Component::getBranch)
      .containsExactlyInAnyOrder(
        tuple(directory.getKey(), branchKey),
        tuple(file.getKey(), branchKey));
  }

  @Test
  public void dont_show_branch_if_main_branch() {
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertFile(project.getMainBranchDto());
    userSession.addProjectPermission(UserRole.USER, project.getProjectDto())
      .addProjectBranchMapping(project.projectUuid(), project.getMainBranchComponent());

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, file.getKey())
      .setParam(PARAM_BRANCH, DEFAULT_MAIN_BRANCH_NAME)
      .executeProtobuf(TreeWsResponse.class);

    assertThat(response.getBaseComponent()).extracting(Component::getKey, Component::getBranch)
      .containsExactlyInAnyOrder(file.getKey(), "");
  }

  @Test
  public void pull_request() {
    ProjectData project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project.getProjectDto());
    String pullRequestId = "pr-123";
    ComponentDto branch = db.components().insertProjectBranch(project.getMainBranchComponent(), b -> b.setKey(pullRequestId).setBranchType(PULL_REQUEST));
    userSession.addProjectBranchMapping(project.projectUuid(), branch);
    ComponentDto directory = db.components().insertComponent(newDirectoryOnBranch(branch, "dir", project.mainBranchUuid()));
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(directory, project.mainBranchUuid()));

    TreeWsResponse response = ws.newRequest()
      .setParam(PARAM_COMPONENT, directory.getKey())
      .setParam(PARAM_PULL_REQUEST, pullRequestId)
      .executeProtobuf(TreeWsResponse.class);

    assertThat(response.getBaseComponent()).extracting(Component::getKey, Component::getPullRequest)
      .containsExactlyInAnyOrder(directory.getKey(), pullRequestId);
    assertThat(response.getComponentsList()).extracting(Component::getKey, Component::getPullRequest)
      .containsExactlyInAnyOrder(
        tuple(file.getKey(), pullRequestId));
  }

  @Test
  public void fail_when_not_enough_privileges() {
    ProjectData project = db.components().insertPrivateProject("project-uuid");
    userSession.logIn()
      .addProjectPermission(UserRole.CODEVIEWER, project.getProjectDto());
    db.commit();

    TestRequest request = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.projectKey());
    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_when_page_size_above_500() {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto("project-uuid"));
    db.commit();

    TestRequest request = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(Param.PAGE_SIZE, "501");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'ps' value (501) must be less than 500");
  }

  @Test
  public void fail_when_search_query_has_less_than_3_characters() {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto("project-uuid"));
    db.commit();

    TestRequest request = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.getKey())
      .setParam(Param.TEXT_QUERY, "fi");
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'q' length (2) is shorter than the minimum authorized (3)");
  }

  @Test
  public void fail_when_sort_is_unknown() {
    db.components().insertComponent(newPrivateProjectDto("project-uuid"));
    db.commit();

    TestRequest request = ws.newRequest()
      .setParam(PARAM_COMPONENT, "project-key")
      .setParam(Param.SORT, "unknown-sort");
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_when_strategy_is_unknown() {
    db.components().insertComponent(newPrivateProjectDto("project-uuid"));
    db.commit();

    TestRequest request = ws.newRequest()
      .setParam(PARAM_COMPONENT, "project-key")
      .setParam(PARAM_STRATEGY, "unknown-strategy");
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_when_base_component_not_found() {
    TestRequest request = ws.newRequest()
      .setParam(PARAM_COMPONENT, "project-key");
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_base_component_is_removed() {

    ProjectData projectData = db.components().insertPrivateProject(p->p.setKey("file-key").setEnabled(false));
    db.components().insertSnapshot(projectData.getMainBranchComponent());
    logInWithBrowsePermission(projectData);

    TestRequest request = ws.newRequest()
      .setParam(PARAM_COMPONENT, "file-key");
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'file-key' not found");
  }

  @Test
  public void fail_when_no_base_component_parameter() {
    TestRequest request = ws.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'component' parameter is missing");
  }

  @Test
  public void fail_if_branch_does_not_exist() {
    ProjectData project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project.getProjectDto());
    db.components().insertProjectBranch(project.getProjectDto(), b -> b.setKey("my_branch"));

    TestRequest request = ws.newRequest()
      .setParam(PARAM_COMPONENT, project.projectKey())
      .setParam(PARAM_BRANCH, "another_branch");
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Component '%s' on branch '%s' not found", project.projectKey(), "another_branch"));
  }

  private static ComponentDto newFileDto(ComponentDto moduleOrProject, @Nullable ComponentDto directory, int i) {
    return ComponentTesting.newFileDto(moduleOrProject, directory, "file-uuid-" + i)
      .setName("file-name-" + i)
      .setKey("file-key-" + i)
      .setPath("file-path-" + i);
  }

  private static ComponentDto newFileDto(ComponentDto moduleOrProject, int i) {
    return newFileDto(moduleOrProject, null, i);
  }

  private ProjectData initJsonExampleComponents() throws IOException {
    ProjectData projectData = db.components().insertPrivateProject(c -> c.setUuid("MY_PROJECT_ID")
        .setDescription("MY_PROJECT_DESCRIPTION")
        .setKey("MY_PROJECT_KEY")
        .setName("Project Name")
        .setBranchUuid("MY_PROJECT_ID"),
      p -> p.setTagsString("abc,def"));
    db.components().insertSnapshot(projectData.getMainBranchComponent());

    Date now = new Date();
    JsonParser jsonParser = new JsonParser();
    JsonElement jsonTree = jsonParser.parseString(IOUtils.toString(getClass().getResource("tree-example.json"), UTF_8));
    JsonArray components = jsonTree.getAsJsonObject().getAsJsonArray("components");
    for (int i = 0; i < components.size(); i++) {
      JsonElement componentAsJsonElement = components.get(i);
      JsonObject componentAsJsonObject = componentAsJsonElement.getAsJsonObject();
      String uuid = format("child-component-uuid-%d", i);
      db.components().insertComponent(newChildComponent(uuid, projectData.getMainBranchComponent(), projectData.getMainBranchComponent())
        .setKey(getJsonField(componentAsJsonObject, "key"))
        .setName(getJsonField(componentAsJsonObject, "name"))
        .setLanguage(getJsonField(componentAsJsonObject, "language"))
        .setPath(getJsonField(componentAsJsonObject, "path"))
        .setQualifier(getJsonField(componentAsJsonObject, "qualifier"))
        .setDescription(getJsonField(componentAsJsonObject, "description"))
        .setEnabled(true)
        .setCreatedAt(now));
    }
    db.commit();
    return projectData;
  }

  @CheckForNull
  private static String getJsonField(JsonObject jsonObject, String field) {
    JsonElement jsonElement = jsonObject.get(field);
    return jsonElement == null ? null : jsonElement.getAsString();
  }

  private void logInWithBrowsePermission(ProjectData project) {
    userSession.logIn().addProjectPermission(UserRole.USER, project.getProjectDto())
      .addProjectBranchMapping(project.projectUuid(), project.getMainBranchComponent());
  }

  @Test
  public void doHandle_whenPassingUnsupportedQualifier_ShouldThrowIllegalArgumentException() {
    ProjectData project = db.components().insertPrivateProject(p->p.setUuid("project-uuid").setBranchUuid("project-uuid"));
    db.components().insertSnapshot(project.getMainBranchComponent());
    db.commit();
    logInWithBrowsePermission(project);

    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_QUALIFIERS, "BRC")
      .setParam(PARAM_COMPONENT, project.getProjectDto().getKey());

    assertThatThrownBy(testRequest::execute).isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'qualifiers' (BRC) must be one of: [UTS, FIL, DIR, TRK]");
  }
}
