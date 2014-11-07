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

import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.fetch.source.FetchSourceContext;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.core.profiling.Profiling;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.SearchClient;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ProxyMultiGetRequestBuilderTest {

  Profiling profiling = new Profiling(new Settings().setProperty(Profiling.CONFIG_PROFILING_LEVEL, Profiling.Level.FULL.name()));
  SearchClient searchClient = new SearchClient(new Settings(), profiling);

  @Test
  public void multi_get() {
    try {
      MultiGetRequestBuilder request = searchClient.prepareMultiGet();
      request.add(new MultiGetRequest.Item(IndexDefinition.RULE.getIndexName(), IndexDefinition.RULE.getIndexType(), "ruleKey")
        .fetchSourceContext(FetchSourceContext.FETCH_SOURCE));
      MultiGetResponse response = request.get();

      // expected to fail because elasticsearch is not correctly configured, but that does not matter
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e.getMessage()).contains("Fail to execute ES multi get request");
    }
  }

  @Test
  public void to_string() {
    MultiGetRequestBuilder request = searchClient.prepareMultiGet();
    assertThat(request.toString()).isEqualTo("ES multi get request");

    request.add(new MultiGetRequest.Item(IndexDefinition.RULE.getIndexName(), IndexDefinition.RULE.getIndexType(), "ruleKey")
      .fetchSourceContext(FetchSourceContext.FETCH_SOURCE));
    assertThat(request.toString()).isEqualTo("ES multi get request [key 'ruleKey', index 'rules', type 'rule'],");
  }

  @Test
  public void get_with_string_timeout_is_not_yet_implemented() throws Exception {
    try {
      searchClient.prepareMultiGet().get("1");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void get_with_time_value_timeout_is_not_yet_implemented() throws Exception {
    try {
      searchClient.prepareMultiGet().get(TimeValue.timeValueMinutes(1));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void execute_should_throw_an_unsupported_operation_exception() throws Exception {
    try {
      searchClient.prepareMultiGet().execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class).hasMessage("execute() should not be called as it's used for asynchronous");
    }
  }

}
