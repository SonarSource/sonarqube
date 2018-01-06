/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.projectlink.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProjectLinksWsTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  WebService.Controller controller;

  @Before
  public void setUp() {
    WsTester tester = new WsTester(new ProjectLinksWs(
      new SearchAction(mock(DbClient.class), userSessionRule, mock(ComponentFinder.class)),
      new CreateAction(mock(DbClient.class), userSessionRule, mock(ComponentFinder.class)),
      new DeleteAction(mock(DbClient.class), userSessionRule)
    ));
    controller = tester.controller("api/project_links");
  }

  @Test
  public void define_controller() {
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.since()).isEqualTo("6.1");
    assertThat(controller.actions()).hasSize(3);
  }

  @Test
  public void define_search_action() {
    WebService.Action action = controller.action("search");
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(2);
  }

  @Test
  public void define_create_action() {
    WebService.Action action = controller.action("create");
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isTrue();
    assertThat(action.handler()).isNotNull();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(4);
  }

  @Test
  public void define_delete_action() {
    WebService.Action action = controller.action("delete");
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isTrue();
    assertThat(action.handler()).isNotNull();
    assertThat(action.responseExample()).isNull();
    assertThat(action.params()).hasSize(1);
  }
}
