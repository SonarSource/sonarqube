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
package org.sonar.server.log.index;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.log.Log;
import org.sonar.core.log.db.LogDto;
import org.sonar.core.log.db.LogKey;
import org.sonar.core.profiling.Profiling;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.search.ESNode;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.IndexField;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.Result;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 4.4
 */
public class LogIndex extends BaseIndex<Log, LogDto, LogKey> {

  public LogIndex(Profiling profiling, LogNormalizer normalizer, WorkQueue workQueue, ESNode node) {
    super(IndexDefinition.LOG, normalizer, workQueue, node, profiling);
  }

  @Override
  protected String getKeyValue(LogKey key) {
    // FIXME too many collision with key.toString() due to lack of time precision
    return null;// return key.toString();
  }

  @Override
  protected Map mapKey() {
    return null;
    // Map<String, Object> mapping = new HashMap<String, Object>();
    // return mapping;
  }

  @Override
  protected Settings getIndexSettings() throws IOException {
    return ImmutableSettings.builder().build();
  }

  @Override
  protected Map mapProperties() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    for (IndexField field : LogNormalizer.LogFields.ALL_FIELDS) {
      mapping.put(field.field(), mapField(field));
    }
    return mapping;
  }

  @Override
  protected Log toDoc(final Map<String, Object> fields) {
    return new LogDoc(fields);
  }

  public Result<Log> findAll() {
    return new Result<Log>(this, getClient().prepareSearch(this.getIndexName())
      .setQuery(QueryBuilders.matchAllQuery())
      .setTypes(this.getIndexType())
      .get());
  }

  public Result<Log> search(LogQuery query, QueryOptions options) {
    SearchRequestBuilder esSearch = getClient()
      .prepareSearch(this.getIndexName())
      .setTypes(this.getIndexType())
      .setIndices(this.getIndexName());

    // TODO implement query and filters based on LogQuery
    esSearch.setQuery(QueryBuilders.matchAllQuery());

    if (options.isScroll()) {
      esSearch.setSearchType(SearchType.SCAN);
      esSearch.setScroll(TimeValue.timeValueMinutes(3));
    }

    SearchResponse esResult = esSearch.get();

    return new Result<Log>(this, esResult);
  }
}
