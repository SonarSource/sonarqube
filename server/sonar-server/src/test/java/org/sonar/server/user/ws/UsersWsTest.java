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
package org.sonar.server.user.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.server.issue.ws.AvatarResolver;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class UsersWsTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private WebService.Controller controller;

  @Before
  public void setUp() {
    WsTester tester = new WsTester(new UsersWs(
      new CreateAction(mock(DbClient.class), mock(UserUpdater.class), userSessionRule),
      new UpdateAction(mock(UserUpdater.class), userSessionRule, mock(UserJsonWriter.class), mock(DbClient.class)),
      new CurrentAction(userSessionRule, mock(DbClient.class), mock(DefaultOrganizationProvider.class), mock(AvatarResolver.class)),
      new ChangePasswordAction(mock(DbClient.class), mock(UserUpdater.class), userSessionRule),
      new SearchAction(userSessionRule, mock(UserIndex.class), mock(DbClient.class), mock(AvatarResolver.class))));
    controller = tester.controller("api/users");
  }

  @Test
  public void define_controller() {
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.since()).isEqualTo("3.6");
    assertThat(controller.actions()).hasSize(5);
  }

  @Test
  public void define_search_action() {
    WebService.Action action = controller.action("search");
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(4);
  }

  @Test
  public void define_create_action() {
    WebService.Action action = controller.action("create");
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isTrue();
    assertThat(action.params()).hasSize(7);
  }

  @Test
  public void define_update_action() {
    WebService.Action action = controller.action("update");
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isTrue();
    assertThat(action.params()).hasSize(5);
  }

  @Test
  public void define_change_password_action() {
    WebService.Action action = controller.action("change_password");
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isTrue();
    assertThat(action.params()).hasSize(3);
  }

  @Test
  public void define_current_action() {
    WebService.Action action = controller.action("current");
    assertThat(action).isNotNull();
    assertThat(action.isPost()).isFalse();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.params()).isEmpty();
  }
}
