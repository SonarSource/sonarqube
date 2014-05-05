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

import com.google.common.collect.ImmutableSet;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.rule.RuleConstants;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.es.ESNode;
import org.sonar.server.rule2.RuleNormalizer.RuleField;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.Results;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class RuleIndex extends BaseIndex<RuleKey, RuleDto> {

  private static final Logger LOG = LoggerFactory.getLogger(RuleIndex.class);

  public static final Set<String> PUBLIC_FIELDS = ImmutableSet.of(
    RuleField.KEY.key(),
    RuleField.NAME.key(),
    RuleField.HTML_DESCRIPTION.key(),
    RuleField.LANGUAGE.key(),
    RuleField.SEVERITY.key(),
    RuleField.STATUS.key(),
    RuleField.TAGS.key(),
    RuleField.SYSTEM_TAGS.key(),
    RuleField.CREATED_AT.key(),
    RuleField.REPOSITORY.key(),
    RuleField.PARAMS.key(),
    RuleField.ACTIVE.key(),
    RuleField.TEMPLATE.key(),
    RuleField.INTERNAL_KEY.key(),
    RuleField.UPDATED_AT.key());

  public RuleIndex(RuleNormalizer normalizer, WorkQueue workQueue,
                   Profiling profiling, ESNode node) {
    super(normalizer, workQueue, profiling, node);
  }

  @Override
  public String getIndexName() {
    return RuleConstants.INDEX_NAME;
  }

  @Override
  protected String getType() {
    return RuleConstants.ES_TYPE;
  }

  protected String getKeyValue(RuleKey key) {
    return key.toString();
  }

  @Override
  protected XContentBuilder getIndexSettings() throws IOException {
    return jsonBuilder().startObject()
      .startObject("index")
        .field("number_of_replicas", 0)
        .field("number_of_shards", 3)
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

  private void addMatchField(XContentBuilder mapping, String field, String type) throws IOException {
    mapping.startObject(field)
      .field("type", type)
      .field("index", "not_analyzed")
      .endObject();
  }

  private void addFindField(XContentBuilder mapping, String field, String type) throws IOException {
    mapping.startObject(field)
      .field("type", type)
      .field("index", "analyzed")
      .endObject();
  }

  @Override
  protected XContentBuilder getMapping() throws IOException {
    XContentBuilder mapping = jsonBuilder().startObject()
      .startObject(this.getType())
      .field("dynamic", true)
      .startObject("properties");

    addMatchField(mapping, RuleField.KEY.key(), "string");
    addMatchField(mapping, RuleField.REPOSITORY.key(), "string");
    addMatchField(mapping, RuleField.SEVERITY.key(), "string");
    addMatchField(mapping, RuleField.STATUS.key(), "string");

    mapping.startObject(RuleField.NAME.key())
        .field("type", "multi_field")
        .startObject("fields")
          .startObject(RuleField.NAME.key())
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

    mapping.startObject(RuleField.ACTIVE.key())
      .field("type", "nested")
      .field("dynamic", true)
      .endObject();

    mapping.startObject(RuleField.PARAMS.key())
      .field("type", "nested")
      .field("dynamic", true)
      .endObject();

    return mapping.endObject()
      .endObject().endObject();
  }

  protected void setFacets(SearchRequestBuilder query){
    //TODO there are no aggregation in 0.9!!! Must use facet...

     /* the Lang facet */
    query.addFacet(FacetBuilders.termsFacet("Languages")
      .field(RuleField.LANGUAGE.key())
      .size(10)
      .global(true)
      .order(TermsFacet.ComparatorType.COUNT));

    /* the Tag facet */
    query.addFacet(FacetBuilders.termsFacet("Tags")
      .field(RuleField.TAGS.key())
      .size(10)
      .global(true)
      .order(TermsFacet.ComparatorType.COUNT));

    /* the Repo facet */
    query.addFacet(FacetBuilders.termsFacet("Repositories")
      .field(RuleField.REPOSITORY.key())
      .size(10)
      .global(true)
      .order(TermsFacet.ComparatorType.COUNT));
  }

  public Results search(RuleQuery query, QueryOptions options) {

    SearchRequestBuilder esSearch = getClient()
      .prepareSearch(this.getIndexName())
      .setIndices(this.getIndexName());

    /* Build main query (search based) */
    QueryBuilder qb;
    if (query.getQueryText() != null && !query.getQueryText().isEmpty()) {
      qb = QueryBuilders.multiMatchQuery(query.getQueryText(),
        "_id",
        RuleField.NAME.key(),
        RuleField.NAME.key()+".search",
        RuleField.HTML_DESCRIPTION.key(),
        RuleField.KEY.key(),
        RuleField.LANGUAGE.key(),
        RuleField.TAGS.key());
    } else {
      qb = QueryBuilders.matchAllQuery();
    }

    /* Build main filter (match based) */
    BoolFilterBuilder fb = FilterBuilders.boolFilter();
    this.addTermFilter(RuleField.LANGUAGE.key(), query.getLanguages(), fb);
    this.addTermFilter(RuleField.REPOSITORY.key(), query.getRepositories(), fb);
    this.addTermFilter(RuleField.SEVERITY.key(), query.getSeverities(), fb);
    this.addTermFilter(RuleField.KEY.key(), query.getKey(), fb);
    if(query.getStatuses() != null && !query.getStatuses().isEmpty()) {
      Collection<String> stringStatus = new ArrayList<String>();
      for (RuleStatus status : query.getStatuses()) {
        stringStatus.add(status.name());
      }
      this.addTermFilter(RuleField.STATUS.key(), stringStatus, fb);
    }

    /* Integrate Query */
    QueryBuilder mainQuery;
    if((query.getLanguages() != null && !query.getLanguages().isEmpty()) ||
      (query.getRepositories() != null && !query.getRepositories().isEmpty()) ||
      (query.getSeverities() != null && !query.getSeverities().isEmpty()) ||
      (query.getStatuses() != null && !query.getStatuses().isEmpty()) ||
      (query.getKey() != null && !query.getKey().isEmpty())) {
      mainQuery = QueryBuilders.filteredQuery(qb, fb);
    } else {
      mainQuery = qb;
    }
    esSearch.setQuery(mainQuery);

    /* Integrate Facets */
    if(options.isFacet()) {
      this.setFacets(esSearch);
    }

    /* integrate Query Sort */
    if(query.getSortField() != null){
      FieldSortBuilder sort = SortBuilders.fieldSort(query.getSortField().field().key());
      if(query.isAscendingSort()){
        sort.order(SortOrder.ASC);
      } else {
        sort.order(SortOrder.DESC);
      }
      esSearch.addSort(sort);
    } else if(query.getQueryText() != null && !query.getQueryText().isEmpty()){
      esSearch.addSort(SortBuilders.scoreSort());
    } else {
      esSearch.addSort(RuleField.UPDATED_AT.key(), SortOrder.DESC);
    }

    /* integrate Option's Pagination */
    esSearch.setFrom(options.getOffset());
    esSearch.setSize(options.getLimit());

    /* integrate Option's Fields */
    if (options.getFieldsToReturn() != null &&
      !options.getFieldsToReturn().isEmpty()) {
      for(String field:options.getFieldsToReturn()) {
        esSearch.addField(field);
      }
    } else {
      for (RuleField field : RuleField.values()) {
        esSearch.addField(field.key());
      }
    }

    /* Get results */
    SearchResponse esResult = esSearch.get();

    /* Integrate ES Results */
    Results results = new Results(esResult)
      .setTotal((int) esResult.getHits().totalHits())
      .setTime(esResult.getTookInMillis())
      .setHits(this.toHit(esResult.getHits()));

    return results;
  }
}
