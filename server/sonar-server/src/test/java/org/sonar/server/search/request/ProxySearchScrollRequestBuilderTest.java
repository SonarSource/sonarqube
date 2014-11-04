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

package org.sonar.server.search.request;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.SearchClient;
import org.sonar.server.tester.ServerTester;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ProxySearchScrollRequestBuilderTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbSession dbSession;

  SearchClient searchClient;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    dbSession = tester.get(DbClient.class).openSession(false);
    searchClient = tester.get(SearchClient.class);
  }

  @After
  public void tearDown() throws Exception {
    dbSession.close();
  }

  @Test
  public void search_scroll() {
    tester.get(RuleDao.class).insert(dbSession, RuleTesting.newXooX1());
    dbSession.commit();

    SearchResponse search = searchClient.prepareSearch(IndexDefinition.RULE.getIndexName())
      .setSearchType(SearchType.SCAN)
      .setScroll(TimeValue.timeValueSeconds(3L))
      .get();

    SearchScrollRequestBuilder scrollRequest = searchClient.prepareSearchScroll(search.getScrollId());

    SearchResponse response = scrollRequest.get();
    assertThat(response.getHits().getTotalHits()).isEqualTo(1);
  }

  @Test
  public void fail_to_search_bad_query() throws Exception {
    try {
      searchClient.prepareSearchScroll("unknown").get();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e.getMessage()).contains("Fail to execute ES search scroll request for scroll id 'null'");
    }
  }

  @Test
  public void get_with_string_timeout_is_not_yet_implemented() throws Exception {
    SearchResponse search = searchClient.prepareSearch(IndexDefinition.RULE.getIndexName())
      .setSearchType(SearchType.SCAN)
      .setScroll(TimeValue.timeValueSeconds(3L))
      .get();

    try {
      searchClient.prepareSearchScroll(search.getScrollId()).get("1");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void get_with_time_value_timeout_is_not_yet_implemented() throws Exception {
    SearchResponse search = searchClient.prepareSearch(IndexDefinition.RULE.getIndexName())
      .setSearchType(SearchType.SCAN)
      .setScroll(TimeValue.timeValueSeconds(3L))
      .get();

    try {
      searchClient.prepareSearchScroll(search.getScrollId()).get(TimeValue.timeValueMinutes(1));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void execute_should_throw_an_unsupported_operation_exception() throws Exception {
    SearchResponse search = searchClient.prepareSearch(IndexDefinition.RULE.getIndexName())
      .setSearchType(SearchType.SCAN)
      .setScroll(TimeValue.timeValueSeconds(3L))
      .get();

    try {
      searchClient.prepareSearchScroll(search.getScrollId()).execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class).hasMessage("execute() should not be called as it's used for asynchronous");
    }
  }
}
