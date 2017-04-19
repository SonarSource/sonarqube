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
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.WsComponents;
import org.sonarqube.ws.WsComponents.ShowWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
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

  private ComponentDbTester componentDb = new ComponentDbTester(db);

  private WsActionTester ws = new WsActionTester(new ShowAction(userSession, db.getDbClient(), new ComponentFinder(db.getDbClient())));

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
    ComponentDto project = newProjectDto(db.organizations().insert(), "project-uuid");
    componentDb.insertProjectAndSnapshot(project);
    userSession.logIn().addProjectPermission(UserRole.USER, project);

    ShowWsResponse response = newRequest("project-uuid", null);

    assertThat(response.getComponent().getId()).isEqualTo("project-uuid");
  }

  @Test
  public void show_provided_project() {
    userSession.logIn().setRoot();
    componentDb.insertComponent(newProjectDto(db.organizations().insert(), "project-uuid").setEnabled(false));

    ShowWsResponse response = newRequest("project-uuid", null);

    assertThat(response.getComponent().getId()).isEqualTo("project-uuid");
    assertThat(response.getComponent().hasAnalysisDate()).isFalse();
  }

  @Test
  public void show_with_ancestors_when_not_project() throws Exception {
    ComponentDto project = componentDb.insertProject();
    ComponentDto module = componentDb.insertComponent(newModuleDto(project));
    ComponentDto directory = componentDb.insertComponent(newDirectory(module, "dir"));
    ComponentDto file = componentDb.insertComponent(newFileDto(directory));
    userSession.addProjectPermission(UserRole.USER, project);

    ShowWsResponse response = newRequest(null, file.key());

    assertThat(response.getComponent().getKey()).isEqualTo(file.key());
    assertThat(response.getAncestorsList()).extracting(WsComponents.Component::getKey).containsOnly(directory.key(), module.key(), project.key());
  }

  @Test
  public void show_without_ancestors_when_project() throws Exception {
    ComponentDto project = componentDb.insertProject();
    componentDb.insertComponent(newModuleDto(project));
    userSession.addProjectPermission(UserRole.USER, project);

    ShowWsResponse response = newRequest(null, project.key());

    assertThat(response.getComponent().getKey()).isEqualTo(project.key());
    assertThat(response.getAncestorsList()).isEmpty();
  }

  @Test
  public void show_with_last_analysis_date() throws Exception {
    ComponentDto project = componentDb.insertProject();
    componentDb.insertSnapshot(newAnalysis(project).setCreatedAt(1_000_000_000L).setLast(false));
    componentDb.insertSnapshot(newAnalysis(project).setCreatedAt(2_000_000_000L).setLast(false));
    componentDb.insertSnapshot(newAnalysis(project).setCreatedAt(3_000_000_000L).setLast(true));
    userSession.addProjectPermission(UserRole.USER, project);

    ShowWsResponse response = newRequest(null, project.key());

    assertThat(response.getComponent().getAnalysisDate()).isNotEmpty().isEqualTo(formatDateTime(new Date(3_000_000_000L)));
  }

  @Test
  public void show_with_ancestors_and_analysis_date() throws Exception {
    ComponentDto project = componentDb.insertProject();
    componentDb.insertSnapshot(newAnalysis(project).setCreatedAt(3_000_000_000L).setLast(true));
    ComponentDto module = componentDb.insertComponent(newModuleDto(project));
    ComponentDto directory = componentDb.insertComponent(newDirectory(module, "dir"));
    ComponentDto file = componentDb.insertComponent(newFileDto(directory));
    userSession.addProjectPermission(UserRole.USER, project);

    ShowWsResponse response = newRequest(null, file.key());

    String expectedDate = formatDateTime(new Date(3_000_000_000L));
    assertThat(response.getAncestorsList()).extracting(WsComponents.Component::getAnalysisDate)
      .containsOnly(expectedDate, expectedDate, expectedDate);
  }

  @Test
  public void throw_ForbiddenException_if_user_doesnt_have_browse_permission_on_project() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);
    componentDb.insertProjectAndSnapshot(newProjectDto(db.organizations().insert(), "project-uuid"));

    newRequest("project-uuid", null);
  }

  @Test
  public void fail_if_component_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Component id 'unknown-uuid' not found");

    newRequest("unknown-uuid", null);
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
    ComponentDto project = componentDb.insertComponent(newProjectDto(organizationDto, "AVIF98jgA3Ax6PH2efOW")
      .setKey("com.sonarsource:java-markdown")
      .setName("Java Markdown")
      .setDescription("Java Markdown Project")
      .setQualifier(Qualifiers.PROJECT)
      .setTagsString("language, plugin"));
    db.getDbClient().snapshotDao().insert(db.getSession(), newAnalysis(project).setCreatedAt(parseDateTime("2017-03-01T11:39:03+0100").getTime()));
    ComponentDto directory = newDirectory(project, "AVIF-FfgA3Ax6PH2efPF", "src/main/java/com/sonarsource/markdown/impl")
      .setKey("com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl")
      .setName("src/main/java/com/sonarsource/markdown/impl")
      .setQualifier(Qualifiers.DIRECTORY);
    componentDb.insertComponent(directory);
    componentDb.insertComponent(
      newFileDto(directory, directory, "AVIF-FffA3Ax6PH2efPD")
        .setKey("com.sonarsource:java-markdown:src/main/java/com/sonarsource/markdown/impl/Rule.java")
        .setName("Rule.java")
        .setPath("src/main/java/com/sonarsource/markdown/impl/Rule.java")
        .setLanguage("java")
        .setQualifier(Qualifiers.FILE));
  }
}
