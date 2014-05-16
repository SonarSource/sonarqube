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
import com.google.common.collect.ImmutableSet;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.es.ESNode;
import org.sonar.server.rule2.Rule;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.search.QueryOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class RuleIndex extends BaseIndex<Rule, RuleDto, RuleKey> {

  public static final Set<String> PUBLIC_FIELDS = ImmutableSet.of(
    RuleNormalizer.RuleField.KEY.key(),
    RuleNormalizer.RuleField.NAME.key(),
    RuleNormalizer.RuleField.HTML_DESCRIPTION.key(),
    RuleNormalizer.RuleField.LANGUAGE.key(),
    RuleNormalizer.RuleField.SEVERITY.key(),
    RuleNormalizer.RuleField.STATUS.key(),
    RuleNormalizer.RuleField.TAGS.key(),
    RuleNormalizer.RuleField.SYSTEM_TAGS.key(),
    RuleNormalizer.RuleField.CREATED_AT.key(),
    RuleNormalizer.RuleField.REPOSITORY.key(),
    RuleNormalizer.RuleField.PARAMS.key(),
    RuleNormalizer.RuleField.TEMPLATE.key(),
    RuleNormalizer.RuleField.INTERNAL_KEY.key(),
    RuleNormalizer.RuleField.UPDATED_AT.key(),
    RuleNormalizer.RuleField.DEBT_FUNCTION_TYPE.key(),
    RuleNormalizer.RuleField.DEBT_FUNCTION_COEFFICIENT.key(),
    RuleNormalizer.RuleField.DEBT_FUNCTION_OFFSET.key(),
    RuleNormalizer.RuleField.SUB_CHARACTERISTIC.key());


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
    }

    /* integrate Option's Pagination */
    esSearch.setFrom(options.getOffset());
    esSearch.setSize(options.getLimit());

    /* integrate Option's Fields */
    Set<String> fields = new HashSet<String>();
    if (options.getFieldsToReturn() != null &&
      !options.getFieldsToReturn().isEmpty()) {
      for (String field : options.getFieldsToReturn()) {
        fields.add(field);
      }
    } else {
      for (RuleNormalizer.RuleField field : RuleNormalizer.RuleField.values()) {
          fields.add(field.key());
      }
    }
    //Add required fields:
    fields.add(RuleNormalizer.RuleField.KEY.key());
    fields.add(RuleNormalizer.RuleField.REPOSITORY.key());

    //TODO limit source for available fields.
    //esSearch.addFields(fields.toArray(new String[fields.size()]));
    //esSearch.setSource(StringUtils.join(fields,','));

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
        RuleNormalizer.RuleField.TAGS.key());
    } else {
      qb = QueryBuilders.matchAllQuery();
    }
    return qb;
  }

  /* Build main filter (match based) */
  protected FilterBuilder getFilter(RuleQuery query, QueryOptions options) {
    BoolFilterBuilder fb = FilterBuilders.boolFilter();
    this.addTermFilter(RuleNormalizer.RuleField.LANGUAGE.key(), query.getLanguages(), fb);
    this.addTermFilter(RuleNormalizer.RuleField.REPOSITORY.key(), query.getRepositories(), fb);
    this.addTermFilter(RuleNormalizer.RuleField.SEVERITY.key(), query.getSeverities(), fb);
    this.addTermFilter(RuleNormalizer.RuleField.KEY.key(), query.getKey(), fb);

    this.addMultiFieldTermFilter(query.getTags(), fb, RuleNormalizer.RuleField.TAGS.key(), RuleNormalizer.RuleField.SYSTEM_TAGS.key());

    if (query.getStatuses() != null && !query.getStatuses().isEmpty()) {
      Collection<String> stringStatus = new ArrayList<String>();
      for (RuleStatus status : query.getStatuses()) {
        stringStatus.add(status.name());
      }
      this.addTermFilter(RuleNormalizer.RuleField.STATUS.key(), stringStatus, fb);
    }

    if ((query.getLanguages() != null && !query.getLanguages().isEmpty()) ||
      (query.getRepositories() != null && !query.getRepositories().isEmpty()) ||
      (query.getSeverities() != null && !query.getSeverities().isEmpty()) ||
      (query.getTags() != null && !query.getTags().isEmpty()) ||
      (query.getStatuses() != null && !query.getStatuses().isEmpty()) ||
      (query.getKey() != null && !query.getKey().isEmpty())) {
      return fb;
    } else {
      return FilterBuilders.matchAllFilter();
    }
  }

  protected void setFacets(SearchRequestBuilder query) {
    //TODO there are no aggregation in 0.9!!! Must use facet...

     /* the Lang facet */
    query.addFacet(FacetBuilders.termsFacet("Languages")
      .field(RuleNormalizer.RuleField.LANGUAGE.key())
      .size(10)
      .global(true)
      .order(TermsFacet.ComparatorType.COUNT));

    /* the Tag facet */
    query.addFacet(FacetBuilders.termsFacet("Tags")
      .field(RuleNormalizer.RuleField.TAGS.key())
      .size(10)
      .global(true)
      .order(TermsFacet.ComparatorType.COUNT));

    /* the Repo facet */
    query.addFacet(FacetBuilders.termsFacet("Repositories")
      .field(RuleNormalizer.RuleField.REPOSITORY.key())
      .size(10)
      .global(true)
      .order(TermsFacet.ComparatorType.COUNT));
  }

  public RuleResult search(RuleQuery query, QueryOptions options) {
    SearchRequestBuilder esSearch = this.buildRequest(query, options);
    FilterBuilder fb = this.getFilter(query, options);
    QueryBuilder qb = this.getQuery(query, options);

    esSearch.setQuery(QueryBuilders.filteredQuery(qb, fb));

    SearchResponse esResult = esSearch.get();

    return new RuleResult(esResult);
  }


  public Rule toDoc(GetResponse response) {
    Preconditions.checkArgument(response != null, "Cannot construct Rule with null response!!!");
    return new RuleDoc(response.getSource());
  }

  public Set<String> terms(String... fields) {
    Set<String> tags = new HashSet<String>();

    SearchRequestBuilder request = this.getClient()
      .prepareSearch(this.getIndexName())
      .setQuery(QueryBuilders.matchAllQuery())
      .addFacet(FacetBuilders.termsFacet("tags")
        .allTerms(false)
        .fields(fields)
        .global(true)
        .size(Integer.MAX_VALUE));

    SearchResponse esResponse = request.get();

    TermsFacet termFacet = esResponse
      .getFacets().facet("tags");

    for (TermsFacet.Entry facetValue : termFacet.getEntries()) {
      tags.add(facetValue.getTerm().string());
    }
    return tags;
  }
}
