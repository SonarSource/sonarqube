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
import org.sonar.db.issue.IssueFilterDto;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchActionTest {
  static final String EMPTY_JSON = "{}";
  static final String EMPTY_ISSUE_FILTERS_JSON = "{" +
    "    \"issueFilters\": []" +
    "}";

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  IssueFilterService service = mock(IssueFilterService.class);
  IssueFilterJsonWriter writer = new IssueFilterJsonWriter();
  WsTester ws;

  @Before
  public void setUp() {
    ws = new WsTester(new IssueFilterWs(new SearchAction(service, writer, userSessionRule)));
  }

  @Test
  public void anonymous_app() throws Exception {
    userSessionRule.anonymous();
    WsTester.Result result = ws.newGetRequest("api/issue_filters", "search").execute();

    assertJson(result.outputAsString()).isSimilarTo(EMPTY_JSON);
  }

  @Test
  public void logged_in_app() throws Exception {
    userSessionRule.login("eric").setUserId(123);
    WsTester.Result result = ws.newGetRequest("api/issue_filters", "search").execute();

    assertJson(result.outputAsString()).isSimilarTo(EMPTY_ISSUE_FILTERS_JSON);
  }

  @Test
  public void logged_in_app_with_all_issue_filters() throws Exception {
    userSessionRule.login("eric").setUserId(123);
    when(service.findByUser(userSessionRule)).thenReturn(Arrays.asList(
      new IssueFilterDto()
        .setId(3L)
        .setName("My Unresolved Issues")
        .setShared(true)
        .setData("resolved=false|assignees=__me__"),
      new IssueFilterDto()
        .setId(2L)
        .setName("False Positive and Won't Fix Issues")
        .setShared(false)
        .setData("resolutions=FALSE-POSITIVE,WONTFIX")
      ));
    ws.newGetRequest("api/issue_filters", "search").execute()
      .assertJson(getClass(), "logged_in_page_with_favorites.json");
  }
}
