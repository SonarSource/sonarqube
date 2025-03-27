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
package org.sonar.server.badge.ws;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.utils.System2;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbTester;
import org.sonar.db.component.ProjectData;
import org.sonar.db.project.ProjectBadgeTokenDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.TokenType;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usertoken.TokenGenerator;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TokenRenewActionIT {

  private final System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final TokenGenerator tokenGenerator = Mockito.mock(TokenGenerator.class);

  private final WsActionTester ws = new WsActionTester(
    new TokenRenewAction(
      db.getDbClient(),
      tokenGenerator, userSession));

  @Before
  public void before(){
    when(system2.now()).thenReturn(1000L);
  }

  @Test
  public void missing_project_parameter_should_fail() {
    TestRequest request = ws.newRequest();
    Assertions.assertThatThrownBy(request::execute)
      .hasMessage("The 'project' parameter is missing")
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void missing_project_admin_permission_should_fail() {
    ProjectData project = db.components().insertPrivateProject();

    TestRequest request = ws.newRequest().setParam("project", project.getProjectDto().getKey());

    Assertions.assertThatThrownBy(request::execute)
      .hasMessage("Insufficient privileges")
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void should_add_token_when_no_token_yet_and_return_204() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);
    when(tokenGenerator.generate(TokenType.PROJECT_BADGE_TOKEN)).thenReturn("generated_token");

    TestResponse response = ws.newRequest().setParam("project", project.getKey()).execute();

    ProjectBadgeTokenDto projectBadgeTokenDto = db.getDbClient().projectBadgeTokenDao().selectTokenByProject(db.getSession(), project);
    assertThat(projectBadgeTokenDto).isNotNull();
    assertThat(projectBadgeTokenDto.getToken()).isEqualTo("generated_token");
    response.assertNoContent();
  }

  @Test
  public void handle_whenApplicationKeyPassed_shouldAddTokenAndReturn204() {
    ProjectDto application = db.components().insertPrivateApplication().getProjectDto();
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, application);
    when(tokenGenerator.generate(TokenType.PROJECT_BADGE_TOKEN)).thenReturn("generated_token");

    TestResponse response = ws.newRequest().setParam("project", application.getKey()).execute();

    ProjectBadgeTokenDto projectBadgeTokenDto = db.getDbClient().projectBadgeTokenDao().selectTokenByProject(db.getSession(), application);
    assertThat(projectBadgeTokenDto).isNotNull();
    assertThat(projectBadgeTokenDto.getToken()).isEqualTo("generated_token");
    response.assertNoContent();
  }

  @Test
  public void should_replace_existing_token_when__token_already_present_and_update_update_at() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn().addProjectPermission(ProjectPermission.ADMIN, project);
    when(tokenGenerator.generate(TokenType.PROJECT_BADGE_TOKEN)).thenReturn("generated_token");

    ws.newRequest().setParam("project", project.getKey()).execute(); //inserting first token with updated at 1000

    when(system2.now()).thenReturn(2000L);
    ws.newRequest().setParam("project", project.getKey()).execute(); //replacing first token with updated at 2000

    ProjectBadgeTokenDto projectBadgeTokenDto = db.getDbClient().projectBadgeTokenDao().selectTokenByProject(db.getSession(), project);
    assertThat(projectBadgeTokenDto).isNotNull();
    assertThat(projectBadgeTokenDto.getToken()).isEqualTo("generated_token");
    assertThat(projectBadgeTokenDto.getUpdatedAt()).isEqualTo(2000L);
  }

}
