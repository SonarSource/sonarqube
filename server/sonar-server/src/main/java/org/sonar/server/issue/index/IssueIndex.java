/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.issue.index;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.TermsLookup;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.HasAggregations;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.ExtendedBounds;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.InternalTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.max.InternalMax;
import org.elasticsearch.search.aggregations.metrics.min.Min;
import org.elasticsearch.search.aggregations.metrics.sum.SumAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.valuecount.InternalValueCount;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.es.BaseDoc;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsUtils;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.Sorting;
import org.sonar.server.es.StickyFacetBuilder;
import org.sonar.server.issue.index.IssueQuery.PeriodStart;
import org.sonar.server.permission.index.AuthorizationDoc;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.user.UserSession;
import org.sonar.server.view.index.ViewIndexDefinition;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.es.BaseDoc.epochMillisToEpochSeconds;
import static org.sonar.server.es.EsUtils.escapeSpecialRegexChars;
import static org.sonar.server.es.IndexType.FIELD_INDEX_TYPE;
import static org.sonar.server.issue.index.IssueIndex.Facet.ASSIGNED_TO_ME;
import static org.sonar.server.issue.index.IssueIndex.Facet.ASSIGNEES;
import static org.sonar.server.issue.index.IssueIndex.Facet.AUTHOR;
import static org.sonar.server.issue.index.IssueIndex.Facet.AUTHORS;
import static org.sonar.server.issue.index.IssueIndex.Facet.CREATED_AT;
import static org.sonar.server.issue.index.IssueIndex.Facet.CWE;
import static org.sonar.server.issue.index.IssueIndex.Facet.DIRECTORIES;
import static org.sonar.server.issue.index.IssueIndex.Facet.FILE_UUIDS;
import static org.sonar.server.issue.index.IssueIndex.Facet.LANGUAGES;
import static org.sonar.server.issue.index.IssueIndex.Facet.MODULE_UUIDS;
import static org.sonar.server.issue.index.IssueIndex.Facet.OWASP_TOP_10;
import static org.sonar.server.issue.index.IssueIndex.Facet.PROJECT_UUIDS;
import static org.sonar.server.issue.index.IssueIndex.Facet.RESOLUTIONS;
import static org.sonar.server.issue.index.IssueIndex.Facet.RULES;
import static org.sonar.server.issue.index.IssueIndex.Facet.SANS_TOP_25;
import static org.sonar.server.issue.index.IssueIndex.Facet.SEVERITIES;
import static org.sonar.server.issue.index.IssueIndex.Facet.SONARSOURCE_SECURITY;
import static org.sonar.server.issue.index.IssueIndex.Facet.STATUSES;
import static org.sonar.server.issue.index.IssueIndex.Facet.TAGS;
import static org.sonar.server.issue.index.IssueIndex.Facet.TYPES;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_BRANCH_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_CWE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_EFFORT;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_FILE_PATH;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_FUNC_CLOSED_AT;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_FUNC_UPDATED_AT;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_IS_MAIN_BRANCH;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_KEY;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_LANGUAGE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_LINE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_MODULE_PATH;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_MODULE_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_ORGANIZATION_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_OWASP_TOP_10;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_RESOLUTION;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_RULE_ID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_SANS_TOP_25;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_SEVERITY;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_SEVERITY_VALUE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_SONARSOURCE_SECURITY;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_STATUS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_TAGS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_TYPE;
import static org.sonar.server.issue.index.IssueIndexDefinition.TYPE_ISSUE;
import static org.sonar.server.security.SecurityStandardHelper.SANS_TOP_25_CWE_MAPPING;
import static org.sonar.server.security.SecurityStandardHelper.SANS_TOP_25_INSECURE_INTERACTION;
import static org.sonar.server.security.SecurityStandardHelper.SANS_TOP_25_POROUS_DEFENSES;
import static org.sonar.server.security.SecurityStandardHelper.SANS_TOP_25_RISKY_RESOURCE;
import static org.sonar.server.security.SecurityStandardHelper.SONARSOURCE_CWE_MAPPING;
import static org.sonar.server.security.SecurityStandardHelper.SONARSOURCE_OTHER_CWES_CATEGORY;
import static org.sonar.server.view.index.ViewIndexDefinition.TYPE_VIEW;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.DEPRECATED_PARAM_AUTHORS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_MODE_EFFORT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNEES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_AUTHOR;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CWE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_DIRECTORIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_FILE_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_LANGUAGES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_MODULE_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_TOP_10;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RESOLUTIONS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RULES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SANS_TOP_25;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SEVERITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SONARSOURCE_SECURITY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_STATUSES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TYPES;

/**
 * The unique entry-point to interact with Elasticsearch index "issues".
 * All the requests are listed here.
 */
public class IssueIndex {

  public static final String FACET_PROJECTS = "projects";
  public static final String FACET_ASSIGNED_TO_ME = "assigned_to_me";

  private static final int DEFAULT_FACET_SIZE = 15;
  private static final int MAX_FACET_SIZE = 100;
  private static final String AGG_VULNERABILITIES = "vulnerabilities";
  private static final String AGG_SEVERITIES = "severities";
  private static final String AGG_TO_REVIEW_SECURITY_HOTSPOTS = "toReviewSecurityHotspots";
  private static final String AGG_IN_REVIEW_SECURITY_HOTSPOTS = "inReviewSecurityHotspots";
  private static final String AGG_REVIEWED_SECURITY_HOTSPOTS = "reviewedSecurityHotspots";
  private static final String AGG_CWES = "cwes";
  private static final BoolQueryBuilder NON_RESOLVED_VULNERABILITIES_FILTER = boolQuery()
    .filter(termQuery(FIELD_ISSUE_TYPE, VULNERABILITY.name()))
    .mustNot(existsQuery(FIELD_ISSUE_RESOLUTION));
  private static final BoolQueryBuilder IN_REVIEW_HOTSPOTS_FILTER = boolQuery()
    .filter(termQuery(FIELD_ISSUE_TYPE, SECURITY_HOTSPOT.name()))
    .filter(termQuery(FIELD_ISSUE_STATUS, Issue.STATUS_IN_REVIEW))
    .mustNot(existsQuery(FIELD_ISSUE_RESOLUTION));
  private static final BoolQueryBuilder TO_REVIEW_HOTSPOTS_FILTER = boolQuery()
    .filter(termQuery(FIELD_ISSUE_TYPE, SECURITY_HOTSPOT.name()))
    .filter(termQuery(FIELD_ISSUE_STATUS, Issue.STATUS_TO_REVIEW))
    .mustNot(existsQuery(FIELD_ISSUE_RESOLUTION));
  private static final BoolQueryBuilder REVIEWED_HOTSPOTS_FILTER = boolQuery()
    .filter(termQuery(FIELD_ISSUE_TYPE, SECURITY_HOTSPOT.name()))
    .filter(termQuery(FIELD_ISSUE_STATUS, Issue.STATUS_REVIEWED))
    .filter(termQuery(FIELD_ISSUE_RESOLUTION, Issue.RESOLUTION_FIXED));

