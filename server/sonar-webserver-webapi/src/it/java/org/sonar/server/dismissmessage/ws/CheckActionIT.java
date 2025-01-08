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
package org.sonar.server.dismissmessage.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ProjectData;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.db.user.UserDismissedMessageDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.test.JsonAssert.assertJson;

public class CheckActionIT {
  private static final String PROJECT_KEY = "project-key";
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final WsActionTester underTest = new WsActionTester(new CheckAction(userSession, db.getDbClient(), TestComponentFinder.from(db)));

  @Test
  public void definition() {
    WebService.Action def = underTest.getDef();
    assertThat(def.key()).isEqualTo("check");
    assertThat(def.isInternal()).isTrue();
    assertThat(def.params()).extracting(WebService.Param::key, WebService.Param::isRequired).containsOnly(
      tuple("projectKey", false),
      tuple("messageType", true));
    assertThat(def.responseExampleAsString()).isNotEmpty();
  }

  @Test
  public void return_true_when_user_dismissed_message() {
    UserDto user = db.users().insertUser();
    ProjectData project = db.components().insertPrivateProject();
    userSession.logIn(user);

    db.getDbClient().userDismissedMessagesDao().insert(db.getSession(), new UserDismissedMessageDto()
      .setUuid("uuid")
      .setUserUuid(user.getUuid())
      .setProjectUuid(project.getProjectDto().getUuid())
      .setMessageType(MessageType.BRANCH_NCD_90)
      .setCreatedAt(1_500_000_000_000L));
    db.commit();

    TestResponse response = underTest.newRequest()
      .setParam("projectKey", project.projectKey())
      .setParam("messageType", MessageType.BRANCH_NCD_90.name())
      .execute();

    assertThat(response.getStatus()).isEqualTo(200);
    assertJson(response.getInput()).isSimilarTo("{\"dismissed\": true}");
  }

  @Test
  public void return_false_when_user_has_not_dismissed_message() {
    UserDto user = db.users().insertUser();
    ProjectData project = db.components().insertPrivateProject();
    userSession.logIn(user);

    TestResponse response = underTest.newRequest()
      .setParam("projectKey", project.projectKey())
      .setParam("messageType", MessageType.BRANCH_NCD_90.name())
      .execute();

    assertThat(response.getStatus()).isEqualTo(200);
    assertJson(response.getInput()).isSimilarTo("{\"dismissed\": false}");
  }

  @Test
  public void throw_IAE_if_project_not_found() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    TestRequest request = underTest.newRequest()
      .setParam("projectKey", PROJECT_KEY)
      .setParam("messageType", MessageType.BRANCH_NCD_90.name());

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project 'project-key' not found");
  }

  @Test
  public void return_401_if_user_is_not_logged_in() {
    userSession.anonymous();
    TestRequest request = underTest.newRequest()
      .setParam("projectKey", PROJECT_KEY)
      .setParam("messageType", MessageType.PROJECT_NCD_90.name());

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  @Test
  public void throw_IAE_if_messageType_is_not_valid() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    TestRequest request = underTest.newRequest()
      .setParam("projectKey", PROJECT_KEY)
      .setParam("messageType", "invalid");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid message type: invalid");
  }

}
