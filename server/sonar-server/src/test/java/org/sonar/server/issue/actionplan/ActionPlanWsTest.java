/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.server.issue.actionplan;

import org.junit.Test;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;

public class ActionPlanWsTest {

  WsTester tester = new WsTester(new ActionPlanWs());

  @Test
  public void define_controller() {
    WebService.Controller controller = tester.controller("api/action_plans");
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(6);
  }

  @Test
  public void define_search_action() {
    WebService.Controller controller = tester.controller("api/action_plans");

    WebService.Action action = controller.action("search");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(2);
  }

  @Test
  public void define_create_action() {
    WebService.Controller controller = tester.controller("api/action_plans");

    WebService.Action action = controller.action("create");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.params()).hasSize(5);
  }

  @Test
  public void define_delete_action() {
    WebService.Controller controller = tester.controller("api/action_plans");

    WebService.Action action = controller.action("delete");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.params()).hasSize(2);
  }

  @Test
  public void define_update_action() {
    WebService.Controller controller = tester.controller("api/action_plans");

    WebService.Action action = controller.action("update");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.params()).hasSize(5);
  }

  @Test
  public void define_open_action() {
    WebService.Controller controller = tester.controller("api/action_plans");

    WebService.Action action = controller.action("open");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.params()).hasSize(2);
  }

  @Test
  public void define_close_action() {
    WebService.Controller controller = tester.controller("api/action_plans");

    WebService.Action action = controller.action("close");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.params()).hasSize(2);
  }
}