  public enum Facet {
    SEVERITIES(PARAM_SEVERITIES, FIELD_ISSUE_SEVERITY, Severity.ALL.size()),
    STATUSES(PARAM_STATUSES, FIELD_ISSUE_STATUS, Issue.STATUSES.size()),
    // Resolutions facet returns one more element than the number of resolutions to take into account unresolved issues
    RESOLUTIONS(PARAM_RESOLUTIONS, FIELD_ISSUE_RESOLUTION, Issue.RESOLUTIONS.size() + 1),
    TYPES(PARAM_TYPES, FIELD_ISSUE_TYPE, RuleType.values().length),
    LANGUAGES(PARAM_LANGUAGES, FIELD_ISSUE_LANGUAGE, MAX_FACET_SIZE),
    RULES(PARAM_RULES, FIELD_ISSUE_RULE_ID, MAX_FACET_SIZE),
    TAGS(PARAM_TAGS, FIELD_ISSUE_TAGS, MAX_FACET_SIZE),
    AUTHORS(DEPRECATED_PARAM_AUTHORS, FIELD_ISSUE_AUTHOR_LOGIN, MAX_FACET_SIZE),
    AUTHOR(PARAM_AUTHOR, FIELD_ISSUE_AUTHOR_LOGIN, MAX_FACET_SIZE),
    PROJECT_UUIDS(FACET_PROJECTS, FIELD_ISSUE_PROJECT_UUID, MAX_FACET_SIZE),
    MODULE_UUIDS(PARAM_MODULE_UUIDS, FIELD_ISSUE_MODULE_UUID, MAX_FACET_SIZE),
    FILE_UUIDS(PARAM_FILE_UUIDS, FIELD_ISSUE_COMPONENT_UUID, MAX_FACET_SIZE),
    DIRECTORIES(PARAM_DIRECTORIES, FIELD_ISSUE_DIRECTORY_PATH, MAX_FACET_SIZE),
    ASSIGNEES(PARAM_ASSIGNEES, FIELD_ISSUE_ASSIGNEE_UUID, MAX_FACET_SIZE),
    ASSIGNED_TO_ME(FACET_ASSIGNED_TO_ME, FIELD_ISSUE_ASSIGNEE_UUID, 1),
    OWASP_TOP_10(PARAM_OWASP_TOP_10, FIELD_ISSUE_OWASP_TOP_10, DEFAULT_FACET_SIZE),
    SANS_TOP_25(PARAM_SANS_TOP_25, FIELD_ISSUE_SANS_TOP_25, DEFAULT_FACET_SIZE),
    CWE(PARAM_CWE, FIELD_ISSUE_CWE, DEFAULT_FACET_SIZE),
    CREATED_AT(PARAM_CREATED_AT, FIELD_ISSUE_FUNC_CREATED_AT, DEFAULT_FACET_SIZE),
    SONARSOURCE_SECURITY(PARAM_SONARSOURCE_SECURITY, FIELD_ISSUE_SONARSOURCE_SECURITY, DEFAULT_FACET_SIZE);

    private final String name;
    private final String fieldName;
    private final int size;

    Facet(String name, String fieldName, int size) {
      this.name = name;
      this.fieldName = fieldName;
      this.size = size;
    }

    public String getName() {
      return name;
    }

    public String getFieldName() {
      return fieldName;
    }

    public int getSize() {
      return size;
    }

    public static Facet of(String name) {
      return stream(values())
        .filter(f -> f.getName().equals(name))
        .reduce((a, b) -> {
          throw new IllegalStateException("Multiple facets with same name: " + a + ", " + b);
        })
        .orElseThrow(() -> new IllegalArgumentException(String.format("Facet name '%s' hasn't been found", name)));
    }
  }

  private static final String SUBSTRING_MATCH_REGEXP = ".*%s.*";
  // TODO to be documented
  // TODO move to Facets ?
  private static final String FACET_SUFFIX_MISSING = "_missing";
  private static final String IS_ASSIGNED_FILTER = "__isAssigned";
  private static final SumAggregationBuilder EFFORT_AGGREGATION = AggregationBuilders.sum(FACET_MODE_EFFORT).field(FIELD_ISSUE_EFFORT);
  private static final BucketOrder EFFORT_AGGREGATION_ORDER = BucketOrder.aggregation(FACET_MODE_EFFORT, false);
  private static final Duration TWENTY_DAYS = Duration.standardDays(20L);
  private static final Duration TWENTY_WEEKS = Duration.standardDays(20L * 7L);
  private static final Duration TWENTY_MONTHS = Duration.standardDays(20L * 30L);
  private static final String AGG_COUNT = "count";
  private final Sorting sorting;
  private final EsClient client;
  private final System2 system;
  private final UserSession userSession;
  private final WebAuthorizationTypeSupport authorizationTypeSupport;

  public IssueIndex(EsClient client, System2 system, UserSession userSession, WebAuthorizationTypeSupport authorizationTypeSupport) {
    this.client = client;
    this.system = system;
    this.userSession = userSession;
    this.authorizationTypeSupport = authorizationTypeSupport;

    this.sorting = new Sorting();
    this.sorting.add(IssueQuery.SORT_BY_STATUS, FIELD_ISSUE_STATUS);
    this.sorting.add(IssueQuery.SORT_BY_SEVERITY, FIELD_ISSUE_SEVERITY_VALUE);
    this.sorting.add(IssueQuery.SORT_BY_CREATION_DATE, FIELD_ISSUE_FUNC_CREATED_AT);
    this.sorting.add(IssueQuery.SORT_BY_UPDATE_DATE, FIELD_ISSUE_FUNC_UPDATED_AT);
    this.sorting.add(IssueQuery.SORT_BY_CLOSE_DATE, FIELD_ISSUE_FUNC_CLOSED_AT);
    this.sorting.add(IssueQuery.SORT_BY_FILE_LINE, FIELD_ISSUE_PROJECT_UUID);
    this.sorting.add(IssueQuery.SORT_BY_FILE_LINE, FIELD_ISSUE_FILE_PATH);
    this.sorting.add(IssueQuery.SORT_BY_FILE_LINE, FIELD_ISSUE_LINE);
    this.sorting.add(IssueQuery.SORT_BY_FILE_LINE, FIELD_ISSUE_SEVERITY_VALUE).reverse();
    this.sorting.add(IssueQuery.SORT_BY_FILE_LINE, FIELD_ISSUE_KEY);

    // by default order by created date, project, file, line and issue key (in order to be deterministic when same ms)
    this.sorting.addDefault(FIELD_ISSUE_FUNC_CREATED_AT).reverse();
    this.sorting.addDefault(FIELD_ISSUE_PROJECT_UUID);
    this.sorting.addDefault(FIELD_ISSUE_FILE_PATH);
    this.sorting.addDefault(FIELD_ISSUE_LINE);
    this.sorting.addDefault(FIELD_ISSUE_KEY);
  }

