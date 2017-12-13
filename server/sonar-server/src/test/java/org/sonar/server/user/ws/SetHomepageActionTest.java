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

import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;

public class SetHomepageActionTest {

  private SetHomepageAction underTest = new SetHomepageAction();
  private WsActionTester wsTester = new WsActionTester(underTest);

  @Test
  public void verify_definition() {
    WebService.Action action = wsTester.getDef();
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


}