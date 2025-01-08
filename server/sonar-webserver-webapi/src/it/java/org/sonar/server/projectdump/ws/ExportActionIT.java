/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.projectdump.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.api.web.UserRole;
import org.sonar.ce.task.CeTask;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.ce.projectdump.ExportSubmitter;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class ExportActionIT {

  private static final String TASK_ID = "THGTpxcB-iU5OvuD2ABC";
  private static final String PROJECT_ID = "ABCTpxcB-iU5Ovuds4rf";
  private static final String PROJECT_KEY = "the_project_key";
  private static final String PROJECT_NAME = "The Project Name";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final ExportSubmitter exportSubmitter = mock(ExportSubmitter.class);
  private final ComponentTypesRule resourceTypes = new ComponentTypesRule().setRootQualifiers(ComponentQualifiers.PROJECT, ComponentQualifiers.VIEW);
  private final ProjectDumpWsSupport projectDumpWsSupport = new ProjectDumpWsSupport(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), resourceTypes));
  private final ExportAction underTest = new ExportAction(projectDumpWsSupport, userSession, exportSubmitter);
  private final WsActionTester actionTester = new WsActionTester(underTest);
  private ProjectDto project;

  @Before
  public void setUp() {
    project = db.components().insertPrivateProject(PROJECT_ID, p -> p.setKey(PROJECT_KEY).setName(PROJECT_NAME)).getProjectDto();
  }

  @Test
  public void response_example_is_defined() {
    assertThat(responseExample()).isNotEmpty();
  }

  @Test
  public void fails_if_missing_project_key() {
    logInAsProjectAdministrator("foo");

    assertThatThrownBy(() -> actionTester.newRequest().setMethod("POST").execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'key' parameter is missing");
  }

  @Test
  public void fails_if_not_project_administrator() {
    userSession.logIn();

    TestRequest request = actionTester.newRequest().setMethod("POST").setParam("key", project.getKey());
    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void triggers_CE_task() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(UserRole.ADMIN, project);

    when(exportSubmitter.submitProjectExport(project.getKey(), user.getUuid())).thenReturn(createResponseExampleTask());
    TestResponse response = actionTester.newRequest().setMethod("POST").setParam("key", project.getKey()).execute();

    assertJson(response.getInput()).isSimilarTo(responseExample());
  }

  @Test
  public void fails_to_trigger_task_if_anonymous() {
    userSession.anonymous();

    TestRequest request = actionTester.newRequest().setMethod("POST").setParam("key", project.getKey());
    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void triggers_CE_task_if_project_admin() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(UserRole.ADMIN, project);

    when(exportSubmitter.submitProjectExport(project.getKey(), user.getUuid())).thenReturn(createResponseExampleTask());
    TestResponse response = actionTester.newRequest().setMethod("POST").setParam("key", project.getKey()).execute();

    assertJson(response.getInput()).isSimilarTo(responseExample());
  }

  private void logInAsProjectAdministrator(String login) {
    userSession.logIn(login).addProjectPermission(UserRole.ADMIN, project);
  }

  private String responseExample() {
    return actionTester.getDef().responseExampleAsString();
  }

  private CeTask createResponseExampleTask() {
    CeTask.Component component = new CeTask.Component(project.getUuid(), project.getKey(), project.getName());
    return new CeTask.Builder()
      .setType(CeTaskTypes.PROJECT_EXPORT)
      .setUuid(TASK_ID)
      .setComponent(component)
      .setEntity(component)
      .build();
  }
}
