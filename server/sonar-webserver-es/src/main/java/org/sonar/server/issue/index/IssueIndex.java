/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.FilterAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.NestedAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.util.NamedValue;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Duration;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.OwaspMobileTop10Version;
import org.sonar.api.server.rule.RulesDefinition.OwaspTop10Version;
import org.sonar.api.server.rule.RulesDefinition.PciDssVersion;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.server.es.ES8QueryHelper;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsUtils;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.Sorting;
import org.sonar.server.es.searchrequest.RequestFiltersComputer;
import org.sonar.server.es.searchrequest.RequestFiltersComputer.AllFilters;
import org.sonar.server.es.searchrequest.SimpleFieldTopAggregationDefinition;
import org.sonar.server.es.searchrequest.SubAggregationHelper;
import org.sonar.server.es.searchrequest.TopAggregationDefinition;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.SimpleFieldFilterScope;
import org.sonar.server.es.searchrequest.TopAggregationHelper;
import org.sonar.server.issue.index.IssueQuery.PeriodStart;
import org.sonar.server.permission.index.AuthorizationDoc;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;
import org.sonar.server.security.SecurityStandards;
import org.sonar.server.security.SecurityStandards.PciDss;
import org.sonar.server.security.SecurityStandards.SQCategory;
import org.sonar.server.security.SecurityStandards.StigSupportedRequirement;
import org.sonar.server.user.UserSession;
import org.sonar.server.view.index.ViewIndexDefinition;
import org.springframework.util.CollectionUtils;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.Strings.CS;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_DEFAULT_VALUE;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;
import static org.sonar.core.rule.RuleType.SECURITY_HOTSPOT;
import static org.sonar.core.rule.RuleType.VULNERABILITY;
import static org.sonar.server.es.EsUtils.escapeSpecialRegexChars;
import static org.sonar.server.es.IndexType.FIELD_INDEX_TYPE;
import static org.sonar.server.es.searchrequest.TopAggregationDefinition.NON_STICKY;
import static org.sonar.server.issue.index.Facet.ASSIGNED_TO_ME;
import static org.sonar.server.issue.index.Facet.ASSIGNEES;
import static org.sonar.server.issue.index.Facet.AUTHOR;
import static org.sonar.server.issue.index.Facet.CASA;
import static org.sonar.server.issue.index.Facet.CLEAN_CODE_ATTRIBUTE_CATEGORY;
import static org.sonar.server.issue.index.Facet.CODE_VARIANTS;
import static org.sonar.server.issue.index.Facet.CREATED_AT;
import static org.sonar.server.issue.index.Facet.CWE;
import static org.sonar.server.issue.index.Facet.DIRECTORIES;
import static org.sonar.server.issue.index.Facet.FILES;
import static org.sonar.server.issue.index.Facet.FROM_SONAR_QUBE_UPDATE;
import static org.sonar.server.issue.index.Facet.IMPACT_SEVERITY;
import static org.sonar.server.issue.index.Facet.IMPACT_SOFTWARE_QUALITY;
import static org.sonar.server.issue.index.Facet.ISSUE_STATUSES;
import static org.sonar.server.issue.index.Facet.LANGUAGES;
import static org.sonar.server.issue.index.Facet.LINKED_JIRA_WORK_ITEM;
import static org.sonar.server.issue.index.Facet.OWASP_ASVS_40;
import static org.sonar.server.issue.index.Facet.OWASP_MOBILE_TOP_10_2024;
import static org.sonar.server.issue.index.Facet.OWASP_TOP_10;
import static org.sonar.server.issue.index.Facet.OWASP_TOP_10_2021;
import static org.sonar.server.issue.index.Facet.PCI_DSS_32;
import static org.sonar.server.issue.index.Facet.PCI_DSS_40;
import static org.sonar.server.issue.index.Facet.PRIORITIZED_RULE;
import static org.sonar.server.issue.index.Facet.PROJECT_UUIDS;
import static org.sonar.server.issue.index.Facet.RESOLUTIONS;
import static org.sonar.server.issue.index.Facet.RULES;
import static org.sonar.server.issue.index.Facet.SANS_TOP_25;
import static org.sonar.server.issue.index.Facet.SCOPES;
import static org.sonar.server.issue.index.Facet.SEVERITIES;
import static org.sonar.server.issue.index.Facet.SONARSOURCE_SECURITY;
import static org.sonar.server.issue.index.Facet.STATUSES;
import static org.sonar.server.issue.index.Facet.STIG_ASD_V5R3;
import static org.sonar.server.issue.index.Facet.TAGS;
import static org.sonar.server.issue.index.Facet.TYPES;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_FROM_SONAR_QUBE_UPDATE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_BRANCH_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_CASA;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_CLEAN_CODE_ATTRIBUTE_CATEGORY;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_CODE_VARIANTS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_CWE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_DIRECTORY_PATH;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_EFFORT;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_FILE_PATH;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_FUNC_CLOSED_AT;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_FUNC_CREATED_AT;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_FUNC_UPDATED_AT;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_IMPACTS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_IMPACT_SEVERITY;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_IS_MAIN_BRANCH;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_KEY;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_LANGUAGE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_LINE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_NEW_CODE_REFERENCE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_NEW_STATUS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_OWASP_ASVS_40;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_OWASP_MOBILE_TOP_10_2024;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_OWASP_TOP_10;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_OWASP_TOP_10_2021;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_PCI_DSS_32;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_PCI_DSS_40;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_PROJECT_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_RESOLUTION;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_RULE_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_SANS_TOP_25;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_SCOPE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_SEVERITY;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_SEVERITY_VALUE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_SQ_SECURITY_CATEGORY;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_STATUS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_STIG_ASD_V5R3;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_TAGS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_MQR_SORT_RANK;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_STANDARD_SORT_RANK;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_TYPE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_VULNERABILITY_PROBABILITY;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_LINKED_TICKET_STATUS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_PRIORITIZED_RULE;
import static org.sonar.server.issue.index.IssueIndexDefinition.TYPE_ISSUE;
import static org.sonar.server.security.SecurityReviewRating.computePercent;
import static org.sonar.server.security.SecurityReviewRating.computeRating;
import static org.sonar.server.security.SecurityStandards.CWES_BY_CWE_TOP_25;
import static org.sonar.server.security.SecurityStandards.OWASP_ASVS_40_REQUIREMENTS_BY_LEVEL;
import static org.sonar.server.view.index.ViewIndexDefinition.TYPE_VIEW;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_MODE_EFFORT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNEES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CASA;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CWE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_IMPACT_SEVERITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_IMPACT_SOFTWARE_QUALITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_ASVS_40;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_MOBILE_TOP_10_2024;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_TOP_10;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_TOP_10_2021;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PCI_DSS_32;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PCI_DSS_40;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RESOLUTIONS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SANS_TOP_25;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SEVERITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SONARSOURCE_SECURITY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_STIG_ASD_V5R3;

/**
 * The unique entry-point to interact with Elasticsearch index "issues".
 * All the requests are listed here.
 */
public class IssueIndex {
  public static final String COMPLIANCE_FILTER_FACET_NAME = "compliance";

  private static final String AGG_VULNERABILITIES = "vulnerabilities";
  private static final String AGG_SEVERITIES = "severities";
  private static final String ISSUES_WITH_SECURITY_IMPACT = "issues_with_security_impact";
  private static final String AGG_IMPACT_SEVERITIES = "impact_severities";
  private static final String AGG_TO_REVIEW_SECURITY_HOTSPOTS = "toReviewSecurityHotspots";
  private static final String AGG_REVIEWED_SECURITY_HOTSPOTS = "reviewedSecurityHotspots";
  private static final String AGG_DISTRIBUTION = "distribution";

  private static final Object[] NO_SELECTED_VALUES = {0};
  private static final SimpleFieldTopAggregationDefinition EFFORT_TOP_AGGREGATION =
    new SimpleFieldTopAggregationDefinition(FIELD_ISSUE_EFFORT, NON_STICKY);

  private static final Map<String, Facet> FACETS_BY_NAME = Arrays.stream(Facet.values())
    .collect(Collectors.toMap(Facet::getName, Function.identity()));

  private static final String SUBSTRING_MATCH_REGEXP = ".*%s.*";
  private static final String FACET_SUFFIX_MISSING = "_missing";
  private static final String SORT_MISSING_LAST = "_last";
  private static final String IS_ASSIGNED_FILTER = "__isAssigned";
  private static final TopAggregationDefinition<?> COMPLIANCE_AGG_DEF = new SimpleFieldTopAggregationDefinition(COMPLIANCE_FILTER_FACET_NAME,
    true);
  private static final Duration TWENTY_DAYS = Duration.standardDays(20L);
  private static final Duration TWENTY_WEEKS = Duration.standardDays(20L * 7L);
  private static final Duration TWENTY_MONTHS = Duration.standardDays(20L * 30L);
  private static final String AGG_COUNT = "count";
  private final Sorting sorting;
  private final EsClient client;
  private final System2 system;
  private final UserSession userSession;
  private final WebAuthorizationTypeSupport authorizationTypeSupport;
  private final Configuration config;

