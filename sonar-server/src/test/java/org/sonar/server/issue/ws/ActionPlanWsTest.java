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

package org.sonar.server.issue.ws;

import org.junit.Test;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;

public class ActionPlanWsTest {

  WsTester tester = new WsTester(new ActionPlanWs());

  @Test
  public void define_action_plan_controller() throws Exception {
    WebService.Controller controller = tester.controller("api/action_plans");
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(6);
  }

  @Test
  public void define_search_action() throws Exception {
    WebService.Controller controller = tester.controller("api/action_plans");

    WebService.Action action = controller.action("search");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.since()).isEqualTo("3.6");
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(1);

    WebService.Param project = action.param("project");
    assertThat(project).isNotNull();
    assertThat(project.description()).isNotNull();
    assertThat(project.exampleValue()).isNotNull();
    assertThat(project.isRequired()).isTrue();
  }

  @Test
  public void define_create_action() throws Exception {
    WebService.Controller controller = tester.controller("api/action_plans");

    WebService.Action action = controller.action("create");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.since()).isEqualTo("3.6");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(4);

    WebService.Param name = action.param("name");
    assertThat(name).isNotNull();
    assertThat(name.description()).isNotNull();
    assertThat(name.exampleValue()).isNotNull();
    assertThat(name.isRequired()).isTrue();

    WebService.Param description = action.param("description");
    assertThat(description).isNotNull();
    assertThat(description.description()).isNotNull();
    assertThat(description.isRequired()).isFalse();

    WebService.Param project = action.param("project");
    assertThat(project).isNotNull();
    assertThat(project.description()).isNotNull();
    assertThat(project.exampleValue()).isNotNull();
    assertThat(project.isRequired()).isTrue();

    WebService.Param deadLine = action.param("deadLine");
    assertThat(deadLine).isNotNull();
    assertThat(deadLine.description()).isNotNull();
    assertThat(deadLine.exampleValue()).isNotNull();
    assertThat(deadLine.isRequired()).isFalse();
  }

  @Test
  public void define_delete_action() throws Exception {
    WebService.Controller controller = tester.controller("api/action_plans");

    WebService.Action action = controller.action("delete");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.since()).isEqualTo("3.6");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(1);

    WebService.Param key = action.param("key");
    assertThat(key).isNotNull();
    assertThat(key.description()).isNotNull();
    assertThat(key.exampleValue()).isNotNull();
    assertThat(key.isRequired()).isTrue();
  }

  @Test
  public void define_update_action() throws Exception {
    WebService.Controller controller = tester.controller("api/action_plans");

    WebService.Action action = controller.action("update");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.since()).isEqualTo("3.6");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(4);

    WebService.Param project = action.param("key");
    assertThat(project).isNotNull();
    assertThat(project.description()).isNotNull();
    assertThat(project.exampleValue()).isNotNull();
    assertThat(project.isRequired()).isTrue();

    WebService.Param name = action.param("name");
    assertThat(name).isNotNull();
    assertThat(name.description()).isNotNull();
    assertThat(name.exampleValue()).isNotNull();
    assertThat(name.isRequired()).isTrue();

    WebService.Param description = action.param("description");
    assertThat(description).isNotNull();
    assertThat(description.description()).isNotNull();
    assertThat(description.isRequired()).isFalse();

    WebService.Param deadLine = action.param("deadLine");
    assertThat(deadLine).isNotNull();
    assertThat(deadLine.description()).isNotNull();
    assertThat(deadLine.exampleValue()).isNotNull();
    assertThat(deadLine.isRequired()).isFalse();
  }

  @Test
  public void define_open_action() throws Exception {
    WebService.Controller controller = tester.controller("api/action_plans");

    WebService.Action action = controller.action("open");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.since()).isEqualTo("3.6");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(1);

    WebService.Param key = action.param("key");
    assertThat(key).isNotNull();
    assertThat(key.description()).isNotNull();
    assertThat(key.exampleValue()).isNotNull();
    assertThat(key.isRequired()).isTrue();
  }

  @Test
  public void define_close_action() throws Exception {
    WebService.Controller controller = tester.controller("api/action_plans");

    WebService.Action action = controller.action("close");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.since()).isEqualTo("3.6");
    assertThat(action.isPost()).isTrue();
    assertThat(action.isInternal()).isFalse();
    assertThat(action.params()).hasSize(1);

    WebService.Param key = action.param("key");
    assertThat(key).isNotNull();
    assertThat(key.description()).isNotNull();
    assertThat(key.exampleValue()).isNotNull();
    assertThat(key.isRequired()).isTrue();
  }
}
