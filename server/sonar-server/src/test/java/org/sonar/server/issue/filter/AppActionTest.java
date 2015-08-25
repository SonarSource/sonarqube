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

import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.db.issue.IssueFilterDto;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AppActionTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Mock
  IssueFilterService service;

  AppAction underTest;
  WsTester ws;

  @Before
  public void setUp() {
    underTest = new AppAction(service, userSessionRule);
    ws = new WsTester(new IssueFilterWs(underTest, mock(ShowAction.class), mock(SearchAction.class), mock(FavoritesAction.class)));
  }

  @Test
  public void anonymous_app() throws Exception {
    userSessionRule.anonymous();
    ws.newGetRequest("api/issue_filters", "app").execute().assertJson(getClass(), "anonymous_page.json");
  }

  @Test
  public void logged_in_app() throws Exception {
    userSessionRule.login("eric").setUserId(123);
    ws.newGetRequest("api/issue_filters", "app").execute()
      .assertJson(getClass(), "logged_in_page.json");
  }

  @Test
  public void logged_in_app_with_favorites() throws Exception {
    userSessionRule.login("eric").setUserId(123);
    when(service.findFavoriteFilters(userSessionRule)).thenReturn(Arrays.asList(
      new IssueFilterDto().setId(6L).setName("My issues"),
      new IssueFilterDto().setId(13L).setName("Blocker issues")
    ));
    ws.newGetRequest("api/issue_filters", "app").execute()
      .assertJson(getClass(), "logged_in_page_with_favorites.json");
  }

  @Test
  public void logged_in_app_with_selected_filter() throws Exception {
    userSessionRule.login("eric").setUserId(123);
    when(service.find(13L, userSessionRule)).thenReturn(
      new IssueFilterDto().setId(13L).setName("Blocker issues").setData("severity=BLOCKER").setUserLogin("eric")
    );

    ws.newGetRequest("api/issue_filters", "app").setParam("id", "13").execute()
      .assertJson(getClass(), "logged_in_page_with_selected_filter.json");
  }

  @Test
  public void app_selected_filter_can_not_be_modified() throws Exception {
    // logged-in user is 'eric' but filter is owned by 'simon'
    userSessionRule.login("eric").setUserId(123).setGlobalPermissions("none");
    when(service.find(13L, userSessionRule)).thenReturn(
      new IssueFilterDto().setId(13L).setName("Blocker issues").setData("severity=BLOCKER").setUserLogin("simon").setShared(true)
    );

    ws.newGetRequest("api/issue_filters", "app").setParam("id", "13").execute()
      .assertJson(getClass(), "selected_filter_can_not_be_modified.json");
  }

}
