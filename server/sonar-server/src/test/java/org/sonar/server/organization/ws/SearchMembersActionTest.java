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

package org.sonar.server.organization.ws;

import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchMembersActionTest {

  private WsActionTester ws = new WsActionTester(new SearchMembersAction());

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();

    assertThat(action.key()).isEqualTo("search_members");
    assertThat(action.params()).extracting(WebService.Param::key)
      .containsOnly("q", "selected", "p", "ps", "organization");
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.since()).isEqualTo("6.4");
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
  }
}