  public IssueIndex(EsClient client, System2 system, UserSession userSession, WebAuthorizationTypeSupport authorizationTypeSupport,
    Configuration config) {
    this.client = client;
    this.system = system;
    this.userSession = userSession;
    this.authorizationTypeSupport = authorizationTypeSupport;
    this.config = config;

    this.sorting = new Sorting();
    this.sorting.add(IssueQuery.SORT_BY_STATUS, FIELD_ISSUE_STATUS);
    this.sorting.add(IssueQuery.SORT_BY_STATUS, FIELD_ISSUE_KEY);
    this.sorting.add(IssueQuery.SORT_BY_SEVERITY, FIELD_ISSUE_SEVERITY_VALUE);
    this.sorting.add(IssueQuery.SORT_BY_SEVERITY, FIELD_ISSUE_KEY);
    this.sorting.add(IssueQuery.SORT_BY_CREATION_DATE, FIELD_ISSUE_FUNC_CREATED_AT);
    this.sorting.add(IssueQuery.SORT_BY_CREATION_DATE, FIELD_ISSUE_KEY);
    this.sorting.add(IssueQuery.SORT_BY_UPDATE_DATE, FIELD_ISSUE_FUNC_UPDATED_AT);
    this.sorting.add(IssueQuery.SORT_BY_UPDATE_DATE, FIELD_ISSUE_KEY);
    this.sorting.add(IssueQuery.SORT_BY_CLOSE_DATE, FIELD_ISSUE_FUNC_CLOSED_AT);
    this.sorting.add(IssueQuery.SORT_BY_CLOSE_DATE, FIELD_ISSUE_KEY);
    this.sorting.add(IssueQuery.SORT_BY_FILE_LINE, FIELD_ISSUE_PROJECT_UUID);
    this.sorting.add(IssueQuery.SORT_BY_FILE_LINE, FIELD_ISSUE_FILE_PATH);
    this.sorting.add(IssueQuery.SORT_BY_FILE_LINE, FIELD_ISSUE_LINE);
    this.sorting.add(IssueQuery.SORT_BY_FILE_LINE, FIELD_ISSUE_SEVERITY_VALUE).reverse();
    this.sorting.add(IssueQuery.SORT_BY_FILE_LINE, FIELD_ISSUE_KEY);
    this.sorting.add(IssueQuery.SORT_HOTSPOTS, FIELD_ISSUE_VULNERABILITY_PROBABILITY).reverse();
    this.sorting.add(IssueQuery.SORT_HOTSPOTS, FIELD_ISSUE_SQ_SECURITY_CATEGORY);
    this.sorting.add(IssueQuery.SORT_HOTSPOTS, FIELD_ISSUE_RULE_UUID);
    this.sorting.add(IssueQuery.SORT_HOTSPOTS, FIELD_ISSUE_PROJECT_UUID);
    this.sorting.add(IssueQuery.SORT_HOTSPOTS, FIELD_ISSUE_FILE_PATH);
    this.sorting.add(IssueQuery.SORT_HOTSPOTS, FIELD_ISSUE_LINE);
    this.sorting.add(IssueQuery.SORT_HOTSPOTS, FIELD_ISSUE_KEY);
    this.sorting.add(IssueQuery.SORT_BY_TYPE_SEVERITY, FIELD_ISSUE_STANDARD_SORT_RANK).reverse();
    this.sorting.add(IssueQuery.SORT_BY_TYPE_SEVERITY, FIELD_ISSUE_KEY);
    this.sorting.add(IssueQuery.SORT_BY_QUALITY_SEVERITY, FIELD_ISSUE_MQR_SORT_RANK).reverse();
    this.sorting.add(IssueQuery.SORT_BY_QUALITY_SEVERITY, FIELD_ISSUE_KEY);
    this.sorting.addDefault(FIELD_ISSUE_FUNC_CREATED_AT).reverse();
    this.sorting.addDefault(FIELD_ISSUE_PROJECT_UUID);
    this.sorting.addDefault(FIELD_ISSUE_FILE_PATH);
    this.sorting.addDefault(FIELD_ISSUE_LINE);
    this.sorting.addDefault(FIELD_ISSUE_KEY);
  }

  public co.elastic.clients.elasticsearch.core.SearchResponse<Object> search(IssueQuery query, SearchOptions options) {
    AllFilters allFilters = createAllFilters(query);
    RequestFiltersComputer filterComputer = newFilterComputer(options, allFilters);

    return client.searchV2(req -> {
      req.index(TYPE_ISSUE.getMainType().getIndex().getName())
        .source(s -> s.fetch(false))
        .trackTotalHits(t -> t.enabled(true))
        .from(options.getOffset())
        .size(options.getLimit());

      createSortOptions(query).forEach(req::sort);
      configureRouting(query, options, req);
      configureTopAggregations(query, options, req, allFilters, filterComputer);

      Query baseQuery = filterComputer.getQueryFiltersV2()
        .map(t -> ES8QueryHelper.boolQuery(b -> b.must(ES8QueryHelper.matchAllQuery()).filter(t)))
        .orElseGet(ES8QueryHelper::matchAllQuery);
      req.query(baseQuery);
      filterComputer.getPostFiltersV2().ifPresent(req::postFilter);
      return req;
    }, Object.class);
  }


  private boolean isMQRMode() {
    return config.getBoolean(MULTI_QUALITY_MODE_ENABLED).orElse(MULTI_QUALITY_MODE_DEFAULT_VALUE);
  }

  private static RequestFiltersComputer newFilterComputer(SearchOptions options, AllFilters allFilters) {
    Collection<String> facetNames = options.getFacets();
    Set<TopAggregationDefinition<?>> facets = Stream.concat(
        Stream.concat(
          Stream.of(EFFORT_TOP_AGGREGATION),
          facetNames.stream()
            .map(FACETS_BY_NAME::get)
            .filter(Objects::nonNull)
            .map(Facet::getTopAggregationDef)),
        Stream.of(COMPLIANCE_AGG_DEF))
      .collect(Collectors.toSet());

    return new RequestFiltersComputer(allFilters, facets);
  }

  private static boolean hasQueryEffortFacet(IssueQuery query) {
    return FACET_MODE_EFFORT.equals(query.facetMode());
  }

  private static Set<String> calculateRequirementsForOwaspAsvs40Params(IssueQuery query) {
    int level = query.getOwaspAsvsLevel().orElse(3);
    List<String> levelRequirements = OWASP_ASVS_40_REQUIREMENTS_BY_LEVEL.get(level);
    return query.owaspAsvs40().stream()
      .flatMap(value -> {
        if (value.contains(".")) {
          return Stream.of(value).filter(levelRequirements::contains);
        }
        return SecurityStandards.getRequirementsForCategoryAndLevel(value, level).stream();
      }).collect(Collectors.toSet());
  }

  private void validateCreationDateBounds(@Nullable Date createdBefore, @Nullable Date createdAfter) {
    Preconditions.checkArgument(createdAfter == null || createdAfter.compareTo(new Date(system.now())) <= 0,
      "Start bound cannot be in the future");
    Preconditions.checkArgument(createdAfter == null || createdBefore == null || createdAfter.before(createdBefore),
      "Start bound cannot be larger or equal to end bound");
  }

  private static SecurityStandardCategoryStatistics emptyCweStatistics(String rule) {
    return new SecurityStandardCategoryStatistics(rule, 0, OptionalInt.of(1), 0, 0, 1, null, null, Map.of());
  }

  private static void configureRouting(IssueQuery query, SearchOptions options,
    co.elastic.clients.elasticsearch.core.SearchRequest.Builder req) {
    Collection<String> uuids = query.projectUuids();
    if (!uuids.isEmpty() && options.getFacets().isEmpty()) {
      req.routing(uuids.stream().map(AuthorizationDoc::idOf).collect(Collectors.joining(",")));
    }
  }

  private List<SortOptions> createSortOptions(IssueQuery query) {
    String sortField = query.sort();
    boolean asc;
    List<Sorting.Field> sortFields;
    if (sortField != null) {
      asc = Boolean.TRUE.equals(query.asc());
      sortFields = sorting.getFields(sortField);
      Preconditions.checkArgument(!sortFields.isEmpty(), "Bad sort field: %s", sortField);
    } else {
      asc = true;
      sortFields = sorting.getDefaultFields();
    }
    return sortFields.stream().map(f -> toSortOption(f, asc)).toList();
  }

  private static SortOptions toSortOption(Sorting.Field field, boolean asc) {
    boolean effectiveAsc = asc != field.isReverse();
    boolean effectiveMissingLast = asc == field.isMissingLast();
    return SortOptions.of(s -> s.field(f -> f
      .field(field.getName())
      .order(effectiveAsc ? SortOrder.Asc : SortOrder.Desc)
      .missing(fv -> fv.stringValue(effectiveMissingLast ? SORT_MISSING_LAST : "_first"))));
  }

  private AllFilters createAllFilters(IssueQuery query) {
    AllFilters filters = RequestFiltersComputer.newAllFilters();
    filters.addFilterV2("__indexType", new SimpleFieldFilterScope(FIELD_INDEX_TYPE),
      ES8QueryHelper.termQuery(FIELD_INDEX_TYPE, TYPE_ISSUE.getName()));
    filters.addFilterV2("__authorization", new SimpleFieldFilterScope("parent"), authorizationTypeSupport.createQueryFilterV2());

    if (Boolean.TRUE.equals(query.assigned())) {
      filters.addFilterV2(IS_ASSIGNED_FILTER, ASSIGNEES.getFilterScope(), ES8QueryHelper.existsQuery(FIELD_ISSUE_ASSIGNEE_UUID));
    } else if (Boolean.FALSE.equals(query.assigned())) {
      filters.addFilterV2(IS_ASSIGNED_FILTER, ASSIGNEES.getFilterScope(),
        ES8QueryHelper.boolQuery(b -> b.mustNot(ES8QueryHelper.existsQuery(FIELD_ISSUE_ASSIGNEE_UUID))));
    }

    if (Boolean.TRUE.equals(query.resolved())) {
      filters.addFilterV2("__isResolved", RESOLUTIONS.getFilterScope(), ES8QueryHelper.existsQuery(FIELD_ISSUE_RESOLUTION));
    } else if (Boolean.FALSE.equals(query.resolved())) {
      filters.addFilterV2("__isResolved", RESOLUTIONS.getFilterScope(),
        ES8QueryHelper.boolQuery(b -> b.mustNot(ES8QueryHelper.existsQuery(FIELD_ISSUE_RESOLUTION))));
    }

    filters.addFilterV2(FIELD_ISSUE_KEY, new SimpleFieldFilterScope(FIELD_ISSUE_KEY),
      createTermsFilterV2ForNullableCollection(FIELD_ISSUE_KEY, query.issueKeys()));
    filters.addFilterV2(FIELD_ISSUE_ASSIGNEE_UUID, ASSIGNEES.getFilterScope(),
      createTermsFilter(FIELD_ISSUE_ASSIGNEE_UUID, query.assignees()));
    filters.addFilterV2(FIELD_ISSUE_SCOPE, SCOPES.getFilterScope(), createTermsFilter(FIELD_ISSUE_SCOPE, query.scopes()));
    filters.addFilterV2(FIELD_ISSUE_LANGUAGE, LANGUAGES.getFilterScope(), createTermsFilter(FIELD_ISSUE_LANGUAGE, query.languages()));
    filters.addFilterV2(FIELD_ISSUE_TAGS, TAGS.getFilterScope(), createTermsFilter(FIELD_ISSUE_TAGS, query.tags()));
    filters.addFilterV2(FIELD_ISSUE_TYPE, TYPES.getFilterScope(), createTermsFilter(FIELD_ISSUE_TYPE, query.types()));
    filters.addFilterV2(
      FIELD_ISSUE_CLEAN_CODE_ATTRIBUTE_CATEGORY,
      CLEAN_CODE_ATTRIBUTE_CATEGORY.getFilterScope(),
      createTermsFilter(FIELD_ISSUE_CLEAN_CODE_ATTRIBUTE_CATEGORY, query.cleanCodeAttributesCategories()));
    filters.addFilterV2(FIELD_ISSUE_RESOLUTION, RESOLUTIONS.getFilterScope(),
      createTermsFilter(FIELD_ISSUE_RESOLUTION, query.resolutions()));
    filters.addFilterV2(FIELD_ISSUE_AUTHOR_LOGIN, AUTHOR.getFilterScope(),
      createTermsFilter(FIELD_ISSUE_AUTHOR_LOGIN, query.authors()));
    filters.addFilterV2(FIELD_ISSUE_RULE_UUID, RULES.getFilterScope(),
      createTermsFilter(FIELD_ISSUE_RULE_UUID, query.ruleUuids()));
    filters.addFilterV2(FIELD_ISSUE_STATUS, STATUSES.getFilterScope(), createTermsFilter(FIELD_ISSUE_STATUS, query.statuses()));
    filters.addFilterV2(FIELD_ISSUE_NEW_STATUS, ISSUE_STATUSES.getFilterScope(),
      createTermsFilter(FIELD_ISSUE_NEW_STATUS, query.issueStatuses()));
    filters.addFilterV2(FIELD_ISSUE_CODE_VARIANTS, CODE_VARIANTS.getFilterScope(),
      createTermsFilter(FIELD_ISSUE_CODE_VARIANTS, query.codeVariants()));
    filters.addFilterV2(FIELD_PRIORITIZED_RULE, PRIORITIZED_RULE.getFilterScope(),
      createTermFilter(FIELD_PRIORITIZED_RULE, query.prioritizedRule()));
    filters.addFilterV2(FIELD_LINKED_TICKET_STATUS, LINKED_JIRA_WORK_ITEM.getFilterScope(),
      createTermsFilter(FIELD_LINKED_TICKET_STATUS, query.linkedTicketStatuses()));
    filters.addFilterV2(FIELD_FROM_SONAR_QUBE_UPDATE, FROM_SONAR_QUBE_UPDATE.getFilterScope(),
      createTermFilter(FIELD_FROM_SONAR_QUBE_UPDATE, query.fromSonarQubeUpdate()));

    addSecurityCategoryPrefixFilter(FIELD_ISSUE_PCI_DSS_32, PCI_DSS_32, query.pciDss32(), filters);
    addSecurityCategoryPrefixFilter(FIELD_ISSUE_PCI_DSS_40, PCI_DSS_40, query.pciDss40(), filters);
    addOwaspAsvsFilter(FIELD_ISSUE_OWASP_ASVS_40, OWASP_ASVS_40, query, filters);
    addSecurityCategoryFilter(FIELD_ISSUE_OWASP_MOBILE_TOP_10_2024, OWASP_MOBILE_TOP_10_2024, query.owaspMobileTop10For2024(), filters);
    addSecurityCategoryFilter(FIELD_ISSUE_OWASP_TOP_10, OWASP_TOP_10, query.owaspTop10(), filters);
    addSecurityCategoryFilter(FIELD_ISSUE_OWASP_TOP_10_2021, OWASP_TOP_10_2021, query.owaspTop10For2021(), filters);
    addSecurityCategoryFilter(FIELD_ISSUE_STIG_ASD_V5R3, STIG_ASD_V5R3, query.stigAsdV5R3(), filters);
    addSecurityCategoryPrefixFilter(FIELD_ISSUE_CASA, CASA, query.casa(), filters);
    addSecurityCategoryFilter(FIELD_ISSUE_SANS_TOP_25, SANS_TOP_25, query.sansTop25(), filters);
    addSecurityCategoryFilter(FIELD_ISSUE_CWE, CWE, query.cwe(), filters);
    addSecurityCategoryFilter(FIELD_ISSUE_SQ_SECURITY_CATEGORY, SONARSOURCE_SECURITY, query.sonarsourceSecurity(), filters);

    addComplianceCategoriesFilter(query, filters);
    addSeverityFilter(query, filters);
    addImpactFilters(query, filters);
    addComponentRelatedFilters(query, filters);
    addDatesFilter(filters, query);
    addNewCodeByProjectsFilter(filters, query);
    addNewCodeReferenceFilter(filters, query);
    return filters;
  }

