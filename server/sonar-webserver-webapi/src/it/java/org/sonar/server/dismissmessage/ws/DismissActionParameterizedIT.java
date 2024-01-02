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
package org.sonar.server.dismissmessage.ws;

import java.util.Arrays;
import java.util.Collection;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.db.component.ProjectData;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(Parameterized.class)
public class DismissActionParameterizedIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Nullable
  private final boolean isProjectKeyNull;

  private final MessageType messageType;

  private final Class<? extends Throwable> expectedException;
  private final WsActionTester underTest = new WsActionTester(new DismissAction(userSession, db.getDbClient(), TestComponentFinder.from(db)));

  public DismissActionParameterizedIT(boolean isProjectKeyNull, MessageType messageType, Class<? extends Throwable> expectedException) {
    this.isProjectKeyNull = isProjectKeyNull;
    this.messageType = messageType;
    this.expectedException = expectedException;
  }

  @Test
  public void test_verifyProjectKeyAndMessageType() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    TestRequest request = underTest.newRequest()
      .setParam("messageType", messageType.name());

    if(!isProjectKeyNull) {
      ProjectData project = db.components().insertPrivateProject();
      request.setParam("projectKey", project.projectKey());
    }

    if(expectedException != null) {
      assertThatThrownBy(request::execute)
        .isInstanceOf(expectedException);
    }  else {
      TestResponse response = request.execute();
      assertThat(response.getStatus()).isEqualTo(204);
    }
  }

  @Parameterized.Parameters
  public static Collection<Object[]> parameterCombination() {
    return Arrays.asList(new Object[][]{
      {true, MessageType.INFO, IllegalArgumentException.class},
      {true, MessageType.GENERIC, IllegalArgumentException.class},
      {true, MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE, IllegalArgumentException.class},
      {false, MessageType.GLOBAL_NCD_90, IllegalArgumentException.class},
      {false, MessageType.GLOBAL_NCD_PAGE_90, IllegalArgumentException.class},
      {true, MessageType.PROJECT_NCD_90, IllegalArgumentException.class},
      {true, MessageType.PROJECT_NCD_PAGE_90, IllegalArgumentException.class},
      {true, MessageType.BRANCH_NCD_90, IllegalArgumentException.class},
      {true, MessageType.GLOBAL_NCD_90, null},
      {true, MessageType.GLOBAL_NCD_PAGE_90, null},
      {false, MessageType.PROJECT_NCD_90, null},
      {false, MessageType.PROJECT_NCD_PAGE_90, null},
      {false, MessageType.BRANCH_NCD_90, null},
    });
  }
}
