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
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.core.TimeValue;
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
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregator;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.db.organization.OrganizationDto;
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
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filters;
import static org.elasticsearch.search.aggregations.AggregationBuilders.reverseNested;
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
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_IMPACTS;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_INHERITANCE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_PROFILE_UUID;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_SEVERITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_PRIORITIZED_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_CLEAN_CODE_ATTRIBUTE_CATEGORY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_CREATED_AT;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_CWE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_EXTENSION_SCOPE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_EXTENSION_TAGS;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_HTML_DESCRIPTION;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_IMPACTS;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_IMPACT_SEVERITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_IMPACT_SOFTWARE_QUALITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_INTERNAL_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_IS_EXTERNAL;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_IS_TEMPLATE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_LANGUAGE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_NAME;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_ORGANIZATION_UUID;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_OWASP_TOP_10;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_OWASP_TOP_10_2021;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_REPOSITORY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_RULE_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_SANS_TOP_25;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_SEVERITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_SONARSOURCE_SECURITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_STATUS;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_TEMPLATE_KEY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_TYPE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_UPDATED_AT;
import static org.sonar.server.rule.index.RuleIndexDefinition.SUB_FIELD_SEVERITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.SUB_FIELD_SOFTWARE_QUALITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_ACTIVE_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_RULE_EXTENSION;

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

  /**
   * @deprecated SansTop25 report is outdated, it has been completely deprecated in version 10.0 and will be removed from version 11.0
   */
  @Deprecated(since = "10.0", forRemoval = true)
  public static final String FACET_SANS_TOP_25 = "sansTop25";
  public static final String FACET_OWASP_TOP_10 = "owaspTop10";
  public static final String FACET_OWASP_TOP_10_2021 = "owaspTop10-2021";
  public static final String FACET_SONARSOURCE_SECURITY = "sonarsourceSecurity";
  public static final String FACET_CLEAN_CODE_ATTRIBUTE_CATEGORY = "cleanCodeAttributeCategories";
  public static final String FACET_IMPACT_SOFTWARE_QUALITY = "impactSoftwareQualities";
  public static final String FACET_IMPACT_SEVERITY = "impactSeverities";

  private static final int MAX_FACET_SIZE = 100;

  public static final List<String> ALL_STATUSES_EXCEPT_REMOVED = Arrays.stream(RuleStatus.values())
    .filter(status -> !RuleStatus.REMOVED.equals(status))
    .map(RuleStatus::toString)
    .toList();

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

    filters.put(FIELD_RULE_ORGANIZATION_UUID,
      boolQuery()
          .should(boolQuery().mustNot(existsQuery(FIELD_RULE_ORGANIZATION_UUID)))
          .should(QueryBuilders.termQuery(FIELD_RULE_ORGANIZATION_UUID, query.getOrganization().getUuid())));

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
      filters.put(FIELD_RULE_EXTENSION_TAGS,
              buildTagsFilter(query.getTags(), query.getOrganization()));
    }

    Collection<RuleType> types = query.getTypes();
    if (isNotEmpty(types)) {
      List<String> typeNames = types.stream().map(RuleType::toString).toList();
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

    if (query.getCleanCodeAttributesCategories() != null) {
      addFilter(filters, query.getCleanCodeAttributesCategories(), FIELD_RULE_CLEAN_CODE_ATTRIBUTE_CATEGORY);
    }

    addImpactFilters(query, filters);
    return filters;
  }

  private static void addImpactFilters(RuleQuery query, Map<String, QueryBuilder> allFilters) {
    if (isEmpty(query.getImpactSoftwareQualities()) && isEmpty(query.getImpactSeverities())) {
      return;
    }
    if (isNotEmpty(query.getImpactSoftwareQualities()) && isEmpty(query.getImpactSeverities())) {
      allFilters.put(
        FIELD_RULE_IMPACT_SOFTWARE_QUALITY,
        nestedQuery(
          FIELD_RULE_IMPACTS,
          termsQuery(FIELD_RULE_IMPACT_SOFTWARE_QUALITY, query.getImpactSoftwareQualities()),
          ScoreMode.Avg));
      return;
    }

    if (isNotEmpty(query.getImpactSeverities()) && isEmpty(query.getImpactSoftwareQualities())) {
      allFilters.put(
        FIELD_RULE_IMPACT_SEVERITY,
        nestedQuery(
          FIELD_RULE_IMPACTS,
          termsQuery(FIELD_RULE_IMPACT_SEVERITY, query.getImpactSeverities()),
          ScoreMode.Avg));
      return;
    }
    BoolQueryBuilder impactsFilter = boolQuery()
      .filter(termsQuery(FIELD_RULE_IMPACT_SOFTWARE_QUALITY, query.getImpactSoftwareQualities()))
      .filter(termsQuery(FIELD_RULE_IMPACT_SEVERITY, query.getImpactSeverities()));
    allFilters.put(FIELD_RULE_IMPACTS, nestedQuery(FIELD_ISSUE_IMPACTS, impactsFilter, ScoreMode.Avg));
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

  private static BoolQueryBuilder buildTagsFilter(Collection<String> tags, OrganizationDto organization) {
    BoolQueryBuilder q = boolQuery();
    tags.stream()
            .map(tag -> boolQuery()
                    .filter(QueryBuilders.termQuery(FIELD_RULE_EXTENSION_TAGS, tag))
                    .filter(termsQuery(FIELD_RULE_EXTENSION_SCOPE, RuleExtensionScope.system().getScope(), RuleExtensionScope.organization(organization).getScope())))
            .map(childQuery -> JoinQueryBuilders.hasChildQuery(TYPE_RULE_EXTENSION.getName(), childQuery, ScoreMode.None))
            .forEach(q::should);
    return q;
  }

  private static QueryBuilder buildActivationFilter(RuleQuery query, QProfileDto profile) {
    // ActiveRule Filter (profile and inheritance)
    BoolQueryBuilder activeRuleFilter = boolQuery();
    addTermFilter(activeRuleFilter, FIELD_ACTIVE_RULE_PROFILE_UUID, profile.getRulesProfileUuid());
    addTermFilter(activeRuleFilter, FIELD_ACTIVE_RULE_INHERITANCE, query.getInheritance());
    addTermFilter(activeRuleFilter, FIELD_ACTIVE_RULE_SEVERITY, query.getActiveSeverities());
    addTermFilter(activeRuleFilter, FIELD_PRIORITIZED_RULE, query.getPrioritizedRule());

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

  private static BoolQueryBuilder addTermFilter(BoolQueryBuilder filter, String field, @Nullable Boolean value) {
    if (value != null) {
      filter.must(QueryBuilders.termQuery(field, value));
    }
    return filter;
  }

  private static Map<String, AggregationBuilder> getFacets(RuleQuery query, SearchOptions options, QueryBuilder queryBuilder,
    Map<String, QueryBuilder> filters) {
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

  private static void addDefaultFacets(RuleQuery query, SearchOptions options, Map<String, AggregationBuilder> aggregations,
    StickyFacetBuilder stickyFacetBuilder) {
    if (options.getFacets().contains(FACET_LANGUAGES) || options.getFacets().contains(FACET_OLD_DEFAULT)) {
      Collection<String> languages = query.getLanguages();
      aggregations.put(FACET_LANGUAGES,
        stickyFacetBuilder.buildStickyFacet(FIELD_RULE_LANGUAGE, FACET_LANGUAGES, MAX_FACET_SIZE,
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

        return JoinAggregationBuilders.children("children_for_" + termsAggregation.getName(), TYPE_RULE_EXTENSION.getName())
                .subAggregation(scopeAggregation);
      };

      aggregations.put(FACET_TAGS,
        stickyFacetBuilder.buildStickyFacet(FIELD_RULE_EXTENSION_TAGS, FACET_TAGS, MAX_FACET_SIZE, childFeature,
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
    if (options.getFacets().contains(FACET_CLEAN_CODE_ATTRIBUTE_CATEGORY)) {
      Collection<String> cleanCodeCategories = query.getCleanCodeAttributesCategories();
      aggregations.put(FACET_CLEAN_CODE_ATTRIBUTE_CATEGORY,
        stickyFacetBuilder.buildStickyFacet(FIELD_RULE_CLEAN_CODE_ATTRIBUTE_CATEGORY, FACET_CLEAN_CODE_ATTRIBUTE_CATEGORY, MAX_FACET_SIZE,
          (cleanCodeCategories == null) ? (new String[0]) : cleanCodeCategories.toArray()));
    }

    addImpactSoftwareQualityFacetIfNeeded(options, query, aggregations, stickyFacetBuilder);
    addImpactSeverityFacetIfNeeded(options, query, aggregations, stickyFacetBuilder);

    addDefaultSecurityFacets(query, options, aggregations, stickyFacetBuilder);
  }

  private static void addImpactSoftwareQualityFacetIfNeeded(SearchOptions options, RuleQuery query, Map<String, AggregationBuilder> aggregations,
    StickyFacetBuilder stickyFacetBuilder) {
    if (!options.getFacets().contains(FACET_IMPACT_SOFTWARE_QUALITY)) {
      return;
    }

    Function<String, BoolQueryBuilder> mainQuery = softwareQuality -> boolQuery()
      .filter(QueryBuilders.termQuery(FIELD_RULE_IMPACT_SOFTWARE_QUALITY, softwareQuality));

    FiltersAggregator.KeyedFilter[] keyedFilters = Arrays.stream(SoftwareQuality.values())
      .map(softwareQuality -> new FiltersAggregator.KeyedFilter(softwareQuality.name(),
        buildSoftwareQualityFacetFilter(query, mainQuery, softwareQuality.name())))
      .toArray(FiltersAggregator.KeyedFilter[]::new);

    NestedAggregationBuilder nestedAggregationBuilder = AggregationBuilders.nested("nested_" + FACET_IMPACT_SOFTWARE_QUALITY, FIELD_RULE_IMPACTS)
      .subAggregation(filters(FACET_IMPACT_SOFTWARE_QUALITY, keyedFilters));

    AggregationBuilder aggregationBuilder = stickyFacetBuilder.buildNestedAggregationStickyFacet(FIELD_RULE_IMPACTS, SUB_FIELD_SOFTWARE_QUALITY,
      FACET_IMPACT_SOFTWARE_QUALITY, nestedAggregationBuilder);

    aggregations.put(FACET_IMPACT_SOFTWARE_QUALITY, aggregationBuilder);
  }

  private static BoolQueryBuilder buildSoftwareQualityFacetFilter(RuleQuery query, Function<String, BoolQueryBuilder> mainQuery, String value) {
    BoolQueryBuilder boolQueryBuilder = mainQuery.apply(value);
    if (isNotEmpty(query.getImpactSeverities())) {
      return boolQueryBuilder.filter(termsQuery(FIELD_RULE_IMPACT_SEVERITY, query.getImpactSeverities()));
    }
    return boolQueryBuilder;
  }

  private static void addImpactSeverityFacetIfNeeded(SearchOptions options, RuleQuery query, Map<String, AggregationBuilder> aggregations, StickyFacetBuilder stickyFacetBuilder) {
    if (!options.getFacets().contains(FACET_IMPACT_SEVERITY)) {
      return;
    }

    Function<String, BoolQueryBuilder> mainQuery = severity -> boolQuery()
      .filter(QueryBuilders.termQuery(FIELD_RULE_IMPACT_SEVERITY, severity));

    FiltersAggregator.KeyedFilter[] keyedFilters = Arrays.stream(org.sonar.api.issue.impact.Severity.values())
      .map(severity -> new FiltersAggregator.KeyedFilter(severity.name(),
        buildSeverityFacetFilter(query, mainQuery, severity.name())))
      .toArray(FiltersAggregator.KeyedFilter[]::new);

    NestedAggregationBuilder nestedAggregationBuilder = AggregationBuilders.nested("nested_" + FACET_IMPACT_SEVERITY, FIELD_RULE_IMPACTS)
      .subAggregation(filters(FACET_IMPACT_SEVERITY, keyedFilters).subAggregation(reverseNested("reverse_nested_" + FIELD_RULE_IMPACT_SEVERITY)));

    AggregationBuilder aggregationBuilder = stickyFacetBuilder.buildNestedAggregationStickyFacet(FIELD_RULE_IMPACTS, SUB_FIELD_SEVERITY,
      FACET_IMPACT_SEVERITY, nestedAggregationBuilder);

    aggregations.put(FACET_IMPACT_SEVERITY, aggregationBuilder);
  }

  private static BoolQueryBuilder buildSeverityFacetFilter(RuleQuery query, Function<String, BoolQueryBuilder> mainQuery, String value) {
    BoolQueryBuilder boolQueryBuilder = mainQuery.apply(value);
    if (isNotEmpty(query.getImpactSoftwareQualities())) {
      return boolQueryBuilder.filter(termsQuery(FIELD_RULE_IMPACT_SOFTWARE_QUALITY, query.getImpactSoftwareQualities()));
    }
    return boolQueryBuilder;
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
      .order(BucketOrder.key(true))
      .minDocCount(1);
    ofNullable(query)
      .map(EsUtils::escapeSpecialRegexChars)
      .map(queryString -> ".*" + queryString + ".*")
      .map(s -> new IncludeExclude(s, null))
      .ifPresent(termsAggregation::includeExclude);

    SearchRequest request = EsClient.prepareSearch(TYPE_RULE_EXTENSION.getMainType())
      .source(new SearchSourceBuilder()
        .query(boolQuery().filter(scopeFilter))
        .size(0)
        .aggregation(termsAggregation));

    SearchResponse esResponse = client.search(request);
    return EsUtils.termsKeys(esResponse.getAggregations().get(AGGREGATION_NAME_FOR_TAGS));
  }

  @CheckForNull
  private static QueryBuilder createTermsFilter(String field, Collection<?> values) {
    return values.isEmpty() ? null : termsQuery(field, values);
  }

  private static boolean isNotEmpty(@Nullable Collection<?> list) {
    return list != null && !list.isEmpty();
  }

  private static boolean isEmpty(@Nullable Collection<?> list) {
    return list == null || list.isEmpty();
  }
}
