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
package org.sonar.server.rule2.index;

import com.google.common.base.Preconditions;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
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
import org.sonar.server.es.ESNode;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexDefinition;
import org.sonar.server.qualityprofile.index.ActiveRuleNormalizer;
import org.sonar.server.rule2.Rule;
import org.sonar.server.search.BaseIndex;
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
    super(new RuleIndexDefinition(), normalizer, workQueue, node);
  }

  protected String getKeyValue(RuleKey key) {
    return key.toString();
  }

  @Override
  protected XContentBuilder getIndexSettings() throws IOException {
    return jsonBuilder().startObject()
      .startObject("index")
      .field("number_of_replicas", 0)
      .field("number_of_shards", 1)
      .startObject("mapper")
      .field("dynamic", true)
      .endObject()
      .startObject("analysis")
      .startObject("analyzer")
      .startObject("path_analyzer")
      .field("type", "custom")
      .field("tokenizer", "path_hierarchy")
      .endObject()
      .startObject("sortable")
      .field("type", "custom")
      .field("tokenizer", "keyword")
      .field("filter", "lowercase")
      .endObject()
      .startObject("rule_name")
      .field("type", "custom")
      .field("tokenizer", "standard")
      .array("filter", "lowercase", "rule_name_ngram")
      .endObject()
      .endObject()
      .startObject("filter")
      .startObject("rule_name_ngram")
      .field("type", "nGram")
      .field("min_gram", 3)
      .field("max_gram", 5)
      .array("token_chars", "letter", "digit")
      .endObject()
      .endObject()
      .endObject()
      .endObject()
      .endObject();
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

    addMatchField(mapping, RuleNormalizer.RuleField.REPOSITORY.key(), "string");
    addMatchField(mapping, RuleNormalizer.RuleField.SEVERITY.key(), "string");
    addMatchField(mapping, RuleNormalizer.RuleField.STATUS.key(), "string");
    ;
    addMatchField(mapping, RuleNormalizer.RuleField.LANGUAGE.key(), "string");

    mapping.startObject(RuleNormalizer.RuleField.CHARACTERISTIC.key())
      .field("type", "string")
      .field("analyzer", "whitespace")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.key())
      .field("type", "string")
      .field("analyzer", "whitespace")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField._TAGS.key())
      .field("type", "string")
      .field("analyzer", "whitespace")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.TAGS.key())
      .field("type", "string")
      .field("analyzer", "whitespace")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.SYSTEM_TAGS.key())
      .field("type", "string")
      .field("analyzer", "whitespace")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.NOTE_CREATED_AT.key())
      .field("type", "date")
      .field("format", "date_time")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.NOTE_UPDATED_AT.key())
      .field("type", "date")
      .field("format", "date_time")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.CREATED_AT.key())
      .field("type", "date")
      .field("format", "date_time")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.UPDATED_AT.key())
      .field("type", "date")
      .field("format", "date_time")
      .endObject();

    mapping.startObject(RuleNormalizer.RuleField.KEY.key())
      .field("type", "multi_field")
      .startObject("fields")
      .startObject(RuleNormalizer.RuleField.KEY.key())
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

    mapping.startObject(RuleNormalizer.RuleField.NAME.key())
      .field("type", "multi_field")
      .startObject("fields")
      .startObject(RuleNormalizer.RuleField.NAME.key())
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

    mapping.startObject(RuleNormalizer.RuleField.PARAMS.key())
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
      FieldSortBuilder sort = SortBuilders.fieldSort(query.getSortField().field().key());
      if (query.isAscendingSort()) {
        sort.order(SortOrder.ASC);
      } else {
        sort.order(SortOrder.DESC);
      }
      esSearch.addSort(sort);
    } else if (query.getQueryText() != null && !query.getQueryText().isEmpty()) {
      esSearch.addSort(SortBuilders.scoreSort());
    } else {
      esSearch.addSort(RuleNormalizer.RuleField.UPDATED_AT.key(), SortOrder.DESC);
      // deterministic sort when exactly the same updated_at (same millisecond)
      esSearch.addSort(RuleNormalizer.RuleField.KEY.key(), SortOrder.ASC);
    }

    /* integrate Option's Pagination */
    esSearch.setFrom(options.getOffset());
    esSearch.setSize(options.getLimit());

    /* integrate Option's Fields */
    Set<String> fields = new HashSet<String>();
    if (!options.getFieldsToReturn().isEmpty()) {
      fields.addAll(options.getFieldsToReturn());
      // required field
      fields.add(RuleNormalizer.RuleField.KEY.key());
    } else {
      fields = RuleNormalizer.RuleField.ALL_KEYS;
    }

    esSearch.setFetchSource(fields.toArray(new String[fields.size()]), null);

    return esSearch;
  }

  /* Build main query (search based) */
  protected QueryBuilder getQuery(RuleQuery query, QueryOptions options) {
    QueryBuilder qb;
    if (query.getQueryText() != null && !query.getQueryText().isEmpty()) {
      qb = QueryBuilders.multiMatchQuery(query.getQueryText(),
        RuleNormalizer.RuleField.NAME.key(),
        RuleNormalizer.RuleField.NAME.key() + ".search",
        RuleNormalizer.RuleField.HTML_DESCRIPTION.key(),
        RuleNormalizer.RuleField.KEY.key(),
        RuleNormalizer.RuleField.KEY.key() + ".search",
        RuleNormalizer.RuleField.LANGUAGE.key(),
        RuleNormalizer.RuleField.CHARACTERISTIC.key(),
        RuleNormalizer.RuleField.SUB_CHARACTERISTIC.key(),
        RuleNormalizer.RuleField._TAGS.key());
    } else {
      qb = QueryBuilders.matchAllQuery();
    }
    return qb;
  }

  /* Build main filter (match based) */
  protected FilterBuilder getFilter(RuleQuery query, QueryOptions options) {

    BoolFilterBuilder fb = FilterBuilders.boolFilter();
    this.addMultiFieldTermFilter(query.getDebtCharacteristics(), fb,
      RuleNormalizer.RuleField.SUB_CHARACTERISTIC.key(),
      RuleNormalizer.RuleField.CHARACTERISTIC.key());
    this.addTermFilter(RuleNormalizer.RuleField.LANGUAGE.key(), query.getLanguages(), fb);
    this.addTermFilter(RuleNormalizer.RuleField.REPOSITORY.key(), query.getRepositories(), fb);
    this.addTermFilter(RuleNormalizer.RuleField.SEVERITY.key(), query.getSeverities(), fb);
    this.addTermFilter(RuleNormalizer.RuleField.KEY.key(), query.getKey(), fb);
    this.addTermFilter(RuleNormalizer.RuleField._TAGS.key(), query.getTags(), fb);


    if (query.getStatuses() != null && !query.getStatuses().isEmpty()) {
      Collection<String> stringStatus = new ArrayList<String>();
      for (RuleStatus status : query.getStatuses()) {
        stringStatus.add(status.name());
      }
      this.addTermFilter(RuleNormalizer.RuleField.STATUS.key(), stringStatus, fb);
    }

    /** Implementation of activation query */
    if (query.getActivation() == Boolean.TRUE) {
      if (query.getQProfileKey() == null) {
        // the rules that are activated at least once
        fb.must(FilterBuilders.hasChildFilter(new ActiveRuleIndexDefinition().getIndexType(),
          QueryBuilders.matchAllQuery()));
      } else {
        // the rules that are activated on this profile
        fb.must(FilterBuilders.hasChildFilter(new ActiveRuleIndexDefinition().getIndexType(),
          QueryBuilders.termQuery(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.key(),
            query.getQProfileKey())
        ));
      }
    } else if (query.getActivation() == Boolean.FALSE) {
      if (query.getQProfileKey() == null) {
        // the rules that are never activated, on any profile
        fb.mustNot(FilterBuilders.hasChildFilter(new ActiveRuleIndexDefinition().getIndexType(),
          QueryBuilders.matchAllQuery()));

      } else {
        // the rules that are not activated on this profile
        fb.mustNot(FilterBuilders.hasChildFilter(new ActiveRuleIndexDefinition().getIndexType(),
          QueryBuilders.termQuery(ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.key(),
            query.getQProfileKey())
        ));
      }
    }

    if ((query.getLanguages() != null && !query.getLanguages().isEmpty()) ||
      (query.getRepositories() != null && !query.getRepositories().isEmpty()) ||
      (query.getSeverities() != null && !query.getSeverities().isEmpty()) ||
      (query.getTags() != null && !query.getTags().isEmpty()) ||
      (query.getStatuses() != null && !query.getStatuses().isEmpty()) ||
      (query.getKey() != null && !query.getKey().isEmpty()) ||
      (query.getDebtCharacteristics() != null && !query.getDebtCharacteristics().isEmpty()) ||
      (query.getActivation() != null)) {
      return fb;
    } else {
      return FilterBuilders.matchAllFilter();
    }
  }

  protected void setFacets(SearchRequestBuilder query) {

    /* the Lang facet */
    query.addAggregation(AggregationBuilders
      .terms("Languages")
      .field(RuleNormalizer.RuleField.LANGUAGE.key())
      .order(Terms.Order.count(false))
      .size(10)
      .minDocCount(0));

     /* the Tag facet */
    query.addAggregation(AggregationBuilders
      .terms("Tags")
      .field(RuleNormalizer.RuleField._TAGS.key())
      .order(Terms.Order.count(false))
      .size(10)
      .minDocCount(0));

     /* the Repo facet */
    query.addAggregation(AggregationBuilders
      .terms("Repositories")
      .field(RuleNormalizer.RuleField.REPOSITORY.key())
      .order(Terms.Order.count(false))
      .size(10)
      .minDocCount(0));

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
