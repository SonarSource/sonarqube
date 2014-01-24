/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.rule.ws;

import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class RulesWsTest {

  RuleShowWsHandler showHandler = mock(RuleShowWsHandler.class);
  WsTester tester = new WsTester(new RulesWs(showHandler));

  @Test
  public void define_ws() throws Exception {
    WebService.Controller controller = tester.controller("api/rules");
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo("api/rules");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(2);

    WebService.Action search = controller.action("list");
    assertThat(search).isNotNull();
    assertThat(search.handler()).isNotNull();
    assertThat(search.since()).isEqualTo("4.2");
    assertThat(search.isPost()).isFalse();
    assertThat(search.isPrivate()).isFalse();

    WebService.Action show = controller.action("show");
    assertThat(show).isNotNull();
    assertThat(show.handler()).isNotNull();
    assertThat(show.since()).isEqualTo("4.2");
    assertThat(show.isPost()).isFalse();
    assertThat(show.isPrivate()).isFalse();
  }

  @Test
  public void search_for_rules() throws Exception {
    tester.newRequest("list").execute().assertJson(getClass(), "list.json");
  }

}
