/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.rule.index;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.join.aggregations.JoinAggregationBuilders;
import org.elasticsearch.join.query.HasParentQueryBuilder;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.support.IncludeExclude;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.es.DefaultIndexSettings;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsUtils;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.StickyFacetBuilder;
import org.sonar.server.es.textsearch.JavaTokenizer;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.sonar.server.es.DefaultIndexSettingsElement.ENGLISH_HTML_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettingsElement.SEARCH_GRAMS_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettingsElement.SEARCH_WORDS_ANALYZER;
import static org.sonar.server.es.DefaultIndexSettingsElement.SORTABLE_ANALYZER;
import static org.sonar.server.es.EsUtils.SCROLL_TIME_IN_MINUTES;
import static org.sonar.server.es.EsUtils.escapeSpecialRegexChars;
import static org.sonar.server.es.EsUtils.optimizeScrollRequest;
import static org.sonar.server.es.EsUtils.scrollIds;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_INHERITANCE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_PROFILE_UUID;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_SEVERITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_CREATED_AT;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_EXTENSION_SCOPE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_EXTENSION_TAGS;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_HTML_DESCRIPTION;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_INTERNAL_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_IS_TEMPLATE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_LANGUAGE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_NAME;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_REPOSITORY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_RULE_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_SEVERITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_STATUS;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_TEMPLATE_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_TYPE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_UPDATED_AT;
import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX_TYPE_ACTIVE_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX_TYPE_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.INDEX_TYPE_RULE_EXTENSION;

/**
 * The unique entry-point to interact with Elasticsearch index "rules".
 * All the requests are listed here.
 */
public class RuleIndex {

  public static final String FACET_LANGUAGES = "languages";
  public static final String FACET_TAGS = "tags";
  public static final String FACET_REPOSITORIES = "repositories";
  public static final String FACET_SEVERITIES = "severities";
  public static final String FACET_ACTIVE_SEVERITIES = "active_severities";
  public static final String FACET_STATUSES = "statuses";
  public static final String FACET_TYPES = "types";
  public static final String FACET_OLD_DEFAULT = "true";

  public static final List<String> ALL_STATUSES_EXCEPT_REMOVED = Arrays.stream(RuleStatus.values())
    .filter(status -> !RuleStatus.REMOVED.equals(status))
    .map(RuleStatus::toString)
    .collect(MoreCollectors.toList());

  private static final String AGGREGATION_NAME = "_ref";
  private static final String AGGREGATION_NAME_FOR_TAGS = "tagsAggregation";

  private final EsClient client;
  private final System2 system2;

  public RuleIndex(EsClient client, System2 system2) {
    this.client = client;
    this.system2 = system2;
  }

  public SearchIdResult<Integer> search(RuleQuery query, SearchOptions options) {
    SearchRequestBuilder esSearch = client
      .prepareSearch(INDEX_TYPE_RULE);

    QueryBuilder qb = buildQuery(query);
    Map<String, QueryBuilder> filters = buildFilters(query);

    if (!options.getFacets().isEmpty()) {
      for (AggregationBuilder aggregation : getFacets(query, options, qb, filters).values()) {
        esSearch.addAggregation(aggregation);
      }
    }

    setSorting(query, esSearch);
    setPagination(options, esSearch);

    BoolQueryBuilder fb = boolQuery();
    for (QueryBuilder filterBuilder : filters.values()) {
      fb.must(filterBuilder);
    }

    esSearch.setQuery(boolQuery().must(qb).filter(fb));
    return new SearchIdResult<>(esSearch.get(), Integer::parseInt, system2.getDefaultTimeZone());
  }

  /**
   * Return all rule ids matching the search query, without pagination nor facets
   */
  public Iterator<Integer> searchAll(RuleQuery query) {
    SearchRequestBuilder esSearch = client
      .prepareSearch(INDEX_TYPE_RULE)
      .setScroll(TimeValue.timeValueMinutes(SCROLL_TIME_IN_MINUTES));

    optimizeScrollRequest(esSearch);
    QueryBuilder qb = buildQuery(query);
    Map<String, QueryBuilder> filters = buildFilters(query);

    BoolQueryBuilder fb = boolQuery();
    for (QueryBuilder filterBuilder : filters.values()) {
      fb.must(filterBuilder);
    }

    esSearch.setQuery(boolQuery().must(qb).filter(fb));
    SearchResponse response = esSearch.get();
    return scrollIds(client, response, Integer::parseInt);
  }

