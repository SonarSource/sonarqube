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
package org.sonar.server.issue.filter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class IssueFilterWsTest {

  @Mock
  IssueFilterService service;

  @Mock
  IssueFilterWriter issueFilterWriter;

  IssueFilterWs ws;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    ws = new IssueFilterWs(new AppAction(service, issueFilterWriter), new ShowAction(service, issueFilterWriter), new FavoritesAction(service));
    tester = new WsTester(ws);
  }

  @Test
  public void define_ws() throws Exception {
    WebService.Controller controller = tester.controller("api/issue_filters");
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.since()).isEqualTo("4.2");

    WebService.Action app = controller.action("app");
    assertThat(app).isNotNull();
    assertThat(app.params()).hasSize(1);

    WebService.Action show = controller.action("show");
    assertThat(show).isNotNull();
    assertThat(show.responseExampleAsString()).isNotEmpty();
    assertThat(show.params()).hasSize(1);

    WebService.Action favorites = controller.action("favorites");
    assertThat(favorites).isNotNull();
    assertThat(favorites.params()).isEmpty();
  }

}
