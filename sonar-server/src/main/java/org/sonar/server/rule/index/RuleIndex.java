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
package org.sonar.server.rule.index;

import com.google.common.base.Preconditions;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.qualityprofile.index.ActiveRuleNormalizer;
import org.sonar.server.rule.Rule;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.search.ESNode;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.IndexField;
import org.sonar.server.search.QueryOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class RuleIndex extends BaseIndex<Rule, RuleDto, RuleKey> {

  public RuleIndex(RuleNormalizer normalizer, WorkQueue workQueue, ESNode node) {
    super(IndexDefinition.RULE, normalizer, workQueue, node);
  }

  protected String getKeyValue(RuleKey key) {
    return key.toString();
  }

  @Override
  protected Settings getIndexSettings() throws IOException {
    return ImmutableSettings.builder()
      .put("index.number_of_replicas",0)
      .put("index.number_of_shards",1)
      .build();
  }

  @Override
  protected XContentBuilder getMapping() throws IOException {

    XContentBuilder mapping = jsonBuilder().startObject()
      .startObject(this.indexDefinition.getIndexType())
      .field("dynamic", true)
      .startObject("_id")
      .field("path", RuleNormalizer.RuleField.KEY)
      .endObject()
      .startObject("properties");

    addMatchField(mapping, RuleNormalizer.RuleField.REPOSITORY.field(), "string");
    addMatchField(mapping, RuleNormalizer.RuleField.SEVERITY.field(), "string");
    addMatchField(mapping, RuleNormalizer.RuleField.STATUS.field(), "string");
    addMatchField(mapping, RuleNormalizer.RuleField.LANGUAGE.field(), "string");

    mapping.startObject(RuleNormalizer.RuleField.CHARACTERISTIC.field())
      .field("type", "string")
      .field("analyzer", "whitespace")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field())
      .field("type", "string")
      .field("analyzer", "whitespace")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField._TAGS.field())
      .field("type", "string")
      .field("analyzer", "whitespace")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.TAGS.field())
      .field("type", "string")
      .field("analyzer", "whitespace")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.SYSTEM_TAGS.field())
      .field("type", "string")
      .field("analyzer", "whitespace")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.NOTE_CREATED_AT.field())
      .field("type", "date")
      .field("format", "date_time")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.NOTE_UPDATED_AT.field())
      .field("type", "date")
      .field("format", "date_time")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.CREATED_AT.field())
      .field("type", "date")
      .field("format", "date_time")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.UPDATED_AT.field())
      .field("type", "date")
      .field("format", "date_time")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.KEY.field())
      .field("type", "multi_field")
      .startObject("fields")
      .startObject(RuleNormalizer.RuleField.KEY.field())
      .field("type", "string")
      .field("index", "analyzed")
      .endObject()
      .startObject("search")
      .field("type", "string")
      .field("index", "analyzed")
      .field("index_analyzer", "rule_name")
      .field("search_analyzer", "standard")
      .endObject()
      .endObject()
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.NAME.field())
      .field("type", "multi_field")
      .startObject("fields")
      .startObject(RuleNormalizer.RuleField.NAME.field())
      .field("type", "string")
      .field("index", "analyzed")
      .endObject()
      .startObject("search")
      .field("type", "string")
      .field("index", "analyzed")
      .field("index_analyzer", "rule_name")
      .field("search_analyzer", "standard")
      .endObject()
      .endObject()
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.PARAMS.field())
      .field("type", "nested")
      .field("dynamic", true)
      .endObject();

    return mapping.endObject()
      .endObject().endObject();
  }

  protected SearchRequestBuilder buildRequest(RuleQuery query, QueryOptions options) {
    SearchRequestBuilder esSearch = getClient()
      .prepareSearch(this.getIndexName())
      .setTypes(this.getIndexType())
      .setIndices(this.getIndexName());

    /* Integrate Facets */
    if (options.isFacet()) {
      this.setFacets(esSearch);
    }

    /* integrate Query Sort */
    if (query.getSortField() != null) {
      FieldSortBuilder sort = SortBuilders.fieldSort(query.getSortField().field());
      if (query.isAscendingSort()) {
        sort.order(SortOrder.ASC);
      } else {
        sort.order(SortOrder.DESC);
      }
      esSearch.addSort(sort);
    } else if (query.getQueryText() != null && !query.getQueryText().isEmpty()) {
      esSearch.addSort(SortBuilders.scoreSort());
    } else {
      esSearch.addSort(RuleNormalizer.RuleField.UPDATED_AT.field(), SortOrder.DESC);
      // deterministic sort when exactly the same updated_at (same millisecond)
      esSearch.addSort(RuleNormalizer.RuleField.KEY.field(), SortOrder.ASC);
    }

    /* integrate Option's Pagination */
    esSearch.setFrom(options.getOffset());
    esSearch.setSize(options.getLimit());

    /* integrate Option's Fields */
    Set<String> fields = new HashSet<String>();
    if (!options.getFieldsToReturn().isEmpty()) {
      for(String fieldToReturn:options.getFieldsToReturn()){
        if(!fieldToReturn.isEmpty()){
          fields.add(fieldToReturn);
        }
      }
      // required field
      fields.add(RuleNormalizer.RuleField.KEY.field());
    } else {
      for(IndexField indexField:RuleNormalizer.RuleField.ALL_FIELDS) {
        fields.add(indexField.field());
      }
    }

    esSearch.setFetchSource(fields.toArray(new String[fields.size()]), null);

    return esSearch;
  }

  /* Build main query (search based) */
  protected QueryBuilder getQuery(RuleQuery query, QueryOptions options) {
    QueryBuilder qb;
    if (query.getQueryText() != null && !query.getQueryText().isEmpty()) {
      qb = QueryBuilders.multiMatchQuery(query.getQueryText(),
        RuleNormalizer.RuleField.NAME.field(),
        RuleNormalizer.RuleField.NAME.field() + ".search",
        RuleNormalizer.RuleField.HTML_DESCRIPTION.field(),
        RuleNormalizer.RuleField.KEY.field(),
        RuleNormalizer.RuleField.KEY.field() + ".search",
        RuleNormalizer.RuleField.LANGUAGE.field(),
        RuleNormalizer.RuleField.CHARACTERISTIC.field(),
        RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field(),
        RuleNormalizer.RuleField._TAGS.field());
    } else {
      qb = QueryBuilders.matchAllQuery();
    }
    return qb;
  }

  /* Build main filter (match based) */
  protected FilterBuilder getFilter(RuleQuery query, QueryOptions options) {

    BoolFilterBuilder fb = FilterBuilders.boolFilter();

    /* Add enforced filter on rules that are REMOVED */
    fb.mustNot(FilterBuilders
      .termFilter(RuleNormalizer.RuleField.STATUS.field(),
        RuleStatus.REMOVED.toString()));

    this.addMultiFieldTermFilter(query.getDebtCharacteristics(), fb,
      RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field(),
      RuleNormalizer.RuleField.CHARACTERISTIC.field());
    this.addTermFilter(RuleNormalizer.RuleField.LANGUAGE.field(), query.getLanguages(), fb);
    this.addTermFilter(RuleNormalizer.RuleField.REPOSITORY.field(), query.getRepositories(), fb);
    this.addTermFilter(RuleNormalizer.RuleField.SEVERITY.field(), query.getSeverities(), fb);
    this.addTermFilter(RuleNormalizer.RuleField.KEY.field(), query.getKey(), fb);
    this.addTermFilter(RuleNormalizer.RuleField._TAGS.field(), query.getTags(), fb);


    if (query.getStatuses() != null && !query.getStatuses().isEmpty()) {
      Collection<String> stringStatus = new ArrayList<String>();
      for (RuleStatus status : query.getStatuses()) {
        stringStatus.add(status.name());
      }
      this.addTermFilter(RuleNormalizer.RuleField.STATUS.field(), stringStatus, fb);
    }

    /** Implementation of activation query */
    if (query.getActivation() == Boolean.TRUE) {
      if (query.getQProfileKey() == null) {
        // the rules that are activated at least once
        fb.must(FilterBuilders.hasChildFilter(IndexDefinition.ACTIVE_RULE.getIndexType(),
          QueryBuilders.matchAllQuery()));
      } else {
        // the rules that are activated on this profile
        fb.must(FilterBuilders.hasChildFilter(IndexDefinition.ACTIVE_RULE.getIndexType(),
          QueryBuilders.termQuery(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.key(),
            query.getQProfileKey())
        ));
      }
    } else if (query.getActivation() == Boolean.FALSE) {
      if (query.getQProfileKey() == null) {
        // the rules that are never activated, on any profile
        fb.mustNot(FilterBuilders.hasChildFilter(IndexDefinition.ACTIVE_RULE.getIndexType(),
          QueryBuilders.matchAllQuery()));

      } else {
        // the rules that are not activated on this profile
        fb.mustNot(FilterBuilders.hasChildFilter(IndexDefinition.ACTIVE_RULE.getIndexType(),
          QueryBuilders.termQuery(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.key(),
            query.getQProfileKey())
        ));
      }
    }

    return fb;
  }

  protected void setFacets(SearchRequestBuilder query) {

    /* the Lang facet */
    query.addAggregation(AggregationBuilders
      .terms("languages")
      .field(RuleNormalizer.RuleField.LANGUAGE.field())
      .order(Terms.Order.count(false))
      .size(10)
      .minDocCount(1));

     /* the Tag facet */
    query.addAggregation(AggregationBuilders
      .terms("tags")
      .field(RuleNormalizer.RuleField._TAGS.field())
      .order(Terms.Order.count(false))
      .size(10)
      .minDocCount(1));

     /* the Repo facet */
    query.addAggregation(AggregationBuilders
      .terms("repositories")
      .field(RuleNormalizer.RuleField.REPOSITORY.field())
      .order(Terms.Order.count(false))
      .size(10)
      .minDocCount(1));

  }

  public RuleResult search(RuleQuery query, QueryOptions options) {
    SearchRequestBuilder esSearch = this.buildRequest(query, options);
    FilterBuilder fb = this.getFilter(query, options);
    QueryBuilder qb = this.getQuery(query, options);

    esSearch.setQuery(QueryBuilders.filteredQuery(qb, fb));

    SearchResponse esResult = esSearch.get();

    return new RuleResult(esResult);
  }


  @Override
  protected Rule toDoc(Map<String, Object> fields) {
    Preconditions.checkArgument(fields != null, "Cannot construct Rule with null response!!!");
    return new RuleDoc(fields);
  }


  public Set<String> terms(String fields) {
    Set<String> tags = new HashSet<String>();
    String key = "_ref";

    SearchRequestBuilder request = this.getClient()
      .prepareSearch(this.getIndexName())
      .setQuery(QueryBuilders.matchAllQuery())
      .addAggregation(AggregationBuilders.terms(key)
        .field(fields)
        .size(Integer.MAX_VALUE)
        .minDocCount(1));

    SearchResponse esResponse = request.get();

    Terms aggregation = esResponse.getAggregations().get(key);

    for (Terms.Bucket value : aggregation.getBuckets()) {
      tags.add(value.getKey());
    }
    return tags;
  }
}