  public SearchResponse search(IssueQuery query, SearchOptions options) {
    SearchRequestBuilder requestBuilder = client.prepareSearch(TYPE_ISSUE.getMainType());

    configureSorting(query, requestBuilder);
    configurePagination(options, requestBuilder);
    configureRouting(query, options, requestBuilder);

    QueryBuilder esQuery = matchAllQuery();
    BoolQueryBuilder esFilter = boolQuery();
    Map<String, QueryBuilder> filters = createFilters(query);
    for (QueryBuilder filter : filters.values()) {
      if (filter != null) {
        esFilter.must(filter);
      }
    }
    if (esFilter.hasClauses()) {
      requestBuilder.setQuery(boolQuery().must(esQuery).filter(esFilter));
    } else {
      requestBuilder.setQuery(esQuery);
    }

    configureStickyFacets(query, options, filters, esQuery, requestBuilder);
    requestBuilder.addAggregation(EFFORT_AGGREGATION);
    requestBuilder.setFetchSource(false);
    return requestBuilder.get();
  }

  /**
   * Optimization - do not send ES request to all shards when scope is restricted
   * to a set of projects. Because project UUID is used for routing, the request
   * can be sent to only the shards containing the specified projects.
   * Note that sticky facets may involve all projects, so this optimization must be
   * disabled when facets are enabled.
   */
  private static void configureRouting(IssueQuery query, SearchOptions options, SearchRequestBuilder requestBuilder) {
    Collection<String> uuids = query.projectUuids();
    if (!uuids.isEmpty() && options.getFacets().isEmpty()) {
      requestBuilder.setRouting(uuids.stream().map(AuthorizationDoc::idOf).toArray(String[]::new));
    }
  }

  private static void configurePagination(SearchOptions options, SearchRequestBuilder esSearch) {
    esSearch.setFrom(options.getOffset()).setSize(options.getLimit());
  }

  private Map<String, QueryBuilder> createFilters(IssueQuery query) {
    Map<String, QueryBuilder> filters = new HashMap<>();
    filters.put("__indexType", termQuery(FIELD_INDEX_TYPE, TYPE_ISSUE.getName()));
    filters.put("__authorization", createAuthorizationFilter());

    // Issue is assigned Filter
    if (BooleanUtils.isTrue(query.assigned())) {
      filters.put(IS_ASSIGNED_FILTER, existsQuery(FIELD_ISSUE_ASSIGNEE_UUID));
    } else if (BooleanUtils.isFalse(query.assigned())) {
      filters.put(IS_ASSIGNED_FILTER, boolQuery().mustNot(existsQuery(FIELD_ISSUE_ASSIGNEE_UUID)));
    }

    // Issue is Resolved Filter
    String isResolved = "__isResolved";
    if (BooleanUtils.isTrue(query.resolved())) {
      filters.put(isResolved, existsQuery(FIELD_ISSUE_RESOLUTION));
    } else if (BooleanUtils.isFalse(query.resolved())) {
      filters.put(isResolved, boolQuery().mustNot(existsQuery(FIELD_ISSUE_RESOLUTION)));
    }

    // Field Filters
    filters.put(FIELD_ISSUE_KEY, createTermsFilter(FIELD_ISSUE_KEY, query.issueKeys()));
    filters.put(FIELD_ISSUE_ASSIGNEE_UUID, createTermsFilter(FIELD_ISSUE_ASSIGNEE_UUID, query.assignees()));
    filters.put(FIELD_ISSUE_LANGUAGE, createTermsFilter(FIELD_ISSUE_LANGUAGE, query.languages()));
    filters.put(FIELD_ISSUE_TAGS, createTermsFilter(FIELD_ISSUE_TAGS, query.tags()));
    filters.put(FIELD_ISSUE_TYPE, createTermsFilter(FIELD_ISSUE_TYPE, query.types()));
    filters.put(FIELD_ISSUE_RESOLUTION, createTermsFilter(FIELD_ISSUE_RESOLUTION, query.resolutions()));
    filters.put(FIELD_ISSUE_AUTHOR_LOGIN, createTermsFilter(FIELD_ISSUE_AUTHOR_LOGIN, query.authors()));
    filters.put(FIELD_ISSUE_RULE_ID, createTermsFilter(
      FIELD_ISSUE_RULE_ID,
      query.rules().stream().map(RuleDefinitionDto::getId).collect(toList())));
    filters.put(FIELD_ISSUE_STATUS, createTermsFilter(FIELD_ISSUE_STATUS, query.statuses()));
    filters.put(FIELD_ISSUE_ORGANIZATION_UUID, createTermFilter(FIELD_ISSUE_ORGANIZATION_UUID, query.organizationUuid()));
    filters.put(FIELD_ISSUE_OWASP_TOP_10, createTermsFilter(FIELD_ISSUE_OWASP_TOP_10, query.owaspTop10()));
    filters.put(FIELD_ISSUE_SANS_TOP_25, createTermsFilter(FIELD_ISSUE_SANS_TOP_25, query.sansTop25()));
    filters.put(FIELD_ISSUE_CWE, createTermsFilter(FIELD_ISSUE_CWE, query.cwe()));
    addSeverityFilter(query, filters);
    filters.put(FIELD_ISSUE_SONARSOURCE_SECURITY, createTermsFilter(FIELD_ISSUE_SONARSOURCE_SECURITY, query.sonarsourceSecurity()));

    addComponentRelatedFilters(query, filters);
    addDatesFilter(filters, query);
    addCreatedAfterByProjectsFilter(filters, query);
    return filters;
  }

  private static void addSeverityFilter(IssueQuery query, Map<String, QueryBuilder> filters) {
    QueryBuilder severityFieldFilter = createTermsFilter(FIELD_ISSUE_SEVERITY, query.severities());
    if (severityFieldFilter != null) {
      filters.put(FIELD_ISSUE_SEVERITY, boolQuery()
        .must(severityFieldFilter)
        // Ignore severity of Security HotSpots
        .mustNot(termQuery(FIELD_ISSUE_TYPE, SECURITY_HOTSPOT.name())));
    }
  }

