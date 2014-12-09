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

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.common.unit.TimeValue;
import org.junit.After;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.core.profiling.Profiling;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.SearchClient;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ProxyPutMappingRequestBuilderTest {

  Profiling profiling = new Profiling(new Settings().setProperty(Profiling.CONFIG_PROFILING_LEVEL, Profiling.Level.NONE.name()));
  SearchClient searchClient = new SearchClient(new Settings(), profiling);

  @After
  public void tearDown() throws Exception {
    searchClient.stop();
  }

  @Test
  public void put_mapping() {
    try {
      PutMappingRequestBuilder requestBuilder = searchClient.preparePutMapping(IndexDefinition.RULE.getIndexName())
        .setType(IndexDefinition.RULE.getIndexType())
        .setIgnoreConflicts(true)
        .setSource(mapDomain());
      requestBuilder.get();

      // expected to fail because elasticsearch is not correctly configured, but that does not matter
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e.getMessage()).contains("Fail to execute ES put mapping request");
    }
  }

  @Test
  public void to_string() {
    assertThat(searchClient.preparePutMapping(IndexDefinition.RULE.getIndexName()).setSource(mapDomain()).toString())
      .isEqualTo("ES put mapping request on indices 'rules' with source '{\"dynamic\":false,\"_all\":{\"enabled\":false}}'");
    assertThat(searchClient.preparePutMapping(IndexDefinition.RULE.getIndexName()).setType(IndexDefinition.RULE.getIndexType()).setSource(mapDomain()).toString())
      .isEqualTo("ES put mapping request on indices 'rules' on type 'rule' with source '{\"dynamic\":false,\"_all\":{\"enabled\":false}}'");
  }

  @Test
  public void with_profiling_basic() {
    Profiling profiling = new Profiling(new Settings().setProperty(Profiling.CONFIG_PROFILING_LEVEL, Profiling.Level.BASIC.name()));
    SearchClient searchClient = new SearchClient(new Settings(), profiling);

    try {
      PutMappingRequestBuilder requestBuilder = searchClient.preparePutMapping(IndexDefinition.RULE.getIndexName())
        .setType(IndexDefinition.RULE.getIndexType())
        .setIgnoreConflicts(true)
        .setSource(mapDomain());
      requestBuilder.get();

      // expected to fail because elasticsearch is not correctly configured, but that does not matter
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e.getMessage()).contains("Fail to execute ES put mapping request");
    }

    // TODO assert profiling
    searchClient.stop();
  }

  @Test
  public void fail_on_bad_query() throws Exception {
    try {
      searchClient.preparePutMapping().get();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
      assertThat(e.getMessage()).contains("Fail to execute ES put mapping request");
    }
  }

  @Test
  public void get_with_string_timeout_is_not_yet_implemented() throws Exception {
    try {
      searchClient.preparePutMapping().get("1");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void get_with_time_value_timeout_is_not_yet_implemented() throws Exception {
    try {
      searchClient.preparePutMapping().get(TimeValue.timeValueMinutes(1));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not yet implemented");
    }
  }

  @Test
  public void execute_should_throw_an_unsupported_operation_exception() throws Exception {
    try {
      searchClient.preparePutMapping().execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(UnsupportedOperationException.class).hasMessage("execute() should not be called as it's used for asynchronous");
    }
  }

  protected static Map mapDomain() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    mapping.put("dynamic", false);
    mapping.put("_all", ImmutableMap.of("enabled", false));
    return mapping;
  }

}
