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

import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WsTester;
import org.sonar.core.issue.DefaultIssueFilter;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.MockUserSession;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IssueFilterWsTest {

  IssueFilterService service = mock(IssueFilterService.class);
  IssueFilterWs ws = new IssueFilterWs(service);
  WsTester tester = new WsTester(ws);

  @Test
  public void define_ws() throws Exception {
    WebService.Controller controller = tester.controller("api/issue_filters");
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.since()).isEqualTo("4.2");

    WebService.Action index = controller.action("app");
    assertThat(index).isNotNull();
    assertThat(index.handler()).isNotNull();
    assertThat(index.isPost()).isFalse();
    assertThat(index.isInternal()).isTrue();
  }

  @Test
  public void anonymous_app() throws Exception {
    MockUserSession.set().setLogin(null);
    tester.newRequest("app").execute().assertJson(getClass(), "anonymous_page.json");
  }

  @Test
  public void logged_in_app() throws Exception {
    MockUserSession.set().setLogin("eric").setUserId(123);
    tester.newRequest("app").execute()
      .assertJson(getClass(), "logged_in_page.json");
  }

  @Test
  public void logged_in_app_with_favorites() throws Exception {
    MockUserSession session = MockUserSession.set().setLogin("eric").setUserId(123);
    when(service.findFavoriteFilters(session)).thenReturn(Arrays.asList(
      new DefaultIssueFilter().setId(6L).setName("My issues"),
      new DefaultIssueFilter().setId(13L).setName("Blocker issues")
    ));
    tester.newRequest("app").execute()
      .assertJson(getClass(), "logged_in_page_with_favorites.json");
  }

  @Test
  public void logged_in_app_with_selected_filter() throws Exception {
    MockUserSession session = MockUserSession.set().setLogin("eric").setUserId(123);
    when(service.find(13L, session)).thenReturn(
      new DefaultIssueFilter().setId(13L).setName("Blocker issues").setData("severity=BLOCKER").setUser("eric")
    );

    tester.newRequest("app").setParam("id", "13").execute()
      .assertJson(getClass(), "logged_in_page_with_selected_filter.json");
  }

  @Test
  public void app_selected_filter_can_not_be_modified() throws Exception {
    // logged-in user is 'eric' but filter is owned by 'simon'
    MockUserSession session = MockUserSession.set().setLogin("eric").setUserId(123).setGlobalPermissions("none");
    when(service.find(13L, session)).thenReturn(
      new DefaultIssueFilter().setId(13L).setName("Blocker issues").setData("severity=BLOCKER").setUser("simon").setShared(true)
    );

    tester.newRequest("app").setParam("id", "13").execute()
      .assertJson(getClass(), "selected_filter_can_not_be_modified.json");
  }

  @Test
  public void show_filter() throws Exception {
    // logged-in user is 'eric' but filter is owned by 'simon'
    MockUserSession session = MockUserSession.set().setLogin("eric").setUserId(123).setGlobalPermissions("none");
    when(service.find(13L, session)).thenReturn(
      new DefaultIssueFilter().setId(13L).setName("Blocker issues").setData("severity=BLOCKER").setUser("simon").setShared(true)
    );

    tester.newRequest("show").setParam("id", "13").execute()
      .assertJson(getClass(), "show_filter.json");
  }

  @Test
  public void show_unknown_filter() throws Exception {
    MockUserSession session = MockUserSession.set().setLogin("eric").setUserId(123).setGlobalPermissions("none");
    when(service.find(42L, session)).thenThrow(new NotFoundException("Filter 42 does not exist"));

    try {
      tester.newRequest("show").setParam("id", "42").execute();
      fail();
    } catch (NotFoundException e) {
      assertThat(e).hasMessage("Filter 42 does not exist");
    }
  }

  @Test
  public void favorites_of_anonymous() throws Exception {
    MockUserSession.set();

    tester.newRequest("favorites").execute()
      .assertJson("{'favoriteFilters': []}");
  }

  @Test
  public void favorites_of_logged_in_user() throws Exception {
    MockUserSession session = MockUserSession.set().setLogin("eric").setUserId(123);
    when(service.findFavoriteFilters(session)).thenReturn(Arrays.asList(
      new DefaultIssueFilter().setId(13L).setName("Blocker issues").setData("severity=BLOCKER").setUser("simon").setShared(true)
    ));

    tester.newRequest("favorites").execute()
      .assertJson("{'favoriteFilters': [{'id': 13, 'name': 'Blocker issues', 'user': 'simon', 'shared': true}]}");
  }
}