  private static void addComplianceCategoriesFilter(IssueQuery query, AllFilters allFilters) {
    if (query.complianceCategoryRules() == null) {
      return;
    }
    Query q = ES8QueryHelper.termsQuery(FIELD_ISSUE_RULE_UUID, query.complianceCategoryRules());
    allFilters.addFilterV2(COMPLIANCE_FILTER_FACET_NAME, new SimpleFieldFilterScope(COMPLIANCE_FILTER_FACET_NAME), q);
  }

  private void addOwaspAsvsFilter(String fieldName, Facet facet, IssueQuery query, AllFilters allFilters) {
    if (!CollectionUtils.isEmpty(query.owaspAsvs40())) {
      Set<String> requirements = calculateRequirementsForOwaspAsvs40Params(query);
      allFilters.addFilterV2(
        fieldName,
        facet.getFilterScope(),
        ES8QueryHelper.boolQuery(b -> b
          .must(ES8QueryHelper.termsQuery(fieldName, requirements))
          .must(getQueryV2ForSecurityCategory())));
    }
  }

  private Query getQueryV2ForSecurityCategory() {
    if (isMQRMode()) {
      return ES8QueryHelper.boolQuery(b -> b
        .should(ES8QueryHelper.nestedQuery(FIELD_ISSUE_IMPACTS,
          ES8QueryHelper.termsQuery(FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY, List.of(SoftwareQuality.SECURITY.name())),
          ChildScoreMode.Avg))
        .should(ES8QueryHelper.termsQuery(FIELD_ISSUE_TYPE, List.of(SECURITY_HOTSPOT.name())))
        .minimumShouldMatch("1"));
    }
    return ES8QueryHelper.termsQuery(FIELD_ISSUE_TYPE, List.of(VULNERABILITY.name(), SECURITY_HOTSPOT.name()));
  }

  private void addSecurityCategoryFilter(String fieldName, Facet facet, Collection<String> values, AllFilters allFilters) {
    Query securityCategoryFilter = createTermsFilter(fieldName, values);
    if (securityCategoryFilter != null) {
      allFilters.addFilterV2(
        fieldName,
        facet.getFilterScope(),
        ES8QueryHelper.boolQuery(b -> b
          .must(securityCategoryFilter)
          .must(getQueryV2ForSecurityCategory())));
    }
  }

  private void addSecurityCategoryPrefixFilter(String fieldName, Facet facet, Collection<String> values, AllFilters allFilters) {
    if (values.isEmpty()) {
      return;
    }
    Query q = ES8QueryHelper.boolQuery(b -> {
      b.minimumShouldMatch("1");
      b.must(getQueryV2ForSecurityCategory());
      values.forEach(v -> b.should(choosePrefixQuery(fieldName, v)));
    });
    allFilters.addFilterV2(fieldName, facet.getFilterScope(), q);
  }

  private static Query choosePrefixQuery(String fieldName, String value) {
    return value.contains(".") ? ES8QueryHelper.termQuery(fieldName, value) : ES8QueryHelper.prefixQuery(fieldName, value + ".");
  }

  private static void addSeverityFilter(IssueQuery query, AllFilters allFilters) {
    Query severityFieldFilter = createTermsFilter(FIELD_ISSUE_SEVERITY, query.severities());
    if (severityFieldFilter != null) {
      allFilters.addFilterV2(
        FIELD_ISSUE_SEVERITY,
        SEVERITIES.getFilterScope(),
        ES8QueryHelper.boolQuery(b -> b
          .must(severityFieldFilter)
          .mustNot(ES8QueryHelper.termQuery(FIELD_ISSUE_TYPE, SECURITY_HOTSPOT.name()))));
    }
  }

  private static void addImpactFilters(IssueQuery query, AllFilters allFilters) {
    if (query.impactSoftwareQualities().isEmpty() && query.impactSeverities().isEmpty()) {
      return;
    }

    if (!query.impactSoftwareQualities().isEmpty() && query.impactSeverities().isEmpty()) {
      allFilters.addFilterV2(
        FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY,
        IMPACT_SOFTWARE_QUALITY.getFilterScope(),
        ES8QueryHelper.nestedQuery(FIELD_ISSUE_IMPACTS,
          ES8QueryHelper.termsQuery(FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY, query.impactSoftwareQualities()),
          ChildScoreMode.Avg));
      return;
    }

    if (!query.impactSeverities().isEmpty() && query.impactSoftwareQualities().isEmpty()) {
      allFilters.addFilterV2(
        FIELD_ISSUE_IMPACT_SEVERITY,
        IMPACT_SEVERITY.getFilterScope(),
        ES8QueryHelper.nestedQuery(FIELD_ISSUE_IMPACTS,
          ES8QueryHelper.termsQuery(FIELD_ISSUE_IMPACT_SEVERITY, query.impactSeverities()),
          ChildScoreMode.Avg));
      return;
    }

    Query impactsFilter = ES8QueryHelper.boolQuery(b -> b
      .filter(ES8QueryHelper.termsQuery(FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY, query.impactSoftwareQualities()))
      .filter(ES8QueryHelper.termsQuery(FIELD_ISSUE_IMPACT_SEVERITY, query.impactSeverities())));

    allFilters.addFilterV2(FIELD_ISSUE_IMPACTS, new SimpleFieldFilterScope(FIELD_ISSUE_IMPACTS),
      ES8QueryHelper.nestedQuery(FIELD_ISSUE_IMPACTS, impactsFilter, ChildScoreMode.Avg));
  }

  private static void addComponentRelatedFilters(IssueQuery query, AllFilters filters) {
    addCommonComponentRelatedFilters(query, filters);
    if (query.viewUuids().isEmpty()) {
      addBranchComponentRelatedFilters(query, filters);
    } else {
      addViewRelatedFilters(query, filters);
    }
  }

  private static void addCommonComponentRelatedFilters(IssueQuery query, AllFilters filters) {
    filters.addFilterV2(FIELD_ISSUE_COMPONENT_UUID, new SimpleFieldFilterScope(FIELD_ISSUE_COMPONENT_UUID),
      createTermsFilter(FIELD_ISSUE_COMPONENT_UUID, query.componentUuids()));

    if (!Boolean.TRUE.equals(query.onComponentOnly())) {
      filters.addFilterV2(
        FIELD_ISSUE_PROJECT_UUID, new SimpleFieldFilterScope(FIELD_ISSUE_PROJECT_UUID),
        createTermsFilter(FIELD_ISSUE_PROJECT_UUID, query.projectUuids()));

      Query directoryFilter = ES8QueryHelper.boolQuery(b -> query.directories()
        .forEach(directory -> b.should(ES8QueryHelper.prefixQuery(FIELD_ISSUE_DIRECTORY_PATH, directory))));
      filters.addFilterV2(FIELD_ISSUE_DIRECTORY_PATH, new SimpleFieldFilterScope(FIELD_ISSUE_DIRECTORY_PATH), directoryFilter);

      filters.addFilterV2(
        FIELD_ISSUE_FILE_PATH, new SimpleFieldFilterScope(FIELD_ISSUE_FILE_PATH),
        createTermsFilter(FIELD_ISSUE_FILE_PATH, query.files()));
    }
  }

  private static void addBranchComponentRelatedFilters(IssueQuery query, AllFilters allFilters) {
    if (Boolean.TRUE.equals(query.onComponentOnly())) {
      return;
    }
    if (query.isMainBranch() != null) {
      allFilters.addFilterV2(
        "__is_main_branch", new SimpleFieldFilterScope(FIELD_ISSUE_IS_MAIN_BRANCH),
        createTermFilter(FIELD_ISSUE_IS_MAIN_BRANCH, query.isMainBranch().toString()));
    }
    allFilters.addFilterV2(
      FIELD_ISSUE_BRANCH_UUID, new SimpleFieldFilterScope(FIELD_ISSUE_BRANCH_UUID),
      createTermFilter(FIELD_ISSUE_BRANCH_UUID, query.branchUuid()));
  }

