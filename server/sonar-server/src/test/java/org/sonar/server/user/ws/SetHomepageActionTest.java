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
package org.sonar.server.user.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.user.ws.SetHomepageAction.PARAM_TYPE;
import static org.sonar.server.user.ws.SetHomepageAction.PARAM_VALUE;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.MY_ISSUES;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.MY_PROJECTS;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.ORGANIZATION;
import static org.sonarqube.ws.Users.CurrentWsResponse.HomepageType.PROJECT;

public class SetHomepageActionTest {

  @Rule
  public UserSessionRule session = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = db.getDbClient();
  private SetHomepageAction underTest = new SetHomepageAction(session, new HomepageUpdater(dbClient));
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void verify_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action.key()).isEqualTo("set_homepage");
    assertThat(action.isInternal()).isFalse();
    assertThat(action.isPost()).isTrue();
    assertThat(action.since()).isEqualTo("7.0");
    assertThat(action.description()).isEqualTo("Set Homepage of current user.<br> Requires authentication.");
    assertThat(action.responseExample()).isNull();
    assertThat(action.deprecatedKey()).isNull();
    assertThat(action.deprecatedSince()).isNull();
    assertThat(action.handler()).isSameAs(underTest);
    assertThat(action.params()).hasSize(2);

    WebService.Param typeParam = action.param("type");
    assertThat(typeParam.isRequired()).isTrue();
    assertThat(typeParam.description()).isEqualTo("Type of the requested page");
    assertThat(typeParam.defaultValue()).isNull();
    assertThat(typeParam.possibleValues()).containsExactlyInAnyOrder("PROJECT", "ORGANIZATION", "MY_PROJECTS", "MY_ISSUES");
    assertThat(typeParam.deprecatedSince()).isNull();
    assertThat(typeParam.deprecatedKey()).isNull();

    WebService.Param keyParam = action.param("value");
    assertThat(keyParam.isRequired()).isFalse();
    assertThat(keyParam.description()).isEqualTo("Additional information to filter the page (project or organization key)");
    assertThat(keyParam.exampleValue()).isEqualTo("my-project-key");
    assertThat(keyParam.defaultValue()).isNull();
    assertThat(keyParam.deprecatedSince()).isNull();
    assertThat(keyParam.deprecatedKey()).isNull();
  }


  @Test
  public void set_project_homepage() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = new ComponentDbTester(db).insertComponent(ComponentTesting.newPrivateProjectDto(organization));

    UserDto user = db.users().insertUser();
    session.logIn(user);

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TYPE, PROJECT.toString())
      .setParam(PARAM_VALUE, project.getKey())
      .execute();

    UserDto actual = db.getDbClient().userDao().selectByLogin(db.getSession(), user.getLogin());
    assertThat(actual).isNotNull();
    assertThat(actual.getHomepageType()).isEqualTo(PROJECT.toString());
    assertThat(actual.getHomepageKey()).isEqualTo(project.uuid());
  }

  @Test
  public void set_organization_homepage() {
    OrganizationDto organization = db.organizations().insert();

    UserDto user = db.users().insertUser();
    session.logIn(user);

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TYPE, ORGANIZATION.toString())
      .setParam(PARAM_VALUE, organization.getKey())
      .execute();

    UserDto actual = db.getDbClient().userDao().selectByLogin(db.getSession(), user.getLogin());
    assertThat(actual).isNotNull();
    assertThat(actual.getHomepageType()).isEqualTo(ORGANIZATION.toString());
    assertThat(actual.getHomepageKey()).isEqualTo(organization.getUuid());
  }

  @Test
  public void set_my_issues_homepage() {
    UserDto user = db.users().insertUser();
    session.logIn(user);

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TYPE, MY_ISSUES.toString())
      .execute();

    UserDto actual = db.getDbClient().userDao().selectByLogin(db.getSession(), user.getLogin());
    assertThat(actual).isNotNull();
    assertThat(actual.getHomepageType()).isEqualTo(MY_ISSUES.toString());
    assertThat(actual.getHomepageKey()).isNullOrEmpty();
  }

  @Test
  public void set_my_projects_homepage() {
    UserDto user = db.users().insertUser();
    session.logIn(user);

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TYPE, MY_PROJECTS.toString())
      .execute();

    UserDto actual = db.getDbClient().userDao().selectByLogin(db.getSession(), user.getLogin());
    assertThat(actual).isNotNull();
    assertThat(actual.getHomepageType()).isEqualTo(MY_PROJECTS.toString());
    assertThat(actual.getHomepageKey()).isNullOrEmpty();
  }

  @Test
  public void response_has_no_content() {
    UserDto user = db.users().insertUser();
    session.logIn(user);

    TestResponse response = ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TYPE, MY_PROJECTS.toString())
      .execute();

    assertThat(response.getStatus()).isEqualTo(SC_NO_CONTENT);
    assertThat(response.getInput()).isEmpty();
  }

  @Test
  public void fail_when_missing_project_id_when_requesting_project_type() {

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("type PROJECT requires a mandatory project key");

    UserDto user = db.users().insertUser();
    session.logIn(user);

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TYPE, PROJECT.toString())
      .setParam(PARAM_VALUE, "")
      .execute();

  }

  @Test
  public void fail_when_missing_organization_id_when_requesting_organization_type() {

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("type ORGANIZATION requires a mandatory project key");

    UserDto user = db.users().insertUser();
    session.logIn(user);

    ws.newRequest()
      .setMethod("POST")
      .setParam(PARAM_TYPE, ORGANIZATION.toString())
      .setParam(PARAM_VALUE, "")
      .execute();

  }

  @Test
  public void fail_for_anonymous() {
    session.anonymous();
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    ws.newRequest().setMethod("POST").execute();
  }
}