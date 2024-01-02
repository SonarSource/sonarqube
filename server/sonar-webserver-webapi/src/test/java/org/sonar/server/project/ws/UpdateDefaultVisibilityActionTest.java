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
package org.sonar.server.project.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.Visibility;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.server.project.ws.UpdateDefaultVisibilityAction.ACTION;
import static org.sonar.server.project.ws.UpdateDefaultVisibilityAction.PARAM_PROJECT_VISIBILITY;

public class UpdateDefaultVisibilityActionTest {
  @Rule
  public final DbTester dbTester = DbTester.create();
  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();
  public final ProjectDefaultVisibility projectDefaultVisibility = new ProjectDefaultVisibility(dbTester.getDbClient());

  private final UpdateDefaultVisibilityAction underTest = new UpdateDefaultVisibilityAction(userSession, dbTester.getDbClient(),
    projectDefaultVisibility);
  private final WsActionTester wsTester = new WsActionTester(underTest);

  @Test
  public void change_project_visibility_to_private() {
    projectDefaultVisibility.set(dbTester.getSession(), Visibility.PUBLIC);
    dbTester.commit();

    userSession.logIn().setSystemAdministrator();

    wsTester.newRequest()
      .setParam(PARAM_PROJECT_VISIBILITY, "private")
      .execute();

    assertThat(projectDefaultVisibility.get(dbTester.getSession())).isEqualTo(Visibility.PRIVATE);
  }

  @Test
  public void change_project_visibility_to_public() {
    projectDefaultVisibility.set(dbTester.getSession(), Visibility.PRIVATE);
    dbTester.commit();

    userSession.logIn().setSystemAdministrator();

    wsTester.newRequest()
      .setParam(PARAM_PROJECT_VISIBILITY, "public")
      .execute();

    assertThat(projectDefaultVisibility.get(dbTester.getSession())).isEqualTo(Visibility.PUBLIC);
  }

  @Test
  public void fail_if_not_logged_as_system_administrator() {
    userSession.logIn();

    TestRequest request = wsTester.newRequest()
      .setParam(PARAM_PROJECT_VISIBILITY, "private");

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void verify_define() {
    WebService.Action action = wsTester.getDef();
    assertThat(action.key()).isEqualTo(ACTION);
    assertThat(action.isPost()).isTrue();
    assertThat(action.description()).isNotEmpty();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.since()).isEqualTo("6.4");
    assertThat(action.handler()).isEqualTo(underTest);
    assertThat(action.changelog())
      .extracting(Change::getVersion, Change::getDescription)
      .contains(tuple("7.3", "This WS used to be located at /api/organizations/update_project_visibility"));

    WebService.Param projectVisibility = action.param(PARAM_PROJECT_VISIBILITY);
    assertThat(projectVisibility.isRequired()).isTrue();
    assertThat(projectVisibility.possibleValues()).containsExactlyInAnyOrder("private", "public");
    assertThat(projectVisibility.description()).isEqualTo("Default visibility for projects");
  }
}
