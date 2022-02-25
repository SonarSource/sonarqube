/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.ws;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.DumbResponse;
import org.sonar.server.ws.TestResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerWsTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private Server server = mock(Server.class);
  private ServerWs underTest = new ServerWs(server, userSessionRule);

  @Test
  public void define_version_action() {
    WebService.Context context = new WebService.Context();

    underTest.define(context);

    WebService.Controller controller = context.controller("api/server");
    assertThat(controller.actions()).hasSize(1);

    WebService.Action versionAction = controller.action("version");
    assertThat(versionAction.since()).isEqualTo("2.10");
    assertThat(versionAction.description()).isNotEmpty();
    assertThat(versionAction.isPost()).isFalse();
    assertThat(versionAction.changelog()).isNotEmpty();
  }

  @Test
  public void require_authentication() {
    DumbResponse response = new DumbResponse();
    Assertions.assertThatThrownBy(() -> underTest.handle(mock(Request.class), response))
      .hasMessage("Authentication is required")
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void returns_version_as_plain_text() throws Exception {
    userSessionRule.logIn();
    when(server.getVersion()).thenReturn("6.4-SNAPSHOT");

    DumbResponse response = new DumbResponse();
    underTest.handle(mock(Request.class), response);

    assertThat(new TestResponse(response).getInput()).isEqualTo("6.4-SNAPSHOT");
  }

  @Test
  public void test_example_of_version() {
    userSessionRule.logIn();
    WebService.Context context = new WebService.Context();
    underTest.define(context);

    WebService.Action action = context.controller("api/server").action("version");
    assertThat(action).isNotNull();
    assertThat(action.responseExampleAsString()).isEqualTo("6.3.0.1234");
  }
}