  /* Build main query (search based) */
  private static QueryBuilder buildQuery(RuleQuery query) {

    // No contextual query case
    String queryText = query.getQueryText();
    if (StringUtils.isEmpty(queryText)) {
      return matchAllQuery();
    }

    // Build RuleBased contextual query
    BoolQueryBuilder qb = boolQuery();
    String queryString = query.getQueryText();

    if (queryString != null && !queryString.isEmpty()) {
      BoolQueryBuilder textQuery = boolQuery();
      JavaTokenizer.split(queryString)
        .stream().map(token -> boolQuery().should(
          matchQuery(
            SEARCH_GRAMS_ANALYZER.subField(FIELD_RULE_NAME),
            StringUtils.left(token, DefaultIndexSettings.MAXIMUM_NGRAM_LENGTH)
          ).boost(20f)).should(
          matchPhraseQuery(
            ENGLISH_HTML_ANALYZER.subField(FIELD_RULE_HTML_DESCRIPTION),
            token
          ).boost(3f))
      ).forEach(textQuery::must);
      qb.should(textQuery.boost(20f));
    }

    // Match and partial Match queries
    // Search by key uses the "sortable" sub-field as it requires to be case-insensitive (lower-case filtering)
    qb.should(matchQuery(SORTABLE_ANALYZER.subField(FIELD_RULE_KEY), queryString).operator(Operator.AND).boost(30f));
    qb.should(matchQuery(SORTABLE_ANALYZER.subField(FIELD_RULE_RULE_KEY), queryString).operator(Operator.AND).boost(15f));
    qb.should(termQuery(FIELD_RULE_LANGUAGE, queryString, 3f));
    return qb;
  }

  private static QueryBuilder termQuery(String field, String query, float boost) {
    return QueryBuilders.multiMatchQuery(query,
      field, SEARCH_WORDS_ANALYZER.subField(field))
      .operator(Operator.AND)
      .boost(boost);
  }

