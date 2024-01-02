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
package org.sonar.server.user.ws;

import java.util.List;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.ServletFilterHandler;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class UsersWsTest {
  private static final TestAction TEST_ACTION_1 = new TestAction();
  private static final TestAction TEST_ACTION_2 = new TestAction();

  private final UsersWs underTest = new UsersWs(List.of(TEST_ACTION_1, TEST_ACTION_2));

  @Test
  public void define_ws() {
    WebService.Context context = new WebService.Context();

    underTest.define(context);

    WebService.Controller controller = context.controller(UsersWs.API_USERS);
    assertThat(controller).isNotNull();
    assertThat(controller.since()).isEqualTo(UsersWs.SINCE_VERSION);
    assertThat(controller.description()).isEqualTo(UsersWs.DESCRIPTION);
  }

  private static class TestAction implements BaseUsersWsAction {

    @Override
    public void define(WebService.NewController context) {
      context.createAction(randomAlphanumeric(10)).setHandler(ServletFilterHandler.INSTANCE);
    }

  }
}