  private static void addViewRelatedFilters(IssueQuery query, AllFilters allFilters) {
    if (Boolean.TRUE.equals(query.onComponentOnly())) {
      return;
    }
    Collection<String> viewUuids = query.viewUuids();
    String branchUuid = query.branchUuid();
    boolean onApplicationBranch = branchUuid != null && !viewUuids.isEmpty();
    if (onApplicationBranch) {
      allFilters.addFilterV2("__view", new SimpleFieldFilterScope("view"), createViewFilter(singletonList(query.branchUuid())));
    } else {
      allFilters.addFilterV2("__view", new SimpleFieldFilterScope("view"), createViewFilter(viewUuids));
    }
  }

  @CheckForNull
  private static Query createViewFilter(Collection<String> viewUuids) {
    if (viewUuids.isEmpty()) {
      return null;
    }
    return ES8QueryHelper.boolQuery(b -> {
      for (String viewUuid : viewUuids) {
        b.should(termsLookupQuery(FIELD_ISSUE_BRANCH_UUID,
          TYPE_VIEW.getIndex().getName(), viewUuid, ViewIndexDefinition.FIELD_PROJECTS));
      }
    });
  }

  private static Query termsLookupQuery(String field, String indexName, String id, String path) {
    return Query.of(q -> q.terms(t -> t.field(field).terms(TermsQueryField.of(tv -> tv
      .lookup(l -> l.index(indexName).id(id).path(path))))));
  }

  @CheckForNull
  private static Query createTermsFilter(String field, Collection<?> values) {
    if (values.isEmpty()) {
      return null;
    }
    List<FieldValue> fvs = values.stream().filter(Objects::nonNull).map(v -> FieldValue.of(v.toString())).toList();
    return Query.of(q -> q.terms(t -> t.field(field).terms(tv -> tv.value(fvs))));
  }

  @CheckForNull
  private static Query createTermsFilterV2ForNullableCollection(String field, @Nullable Collection<?> values) {
    if (values == null) {
      return null;
    }
    List<FieldValue> fvs = values.stream().filter(Objects::nonNull).map(v -> FieldValue.of(v.toString())).toList();
    return Query.of(q -> q.terms(t -> t.field(field).terms(tv -> tv.value(fvs))));
  }

  @CheckForNull
  private static Query createTermFilter(String field, @Nullable String value) {
    return value == null ? null : ES8QueryHelper.termQuery(field, value);
  }

  @CheckForNull
  private static Query createTermFilter(String field, @Nullable Boolean value) {
    return value == null ? null : ES8QueryHelper.termQuery(field, value);
  }

  private void addDatesFilter(AllFilters filters, IssueQuery query) {
    PeriodStart createdAfter = query.createdAfter();
    Date createdBefore = query.createdBefore();

    validateCreationDateBounds(createdBefore, createdAfter != null ? createdAfter.date() : null);

    if (createdAfter != null) {
      long after = createdAfter.date().getTime();
      Query q = createdAfter.inclusive()
        ? ES8QueryHelper.untypedRangeQueryGte(FIELD_ISSUE_FUNC_CREATED_AT, after)
        : ES8QueryHelper.untypedRangeQueryGt(FIELD_ISSUE_FUNC_CREATED_AT, after);
      filters.addFilterV2("__createdAfter", CREATED_AT.getFilterScope(), q);
    }
    if (createdBefore != null) {
      filters.addFilterV2("__createdBefore", CREATED_AT.getFilterScope(),
        ES8QueryHelper.untypedRangeQueryLt(FIELD_ISSUE_FUNC_CREATED_AT, createdBefore.getTime()));
    }
    Date createdAt = query.createdAt();
    if (createdAt != null) {
      filters.addFilterV2("__createdAt", CREATED_AT.getFilterScope(),
        ES8QueryHelper.termQuery(FIELD_ISSUE_FUNC_CREATED_AT, createdAt.getTime()));
    }
  }

  private static void addNewCodeReferenceFilter(AllFilters filters, IssueQuery query) {
    Boolean newCodeOnReference = query.newCodeOnReference();
    if (newCodeOnReference != null) {
      filters.addFilterV2(
        FIELD_ISSUE_NEW_CODE_REFERENCE, new SimpleFieldFilterScope(FIELD_ISSUE_NEW_CODE_REFERENCE),
        ES8QueryHelper.termQuery(FIELD_ISSUE_NEW_CODE_REFERENCE, newCodeOnReference));
    }
  }

