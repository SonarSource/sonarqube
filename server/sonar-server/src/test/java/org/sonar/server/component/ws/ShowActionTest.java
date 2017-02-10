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

import com.google.common.base.Throwables;
import java.io.IOException;
import java.io.InputStream;
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
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsComponents.ShowWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_KEY;

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
  public void show_with_browse_permission() {
    userSession.logIn().addProjectUuidPermissions(UserRole.USER, "project-uuid");
    componentDb.insertProjectAndSnapshot(newProjectDto(db.organizations().insert(), "project-uuid"));

    ShowWsResponse response = newRequest("project-uuid", null);

    assertThat(response.getComponent().getId()).isEqualTo("project-uuid");
  }

  @Test
  public void show_provided_project() {
    userSession.logIn().setRoot();
    componentDb.insertComponent(newProjectDto(db.organizations().insert(), "project-uuid").setEnabled(false));

    ShowWsResponse response = newRequest("project-uuid", null);

    assertThat(response.getComponent().getId()).isEqualTo("project-uuid");
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
    TestRequest request = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF);

    if (uuid != null) {
      request.setParam(PARAM_ID, uuid);
    }
    if (key != null) {
      request.setParam(PARAM_KEY, key);
    }

    try (InputStream responseStream = request.execute().getInputStream()) {
      return ShowWsResponse.parseFrom(responseStream);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private void insertJsonExampleComponentsAndSnapshots() {
    OrganizationDto organizationDto = db.organizations().insertForKey("my-org-1");
    ComponentDto project = newProjectDto(organizationDto, "AVIF98jgA3Ax6PH2efOW")
      .setKey("com.sonarsource:java-markdown")
      .setName("Java Markdown")
      .setDescription("Java Markdown Project")
      .setQualifier(Qualifiers.PROJECT);
    componentDb.insertProjectAndSnapshot(project);
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