  /* Build main filter (match based) */
  private static Map<String, QueryBuilder> buildFilters(RuleQuery query) {
    Map<String, QueryBuilder> filters = new HashMap<>();

    /* Add enforced filter on rules that are REMOVED */
    filters.put(FIELD_RULE_STATUS,
      boolQuery().mustNot(
        QueryBuilders.termQuery(FIELD_RULE_STATUS,
          RuleStatus.REMOVED.toString())));

    if (StringUtils.isNotEmpty(query.getInternalKey())) {
      filters.put(FIELD_RULE_INTERNAL_KEY,
        QueryBuilders.termQuery(FIELD_RULE_INTERNAL_KEY, query.getInternalKey()));
    }

    if (StringUtils.isNotEmpty(query.getRuleKey())) {
      filters.put(FIELD_RULE_RULE_KEY,
        QueryBuilders.termQuery(FIELD_RULE_RULE_KEY, query.getRuleKey()));
    }

    if (isNotEmpty(query.getLanguages())) {
      filters.put(FIELD_RULE_LANGUAGE,
        QueryBuilders.termsQuery(FIELD_RULE_LANGUAGE, query.getLanguages()));
    }

    if (isNotEmpty(query.getRepositories())) {
      filters.put(FIELD_RULE_REPOSITORY,
        QueryBuilders.termsQuery(FIELD_RULE_REPOSITORY, query.getRepositories()));
    }

    if (isNotEmpty(query.getSeverities())) {
      filters.put(FIELD_RULE_SEVERITY,
        QueryBuilders.termsQuery(FIELD_RULE_SEVERITY, query.getSeverities()));
    }

    if (StringUtils.isNotEmpty(query.getKey())) {
      filters.put(FIELD_RULE_KEY,
        QueryBuilders.termQuery(FIELD_RULE_KEY, query.getKey()));
    }

    if (isNotEmpty(query.getTags())) {
      filters.put(FIELD_RULE_EXTENSION_TAGS,
        buildTagsFilter(query.getTags(), query.getOrganization()));
    }

    Collection<RuleType> types = query.getTypes();
    if (isNotEmpty(types)) {
      List<String> typeNames = types.stream().map(RuleType::toString).collect(MoreCollectors.toList());
      filters.put(FIELD_RULE_TYPE,
        QueryBuilders.termsQuery(FIELD_RULE_TYPE, typeNames));
    }

    if (query.getAvailableSinceLong() != null) {
      filters.put("availableSince", QueryBuilders.rangeQuery(FIELD_RULE_CREATED_AT)
        .gte(query.getAvailableSinceLong()));
    }

    if (isNotEmpty(query.getStatuses())) {
      Collection<String> stringStatus = new ArrayList<>();
      for (RuleStatus status : query.getStatuses()) {
        stringStatus.add(status.name());
      }
      filters.put(FIELD_RULE_STATUS,
        QueryBuilders.termsQuery(FIELD_RULE_STATUS, stringStatus));
    }

    Boolean isTemplate = query.isTemplate();
    if (isTemplate != null) {
      filters.put(FIELD_RULE_IS_TEMPLATE,
        QueryBuilders.termQuery(FIELD_RULE_IS_TEMPLATE, Boolean.toString(isTemplate)));
    }

    String template = query.templateKey();
    if (template != null) {
      filters.put(FIELD_RULE_TEMPLATE_KEY,
        QueryBuilders.termQuery(FIELD_RULE_TEMPLATE_KEY, template));
    }

    /* Implementation of activation query */
    QProfileDto profile = query.getQProfile();
    if (query.getActivation() != null && profile != null) {
      QueryBuilder childQuery = buildActivationFilter(query, profile);

      if (TRUE.equals(query.getActivation())) {
        filters.put("activation",
          JoinQueryBuilders.hasChildQuery(INDEX_TYPE_ACTIVE_RULE.getType(),
            childQuery, ScoreMode.None));
      } else if (FALSE.equals(query.getActivation())) {
        filters.put("activation",
          boolQuery().mustNot(
            JoinQueryBuilders.hasChildQuery(INDEX_TYPE_ACTIVE_RULE.getType(),
              childQuery, ScoreMode.None)));
      }
      QProfileDto compareToQProfile = query.getCompareToQProfile();
      if (compareToQProfile != null) {
        filters.put("comparison",
          JoinQueryBuilders.hasChildQuery(
            INDEX_TYPE_ACTIVE_RULE.getType(),
            boolQuery().must(QueryBuilders.termQuery(FIELD_ACTIVE_RULE_PROFILE_UUID, compareToQProfile.getRulesProfileUuid())),
            ScoreMode.None));
      }
    }

    return filters;
  }

  private static BoolQueryBuilder buildTagsFilter(Collection<String> tags, OrganizationDto organization) {
    BoolQueryBuilder q = boolQuery();
    tags.stream()
      .map(tag -> boolQuery()
        .filter(QueryBuilders.termQuery(FIELD_RULE_EXTENSION_TAGS, tag))
        .filter(termsQuery(FIELD_RULE_EXTENSION_SCOPE, RuleExtensionScope.system().getScope(), RuleExtensionScope.organization(organization).getScope())))
      .map(childQuery -> JoinQueryBuilders.hasChildQuery(INDEX_TYPE_RULE_EXTENSION.getType(), childQuery, ScoreMode.None))
      .forEach(q::should);
    return q;
  }

