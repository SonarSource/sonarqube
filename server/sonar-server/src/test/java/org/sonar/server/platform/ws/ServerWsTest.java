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
package org.sonar.server.platform.ws;

import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerWsTest {

  private Server server = mock(Server.class);
  private WsTester tester = new WsTester(new ServerWs(server));

  @Test
  public void define_version_action() {
    WebService.Controller controller = tester.controller("api/server");
    assertThat(controller.actions()).hasSize(1);

    WebService.Action versionAction = controller.action("version");
    assertThat(versionAction.since()).isEqualTo("2.10");
    assertThat(versionAction.description()).isNotEmpty();
    assertThat(versionAction.isPost()).isFalse();
  }

  @Test
  public void returns_version_as_plain_text() throws Exception {
    when(server.getVersion()).thenReturn("6.4-SNAPSHOT");
    WsTester.Result result = tester.newGetRequest("api/server", "version").execute();
    assertThat(result.outputAsString()).isEqualTo("6.4-SNAPSHOT");
  }

  @Test
  public void test_example_of_version() {
    WebService.Action versionAction = tester.action("api/server", "version");
    assertThat(versionAction.responseExampleAsString()).isEqualTo("6.3.0.1234");
  }
}