  private static void addComponentRelatedFilters(IssueQuery query, Map<String, QueryBuilder> filters) {
    addCommonComponentRelatedFilters(query, filters);
    if (query.viewUuids().isEmpty()) {
      addBranchComponentRelatedFilters(query, filters);
    } else {
      addViewRelatedFilters(query, filters);
    }
  }

  private static void addCommonComponentRelatedFilters(IssueQuery query, Map<String, QueryBuilder> filters) {
    QueryBuilder componentFilter = createTermsFilter(FIELD_ISSUE_COMPONENT_UUID, query.componentUuids());
    QueryBuilder projectFilter = createTermsFilter(FIELD_ISSUE_PROJECT_UUID, query.projectUuids());
    QueryBuilder moduleRootFilter = createTermsFilter(FIELD_ISSUE_MODULE_PATH, query.moduleRootUuids());
    QueryBuilder moduleFilter = createTermsFilter(FIELD_ISSUE_MODULE_UUID, query.moduleUuids());
    QueryBuilder directoryFilter = createTermsFilter(FIELD_ISSUE_DIRECTORY_PATH, query.directories());
    QueryBuilder fileFilter = createTermsFilter(FIELD_ISSUE_COMPONENT_UUID, query.fileUuids());

    if (BooleanUtils.isTrue(query.onComponentOnly())) {
      filters.put(FIELD_ISSUE_COMPONENT_UUID, componentFilter);
    } else {
      filters.put(FIELD_ISSUE_PROJECT_UUID, projectFilter);
      filters.put("__module", moduleRootFilter);
      filters.put(FIELD_ISSUE_MODULE_UUID, moduleFilter);
      filters.put(FIELD_ISSUE_DIRECTORY_PATH, directoryFilter);
      filters.put(FIELD_ISSUE_COMPONENT_UUID, fileFilter != null ? fileFilter : componentFilter);
    }
  }

  private static void addBranchComponentRelatedFilters(IssueQuery query, Map<String, QueryBuilder> filters) {
    if (BooleanUtils.isTrue(query.onComponentOnly())) {
      return;
    }
    QueryBuilder branchFilter = createTermFilter(FIELD_ISSUE_BRANCH_UUID, query.branchUuid());
    filters.put("__is_main_branch", createTermFilter(FIELD_ISSUE_IS_MAIN_BRANCH, Boolean.toString(query.isMainBranch())));
    filters.put(FIELD_ISSUE_BRANCH_UUID, branchFilter);
  }

  private static void addViewRelatedFilters(IssueQuery query, Map<String, QueryBuilder> filters) {
    if (BooleanUtils.isTrue(query.onComponentOnly())) {
      return;
    }
    Collection<String> viewUuids = query.viewUuids();
    String branchUuid = query.branchUuid();
    boolean onApplicationBranch = branchUuid != null && !viewUuids.isEmpty();
    if (onApplicationBranch) {
      filters.put("__view", createViewFilter(singletonList(query.branchUuid())));
    } else {
      filters.put("__is_main_branch", createTermFilter(FIELD_ISSUE_IS_MAIN_BRANCH, Boolean.toString(true)));
      filters.put("__view", createViewFilter(viewUuids));
    }
  }

  @CheckForNull
  private static QueryBuilder createViewFilter(Collection<String> viewUuids) {
    if (viewUuids.isEmpty()) {
      return null;
    }

    BoolQueryBuilder viewsFilter = boolQuery();
    for (String viewUuid : viewUuids) {
      IndexType.IndexMainType mainType = TYPE_VIEW;
      viewsFilter.should(QueryBuilders.termsLookupQuery(FIELD_ISSUE_BRANCH_UUID,
        new TermsLookup(
          mainType.getIndex().getName(),
          mainType.getType(),
          viewUuid,
          ViewIndexDefinition.FIELD_PROJECTS)));
    }
    return viewsFilter;
  }

  private static AggregationBuilder addEffortAggregationIfNeeded(IssueQuery query, AggregationBuilder aggregation) {
    if (hasQueryEffortFacet(query)) {
      aggregation.subAggregation(EFFORT_AGGREGATION);
    }
    return aggregation;
  }

  private static boolean hasQueryEffortFacet(IssueQuery query) {
    return FACET_MODE_EFFORT.equals(query.facetMode());
  }

  private static AggregationBuilder createSeverityFacet(IssueQuery query, Map<String, QueryBuilder> filters, QueryBuilder queryBuilder) {
    String fieldName = SEVERITIES.getFieldName();
    String facetName = SEVERITIES.getName();
    StickyFacetBuilder stickyFacetBuilder = newStickyFacetBuilder(query, filters, queryBuilder);
    BoolQueryBuilder facetFilter = stickyFacetBuilder.getStickyFacetFilter(fieldName)
      // Ignore severity of Security HotSpots
      .mustNot(termQuery(FIELD_ISSUE_TYPE, SECURITY_HOTSPOT.name()));
    FilterAggregationBuilder facetTopAggregation = stickyFacetBuilder.buildTopFacetAggregation(fieldName, facetName, facetFilter, SEVERITIES.getSize());
    return AggregationBuilders
      .global(facetName)
      .subAggregation(facetTopAggregation);
  }

  private static AggregationBuilder createAssigneesFacet(IssueQuery query, Map<String, QueryBuilder> filters, QueryBuilder queryBuilder) {
    String fieldName = ASSIGNEES.getFieldName();
    String facetName = ASSIGNEES.getName();

    // Same as in super.stickyFacetBuilder
    Map<String, QueryBuilder> assigneeFilters = Maps.newHashMap(filters);
    assigneeFilters.remove(IS_ASSIGNED_FILTER);
    assigneeFilters.remove(fieldName);
    StickyFacetBuilder stickyFacetBuilder = newStickyFacetBuilder(query, assigneeFilters, queryBuilder);
    BoolQueryBuilder facetFilter = stickyFacetBuilder.getStickyFacetFilter(fieldName);
    FilterAggregationBuilder facetTopAggregation = stickyFacetBuilder.buildTopFacetAggregation(fieldName, facetName, facetFilter, ASSIGNEES.getSize());
    if (!query.assignees().isEmpty()) {
      facetTopAggregation = stickyFacetBuilder.addSelectedItemsToFacet(fieldName, facetName, facetTopAggregation, t -> t, query.assignees().toArray());
    }

    // Add missing facet for unassigned issues
    facetTopAggregation.subAggregation(
      addEffortAggregationIfNeeded(query, AggregationBuilders
        .missing(facetName + FACET_SUFFIX_MISSING)
        .field(fieldName)));

    return AggregationBuilders
      .global(facetName)
      .subAggregation(facetTopAggregation);
  }

