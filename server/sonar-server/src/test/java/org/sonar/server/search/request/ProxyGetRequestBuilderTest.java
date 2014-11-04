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

import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.indices.IndexMissingException;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.SearchClient;
import org.sonar.server.tester.ServerTester;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ProxyGetRequestBuilderTest {

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
  public void get() {
    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(dbSession, rule);
    dbSession.commit();

    GetResponse response = searchClient.prepareGet()
      .setIndex(IndexDefinition.RULE.getIndexName())
      .setType(IndexDefinition.RULE.getIndexType())
      .setId(rule.getKey().toString())
      .get();
    assertThat(response.getSource()).isNotEmpty();
  }

  @Test
  public void get_with_index_type_and_id() {
    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(dbSession, RuleTesting.newXooX1());
    dbSession.commit();

    GetResponse response = searchClient.prepareGet(IndexDefinition.RULE.getIndexName(), IndexDefinition.RULE.getIndexType(), rule.getKey().toString()).get();
    assertThat(response.getSource()).isNotEmpty();
  }

  @Test
  public void fail_to_get_bad_query() throws Exception {
    GetRequestBuilder requestBuilder = searchClient.prepareGet()
      .setIndex("unknown")
      .setType("test")
      .setId("rule1");
    try {
      requestBuilder.get();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e.getMessage()).contains("Fail to execute ES get request for key 'rule1' on index 'unknown' on type 'test'");
    }
  }

  @Test(expected = IndexMissingException.class)
  public void fail_to_get_bad_query_using_new_builder() throws Exception {
    GetRequestBuilder requestBuilder = new GetRequestBuilder(searchClient)
      .setIndex("unknown")
      .setType("test")
      .setId("rule1");
    requestBuilder.get();
  }

  @Test
  public void get_with_string_timeout_is_not_yet_implemented() throws Exception {
    try {
      searchClient.prepareGet().get("1");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void get_with_time_value_timeout_is_not_yet_implemented() throws Exception {
    try {
      searchClient.prepareGet().get(TimeValue.timeValueMinutes(1));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void execute_should_throw_an_unsupported_operation_exception() throws Exception {
    try {
      searchClient.prepareGet().execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class).hasMessage("execute() should not be called as it's used for asynchronous");
    }
  }

}
