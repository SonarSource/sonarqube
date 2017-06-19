/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.io.IOException;
import java.util.Date;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsComponents.ShowWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_COMPONENT;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_COMPONENT_ID;

public class ShowActionTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private WsActionTester ws = new WsActionTester(new ShowAction(userSession, db.getDbClient(), TestComponentFinder.from(db)));

  @Test
  public void verify_definition() throws Exception {
    WebService.Action action = ws.getDef();

    assertThat(action.since()).isEqualTo("5.4");
    assertThat(action.description()).isNotNull();
    assertThat(action.responseExample()).isNotNull();
    assertThat(action.changelog()).extracting(Change::getVersion, Change::getDescription).containsExactlyInAnyOrder(
      tuple("6.4", "Analysis date has been added to the response"),
      tuple("6.4", "The field 'id' is deprecated in the response"),
      tuple("6.4", "The 'visibility' field is added to the response"),
      tuple("6.5", "Leak period date is added to the response"));

    WebService.Param componentId = action.param(PARAM_COMPONENT_ID);
    assertThat(componentId.isRequired()).isFalse();
    assertThat(componentId.description()).isNotNull();
    assertThat(componentId.exampleValue()).isNotNull();
    assertThat(componentId.deprecatedSince()).isEqualTo("6.4");
    assertThat(componentId.deprecatedKey()).isEqualTo("id");
    assertThat(componentId.deprecatedKeySince()).isEqualTo("6.4");

    WebService.Param component = action.param(PARAM_COMPONENT);
    assertThat(component.isRequired()).isFalse();
    assertThat(component.description()).isNotNull();
    assertThat(component.exampleValue()).isNotNull();
    assertThat(component.deprecatedKey()).isEqualTo("key");
    assertThat(component.deprecatedKeySince()).isEqualTo("6.4");
  }

  @Test
  public void json_example() throws IOException {
    userSession.logIn().setRoot();
    insertJsonExampleComponentsAndSnapshots();

    String response = ws.newRequest()
      .setParam("id", "AVIF-FffA3Ax6PH2efPD")
      .execute()
      .getInput();

    assertJson(response).isSimilarTo(getClass().getResource("show-example.json"));
  }

  @Test
  public void tags_displayed_only_for_project() throws IOException {
    userSession.logIn().setRoot();
    insertJsonExampleComponentsAndSnapshots();

    String response = ws.newRequest()
      .setParam("id", "AVIF-FffA3Ax6PH2efPD")
      .execute()
      .getInput();

    assertThat(response).containsOnlyOnce("\"tags\"");
  }

  @Test
  public void show_with_browse_permission() {
    ComponentDto project = newPrivateProjectDto(db.organizations().insert(), "project-uuid");
    db.components().insertProjectAndSnapshot(project);
    userSession.logIn().addProjectPermission(USER, project);

    ShowWsResponse response = newRequest("project-uuid", null);

    assertThat(response.getComponent().getId()).isEqualTo("project-uuid");
  }

  @Test
  public void show_provided_project() {
    userSession.logIn().setRoot();
    db.components().insertComponent(newPrivateProjectDto(db.organizations().insert(), "project-uuid"));

    ShowWsResponse response = newRequest("project-uuid", null);

    assertThat(response.getComponent().getId()).isEqualTo("project-uuid");
    assertThat(response.getComponent().hasAnalysisDate()).isFalse();
    assertThat(response.getComponent().hasLeakPeriodDate()).isFalse();
  }

  @Test
  public void show_with_ancestors_when_not_project() throws Exception {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto directory = db.components().insertComponent(newDirectory(module, "dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(directory));
    userSession.addProjectPermission(USER, project);

    ShowWsResponse response = newRequest(null, file.key());

    assertThat(response.getComponent().getKey()).isEqualTo(file.key());
    assertThat(response.getAncestorsList()).extracting(WsComponents.Component::getKey).containsOnly(directory.key(), module.key(), project.key());
  }

  @Test
  public void show_without_ancestors_when_project() throws Exception {
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertComponent(newModuleDto(project));
    userSession.addProjectPermission(USER, project);

    ShowWsResponse response = newRequest(null, project.key());

    assertThat(response.getComponent().getKey()).isEqualTo(project.key());
    assertThat(response.getAncestorsList()).isEmpty();
  }

  @Test
  public void show_with_last_analysis_date() throws Exception {
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertSnapshots(
      newAnalysis(project).setCreatedAt(1_000_000_000L).setLast(false),
      newAnalysis(project).setCreatedAt(2_000_000_000L).setLast(false),
      newAnalysis(project).setCreatedAt(3_000_000_000L).setLast(true));
    userSession.addProjectPermission(USER, project);

    ShowWsResponse response = newRequest(null, project.key());

    assertThat(response.getComponent().getAnalysisDate()).isNotEmpty().isEqualTo(formatDateTime(new Date(3_000_000_000L)));
  }

  @Test
  public void show_with_leak_period_date() throws Exception {
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertSnapshots(
      newAnalysis(project).setPeriodDate(1_000_000_000L).setLast(false),
      newAnalysis(project).setPeriodDate(2_000_000_000L).setLast(false),
      newAnalysis(project).setPeriodDate(3_000_000_000L).setLast(true));

    userSession.addProjectPermission(USER, project);

    ShowWsResponse response = newRequest(null, project.key());

    assertThat(response.getComponent().getLeakPeriodDate()).isNotEmpty().isEqualTo(formatDateTime(new Date(3_000_000_000L)));
  }

  @Test
  public void show_with_ancestors_and_analysis_date() throws Exception {
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertSnapshot(newAnalysis(project).setCreatedAt(3_000_000_000L).setLast(true));
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto directory = db.components().insertComponent(newDirectory(module, "dir"));
    ComponentDto file = db.components().insertComponent(newFileDto(directory));
    userSession.addProjectPermission(USER, project);

    ShowWsResponse response = newRequest(null, file.key());

    String expectedDate = formatDateTime(new Date(3_000_000_000L));
    assertThat(response.getAncestorsList()).extracting(WsComponents.Component::getAnalysisDate)
      .containsOnly(expectedDate, expectedDate, expectedDate);
  }

  @Test
  public void should_return_visibility_for_private_project() throws Exception {
    userSession.logIn().setRoot();
    ComponentDto privateProject = db.components().insertPrivateProject();

    ShowWsResponse result = newRequest(null, privateProject.key());
    assertThat(result.getComponent().hasVisibility()).isTrue();
    assertThat(result.getComponent().getVisibility()).isEqualTo("private");
  }

  @Test
  public void should_return_visibility_for_public_project() throws Exception {
    userSession.logIn().setRoot();
    ComponentDto publicProject = db.components().insertPublicProject();

    ShowWsResponse result = newRequest(null, publicProject.key());
    assertThat(result.getComponent().hasVisibility()).isTrue();
    assertThat(result.getComponent().getVisibility()).isEqualTo("public");
  }

  @Test
  public void should_return_visibility_for_view() throws Exception {
    userSession.logIn().setRoot();
    ComponentDto view = db.components().insertView();

    ShowWsResponse result = newRequest(null, view.key());
    assertThat(result.getComponent().hasVisibility()).isTrue();
  }

  @Test
  public void should_not_return_visibility_for_module() throws Exception {
    userSession.logIn().setRoot();
    ComponentDto privateProject = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(privateProject));

    ShowWsResponse result = newRequest(null, module.key());
    assertThat(result.getComponent().hasVisibility()).isFalse();
  }

  @Test
  public void throw_ForbiddenException_if_user_doesnt_have_browse_permission_on_project() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    db.components().insertProjectAndSnapshot(newPrivateProjectDto(db.organizations().insert(), "project-uuid"));

    newRequest("project-uuid", null);
  }

  @Test
  public void fail_if_component_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component id 'unknown-uuid' not found");

    newRequest("unknown-uuid", null);
  }

  @Test
  public void fail_if_component_is_removed() {
    userSession.logIn().setRoot();
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization()));
    db.components().insertComponent(newFileDto(project).setKey("file-key").setEnabled(false));

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component key 'file-key' not found");

    newRequest(null, "file-key");
  }

  private ShowWsResponse newRequest(@Nullable String uuid, @Nullable String key) {
    TestRequest request = ws.newRequest();
    if (uuid != null) {
      request.setParam(PARAM_COMPONENT_ID, uuid);
    }
    if (key != null) {
      request.setParam(PARAM_COMPONENT, key);
    }
    return request.executeProtobuf(ShowWsResponse.class);
  }

  private void insertJsonExampleComponentsAndSnapshots() {
    OrganizationDto organizationDto = db.organizations().insertForKey("my-org-1");
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto(organizationDto, "AVIF98jgA3Ax6PH2efOW")
      .setKey("com.sonarsource:java-markdown")
      .setName("Java Markdown")
      .setDescription("Java Markdown Project")
      .setQualifier(Qualifiers.PROJECT)
      .setTagsString("language, plugin"));
    db.components().insertSnapshot(project, snapshot -> snapshot
      .setCreatedAt(parseDateTime("2017-03-01T11:39:03+0100").getTime())
      .setPeriodDate(parseDateTime("2017-01-01T11:39:03+0100").getTime()));
    ComponentDto directory = newDirectory(project, "AVIF-FfgA3Ax6PH2efPF", "src/main/java/com/sonarsource/markdown/impl")
      .setKey("com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl")
      .setName("src/main/java/com/sonarsource/markdown/impl")
      .setQualifier(Qualifiers.DIRECTORY);
    db.components().insertComponent(directory);
    db.components().insertComponent(
      newFileDto(directory, directory, "AVIF-FffA3Ax6PH2efPD")
        .setKey("com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl/Rule.java")
        .setName("Rule.java")
        .setPath("src/main/java/com/sonarsource/markdown/impl/Rule.java")
        .setLanguage("java")
        .setQualifier(Qualifiers.FILE));
  }
}