  private static QueryBuilder buildActivationFilter(RuleQuery query, QProfileDto profile) {
    // ActiveRule Filter (profile and inheritance)
    BoolQueryBuilder activeRuleFilter = boolQuery();
    addTermFilter(activeRuleFilter, FIELD_ACTIVE_RULE_PROFILE_UUID, profile.getRulesProfileUuid());
    addTermFilter(activeRuleFilter, FIELD_ACTIVE_RULE_INHERITANCE, query.getInheritance());
    addTermFilter(activeRuleFilter, FIELD_ACTIVE_RULE_SEVERITY, query.getActiveSeverities());

    // ChildQuery
    QueryBuilder childQuery;
    if (activeRuleFilter.hasClauses()) {
      childQuery = activeRuleFilter;
    } else {
      childQuery = matchAllQuery();
    }
    return childQuery;
  }

  private static BoolQueryBuilder addTermFilter(BoolQueryBuilder filter, String field, @Nullable Collection<String> values) {
    if (isNotEmpty(values)) {
      BoolQueryBuilder valuesFilter = boolQuery();
      for (String value : values) {
        QueryBuilder valueFilter = QueryBuilders.termQuery(field, value);
        valuesFilter.should(valueFilter);
      }
      filter.must(valuesFilter);
    }
    return filter;
  }

  private static BoolQueryBuilder addTermFilter(BoolQueryBuilder filter, String field, @Nullable String value) {
    if (StringUtils.isNotEmpty(value)) {
      filter.must(QueryBuilders.termQuery(field, value));
    }
    return filter;
  }

  private static Map<String, AggregationBuilder> getFacets(RuleQuery query, SearchOptions options, QueryBuilder queryBuilder, Map<String, QueryBuilder> filters) {
    Map<String, AggregationBuilder> aggregations = new HashMap<>();
    StickyFacetBuilder stickyFacetBuilder = stickyFacetBuilder(queryBuilder, filters);

    addDefaultFacets(query, options, aggregations, stickyFacetBuilder);

    addStatusFacetIfNeeded(options, aggregations, stickyFacetBuilder);

    if (options.getFacets().contains(FACET_SEVERITIES)) {
      aggregations.put(FACET_SEVERITIES,
        stickyFacetBuilder.buildStickyFacet(FIELD_RULE_SEVERITY, FACET_SEVERITIES, Severity.ALL.toArray()));
    }

    addActiveSeverityFacetIfNeeded(query, options, aggregations, stickyFacetBuilder);
    return aggregations;
  }

  private static void addDefaultFacets(RuleQuery query, SearchOptions options, Map<String, AggregationBuilder> aggregations, StickyFacetBuilder stickyFacetBuilder) {
    if (options.getFacets().contains(FACET_LANGUAGES) || options.getFacets().contains(FACET_OLD_DEFAULT)) {
      Collection<String> languages = query.getLanguages();
      aggregations.put(FACET_LANGUAGES,
        stickyFacetBuilder.buildStickyFacet(FIELD_RULE_LANGUAGE, FACET_LANGUAGES,
          (languages == null) ? (new String[0]) : languages.toArray()));
    }
    if (options.getFacets().contains(FACET_TAGS) || options.getFacets().contains(FACET_OLD_DEFAULT)) {
      Collection<String> tags = query.getTags();
      checkArgument(query.getOrganization() != null, "Cannot use tags facet, if no organization is specified.", query.getTags());

      Function<TermsAggregationBuilder, AggregationBuilder> childFeature = termsAggregation -> {

        FilterAggregationBuilder scopeAggregation = AggregationBuilders.filter(
          "scope_filter_for_" + FACET_TAGS,
          termsQuery(FIELD_RULE_EXTENSION_SCOPE,
            RuleExtensionScope.system().getScope(),
            RuleExtensionScope.organization(query.getOrganization()).getScope()))
          .subAggregation(termsAggregation);

        return JoinAggregationBuilders.children("children_for_" + termsAggregation.getName(), INDEX_TYPE_RULE_EXTENSION.getType())
          .subAggregation(scopeAggregation);
      };

      aggregations.put(FACET_TAGS,
        stickyFacetBuilder.buildStickyFacet(FIELD_RULE_EXTENSION_TAGS, FACET_TAGS, childFeature,
          (tags == null) ? (new String[0]) : tags.toArray()));
    }
    if (options.getFacets().contains(FACET_TYPES)) {
      Collection<RuleType> types = query.getTypes();
      aggregations.put(FACET_TYPES,
        stickyFacetBuilder.buildStickyFacet(FIELD_RULE_TYPE, FACET_TYPES,
          (types == null) ? (new String[0]) : types.toArray()));
    }
    if (options.getFacets().contains("repositories") || options.getFacets().contains(FACET_OLD_DEFAULT)) {
      Collection<String> repositories = query.getRepositories();
      aggregations.put(FACET_REPOSITORIES,
        stickyFacetBuilder.buildStickyFacet(FIELD_RULE_REPOSITORY, FACET_REPOSITORIES,
          (repositories == null) ? (new String[0]) : repositories.toArray()));
    }
  }