  private static AggregationBuilder createResolutionFacet(IssueQuery query, Map<String, QueryBuilder> filters, QueryBuilder esQuery) {
    String fieldName = RESOLUTIONS.getFieldName();
    String facetName = RESOLUTIONS.getName();

    // Same as in super.stickyFacetBuilder
    Map<String, QueryBuilder> resolutionFilters = Maps.newHashMap(filters);
    resolutionFilters.remove("__isResolved");
    resolutionFilters.remove(fieldName);
    StickyFacetBuilder stickyFacetBuilder = newStickyFacetBuilder(query, resolutionFilters, esQuery);
    BoolQueryBuilder facetFilter = stickyFacetBuilder.getStickyFacetFilter(fieldName);
    FilterAggregationBuilder facetTopAggregation = stickyFacetBuilder.buildTopFacetAggregation(fieldName, facetName, facetFilter, RESOLUTIONS.getSize());
    facetTopAggregation = stickyFacetBuilder.addSelectedItemsToFacet(fieldName, facetName, facetTopAggregation, t -> t);

    // Add missing facet for unresolved issues
    facetTopAggregation.subAggregation(
      addEffortAggregationIfNeeded(query, AggregationBuilders
        .missing(facetName + FACET_SUFFIX_MISSING)
        .field(fieldName)));

    return AggregationBuilders
      .global(facetName)
      .subAggregation(facetTopAggregation);
  }

  @CheckForNull
  private static QueryBuilder createTermsFilter(String field, Collection<?> values) {
    return values.isEmpty() ? null : termsQuery(field, values);
  }

  @CheckForNull
  private static QueryBuilder createTermFilter(String field, @Nullable String value) {
    return value == null ? null : termQuery(field, value);
  }

  private void configureSorting(IssueQuery query, SearchRequestBuilder esRequest) {
    createSortBuilders(query).forEach(esRequest::addSort);
  }

  private List<FieldSortBuilder> createSortBuilders(IssueQuery query) {
    String sortField = query.sort();
    if (sortField != null) {
      boolean asc = BooleanUtils.isTrue(query.asc());
      return sorting.fill(sortField, asc);
    }
    return sorting.fillDefault();
  }

  private QueryBuilder createAuthorizationFilter() {
    return authorizationTypeSupport.createQueryFilter();
  }

  private void addDatesFilter(Map<String, QueryBuilder> filters, IssueQuery query) {
    PeriodStart createdAfter = query.createdAfter();
    Date createdBefore = query.createdBefore();

    validateCreationDateBounds(createdBefore, createdAfter != null ? createdAfter.date() : null);

    if (createdAfter != null) {
      filters.put("__createdAfter", QueryBuilders
        .rangeQuery(FIELD_ISSUE_FUNC_CREATED_AT)
        .from(BaseDoc.dateToEpochSeconds(createdAfter.date()), createdAfter.inclusive()));
    }
    if (createdBefore != null) {
      filters.put("__createdBefore", QueryBuilders
        .rangeQuery(FIELD_ISSUE_FUNC_CREATED_AT)
        .lt(BaseDoc.dateToEpochSeconds(createdBefore)));
    }
    Date createdAt = query.createdAt();
    if (createdAt != null) {
      filters.put("__createdAt", termQuery(FIELD_ISSUE_FUNC_CREATED_AT, BaseDoc.dateToEpochSeconds(createdAt)));
    }
  }

  private static void addCreatedAfterByProjectsFilter(Map<String, QueryBuilder> filters, IssueQuery query) {
    Map<String, PeriodStart> createdAfterByProjectUuids = query.createdAfterByProjectUuids();
    BoolQueryBuilder boolQueryBuilder = boolQuery();
    createdAfterByProjectUuids.forEach((projectUuid, createdAfterDate) -> boolQueryBuilder.should(boolQuery()
      .filter(termQuery(FIELD_ISSUE_PROJECT_UUID, projectUuid))
      .filter(rangeQuery(FIELD_ISSUE_FUNC_CREATED_AT).from(BaseDoc.dateToEpochSeconds(createdAfterDate.date()), createdAfterDate.inclusive()))));
    filters.put("createdAfterByProjectUuids", boolQueryBuilder);
  }

  private void validateCreationDateBounds(@Nullable Date createdBefore, @Nullable Date createdAfter) {
    Preconditions.checkArgument(createdAfter == null || createdAfter.before(new Date(system.now())),
      "Start bound cannot be in the future");
    Preconditions.checkArgument(createdAfter == null || createdBefore == null || createdAfter.before(createdBefore),
      "Start bound cannot be larger or equal to end bound");
  }