  private static void addNewCodeByProjectsFilter(AllFilters allFilters, IssueQuery query) {
    Map<String, PeriodStart> createdAfterByProjectUuids = query.createdAfterByProjectUuids();
    Query boolQ = ES8QueryHelper.boolQuery(b -> {
      createdAfterByProjectUuids.forEach((projectOrProjectBranchUuid, createdAfterDate) -> b.should(ES8QueryHelper.boolQuery(bb -> {
        bb.filter(ES8QueryHelper.termQuery(FIELD_ISSUE_BRANCH_UUID, projectOrProjectBranchUuid));
        long after = createdAfterDate.date().getTime();
        bb.filter(createdAfterDate.inclusive()
          ? ES8QueryHelper.untypedRangeQueryGte(FIELD_ISSUE_FUNC_CREATED_AT, after)
          : ES8QueryHelper.untypedRangeQueryGt(FIELD_ISSUE_FUNC_CREATED_AT, after));
      })));
      query.newCodeOnReferenceByProjectUuids().forEach(projectOrProjectBranchUuid -> b.should(ES8QueryHelper.boolQuery(bb -> bb
        .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_BRANCH_UUID, projectOrProjectBranchUuid))
        .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_NEW_CODE_REFERENCE, true)))));
    });
    allFilters.addFilterV2("__new_code_by_project_uuids", new SimpleFieldFilterScope("newCodeByProjectUuids"), boolQ);
  }

  private void configureTopAggregations(IssueQuery query, SearchOptions options,
    co.elastic.clients.elasticsearch.core.SearchRequest.Builder esRequest, AllFilters allFilters,
    RequestFiltersComputer filterComputer) {
    TopAggregationHelper aggregationHelper = newAggregationHelper(filterComputer, query);

    addFacetIfNeeded(options, aggregationHelper, esRequest, STATUSES, NO_SELECTED_VALUES);
    addFacetIfNeeded(options, aggregationHelper, esRequest, ISSUE_STATUSES, query.issueStatuses().toArray());
    addFacetIfNeeded(options, aggregationHelper, esRequest, PROJECT_UUIDS, query.projectUuids().toArray());
    addFacetIfNeeded(options, aggregationHelper, esRequest, DIRECTORIES, query.directories().toArray());
    addFacetIfNeeded(options, aggregationHelper, esRequest, FILES, query.files().toArray());
    addFacetIfNeeded(options, aggregationHelper, esRequest, SCOPES, query.scopes().toArray());
    addFacetIfNeeded(options, aggregationHelper, esRequest, LANGUAGES, query.languages().toArray());
    addFacetIfNeeded(options, aggregationHelper, esRequest, RULES, query.ruleUuids().toArray());
    addFacetIfNeeded(options, aggregationHelper, esRequest, AUTHOR, query.authors().toArray());
    addFacetIfNeeded(options, aggregationHelper, esRequest, TAGS, query.tags().toArray());
    addFacetIfNeeded(options, aggregationHelper, esRequest, TYPES, query.types().toArray());
    addFacetIfNeeded(options, aggregationHelper, esRequest, CODE_VARIANTS, query.codeVariants().toArray());
    addFacetIfNeeded(options, aggregationHelper, esRequest, CLEAN_CODE_ATTRIBUTE_CATEGORY,
      query.cleanCodeAttributesCategories().toArray());
    addFacetIfNeeded(options, aggregationHelper, esRequest, PRIORITIZED_RULE, ArrayUtils.EMPTY_OBJECT_ARRAY);
    addFacetIfNeeded(options, aggregationHelper, esRequest, FROM_SONAR_QUBE_UPDATE, ArrayUtils.EMPTY_OBJECT_ARRAY);
    addFacetIfNeeded(options, aggregationHelper, esRequest, LINKED_JIRA_WORK_ITEM, ArrayUtils.EMPTY_OBJECT_ARRAY);

    addSecurityCategoryFacetIfNeeded(PARAM_PCI_DSS_32, PCI_DSS_32, options, aggregationHelper, esRequest, query.pciDss32().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_PCI_DSS_40, PCI_DSS_40, options, aggregationHelper, esRequest, query.pciDss40().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_OWASP_ASVS_40, OWASP_ASVS_40, options, aggregationHelper, esRequest,
      query.owaspAsvs40().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_OWASP_MOBILE_TOP_10_2024, OWASP_MOBILE_TOP_10_2024, options, aggregationHelper, esRequest,
      query.owaspMobileTop10For2024().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_OWASP_TOP_10, OWASP_TOP_10, options, aggregationHelper, esRequest,
      query.owaspTop10().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_OWASP_TOP_10_2021, OWASP_TOP_10_2021, options, aggregationHelper, esRequest,
      query.owaspTop10For2021().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_STIG_ASD_V5R3, STIG_ASD_V5R3, options, aggregationHelper, esRequest,
      query.stigAsdV5R3().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_CASA, CASA, options, aggregationHelper, esRequest, query.casa().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_SANS_TOP_25, SANS_TOP_25, options, aggregationHelper, esRequest, query.sansTop25().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_CWE, CWE, options, aggregationHelper, esRequest, query.cwe().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_SONARSOURCE_SECURITY, SONARSOURCE_SECURITY, options, aggregationHelper, esRequest,
      query.sonarsourceSecurity().toArray());

    addSeverityFacetIfNeeded(options, aggregationHelper, esRequest);
    addImpactSoftwareQualityFacetIfNeeded(options, query, aggregationHelper, esRequest);
    addImpactSeverityFacetIfNeeded(options, query, aggregationHelper, esRequest);
    addResolutionFacetIfNeeded(options, query, aggregationHelper, esRequest);
    addAssigneesFacetIfNeeded(options, query, aggregationHelper, esRequest);
    addCreatedAtFacetIfNeeded(options, query, aggregationHelper, allFilters, esRequest);
    addAssignedToMeFacetIfNeeded(options, aggregationHelper, esRequest);
    addEffortTopAggregation(aggregationHelper, esRequest);
    addComplianceStandardsFacetsIfNeeded(options, aggregationHelper, esRequest);
  }

  private static TopAggregationHelper newAggregationHelper(RequestFiltersComputer filterComputer, IssueQuery query) {
    if (hasQueryEffortFacet(query)) {
      Aggregation effortSubAgg = Aggregation.of(a -> a.sum(s -> s.field(FIELD_ISSUE_EFFORT)));
      List<NamedValue<SortOrder>> effortOrder = List.of(new NamedValue<>(FACET_MODE_EFFORT, SortOrder.Desc));
      return new TopAggregationHelper(filterComputer, new SubAggregationHelper(effortSubAgg, effortOrder, FACET_MODE_EFFORT));
    }
    return new TopAggregationHelper(filterComputer, new SubAggregationHelper());
  }

  private static Aggregation effortAggregation() {
    return Aggregation.of(a -> a.sum(s -> s.field(FIELD_ISSUE_EFFORT)));
  }

  private static void addFacetIfNeeded(SearchOptions options, TopAggregationHelper aggregationHelper,
    co.elastic.clients.elasticsearch.core.SearchRequest.Builder esRequest, Facet facet, Object[] selectedValues) {
    if (!options.getFacets().contains(facet.getName())) {
      return;
    }
    Aggregation topAggregation = aggregationHelper.buildTermTopAggregationV2(
      facet.getName(), facet.getTopAggregationDef(), facet.getNumberOfTerms(),
      TopAggregationHelper.NO_EXTRA_FILTER_V2,
      subAggs -> aggregationHelper.getSubAggregationHelper()
        .buildSelectedItemsAggregationV2(facet.getTopAggregationDef(), selectedValues)
        .ifPresent(agg -> subAggs.put(facet.getName() + org.sonar.server.es.Facets.SELECTED_SUB_AGG_NAME_SUFFIX, agg)));
    esRequest.aggregations(facet.getName(), topAggregation);
  }

  private static void addComplianceStandardsFacetsIfNeeded(SearchOptions options, TopAggregationHelper aggregationHelper,
    co.elastic.clients.elasticsearch.core.SearchRequest.Builder esRequest) {
    if (options.getComplianceFacets().isEmpty()) {
      return;
    }
    TopAggregationDefinition<?> aggDefinition = new SimpleFieldTopAggregationDefinition(FIELD_ISSUE_RULE_UUID, true);
    Aggregation aggregation = aggregationHelper.buildTermTopAggregationV2(COMPLIANCE_FILTER_FACET_NAME, aggDefinition, 65535,
      COMPLIANCE_FILTER_FACET_NAME);
    esRequest.aggregations(COMPLIANCE_FILTER_FACET_NAME, aggregation);
  }

  private void addSecurityCategoryFacetIfNeeded(String param, Facet facet, SearchOptions options,
    TopAggregationHelper aggregationHelper,
    co.elastic.clients.elasticsearch.core.SearchRequest.Builder esRequest, Object[] selectedValues) {
    if (!options.getFacets().contains(param)) {
      return;
    }
    Query securityCategoryFilter = isMQRMode()
      ? ES8QueryHelper.nestedQuery(FIELD_ISSUE_IMPACTS,
        ES8QueryHelper.termsQuery(FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY, List.of(SoftwareQuality.SECURITY.name())),
        ChildScoreMode.Avg)
      : ES8QueryHelper.termQuery(FIELD_ISSUE_TYPE, VULNERABILITY.name());

    Aggregation aggregation = aggregationHelper.buildTermTopAggregationV2(
      facet.getName(), facet.getTopAggregationDef(), facet.getNumberOfTerms(),
      b -> b.must(securityCategoryFilter),
      subAggs -> aggregationHelper.getSubAggregationHelper()
        .buildSelectedItemsAggregationV2(facet.getTopAggregationDef(), selectedValues)
        .ifPresent(agg -> subAggs.put(facet.getName() + org.sonar.server.es.Facets.SELECTED_SUB_AGG_NAME_SUFFIX, agg)));
    esRequest.aggregations(facet.getName(), aggregation);
  }

  private static void addSeverityFacetIfNeeded(SearchOptions options, TopAggregationHelper aggregationHelper,
    co.elastic.clients.elasticsearch.core.SearchRequest.Builder esRequest) {
    if (!options.getFacets().contains(PARAM_SEVERITIES)) {
      return;
    }
    Aggregation aggregation = aggregationHelper.buildTermTopAggregationV2(
      SEVERITIES.getName(), SEVERITIES.getTopAggregationDef(), SEVERITIES.getNumberOfTerms(),
      b -> b.mustNot(ES8QueryHelper.termQuery(FIELD_ISSUE_TYPE, SECURITY_HOTSPOT.name())),
      TopAggregationHelper.NO_OTHER_SUBAGGREGATION_V2);
    esRequest.aggregations(SEVERITIES.getName(), aggregation);
  }

  private static void addImpactSoftwareQualityFacetIfNeeded(SearchOptions options, IssueQuery query,
    TopAggregationHelper aggregationHelper, co.elastic.clients.elasticsearch.core.SearchRequest.Builder esRequest) {
    if (!options.getFacets().contains(PARAM_IMPACT_SOFTWARE_QUALITIES)) {
      return;
    }

    Function<SoftwareQuality, Query> mainQuery = softwareQuality -> ES8QueryHelper.boolQuery(b -> b
      .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY, softwareQuality.name())));

    Map<String, Query> keyed = new LinkedHashMap<>();
    Arrays.stream(SoftwareQuality.values()).forEach(softwareQuality -> {
      Query mq = mainQuery.apply(softwareQuality);
      Query effective = query.impactSeverities().isEmpty()
        ? mq
        : ES8QueryHelper.boolQuery(b -> b.filter(mq).filter(ES8QueryHelper.termsQuery(FIELD_ISSUE_IMPACT_SEVERITY,
            query.impactSeverities())));
      keyed.put(softwareQuality.name(), effective);
    });

    Aggregation aggregation = aggregationHelper.buildTopAggregationV2(
      IMPACT_SOFTWARE_QUALITY.getName(), IMPACT_SOFTWARE_QUALITY.getTopAggregationDef(),
      TopAggregationHelper.NO_EXTRA_FILTER_V2,
      subAggs -> subAggs.put("nested_" + IMPACT_SOFTWARE_QUALITY.getName(), Aggregation.of(a -> a
        .nested(n -> n.path(FIELD_ISSUE_IMPACTS))
        .aggregations(IMPACT_SOFTWARE_QUALITY.getName(),
          Aggregation.of(fa -> fa.filters(f -> f.filters(fb -> fb.keyed(keyed))))))));
    esRequest.aggregations(IMPACT_SOFTWARE_QUALITY.getName(), aggregation);
  }

  private static void addImpactSeverityFacetIfNeeded(SearchOptions options, IssueQuery query,
    TopAggregationHelper aggregationHelper, co.elastic.clients.elasticsearch.core.SearchRequest.Builder esRequest) {
    if (!options.getFacets().contains(PARAM_IMPACT_SEVERITIES)) {
      return;
    }

    Function<org.sonar.api.issue.impact.Severity, Query> mainQuery = severity -> ES8QueryHelper.boolQuery(b -> b
      .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_IMPACT_SEVERITY, severity.name())));

    Map<String, Query> keyed = new LinkedHashMap<>();
    Arrays.stream(org.sonar.api.issue.impact.Severity.values()).forEach(severity -> {
      Query mq = mainQuery.apply(severity);
      Query effective = query.impactSoftwareQualities().isEmpty()
        ? mq
        : ES8QueryHelper.boolQuery(b -> b.filter(mq).filter(ES8QueryHelper.termsQuery(FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY,
            query.impactSoftwareQualities())));
      keyed.put(severity.name(), effective);
    });

    Aggregation filtersAgg = Aggregation.of(fa -> fa
      .filters(f -> f.filters(fb -> fb.keyed(keyed)))
      .aggregations("reverse_nested_" + IMPACT_SOFTWARE_QUALITY.getName(),
        Aggregation.of(ra -> ra.reverseNested(rn -> rn))));

    Aggregation aggregation = aggregationHelper.buildTopAggregationV2(
      IMPACT_SEVERITY.getName(), IMPACT_SEVERITY.getTopAggregationDef(),
      TopAggregationHelper.NO_EXTRA_FILTER_V2,
      subAggs -> subAggs.put("nested_" + IMPACT_SEVERITY.getName(), Aggregation.of(a -> a
        .nested(n -> n.path(FIELD_ISSUE_IMPACTS))
        .aggregations(IMPACT_SEVERITY.getName(), filtersAgg))));
    esRequest.aggregations(IMPACT_SEVERITY.getName(), aggregation);
  }

  private static void addResolutionFacetIfNeeded(SearchOptions options, IssueQuery query, TopAggregationHelper aggregationHelper,
    co.elastic.clients.elasticsearch.core.SearchRequest.Builder esRequest) {
    if (!options.getFacets().contains(PARAM_RESOLUTIONS)) {
      return;
    }
    Aggregation aggregation = aggregationHelper.buildTermTopAggregationV2(
      RESOLUTIONS.getName(), RESOLUTIONS.getTopAggregationDef(), RESOLUTIONS.getNumberOfTerms(),
      TopAggregationHelper.NO_EXTRA_FILTER_V2,
      subAggs -> subAggs.put(RESOLUTIONS.getName() + FACET_SUFFIX_MISSING, missingFacet(query, RESOLUTIONS.getFieldName())));
    esRequest.aggregations(RESOLUTIONS.getName(), aggregation);
  }

  private static Aggregation missingFacet(IssueQuery query, String fieldName) {
    return Aggregation.of(a -> {
      a.missing(m -> m.field(fieldName));
      if (hasQueryEffortFacet(query)) {
        a.aggregations(FACET_MODE_EFFORT, effortAggregation());
      }
      return a;
    });
  }

  private static void addAssigneesFacetIfNeeded(SearchOptions options, IssueQuery query, TopAggregationHelper aggregationHelper,
    co.elastic.clients.elasticsearch.core.SearchRequest.Builder esRequest) {
    if (!options.getFacets().contains(PARAM_ASSIGNEES)) {
      return;
    }
    Aggregation aggregation = aggregationHelper.buildTermTopAggregationV2(
      ASSIGNEES.getName(), ASSIGNEES.getTopAggregationDef(), ASSIGNEES.getNumberOfTerms(),
      TopAggregationHelper.NO_EXTRA_FILTER_V2,
      subAggs -> {
        Object[] assignees = query.assignees().toArray();
        aggregationHelper.getSubAggregationHelper()
          .buildSelectedItemsAggregationV2(ASSIGNEES.getTopAggregationDef(), assignees)
          .ifPresent(agg -> subAggs.put(ASSIGNEES.getName() + org.sonar.server.es.Facets.SELECTED_SUB_AGG_NAME_SUFFIX, agg));
        subAggs.put(ASSIGNEES.getName() + FACET_SUFFIX_MISSING, missingFacet(query, ASSIGNEES.getFieldName()));
      });
    esRequest.aggregations(ASSIGNEES.getName(), aggregation);
  }

  private void addCreatedAtFacetIfNeeded(SearchOptions options, IssueQuery query, TopAggregationHelper aggregationHelper,
    AllFilters allFilters, co.elastic.clients.elasticsearch.core.SearchRequest.Builder esRequest) {
    if (!options.getFacets().contains(PARAM_CREATED_AT)) {
      return;
    }
    getCreatedAtFacet(query, aggregationHelper, allFilters).ifPresent(agg -> esRequest.aggregations(CREATED_AT.getName(), agg));
  }

  private Optional<Aggregation> getCreatedAtFacet(IssueQuery query, TopAggregationHelper aggregationHelper, AllFilters allFilters) {
    long startTime;
    boolean startInclusive;
    PeriodStart createdAfter = query.createdAfter();
    if (createdAfter == null) {
      OptionalLong minDate = getMinCreatedAt(allFilters);
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
    CalendarInterval bucketSize = computeDateHistogramBucketSize(timeSpan);
    long effectiveStart = startInclusive ? startTime : (startTime + 1);
    long effectiveEnd = endTime - 1L;
    String timeZone = Optional.ofNullable(query.timeZone()).orElse(system.getDefaultTimeZone().toZoneId()).getId();

    Aggregation topAggregation = aggregationHelper.buildTopAggregationV2(
      CREATED_AT.getName(),
      CREATED_AT.getTopAggregationDef(),
      TopAggregationHelper.NO_EXTRA_FILTER_V2,
      subAggs -> subAggs.put(CREATED_AT.getName(), Aggregation.of(a -> {
        a.dateHistogram(dh -> {
          dh.field(CREATED_AT.getFieldName())
            .calendarInterval(bucketSize)
            .minDocCount(0)
            .format(DateUtils.DATETIME_FORMAT)
            .timeZone(timeZone)
            .extendedBounds(eb -> eb
              .min(co.elastic.clients.elasticsearch._types.aggregations.FieldDateMath.of(b -> b.value((double) effectiveStart)))
              .max(co.elastic.clients.elasticsearch._types.aggregations.FieldDateMath.of(b -> b.value((double) effectiveEnd))));
          return dh;
        });
        if (hasQueryEffortFacet(query)) {
          a.aggregations(FACET_MODE_EFFORT, effortAggregation());
        }
        return a;
      })));

    return Optional.of(topAggregation);
  }

  private static CalendarInterval computeDateHistogramBucketSize(Duration timeSpan) {
    if (timeSpan.isShorterThan(TWENTY_DAYS)) {
      return CalendarInterval.Day;
    }
    if (timeSpan.isShorterThan(TWENTY_WEEKS)) {
      return CalendarInterval.Week;
    }
    if (timeSpan.isShorterThan(TWENTY_MONTHS)) {
      return CalendarInterval.Month;
    }
    return CalendarInterval.Year;
  }

  private OptionalLong getMinCreatedAt(AllFilters filters) {
    String facetNameAndField = CREATED_AT.getFieldName();
    List<Query> mustFilters = filters.streamV2().filter(Objects::nonNull).toList();

    co.elastic.clients.elasticsearch.core.SearchResponse<Object> response = client.searchV2(req -> {
      req.index(TYPE_ISSUE.getMainType().getIndex().getName())
        .size(0)
        .source(s -> s.fetch(false))
        .trackTotalHits(t -> t.enabled(true))
        .aggregations(facetNameAndField, Aggregation.of(a -> a.min(m -> m.field(facetNameAndField))));
      if (!mustFilters.isEmpty()) {
        req.query(ES8QueryHelper.boolQuery(b -> b.filter(ES8QueryHelper.boolQuery(bb -> mustFilters.forEach(bb::must)))));
      }
      return req;
    }, Object.class);

    // Min over zero docs is meaningless. ES8 client may serialise it as 0, NaN, or +/-infinity depending on version,
    // so we rely on the total hit count instead of trying to interpret the value.
    if (response.hits().total() != null && response.hits().total().value() == 0L) {
      return OptionalLong.empty();
    }
    Aggregate minAgg = response.aggregations().get(facetNameAndField);
    if (minAgg == null || !minAgg.isMin()) {
      return OptionalLong.empty();
    }
    double actualValue = minAgg.min().value();
    if (Double.isInfinite(actualValue) || Double.isNaN(actualValue)) {
      return OptionalLong.empty();
    }
    return OptionalLong.of((long) actualValue);
  }

  private void addAssignedToMeFacetIfNeeded(SearchOptions options, TopAggregationHelper aggregationHelper,
    co.elastic.clients.elasticsearch.core.SearchRequest.Builder esRequest) {
    String uuid = userSession.getUuid();
    if (options.getFacets().contains(ASSIGNED_TO_ME.getName()) && !StringUtils.isEmpty(uuid)) {
      Aggregation aggregation = aggregationHelper.buildTopAggregationV2(
        ASSIGNED_TO_ME.getName(),
        ASSIGNED_TO_ME.getTopAggregationDef(),
        TopAggregationHelper.NO_EXTRA_FILTER_V2,
        subAggs -> aggregationHelper.getSubAggregationHelper()
          .buildSelectedItemsAggregationV2(ASSIGNED_TO_ME.getTopAggregationDef(), new String[]{uuid})
          .ifPresent(agg -> subAggs.put(ASSIGNED_TO_ME.getName() + org.sonar.server.es.Facets.SELECTED_SUB_AGG_NAME_SUFFIX, agg)));
      esRequest.aggregations(ASSIGNED_TO_ME.getName(), aggregation);
    }
  }

  private static void addEffortTopAggregation(TopAggregationHelper aggregationHelper,
    co.elastic.clients.elasticsearch.core.SearchRequest.Builder esRequest) {
    Aggregation topAggregation = aggregationHelper.buildTopAggregationV2(
      FACET_MODE_EFFORT,
      EFFORT_TOP_AGGREGATION,
      TopAggregationHelper.NO_EXTRA_FILTER_V2,
      subAggs -> subAggs.put(FACET_MODE_EFFORT, effortAggregation()));
    esRequest.aggregations(FACET_MODE_EFFORT, topAggregation);
  }

  public List<String> searchTags(IssueQuery query, @Nullable String textQuery, int size) {
    StringTermsAggregate terms = listTermsMatching(FIELD_ISSUE_TAGS, query, textQuery,
      List.of(new NamedValue<>("_key", SortOrder.Asc)), size);
    return terms.buckets().array().stream().map(b -> b.key().stringValue()).toList();
  }

  public Map<String, Long> countTags(IssueQuery query, int maxNumberOfTags) {
    StringTermsAggregate terms = listTermsMatching(FIELD_ISSUE_TAGS, query, null,
      List.of(new NamedValue<>("_count", SortOrder.Desc)), maxNumberOfTags);
    Map<String, Long> result = new LinkedHashMap<>();
    terms.buckets().array().forEach(b -> result.put(b.key().stringValue(), b.docCount()));
    return result;
  }

  public List<String> searchAuthors(IssueQuery query, @Nullable String textQuery, int maxNumberOfAuthors) {
    StringTermsAggregate terms = listTermsMatching(FIELD_ISSUE_AUTHOR_LOGIN, query, textQuery,
      List.of(new NamedValue<>("_key", SortOrder.Asc)), maxNumberOfAuthors);
    return terms.buckets().array().stream().map(b -> b.key().stringValue()).toList();
  }

  private StringTermsAggregate listTermsMatching(String fieldName, IssueQuery query, @Nullable String textQuery,
    List<NamedValue<SortOrder>> termsOrder, int size) {
    co.elastic.clients.elasticsearch.core.SearchResponse<Object> response = client.searchV2(req -> req
      .index(TYPE_ISSUE.getMainType().getIndex().getName())
      .size(0)
      .source(s -> s.fetch(false))
      .query(ES8QueryHelper.boolQuery(b -> b
        .must(ES8QueryHelper.matchAllQuery())
        .filter(createBoolFilter(query))))
      .aggregations("_ref", Aggregation.of(a -> a.terms(t -> {
        t.field(fieldName)
          .size(size)
          .order(termsOrder)
          .minDocCount(1);
        if (textQuery != null) {
          t.include(i -> i.regexp(format(SUBSTRING_MATCH_REGEXP, escapeSpecialRegexChars(textQuery))));
        }
        return t;
      }))),
      Object.class);
    return response.aggregations().get("_ref").sterms();
  }

  private Query createBoolFilter(IssueQuery query) {
    return ES8QueryHelper.boolQuery(b -> createAllFilters(query).streamV2()
      .filter(Objects::nonNull)
      .forEach(b::must));
  }

  public List<ProjectStatistics> searchProjectStatistics(List<String> projectUuids, List<Long> froms,
    @Nullable String assigneeUuid) {
    checkState(projectUuids.size() == froms.size(),
      "Expected same size for projectUuids (had size %s) and froms (had size %s)", projectUuids.size(), froms.size());
    if (projectUuids.isEmpty()) {
      return Collections.emptyList();
    }

    co.elastic.clients.elasticsearch.core.SearchResponse<Object> response = client.searchV2(req -> {
      req.index(TYPE_ISSUE.getMainType().getIndex().getName())
        .size(0)
        .source(s -> s.fetch(false))
        .query(ES8QueryHelper.boolQuery(b -> b
          .mustNot(ES8QueryHelper.existsQuery(FIELD_ISSUE_RESOLUTION))
          .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_ASSIGNEE_UUID, assigneeUuid == null ? "" : assigneeUuid))
          .mustNot(ES8QueryHelper.termQuery(FIELD_ISSUE_TYPE, SECURITY_HOTSPOT.name()))));

      IntStream.range(0, projectUuids.size()).forEach(i -> {
        String projectUuid = projectUuids.get(i);
        long from = froms.get(i);
        req.aggregations(projectUuid, Aggregation.of(a -> a
          .filter(ES8QueryHelper.boolQuery(b -> b
            .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_PROJECT_UUID, projectUuid))
            .filter(ES8QueryHelper.untypedRangeQueryGte(FIELD_ISSUE_FUNC_CREATED_AT, from))))
          .aggregations("branchUuid", Aggregation.of(ba -> ba
            .terms(t -> t.field(FIELD_ISSUE_BRANCH_UUID))
            .aggregations(AGG_COUNT, Aggregation.of(ca -> ca.valueCount(vc -> vc.field(FIELD_ISSUE_KEY))))
            .aggregations("maxFuncCreatedAt", Aggregation.of(ma -> ma.max(m -> m.field(FIELD_ISSUE_FUNC_CREATED_AT))))))));
      });
      return req;
    }, Object.class);

    List<ProjectStatistics> results = new ArrayList<>();
    response.aggregations().forEach((projectUuid, agg) -> {
      FilterAggregate projectFilter = agg.filter();
      StringTermsAggregate branchTerms = projectFilter.aggregations().get("branchUuid").sterms();
      for (StringTermsBucket branchBucket : branchTerms.buckets().array()) {
        long count = (long) branchBucket.aggregations().get(AGG_COUNT).valueCount().value();
        if (count < 1L) {
          continue;
        }
        long lastIssueDate = (long) branchBucket.aggregations().get("maxFuncCreatedAt").max().value();
        results.add(new ProjectStatistics(branchBucket.key().stringValue(), count, lastIssueDate));
      }
    });
    return results;
  }

  // ---------- Security reports V2 ----------

  public List<SecurityStandardCategoryStatistics> getCweTop25Reports(String projectUuid, boolean isViewOrApp) {
    List<SecurityStandardCategoryStatistics> result = searchSecurityReports(projectUuid, isViewOrApp, builder -> CWES_BY_CWE_TOP_25.keySet()
      .forEach(cweYear -> builder.put(cweYear,
        newSecurityReportSubAggregations(
          ES8QueryHelper.existsQuery(FIELD_ISSUE_CWE),
          true,
          CWES_BY_CWE_TOP_25.get(cweYear)))), true, null);

    for (SecurityStandardCategoryStatistics cweReport : result) {
      Set<String> foundRules = cweReport.getChildren().stream()
        .map(SecurityStandardCategoryStatistics::getCategory)
        .collect(Collectors.toSet());
      CWES_BY_CWE_TOP_25.get(cweReport.getCategory()).stream()
        .filter(rule -> !foundRules.contains(rule))
        .forEach(rule -> cweReport.getChildren().add(emptyCweStatistics(rule)));
    }
    return result;
  }

  public List<SecurityStandardCategoryStatistics> getSonarSourceReport(String projectUuid, boolean isViewOrApp, boolean includeCwe) {
    return searchSecurityReports(projectUuid, isViewOrApp, builder -> Arrays.stream(SQCategory.values())
      .forEach(sonarsourceCategory -> builder.put(sonarsourceCategory.getKey(),
        newSecurityReportSubAggregations(
          ES8QueryHelper.termQuery(FIELD_ISSUE_SQ_SECURITY_CATEGORY, sonarsourceCategory.getKey()),
          includeCwe,
          SecurityStandards.CWES_BY_SQ_CATEGORY.get(sonarsourceCategory)))), includeCwe, null);
  }

  public List<SecurityStandardCategoryStatistics> getPciDssReport(String projectUuid, boolean isViewOrApp, PciDssVersion version) {
    return searchSecurityReportsWithDistribution(projectUuid, isViewOrApp, builder -> Arrays.stream(PciDss.values())
      .forEach(pciDss -> builder.put(pciDss.category(),
        newSecurityReportSubAggregations(
          ES8QueryHelper.prefixQuery(version.prefix(), pciDss.category() + "."),
          version.prefix()))), version.label(), null);
  }

  public List<SecurityStandardCategoryStatistics> getOwaspAsvsReport(String projectUuid, boolean isViewOrApp,
    RulesDefinition.OwaspAsvsVersion version, Integer level) {
    return searchSecurityReportsWithDistribution(projectUuid, isViewOrApp, builder -> Arrays.stream(SecurityStandards.OwaspAsvs.values())
      .forEach(owaspAsvs -> builder.put(owaspAsvs.category(),
        newSecurityReportSubAggregations(
          ES8QueryHelper.termsQuery(version.prefix(),
            SecurityStandards.getRequirementsForCategoryAndLevel(owaspAsvs, level)),
          version.prefix()))), version.label(), level);
  }

  public List<SecurityStandardCategoryStatistics> getOwaspAsvsReportGroupedByLevel(String projectUuid, boolean isViewOrApp,
    RulesDefinition.OwaspAsvsVersion version, int level) {
    String name = "l" + level;
    return searchSecurityReportsWithLevelDistribution(projectUuid, isViewOrApp, builder -> builder.put(name,
      newSecurityReportSubAggregations(
        ES8QueryHelper.termsQuery(version.prefix(),
          SecurityStandards.OWASP_ASVS_REQUIREMENTS_BY_LEVEL.get(version).get(level)),
        version.prefix())), version.label(), Integer.toString(level));
  }

  public List<SecurityStandardCategoryStatistics> getOwaspMobileTop10Report(String projectUuid, boolean isViewOrApp,
    boolean includeCwe, OwaspMobileTop10Version version) {
    return searchSecurityReports(projectUuid, isViewOrApp, builder -> IntStream.rangeClosed(1, 10).mapToObj(i -> "m" + i)
      .forEach(owaspMobileCategory -> builder.put(owaspMobileCategory,
        newSecurityReportSubAggregations(
          ES8QueryHelper.termQuery(version.prefix(), owaspMobileCategory),
          includeCwe, null))), includeCwe, version.label());
  }

  public List<SecurityStandardCategoryStatistics> getOwaspTop10Report(String projectUuid, boolean isViewOrApp, boolean includeCwe,
    OwaspTop10Version version) {
    return searchSecurityReports(projectUuid, isViewOrApp, builder -> IntStream.rangeClosed(1, 10).mapToObj(i -> "a" + i)
      .forEach(owaspCategory -> builder.put(owaspCategory,
        newSecurityReportSubAggregations(
          ES8QueryHelper.termQuery(version.prefix(), owaspCategory),
          includeCwe, null))), includeCwe, version.label());
  }

  public List<SecurityStandardCategoryStatistics> getStigReport(String projectUuid, boolean isViewOrApp,
    RulesDefinition.StigVersion stigVersion) {
    return searchSecurityReports(projectUuid, isViewOrApp, builder -> Arrays.stream(StigSupportedRequirement.values())
      .forEach(stigSupportedRequirement -> builder.put(stigSupportedRequirement.getRequirement(),
        newSecurityReportSubAggregations(
          ES8QueryHelper.termQuery(stigVersion.prefix(), stigSupportedRequirement.getRequirement()),
          false, null))), false, stigVersion.label());
  }

  public List<SecurityStandardCategoryStatistics> getCasaReport(String projectUuid, boolean isViewOrApp) {
    return searchSecurityReportsWithDistribution(projectUuid, isViewOrApp, builder -> IntStream.range(1, 15)
      .forEach(casaTopCategory -> builder.put(String.valueOf(casaTopCategory),
        newSecurityReportSubAggregations(
          ES8QueryHelper.prefixQuery(FIELD_ISSUE_CASA, casaTopCategory + "."),
          FIELD_ISSUE_CASA))), null, null);
  }
  private List<SecurityStandardCategoryStatistics> searchSecurityReports(String projectUuid, boolean isViewOrApp,
    Consumer<Map<String, Aggregation>> aggsBuilder, boolean includeDistribution, @Nullable String version) {
    co.elastic.clients.elasticsearch.core.SearchResponse<Object> response = executeSecurityReportSearch(projectUuid, isViewOrApp, aggsBuilder);
    return response.aggregations().entrySet().stream()
      .map(entry -> processSecurityReportIssueSearchResults(entry.getKey(), entry.getValue().filter(), includeDistribution, version))
      .toList();
  }

  private List<SecurityStandardCategoryStatistics> searchSecurityReportsWithDistribution(String projectUuid, boolean isViewOrApp,
    Consumer<Map<String, Aggregation>> aggsBuilder, @Nullable String version, @Nullable Integer level) {
    co.elastic.clients.elasticsearch.core.SearchResponse<Object> response = executeSecurityReportSearch(projectUuid, isViewOrApp, aggsBuilder);
    return response.aggregations().entrySet().stream()
      .map(entry -> processSecurityReportIssueSearchResultsWithDistribution(entry.getKey(), entry.getValue().filter(), version, level))
      .toList();
  }

  private List<SecurityStandardCategoryStatistics> searchSecurityReportsWithLevelDistribution(String projectUuid, boolean isViewOrApp,
    Consumer<Map<String, Aggregation>> aggsBuilder, String version, String level) {
    co.elastic.clients.elasticsearch.core.SearchResponse<Object> response = executeSecurityReportSearch(projectUuid, isViewOrApp, aggsBuilder);
    return response.aggregations().entrySet().stream()
      .map(entry -> processSecurityReportIssueSearchResultsWithLevelDistribution(entry.getKey(), entry.getValue().filter(), version, level))
      .toList();
  }

  private co.elastic.clients.elasticsearch.core.SearchResponse<Object> executeSecurityReportSearch(String projectUuid,
    boolean isViewOrApp, Consumer<Map<String, Aggregation>> aggsBuilder) {
    return client.searchV2(req -> {
      req.index(TYPE_ISSUE.getMainType().getIndex().getName())
        .size(0)
        .source(s -> s.fetch(false))
        .query(prepareNonClosedVulnerabilitiesAndHotspotQuery(projectUuid, isViewOrApp));
      Map<String, Aggregation> aggs = new LinkedHashMap<>();
      aggsBuilder.accept(aggs);
      aggs.forEach(req::aggregations);
      return req;
    }, Object.class);
  }

  private Query prepareNonClosedVulnerabilitiesAndHotspotQuery(String projectUuid, boolean isViewOrApp) {
    Query componentFilter = isViewOrApp
      ? termsLookupQuery(FIELD_ISSUE_BRANCH_UUID, TYPE_VIEW.getIndex().getName(), projectUuid, ViewIndexDefinition.FIELD_PROJECTS)
      : ES8QueryHelper.termQuery(FIELD_ISSUE_BRANCH_UUID, projectUuid);

    Query nonResolvedOrSecImpact = isMQRMode()
      ? ES8QueryHelper.boolQuery(b -> b
        .filter(ES8QueryHelper.nestedQuery(FIELD_ISSUE_IMPACTS,
          ES8QueryHelper.termsQuery(FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY, List.of(SoftwareQuality.SECURITY.name())),
          ChildScoreMode.Avg))
        .mustNot(ES8QueryHelper.existsQuery(FIELD_ISSUE_RESOLUTION)))
      : ES8QueryHelper.boolQuery(b -> b
        .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_TYPE, VULNERABILITY.name()))
        .mustNot(ES8QueryHelper.existsQuery(FIELD_ISSUE_RESOLUTION)));

    Query toReviewHotspots = ES8QueryHelper.boolQuery(b -> b
      .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_TYPE, SECURITY_HOTSPOT.name()))
      .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_STATUS, Issue.STATUS_TO_REVIEW))
      .mustNot(ES8QueryHelper.existsQuery(FIELD_ISSUE_RESOLUTION)));

    Query reviewedHotspots = ES8QueryHelper.boolQuery(b -> b
      .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_TYPE, SECURITY_HOTSPOT.name()))
      .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_STATUS, Issue.STATUS_REVIEWED))
      .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_RESOLUTION, Issue.RESOLUTION_FIXED)));

    return ES8QueryHelper.boolQuery(b -> b
      .filter(componentFilter)
      .minimumShouldMatch("1")
      .should(nonResolvedOrSecImpact)
      .should(toReviewHotspots)
      .should(reviewedHotspots));
  }

  private Aggregation newSecurityReportSubAggregations(Query categoryFilter, String securityStandardVersionPrefix) {
    Aggregation distributionTerms = Aggregation.of(a -> a
      .terms(t -> t.field(securityStandardVersionPrefix).size(Facet.MAX_FACET_SIZE)));
    Aggregation distributionWithCounts = appendIssueCountSubAggs(distributionTerms);
    return Aggregation.of(a -> a
      .filter(ES8QueryHelper.boolQuery(b -> b.filter(categoryFilter)))
      .aggregations(buildIssueCountSubAggs(Map.of(AGG_DISTRIBUTION, distributionWithCounts))));
  }

  private Aggregation newSecurityReportSubAggregations(Query categoryFilter, boolean includeCwe,
    @Nullable Collection<String> cwesInCategory) {
    Map<String, Aggregation> extra = new LinkedHashMap<>();
    if (includeCwe) {
      Aggregation cwesTerms = Aggregation.of(a -> a.terms(t -> {
        t.field(FIELD_ISSUE_CWE).size(Facet.MAX_FACET_SIZE);
        if (cwesInCategory != null) {
          t.include(i -> i.terms(new ArrayList<>(cwesInCategory)));
        }
        return t;
      }));
      extra.put(AGG_DISTRIBUTION, appendIssueCountSubAggs(cwesTerms));
    }
    return Aggregation.of(a -> a
      .filter(ES8QueryHelper.boolQuery(b -> b.filter(categoryFilter)))
      .aggregations(buildIssueCountSubAggs(extra)));
  }

  /**
   * Adds the standard issue-count sub-aggregations (vulnerabilities/toReview/reviewed) to an existing terms aggregation
   * by combining them into a new Aggregation that copies the original terms config.
   */
  private Aggregation appendIssueCountSubAggs(Aggregation termsAgg) {
    Map<String, Aggregation> subAggs = buildIssueCountSubAggs(Map.of());
    return Aggregation.of(a -> {
      a.terms(termsAgg.terms());
      Map<String, Aggregation> existing = termsAgg.aggregations();
      if (existing != null) {
        existing.forEach(a::aggregations);
      }
      subAggs.forEach(a::aggregations);
      return a;
    });
  }

  private Map<String, Aggregation> buildIssueCountSubAggs(Map<String, Aggregation> additionalSubAggs) {
    Map<String, Aggregation> map = new LinkedHashMap<>(additionalSubAggs);
    map.put(AGG_VULNERABILITIES, Aggregation.of(a -> a
      .filter(getNonResolvedIssuesOrNonResolvedSecurityImpactQuery())
      .aggregations(AGG_SEVERITIES, getAggregationBuilderBasedOnMode())));
    map.put(AGG_TO_REVIEW_SECURITY_HOTSPOTS, Aggregation.of(a -> a
      .filter(toReviewHotspotsFilter())
      .aggregations(AGG_COUNT, Aggregation.of(c -> c.valueCount(vc -> vc.field(FIELD_ISSUE_KEY))))));
    map.put(AGG_REVIEWED_SECURITY_HOTSPOTS, Aggregation.of(a -> a
      .filter(reviewedHotspotsFilter())
      .aggregations(AGG_COUNT, Aggregation.of(c -> c.valueCount(vc -> vc.field(FIELD_ISSUE_KEY))))));
    return map;
  }

  private Aggregation getAggregationBuilderBasedOnMode() {
    if (isMQRMode()) {
      return Aggregation.of(a -> a
        .nested(n -> n.path(FIELD_ISSUE_IMPACTS))
        .aggregations(ISSUES_WITH_SECURITY_IMPACT, Aggregation.of(fa -> fa
          .filter(ES8QueryHelper.boolQuery(b -> b
            .filter(ES8QueryHelper.termsQuery(FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY, List.of(SoftwareQuality.SECURITY.name())))))
          .aggregations(AGG_IMPACT_SEVERITIES, Aggregation.of(ta -> ta
            .terms(t -> t.field(FIELD_ISSUE_IMPACT_SEVERITY))
            .aggregations(AGG_COUNT, Aggregation.of(c -> c.valueCount(vc -> vc.field(FIELD_ISSUE_IMPACT_SEVERITY)))))))));
    }
    return Aggregation.of(a -> a
      .terms(t -> t.field(FIELD_ISSUE_SEVERITY))
      .aggregations(AGG_COUNT, Aggregation.of(c -> c.valueCount(vc -> vc.field(FIELD_ISSUE_KEY)))));
  }

  private Query getNonResolvedIssuesOrNonResolvedSecurityImpactQuery() {
    return isMQRMode()
      ? ES8QueryHelper.boolQuery(b -> b
        .filter(ES8QueryHelper.nestedQuery(FIELD_ISSUE_IMPACTS,
          ES8QueryHelper.termsQuery(FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY, List.of(SoftwareQuality.SECURITY.name())),
          ChildScoreMode.Avg))
        .mustNot(ES8QueryHelper.existsQuery(FIELD_ISSUE_RESOLUTION)))
      : ES8QueryHelper.boolQuery(b -> b
        .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_TYPE, VULNERABILITY.name()))
        .mustNot(ES8QueryHelper.existsQuery(FIELD_ISSUE_RESOLUTION)));
  }

  private static Query toReviewHotspotsFilter() {
    return ES8QueryHelper.boolQuery(b -> b
      .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_TYPE, SECURITY_HOTSPOT.name()))
      .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_STATUS, Issue.STATUS_TO_REVIEW))
      .mustNot(ES8QueryHelper.existsQuery(FIELD_ISSUE_RESOLUTION)));
  }

  private static Query reviewedHotspotsFilter() {
    return ES8QueryHelper.boolQuery(b -> b
      .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_TYPE, SECURITY_HOTSPOT.name()))
      .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_STATUS, Issue.STATUS_REVIEWED))
      .filter(ES8QueryHelper.termQuery(FIELD_ISSUE_RESOLUTION, Issue.RESOLUTION_FIXED)));
  }

  // -- Security reports V2: result parsing --

  private SecurityStandardCategoryStatistics processSecurityReportIssueSearchResultsWithDistribution(String name, FilterAggregate categoryFilter,
    @Nullable String version, @Nullable Integer level) {
    StringTermsAggregate dist = categoryFilter.aggregations().get(AGG_DISTRIBUTION).sterms();
    List<SecurityStandardCategoryStatistics> children = dist.buckets().array().stream()
      .filter(categoryBucket -> CS.startsWith(categoryBucket.key().stringValue(), name + "."))
      .filter(categoryBucket -> level == null
        || OWASP_ASVS_40_REQUIREMENTS_BY_LEVEL.get(level).contains(categoryBucket.key().stringValue()))
      .map(categoryBucket -> processSecurityReportCategory(categoryBucket.aggregations(), categoryBucket.key().stringValue(), null, null))
      .toList();

    return processSecurityReportCategory(categoryFilter.aggregations(), name, children, version);
  }

  private SecurityStandardCategoryStatistics processSecurityReportIssueSearchResultsWithLevelDistribution(String name,
    FilterAggregate categoryFilter, String version, String level) {
    StringTermsAggregate dist = categoryFilter.aggregations().get(AGG_DISTRIBUTION).sterms();
    List<SecurityStandardCategoryStatistics> children = dist.buckets().array().stream()
      .filter(categoryBucket -> OWASP_ASVS_40_REQUIREMENTS_BY_LEVEL.get(Integer.parseInt(level))
        .contains(categoryBucket.key().stringValue()))
      .map(categoryBucket -> processSecurityReportCategory(categoryBucket.aggregations(), categoryBucket.key().stringValue(), null, null))
      .toList();

    return processSecurityReportCategory(categoryFilter.aggregations(), name, children, version);
  }

  private SecurityStandardCategoryStatistics processSecurityReportIssueSearchResults(String name, FilterAggregate categoryBucket,
    boolean includeDistribution, @Nullable String version) {
    List<SecurityStandardCategoryStatistics> children = new ArrayList<>();
    if (includeDistribution) {
      StringTermsAggregate dist = categoryBucket.aggregations().get(AGG_DISTRIBUTION).sterms();
      children = dist.buckets().array().stream()
        .map(cweBucket -> processSecurityReportCategory(cweBucket.aggregations(), cweBucket.key().stringValue(), null, null))
        .collect(toCollection(ArrayList::new));
    }
    return processSecurityReportCategory(categoryBucket.aggregations(), name, children, version);
  }

  private SecurityStandardCategoryStatistics processSecurityReportCategory(Map<String, Aggregate> categoryAggs, String categoryName,
    @Nullable List<SecurityStandardCategoryStatistics> children, @Nullable String version) {
    Aggregate severitiesAggregate = categoryAggs.get(AGG_VULNERABILITIES).filter().aggregations().get(AGG_SEVERITIES);

    SeverityAggregationDetails severityAggregationDetails = getSeverityDetails(severitiesAggregate);
    long vulnerabilities = severityAggregationDetails.getCount();
    OptionalInt severityRating = severityAggregationDetails.getRating();
    Map<String, Long> severityDistribution = severityAggregationDetails.getDistribution();

    long toReviewSecurityHotspots = (long) categoryAggs.get(AGG_TO_REVIEW_SECURITY_HOTSPOTS)
      .filter().aggregations().get(AGG_COUNT).valueCount().value();
    long reviewedSecurityHotspots = (long) categoryAggs.get(AGG_REVIEWED_SECURITY_HOTSPOTS)
      .filter().aggregations().get(AGG_COUNT).valueCount().value();

    Optional<Double> percent = computePercent(toReviewSecurityHotspots, reviewedSecurityHotspots);
    Integer securityReviewRating = computeRating(percent.orElse(null)).getIndex();

    return new SecurityStandardCategoryStatistics(categoryName, vulnerabilities, severityRating, toReviewSecurityHotspots,
      reviewedSecurityHotspots, securityReviewRating, children, version, severityDistribution);
  }

  private SeverityAggregationDetails getSeverityDetails(Aggregate severitiesAggregate) {
    List<StringTermsBucket> severityBuckets;
    long vulnerabilities;
    OptionalInt severityRating;
    if (isMQRMode()) {
      NestedAggregate nested = severitiesAggregate.nested();
      severityBuckets = nested.aggregations().get(ISSUES_WITH_SECURITY_IMPACT).filter()
        .aggregations().get(AGG_IMPACT_SEVERITIES).sterms().buckets().array();
      vulnerabilities = severityBuckets.stream()
        .mapToLong(b -> (long) b.aggregations().get(AGG_COUNT).valueCount().value()).sum();
      severityRating = severityBuckets.stream()
        .filter(b -> (long) b.aggregations().get(AGG_COUNT).valueCount().value() != 0)
        .mapToInt(b -> org.sonar.api.issue.impact.Severity.valueOf(b.key().stringValue()).ordinal() + 1)
        .max();
    } else {
      severityBuckets = severitiesAggregate.sterms().buckets().array();
      vulnerabilities = severityBuckets.stream()
        .mapToLong(b -> (long) b.aggregations().get(AGG_COUNT).valueCount().value()).sum();
      severityRating = severityBuckets.stream()
        .filter(b -> (long) b.aggregations().get(AGG_COUNT).valueCount().value() != 0)
        .mapToInt(b -> Severity.ALL.indexOf(b.key().stringValue()) + 1)
        .max();
    }
    Map<String, Long> severityDistribution = severityBuckets.stream()
      .collect(Collectors.toMap(
        b -> b.key().stringValue().toLowerCase(Locale.US),
        StringTermsBucket::docCount));
    return new SeverityAggregationDetails(vulnerabilities, severityRating, severityDistribution);
  }

  private static class SeverityAggregationDetails {
    private long count;
    private OptionalInt rating;
    private Map<String, Long> distribution;

    public SeverityAggregationDetails(long count, OptionalInt rating, Map<String, Long> distribution) {
      this.count = count;
      this.rating = rating;
      this.distribution = distribution;
    }

    public long getCount() {
      return count;
    }

    public OptionalInt getRating() {
      return rating;
    }

    public Map<String, Long> getDistribution() {
      return distribution;
    }
  }
}
