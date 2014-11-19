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

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.junit.After;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.core.profiling.Profiling;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.SearchClient;

import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ProxyBulkRequestBuilderTest {

  Profiling profiling = new Profiling(new Settings().setProperty(Profiling.CONFIG_PROFILING_LEVEL, Profiling.Level.FULL.name()));
  SearchClient searchClient = new SearchClient(new Settings(), profiling);

  @After
  public void tearDown() throws Exception {
    searchClient.stop();
  }

  @Test
  public void bulk() {
    BulkRequestBuilder bulkRequestBuilder = searchClient.prepareBulk();
    bulkRequestBuilder.add(new UpdateRequest(IndexDefinition.RULE.getIndexName(), IndexDefinition.RULE.getIndexName(), "rule1").doc(Collections.emptyMap()));
    bulkRequestBuilder.add(new DeleteRequest(IndexDefinition.RULE.getIndexName(), IndexDefinition.RULE.getIndexName(), "rule1"));
    bulkRequestBuilder.add(new IndexRequest(IndexDefinition.RULE.getIndexName(), IndexDefinition.RULE.getIndexName(), "rule1").source(Collections.emptyMap()));
    try {
      bulkRequestBuilder.get();

      // expected to fail because elasticsearch is not correctly configured, but that does not matter
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage())
        .contains("Fail to execute ES bulk request for [Action 'UpdateRequest' for key 'rule1' on index 'rules' on type 'rules'],")
        .contains("[Action 'DeleteRequest' for key 'rule1' on index 'rules' on type 'rules'],")
        .contains("[Action 'IndexRequest' for key 'rule1' on index 'rules' on type 'rules'],");
    }

    // TODO assert profiling
  }

  @Test
  public void to_string() {
    BulkRequestBuilder bulkRequestBuilder = searchClient.prepareBulk();
    bulkRequestBuilder.add(new UpdateRequest(IndexDefinition.RULE.getIndexName(), IndexDefinition.RULE.getIndexName(), "rule1").doc(Collections.emptyMap()));
    bulkRequestBuilder.add(new DeleteRequest(IndexDefinition.RULE.getIndexName(), IndexDefinition.RULE.getIndexName(), "rule1"));
    bulkRequestBuilder.add(new IndexRequest(IndexDefinition.RULE.getIndexName(), IndexDefinition.RULE.getIndexName(), "rule1").source(Collections.emptyMap()));

    assertThat(bulkRequestBuilder.toString()).contains("ES bulk request for [Action 'UpdateRequest' for key 'rule1' on index 'rules' on type 'rules'],")
      .contains("[Action 'DeleteRequest' for key 'rule1' on index 'rules' on type 'rules'],")
      .contains("[Action 'IndexRequest' for key 'rule1' on index 'rules' on type 'rules'],");
  }

  @Test
  public void with_profiling_basic() {
    Profiling profiling = new Profiling(new Settings().setProperty(Profiling.CONFIG_PROFILING_LEVEL, Profiling.Level.BASIC.name()));
    SearchClient searchClient = new SearchClient(new Settings(), profiling);

    BulkRequestBuilder bulkRequestBuilder = searchClient.prepareBulk();
    bulkRequestBuilder.add(new UpdateRequest(IndexDefinition.RULE.getIndexName(), IndexDefinition.RULE.getIndexName(), "rule1").doc(Collections.emptyMap()));
    bulkRequestBuilder.add(new DeleteRequest(IndexDefinition.RULE.getIndexName(), IndexDefinition.RULE.getIndexName(), "rule1"));
    bulkRequestBuilder.add(new IndexRequest(IndexDefinition.RULE.getIndexName(), IndexDefinition.RULE.getIndexName(), "rule1").source(Collections.emptyMap()));
    try {
      bulkRequestBuilder.get();

      // expected to fail because elasticsearch is not correctly configured, but that does not matter
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage())
        .contains("Fail to execute ES bulk request for [Action 'UpdateRequest' for key 'rule1' on index 'rules' on type 'rules'],")
        .contains("[Action 'DeleteRequest' for key 'rule1' on index 'rules' on type 'rules'],")
        .contains("[Action 'IndexRequest' for key 'rule1' on index 'rules' on type 'rules'],");
    }

    // TODO assert profiling
    searchClient.stop();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void get_with_string_timeout_is_not_yet_implemented() throws Exception {
    searchClient.prepareBulk().get("1");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void get_with_time_value_timeout_is_not_yet_implemented() throws Exception {
    searchClient.prepareBulk().get(TimeValue.timeValueMinutes(1));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void execute_is_not_yet_implemented() throws Exception {
    searchClient.prepareBulk().execute();
  }

}
