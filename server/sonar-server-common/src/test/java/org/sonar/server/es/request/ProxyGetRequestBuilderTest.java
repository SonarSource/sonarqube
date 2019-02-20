/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.es.request;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.Index;
import org.sonar.server.es.newindex.FakeIndexDefinition;
import org.sonar.server.es.IndexType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.server.es.newindex.FakeIndexDefinition.TYPE_FAKE;

@RunWith(DataProviderRunner.class)
public class ProxyGetRequestBuilderTest {

  @Rule
  public EsTester es = EsTester.createCustom(new FakeIndexDefinition());

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void trace_logs() {
    logTester.setLevel(LoggerLevel.TRACE);

    es.client().prepareGet(TYPE_FAKE, "ruleKey")
      .get();
    assertThat(logTester.logs(LoggerLevel.TRACE)).hasSize(1);
  }

  @Test
  @UseDataProvider("mainAndRelationWithUnknownIndex")
  public void prepareGet_fails_if_index_unknown(IndexType indexType) {
    GetRequestBuilder requestBuilder = es.client().prepareGet(indexType, "rule1");
    try {
      requestBuilder.get();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e.getMessage()).contains("Fail to execute ES get request for key 'rule1' on index 'unknown' on type 'test'");
    }
  }

  @DataProvider
  public static Object[][] mainAndRelationWithUnknownIndex() {
    IndexType.IndexMainType mainType = IndexType.main(Index.withRelations("unknown"), "test");
    return new Object[][] {
      {mainType},
      {IndexType.relation(mainType, "donut")}
    };
  }

  @Test
  public void get_with_string_timeout_is_not_implemented() {
    try {
      es.client().prepareGet(TYPE_FAKE, "ruleKey").get("1");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void get_with_time_value_timeout_is_not_yet_implemented() {
    try {
      es.client().prepareGet(TYPE_FAKE, "ruleKey").get(TimeValue.timeValueMinutes(1));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void execute_should_throw_an_unsupported_operation_exception() {
    try {
      es.client().prepareGet(TYPE_FAKE, "ruleKey").execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class).hasMessage("execute() should not be called as it's used for asynchronous");
    }
  }

}
