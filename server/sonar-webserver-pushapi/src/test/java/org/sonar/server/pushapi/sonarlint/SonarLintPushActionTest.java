/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.server.pushapi.sonarlint;

import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

public class SonarLintPushActionTest {

  private final WsActionTester ws = new WsActionTester(new SonarLintPushAction());

  @Test
  public void defineTest() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("9.4");
    assertThat(def.isInternal()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("languages", true), tuple("projectKeys", true));
  }

  @Test
  public void handle_returnsNoResponseWhenParamsProvided() {
    TestResponse response = ws.newRequest()
      .setParam("projectKeys", "project1,project2")
      .setParam("languages", "java")
      .execute();

    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  public void handle_whenParamsNotProvided_throwException() {
    TestRequest testRequest = ws.newRequest();
    assertThatThrownBy(testRequest::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'projectKeys' parameter is missing");
  }
}