  private static void addStatusFacetIfNeeded(SearchOptions options, Map<String, AggregationBuilder> aggregations, StickyFacetBuilder stickyFacetBuilder) {
    if (options.getFacets().contains(FACET_STATUSES)) {
      BoolQueryBuilder facetFilter = stickyFacetBuilder.getStickyFacetFilter(FIELD_RULE_STATUS);
      AggregationBuilder statuses = AggregationBuilders.filter(FACET_STATUSES + "_filter", facetFilter)
        .subAggregation(
          AggregationBuilders
            .terms(FACET_STATUSES)
            .field(FIELD_RULE_STATUS)
            .includeExclude(new IncludeExclude(Joiner.on('|').join(ALL_STATUSES_EXCEPT_REMOVED), RuleStatus.REMOVED.toString()))
            .size(ALL_STATUSES_EXCEPT_REMOVED.size()));

      aggregations.put(FACET_STATUSES, AggregationBuilders.global(FACET_STATUSES).subAggregation(statuses));
    }
  }

  private static void addActiveSeverityFacetIfNeeded(RuleQuery query, SearchOptions options, Map<String, AggregationBuilder> aggregations,
    StickyFacetBuilder stickyFacetBuilder) {
    QProfileDto profile = query.getQProfile();
    if (options.getFacets().contains(FACET_ACTIVE_SEVERITIES) && profile != null) {
      // We are building a children aggregation on active rules
      // so the rule filter has to be used as parent filter for active rules
      // from which we remove filters that concern active rules ("activation")
      HasParentQueryBuilder ruleFilter = JoinQueryBuilders.hasParentQuery(
        INDEX_TYPE_RULE.getType(),
        stickyFacetBuilder.getStickyFacetFilter("activation"),
        false);

      // Rebuilding the active rule filter without severities
      BoolQueryBuilder childrenFilter = boolQuery();
      addTermFilter(childrenFilter, FIELD_ACTIVE_RULE_PROFILE_UUID, profile.getRulesProfileUuid());
      RuleIndex.addTermFilter(childrenFilter, FIELD_ACTIVE_RULE_INHERITANCE, query.getInheritance());
      QueryBuilder activeRuleFilter = childrenFilter.must(ruleFilter);

      AggregationBuilder activeSeverities = JoinAggregationBuilders.children(FACET_ACTIVE_SEVERITIES + "_children", INDEX_TYPE_ACTIVE_RULE.getType())
        .subAggregation(
          AggregationBuilders.filter(FACET_ACTIVE_SEVERITIES + "_filter", activeRuleFilter)
            .subAggregation(
              AggregationBuilders
                .terms(FACET_ACTIVE_SEVERITIES)
                .field(FIELD_ACTIVE_RULE_SEVERITY)
                .includeExclude(new IncludeExclude(Joiner.on('|').join(Severity.ALL), null))
                .size(Severity.ALL.size())));

      aggregations.put(FACET_ACTIVE_SEVERITIES, AggregationBuilders.global(FACET_ACTIVE_SEVERITIES).subAggregation(activeSeverities));
    }
  }

  private static StickyFacetBuilder stickyFacetBuilder(QueryBuilder query, Map<String, QueryBuilder> filters) {
    return new StickyFacetBuilder(query, filters, null, Terms.Order.compound(Terms.Order.count(false), Terms.Order.term(true)));
  }

