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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IssueFilterWsTest {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  IssueFilterWs underTest;
  WsTester ws;

  @Before
  public void setUp() {
    IssueFilterService service = mock(IssueFilterService.class);
    DbClient dbClient = mock(DbClient.class);
    underTest = new IssueFilterWs(
      new AppAction(service, userSession),
      new ShowAction(service, userSession),
      new SearchAction(dbClient, userSession),
      new FavoritesAction(service, userSession));
    ws = new WsTester(underTest);
  }

  @Test
  public void define_ws() {
    WebService.Controller controller = ws.controller("api/issue_filters");
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

    WebService.Action search = controller.action("search");
    assertThat(search).isNotNull();
    assertThat(search.params()).isEmpty();
  }

}
