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
package org.sonar.server.rule2;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.profiling.Profiling;
import org.sonar.server.cluster.WorkQueue;
import org.sonar.server.search.BaseIndex;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


public class RuleIndex extends BaseIndex<RuleKey> {

  private static final Logger LOG = LoggerFactory.getLogger(RuleIndex.class);

  public RuleIndex(WorkQueue workQueue, RuleDao dao, Profiling profiling) {
    super(workQueue, dao, profiling);
  }

  @Override
  public String getIndexName() {
    return RuleConstants.INDEX_NAME;
  }

  @Override
  protected String getType() {
    return RuleConstants.ES_TYPE;
  }

  protected QueryBuilder getKeyQuery(RuleKey key){
    return QueryBuilders.boolQuery()
    .must(QueryBuilders.termQuery("repositoryKey", key.repository()))
    .must(QueryBuilders.termQuery("ruleKey", key.rule()));
  }

  @Override
  protected Settings getIndexSettings() {
    try {
      return ImmutableSettings.builder()
        .loadFromSource(
          jsonBuilder().startObject()
          .startObject("index")
            .field("number_of_replicas", 0)
            .field("number_of_shards",4)
            .startObject("mapper")
              .field("dynamic", true)
            .endObject()
            .startObject("analysis")
              .startObject("analyzer")
                .startObject("path_analyzer")
                  .field("type", "custom")
                  .field("tokenizer", "path_hierarchy")
                .endObject()
              .endObject()
            .endObject()
          .endObject().toString())
        .build();
    } catch (IOException e) {
      LOG.error("Could not create index settings for {}",this.getIndexName());
      return ImmutableSettings.builder().build();
    }
  }

  @Override
  protected XContentBuilder getMapping() {
    try {
      return jsonBuilder().startObject()
        .startObject("issue")
          .startObject("properties")
            .startObject("component.path")
              .field("type", "string")
              .field("index_analyzer", "path_analyzer")
              .field("search_analyzer", "keyword")
            .endObject()
          .startObject("rule.name")
            .field("type", "string")
            .field("analyzer", "keyword")
          .endObject()
          .startObject("root.id")
            .field("type", "multi_field")
            .startObject("fields")
              .startObject("str")
                .field("type", "string")
                .field("index","analyzed")
                .field("analyzer", "default")
              .endObject()
              .startObject("num")
                .field("type", "long")
                .field("index","analyzed")
              .endObject()
          .endObject()
        .endObject()
      .endObject().endObject();
    } catch (IOException e) {
      LOG.error("Could not create mapping for {}",this.getIndexName());
      return null;
    }
  }

  @Override
  public Map<String, Object> normalize(RuleKey key) {
    //Use a MyBatis to normalize the Rule form multiple Table
    return null;
  }

  @Override
  public Collection<RuleKey> synchronizeSince(Long date) {
    //Use MyBatis to get the RuleKey created since date X
    return null;
  }
}
