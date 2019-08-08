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
package org.sonar.server.project.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.DefaultOrganizationProviderImpl;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.server.project.ws.UpdateDefaultVisibilityAction.ACTION;
import static org.sonar.server.project.ws.UpdateDefaultVisibilityAction.PARAM_PROJECT_VISIBILITY;

public class UpdateDefaultVisibilityActionTest {
  @Rule
  public DbTester dbTester = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DefaultOrganizationProvider defaultOrganizationProvider = new DefaultOrganizationProviderImpl(dbTester.getDbClient());
  private UpdateDefaultVisibilityAction underTest = new UpdateDefaultVisibilityAction(userSession, dbTester.getDbClient(), defaultOrganizationProvider);
  private WsActionTester wsTester = new WsActionTester(underTest);

  @Test
  public void change_project_visibility_to_private() {
    userSession.logIn().setSystemAdministrator();

    wsTester.newRequest()
      .setParam(PARAM_PROJECT_VISIBILITY, "private")
      .execute();

    assertThat(dbTester.getDbClient().organizationDao().getNewProjectPrivate(dbTester.getSession(), dbTester.getDefaultOrganization())).isTrue();
  }

  @Test
  public void change_project_visibility_to_public() {
    dbTester.organizations().setNewProjectPrivate(dbTester.getDefaultOrganization(), true);
    userSession.logIn().setSystemAdministrator();

    wsTester.newRequest()
      .setParam(PARAM_PROJECT_VISIBILITY, "public")
      .execute();

    assertThat(dbTester.organizations().getNewProjectPrivate(dbTester.getDefaultOrganization())).isFalse();
  }

  @Test
  public void fail_if_not_loggued_as_system_administrator() {
    userSession.logIn();

    TestRequest request = wsTester.newRequest()
      .setParam(PARAM_PROJECT_VISIBILITY, "private");

    expectedException.expect(ForbiddenException.class);
    request.execute();
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
