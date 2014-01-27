/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.sonar.server.user.MockUserSession;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
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

    WebService.Action index = controller.action("page");
    assertThat(index).isNotNull();
    assertThat(index.handler()).isNotNull();
    assertThat(index.isPost()).isFalse();
    assertThat(index.isPrivate()).isTrue();
  }

  @Test
  public void anonymous_page() throws Exception {
    MockUserSession.set().setLogin(null);
    tester.newRequest("page").execute().assertJson(getClass(), "anonymous_page.json");
  }

  @Test
  public void logged_in_page() throws Exception {
    MockUserSession.set().setLogin("eric").setUserId(123);
    tester.newRequest("page").execute()
      .assertJson(getClass(), "logged_in_page.json");
  }

  @Test
  public void logged_in_page_with_favorites() throws Exception {
    MockUserSession session = MockUserSession.set().setLogin("eric").setUserId(123);
    when(service.findFavoriteFilters(session)).thenReturn(Arrays.asList(
      new DefaultIssueFilter().setId(6L).setName("My issues"),
      new DefaultIssueFilter().setId(13L).setName("Blocker issues")
    ));
    tester.newRequest("page").execute()
      .assertJson(getClass(), "logged_in_page_with_favorites.json");
  }

  @Test
  public void logged_in_page_with_selected_filter() throws Exception {
    MockUserSession session = MockUserSession.set().setLogin("eric").setUserId(123);
    when(service.find(13L, session)).thenReturn(
      new DefaultIssueFilter().setId(13L).setName("Blocker issues").setData("severity=BLOCKER")
    );

    tester.newRequest("page").setParam("id", "13").execute()
      .assertJson(getClass(), "logged_in_page_with_selected_filter.json");
  }
}
