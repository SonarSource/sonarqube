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
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.issue.IssueFilterDto;
import org.sonar.db.issue.IssueFilterFavouriteDto;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.DbTests;

import static org.sonar.test.JsonAssert.assertJson;

@Category(DbTests.class)
public class SearchActionTest {
  static final String EMPTY_ISSUE_FILTERS_JSON = "{" +
    "    \"issueFilters\": []" +
    "}";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  WsActionTester ws;
  DbClient dbClient;
  DbSession dbSession;

  @Before
  public void setUp() {
    dbClient = db.getDbClient();
    dbSession = db.getSession();
    userSession.anonymous();

    ws = new WsActionTester(new SearchAction(dbClient, userSession));
  }

  @Test
  public void empty_response() throws Exception {
    TestResponse result = newRequest();

    assertJson(result.getInput()).isSimilarTo(EMPTY_ISSUE_FILTERS_JSON);
  }

  @Test
  public void issue_filter_with_all_cases() {
    userSession.login("grace.hopper");
    IssueFilterDto myUnresolvedIssues = insertIssueFilter(new IssueFilterDto()
      .setName("My Unresolved Issues")
      .setShared(true)
      .setData("resolved=false|assignees=__me__"));
    IssueFilterDto falsePositiveAndWontFixIssues = insertIssueFilter(new IssueFilterDto()
      .setName("False Positive and Won't Fix Issues")
      .setShared(true)
      .setData("resolutions=FALSE-POSITIVE,WONTFIX"));
    IssueFilterDto unresolvedIssues = insertIssueFilter(new IssueFilterDto()
      .setName("Unresolved Issues")
      .setShared(true)
      .setUserLogin("grace.hopper")
      .setData("resolved=false"));
    IssueFilterDto myCustomFilter = insertIssueFilter(new IssueFilterDto()
      .setName("My Custom Filter")
      .setShared(false)
      .setUserLogin("grace.hopper")
      .setData("resolved=false|statuses=OPEN,REOPENED|assignees=__me__"));
    linkFilterToUser(myUnresolvedIssues.getId(), "grace.hopper");
    linkFilterToUser(myCustomFilter.getId(), "grace.hopper");
    linkFilterToUser(falsePositiveAndWontFixIssues.getId(), "another-login");
    linkFilterToUser(unresolvedIssues.getId(), "yet-another-login");
    commit();

    TestResponse result = newRequest();

    assertJson(result.getInput()).isSimilarTo(getClass().getResource("SearchActionTest/search.json"));
  }

  private TestResponse newRequest() {
    return ws.newRequest().execute();
  }

  private void linkFilterToUser(long filterId, String userLogin) {
    dbClient.issueFilterFavouriteDao().insert(dbSession, new IssueFilterFavouriteDto()
      .setIssueFilterId(filterId)
      .setUserLogin(userLogin));
  }

  private IssueFilterDto insertIssueFilter(IssueFilterDto issueFilter) {
    return dbClient.issueFilterDao().insert(dbSession, issueFilter);
  }

  private void commit() {
    dbSession.commit();
  }
}
