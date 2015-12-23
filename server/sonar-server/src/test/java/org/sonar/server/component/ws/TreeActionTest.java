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

package org.sonar.server.component.ws;

import com.google.common.base.Charsets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.DbTests;
import org.sonar.test.JsonAssert;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsComponents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newSubView;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_BASE_COMPONENT_ID;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_QUALIFIERS;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_STRATEGY;

@Category(DbTests.class)
public class TreeActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  ResourceTypesRule resourceTypes = new ResourceTypesRule();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  ComponentDbTester componentDb = new ComponentDbTester(db);
  DbClient dbClient = db.getDbClient();

  WsActionTester ws;

  @Before
  public void setUp() {
    userSession.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    ws = new WsActionTester(new TreeAction(dbClient, new ComponentFinder(dbClient), resourceTypes, userSession, Mockito.mock(I18n.class)));
    resourceTypes.setChildrenQualifiers(Qualifiers.MODULE, Qualifiers.FILE, Qualifiers.DIRECTORY);
    resourceTypes.setLeavesQualifiers(Qualifiers.FILE);
  }

  @Test
  public void json_example() throws IOException {
    ComponentDto project = initJsonExampleComponents();

    String response = ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, project.uuid())
      .execute().getInput();

    JsonAssert.assertJson(response)
      .withStrictArrayOrder()
      .isSimilarTo(getClass().getResource("tree-example.json"));
  }

  @Test
  public void direct_children() throws IOException {
    userSession.anonymous().login().addProjectUuidPermissions(UserRole.ADMIN, "project-uuid");
    ComponentDto project = newProjectDto("project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(project);
    SnapshotDto moduleSnapshot = componentDb.insertComponentAndSnapshot(newModuleDto("module-uuid-1", project), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, 1), projectSnapshot);
    for (int i = 2; i <= 9; i++) {
      componentDb.insertComponentAndSnapshot(newFileDto(project, i), moduleSnapshot);
    }
    SnapshotDto directorySnapshot = componentDb.insertComponentAndSnapshot(newDirectory(project, "directory-path-1"), moduleSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, 10), directorySnapshot);
    db.commit();
    componentDb.indexProjects();

    InputStream responseStream = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_STRATEGY, "children")
      .setParam(PARAM_BASE_COMPONENT_ID, "module-uuid-1")
      .setParam(Param.PAGE, "2")
      .setParam(Param.PAGE_SIZE, "3")
      .setParam(Param.TEXT_QUERY, "file-name")
      .setParam(Param.ASCENDING, "false")
      .setParam(Param.SORT, "name")
      .execute().getInputStream();
    WsComponents.TreeWsResponse response = WsComponents.TreeWsResponse.parseFrom(responseStream);

    assertThat(response.getComponentsCount()).isEqualTo(3);
    assertThat(response.getPaging().getTotal()).isEqualTo(8);
    assertThat(response.getComponentsList()).extracting("id").containsExactly("file-uuid-6", "file-uuid-5", "file-uuid-4");
  }

  @Test
  public void all_children() throws IOException {
    userSession.anonymous().login()
      .addProjectUuidPermissions(UserRole.USER, "project-uuid");

    ComponentDto project = newProjectDto("project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(project);
    SnapshotDto moduleSnapshot = componentDb.insertComponentAndSnapshot(newModuleDto("module-uuid-1", project), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, 10), projectSnapshot);
    for (int i = 2; i <= 9; i++) {
      componentDb.insertComponentAndSnapshot(newFileDto(project, i), moduleSnapshot);
    }
    SnapshotDto directorySnapshot = componentDb.insertComponentAndSnapshot(newDirectory(project, "directory-path-1"), moduleSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, 1), directorySnapshot);
    db.commit();
    componentDb.indexProjects();

    InputStream responseStream = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_STRATEGY, "all")
      .setParam(PARAM_BASE_COMPONENT_ID, "module-uuid-1")
      .setParam(Param.PAGE, "2")
      .setParam(Param.PAGE_SIZE, "3")
      .setParam(Param.TEXT_QUERY, "file-name")
      .setParam(Param.ASCENDING, "true")
      .setParam(Param.SORT, "path")
      .execute().getInputStream();
    WsComponents.TreeWsResponse response = WsComponents.TreeWsResponse.parseFrom(responseStream);

    assertThat(response.getComponentsCount()).isEqualTo(3);
    assertThat(response.getPaging().getTotal()).isEqualTo(9);
    assertThat(response.getComponentsList()).extracting("id").containsExactly("file-uuid-4", "file-uuid-5", "file-uuid-6");
  }

  @Test
  public void leaves_children() throws IOException {
    ComponentDto project = newProjectDto().setUuid("project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(project);
    SnapshotDto moduleSnapshot = componentDb.insertComponentAndSnapshot(newModuleDto("module-uuid-1", project), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, 1), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, 2), moduleSnapshot);
    SnapshotDto directorySnapshot = componentDb.insertComponentAndSnapshot(newDirectory(project, "directory-path-1"), moduleSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, 3), directorySnapshot);
    db.commit();
    componentDb.indexProjects();

    InputStream responseStream = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_STRATEGY, "leaves")
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .execute().getInputStream();
    WsComponents.TreeWsResponse response = WsComponents.TreeWsResponse.parseFrom(responseStream);

    assertThat(response.getComponentsCount()).isEqualTo(3);
    assertThat(response.getPaging().getTotal()).isEqualTo(3);
    assertThat(response.getComponentsList()).extracting("id").containsExactly("file-uuid-1", "file-uuid-2", "file-uuid-3");
  }

  @Test
  public void all_children_by_file_qualifier() throws IOException {
    ComponentDto project = newProjectDto().setUuid("project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(project);
    componentDb.insertComponentAndSnapshot(newFileDto(project, 1), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, 2), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newModuleDto("module-uuid-1", project), projectSnapshot);
    db.commit();
    componentDb.indexProjects();

    InputStream responseStream = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_STRATEGY, "all")
      .setParam(PARAM_QUALIFIERS, Qualifiers.FILE)
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .execute().getInputStream();
    WsComponents.TreeWsResponse response = WsComponents.TreeWsResponse.parseFrom(responseStream);

    assertThat(response.getComponentsList()).extracting("id").containsExactly("file-uuid-1", "file-uuid-2");
  }

  @Test
  public void all_children_sort_by_qualifier() throws IOException {
    ComponentDto project = newProjectDto().setUuid("project-uuid");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(project);
    componentDb.insertComponentAndSnapshot(newFileDto(project, 2), projectSnapshot);
    componentDb.insertComponentAndSnapshot(newFileDto(project, 1), projectSnapshot);
    ComponentDto module = newModuleDto("module-uuid-1", project);
    componentDb.insertComponentAndSnapshot(module, projectSnapshot);
    componentDb.insertComponentAndSnapshot(newDirectory(project, "path/directory/", "directory-uuid-1"), projectSnapshot);
    db.commit();
    componentDb.indexProjects();

    InputStream responseStream = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_STRATEGY, "all")
      .setParam(Param.SORT, "qualifier, name")
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .execute().getInputStream();
    WsComponents.TreeWsResponse response = WsComponents.TreeWsResponse.parseFrom(responseStream);

    assertThat(response.getComponentsList()).extracting("id").containsExactly("module-uuid-1", "path/directory/", "file-uuid-1", "file-uuid-2");
  }

  @Test
  public void direct_children_of_a_view() throws IOException {
    ComponentDto view = newView("view-uuid");
    SnapshotDto viewSnapshot = componentDb.insertViewAndSnapshot(view);
    ComponentDto project = newProjectDto("project-uuid-1");
    componentDb.insertProjectAndSnapshot(project);
    componentDb.insertComponentAndSnapshot(newProjectCopy("project-uuid-1-copy", project, view), viewSnapshot);
    componentDb.insertComponentAndSnapshot(newSubView(view, "sub-view-uuid", "sub-view-key"), viewSnapshot);
    db.commit();
    componentDb.indexProjects();

    InputStream responseStream = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_STRATEGY, "children")
      .setParam(PARAM_BASE_COMPONENT_ID, "view-uuid")
      .execute().getInputStream();
    WsComponents.TreeWsResponse response = WsComponents.TreeWsResponse.parseFrom(responseStream);

    assertThat(response.getComponentsList()).extracting("id").containsExactly("project-uuid-1-copy", "sub-view-uuid");
  }

  @Test
  public void empty_response_for_provisioned_project() throws IOException {
    componentDb.insertComponent(newProjectDto("project-uuid"));
    db.commit();

    InputStream responseStream = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .execute().getInputStream();
    WsComponents.TreeWsResponse response = WsComponents.TreeWsResponse.parseFrom(responseStream);

    assertThat(response.getComponentsList()).isEmpty();
    assertThat(response.getPaging().getTotal()).isEqualTo(0);
    assertThat(response.getPaging().getPageSize()).isEqualTo(100);
    assertThat(response.getPaging().getPageIndex()).isEqualTo(1);
  }

  @Test
  public void fail_when_not_enough_privileges() {
    expectedException.expect(ForbiddenException.class);
    userSession.anonymous().login()
      .addProjectUuidPermissions(UserRole.CODEVIEWER, "project-uuid");
    componentDb.insertComponent(newProjectDto("project-uuid"));
    db.commit();

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .execute();
  }

  @Test
  public void fail_when_page_size_above_500() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The 'ps' parameter must be less thant 500");
    componentDb.insertComponent(newProjectDto("project-uuid"));
    db.commit();

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(Param.PAGE_SIZE, "501")
      .execute();
  }

  @Test
  public void fail_when_sort_is_unknown() {
    expectedException.expect(IllegalArgumentException.class);
    componentDb.insertComponent(newProjectDto("project-uuid"));
    db.commit();

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(Param.SORT, "unknown-sort")
      .execute();
  }

  @Test
  public void fail_when_strategy_is_unknown() {
    expectedException.expect(IllegalArgumentException.class);
    componentDb.insertComponent(newProjectDto("project-uuid"));
    db.commit();

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .setParam(PARAM_STRATEGY, "unknown-strategy")
      .execute();
  }

  @Test
  public void fail_when_base_component_not_found() {
    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam(PARAM_BASE_COMPONENT_ID, "project-uuid")
      .execute();
  }

  @Test
  public void fail_when_no_base_component_parameter() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Either 'baseComponentId' or 'baseComponentKey' must be provided, not both");

    ws.newRequest().execute();
  }

  private static ComponentDto newFileDto(ComponentDto parentComponent, int i) {
    return ComponentTesting.newFileDto(parentComponent, "file-uuid-" + i)
      .setName("file-name-" + i)
      .setKey("file-key-" + i)
      .setPath("file-path-" + i);
  }

  private ComponentDto initJsonExampleComponents() throws IOException {
    ComponentDto project = newProjectDto("AVHE6JiwEplJjXTo0Rza");
    SnapshotDto projectSnapshot = componentDb.insertProjectAndSnapshot(project);
    Date now = new Date();
    JsonParser jsonParser = new JsonParser();
    JsonElement jsonTree = jsonParser.parse(IOUtils.toString(getClass().getResource("tree-example.json"), Charsets.UTF_8));
    JsonArray components = jsonTree.getAsJsonObject().getAsJsonArray("components");
    for (JsonElement componentAsJsonElement : components) {
      JsonObject componentAsJsonObject = componentAsJsonElement.getAsJsonObject();
      componentDb.insertComponentAndSnapshot(new ComponentDto()
        .setUuid(getJsonField(componentAsJsonObject, "id"))
        .setKey(getJsonField(componentAsJsonObject, "key"))
        .setName(getJsonField(componentAsJsonObject, "name"))
        .setPath(getJsonField(componentAsJsonObject, "path"))
        .setProjectUuid(project.projectUuid())
        .setQualifier(getJsonField(componentAsJsonObject, "qualifier"))
        .setDescription(getJsonField(componentAsJsonObject, "description"))
        .setEnabled(true)
        .setCreatedAt(now),
        projectSnapshot);
    }
    db.commit();
    componentDb.indexProjects();
    return project;
  }

  private static String getJsonField(JsonObject jsonObject, String field) {
    JsonElement jsonElement = jsonObject.get(field);
    return jsonElement == null ? null : jsonElement.getAsString();
  }
}