  private static void setSorting(RuleQuery query, SearchRequestBuilder esSearch) {
    /* integrate Query Sort */
    String queryText = query.getQueryText();
    if (query.getSortField() != null) {
      FieldSortBuilder sort = SortBuilders.fieldSort(appendSortSuffixIfNeeded(query.getSortField()));
      if (query.isAscendingSort()) {
        sort.order(SortOrder.ASC);
      } else {
        sort.order(SortOrder.DESC);
      }
      esSearch.addSort(sort);
    } else if (StringUtils.isNotEmpty(queryText)) {
      esSearch.addSort(SortBuilders.scoreSort());
    } else {
      esSearch.addSort(appendSortSuffixIfNeeded(FIELD_RULE_UPDATED_AT), SortOrder.DESC);
      // deterministic sort when exactly the same updated_at (same millisecond)
      esSearch.addSort(appendSortSuffixIfNeeded(FIELD_RULE_KEY), SortOrder.ASC);
    }
  }

  private static String appendSortSuffixIfNeeded(String field) {
    return field +
      ((field.equals(FIELD_RULE_NAME) || field.equals(FIELD_RULE_KEY))
        ? ("." + SORTABLE_ANALYZER.getSubFieldSuffix())
        : "");
  }

  private static void setPagination(SearchOptions options, SearchRequestBuilder esSearch) {
    esSearch.setFrom(options.getOffset());
    esSearch.setSize(options.getLimit());
  }

  public List<String> terms(String fields) {
    return terms(fields, null, Integer.MAX_VALUE);
  }

  public List<String> terms(String fields, @Nullable String query, int size) {
    TermsAggregationBuilder termsAggregation = AggregationBuilders.terms(AGGREGATION_NAME)
      .field(fields)
      .size(size)
      .minDocCount(1);
    if (query != null) {
      termsAggregation.includeExclude(new IncludeExclude(".*" + escapeSpecialRegexChars(query) + ".*", null));
    }
    SearchRequestBuilder request = client
      .prepareSearch(INDEX_TYPE_RULE, INDEX_TYPE_ACTIVE_RULE)
      .setQuery(matchAllQuery())
      .setSize(0)
      .addAggregation(termsAggregation);

    SearchResponse esResponse = request.get();
    return EsUtils.termsKeys(esResponse.getAggregations().get(AGGREGATION_NAME));
  }

  public List<String> listTags(@Nullable OrganizationDto organization, @Nullable String query, int size) {
    int maxPageSize = 500;
    checkArgument(size <= maxPageSize, "Page size must be lower than or equals to " + maxPageSize);
    if (size <= 0) {
      return emptyList();
    }

    ImmutableList.Builder<String> scopes = ImmutableList.<String>builder()
      .add(RuleExtensionScope.system().getScope());
    if (organization != null) {
      scopes.add(RuleExtensionScope.organization(organization).getScope());
    }
    TermsQueryBuilder scopeFilter = QueryBuilders.termsQuery(
      FIELD_RULE_EXTENSION_SCOPE,
      scopes.build().toArray(new String[0]));

    TermsAggregationBuilder termsAggregation = AggregationBuilders.terms(AGGREGATION_NAME_FOR_TAGS)
      .field(FIELD_RULE_EXTENSION_TAGS)
      .size(size)
      .order(Terms.Order.term(true))
      .minDocCount(1);
    ofNullable(query)
      .map(EsUtils::escapeSpecialRegexChars)
      .map(queryString -> ".*" + queryString + ".*")
      .map(s -> new IncludeExclude(s, null))
      .ifPresent(termsAggregation::includeExclude);

    SearchRequestBuilder request = client
      .prepareSearch(INDEX_TYPE_RULE_EXTENSION)
      .setQuery(boolQuery().filter(scopeFilter))
      .setSize(0)
      .addAggregation(termsAggregation);

    SearchResponse esResponse = request.get();
    return EsUtils.termsKeys(esResponse.getAggregations().get(AGGREGATION_NAME_FOR_TAGS));
  }

  private static boolean isNotEmpty(@Nullable Collection list) {
    return list != null && !list.isEmpty();
  }
}
