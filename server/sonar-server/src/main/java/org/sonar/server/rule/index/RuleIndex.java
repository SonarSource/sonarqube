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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.qualityprofile.index.ActiveRuleNormalizer;
import org.sonar.server.rule.Rule;
import org.sonar.server.search.*;

import javax.annotation.CheckForNull;

import java.io.IOException;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

public class RuleIndex extends BaseIndex<Rule, RuleDto, RuleKey> {

  public static final String FACET_LANGUAGES = "languages";
  public static final String FACET_TAGS = "tags";
  public static final String FACET_REPOSITORIES = "repositories";

  public RuleIndex(RuleNormalizer normalizer, SearchClient client) {
    super(IndexDefinition.RULE, normalizer, client);
  }

  protected String getKeyValue(RuleKey key) {
    return key.toString();
  }

  @Override
  protected Settings getIndexSettings() throws IOException {
    return ImmutableSettings.builder()
      .put("index.number_of_replicas", 0)
      .put("index.number_of_shards", 1)
      .build();
  }

  @Override
  protected Map mapKey() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    mapping.put("path", RuleNormalizer.RuleField.KEY.field());
    return mapping;
  }

  @Override
  protected Map mapProperties() {
    Map<String, Object> mapping = new HashMap<String, Object>();
    for (IndexField field : RuleNormalizer.RuleField.ALL_FIELDS) {
      mapping.put(field.field(), mapField(field));
    }
    return mapping;
  }

  private void setFields(QueryOptions options, SearchRequestBuilder esSearch) {
    /* integrate Option's Fields */
    Set<String> fields = new HashSet<String>();
    if (!options.getFieldsToReturn().isEmpty()) {
      for (String fieldToReturn : options.getFieldsToReturn()) {
        if (!fieldToReturn.isEmpty()) {
          fields.add(fieldToReturn);
        }
      }
      // required field
      fields.add(RuleNormalizer.RuleField.KEY.field());
    } else {
      for (IndexField indexField : RuleNormalizer.RuleField.ALL_FIELDS) {
        fields.add(indexField.field());
      }
    }

    esSearch.setFetchSource(fields.toArray(new String[fields.size()]), null);
  }

  private void setSorting(RuleQuery query, SearchRequestBuilder esSearch) {
    /* integrate Query Sort */
    String queryText = query.getQueryText();
    if (query.getSortField() != null) {
      FieldSortBuilder sort = SortBuilders.fieldSort(query.getSortField().sortField());
      if (query.isAscendingSort()) {
        sort.order(SortOrder.ASC);
      } else {
        sort.order(SortOrder.DESC);
      }
      esSearch.addSort(sort);
    } else if (queryText != null && !queryText.isEmpty()) {
      esSearch.addSort(SortBuilders.scoreSort());
    } else {
      esSearch.addSort(RuleNormalizer.RuleField.UPDATED_AT.sortField(), SortOrder.DESC);
      // deterministic sort when exactly the same updated_at (same millisecond)
      esSearch.addSort(RuleNormalizer.RuleField.KEY.sortField()
        , SortOrder.ASC);
    }
  }

  protected void setPagination(QueryOptions options, SearchRequestBuilder esSearch) {
    esSearch.setFrom(options.getOffset());
    esSearch.setSize(options.getLimit());
  }

  private QueryBuilder termQuery(IndexField field, String query, float boost) {
    return QueryBuilders.multiMatchQuery(query,
      field.field(), field.field() + "." + IndexField.SEARCH_PARTIAL_SUFFIX)
      .operator(MatchQueryBuilder.Operator.AND)
      .boost(boost);
  }

  private QueryBuilder termAnyQuery(IndexField field, String query, float boost) {
    return QueryBuilders.multiMatchQuery(query,
      field.field(), field.field() + "." + IndexField.SEARCH_PARTIAL_SUFFIX)
      .operator(MatchQueryBuilder.Operator.OR)
      .boost(boost);
  }

  /* Build main query (search based) */
  protected QueryBuilder getQuery(RuleQuery query) {

    // No contextual query case
    String queryText = query.getQueryText();
    if (queryText == null || queryText.isEmpty()) {
      return QueryBuilders.matchAllQuery();
    }

    // Build RuleBased contextual query
    BoolQueryBuilder qb = QueryBuilders.boolQuery();
    String queryString = query.getQueryText();

    // Human readable type of querying
    qb.should(QueryBuilders.simpleQueryString(query.getQueryText())
        .field(RuleNormalizer.RuleField.NAME.field() + "." + IndexField.SEARCH_WORDS_SUFFIX, 20f)
        .field(RuleNormalizer.RuleField.HTML_DESCRIPTION.field() + "." + IndexField.SEARCH_WORDS_SUFFIX, 3f)
        .defaultOperator(SimpleQueryStringBuilder.Operator.AND)
    ).boost(20f);

    // Match and partial Match queries
    qb.should(this.termQuery(RuleNormalizer.RuleField.KEY, queryString, 15f));
    qb.should(this.termQuery(RuleNormalizer.RuleField._KEY, queryString, 35f));
    qb.should(this.termQuery(RuleNormalizer.RuleField.LANGUAGE, queryString, 3f));
    qb.should(this.termQuery(RuleNormalizer.RuleField.CHARACTERISTIC, queryString, 5f));
    qb.should(this.termQuery(RuleNormalizer.RuleField.SUB_CHARACTERISTIC, queryString, 5f));
    qb.should(this.termQuery(RuleNormalizer.RuleField._TAGS, queryString, 10f));
    qb.should(this.termAnyQuery(RuleNormalizer.RuleField.CHARACTERISTIC, queryString, 1f));
    qb.should(this.termAnyQuery(RuleNormalizer.RuleField.SUB_CHARACTERISTIC, queryString, 1f));
    qb.should(this.termAnyQuery(RuleNormalizer.RuleField._TAGS, queryString, 1f));

    return qb;
  }

  /* Build main filter (match based) */
  protected HashMap<String, FilterBuilder> getFilters(RuleQuery query, QueryOptions options) {

    HashMap<String, FilterBuilder> filters = new HashMap<String, FilterBuilder>();

    /* Add enforced filter on rules that are REMOVED */
    filters.put(RuleNormalizer.RuleField.STATUS.field(),
      FilterBuilders.boolFilter().mustNot(
        FilterBuilders.termFilter(RuleNormalizer.RuleField.STATUS.field(),
          RuleStatus.REMOVED.toString())));

    if (!StringUtils.isEmpty(query.getInternalKey())) {
      filters.put(RuleNormalizer.RuleField.INTERNAL_KEY.field(),
        FilterBuilders.termFilter(RuleNormalizer.RuleField.INTERNAL_KEY.field(), query.getInternalKey()));
    }

    if (!StringUtils.isEmpty(query.getRuleKey())) {
      filters.put(RuleNormalizer.RuleField.RULE_KEY.field(),
        FilterBuilders.termFilter(RuleNormalizer.RuleField.RULE_KEY.field(), query.getRuleKey()));
    }

    if (!CollectionUtils.isEmpty(query.getLanguages())) {
      filters.put(RuleNormalizer.RuleField.LANGUAGE.field(),
        FilterBuilders.termsFilter(RuleNormalizer.RuleField.LANGUAGE.field(), query.getLanguages()));
    }

    if (!CollectionUtils.isEmpty(query.getRepositories())) {
      filters.put(RuleNormalizer.RuleField.REPOSITORY.field(),
        FilterBuilders.termsFilter(RuleNormalizer.RuleField.REPOSITORY.field(), query.getRepositories()));
    }

    if (!CollectionUtils.isEmpty(query.getSeverities())) {
      filters.put(RuleNormalizer.RuleField.SEVERITY.field(),
        FilterBuilders.termsFilter(RuleNormalizer.RuleField.SEVERITY.field(), query.getSeverities()));
    }

    if (!StringUtils.isEmpty(query.getKey())) {
      filters.put(RuleNormalizer.RuleField.KEY.field(),
        FilterBuilders.termFilter(RuleNormalizer.RuleField.KEY.field(), query.getKey()));
    }

    if (!CollectionUtils.isEmpty(query.getTags())) {
      filters.put(RuleNormalizer.RuleField._TAGS.field(),
        FilterBuilders.termsFilter(RuleNormalizer.RuleField._TAGS.field(), query.getTags()));
    }

    // Construct the debt filter on effective char and subChar
    Collection<String> debtCharacteristics = query.getDebtCharacteristics();
    if (debtCharacteristics != null && !debtCharacteristics.isEmpty()) {
      filters.put("debtCharacteristics",
        FilterBuilders.boolFilter().must(
          FilterBuilders.orFilter(
            // Match only when NONE (overridden)
            FilterBuilders.andFilter(
              FilterBuilders.notFilter(
                FilterBuilders.termsFilter(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field(), DebtCharacteristic.NONE)),
              FilterBuilders.orFilter(
                FilterBuilders.termsFilter(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field(), debtCharacteristics),
                FilterBuilders.termsFilter(RuleNormalizer.RuleField.CHARACTERISTIC.field(), debtCharacteristics))
              ),

            // Match only when NOT NONE (not overridden)
            FilterBuilders.andFilter(
              FilterBuilders.orFilter(
                FilterBuilders.termsFilter(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field(), ""),
                FilterBuilders.notFilter(FilterBuilders.existsFilter(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field()))),
              FilterBuilders.orFilter(
                FilterBuilders.termsFilter(RuleNormalizer.RuleField.DEFAULT_SUB_CHARACTERISTIC.field(), debtCharacteristics),
                FilterBuilders.termsFilter(RuleNormalizer.RuleField.DEFAULT_CHARACTERISTIC.field(), debtCharacteristics)))
            )
          ));
    }

    // Debt char exist filter
    Boolean hasDebtCharacteristic = query.getHasDebtCharacteristic();
    if (hasDebtCharacteristic != null && hasDebtCharacteristic) {
      filters.put("hasDebtCharacteristic",
        FilterBuilders.boolFilter().mustNot(
          FilterBuilders.termsFilter(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field(), DebtCharacteristic.NONE))
          .should(
            FilterBuilders.existsFilter(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field()))
          .should(
            FilterBuilders.existsFilter(RuleNormalizer.RuleField.DEFAULT_SUB_CHARACTERISTIC.field())));
    }

    if (query.getAvailableSince() != null) {
      filters.put("availableSince", FilterBuilders.rangeFilter(RuleNormalizer.RuleField.CREATED_AT.field())
        .gte(query.getAvailableSince()));
    }

    Collection<RuleStatus> statusValues = query.getStatuses();
    if (statusValues != null && !statusValues.isEmpty()) {
      Collection<String> stringStatus = new ArrayList<String>();
      for (RuleStatus status : statusValues) {
        stringStatus.add(status.name());
      }
      filters.put(RuleNormalizer.RuleField.STATUS.field(),
        FilterBuilders.termsFilter(RuleNormalizer.RuleField.STATUS.field(), stringStatus));
    }

    Boolean isTemplate = query.isTemplate();
    if (isTemplate != null) {
      filters.put(RuleNormalizer.RuleField.IS_TEMPLATE.field(),
        FilterBuilders.termFilter(RuleNormalizer.RuleField.IS_TEMPLATE.field(), Boolean.toString(isTemplate)));
    }

    String template = query.templateKey();
    if (template != null) {
      filters.put(RuleNormalizer.RuleField.TEMPLATE_KEY.field(),
        FilterBuilders.termFilter(RuleNormalizer.RuleField.TEMPLATE_KEY.field(), template));
    }

    // ActiveRule Filter (profile and inheritance)
    BoolFilterBuilder childrenFilter = FilterBuilders.boolFilter();
    this.addTermFilter(childrenFilter, ActiveRuleNormalizer.ActiveRuleField.PROFILE_KEY.field(), query.getQProfileKey());
    this.addTermFilter(childrenFilter, ActiveRuleNormalizer.ActiveRuleField.INHERITANCE.field(), query.getInheritance());
    this.addTermFilter(childrenFilter, ActiveRuleNormalizer.ActiveRuleField.SEVERITY.field(), query.getActiveSeverities());

    // ChildQuery
    QueryBuilder childQuery;
    if (childrenFilter.hasClauses()) {
      childQuery = QueryBuilders.constantScoreQuery(childrenFilter);
    } else {
      childQuery = QueryBuilders.matchAllQuery();
    }

    /** Implementation of activation query */
    if (Boolean.TRUE.equals(query.getActivation())) {
      filters.put("activation",
        FilterBuilders.hasChildFilter(IndexDefinition.ACTIVE_RULE.getIndexType(),
          childQuery));
    } else if (Boolean.FALSE.equals(query.getActivation())) {
      filters.put("activation",
        FilterBuilders.boolFilter().mustNot(
          FilterBuilders.hasChildFilter(IndexDefinition.ACTIVE_RULE.getIndexType(),
            childQuery)));
    }

    return filters;
  }

  protected Map<String, AggregationBuilder> getFacets(QueryBuilder query, HashMap<String, FilterBuilder> filters) {
    Map<String, AggregationBuilder> aggregations = new HashMap<String, AggregationBuilder>();

    BoolFilterBuilder langFacetFilter = FilterBuilders.boolFilter().must(FilterBuilders.queryFilter(query));
    for (Map.Entry<String, FilterBuilder> filter : filters.entrySet()) {
      if (filter.getKey() != RuleNormalizer.RuleField.LANGUAGE.field()) {
        langFacetFilter.must(filter.getValue());
      }
    }
    /* the Lang facet */
    aggregations.put(FACET_LANGUAGES + "global",
      AggregationBuilders
        .global(FACET_LANGUAGES)
        .subAggregation(
          AggregationBuilders
            .filter(FACET_LANGUAGES + "_filter")
            .filter(langFacetFilter)
            .subAggregation(
              AggregationBuilders.terms(FACET_LANGUAGES)
                .field(RuleNormalizer.RuleField.LANGUAGE.field())
                .order(Terms.Order.count(false))
                .size(10)
                .minDocCount(1))));

    BoolFilterBuilder tagsFacetFilter = FilterBuilders.boolFilter().must(FilterBuilders.queryFilter(query));
    for (Map.Entry<String, FilterBuilder> filter : filters.entrySet()) {
      if (filter.getKey() != RuleNormalizer.RuleField._TAGS.field()) {
        tagsFacetFilter.must(filter.getValue());
      }
    }
    /* the Tag facet */
    aggregations.put(FACET_TAGS + "global",
      AggregationBuilders
        .global(FACET_TAGS)
        .subAggregation(
          AggregationBuilders
            .filter(FACET_TAGS + "_filter")
            .filter(tagsFacetFilter)
            .subAggregation(
              AggregationBuilders.terms(FACET_TAGS)
                .field(RuleNormalizer.RuleField._TAGS.field())
                .order(Terms.Order.count(false))
                .size(10)
                .minDocCount(1))));

    BoolFilterBuilder repositoriesFacetFilter = FilterBuilders.boolFilter().must(FilterBuilders.queryFilter(query));
    for (Map.Entry<String, FilterBuilder> filter : filters.entrySet()) {
      if (filter.getKey() != RuleNormalizer.RuleField.REPOSITORY.field()) {
        repositoriesFacetFilter.must(filter.getValue());
      }
    }
    /* the Repo facet */
    aggregations.put(FACET_REPOSITORIES + "global",
      AggregationBuilders
        .global(FACET_REPOSITORIES)
        .subAggregation(
          AggregationBuilders
            .filter(FACET_REPOSITORIES + "_filter")
            .filter(repositoriesFacetFilter)
            .subAggregation(
              AggregationBuilders.terms(FACET_REPOSITORIES)
                .field(RuleNormalizer.RuleField.REPOSITORY.field())
                .order(Terms.Order.count(false))
                .size(10)
                .minDocCount(1))));

    return aggregations;

  }

  public Result<Rule> search(RuleQuery query, QueryOptions options) {
    SearchRequestBuilder esSearch = getClient()
      .prepareSearch(this.getIndexName())
      .setTypes(this.getIndexType())
      .setIndices(this.getIndexName());

    if (options.isScroll()) {
      esSearch.setSearchType(SearchType.SCAN);
      esSearch.setScroll(TimeValue.timeValueMinutes(3));
    }

    QueryBuilder qb = this.getQuery(query);
    HashMap<String, FilterBuilder> filters = this.getFilters(query, options);

    if (options.isFacet()) {
      for (AggregationBuilder aggregation : getFacets(qb, filters).values()) {
        esSearch.addAggregation(aggregation);
      }
    }

    setSorting(query, esSearch);
    setPagination(options, esSearch);
    setFields(options, esSearch);

    BoolFilterBuilder fb = FilterBuilders.boolFilter();
    for (FilterBuilder ffb : filters.values()) {
      fb.must(ffb);
    }

    esSearch.setQuery(QueryBuilders.filteredQuery(qb, fb));
    SearchResponse esResult = getClient().execute(esSearch);

    return new Result<Rule>(this, esResult);
  }

  @Override
  protected Rule toDoc(Map<String, Object> fields) {
    Preconditions.checkNotNull(fields, "Cannot construct Rule with null response");
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

    SearchResponse esResponse = getClient().execute(request);

    Terms aggregation = esResponse.getAggregations().get(key);

    if (aggregation != null) {
      for (Terms.Bucket value : aggregation.getBuckets()) {
        tags.add(value.getKey());
      }
    }
    return tags;
  }

  /**
   * @deprecated please use getByKey(RuleKey key)
   */
  @Deprecated
  @CheckForNull
  public Rule getById(int id) {
    SearchRequestBuilder request = getClient().prepareSearch(this.getIndexName())
      .setTypes(this.getIndexType())
      .setQuery(QueryBuilders.termQuery(RuleNormalizer.RuleField.ID.field(), id))
      .setSize(1);
    SearchResponse response = getClient().execute(request);

    SearchHit hit = response.getHits().getAt(0);
    if (hit == null) {
      return null;
    } else {
      return toDoc(hit.getSource());
    }
  }

  /**
   * @deprecated please use getByKey(RuleKey key)
   */
  @Deprecated
  public List<Rule> getByIds(Collection<Integer> ids) {
    SearchRequestBuilder request = getClient().prepareSearch(this.getIndexName())
      .setTypes(this.getIndexType())
      .setSearchType(SearchType.SCAN)
      .setScroll(TimeValue.timeValueSeconds(3L))
      .setSize(100)
      .setQuery(QueryBuilders.termsQuery(RuleNormalizer.RuleField.ID.field(), ids));
    SearchResponse scrollResp = getClient().execute(request);

    List<Rule> rules = newArrayList();
    while (true) {
      SearchScrollRequestBuilder scrollRequest = getClient()
        .prepareSearchScroll(scrollResp.getScrollId())
        .setScroll(TimeValue.timeValueSeconds(3L));

      scrollResp = getClient().execute(scrollRequest);

      for (SearchHit hit : scrollResp.getHits()) {
        rules.add(toDoc(hit.getSource()));
      }
      //Break condition: No hits are returned
      if (scrollResp.getHits().getHits().length == 0) {
        break;
      }
    }
    return rules;
  }
}
