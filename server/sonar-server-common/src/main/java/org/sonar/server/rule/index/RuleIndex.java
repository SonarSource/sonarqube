/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.join.aggregations.JoinAggregationBuilders;
import org.elasticsearch.join.query.HasParentQueryBuilder;
import org.elasticsearch.join.query.JoinQueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsUtils;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.StickyFacetBuilder;
import org.sonar.server.es.newindex.DefaultIndexSettings;
import org.sonar.server.es.textsearch.JavaTokenizer;
import org.sonar.server.security.SecurityStandards;

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
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.server.es.EsUtils.SCROLL_TIME_IN_MINUTES;
import static org.sonar.server.es.EsUtils.optimizeScrollRequest;
import static org.sonar.server.es.EsUtils.scrollIds;
import static org.sonar.server.es.IndexType.FIELD_INDEX_TYPE;
import static org.sonar.server.es.StickyFacetBuilder.FACET_DEFAULT_SIZE;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.ENGLISH_HTML_ANALYZER;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SEARCH_GRAMS_ANALYZER;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SEARCH_WORDS_ANALYZER;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SORTABLE_ANALYZER;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_INHERITANCE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_PROFILE_UUID;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_SEVERITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_CREATED_AT;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_CWE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_HTML_DESCRIPTION;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_INTERNAL_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_IS_EXTERNAL;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_IS_TEMPLATE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_LANGUAGE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_NAME;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_OWASP_TOP_10;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_OWASP_TOP_10_2021;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_REPOSITORY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_RULE_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_SANS_TOP_25;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_SEVERITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_SONARSOURCE_SECURITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_STATUS;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_TAGS;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_TEMPLATE_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_TYPE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_UPDATED_AT;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_ACTIVE_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_RULE;

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
  public static final String FACET_CWE = "cwe";
  public static final String FACET_SANS_TOP_25 = "sansTop25";
  public static final String FACET_OWASP_TOP_10 = "owaspTop10";
  public static final String FACET_OWASP_TOP_10_2021 = "owaspTop10-2021";
  public static final String FACET_SONARSOURCE_SECURITY = "sonarsourceSecurity";

  private static final int MAX_FACET_SIZE = 100;

  public static final List<String> ALL_STATUSES_EXCEPT_REMOVED = Arrays.stream(RuleStatus.values())
    .filter(status -> !RuleStatus.REMOVED.equals(status))
    .map(RuleStatus::toString)
    .collect(MoreCollectors.toList());

  private static final String AGGREGATION_NAME_FOR_TAGS = "tagsAggregation";

  private final EsClient client;
  private final System2 system2;

  public RuleIndex(EsClient client, System2 system2) {
    this.client = client;
    this.system2 = system2;
  }

  public SearchIdResult<String> search(RuleQuery query, SearchOptions options) {
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

    QueryBuilder qb = buildQuery(query);
    Map<String, QueryBuilder> filters = buildFilters(query);

    if (!options.getFacets().isEmpty()) {
      for (AggregationBuilder aggregation : getFacets(query, options, qb, filters).values()) {
        sourceBuilder.aggregation(aggregation);
      }
    }

    setSorting(query, sourceBuilder);
    setPagination(options, sourceBuilder);

    BoolQueryBuilder fb = boolQuery();
    for (QueryBuilder filterBuilder : filters.values()) {
      fb.must(filterBuilder);
    }

    sourceBuilder.query(boolQuery().must(qb).filter(fb));

    SearchRequest esSearch = EsClient.prepareSearch(TYPE_RULE)
      .source(sourceBuilder);

    return new SearchIdResult<>(client.search(esSearch), input -> input, system2.getDefaultTimeZone().toZoneId());
  }

  /**
   * Return all rule uuids matching the search query, without pagination nor facets
   */
  public Iterator<String> searchAll(RuleQuery query) {
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

    optimizeScrollRequest(sourceBuilder);
    QueryBuilder qb = buildQuery(query);
    Map<String, QueryBuilder> filters = buildFilters(query);

    BoolQueryBuilder fb = boolQuery();
    for (QueryBuilder filterBuilder : filters.values()) {
      fb.must(filterBuilder);
    }

    sourceBuilder.query(boolQuery().must(qb).filter(fb));

    SearchRequest esSearch = EsClient.prepareSearch(TYPE_RULE)
      .scroll(TimeValue.timeValueMinutes(SCROLL_TIME_IN_MINUTES))
      .source(sourceBuilder);

    SearchResponse response = client.search(esSearch);
    return scrollIds(client, response, i -> i);
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

    BoolQueryBuilder textQuery = boolQuery();
    JavaTokenizer.split(queryText)
      .stream().map(token -> boolQuery().should(
          matchQuery(
            SEARCH_GRAMS_ANALYZER.subField(FIELD_RULE_NAME),
            StringUtils.left(token, DefaultIndexSettings.MAXIMUM_NGRAM_LENGTH)).boost(20F))
        .should(
          matchPhraseQuery(
            ENGLISH_HTML_ANALYZER.subField(FIELD_RULE_HTML_DESCRIPTION),
            token).boost(3F)))
      .forEach(textQuery::must);
    qb.should(textQuery.boost(20F));

    // Match and partial Match queries
    // Search by key uses the "sortable" sub-field as it requires to be case-insensitive (lower-case filtering)
    qb.should(matchQuery(SORTABLE_ANALYZER.subField(FIELD_RULE_KEY), queryText).operator(Operator.AND).boost(30F));
    qb.should(matchQuery(SORTABLE_ANALYZER.subField(FIELD_RULE_RULE_KEY), queryText).operator(Operator.AND).boost(15F));
    qb.should(termQuery(FIELD_RULE_LANGUAGE, queryText, 3F));
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

    /* Add enforced filter on main type Rule */
    filters.put(
      FIELD_INDEX_TYPE,
      boolQuery().must(QueryBuilders.termsQuery(FIELD_INDEX_TYPE, TYPE_RULE.getType())));

    /* Add enforced filter on rules that are REMOVED */
    filters.put(FIELD_RULE_STATUS,
      boolQuery().mustNot(
        QueryBuilders.termQuery(FIELD_RULE_STATUS,
          RuleStatus.REMOVED.toString())));

    addFilter(filters, FIELD_RULE_INTERNAL_KEY, query.getInternalKey());

    addFilter(filters, FIELD_RULE_RULE_KEY, query.getRuleKey());

    addFilter(filters, query.getLanguages(), FIELD_RULE_LANGUAGE);

    addFilter(filters, query.getRepositories(), FIELD_RULE_REPOSITORY);

    addFilter(filters, query.getSeverities(), FIELD_RULE_SEVERITY);

    addSecurityStandardFilter(filters, FIELD_RULE_CWE, query.getCwe());

    addSecurityStandardFilter(filters, FIELD_RULE_OWASP_TOP_10, query.getOwaspTop10());

    addSecurityStandardFilter(filters, FIELD_RULE_OWASP_TOP_10_2021, query.getOwaspTop10For2021());

    addSecurityStandardFilter(filters, FIELD_RULE_SANS_TOP_25, query.getSansTop25());

    addSecurityStandardFilter(filters, FIELD_RULE_SONARSOURCE_SECURITY, query.getSonarsourceSecurity());

    addFilter(filters, FIELD_RULE_KEY, query.getKey());

    if (isNotEmpty(query.getTags())) {
      filters.put(FIELD_RULE_TAGS, buildTagsFilter(query.getTags()));
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

    boolean includeExternal = query.includeExternal();
    if (!includeExternal) {
      filters.put(FIELD_RULE_IS_EXTERNAL,
        QueryBuilders.termQuery(FIELD_RULE_IS_EXTERNAL, false));
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
          JoinQueryBuilders.hasChildQuery(TYPE_ACTIVE_RULE.getName(),
            childQuery, ScoreMode.None));
      } else if (FALSE.equals(query.getActivation())) {
        filters.put("activation",
          boolQuery().mustNot(
            JoinQueryBuilders.hasChildQuery(TYPE_ACTIVE_RULE.getName(),
              childQuery, ScoreMode.None)));
      }
      QProfileDto compareToQProfile = query.getCompareToQProfile();
      if (compareToQProfile != null) {
        filters.put("comparison",
          JoinQueryBuilders.hasChildQuery(
            TYPE_ACTIVE_RULE.getName(),
            boolQuery().must(QueryBuilders.termQuery(FIELD_ACTIVE_RULE_PROFILE_UUID, compareToQProfile.getRulesProfileUuid())),
            ScoreMode.None));
      }
    }

    return filters;
  }

  private static void addSecurityStandardFilter(Map<String, QueryBuilder> filters, String key, Collection<String> values) {
    if (isNotEmpty(values)) {
      filters.put(key,
        boolQuery()
          .must(QueryBuilders.termsQuery(key, values))
          .must(QueryBuilders.termsQuery(FIELD_RULE_TYPE, VULNERABILITY.name(), SECURITY_HOTSPOT.name())));
    }
  }

  private static void addFilter(Map<String, QueryBuilder> filters, Collection<String> key, String value) {
    if (isNotEmpty(key)) {
      filters.put(value,
        QueryBuilders.termsQuery(value, key));
    }
  }

  private static void addFilter(Map<String, QueryBuilder> filters, String key, String value) {
    if (StringUtils.isNotEmpty(value)) {
      filters.put(key,
        QueryBuilders.termQuery(key, value));
    }
  }

  private static BoolQueryBuilder buildTagsFilter(Collection<String> tags) {
    BoolQueryBuilder q = boolQuery();
    for (String tag : tags) {
      q.should(boolQuery().filter(QueryBuilders.termQuery(FIELD_RULE_TAGS, tag)));
    }
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
        stickyFacetBuilder.buildStickyFacet(FIELD_RULE_LANGUAGE, FACET_LANGUAGES, MAX_FACET_SIZE,
          (languages == null) ? (new String[0]) : languages.toArray()));
    }
    if (options.getFacets().contains(FACET_TAGS) || options.getFacets().contains(FACET_OLD_DEFAULT)) {
      Collection<String> tags = query.getTags();
      aggregations.put(FACET_TAGS,
        stickyFacetBuilder.buildStickyFacet(FIELD_RULE_TAGS, FACET_TAGS, MAX_FACET_SIZE,
          (tags == null) ? (new String[0]) : tags.toArray()));
    }
    if (options.getFacets().contains(FACET_TYPES)) {
      Collection<RuleType> types = query.getTypes();
      aggregations.put(FACET_TYPES,
        stickyFacetBuilder.buildStickyFacet(FIELD_RULE_TYPE, FACET_TYPES,
          (types == null) ? (new String[0]) : types.toArray()));
    }
    if (options.getFacets().contains(FACET_REPOSITORIES) || options.getFacets().contains(FACET_OLD_DEFAULT)) {
      Collection<String> repositories = query.getRepositories();
      aggregations.put(FACET_REPOSITORIES,
        stickyFacetBuilder.buildStickyFacet(FIELD_RULE_REPOSITORY, FACET_REPOSITORIES, MAX_FACET_SIZE,
          (repositories == null) ? (new String[0]) : repositories.toArray()));
    }

    addDefaultSecurityFacets(query, options, aggregations, stickyFacetBuilder);
  }

  private static Function<TermsAggregationBuilder, AggregationBuilder> filterSecurityCategories() {
    return termsAggregation -> AggregationBuilders.filter(
        "filter_by_rule_types_" + termsAggregation.getName(),
        termsQuery(FIELD_RULE_TYPE,
          VULNERABILITY.name(),
          SECURITY_HOTSPOT.name()))
      .subAggregation(termsAggregation);
  }

  private static void addDefaultSecurityFacets(RuleQuery query, SearchOptions options, Map<String, AggregationBuilder> aggregations,
    StickyFacetBuilder stickyFacetBuilder) {
    if (options.getFacets().contains(FACET_CWE)) {
      Collection<String> categories = query.getCwe();
      aggregations.put(FACET_CWE,
        stickyFacetBuilder.buildStickyFacet(FIELD_RULE_CWE, FACET_CWE,
          FACET_DEFAULT_SIZE, filterSecurityCategories(),
          (categories == null) ? (new String[0]) : categories.toArray()));
    }
    if (options.getFacets().contains(FACET_OWASP_TOP_10)) {
      Collection<String> categories = query.getOwaspTop10();
      aggregations.put(FACET_OWASP_TOP_10,
        stickyFacetBuilder.buildStickyFacet(FIELD_RULE_OWASP_TOP_10, FACET_OWASP_TOP_10,
          FACET_DEFAULT_SIZE, filterSecurityCategories(),
          (categories == null) ? (new String[0]) : categories.toArray()));
    }
    if (options.getFacets().contains(FACET_OWASP_TOP_10_2021)) {
      Collection<String> categories = query.getOwaspTop10For2021();
      aggregations.put(FACET_OWASP_TOP_10_2021,
        stickyFacetBuilder.buildStickyFacet(FIELD_RULE_OWASP_TOP_10_2021, FACET_OWASP_TOP_10_2021,
          FACET_DEFAULT_SIZE, filterSecurityCategories(),
          (categories == null) ? (new String[0]) : categories.toArray()));
    }
    if (options.getFacets().contains(FACET_SANS_TOP_25)) {
      Collection<String> categories = query.getSansTop25();
      aggregations.put(FACET_SANS_TOP_25,
        stickyFacetBuilder.buildStickyFacet(FIELD_RULE_SANS_TOP_25, FACET_SANS_TOP_25,
          FACET_DEFAULT_SIZE, filterSecurityCategories(),
          (categories == null) ? (new String[0]) : categories.toArray()));
    }
    if (options.getFacets().contains(FACET_SONARSOURCE_SECURITY)) {
      Collection<String> categories = query.getSonarsourceSecurity();
      aggregations.put(FACET_SONARSOURCE_SECURITY,
        stickyFacetBuilder.buildStickyFacet(FIELD_RULE_SONARSOURCE_SECURITY, FACET_SONARSOURCE_SECURITY,
          SecurityStandards.SQCategory.values().length, filterSecurityCategories(),
          (categories == null) ? (new String[0]) : categories.toArray()));
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
        TYPE_RULE.getType(),
        stickyFacetBuilder.getStickyFacetFilter("activation"),
        false);

      // Rebuilding the active rule filter without severities
      BoolQueryBuilder childrenFilter = boolQuery();
      addTermFilter(childrenFilter, FIELD_ACTIVE_RULE_PROFILE_UUID, profile.getRulesProfileUuid());
      RuleIndex.addTermFilter(childrenFilter, FIELD_ACTIVE_RULE_INHERITANCE, query.getInheritance());
      QueryBuilder activeRuleFilter = childrenFilter.must(ruleFilter);

      AggregationBuilder activeSeverities = JoinAggregationBuilders.children(FACET_ACTIVE_SEVERITIES + "_children", TYPE_ACTIVE_RULE.getName())
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
    return new StickyFacetBuilder(query, filters, null, BucketOrder.compound(BucketOrder.count(false), BucketOrder.key(true)));
  }

  private static void setSorting(RuleQuery query, SearchSourceBuilder esSearch) {
    /* integrate Query Sort */
    String queryText = query.getQueryText();
    if (query.getSortField() != null) {
      FieldSortBuilder sort = SortBuilders.fieldSort(appendSortSuffixIfNeeded(query.getSortField()));
      if (query.isAscendingSort()) {
        sort.order(SortOrder.ASC);
      } else {
        sort.order(SortOrder.DESC);
      }
      esSearch.sort(sort);
    } else if (StringUtils.isNotEmpty(queryText)) {
      esSearch.sort(SortBuilders.scoreSort());
    } else {
      esSearch.sort(appendSortSuffixIfNeeded(FIELD_RULE_UPDATED_AT), SortOrder.DESC);
      // deterministic sort when exactly the same updated_at (same millisecond)
      esSearch.sort(appendSortSuffixIfNeeded(FIELD_RULE_KEY), SortOrder.ASC);
    }
  }

  private static String appendSortSuffixIfNeeded(String field) {
    return field +
      ((field.equals(FIELD_RULE_NAME) || field.equals(FIELD_RULE_KEY))
        ? ("." + SORTABLE_ANALYZER.getSubFieldSuffix())
        : "");
  }

  private static void setPagination(SearchOptions options, SearchSourceBuilder esSearch) {
    esSearch.from(options.getOffset());
    esSearch.size(options.getLimit());
  }

  public List<String> listTags(@Nullable String query, int size) {
    int maxPageSize = 500;
    checkArgument(size <= maxPageSize, "Page size must be lower than or equals to " + maxPageSize);
    if (size <= 0) {
      return emptyList();
    }

    TermsAggregationBuilder termsAggregation = AggregationBuilders.terms(AGGREGATION_NAME_FOR_TAGS)
      .field(FIELD_RULE_TAGS)
      .size(size)
      .order(BucketOrder.key(true))
      .minDocCount(1);
    ofNullable(query)
      .map(EsUtils::escapeSpecialRegexChars)
      .map(queryString -> ".*" + queryString + ".*")
      .map(s -> new IncludeExclude(s, null))
      .ifPresent(termsAggregation::includeExclude);

    SearchRequest request = EsClient.prepareSearch(TYPE_RULE.getMainType())
      .source(new SearchSourceBuilder()
        .query(matchAllQuery())
        .size(0)
        .aggregation(termsAggregation));

    SearchResponse esResponse = client.search(request);
    return EsUtils.termsKeys(esResponse.getAggregations().get(AGGREGATION_NAME_FOR_TAGS));
  }

  private static boolean isNotEmpty(@Nullable Collection<?> list) {
    return list != null && !list.isEmpty();
  }
}