  private void configureStickyFacets(IssueQuery query, SearchOptions options, Map<String, QueryBuilder> filters, QueryBuilder esQuery, SearchRequestBuilder esSearch) {
    if (!options.getFacets().isEmpty()) {
      StickyFacetBuilder stickyFacetBuilder = newStickyFacetBuilder(query, filters, esQuery);
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch, STATUSES);
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch, PROJECT_UUIDS, query.projectUuids().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch, MODULE_UUIDS, query.moduleUuids().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch, DIRECTORIES, query.directories().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch, FILE_UUIDS, query.fileUuids().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch, LANGUAGES, query.languages().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch, RULES, query.rules().stream().map(RuleDefinitionDto::getId).toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch, AUTHORS, query.authors().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch, AUTHOR, query.authors().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch, TAGS, query.tags().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch, TYPES, query.types().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch, OWASP_TOP_10, query.owaspTop10().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch, SANS_TOP_25, query.sansTop25().toArray());
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch, CWE, query.cwe().toArray());
      if (options.getFacets().contains(PARAM_SEVERITIES)) {
        esSearch.addAggregation(createSeverityFacet(query, filters, esQuery));
      }
      addSimpleStickyFacetIfNeeded(options, stickyFacetBuilder, esSearch, SONARSOURCE_SECURITY, query.sonarsourceSecurity().toArray());
      if (options.getFacets().contains(PARAM_RESOLUTIONS)) {
        esSearch.addAggregation(createResolutionFacet(query, filters, esQuery));
      }
      if (options.getFacets().contains(PARAM_ASSIGNEES)) {
        esSearch.addAggregation(createAssigneesFacet(query, filters, esQuery));
      }
      if (options.getFacets().contains(PARAM_CREATED_AT)) {
        getCreatedAtFacet(query, filters, esQuery).ifPresent(esSearch::addAggregation);
      }
      addAssignedToMeFacetIfNeeded(esSearch, options, query, filters, esQuery);
    }
  }

  private Optional<AggregationBuilder> getCreatedAtFacet(IssueQuery query, Map<String, QueryBuilder> filters, QueryBuilder esQuery) {
    long startTime;
    boolean startInclusive;
    PeriodStart createdAfter = query.createdAfter();
    if (createdAfter == null) {
      OptionalLong minDate = getMinCreatedAt(filters, esQuery);
      if (!minDate.isPresent()) {
        return Optional.empty();
      }
      startTime = minDate.getAsLong();
      startInclusive = true;
    } else {
      startTime = createdAfter.date().getTime();
      startInclusive = createdAfter.inclusive();
    }
    Date createdBefore = query.createdBefore();
    long endTime = createdBefore == null ? system.now() : createdBefore.getTime();

    Duration timeSpan = new Duration(startTime, endTime);
    DateHistogramInterval bucketSize = DateHistogramInterval.YEAR;
    if (timeSpan.isShorterThan(TWENTY_DAYS)) {
      bucketSize = DateHistogramInterval.DAY;
    } else if (timeSpan.isShorterThan(TWENTY_WEEKS)) {
      bucketSize = DateHistogramInterval.WEEK;
    } else if (timeSpan.isShorterThan(TWENTY_MONTHS)) {
      bucketSize = DateHistogramInterval.MONTH;
    }

    AggregationBuilder dateHistogram = AggregationBuilders.dateHistogram(CREATED_AT.getName())
      .field(CREATED_AT.getFieldName())
      .dateHistogramInterval(bucketSize)
      .minDocCount(0L)
      .format(DateUtils.DATETIME_FORMAT)
      .timeZone(DateTimeZone.forOffsetMillis(system.getDefaultTimeZone().getRawOffset()))
      // ES dateHistogram bounds are inclusive while createdBefore parameter is exclusive
      .extendedBounds(new ExtendedBounds(startInclusive ? startTime : (startTime + 1), endTime - 1L));
    addEffortAggregationIfNeeded(query, dateHistogram);
    return Optional.of(dateHistogram);
  }

  private OptionalLong getMinCreatedAt(Map<String, QueryBuilder> filters, QueryBuilder esQuery) {
    String facetNameAndField = CREATED_AT.getFieldName();
    SearchRequestBuilder esRequest = client
      .prepareSearch(TYPE_ISSUE.getMainType())
      .setSize(0);
    BoolQueryBuilder esFilter = boolQuery();
    filters.values().stream().filter(Objects::nonNull).forEach(esFilter::must);
    if (esFilter.hasClauses()) {
      esRequest.setQuery(QueryBuilders.boolQuery().must(esQuery).filter(esFilter));
    } else {
      esRequest.setQuery(esQuery);
    }
    esRequest.addAggregation(AggregationBuilders.min(facetNameAndField).field(facetNameAndField));
    Min minValue = esRequest.get().getAggregations().get(facetNameAndField);

    double actualValue = minValue.getValue();
    if (Double.isInfinite(actualValue)) {
      return OptionalLong.empty();
    }
    return OptionalLong.of((long) actualValue);
  }

  private void addAssignedToMeFacetIfNeeded(SearchRequestBuilder builder, SearchOptions options, IssueQuery query, Map<String, QueryBuilder> filters, QueryBuilder queryBuilder) {
    String uuid = userSession.getUuid();

    if (!options.getFacets().contains(ASSIGNED_TO_ME.getName()) || StringUtils.isEmpty(uuid)) {
      return;
    }

    String fieldName = ASSIGNED_TO_ME.getFieldName();
    String facetName = ASSIGNED_TO_ME.getName();

    // Same as in super.stickyFacetBuilder
    StickyFacetBuilder assignedToMeFacetBuilder = newStickyFacetBuilder(query, filters, queryBuilder);
    BoolQueryBuilder facetFilter = assignedToMeFacetBuilder.getStickyFacetFilter(IS_ASSIGNED_FILTER, fieldName);

    FilterAggregationBuilder facetTopAggregation = AggregationBuilders
      .filter(facetName + "__filter", facetFilter)
      .subAggregation(addEffortAggregationIfNeeded(query, AggregationBuilders.terms(facetName + "__terms")
        .field(fieldName)
        .includeExclude(new IncludeExclude(escapeSpecialRegexChars(uuid), null))));

    builder.addAggregation(
      AggregationBuilders.global(facetName)
        .subAggregation(facetTopAggregation));
  }

  private static StickyFacetBuilder newStickyFacetBuilder(IssueQuery query, Map<String, QueryBuilder> filters, QueryBuilder esQuery) {
    if (hasQueryEffortFacet(query)) {
      return new StickyFacetBuilder(esQuery, filters, EFFORT_AGGREGATION, EFFORT_AGGREGATION_ORDER);
    }
    return new StickyFacetBuilder(esQuery, filters);
  }

  private static void addSimpleStickyFacetIfNeeded(SearchOptions options, StickyFacetBuilder stickyFacetBuilder, SearchRequestBuilder esSearch,
    Facet facet, Object... selectedValues) {
    if (options.getFacets().contains(facet.getName())) {
      esSearch.addAggregation(stickyFacetBuilder.buildStickyFacet(facet.getFieldName(), facet.getName(), facet.getSize(), selectedValues));
    }
  }

  public List<String> searchTags(IssueQuery query, @Nullable String textQuery, int size) {
    Terms terms = listTermsMatching(FIELD_ISSUE_TAGS, query, textQuery, BucketOrder.key(true), size);
    return EsUtils.termsKeys(terms);
  }

  public Map<String, Long> countTags(IssueQuery query, int maxNumberOfTags) {
    Terms terms = listTermsMatching(FIELD_ISSUE_TAGS, query, null, BucketOrder.count(false), maxNumberOfTags);
    return EsUtils.termsToMap(terms);
  }

  public List<String> searchAuthors(IssueQuery query, @Nullable String textQuery, int maxNumberOfAuthors) {
    Terms terms = listTermsMatching(FIELD_ISSUE_AUTHOR_LOGIN, query, textQuery, BucketOrder.key(true), maxNumberOfAuthors);
    return EsUtils.termsKeys(terms);
  }

  private Terms listTermsMatching(String fieldName, IssueQuery query, @Nullable String textQuery, BucketOrder termsOrder, int size) {
    SearchRequestBuilder requestBuilder = client
      .prepareSearch(TYPE_ISSUE.getMainType())
      // Avoids returning search hits
      .setSize(0);

    requestBuilder.setQuery(boolQuery().must(QueryBuilders.matchAllQuery()).filter(createBoolFilter(query)));

    TermsAggregationBuilder aggreg = AggregationBuilders.terms("_ref")
      .field(fieldName)
      .size(size)
      .order(termsOrder)
      .minDocCount(1L);
    if (textQuery != null) {
      aggreg.includeExclude(new IncludeExclude(format(SUBSTRING_MATCH_REGEXP, escapeSpecialRegexChars(textQuery)), null));
    }

    SearchResponse searchResponse = requestBuilder.addAggregation(aggreg).get();
    return searchResponse.getAggregations().get("_ref");
  }

  private BoolQueryBuilder createBoolFilter(IssueQuery query) {
    BoolQueryBuilder boolQuery = boolQuery();
    for (QueryBuilder filter : createFilters(query).values()) {
      if (filter != null) {
        boolQuery.must(filter);
      }
    }
    return boolQuery;
  }

  public List<ProjectStatistics> searchProjectStatistics(List<String> projectUuids, List<Long> froms, @Nullable String assigneeUuid) {
    checkState(projectUuids.size() == froms.size(),
      "Expected same size for projectUuids (had size %s) and froms (had size %s)", projectUuids.size(), froms.size());
    if (projectUuids.isEmpty()) {
      return Collections.emptyList();
    }
    SearchRequestBuilder request = client.prepareSearch(TYPE_ISSUE.getMainType())
      .setQuery(
        boolQuery()
          .mustNot(existsQuery(FIELD_ISSUE_RESOLUTION))
          .filter(termQuery(FIELD_ISSUE_ASSIGNEE_UUID, assigneeUuid))
          .mustNot(termQuery(FIELD_ISSUE_TYPE, SECURITY_HOTSPOT.name())))
      .setSize(0);
    IntStream.range(0, projectUuids.size()).forEach(i -> {
      String projectUuid = projectUuids.get(i);
      long from = froms.get(i);
      request
        .addAggregation(AggregationBuilders
          .filter(projectUuid, boolQuery()
            .filter(termQuery(FIELD_ISSUE_PROJECT_UUID, projectUuid))
            .filter(rangeQuery(FIELD_ISSUE_FUNC_CREATED_AT).gte(epochMillisToEpochSeconds(from))))
          .subAggregation(
            AggregationBuilders.terms("branchUuid").field(FIELD_ISSUE_BRANCH_UUID)
              .subAggregation(
                AggregationBuilders.count(AGG_COUNT).field(FIELD_ISSUE_KEY))
              .subAggregation(
                AggregationBuilders.max("maxFuncCreatedAt").field(FIELD_ISSUE_FUNC_CREATED_AT))));
    });
    SearchResponse response = request.get();
    return response.getAggregations().asList().stream()
      .map(x -> (InternalFilter) x)
      .flatMap(projectBucket -> ((StringTerms) projectBucket.getAggregations().get("branchUuid")).getBuckets().stream()
        .flatMap(branchBucket -> {
          long count = ((InternalValueCount) branchBucket.getAggregations().get(AGG_COUNT)).getValue();
          if (count < 1L) {
            return Stream.empty();
          }
          long lastIssueDate = (long) ((InternalMax) branchBucket.getAggregations().get("maxFuncCreatedAt")).getValue();
          return Stream.of(new ProjectStatistics(branchBucket.getKeyAsString(), count, lastIssueDate));
        }))
      .collect(MoreCollectors.toList(projectUuids.size()));
  }

  public List<BranchStatistics> searchBranchStatistics(String projectUuid, List<String> branchUuids) {
    if (branchUuids.isEmpty()) {
      return Collections.emptyList();
    }

    SearchRequestBuilder request = client.prepareSearch(TYPE_ISSUE.getMainType())
      .setRouting(AuthorizationDoc.idOf(projectUuid))
      .setQuery(
        boolQuery()
          .must(termsQuery(FIELD_ISSUE_BRANCH_UUID, branchUuids))
          .mustNot(existsQuery(FIELD_ISSUE_RESOLUTION))
          .must(termQuery(FIELD_ISSUE_IS_MAIN_BRANCH, Boolean.toString(false))))
      .setSize(0)
      .addAggregation(AggregationBuilders.terms("branchUuids")
        .field(FIELD_ISSUE_BRANCH_UUID)
        .size(branchUuids.size())
        .subAggregation(AggregationBuilders.terms("types")
          .field(FIELD_ISSUE_TYPE)));
    SearchResponse response = request.get();
    return ((StringTerms) response.getAggregations().get("branchUuids")).getBuckets().stream()
      .map(bucket -> new BranchStatistics(bucket.getKeyAsString(),
        ((StringTerms) bucket.getAggregations().get("types")).getBuckets()
          .stream()
          .collect(uniqueIndex(StringTerms.Bucket::getKeyAsString, InternalTerms.Bucket::getDocCount))))
      .collect(MoreCollectors.toList(branchUuids.size()));
  }

  public List<SecurityStandardCategoryStatistics> getSansTop25Report(String projectUuid, boolean isViewOrApp, boolean includeCwe) {
    SearchRequestBuilder request = prepareNonClosedVulnerabilitiesAndHotspotSearch(projectUuid, isViewOrApp);
    Stream.of(SANS_TOP_25_INSECURE_INTERACTION, SANS_TOP_25_RISKY_RESOURCE, SANS_TOP_25_POROUS_DEFENSES)
      .forEach(sansCategory -> request.addAggregation(createAggregation(FIELD_ISSUE_SANS_TOP_25, sansCategory, includeCwe, Optional.of(SANS_TOP_25_CWE_MAPPING))));
    return processSecurityReportSearchResults(request, includeCwe);
  }

  public List<SecurityStandardCategoryStatistics> getSonarSourceReport(String projectUuid, boolean isViewOrApp, boolean includeCwe) {
    SearchRequestBuilder request = prepareNonClosedVulnerabilitiesAndHotspotSearch(projectUuid, isViewOrApp);
    Stream.concat(SONARSOURCE_CWE_MAPPING.keySet().stream(), Stream.of(SONARSOURCE_OTHER_CWES_CATEGORY))
      .forEach(sonarsourceCategory -> request.addAggregation(
        createAggregation(FIELD_ISSUE_SONARSOURCE_SECURITY, sonarsourceCategory, includeCwe, Optional.of(SONARSOURCE_CWE_MAPPING))));
    return processSecurityReportSearchResults(request, includeCwe);
  }

  public List<SecurityStandardCategoryStatistics> getOwaspTop10Report(String projectUuid, boolean isViewOrApp, boolean includeCwe) {
    SearchRequestBuilder request = prepareNonClosedVulnerabilitiesAndHotspotSearch(projectUuid, isViewOrApp);
    IntStream.rangeClosed(1, 10).mapToObj(i -> "a" + i)
      .forEach(owaspCategory -> request.addAggregation(createAggregation(FIELD_ISSUE_OWASP_TOP_10, owaspCategory, includeCwe, Optional.empty())));
    return processSecurityReportSearchResults(request, includeCwe);
  }

  private static AggregationBuilder createAggregation(String categoryField, String category, boolean includeCwe, Optional<Map<String, Set<String>>> categoryToCwesMap) {
    return addSecurityReportSubAggregations(AggregationBuilders
      .filter(category, boolQuery()
        .filter(termQuery(categoryField, category))),
      includeCwe, categoryToCwesMap.map(m -> m.get(category)));
  }

  private static List<SecurityStandardCategoryStatistics> processSecurityReportSearchResults(SearchRequestBuilder request, boolean includeCwe) {
    SearchResponse response = request.get();
    return response.getAggregations().asList().stream()
      .map(c -> processSecurityReportIssueSearchResults((InternalFilter) c, includeCwe))
      .collect(MoreCollectors.toList());
  }

  private static SecurityStandardCategoryStatistics processSecurityReportIssueSearchResults(InternalFilter categoryBucket, boolean includeCwe) {
    List<SecurityStandardCategoryStatistics> children = new ArrayList<>();
    if (includeCwe) {
      Stream<StringTerms.Bucket> stream = ((StringTerms) categoryBucket.getAggregations().get(AGG_CWES)).getBuckets().stream();
      children = stream.map(cweBucket -> processSecurityReportCategorySearchResults(cweBucket, cweBucket.getKeyAsString(), null)).collect(toList());
    }

    return processSecurityReportCategorySearchResults(categoryBucket, categoryBucket.getName(), children);
  }

  private static SecurityStandardCategoryStatistics processSecurityReportCategorySearchResults(HasAggregations categoryBucket, String categoryName,
    @Nullable List<SecurityStandardCategoryStatistics> children) {
    List<StringTerms.Bucket> severityBuckets = ((StringTerms) ((InternalFilter) categoryBucket.getAggregations().get(AGG_VULNERABILITIES)).getAggregations().get(AGG_SEVERITIES))
      .getBuckets();
    long vulnerabilities = severityBuckets.stream().mapToLong(b -> ((InternalValueCount) b.getAggregations().get(AGG_COUNT)).getValue()).sum();
    // Worst severity having at least one issue
    OptionalInt severityRating = severityBuckets.stream()
      .filter(b -> ((InternalValueCount) b.getAggregations().get(AGG_COUNT)).getValue() != 0)
      .mapToInt(b -> Severity.ALL.indexOf(b.getKeyAsString()) + 1)
      .max();

    long toReviewSecurityHotspots = ((InternalValueCount) ((InternalFilter) categoryBucket.getAggregations().get(AGG_TO_REVIEW_SECURITY_HOTSPOTS)).getAggregations().get(AGG_COUNT))
      .getValue();
    long inReviewSecurityHotspots = ((InternalValueCount) ((InternalFilter) categoryBucket.getAggregations().get(AGG_IN_REVIEW_SECURITY_HOTSPOTS)).getAggregations().get(AGG_COUNT))
      .getValue();
    long reviewedSecurityHotspots = ((InternalValueCount) ((InternalFilter) categoryBucket.getAggregations().get(AGG_REVIEWED_SECURITY_HOTSPOTS)).getAggregations().get(AGG_COUNT))
      .getValue();

    return new SecurityStandardCategoryStatistics(categoryName, vulnerabilities, severityRating, inReviewSecurityHotspots, toReviewSecurityHotspots,
      reviewedSecurityHotspots, children);
  }

  private static AggregationBuilder addSecurityReportSubAggregations(AggregationBuilder categoriesAggs, boolean includeCwe, Optional<Set<String>> cwesInCategory) {
    AggregationBuilder aggregationBuilder = addSecurityReportIssueCountAggregations(categoriesAggs);
    if (includeCwe) {
      final TermsAggregationBuilder cwesAgg = AggregationBuilders.terms(AGG_CWES)
        .field(FIELD_ISSUE_CWE)
        // 100 should be enough to display all CWEs. If not, the UI will be broken anyway
        .size(MAX_FACET_SIZE);
      cwesInCategory.ifPresent(set -> {
        cwesAgg.includeExclude(new IncludeExclude(set.toArray(new String[0]), new String[0]));
      });
      categoriesAggs
        .subAggregation(addSecurityReportIssueCountAggregations(cwesAgg));
    }
    return aggregationBuilder;
  }

  private static AggregationBuilder addSecurityReportIssueCountAggregations(AggregationBuilder categoryAggs) {
    return categoryAggs
      .subAggregation(
        AggregationBuilders.filter(AGG_VULNERABILITIES, NON_RESOLVED_VULNERABILITIES_FILTER)
          .subAggregation(
            AggregationBuilders.terms(AGG_SEVERITIES).field(FIELD_ISSUE_SEVERITY)
              .subAggregation(
                AggregationBuilders.count(AGG_COUNT).field(FIELD_ISSUE_KEY))))
      .subAggregation(AggregationBuilders.filter(AGG_TO_REVIEW_SECURITY_HOTSPOTS, TO_REVIEW_HOTSPOTS_FILTER)
        .subAggregation(
          AggregationBuilders.count(AGG_COUNT).field(FIELD_ISSUE_KEY)))
      .subAggregation(AggregationBuilders.filter(AGG_IN_REVIEW_SECURITY_HOTSPOTS, IN_REVIEW_HOTSPOTS_FILTER)
        .subAggregation(
          AggregationBuilders.count(AGG_COUNT).field(FIELD_ISSUE_KEY)))
      .subAggregation(AggregationBuilders.filter(AGG_REVIEWED_SECURITY_HOTSPOTS, REVIEWED_HOTSPOTS_FILTER)
        .subAggregation(
          AggregationBuilders.count(AGG_COUNT).field(FIELD_ISSUE_KEY)));
  }

  private SearchRequestBuilder prepareNonClosedVulnerabilitiesAndHotspotSearch(String projectUuid, boolean isViewOrApp) {
    BoolQueryBuilder componentFilter = boolQuery();
    if (isViewOrApp) {
      IndexType.IndexMainType mainType = TYPE_VIEW;
      componentFilter.filter(QueryBuilders.termsLookupQuery(FIELD_ISSUE_BRANCH_UUID,
        new TermsLookup(
          mainType.getIndex().getName(),
          mainType.getType(),
          projectUuid,
          ViewIndexDefinition.FIELD_PROJECTS)));
    } else {
      componentFilter.filter(termQuery(FIELD_ISSUE_BRANCH_UUID, projectUuid));
    }
    return client.prepareSearch(TYPE_ISSUE.getMainType())
      .setQuery(
        componentFilter
          .should(NON_RESOLVED_VULNERABILITIES_FILTER)
          .should(TO_REVIEW_HOTSPOTS_FILTER)
          .should(IN_REVIEW_HOTSPOTS_FILTER)
          .should(REVIEWED_HOTSPOTS_FILTER)
          .minimumShouldMatch(1))
      .setSize(0);
  }

}
