/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.project.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.server.component.ComponentCleanerService;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProjectsWsTest {

  WebService.Controller controller;
  WsTester ws;

  @Before
  public void setUp() {
    ws = new WsTester(new ProjectsWs(
      new BulkDeleteAction(mock(ComponentCleanerService.class), mock(DbClient.class), mock(UserSession.class)),
      new GhostsAction(mock(DbClient.class), mock(UserSession.class)),
      new ProvisionedAction(mock(DbClient.class), mock(UserSession.class))));
    controller = ws.controller("api/projects");
  }

  @Test
  public void define_controller() {
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.since()).isEqualTo("2.10");
    assertThat(controller.actions()).hasSize(4);
  }

  @Test
  public void define_index_action() {
    WebService.Action action = controller.action("index");
    assertThat(action).isNotNull();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(8);
  }

}
