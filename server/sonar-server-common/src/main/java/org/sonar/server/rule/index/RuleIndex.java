/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.util.NamedValue;
import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.core.rule.RuleType;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.es.ES8QueryHelper;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsUtils;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.StickyFacetBuilder;
import org.sonar.server.es.newindex.DefaultIndexSettings;
import org.sonar.server.es.textsearch.JavaTokenizer;
import org.sonar.server.security.SecurityStandards;
import org.sonarsource.compliancereports.reports.ComplianceCategoryRules;
import org.sonarsource.compliancereports.reports.RepositoryRuleKey;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_DEFAULT_VALUE;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;
import static org.sonar.core.rule.RuleType.SECURITY_HOTSPOT;
import static org.sonar.core.rule.RuleType.VULNERABILITY;
import static org.sonar.server.es.ES8QueryHelper.hasChildQuery;
import static org.sonar.server.es.ES8QueryHelper.rangeQueryGte;
import static org.sonar.server.es.EsUtils.SEARCH_AFTER_PAGE_SIZE;
import static org.sonar.server.es.EsUtils.optimizeSearchAfterRequest;
import static org.sonar.server.es.EsUtils.searchAfterIds;
import static org.sonar.server.es.IndexType.FIELD_INDEX_TYPE;
import static org.sonar.server.es.StickyFacetBuilder.FACET_DEFAULT_SIZE;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.ENGLISH_HTML_ANALYZER;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SEARCH_GRAMS_ANALYZER;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SEARCH_WORDS_ANALYZER;
import static org.sonar.server.es.newindex.DefaultIndexSettingsElement.SORTABLE_ANALYZER;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_IMPACTS;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_IMPACTS;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_IMPACT_SEVERITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_INHERITANCE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_PROFILE_UUID;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_ACTIVE_RULE_SEVERITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_PRIORITIZED_RULE;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_CLEAN_CODE_ATTRIBUTE_CATEGORY;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_CREATED_AT;
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_CWE;
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
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_OWASP_MOBILE_TOP_10_2024;
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
import static org.sonar.server.rule.index.RuleIndexDefinition.FIELD_RULE_UUID;
import static org.sonar.server.rule.index.RuleIndexDefinition.SUB_FIELD_SEVERITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.SUB_FIELD_SOFTWARE_QUALITY;
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

  /**
   * @deprecated SansTop25 report is outdated, it has been completely deprecated in version 10.0 and will be removed from version 11.0
   */
  @Deprecated(since = "10.0", forRemoval = true)
  public static final String FACET_SANS_TOP_25 = "sansTop25";
  public static final String FACET_OWASP_TOP_10 = "owaspTop10";
  public static final String FACET_OWASP_TOP_10_2021 = "owaspTop10-2021";
  public static final String FACET_OWASP_MOBILE_TOP_10_2024 = "owaspMobileTop10-2024";
  public static final String FACET_SONARSOURCE_SECURITY = "sonarsourceSecurity";
  public static final String FACET_CLEAN_CODE_ATTRIBUTE_CATEGORY = "cleanCodeAttributeCategories";
  public static final String FACET_IMPACT_SOFTWARE_QUALITY = "impactSoftwareQualities";
  public static final String FACET_IMPACT_SEVERITY = "impactSeverities";
  public static final String FACET_ACTIVE_IMPACT_SEVERITY = "active_impactSeverities";
  public static final String FACET_COMPLIANCE_STANDARDS = "complianceStandards";
  public static final String COMPLIANCE_FILTER_FACET = "compliance";

  private static final Query SECURITY_IMPACT_AND_HOTSPOT_FILTER_V2 = ES8QueryHelper.boolQuery(b -> b.should(ES8QueryHelper.nestedQuery(FIELD_RULE_IMPACTS,
    ES8QueryHelper.termsQuery(FIELD_RULE_IMPACT_SOFTWARE_QUALITY, List.of(SoftwareQuality.SECURITY.name()))))
    .should(ES8QueryHelper.termsQuery(FIELD_RULE_TYPE, List.of(SECURITY_HOTSPOT.name())))
    .minimumShouldMatch("1"));

  private static final int MAX_FACET_SIZE = 100;

  public static final List<String> ALL_STATUSES_EXCEPT_REMOVED = Arrays.stream(RuleStatus.values())
    .filter(status -> !RuleStatus.REMOVED.equals(status))
    .map(RuleStatus::toString)
    .toList();

  private static final String AGGREGATION_NAME_FOR_TAGS = "tagsAggregation";
  public static final String FILTER = "_filter";
  public static final String REVERSE_NESTED = "reverse_nested_";
  public static final String ACTIVATION = "activation";
  public static final String CHILDREN = "_children";

  private final EsClient client;
  private final System2 system2;
  private final Configuration config;

  public RuleIndex(EsClient client, System2 system2, Configuration config) {
    this.client = client;
    this.system2 = system2;
    this.config = config;
  }

  public SearchIdResult<String> searchV2(RuleQuery query, SearchOptions options) {
    Query qb = buildQueryV2(query);
    Map<String, Query> filters = buildFiltersV2(query);

    List<Query> filterQueries = new ArrayList<>(filters.values());
    Query finalQuery = ES8QueryHelper.boolQuery(b -> {
      b.must(qb);
      for (Query filter : filterQueries) {
        b.filter(filter);
      }
    });

    co.elastic.clients.elasticsearch.core.SearchResponse<Object> esResponse = client.searchV2(req -> {
      req.index(TYPE_RULE.getIndex().getName())
        .query(finalQuery);

      if (!options.getFacets().isEmpty() || !options.getComplianceFacets().isEmpty()) {
        Map<String, Aggregation> facets = getFacetsV2(query, options, qb, filters);
        for (Map.Entry<String, Aggregation> entry : facets.entrySet()) {
          req.aggregations(entry.getKey(), entry.getValue());
        }
      }

      setSortingV2(query, req);
      setPaginationV2(options, req);

      return req;
    }, Object.class);

    return new SearchIdResult<>(esResponse, input -> input, system2.getDefaultTimeZone().toZoneId());
  }

  /**
   * Return all rule uuids matching the search query, without pagination nor facets
   */
  /**
   * ES 8: Return all rule uuids matching the search query, without pagination nor facets.
   * Uses search_after API for efficient deep pagination.
   */
  public Iterator<String> searchAllV2(RuleQuery query) {
    co.elastic.clients.elasticsearch.core.SearchRequest.Builder sourceBuilder = new co.elastic.clients.elasticsearch.core.SearchRequest.Builder();

    optimizeSearchAfterRequest(sourceBuilder, FIELD_RULE_UUID);
    sourceBuilder.size(SEARCH_AFTER_PAGE_SIZE);

    Query qb = buildQueryV2(query);
    Map<String, Query> filters = buildFiltersV2(query);

    List<Query> filterQueries = new ArrayList<>(filters.values());
    Query finalQuery = ES8QueryHelper.boolQuery(b -> {
      b.must(qb);
      for (Query filter : filterQueries) {
        b.filter(filter);
      }
    });

    sourceBuilder.query(finalQuery);

    co.elastic.clients.elasticsearch.core.SearchRequest esSearch = sourceBuilder
      .index(TYPE_RULE.getIndex().getName())
      .build();

    return searchAfterIds(client, esSearch, Void.class, i -> i);
  }

  /* Build main query (search based) */
  private static Query buildQueryV2(RuleQuery query) {

    // No contextual query case
    String queryText = query.getQueryText();
    if (StringUtils.isEmpty(queryText)) {
      return ES8QueryHelper.matchAllQuery();
    }

    List<Query> tokenQueries = JavaTokenizer.split(queryText)
      .stream()
      .map(token -> ES8QueryHelper.boolQuery(b -> b
        .should(Query.of(q -> q.match(m -> m
          .field(SEARCH_GRAMS_ANALYZER.subField(FIELD_RULE_NAME))
          .query(StringUtils.left(token, DefaultIndexSettings.MAXIMUM_NGRAM_LENGTH))
          .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or)
          .boost(20F))))
        .should(Query.of(q -> q.matchPhrase(mp -> mp
          .field(ENGLISH_HTML_ANALYZER.subField(FIELD_RULE_HTML_DESCRIPTION))
          .query(token)
          .boost(3F))))))
      .toList();

    Query textQuery = Query.of(q -> q.bool(qb -> {
      for (Query tokenQuery : tokenQueries) {
        qb.must(tokenQuery);
      }
      qb.boost(20F);
      return qb;
    }));

    return ES8QueryHelper.boolQuery(b -> b
      .should(textQuery)
      .should(Query.of(q -> q.match(m -> m
        .field(SORTABLE_ANALYZER.subField(FIELD_RULE_KEY))
        .query(queryText)
        .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
        .boost(30F))))
      .should(Query.of(q -> q.match(m -> m
        .field(SORTABLE_ANALYZER.subField(FIELD_RULE_RULE_KEY))
        .query(queryText)
        .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
        .boost(15F))))
      .should(Query.of(q -> q.multiMatch(m -> m
        .fields(
          FIELD_RULE_LANGUAGE,
          SEARCH_WORDS_ANALYZER.subField(FIELD_RULE_LANGUAGE))
        .query(queryText)
        .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
        .boost(3F)))));
  }

  private Map<String, Query> buildFiltersV2(RuleQuery query) {
    Map<String, Query> filters = new HashMap<>();

    /* Add enforced filter on main type Rule */
    filters.put(
      FIELD_INDEX_TYPE,
      ES8QueryHelper.boolQuery(b -> b.must(ES8QueryHelper.termsQuery(FIELD_INDEX_TYPE, List.of(TYPE_RULE.getType())))));

    /* Add enforced filter on rules that are REMOVED */
    filters.put(FIELD_RULE_STATUS,
      ES8QueryHelper.boolQuery(b -> b.mustNot(ES8QueryHelper.termQuery(FIELD_RULE_STATUS, RuleStatus.REMOVED.toString()))));

    addFilterV2(filters, FIELD_RULE_INTERNAL_KEY, query.getInternalKey());

    addFilterV2(filters, FIELD_RULE_RULE_KEY, query.getRuleKey());

    addFilterV2(filters, query.getLanguages(), FIELD_RULE_LANGUAGE);

    addFilterV2(filters, query.getRepositories(), FIELD_RULE_REPOSITORY);

    addFilterV2(filters, query.getSeverities(), FIELD_RULE_SEVERITY);

    addSecurityStandardFilterV2(filters, FIELD_RULE_CWE, query.getCwe());

    addSecurityStandardFilterV2(filters, FIELD_RULE_OWASP_TOP_10, query.getOwaspTop10());

    addSecurityStandardFilterV2(filters, FIELD_RULE_OWASP_TOP_10_2021, query.getOwaspTop10For2021());

    addSecurityStandardFilterV2(filters, FIELD_RULE_OWASP_MOBILE_TOP_10_2024, query.getOwaspMobileTop10For2024());

    addSecurityStandardFilterV2(filters, FIELD_RULE_SANS_TOP_25, query.getSansTop25());

    addSecurityStandardFilterV2(filters, FIELD_RULE_SONARSOURCE_SECURITY, query.getSonarsourceSecurity());

    addComplianceCategoriesFilterV2(filters, query.getComplianceCategoryRules());

    addFilterV2(filters, FIELD_RULE_KEY, query.getKey());

    if (isNotEmpty(query.getTags())) {
      filters.put(FIELD_RULE_TAGS, buildTagsFilterV2(query.getTags()));
    }

    Collection<RuleType> types = query.getTypes();
    if (isNotEmpty(types)) {
      List<String> typeNames = types.stream().map(RuleType::toString).toList();
      filters.put(FIELD_RULE_TYPE,
        ES8QueryHelper.termsQuery(FIELD_RULE_TYPE, typeNames));
    }

    if (query.getAvailableSinceLong() != null) {
      filters.put("availableSince", rangeQueryGte(FIELD_RULE_CREATED_AT, query.getAvailableSinceLong()));
    }

    if (isNotEmpty(query.getStatuses())) {
      Collection<String> stringStatus = new ArrayList<>();
      for (RuleStatus status : query.getStatuses()) {
        stringStatus.add(status.name());
      }
      filters.put(FIELD_RULE_STATUS,
        ES8QueryHelper.termsQuery(FIELD_RULE_STATUS, stringStatus));
    }

    Boolean isTemplate = query.isTemplate();
    if (isTemplate != null) {
      filters.put(FIELD_RULE_IS_TEMPLATE,
        ES8QueryHelper.termQuery(FIELD_RULE_IS_TEMPLATE, Boolean.toString(isTemplate)));
    }

    boolean includeExternal = query.includeExternal();
    if (!includeExternal) {
      filters.put(FIELD_RULE_IS_EXTERNAL,
        ES8QueryHelper.termQuery(FIELD_RULE_IS_EXTERNAL, false));
    }

    String template = query.templateKey();
    if (template != null) {
      filters.put(FIELD_RULE_TEMPLATE_KEY,
        ES8QueryHelper.termQuery(FIELD_RULE_TEMPLATE_KEY, template));
    }

    addActivationFiltersV2(query, filters);

    if (query.getCleanCodeAttributesCategories() != null) {
      addFilterV2(filters, query.getCleanCodeAttributesCategories(), FIELD_RULE_CLEAN_CODE_ATTRIBUTE_CATEGORY);
    }

    addImpactFiltersV2(query, filters);
    return filters;
  }

  private static void addActivationFiltersV2(RuleQuery query, Map<String, Query> filters) {
    QProfileDto profile = query.getQProfile();
    if (query.getActivation() == null || profile == null) {
      return;
    }

    Query childQuery = buildActivationFilterV2(query, profile);

    if (TRUE.equals(query.getActivation())) {
      filters.put(ACTIVATION,
        hasChildQuery(TYPE_ACTIVE_RULE.getName(),
          childQuery, ChildScoreMode.None));
    } else if (FALSE.equals(query.getActivation())) {
      filters.put(ACTIVATION,
        ES8QueryHelper.boolQuery(b -> b.mustNot(
          hasChildQuery(TYPE_ACTIVE_RULE.getName(),
            childQuery, ChildScoreMode.None))));
    }

    QProfileDto compareToQProfile = query.getCompareToQProfile();
    if (compareToQProfile != null) {
      filters.put("comparison",
        hasChildQuery(
          TYPE_ACTIVE_RULE.getName(),
          ES8QueryHelper.boolQuery(b -> b.must(ES8QueryHelper.termQuery(FIELD_ACTIVE_RULE_PROFILE_UUID, compareToQProfile.getRulesProfileUuid()))),
          ChildScoreMode.None));
    }
  }

  private static void addComplianceCategoriesFilterV2(Map<String, Query> filters, @Nullable Collection<ComplianceCategoryRules> rulesCollection) {
    if (rulesCollection == null) {
      return;
    }

    for (ComplianceCategoryRules rules : rulesCollection) {

      List<Query> shouldQueries = new ArrayList<>();
      if (!rules.allRuleKeys().isEmpty()) {
        shouldQueries.add(ES8QueryHelper.termsQuery(FIELD_RULE_RULE_KEY, rules.allRuleKeys()));
      }
      if (!rules.allRepoRuleKeys().isEmpty()) {
        Collection<String> repoRuleKeys = rules.allRepoRuleKeys().stream().map(RepositoryRuleKey::toString).toList();
        shouldQueries.add(ES8QueryHelper.termsQuery(FIELD_RULE_KEY, repoRuleKeys));
      }

      if (!shouldQueries.isEmpty()) {
        Query boolQuery = ES8QueryHelper.boolQuery(b -> b.should(shouldQueries));
        filters.put(COMPLIANCE_FILTER_FACET, boolQuery);
      }
    }
  }

  private static void addImpactFiltersV2(RuleQuery query, Map<String, Query> allFilters) {
    if (isEmpty(query.getImpactSoftwareQualities()) && isEmpty(query.getImpactSeverities())) {
      return;
    }
    if (isNotEmpty(query.getImpactSoftwareQualities()) && isEmpty(query.getImpactSeverities())) {
      allFilters.put(
        FIELD_RULE_IMPACT_SOFTWARE_QUALITY,
        ES8QueryHelper.nestedQuery(
          FIELD_RULE_IMPACTS,
          ES8QueryHelper.termsQuery(FIELD_RULE_IMPACT_SOFTWARE_QUALITY, query.getImpactSoftwareQualities()),
          ChildScoreMode.Avg));
      return;
    }

    if (isNotEmpty(query.getImpactSeverities()) && isEmpty(query.getImpactSoftwareQualities())) {
      allFilters.put(
        FIELD_RULE_IMPACT_SEVERITY,
        ES8QueryHelper.nestedQuery(
          FIELD_RULE_IMPACTS,
          ES8QueryHelper.termsQuery(FIELD_RULE_IMPACT_SEVERITY, query.getImpactSeverities()),
          ChildScoreMode.Avg));
      return;
    }
    Query impactsFilter = ES8QueryHelper.boolQuery(b -> b
      .filter(ES8QueryHelper.termsQuery(FIELD_RULE_IMPACT_SOFTWARE_QUALITY, query.getImpactSoftwareQualities()))
      .filter(ES8QueryHelper.termsQuery(FIELD_RULE_IMPACT_SEVERITY, query.getImpactSeverities())));
    allFilters.put(FIELD_RULE_IMPACTS, ES8QueryHelper.nestedQuery(FIELD_ISSUE_IMPACTS, impactsFilter, ChildScoreMode.Avg));
  }

  private void addSecurityStandardFilterV2(Map<String, Query> filters, String key, Collection<String> values) {
    if (isNotEmpty(values)) {
      filters.put(key,
        ES8QueryHelper.boolQuery(b -> b
          .must(ES8QueryHelper.termsQuery(key, values))
          .must(isMQRMode() ? SECURITY_IMPACT_AND_HOTSPOT_FILTER_V2 : ES8QueryHelper.termsQuery(FIELD_RULE_TYPE, VULNERABILITY.name(), SECURITY_HOTSPOT.name()))));
    }
  }

  private static void addFilterV2(Map<String, Query> filters, Collection<String> key, String value) {
    if (isNotEmpty(key)) {
      filters.put(value, ES8QueryHelper.termsQuery(value, key));
    }
  }

  private static void addFilterV2(Map<String, Query> filters, String key, String value) {
    if (StringUtils.isNotEmpty(value)) {
      filters.put(key, ES8QueryHelper.termQuery(key, value));
    }
  }

  private static Query buildTagsFilterV2(@Nullable Collection<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return ES8QueryHelper.matchAllQuery();
    }

    List<Query> shouldQueries = tags.stream()
      .map(tag -> ES8QueryHelper.boolQuery(b -> b.filter(ES8QueryHelper.termQuery(FIELD_RULE_TAGS, tag))))
      .toList();

    return ES8QueryHelper.boolQuery(b -> b.should(shouldQueries));
  }

  private static Query buildActivationFilterV2(RuleQuery query, QProfileDto profile) {
    List<Query> mustQueries = new ArrayList<>();

    if (StringUtils.isNotEmpty(profile.getRulesProfileUuid())) {
      mustQueries.add(ES8QueryHelper.termQuery(FIELD_ACTIVE_RULE_PROFILE_UUID, profile.getRulesProfileUuid()));
    }

    if (isNotEmpty(query.getInheritance())) {
      mustQueries.add(buildTermsShouldQuery(query.getInheritance(), FIELD_ACTIVE_RULE_INHERITANCE));
    }

    if (isNotEmpty(query.getActiveSeverities())) {
      mustQueries.add(buildTermsShouldQuery(query.getActiveSeverities(), FIELD_ACTIVE_RULE_SEVERITY));
    }

    if (query.getPrioritizedRule() != null) {
      mustQueries.add(ES8QueryHelper.termQuery(FIELD_PRIORITIZED_RULE, query.getPrioritizedRule()));
    }

    if (isNotEmpty(query.getActiveImpactSeverities())) {
      mustQueries.add(ES8QueryHelper.nestedQuery(FIELD_ACTIVE_RULE_IMPACTS,
        ES8QueryHelper.termsQuery(FIELD_ACTIVE_RULE_IMPACT_SEVERITY, query.getActiveImpactSeverities()),
        ChildScoreMode.Avg));
    }

    if (mustQueries.isEmpty()) {
      return ES8QueryHelper.matchAllQuery();
    }

    return ES8QueryHelper.boolQuery(b -> {
      for (Query q : mustQueries) {
        b.must(q);
      }
    });
  }

  private static Query buildTermsShouldQuery(@Nullable Collection<String> values, String field) {
    if (values == null || values.isEmpty()) {
      return ES8QueryHelper.matchAllQuery();
    }

    List<Query> shouldQueries = new ArrayList<>();
    for (String value : values) {
      shouldQueries.add(ES8QueryHelper.termQuery(field, value));
    }
    return ES8QueryHelper.boolQuery(b -> {
      for (Query q : shouldQueries) {
        b.should(q);
      }
    });
  }

  private Map<String, Aggregation> getFacetsV2(RuleQuery query, SearchOptions options, Query queryBuilder,
    Map<String, Query> filters) {
    Map<String, Aggregation> aggregations = new HashMap<>();
    StickyFacetBuilder stickyFacetBuilder = stickyFacetBuilderV2(queryBuilder, filters);

    addDefaultFacetsV2(query, options, aggregations, stickyFacetBuilder);

    addStatusFacetIfNeededV2(options, aggregations, stickyFacetBuilder);

    if (options.getFacets().contains(FACET_SEVERITIES)) {
      aggregations.put(FACET_SEVERITIES,
        stickyFacetBuilder.buildStickyFacetV2(FIELD_RULE_SEVERITY, FACET_SEVERITIES, Severity.ALL.toArray()));
    }

    addActiveSeverityFacetIfNeededV2(query, options, aggregations, stickyFacetBuilder);
    return aggregations;
  }

  private void addDefaultFacetsV2(RuleQuery query, SearchOptions options, Map<String, Aggregation> aggregations,
    StickyFacetBuilder stickyFacetBuilder) {
    if (options.getFacets().contains(FACET_LANGUAGES) || options.getFacets().contains(FACET_OLD_DEFAULT)) {
      Collection<String> languages = query.getLanguages();
      aggregations.put(FACET_LANGUAGES,
        stickyFacetBuilder.buildStickyFacetV2(FIELD_RULE_LANGUAGE, FACET_LANGUAGES, MAX_FACET_SIZE,
          toStringArray(languages)));
    }
    if (options.getFacets().contains(FACET_TAGS) || options.getFacets().contains(FACET_OLD_DEFAULT)) {
      Collection<String> tags = query.getTags();
      aggregations.put(FACET_TAGS,
        stickyFacetBuilder.buildStickyFacetV2(FIELD_RULE_TAGS, FACET_TAGS, MAX_FACET_SIZE,
          toStringArray(tags)));
    }
    if (options.getFacets().contains(FACET_TYPES)) {
      Collection<RuleType> types = query.getTypes();
      aggregations.put(FACET_TYPES,
        stickyFacetBuilder.buildStickyFacetV2(FIELD_RULE_TYPE, FACET_TYPES,
          (types == null) ? (new String[0]) : types.toArray()));
    }
    if (options.getFacets().contains(FACET_REPOSITORIES) || options.getFacets().contains(FACET_OLD_DEFAULT)) {
      Collection<String> repositories = query.getRepositories();
      aggregations.put(FACET_REPOSITORIES,
        stickyFacetBuilder.buildStickyFacetV2(FIELD_RULE_REPOSITORY, FACET_REPOSITORIES, MAX_FACET_SIZE,
          toStringArray(repositories)));
    }
    if (options.getFacets().contains(FACET_CLEAN_CODE_ATTRIBUTE_CATEGORY)) {
      Collection<String> cleanCodeCategories = query.getCleanCodeAttributesCategories();
      aggregations.put(FACET_CLEAN_CODE_ATTRIBUTE_CATEGORY,
        stickyFacetBuilder.buildStickyFacetV2(FIELD_RULE_CLEAN_CODE_ATTRIBUTE_CATEGORY, FACET_CLEAN_CODE_ATTRIBUTE_CATEGORY, MAX_FACET_SIZE,
          toStringArray(cleanCodeCategories)));
    }

    addImpactSoftwareQualityFacetIfNeededV2(options, query, aggregations, stickyFacetBuilder);
    addImpactSeverityFacetIfNeededV2(options, query, aggregations, stickyFacetBuilder);
    addActiveRuleImpactSeverityFacetIfNeededV2(options, query, aggregations, stickyFacetBuilder);

    addDefaultSecurityFacetsV2(query, options, aggregations, stickyFacetBuilder);
    addComplianceFacetsIfNeededV2(options, aggregations, stickyFacetBuilder);
  }

  private static void addComplianceFacetsIfNeededV2(SearchOptions options, Map<String, Aggregation> aggregations,
    StickyFacetBuilder stickyFacetBuilder) {
    if (!options.getComplianceFacets().isEmpty()) {
      aggregations.put(COMPLIANCE_FILTER_FACET,
        stickyFacetBuilder.buildStickyFacetV2(FIELD_RULE_KEY, COMPLIANCE_FILTER_FACET, COMPLIANCE_FILTER_FACET, 65525, t -> t));
    }
  }

  private static void addImpactSoftwareQualityFacetIfNeededV2(SearchOptions options, RuleQuery query, Map<String, Aggregation> aggregations,
    StickyFacetBuilder stickyFacetBuilder) {
    if (!options.getFacets().contains(FACET_IMPACT_SOFTWARE_QUALITY)) {
      return;
    }

    Map<String, Query> softwareQualityFilters = buildSoftwareQualityFacetFilterV2(query);

    Aggregation filtersAgg = Aggregation.of(a -> a.filters(f -> f.filters(fb -> fb.keyed(softwareQualityFilters))));

    Aggregation nestedAgg = Aggregation.of(a -> a
      .nested(n -> n.path(FIELD_RULE_IMPACTS))
      .aggregations(FACET_IMPACT_SOFTWARE_QUALITY, filtersAgg));

    Aggregation aggregationBuilder = stickyFacetBuilder.buildNestedAggregationStickyFacetV2(FIELD_RULE_IMPACTS, SUB_FIELD_SOFTWARE_QUALITY,
      FACET_IMPACT_SOFTWARE_QUALITY, nestedAgg);

    aggregations.put(FACET_IMPACT_SOFTWARE_QUALITY, aggregationBuilder);
  }

  @NotNull
  private static Map<String, Query> buildSoftwareQualityFacetFilterV2(RuleQuery query) {
    Map<String, Query> softwareQualityFilters = new HashMap<>();
    for (SoftwareQuality softwareQuality : SoftwareQuality.values()) {
      Query sqQuery = ES8QueryHelper.termQuery(FIELD_RULE_IMPACT_SOFTWARE_QUALITY, softwareQuality.name());
      if (isNotEmpty(query.getImpactSeverities())) {
        Query finalSqQuery = sqQuery;
        sqQuery = ES8QueryHelper.boolQuery(b -> b
          .must(finalSqQuery)
          .must(ES8QueryHelper.termsQuery(FIELD_RULE_IMPACT_SEVERITY, query.getImpactSeverities())));
      }
      softwareQualityFilters.put(softwareQuality.name(), sqQuery);
    }
    return softwareQualityFilters;
  }

  private static void addImpactSeverityFacetIfNeededV2(SearchOptions options, RuleQuery query, Map<String, Aggregation> aggregations, StickyFacetBuilder stickyFacetBuilder) {
    if (!options.getFacets().contains(FACET_IMPACT_SEVERITY)) {
      return;
    }

    Map<String, Query> severityFilters = new HashMap<>();
    for (org.sonar.api.issue.impact.Severity severity : org.sonar.api.issue.impact.Severity.values()) {
      Query severityQuery = ES8QueryHelper.termQuery(FIELD_RULE_IMPACT_SEVERITY, severity.name());
      if (isNotEmpty(query.getImpactSoftwareQualities())) {
        Query finalSeverityQuery = severityQuery;
        severityQuery = ES8QueryHelper.boolQuery(b -> b
          .must(finalSeverityQuery)
          .must(ES8QueryHelper.termsQuery(FIELD_RULE_IMPACT_SOFTWARE_QUALITY, query.getImpactSoftwareQualities())));
      }
      severityFilters.put(severity.name(), severityQuery);
    }

    Aggregation reverseNestedAgg = Aggregation.of(a -> a.reverseNested(rn -> rn));

    Aggregation filtersAgg = Aggregation.of(a -> a
      .filters(f -> f.filters(fb -> fb.keyed(severityFilters)))
      .aggregations(REVERSE_NESTED + FIELD_RULE_IMPACT_SEVERITY, reverseNestedAgg));

    Aggregation nestedAgg = Aggregation.of(a -> a
      .nested(n -> n.path(FIELD_RULE_IMPACTS))
      .aggregations(FACET_IMPACT_SEVERITY, filtersAgg));

    Aggregation aggregationBuilder = stickyFacetBuilder.buildNestedAggregationStickyFacetV2(FIELD_RULE_IMPACTS, SUB_FIELD_SEVERITY,
      FACET_IMPACT_SEVERITY, nestedAgg);

    aggregations.put(FACET_IMPACT_SEVERITY, aggregationBuilder);
  }

  private static void addActiveRuleImpactSeverityFacetIfNeededV2(SearchOptions options, RuleQuery query,
    Map<String, Aggregation> aggregations, StickyFacetBuilder stickyFacetBuilder) {
    QProfileDto profile = query.getQProfile();
    if (!options.getFacets().contains(FACET_ACTIVE_IMPACT_SEVERITY) || profile == null) {
      return;
    }

    // We are building a children aggregation on active rules
    // so the rule filter has to be used as parent filter for active rules
    // from which we remove filters that concern active rules ("activation")
    Query ruleFilter = ES8QueryHelper.hasParentQuery(
      TYPE_RULE.getType(),
      stickyFacetBuilder.getStickyFacetFilterV2(ACTIVATION));

    // ES 8: Rebuilding the active rule filter without impact severities
    List<Query> childrenMustQueries = new ArrayList<>();
    if (StringUtils.isNotEmpty(profile.getRulesProfileUuid())) {
      childrenMustQueries.add(ES8QueryHelper.termQuery(FIELD_ACTIVE_RULE_PROFILE_UUID, profile.getRulesProfileUuid()));
    }
    if (isNotEmpty(query.getActiveSeverities())) {
      childrenMustQueries.add(ES8QueryHelper.termsQuery(FIELD_ACTIVE_RULE_SEVERITY, query.getActiveSeverities()));
    }
    if (isNotEmpty(query.getInheritance())) {
      childrenMustQueries.add(ES8QueryHelper.termsQuery(FIELD_ACTIVE_RULE_INHERITANCE, query.getInheritance()));
    }
    childrenMustQueries.add(ruleFilter);

    Query activeRuleFilter = ES8QueryHelper.boolQuery(b -> {
      for (Query q : childrenMustQueries) {
        b.must(q);
      }
    });

    Map<String, Query> severityFilters = new HashMap<>();
    for (org.sonar.api.issue.impact.Severity severity : org.sonar.api.issue.impact.Severity.values()) {
      severityFilters.put(severity.name(), ES8QueryHelper.termQuery(FIELD_ACTIVE_RULE_IMPACT_SEVERITY, severity.name()));
    }

    Aggregation reverseNestedAgg = Aggregation.of(a -> a.reverseNested(rn -> rn));

    Aggregation filtersAgg = Aggregation.of(a -> a
      .filters(f -> f.filters(fb -> fb.keyed(severityFilters)))
      .aggregations(REVERSE_NESTED + FIELD_ACTIVE_RULE_IMPACT_SEVERITY, reverseNestedAgg));

    Aggregation nestedAgg = Aggregation.of(a -> a
      .nested(n -> n.path(FIELD_ACTIVE_RULE_IMPACTS))
      .aggregations(FACET_ACTIVE_IMPACT_SEVERITY, filtersAgg));

    Aggregation filterAgg = Aggregation.of(a -> a
      .filter(activeRuleFilter)
      .aggregations("nested_" + FACET_ACTIVE_IMPACT_SEVERITY, nestedAgg));

    Aggregation childrenAgg = Aggregation.of(a -> a
      .children(c -> c.type(TYPE_ACTIVE_RULE.getName()))
      .aggregations(FACET_ACTIVE_IMPACT_SEVERITY + FILTER, filterAgg));

    Aggregation globalAgg = Aggregation.of(a -> a
      .global(g -> g)
      .aggregations(FACET_ACTIVE_IMPACT_SEVERITY + CHILDREN, childrenAgg));

    aggregations.put(FACET_ACTIVE_IMPACT_SEVERITY, globalAgg);
  }

  private UnaryOperator<Aggregation> filterSecurityCategoriesV2() {
    if (isMQRMode()) {
      return termsAggregation -> Aggregation.of(a -> a
        .filter(SECURITY_IMPACT_AND_HOTSPOT_FILTER_V2)
        .aggregations("filtered_terms", termsAggregation));

    } else {
      // ES 8: Wrap terms aggregation in a filter aggregation for rule types
      return termsAggregation -> Aggregation.of(a -> a
        .filter(ES8QueryHelper.termsQuery(FIELD_RULE_TYPE, VULNERABILITY.name(), SECURITY_HOTSPOT.name()))
        .aggregations("filtered_terms", termsAggregation));
    }
  }

  private void addDefaultSecurityFacetsV2(RuleQuery query, SearchOptions options, Map<String, Aggregation> aggregations,
    StickyFacetBuilder stickyFacetBuilder) {
    if (options.getFacets().contains(FACET_CWE)) {
      Collection<String> categories = query.getCwe();
      aggregations.put(FACET_CWE,
        stickyFacetBuilder.buildStickyFacetV2(FIELD_RULE_CWE, FACET_CWE,
          FACET_DEFAULT_SIZE, filterSecurityCategoriesV2(),
          toStringArray(categories)));
    }
    if (options.getFacets().contains(FACET_OWASP_TOP_10)) {
      Collection<String> categories = query.getOwaspTop10();
      aggregations.put(FACET_OWASP_TOP_10,
        stickyFacetBuilder.buildStickyFacetV2(FIELD_RULE_OWASP_TOP_10, FACET_OWASP_TOP_10,
          FACET_DEFAULT_SIZE, filterSecurityCategoriesV2(),
          toStringArray(categories)));
    }
    if (options.getFacets().contains(FACET_OWASP_TOP_10_2021)) {
      Collection<String> categories = query.getOwaspTop10For2021();
      aggregations.put(FACET_OWASP_TOP_10_2021,
        stickyFacetBuilder.buildStickyFacetV2(FIELD_RULE_OWASP_TOP_10_2021, FACET_OWASP_TOP_10_2021,
          FACET_DEFAULT_SIZE, filterSecurityCategoriesV2(),
          toStringArray(categories)));
    }
    if (options.getFacets().contains(FACET_OWASP_MOBILE_TOP_10_2024)) {
      Collection<String> categories = query.getOwaspTop10For2021();
      aggregations.put(FACET_OWASP_MOBILE_TOP_10_2024,
        stickyFacetBuilder.buildStickyFacetV2(FIELD_RULE_OWASP_MOBILE_TOP_10_2024, FACET_OWASP_MOBILE_TOP_10_2024,
          FACET_DEFAULT_SIZE, filterSecurityCategoriesV2(),
          toStringArray(categories)));
    }
    if (options.getFacets().contains(FACET_SANS_TOP_25)) {
      Collection<String> categories = query.getSansTop25();
      aggregations.put(FACET_SANS_TOP_25,
        stickyFacetBuilder.buildStickyFacetV2(FIELD_RULE_SANS_TOP_25, FACET_SANS_TOP_25,
          FACET_DEFAULT_SIZE, filterSecurityCategoriesV2(),
          toStringArray(categories)));
    }
    if (options.getFacets().contains(FACET_SONARSOURCE_SECURITY)) {
      Collection<String> categories = query.getSonarsourceSecurity();
      aggregations.put(FACET_SONARSOURCE_SECURITY,
        stickyFacetBuilder.buildStickyFacetV2(FIELD_RULE_SONARSOURCE_SECURITY, FACET_SONARSOURCE_SECURITY,
          SecurityStandards.SQCategory.values().length, filterSecurityCategoriesV2(),
          toStringArray(categories)));
    }
  }

  @NotNull
  private static Object[] toStringArray(@Nullable Collection<String> items) {
    return (items == null) ? (new String[0]) : items.toArray(new String[0]);
  }

  private static void addStatusFacetIfNeededV2(SearchOptions options, Map<String, Aggregation> aggregations, StickyFacetBuilder stickyFacetBuilder) {
    if (options.getFacets().contains(FACET_STATUSES)) {
      Query facetFilter = stickyFacetBuilder.getStickyFacetFilterV2(FIELD_RULE_STATUS);

      String includePattern = Joiner.on('|').join(ALL_STATUSES_EXCEPT_REMOVED);
      String excludePattern = RuleStatus.REMOVED.toString();
      Aggregation termsAgg = Aggregation.of(a -> a.terms(t -> t
        .field(FIELD_RULE_STATUS)
        .include(i -> i.regexp(includePattern))
        .exclude(e -> e.regexp(excludePattern))
        .size(ALL_STATUSES_EXCEPT_REMOVED.size())));

      Aggregation filterAgg = Aggregation.of(a -> a
        .filter(facetFilter)
        .aggregations(FACET_STATUSES, termsAgg));

      Aggregation globalAgg = Aggregation.of(a -> a
        .global(g -> g)
        .aggregations(FACET_STATUSES + FILTER, filterAgg));

      aggregations.put(FACET_STATUSES, globalAgg);
    }
  }

  private static void addActiveSeverityFacetIfNeededV2(RuleQuery query, SearchOptions options, Map<String, Aggregation> aggregations,
    StickyFacetBuilder stickyFacetBuilder) {
    QProfileDto profile = query.getQProfile();
    if (options.getFacets().contains(FACET_ACTIVE_SEVERITIES) && profile != null) {
      // We are building a children aggregation on active rules
      // so the rule filter has to be used as parent filter for active rules
      // from which we remove filters that concern active rules ("activation")
      Query ruleFilter = ES8QueryHelper.hasParentQuery(
        TYPE_RULE.getType(),
        stickyFacetBuilder.getStickyFacetFilterV2(ACTIVATION));

      List<Query> childrenMustQueries = new ArrayList<>();
      if (StringUtils.isNotEmpty(profile.getRulesProfileUuid())) {
        childrenMustQueries.add(ES8QueryHelper.termQuery(FIELD_ACTIVE_RULE_PROFILE_UUID, profile.getRulesProfileUuid()));
      }
      if (isNotEmpty(query.getActiveImpactSeverities())) {
        childrenMustQueries.add(ES8QueryHelper.nestedQuery(FIELD_ACTIVE_RULE_IMPACTS,
          ES8QueryHelper.termsQuery(FIELD_ACTIVE_RULE_IMPACT_SEVERITY, query.getActiveImpactSeverities()),
          ChildScoreMode.Avg));
      }
      if (isNotEmpty(query.getInheritance())) {
        childrenMustQueries.add(ES8QueryHelper.termsQuery(FIELD_ACTIVE_RULE_INHERITANCE, query.getInheritance()));
      }
      childrenMustQueries.add(ruleFilter);

      Query activeRuleFilter = ES8QueryHelper.boolQuery(b -> {
        for (Query q : childrenMustQueries) {
          b.must(q);
        }
      });

      String includePattern = Joiner.on('|').join(Severity.ALL);
      Aggregation termsAgg = Aggregation.of(a -> a.terms(t -> t
        .field(FIELD_ACTIVE_RULE_SEVERITY)
        .include(i -> i.regexp(includePattern))
        .size(Severity.ALL.size())));

      Aggregation filterAgg = Aggregation.of(a -> a
        .filter(activeRuleFilter)
        .aggregations(FACET_ACTIVE_SEVERITIES, termsAgg));

      Aggregation childrenAgg = Aggregation.of(a -> a
        .children(c -> c.type(TYPE_ACTIVE_RULE.getName()))
        .aggregations(FACET_ACTIVE_SEVERITIES + FILTER, filterAgg));

      Aggregation globalAgg = Aggregation.of(a -> a
        .global(g -> g)
        .aggregations(FACET_ACTIVE_SEVERITIES + CHILDREN, childrenAgg));

      aggregations.put(FACET_ACTIVE_SEVERITIES, globalAgg);
    }
  }

  private static StickyFacetBuilder stickyFacetBuilderV2(Query query, Map<String, Query> filters) {
    Map<String, SortOrder> order = new LinkedHashMap<>();
    order.put("_count", SortOrder.Desc);
    order.put("_key", SortOrder.Asc);
    return new StickyFacetBuilder(query, filters, order);
  }

  private static void setSortingV2(RuleQuery query, co.elastic.clients.elasticsearch.core.SearchRequest.Builder esSearch) {
    /* integrate Query Sort */
    String queryText = query.getQueryText();
    if (query.getSortField() != null) {
      String fieldName = appendSortSuffixIfNeeded(query.getSortField());
      SortOrder order = query.isAscendingSort() ? SortOrder.Asc : SortOrder.Desc;
      esSearch.sort(s -> s.field(f -> f.field(fieldName).order(order)));
    } else if (StringUtils.isNotEmpty(queryText)) {
      esSearch.sort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
    } else {
      esSearch.sort(s -> s.field(f -> f.field(appendSortSuffixIfNeeded(FIELD_RULE_UPDATED_AT)).order(SortOrder.Desc)));
      // deterministic sort when exactly the same updated_at (same millisecond)
      esSearch.sort(s -> s.field(f -> f.field(appendSortSuffixIfNeeded(FIELD_RULE_KEY)).order(SortOrder.Asc)));
    }
  }

  private static String appendSortSuffixIfNeeded(String field) {
    return field +
      ((field.equals(FIELD_RULE_NAME) || field.equals(FIELD_RULE_KEY))
        ? ("." + SORTABLE_ANALYZER.getSubFieldSuffix())
        : "");
  }

  private static void setPaginationV2(SearchOptions options, co.elastic.clients.elasticsearch.core.SearchRequest.Builder esSearch) {
    esSearch.from(options.getOffset());
    esSearch.size(options.getLimit());
  }

  public List<String> listTags(@Nullable String query, int size) {
    int maxPageSize = 500;
    checkArgument(size <= maxPageSize, "Page size must be lower than or equals to " + maxPageSize);
    if (size <= 0) {
      return emptyList();
    }

    co.elastic.clients.elasticsearch.core.SearchResponse<Void> esResponse = client.searchV2(req -> {
      req.index(TYPE_RULE.getMainType().getIndex().getName())
        .query(q -> q.matchAll(m -> m))
        .size(0)
        .aggregations(AGGREGATION_NAME_FOR_TAGS, agg -> agg.terms(t -> {
          t.field(FIELD_RULE_TAGS)
            .size(size)
            .minDocCount(1)
            .order(NamedValue.of("_key", SortOrder.Asc));

          // Apply include/exclude filter if query is provided
          if (query != null) {
            String pattern = ".*" + EsUtils.escapeSpecialRegexChars(query) + ".*";
            t.include(i -> i.regexp(pattern));
          }

          return t;
        }));
      return req;
    }, Void.class);

    // Extract terms aggregation results
    StringTermsAggregate termsAggregate = esResponse.aggregations().get(AGGREGATION_NAME_FOR_TAGS).sterms();

    return termsAggregate.buckets().array().stream()
      .map(StringTermsBucket::key)
      .map(FieldValue::stringValue)
      .toList();
  }

  private static boolean isNotEmpty(@Nullable Collection<?> list) {
    return list != null && !list.isEmpty();
  }

  private static boolean isEmpty(@Nullable Collection<?> list) {
    return list == null || list.isEmpty();
  }

  private boolean isMQRMode() {
    return config.getBoolean(MULTI_QUALITY_MODE_ENABLED).orElse(MULTI_QUALITY_MODE_DEFAULT_VALUE);
  }
}
