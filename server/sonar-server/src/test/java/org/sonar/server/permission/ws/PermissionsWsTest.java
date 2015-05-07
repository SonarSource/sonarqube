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

package org.sonar.server.permission.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionsWsTest {

  WsTester tester;

  @Before
  public void setUp() {
    tester = new WsTester(new PermissionsWs());
  }

  @Test
  public void define_controller() {
    WebService.Controller controller = tester.controller("api/permissions");
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.since()).isEqualTo("3.7");
    assertThat(controller.actions()).hasSize(2);
  }

  @Test
  public void define_add_action() {
    WebService.Controller controller = tester.controller("api/permissions");

    WebService.Action action = controller.action("add");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.INSTANCE.getClass());
    assertThat(action.params()).hasSize(5);

    WebService.Param permission = action.param("permission");
    assertThat(permission).isNotNull();
    assertThat(permission.possibleValues()).hasSize(9);
  }

  @Test
  public void define_remove_action() {
    WebService.Controller controller = tester.controller("api/permissions");

    WebService.Action action = controller.action("remove");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.INSTANCE.getClass());
    assertThat(action.params()).hasSize(5);
  }
}
