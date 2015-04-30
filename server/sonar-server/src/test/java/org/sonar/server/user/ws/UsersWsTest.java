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

package org.sonar.server.user.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.i18n.I18n;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class UsersWsTest {

  WebService.Controller controller;

  @Before
  public void setUp() throws Exception {
    WsTester tester = new WsTester(new UsersWs(
      new CreateAction(mock(UserIndex.class), mock(UserUpdater.class), mock(I18n.class)),
      new UpdateAction(mock(UserIndex.class), mock(UserUpdater.class)),
      new CurrentUserAction()));
    controller = tester.controller("api/users");
  }

  @Test
  public void define_controller() throws Exception {
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.since()).isEqualTo("3.6");
    assertThat(controller.actions()).hasSize(5);
  }

  @Test
  public void define_search_action() throws Exception {
    WebService.Action action = controller.action("search");
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(4);
  }

  @Test
  public void define_create_action() throws Exception {
    WebService.Action action = controller.action("create");
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isTrue();
    assertThat(action.params()).hasSize(6);
  }

  @Test
  public void define_update_action() throws Exception {
    WebService.Action action = controller.action("update");
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isTrue();
    assertThat(action.params()).hasSize(6);
  }

  @Test
  public void define_deactivate_action() throws Exception {
    WebService.Action action = controller.action("deactivate");
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isTrue();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.params()).hasSize(2);
  }

  @Test
  public void define_current_action() throws Exception {
    WebService.Action action = controller.action("current");
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.params()).isEmpty();
  }
}
