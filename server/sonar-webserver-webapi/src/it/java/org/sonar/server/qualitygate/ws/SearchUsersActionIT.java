/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.qualitygate.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.common.avatar.AvatarResolver;
import org.sonar.server.common.avatar.AvatarResolverImpl;
import org.sonar.server.issue.FakeAvatarResolver;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualitygates.SearchUsersResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_NAME;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.MediaTypes.JSON;

public class SearchUsersActionIT {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final QualityGatesWsSupport wsSupport = new QualityGatesWsSupport(db.getDbClient(), userSession, TestComponentFinder.from(db));
  private AvatarResolver avatarResolver = new FakeAvatarResolver();

  private WsActionTester ws = new WsActionTester(new SearchUsersAction(db.getDbClient(), wsSupport, avatarResolver));

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("search_users");
    assertThat(def.isPost()).isFalse();
    assertThat(def.params()).extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("gateName", "selected", "q", "p", "ps");
  }

  @Test
  public void test_example() {
    avatarResolver = new AvatarResolverImpl();
    ws = new WsActionTester(new SearchUsersAction(db.getDbClient(), wsSupport, avatarResolver));
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    UserDto user1 = db.users().insertUser(u -> u.setLogin("admin").setName("Administrator").setEmail("admin@email.com"));
    UserDto user2 = db.users().insertUser(u -> u.setLogin("george.orwell").setName("George Orwell").setEmail("george@orwell.com"));
    db.qualityGates().addUserPermission(gate, user1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    String result = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(SELECTED, "all")
      .setMediaType(JSON)
      .execute()
      .getInput();

    assertJson(result).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void search_all_users() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    UserDto user1 = db.users().insertUser(u -> u.setEmail("user1@email.com"));
    UserDto user2 = db.users().insertUser(u -> u.setEmail("user2@email.com"));
    db.qualityGates().addUserPermission(gate, user1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList())
      .extracting(SearchUsersResponse.User::getLogin, SearchUsersResponse.User::getName, SearchUsersResponse.User::getAvatar, SearchUsersResponse.User::getSelected)
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), user1.getName(), "user1@email.com_avatar", true),
        tuple(user2.getLogin(), user2.getName(), "user2@email.com_avatar", false));
  }

  @Test
  public void search_selected_users() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.qualityGates().addUserPermission(gate, user1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(SELECTED, "selected")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList()).extracting(SearchUsersResponse.User::getLogin, SearchUsersResponse.User::getName, SearchUsersResponse.User::getSelected)
      .containsExactlyInAnyOrder(
        tuple(user1.getLogin(), user1.getName(), true));
  }

  @Test
  public void search_deselected_users() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.qualityGates().addUserPermission(gate, user1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(SELECTED, "deselected")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList()).extracting(SearchUsersResponse.User::getLogin, SearchUsersResponse.User::getName, SearchUsersResponse.User::getSelected)
      .containsExactlyInAnyOrder(
        tuple(user2.getLogin(), user2.getName(), false));
  }

  @Test
  public void search_by_login() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.qualityGates().addUserPermission(gate, user1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(TEXT_QUERY, user1.getLogin())
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList()).extracting(SearchUsersResponse.User::getLogin)
      .containsExactlyInAnyOrder(user1.getLogin());
  }

  @Test
  public void search_by_name() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    UserDto user1 = db.users().insertUser(u -> u.setName("John Doe"));
    UserDto user2 = db.users().insertUser(u -> u.setName("Jane Doe"));
    UserDto user3 = db.users().insertUser(u -> u.setName("John Smith"));
    db.qualityGates().addUserPermission(gate, user1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(TEXT_QUERY, "ohn")
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList()).extracting(SearchUsersResponse.User::getLogin)
      .containsExactlyInAnyOrder(user1.getLogin(), user3.getLogin());
  }

  @Test
  public void user_without_email() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser(u -> u.setEmail(null));
    db.qualityGates().addUserPermission(gate, user);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList()).extracting(SearchUsersResponse.User::getLogin, SearchUsersResponse.User::hasAvatar)
      .containsExactlyInAnyOrder(tuple(user.getLogin(), false));
  }

  @Test
  public void paging_search() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    UserDto user2 = db.users().insertUser(u -> u.setName("user2"));
    UserDto user3 = db.users().insertUser(u -> u.setName("user3"));
    UserDto user1 = db.users().insertUser(u -> u.setName("user1"));
    db.qualityGates().addUserPermission(gate, user1);
    db.qualityGates().addUserPermission(gate, user2);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    assertThat(ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(SELECTED, "all")
      .setParam(PAGE, "1")
      .setParam(PAGE_SIZE, "1")
      .executeProtobuf(SearchUsersResponse.class).getUsersList())
      .extracting(SearchUsersResponse.User::getLogin)
      .containsExactly(user1.getLogin());

    assertThat(ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(SELECTED, "all")
      .setParam(PAGE, "3")
      .setParam(PAGE_SIZE, "1")
      .executeProtobuf(SearchUsersResponse.class).getUsersList())
      .extracting(SearchUsersResponse.User::getLogin)
      .containsExactly(user3.getLogin());

    assertThat(ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(SELECTED, "all")
      .setParam(PAGE, "1")
      .setParam(PAGE_SIZE, "10")
      .executeProtobuf(SearchUsersResponse.class).getUsersList())
      .extracting(SearchUsersResponse.User::getLogin)
      .containsExactly(user1.getLogin(), user2.getLogin(), user3.getLogin());
  }

  @Test
  public void uses_global_permission() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    UserDto user1 = db.users().insertUser();
    db.qualityGates().addUserPermission(gate, user1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList()).extracting(SearchUsersResponse.User::getLogin).containsExactlyInAnyOrder(user1.getLogin());
  }

  @Test
  public void qp_administers_can_search_users() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList()).extracting(SearchUsersResponse.User::getLogin).containsExactlyInAnyOrder(user.getLogin());
  }

  @Test
  public void qp_editors_can_search_users() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();
    UserDto userAllowedToEditProfile = db.users().insertUser();
    db.qualityGates().addUserPermission(gate, userAllowedToEditProfile);
    userSession.logIn(userAllowedToEditProfile);

    SearchUsersResponse response = ws.newRequest()
      .setParam(PARAM_GATE_NAME, gate.getName())
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchUsersResponse.class);

    assertThat(response.getUsersList()).extracting(SearchUsersResponse.User::getLogin).containsExactlyInAnyOrder(user.getLogin(), userAllowedToEditProfile.getLogin());
  }

  @Test
  public void fail_when_quality_gate_does_not_exist() {
    UserDto user = db.users().insertUser();
    userSession.logIn().addPermission(ADMINISTER_QUALITY_GATES);

    TestRequest request = ws.newRequest().setParam(PARAM_GATE_NAME, "unknown");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("No quality gate has been found for name unknown");
  }


  @Test
  public void fail_when_not_enough_permission() {
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();
    userSession.logIn(db.users().insertUser());

    TestRequest request = ws.newRequest().setParam(PARAM_GATE_NAME, gate.getName());

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }
}
