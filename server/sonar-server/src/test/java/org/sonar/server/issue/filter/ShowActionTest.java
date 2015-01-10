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
import org.sonar.core.issue.DefaultIssueFilter;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ShowActionTest {

  @Mock
  IssueFilterService service;

  IssueFilterWriter writer = new IssueFilterWriter();

  ShowAction action;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    action = new ShowAction(service, writer);
    tester = new WsTester(new IssueFilterWs(mock(AppAction.class), action, mock(FavoritesAction.class)));
  }

  @Test
  public void show_filter() throws Exception {
    // logged-in user is 'eric' but filter is owned by 'simon'
    MockUserSession session = MockUserSession.set().setLogin("eric").setUserId(123).setGlobalPermissions("none");
    when(service.find(13L, session)).thenReturn(
      new DefaultIssueFilter().setId(13L).setName("Blocker issues").setDescription("All Blocker Issues").setData("severity=BLOCKER").setUser("simon").setShared(true)
    );

    tester.newGetRequest("api/issue_filters", "show").setParam("id", "13").execute()
      .assertJson(getClass(), "show_filter.json");
  }

  @Test
  public void show_unknown_filter() throws Exception {
    MockUserSession session = MockUserSession.set().setLogin("eric").setUserId(123).setGlobalPermissions("none");
    when(service.find(42L, session)).thenThrow(new NotFoundException("Filter 42 does not exist"));

    try {
      tester.newGetRequest("api/issue_filters", "show").setParam("id", "42").execute();
      fail();
    } catch (NotFoundException e) {
      assertThat(e).hasMessage("Filter 42 does not exist");
    }
  }

}
