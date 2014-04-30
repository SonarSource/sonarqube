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
import org.sonar.api.i18n.I18n;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Durations;
import org.sonar.server.debt.DebtModelService;
import org.sonar.server.issue.ActionService;
import org.sonar.server.issue.IssueChangelogService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IssuesWsTest {

  IssueShowAction showAction = new IssueShowAction(mock(IssueFinder.class), mock(IssueService.class), mock(IssueChangelogService.class), mock(ActionService.class),
    mock(DebtModelService.class), mock(I18n.class), mock(Durations.class));
  WsTester tester = new WsTester(new IssuesWs(showAction));

  @Test
  public void define_controller() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.since()).isEqualTo("3.6");
    assertThat(controller.actions()).hasSize(2);
  }

  @Test
  public void define_show_action() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");

    WebService.Action show = controller.action("show");
    assertThat(show).isNotNull();
    assertThat(show.handler()).isNotNull();
    assertThat(show.since()).isEqualTo("4.2");
    assertThat(show.isPost()).isFalse();
    assertThat(show.isInternal()).isTrue();
    assertThat(show.handler()).isSameAs(showAction);

    WebService.Param key = show.param("key");
    assertThat(key).isNotNull();
    assertThat(key.description()).isNotNull();
    assertThat(key.isRequired()).isFalse();
  }

  @Test
  public void define_search_action() throws Exception {
    WebService.Controller controller = tester.controller("api/issues");

    WebService.Action show = controller.action("search");
    assertThat(show).isNotNull();
    assertThat(show.handler()).isNotNull();
    assertThat(show.since()).isEqualTo("3.6");
    assertThat(show.isPost()).isFalse();
    assertThat(show.isInternal()).isFalse();
    assertThat(show.handler()).isInstanceOf(RailsHandler.class);
    assertThat(show.params()).hasSize(2);
  }

}
