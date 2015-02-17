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

package org.sonar.server.component.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.i18n.I18n;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Durations;
import org.sonar.server.db.DbClient;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ComponentsWsTest {

  WebService.Controller controller;

  @Before
  public void setUp() throws Exception {
    WsTester tester = new WsTester(new ComponentsWs(new ComponentAppAction(mock(DbClient.class), mock(Durations.class), mock(I18n.class)), new SearchAction(mock(DbClient.class))));
    controller = tester.controller("api/components");
  }

  @Test
  public void define_controller() throws Exception {
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.since()).isEqualTo("4.2");
    assertThat(controller.actions()).hasSize(3);
  }

  @Test
  public void define_suggestions_action() throws Exception {
    WebService.Action action = controller.action("suggestions");
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isInstanceOf(RailsHandler.class);
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).hasSize(2);
  }

  @Test
  public void define_app_action() throws Exception {
    WebService.Action action = controller.action("app");
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.params()).hasSize(2);
  }

  @Test
  public void define_search_action() throws Exception {
    WebService.Action action = controller.action("search");
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.handler()).isNotNull();
    assertThat(action.params()).hasSize(4);
  }

}
