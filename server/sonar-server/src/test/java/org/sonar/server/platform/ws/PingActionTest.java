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
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;

public class PingActionTest {

  private PingAction underTest = new PingAction();
  private WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void test_definition() {
    WebService.Action action = tester.getDef();

    assertThat(action.key()).isEqualTo("ping");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).isEmpty();
  }

  @Test
  public void returns_pong_as_plain_text() {
    TestResponse response = tester.newRequest().execute();

    assertThat(response.getMediaType()).isEqualTo("text/plain");
    assertThat(response.getInput()).isEqualTo("pong");
    assertThat(response.getInput()).isEqualTo(tester.getDef().responseExampleAsString());
  }
}
