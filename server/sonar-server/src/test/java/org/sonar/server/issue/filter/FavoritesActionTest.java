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
public class FavoritesActionTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Mock
  IssueFilterService service;

  @Mock
  IssueFilterJsonWriter writer;

  FavoritesAction action;

  WsTester tester;

  @Before
  public void setUp() {
    action = new FavoritesAction(service, userSessionRule);
    tester = new WsTester(new IssueFilterWs(mock(AppAction.class), mock(ShowAction.class), mock(SearchAction.class), action));
  }

  @Test
  public void favorites_of_anonymous() throws Exception {
    userSessionRule.login();

    tester.newGetRequest("api/issue_filters", "favorites").execute()
      .assertJson("{\"favoriteFilters\": []}");
  }

  @Test
  public void favorites_of_logged_in_user() throws Exception {
    userSessionRule.login("eric").setUserId(123);
    when(service.findFavoriteFilters(userSessionRule)).thenReturn(Arrays.asList(
      new IssueFilterDto().setId(13L).setName("Blocker issues").setData("severity=BLOCKER").setUserLogin("simon").setShared(true)
    ));

    tester.newGetRequest("api/issue_filters", "favorites").execute()
      .assertJson("{\"favoriteFilters\": [{\"id\": 13, \"name\": \"Blocker issues\", \"user\": \"simon\", \"shared\": true}]}");
  }

}
