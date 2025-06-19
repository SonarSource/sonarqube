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
package org.sonar.server.issue.index;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.TermsLookup;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.HasAggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregator;
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.LongBounds;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Min;
import org.elasticsearch.search.aggregations.metrics.ParsedMax;
import org.elasticsearch.search.aggregations.metrics.ParsedValueCount;
import org.elasticsearch.search.aggregations.metrics.SumAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.joda.time.Duration;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.CleanCodeAttributeCategory;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.OwaspTop10Version;
import org.sonar.api.server.rule.RulesDefinition.PciDssVersion;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsUtils;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.Sorting;
import org.sonar.server.es.searchrequest.RequestFiltersComputer;
import org.sonar.server.es.searchrequest.RequestFiltersComputer.AllFilters;
import org.sonar.server.es.searchrequest.SimpleFieldTopAggregationDefinition;
import org.sonar.server.es.searchrequest.SubAggregationHelper;
import org.sonar.server.es.searchrequest.TopAggregationDefinition;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.FilterScope;
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
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filters;
import static org.elasticsearch.search.aggregations.AggregationBuilders.reverseNested;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_DEFAULT_VALUE;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;
import static org.sonar.server.es.EsClient.LOGGER;
import static org.sonar.server.es.EsUtils.escapeSpecialRegexChars;
import static org.sonar.server.es.IndexType.FIELD_INDEX_TYPE;
import static org.sonar.server.es.searchrequest.TopAggregationDefinition.NON_STICKY;
import static org.sonar.server.es.searchrequest.TopAggregationDefinition.STICKY;
import static org.sonar.server.es.searchrequest.TopAggregationHelper.NO_EXTRA_FILTER;
import static org.sonar.server.es.searchrequest.TopAggregationHelper.NO_OTHER_SUBAGGREGATION;
import static org.sonar.server.issue.index.IssueIndex.Facet.ASSIGNED_TO_ME;
import static org.sonar.server.issue.index.IssueIndex.Facet.ASSIGNEES;
import static org.sonar.server.issue.index.IssueIndex.Facet.AUTHOR;
import static org.sonar.server.issue.index.IssueIndex.Facet.CASA;
import static org.sonar.server.issue.index.IssueIndex.Facet.CLEAN_CODE_ATTRIBUTE_CATEGORY;
import static org.sonar.server.issue.index.IssueIndex.Facet.CODE_VARIANTS;
import static org.sonar.server.issue.index.IssueIndex.Facet.CREATED_AT;
import static org.sonar.server.issue.index.IssueIndex.Facet.CWE;
import static org.sonar.server.issue.index.IssueIndex.Facet.DIRECTORIES;
import static org.sonar.server.issue.index.IssueIndex.Facet.FILES;
import static org.sonar.server.issue.index.IssueIndex.Facet.IMPACT_SEVERITY;
import static org.sonar.server.issue.index.IssueIndex.Facet.IMPACT_SOFTWARE_QUALITY;
import static org.sonar.server.issue.index.IssueIndex.Facet.ISSUE_STATUSES;
import static org.sonar.server.issue.index.IssueIndex.Facet.LANGUAGES;
import static org.sonar.server.issue.index.IssueIndex.Facet.OWASP_ASVS_40;
import static org.sonar.server.issue.index.IssueIndex.Facet.OWASP_TOP_10;
import static org.sonar.server.issue.index.IssueIndex.Facet.OWASP_TOP_10_2021;
import static org.sonar.server.issue.index.IssueIndex.Facet.PCI_DSS_32;
import static org.sonar.server.issue.index.IssueIndex.Facet.PCI_DSS_40;
import static org.sonar.server.issue.index.IssueIndex.Facet.PRIORITIZED_RULE;
import static org.sonar.server.issue.index.IssueIndex.Facet.PROJECT_UUIDS;
import static org.sonar.server.issue.index.IssueIndex.Facet.RESOLUTIONS;
import static org.sonar.server.issue.index.IssueIndex.Facet.RULES;
import static org.sonar.server.issue.index.IssueIndex.Facet.SANS_TOP_25;
import static org.sonar.server.issue.index.IssueIndex.Facet.SCOPES;
import static org.sonar.server.issue.index.IssueIndex.Facet.SEVERITIES;
import static org.sonar.server.issue.index.IssueIndex.Facet.SONARSOURCE_SECURITY;
import static org.sonar.server.issue.index.IssueIndex.Facet.STATUSES;
import static org.sonar.server.issue.index.IssueIndex.Facet.STIG_ASD_V5R3;
import static org.sonar.server.issue.index.IssueIndex.Facet.TAGS;
import static org.sonar.server.issue.index.IssueIndex.Facet.TYPES;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_ASSIGNEE_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_AUTHOR_LOGIN;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_BRANCH_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_CASA;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_CLEAN_CODE_ATTRIBUTE_CATEGORY;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_CODE_VARIANTS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_COMPONENT_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_CVSS;
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
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_ORGANIZATION_UUID;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_NEW_STATUS;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_OWASP_ASVS_40;
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
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_TYPE;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_ISSUE_VULNERABILITY_PROBABILITY;
import static org.sonar.server.issue.index.IssueIndexDefinition.FIELD_PRIORITIZED_RULE;
import static org.sonar.server.issue.index.IssueIndexDefinition.TYPE_ISSUE;
import static org.sonar.server.security.SecurityReviewRating.computePercent;
import static org.sonar.server.security.SecurityReviewRating.computeRating;
import static org.sonar.server.security.SecurityStandards.CWES_BY_CWE_TOP_25;
import static org.sonar.server.security.SecurityStandards.OWASP_ASVS_40_REQUIREMENTS_BY_LEVEL;
import static org.sonar.server.view.index.ViewIndexDefinition.TYPE_VIEW;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.FACET_MODE_EFFORT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ASSIGNEES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_AUTHOR;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CASA;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CODE_VARIANTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AT;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CWE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_DIRECTORIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_FILES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_IMPACT_SEVERITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_IMPACT_SOFTWARE_QUALITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_ISSUE_STATUSES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_LANGUAGES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_ASVS_40;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_TOP_10;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_OWASP_TOP_10_2021;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PCI_DSS_32;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PCI_DSS_40;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_PRIORITIZED_RULE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RESOLUTIONS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_RULES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SANS_TOP_25;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SCOPES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SEVERITIES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SONARSOURCE_SECURITY;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_STATUSES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_STIG_ASD_V5R3;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_TYPES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CVSS;
import static org.sonar.server.issue.index.IssueIndex.Facet.CVSS;



/**
 * The unique entry-point to interact with Elasticsearch index "issues".
 * All the requests are listed here.
 */
public class IssueIndex {

  public static final String FACET_PROJECTS = "projects";
  public static final String FACET_ASSIGNED_TO_ME = "assigned_to_me";

  private static final int DEFAULT_FACET_SIZE = 15;
  private static final int DEFAULT_FACET_CVSS_SIZE = 50;

  private static final int MAX_FACET_SIZE = 100;
  private static final String AGG_VULNERABILITIES = "vulnerabilities";
  private static final String AGG_SEVERITIES = "severities";
  private static final String ISSUES_WITH_SECURITY_IMPACT = "issues_with_security_impact";
  private static final String AGG_IMPACT_SEVERITIES = "impact_severities";
  private static final String AGG_TO_REVIEW_SECURITY_HOTSPOTS = "toReviewSecurityHotspots";
  private static final String AGG_IN_REVIEW_SECURITY_HOTSPOTS = "inReviewSecurityHotspots";
  private static final String AGG_REVIEWED_SECURITY_HOTSPOTS = "reviewedSecurityHotspots";
  private static final String AGG_DISTRIBUTION = "distribution";
  private static final BoolQueryBuilder NON_RESOLVED_VULNERABILITIES_FILTER = boolQuery()
    .filter(termQuery(FIELD_ISSUE_TYPE, VULNERABILITY.name()))
    .mustNot(existsQuery(FIELD_ISSUE_RESOLUTION));
  private static final BoolQueryBuilder NON_RESOLVED_SECURITY_IMPACT_FILTER = boolQuery()
    .filter(nestedQuery(FIELD_ISSUE_IMPACTS, termsQuery(FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY, SoftwareQuality.SECURITY.name()),
      ScoreMode.Avg))
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
  private static final NestedQueryBuilder SECURITY_IMPACT_FILTER = nestedQuery(FIELD_ISSUE_IMPACTS,
    termsQuery(FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY, SoftwareQuality.SECURITY.name()), ScoreMode.Avg);
  private static final BoolQueryBuilder SECURITY_IMPACT_AND_HOTSPOT_FILTER =
    boolQuery()
      .should(SECURITY_IMPACT_FILTER)
      .should(termsQuery(FIELD_ISSUE_TYPE, SECURITY_HOTSPOT.name()))
      .minimumShouldMatch(1);




  private static final Object[] NO_SELECTED_VALUES = {0};
  private static final SimpleFieldTopAggregationDefinition EFFORT_TOP_AGGREGATION = new SimpleFieldTopAggregationDefinition(FIELD_ISSUE_EFFORT, NON_STICKY);

  public enum Facet {
    SEVERITIES(PARAM_SEVERITIES, FIELD_ISSUE_SEVERITY, STICKY, Severity.ALL.size()),
    IMPACT_SOFTWARE_QUALITY(PARAM_IMPACT_SOFTWARE_QUALITIES, FIELD_ISSUE_IMPACTS, STICKY),
    IMPACT_SEVERITY(PARAM_IMPACT_SEVERITIES, FIELD_ISSUE_IMPACTS, STICKY),
    CLEAN_CODE_ATTRIBUTE_CATEGORY(PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES, FIELD_ISSUE_CLEAN_CODE_ATTRIBUTE_CATEGORY, STICKY, CleanCodeAttributeCategory.values().length),
    STATUSES(PARAM_STATUSES, FIELD_ISSUE_STATUS, STICKY, Issue.STATUSES.size()),
    // Resolutions facet returns one more element than the number of resolutions to take into account unresolved issues
    RESOLUTIONS(PARAM_RESOLUTIONS, FIELD_ISSUE_RESOLUTION, STICKY, Issue.RESOLUTIONS.size() + 1),
    ISSUE_STATUSES(PARAM_ISSUE_STATUSES, FIELD_ISSUE_NEW_STATUS, STICKY, IssueStatus.values().length),
    TYPES(PARAM_TYPES, FIELD_ISSUE_TYPE, STICKY, RuleType.values().length),
    SCOPES(PARAM_SCOPES, FIELD_ISSUE_SCOPE, STICKY, MAX_FACET_SIZE),
    LANGUAGES(PARAM_LANGUAGES, FIELD_ISSUE_LANGUAGE, STICKY, MAX_FACET_SIZE),
    RULES(PARAM_RULES, FIELD_ISSUE_RULE_UUID, STICKY, MAX_FACET_SIZE),
    TAGS(PARAM_TAGS, FIELD_ISSUE_TAGS, STICKY, MAX_FACET_SIZE),
    AUTHOR(PARAM_AUTHOR, FIELD_ISSUE_AUTHOR_LOGIN, STICKY, MAX_FACET_SIZE),
    PROJECT_UUIDS(FACET_PROJECTS, FIELD_ISSUE_PROJECT_UUID, STICKY, MAX_FACET_SIZE),
    FILES(PARAM_FILES, FIELD_ISSUE_FILE_PATH, STICKY, MAX_FACET_SIZE),
    DIRECTORIES(PARAM_DIRECTORIES, FIELD_ISSUE_DIRECTORY_PATH, STICKY, MAX_FACET_SIZE),
    ASSIGNEES(PARAM_ASSIGNEES, FIELD_ISSUE_ASSIGNEE_UUID, STICKY, MAX_FACET_SIZE),
    ASSIGNED_TO_ME(FACET_ASSIGNED_TO_ME, FIELD_ISSUE_ASSIGNEE_UUID, STICKY, 1),
    PCI_DSS_32(PARAM_PCI_DSS_32, FIELD_ISSUE_PCI_DSS_32, STICKY, DEFAULT_FACET_SIZE),
    PCI_DSS_40(PARAM_PCI_DSS_40, FIELD_ISSUE_PCI_DSS_40, STICKY, DEFAULT_FACET_SIZE),
    OWASP_ASVS_40(PARAM_OWASP_ASVS_40, FIELD_ISSUE_OWASP_ASVS_40, STICKY, DEFAULT_FACET_SIZE),
    OWASP_TOP_10(PARAM_OWASP_TOP_10, FIELD_ISSUE_OWASP_TOP_10, STICKY, DEFAULT_FACET_SIZE),
    OWASP_TOP_10_2021(PARAM_OWASP_TOP_10_2021, FIELD_ISSUE_OWASP_TOP_10_2021, STICKY, DEFAULT_FACET_SIZE),
    STIG_ASD_V5R3(PARAM_STIG_ASD_V5R3, FIELD_ISSUE_STIG_ASD_V5R3, STICKY, DEFAULT_FACET_SIZE),
    CASA(PARAM_CASA, FIELD_ISSUE_CASA, STICKY, DEFAULT_FACET_SIZE),
    SANS_TOP_25(PARAM_SANS_TOP_25, FIELD_ISSUE_SANS_TOP_25, STICKY, DEFAULT_FACET_SIZE),
    CWE(PARAM_CWE, FIELD_ISSUE_CWE, STICKY, DEFAULT_FACET_SIZE),
    CREATED_AT(PARAM_CREATED_AT, FIELD_ISSUE_FUNC_CREATED_AT, NON_STICKY),
    SONARSOURCE_SECURITY(PARAM_SONARSOURCE_SECURITY, FIELD_ISSUE_SQ_SECURITY_CATEGORY, STICKY, DEFAULT_FACET_SIZE),
    CODE_VARIANTS(PARAM_CODE_VARIANTS, FIELD_ISSUE_CODE_VARIANTS, STICKY, MAX_FACET_SIZE),
    PRIORITIZED_RULE(PARAM_PRIORITIZED_RULE, FIELD_PRIORITIZED_RULE, STICKY, 2),
    CVSS(PARAM_CVSS, FIELD_ISSUE_CVSS, STICKY, DEFAULT_FACET_CVSS_SIZE);


    private final String name;
    private final TopAggregationDefinition<FilterScope> topAggregation;
    private final Integer numberOfTerms;

    Facet(String name, String fieldName, boolean sticky, int numberOfTerms) {
      this.name = name;
      this.topAggregation = new SimpleFieldTopAggregationDefinition(fieldName, sticky);
      this.numberOfTerms = numberOfTerms;
    }

    Facet(String name, String fieldName, boolean sticky) {
      this.name = name;
      this.topAggregation = new SimpleFieldTopAggregationDefinition(fieldName, sticky);
      this.numberOfTerms = null;
    }

    public String getName() {
      return name;
    }

    public String getFieldName() {
      return topAggregation.getFilterScope().getFieldName();
    }

    public FilterScope getFilterScope() {
      return topAggregation.getFilterScope();
    }

    public TopAggregationDefinition<FilterScope> getTopAggregationDef() {
      return topAggregation;
    }

    public int getNumberOfTerms() {
      checkState(numberOfTerms != null, "numberOfTerms should have been provided in constructor");

      return numberOfTerms;
    }
  }

  private static final Map<String, Facet> FACETS_BY_NAME = Arrays.stream(Facet.values())
    .collect(Collectors.toMap(Facet::getName, Function.identity()));

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

    // by default order by project, line , severity and issue key (in order to be deterministic when same ms)
    this.sorting.addDefault(FIELD_ISSUE_PROJECT_UUID);
    this.sorting.addDefault(FIELD_ISSUE_LINE);
    this.sorting.addDefault(FIELD_ISSUE_SEVERITY_VALUE).reverse();
    this.sorting.addDefault(FIELD_ISSUE_KEY);
  }

  public SearchResponse search(IssueQuery query, SearchOptions options) {
    SearchRequest requestBuilder = EsClient.prepareSearch(TYPE_ISSUE.getMainType());

    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    // Adding search_after parameter  to retrieve the next page of hits using a set of sort values from the previous page.
    if (StringUtils.isNotEmpty(query.searchAfter())) {
      Object[] searchAfterValues = Arrays.stream(query.searchAfter().split(",")).map(String::trim).toArray();
      sourceBuilder.searchAfter(searchAfterValues);
    }
    requestBuilder.source(sourceBuilder);

    configureSorting(query, sourceBuilder);
    configurePagination(options, sourceBuilder);
    configureRouting(query, options, requestBuilder);

    AllFilters allFilters = createAllFilters(query);
    RequestFiltersComputer filterComputer = newFilterComputer(options, allFilters);

    configureTopAggregations(query, options, sourceBuilder, allFilters, filterComputer);
    configureQuery(sourceBuilder, filterComputer);
    configureTopFilters(sourceBuilder, filterComputer);

    sourceBuilder.fetchSource(false)
      .trackTotalHits(true);

    return client.search(requestBuilder);
  }

  private void configureTopAggregations(IssueQuery query, SearchOptions options, SearchSourceBuilder esRequest, AllFilters allFilters,
    RequestFiltersComputer filterComputer) {
    TopAggregationHelper aggregationHelper = newAggregationHelper(filterComputer, query);

    configureTopAggregations(aggregationHelper, query, options, allFilters, esRequest);
  }

  private static void configureQuery(SearchSourceBuilder esRequest, RequestFiltersComputer filterComputer) {
    QueryBuilder esQuery = filterComputer.getQueryFilters()
      .map(t -> (QueryBuilder) boolQuery().must(matchAllQuery()).filter(t))
      .orElse(matchAllQuery());
    esRequest.query(esQuery);
  }

  private static void configureTopFilters(SearchSourceBuilder esRequest, RequestFiltersComputer filterComputer) {
    filterComputer.getPostFilters().ifPresent(esRequest::postFilter);
  }

  /**
   * Optimization - do not send ES request to all shards when scope is restricted
   * to a set of projects. Because project UUID is used for routing, the request
   * can be sent to only the shards containing the specified projects.
   * Note that sticky facets may involve all projects, so this optimization must be
   * disabled when facets are enabled.
   */
  private static void configureRouting(IssueQuery query, SearchOptions options, SearchRequest searchRequest) {
    Collection<String> uuids = query.projectUuids();
    if (!uuids.isEmpty() && options.getFacets().isEmpty()) {
      searchRequest.routing(uuids.stream().map(AuthorizationDoc::idOf).toArray(String[]::new));
    }
  }

  private static void configurePagination(SearchOptions options, SearchSourceBuilder esSearch) {
    esSearch.from(options.getOffset()).size(options.getLimit());
  }

  private AllFilters createAllFilters(IssueQuery query) {
    AllFilters filters = RequestFiltersComputer.newAllFilters();
    filters.addFilter("__indexType", new SimpleFieldFilterScope(FIELD_INDEX_TYPE), termQuery(FIELD_INDEX_TYPE, TYPE_ISSUE.getName()));
    filters.addFilter("__authorization", new SimpleFieldFilterScope("parent"), createAuthorizationFilter());

    // Issue is assigned Filter
    if (Boolean.TRUE.equals(query.assigned())) {
      filters.addFilter(IS_ASSIGNED_FILTER, Facet.ASSIGNEES.getFilterScope(), existsQuery(FIELD_ISSUE_ASSIGNEE_UUID));
    } else if (Boolean.FALSE.equals(query.assigned())) {
      filters.addFilter(IS_ASSIGNED_FILTER, ASSIGNEES.getFilterScope(), boolQuery().mustNot(existsQuery(FIELD_ISSUE_ASSIGNEE_UUID)));
    }

    // Issue is Resolved Filter
    if (Boolean.TRUE.equals(query.resolved())) {
      filters.addFilter("__isResolved", Facet.RESOLUTIONS.getFilterScope(), existsQuery(FIELD_ISSUE_RESOLUTION));
    } else if (Boolean.FALSE.equals(query.resolved())) {
      filters.addFilter("__isResolved", Facet.RESOLUTIONS.getFilterScope(), boolQuery().mustNot(existsQuery(FIELD_ISSUE_RESOLUTION)));
    }

    // Field Filters
    filters.addFilter(FIELD_ISSUE_KEY, new SimpleFieldFilterScope(FIELD_ISSUE_KEY), createTermsFilterForNullableCollection(FIELD_ISSUE_KEY, query.issueKeys()));
    filters.addFilter(FIELD_ISSUE_ASSIGNEE_UUID, ASSIGNEES.getFilterScope(), createTermsFilter(FIELD_ISSUE_ASSIGNEE_UUID, query.assignees()));
    filters.addFilter(FIELD_ISSUE_SCOPE, SCOPES.getFilterScope(), createTermsFilter(FIELD_ISSUE_SCOPE, query.scopes()));
    filters.addFilter(FIELD_ISSUE_LANGUAGE, LANGUAGES.getFilterScope(), createTermsFilter(FIELD_ISSUE_LANGUAGE, query.languages()));
    filters.addFilter(FIELD_ISSUE_TAGS, TAGS.getFilterScope(), createTermsFilter(FIELD_ISSUE_TAGS, query.tags()));
    filters.addFilter(FIELD_ISSUE_TYPE, TYPES.getFilterScope(), createTermsFilter(FIELD_ISSUE_TYPE, query.types()));
    filters.addFilter(
      FIELD_ISSUE_CLEAN_CODE_ATTRIBUTE_CATEGORY,
      CLEAN_CODE_ATTRIBUTE_CATEGORY.getFilterScope(),
      createTermsFilter(FIELD_ISSUE_CLEAN_CODE_ATTRIBUTE_CATEGORY, query.cleanCodeAttributesCategories()));
    filters.addFilter(
      FIELD_ISSUE_RESOLUTION, RESOLUTIONS.getFilterScope(),
      createTermsFilter(FIELD_ISSUE_RESOLUTION, query.resolutions()));
    filters.addFilter(
      FIELD_ISSUE_AUTHOR_LOGIN, AUTHOR.getFilterScope(),
      createTermsFilter(FIELD_ISSUE_AUTHOR_LOGIN, query.authors()));
    filters.addFilter(
      FIELD_ISSUE_RULE_UUID, RULES.getFilterScope(), createTermsFilter(
        FIELD_ISSUE_RULE_UUID,
        query.ruleUuids()));
    filters.addFilter(FIELD_ISSUE_STATUS, STATUSES.getFilterScope(), createTermsFilter(FIELD_ISSUE_STATUS, query.statuses()));
    filters.addFilter(FIELD_ISSUE_NEW_STATUS, ISSUE_STATUSES.getFilterScope(), createTermsFilter(FIELD_ISSUE_NEW_STATUS, query.issueStatuses()));
    filters.addFilter(FIELD_ISSUE_CODE_VARIANTS, CODE_VARIANTS.getFilterScope(), createTermsFilter(FIELD_ISSUE_CODE_VARIANTS, query.codeVariants()));
    filters.addFilter(FIELD_PRIORITIZED_RULE, PRIORITIZED_RULE.getFilterScope(), createTermFilter(FIELD_PRIORITIZED_RULE, query.prioritizedRule()));
    filters.addFilter(
      FIELD_ISSUE_ORGANIZATION_UUID, new SimpleFieldFilterScope(FIELD_ISSUE_ORGANIZATION_UUID),
      createTermFilter(FIELD_ISSUE_ORGANIZATION_UUID, query.organizationUuid()));

    // security category
    addSecurityCategoryPrefixFilter(FIELD_ISSUE_PCI_DSS_32, PCI_DSS_32, query.pciDss32(), filters);
    addSecurityCategoryPrefixFilter(FIELD_ISSUE_PCI_DSS_40, PCI_DSS_40, query.pciDss40(), filters);
    addOwaspAsvsFilter(FIELD_ISSUE_OWASP_ASVS_40, OWASP_ASVS_40, query, filters);
    addSecurityCategoryFilter(FIELD_ISSUE_OWASP_TOP_10, OWASP_TOP_10, query.owaspTop10(), filters);
    addSecurityCategoryFilter(FIELD_ISSUE_OWASP_TOP_10_2021, OWASP_TOP_10_2021, query.owaspTop10For2021(), filters);
    addSecurityCategoryFilter(FIELD_ISSUE_STIG_ASD_V5R3, STIG_ASD_V5R3, query.stigAsdV5R3(), filters);
    addSecurityCategoryPrefixFilter(FIELD_ISSUE_CASA, CASA, query.casa(), filters);
    addSecurityCategoryFilter(FIELD_ISSUE_SANS_TOP_25, SANS_TOP_25, query.sansTop25(), filters);
    addSecurityCategoryFilter(FIELD_ISSUE_CWE, CWE, query.cwe(), filters);
    addSecurityCategoryFilter(FIELD_ISSUE_SQ_SECURITY_CATEGORY, SONARSOURCE_SECURITY, query.sonarsourceSecurity(), filters);
    addCvssFilter(FIELD_ISSUE_CVSS, CVSS, query.cvss(), filters);

    addSeverityFilter(query, filters);
    addImpactFilters(query, filters);
    addComponentRelatedFilters(query, filters);
    addDatesFilter(filters, query);
    addCreatedAfterByProjectsFilter(filters, query);
    addNewCodeReferenceFilter(filters, query);
    addNewCodeReferenceFilterByProjectsFilter(filters, query);
    return filters;
  }

  private void addCvssFilter(String fieldName, Facet facet, Collection<String> cvssValues, AllFilters allFilters) {

    if (cvssValues == null || cvssValues.isEmpty()) {
      return;
    }

    BoolQueryBuilder cvssBool = QueryBuilders.boolQuery();

    for (String cvss : cvssValues) {
      try {
        if (cvss.contains("-")) {
          String[] range = cvss.split("-");
          double min = Double.parseDouble(range[0]);
          double max = Double.parseDouble(range[1]);
          cvssBool.should(QueryBuilders.rangeQuery(fieldName).gte(min).lt(max));
        } else {
          double val = Double.parseDouble(cvss);
          cvssBool.should(QueryBuilders.termQuery(fieldName, val));
        }
      } catch (NumberFormatException e) {
        LOGGER.error("Invalid CVSS value: ", cvss);
      }
    }
    cvssBool.minimumShouldMatch(1);

      allFilters.addFilter(fieldName, facet.getFilterScope(), cvssBool.must(getQueryBuilderForSecurityCategory()));
  }



  private void addOwaspAsvsFilter(String fieldName, Facet facet, IssueQuery query, AllFilters allFilters) {
    if (!CollectionUtils.isEmpty(query.owaspAsvs40())) {
      Set<String> requirements = calculateRequirementsForOwaspAsvs40Params(query);
      QueryBuilder securityCategoryFilter = termsQuery(fieldName, requirements);
      allFilters.addFilter(
        fieldName,
        facet.getFilterScope(),
        boolQuery()
          .must(securityCategoryFilter)
          .must(getQueryBuilderForSecurityCategory()));
    }
  }

  private QueryBuilder getQueryBuilderForSecurityCategory() {
    return isMQRMode() ? SECURITY_IMPACT_AND_HOTSPOT_FILTER : termsQuery(FIELD_ISSUE_TYPE, VULNERABILITY.name(), SECURITY_HOTSPOT.name());
  }

  private static Set<String> calculateRequirementsForOwaspAsvs40Params(IssueQuery query) {
    int level = query.getOwaspAsvsLevel().orElse(3);
    List<String> levelRequirements = OWASP_ASVS_40_REQUIREMENTS_BY_LEVEL.get(level);
    return query.owaspAsvs40().stream()
      .flatMap(value -> {
        // it's a specific category required
        if (value.contains(".")) {
          return Stream.of(value).filter(levelRequirements::contains);
        } else {
          return SecurityStandards.getRequirementsForCategoryAndLevel(value, level).stream();
        }
      }).collect(Collectors.toSet());
  }

  private void addSecurityCategoryFilter(String fieldName, Facet facet, Collection<String> values, AllFilters allFilters) {
    QueryBuilder securityCategoryFilter = createTermsFilter(fieldName, values);
    if (securityCategoryFilter != null) {
      allFilters.addFilter(
        fieldName,
        facet.getFilterScope(),
        boolQuery()
          .must(securityCategoryFilter)
          .must(getQueryBuilderForSecurityCategory()));
    }
  }

  /**
   * <p>Builds the Elasticsearch boolean query to filter the PCI DSS and CASA.</p>
   *
   * <p>The PCI DSS security report handles all the subcategories as one level. This means that subcategory 1.1 doesn't include the issues from 1.1.1.
   * Taking this into account, the search filter follows the same logic and uses prefix matching for top-level categories and exact matching for subcategories</p>
   *
   * <p>Example</p>
   * <p>List of PCI DSS categories in issues: {1.5.8, 1.5.9, 1.6.7}
   *   <ul>
   *     <li>Search: {1}, returns {1.5.8, 1.5.9, 1.6.7}</li>
   *     <li>Search: {1.5.8}, returns {1.5.8}</li>
   *     <li>Search: {1.5}, returns {}</li>
   *   </ul>
   * </p>
   *
   * @param fieldName  The PCI DSS version, e.g. pciDss-3.2
   * @param facet      The facet used for the filter
   * @param values     The PCI DSS categories to search for
   * @param allFilters Object that holds all the filters for the Elastic search call
   */
  private void addSecurityCategoryPrefixFilter(String fieldName, Facet facet, Collection<String> values, AllFilters allFilters) {
    if (values.isEmpty()) {
      return;
    }

    BoolQueryBuilder boolQueryBuilder = boolQuery()
      // ensures that at least one "should" query is matched. Without it, "should" queries are optional, when a "must" is also present.
      .minimumShouldMatch(1)
      .must(getQueryBuilderForSecurityCategory());
    // for top level categories a prefix query is added, while for subcategories a term query is used for exact matching
    values.stream().map(v -> choosePrefixQuery(fieldName, v)).forEach(boolQueryBuilder::should);

    allFilters.addFilter(
      fieldName,
      facet.getFilterScope(),
      boolQueryBuilder);
  }

  private static QueryBuilder choosePrefixQuery(String fieldName, String value) {
    return value.contains(".") ? createTermFilter(fieldName, value) : createPrefixFilter(fieldName, value + ".");
  }

  private static void addSeverityFilter(IssueQuery query, AllFilters allFilters) {
    QueryBuilder severityFieldFilter = createTermsFilter(FIELD_ISSUE_SEVERITY, query.severities());
    if (severityFieldFilter != null) {
      allFilters.addFilter(
        FIELD_ISSUE_SEVERITY,
        SEVERITIES.getFilterScope(),
        boolQuery()
          .must(severityFieldFilter)
          // Ignore severity of Security HotSpots
          .mustNot(termQuery(FIELD_ISSUE_TYPE, SECURITY_HOTSPOT.name())));
    }
  }

  private static void addImpactFilters(IssueQuery query, AllFilters allFilters) {
    if (query.impactSoftwareQualities().isEmpty() && query.impactSeverities().isEmpty()) {
      return;
    }

    if (!query.impactSoftwareQualities().isEmpty() && query.impactSeverities().isEmpty()) {
      allFilters.addFilter(
        FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY,
        IMPACT_SOFTWARE_QUALITY.getFilterScope(),
        nestedQuery(
          FIELD_ISSUE_IMPACTS,
          termsQuery(FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY, query.impactSoftwareQualities()),
          ScoreMode.Avg));
      return;
    }

    if (!query.impactSeverities().isEmpty() && query.impactSoftwareQualities().isEmpty()) {
      allFilters.addFilter(
        FIELD_ISSUE_IMPACT_SEVERITY,
        IMPACT_SEVERITY.getFilterScope(),
        nestedQuery(
          FIELD_ISSUE_IMPACTS,
          termsQuery(FIELD_ISSUE_IMPACT_SEVERITY, query.impactSeverities()),
          ScoreMode.Avg));
      return;
    }

    BoolQueryBuilder impactsFilter = boolQuery()
      .filter(termsQuery(FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY, query.impactSoftwareQualities()))
      .filter(termsQuery(FIELD_ISSUE_IMPACT_SEVERITY, query.impactSeverities()));

    allFilters.addFilter(FIELD_ISSUE_IMPACTS, new SimpleFieldFilterScope(FIELD_ISSUE_IMPACTS),
      nestedQuery(FIELD_ISSUE_IMPACTS, impactsFilter, ScoreMode.Avg));
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
    filters.addFilter(FIELD_ISSUE_COMPONENT_UUID, new SimpleFieldFilterScope(FIELD_ISSUE_COMPONENT_UUID),
      createTermsFilter(FIELD_ISSUE_COMPONENT_UUID, query.componentUuids()));

    if (!Boolean.TRUE.equals(query.onComponentOnly())) {
      filters.addFilter(
        FIELD_ISSUE_PROJECT_UUID, new SimpleFieldFilterScope(FIELD_ISSUE_PROJECT_UUID),
        createTermsFilter(FIELD_ISSUE_PROJECT_UUID, query.projectUuids()));
      filters.addFilter(
        FIELD_ISSUE_DIRECTORY_PATH, new SimpleFieldFilterScope(FIELD_ISSUE_DIRECTORY_PATH),
        createTermsFilter(FIELD_ISSUE_DIRECTORY_PATH, query.directories()));
      filters.addFilter(
        FIELD_ISSUE_FILE_PATH, new SimpleFieldFilterScope(FIELD_ISSUE_FILE_PATH),
        createTermsFilter(FIELD_ISSUE_FILE_PATH, query.files()));
    }
  }

  private static void addBranchComponentRelatedFilters(IssueQuery query, AllFilters allFilters) {
    if (Boolean.TRUE.equals(query.onComponentOnly())) {
      return;
    }
    if (query.isMainBranch() != null) {
      allFilters.addFilter(
        "__is_main_branch", new SimpleFieldFilterScope(FIELD_ISSUE_IS_MAIN_BRANCH),
        createTermFilter(FIELD_ISSUE_IS_MAIN_BRANCH, query.isMainBranch().toString()));
    }
    allFilters.addFilter(
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
      allFilters.addFilter("__view", new SimpleFieldFilterScope("view"), createViewFilter(singletonList(query.branchUuid())));
    } else {
      allFilters.addFilter("__view", new SimpleFieldFilterScope("view"), createViewFilter(viewUuids));
    }
  }

  @CheckForNull
  private static QueryBuilder createViewFilter(Collection<String> viewUuids) {
    if (viewUuids.isEmpty()) {
      return null;
    }

    BoolQueryBuilder viewsFilter = boolQuery();
    for (String viewUuid : viewUuids) {
      viewsFilter.should(QueryBuilders.termsLookupQuery(FIELD_ISSUE_BRANCH_UUID,
        new TermsLookup(
          TYPE_VIEW.getIndex().getName(),
          viewUuid,
          ViewIndexDefinition.FIELD_PROJECTS)));
    }
    return viewsFilter;
  }

  private static RequestFiltersComputer newFilterComputer(SearchOptions options, AllFilters allFilters) {
    Collection<String> facetNames = options.getFacets();
    Set<TopAggregationDefinition<?>> facets = Stream.concat(
      Stream.of(EFFORT_TOP_AGGREGATION),
      facetNames.stream()
        .map(FACETS_BY_NAME::get)
        .filter(Objects::nonNull)
        .map(Facet::getTopAggregationDef))
      .collect(Collectors.toSet());

    return new RequestFiltersComputer(allFilters, facets);
  }

  private static TopAggregationHelper newAggregationHelper(RequestFiltersComputer filterComputer, IssueQuery query) {
    if (hasQueryEffortFacet(query)) {
      return new TopAggregationHelper(filterComputer, new SubAggregationHelper(EFFORT_AGGREGATION, EFFORT_AGGREGATION_ORDER));
    }
    return new TopAggregationHelper(filterComputer, new SubAggregationHelper());
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

  /**
   * This method is for creating a filter that passes null to the elasticsearch query whenever empty or null collection is passed.
   * This means that filter will not filter anything, all the documents (issues) will be returned in this case.
   */
  @CheckForNull
  private static QueryBuilder createTermsFilter(String field, Collection<?> values) {
    return values.isEmpty() ? null : termsQuery(field, values);
  }

  /**
   * This method is for creating a filter that passes null to the elasticsearch query only when null collection is passed.
   * This ensures that whenever we pass empty collection to the filter, it will filter out all the documents (issues).
   */
  @CheckForNull
  private static QueryBuilder createTermsFilterForNullableCollection(String field, @Nullable Collection<?> values) {
    return values != null ? termsQuery(field, values) : null;
  }

  @CheckForNull
  private static QueryBuilder createTermFilter(String field, @Nullable String value) {
    return value == null ? null : termQuery(field, value);
  }

  @CheckForNull
  private static QueryBuilder createTermFilter(String field, @Nullable Boolean value) {
    return value == null ? null : termQuery(field, value);
  }

  private static QueryBuilder createPrefixFilter(String field, String value) {
    return prefixQuery(field, value);
  }

  private void configureSorting(IssueQuery query, SearchSourceBuilder esRequest) {
    createSortBuilders(query).forEach(esRequest::sort);
  }

  private List<FieldSortBuilder> createSortBuilders(IssueQuery query) {
    String sortField = query.sort();
    if (sortField != null) {
      boolean asc = Boolean.TRUE.equals(query.asc());
      return sorting.fill(sortField, asc);
    }
    return sorting.fillDefault();
  }

  private QueryBuilder createAuthorizationFilter() {
    return authorizationTypeSupport.createQueryFilter();
  }

  private void addDatesFilter(AllFilters filters, IssueQuery query) {
    PeriodStart createdAfter = query.createdAfter();
    Date createdBefore = query.createdBefore();

    validateCreationDateBounds(createdBefore, createdAfter != null ? createdAfter.date() : null);

    if (createdAfter != null) {
      filters.addFilter(
        "__createdAfter", CREATED_AT.getFilterScope(),
        QueryBuilders
          .rangeQuery(FIELD_ISSUE_FUNC_CREATED_AT)
          .from(createdAfter.date().getTime(), createdAfter.inclusive()));
    }
    if (createdBefore != null) {
      filters.addFilter(
        "__createdBefore", CREATED_AT.getFilterScope(),
        QueryBuilders
          .rangeQuery(FIELD_ISSUE_FUNC_CREATED_AT)
          .lt(createdBefore.getTime()));
    }
    Date createdAt = query.createdAt();
    if (createdAt != null) {
      filters.addFilter(
        "__createdAt", CREATED_AT.getFilterScope(),
        termQuery(FIELD_ISSUE_FUNC_CREATED_AT, createdAt.getTime()));
    }
  }

  private static void addNewCodeReferenceFilter(AllFilters filters, IssueQuery query) {
    Boolean newCodeOnReference = query.newCodeOnReference();

    if (newCodeOnReference != null) {
      filters.addFilter(
        FIELD_ISSUE_NEW_CODE_REFERENCE, new SimpleFieldFilterScope(FIELD_ISSUE_NEW_CODE_REFERENCE),
        termQuery(FIELD_ISSUE_NEW_CODE_REFERENCE, true));
    }
  }

  private static void addNewCodeReferenceFilterByProjectsFilter(AllFilters allFilters, IssueQuery query) {
    Collection<String> newCodeOnReferenceByProjectUuids = query.newCodeOnReferenceByProjectUuids();
    BoolQueryBuilder boolQueryBuilder = boolQuery();

    if (!newCodeOnReferenceByProjectUuids.isEmpty()) {

      newCodeOnReferenceByProjectUuids.forEach(projectOrProjectBranchUuid -> boolQueryBuilder.should(boolQuery()
        .filter(termQuery(FIELD_ISSUE_BRANCH_UUID, projectOrProjectBranchUuid))
        .filter(termQuery(FIELD_ISSUE_NEW_CODE_REFERENCE, true))));

      allFilters.addFilter("__is_new_code_reference_by_project_uuids",
        new SimpleFieldFilterScope("newCodeReferenceByProjectUuids"), boolQueryBuilder);
    }
  }

  private static void addCreatedAfterByProjectsFilter(AllFilters allFilters, IssueQuery query) {
    Map<String, PeriodStart> createdAfterByProjectUuids = query.createdAfterByProjectUuids();
    BoolQueryBuilder boolQueryBuilder = boolQuery();
    createdAfterByProjectUuids.forEach((projectOrProjectBranchUuid, createdAfterDate) -> boolQueryBuilder.should(boolQuery()
      .filter(termQuery(FIELD_ISSUE_BRANCH_UUID, projectOrProjectBranchUuid))
      .filter(rangeQuery(FIELD_ISSUE_FUNC_CREATED_AT).from(createdAfterDate.date().getTime(), createdAfterDate.inclusive()))));

    allFilters.addFilter("__created_after_by_project_uuids", new SimpleFieldFilterScope("createdAfterByProjectUuids"), boolQueryBuilder);
  }

  private void validateCreationDateBounds(@Nullable Date createdBefore, @Nullable Date createdAfter) {
    Preconditions.checkArgument(createdAfter == null || createdAfter.compareTo(new Date(system.now())) <= 0,
      "Start bound cannot be in the future");
    Preconditions.checkArgument(createdAfter == null || createdBefore == null || createdAfter.before(createdBefore),
      "Start bound cannot be larger or equal to end bound");
  }

  private void configureTopAggregations(TopAggregationHelper aggregationHelper, IssueQuery query, SearchOptions options,
    AllFilters queryFilters, SearchSourceBuilder esRequest) {
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
    addFacetIfNeeded(options, aggregationHelper, esRequest, CLEAN_CODE_ATTRIBUTE_CATEGORY, query.cleanCodeAttributesCategories().toArray());
    addFacetIfNeeded(options, aggregationHelper, esRequest, PRIORITIZED_RULE, ArrayUtils.EMPTY_OBJECT_ARRAY);

    addSecurityCategoryFacetIfNeeded(PARAM_PCI_DSS_32, PCI_DSS_32, options, aggregationHelper, esRequest, query.pciDss32().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_PCI_DSS_40, PCI_DSS_40, options, aggregationHelper, esRequest, query.pciDss40().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_OWASP_ASVS_40, OWASP_ASVS_40, options, aggregationHelper, esRequest, query.owaspAsvs40().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_OWASP_TOP_10, OWASP_TOP_10, options, aggregationHelper, esRequest, query.owaspTop10().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_OWASP_TOP_10_2021, OWASP_TOP_10_2021, options, aggregationHelper, esRequest, query.owaspTop10For2021().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_STIG_ASD_V5R3, STIG_ASD_V5R3, options, aggregationHelper, esRequest, query.stigAsdV5R3().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_CASA, CASA, options, aggregationHelper, esRequest, query.casa().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_SANS_TOP_25, SANS_TOP_25, options, aggregationHelper, esRequest, query.sansTop25().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_CWE, CWE, options, aggregationHelper, esRequest, query.cwe().toArray());
    addSecurityCategoryFacetIfNeeded(PARAM_SONARSOURCE_SECURITY, SONARSOURCE_SECURITY, options, aggregationHelper, esRequest, query.sonarsourceSecurity().toArray());

    if (options.getFacets().contains(PARAM_CVSS)) {

      RangeAggregationBuilder cvssRangeAgg = AggregationBuilders.range(PARAM_CVSS)
              .field(FIELD_ISSUE_CVSS)
              .addRange("0 - 1", 0.0, 1.0)
              .addRange("1 - 2", 1.0, 2.0)
              .addRange("2 - 3", 2.0, 3.0)
              .addRange("3 - 4", 3.0, 4.0)
              .addRange("4 - 5", 4.0, 5.0)
              .addRange("5 - 6", 5.0, 6.0)
              .addRange("6 - 7", 6.0, 7.0)
              .addRange("7 - 8", 7.0, 8.0)
              .addRange("8 - 9", 8.0, 9.0)
              .addRange("9 - 10", 9.0, 10.0);
      esRequest.aggregation(cvssRangeAgg);
    }

    addSeverityFacetIfNeeded(options, aggregationHelper, esRequest);
    addImpactSoftwareQualityFacetIfNeeded(options, query, aggregationHelper, esRequest);
    addImpactSeverityFacetIfNeeded(options, query, aggregationHelper, esRequest);
    addResolutionFacetIfNeeded(options, query, aggregationHelper, esRequest);
    addAssigneesFacetIfNeeded(options, query, aggregationHelper, esRequest);
    addCreatedAtFacetIfNeeded(options, query, aggregationHelper, queryFilters, esRequest);
    addAssignedToMeFacetIfNeeded(options, aggregationHelper, esRequest);
    addEffortTopAggregation(aggregationHelper, esRequest);
  }

  private static void addFacetIfNeeded(SearchOptions options, TopAggregationHelper aggregationHelper,
    SearchSourceBuilder esRequest, Facet facet, Object[] selectedValues) {
    if (!options.getFacets().contains(facet.getName())) {
      return;
    }

    FilterAggregationBuilder topAggregation = aggregationHelper.buildTermTopAggregation(
      facet.getName(), facet.getTopAggregationDef(), facet.getNumberOfTerms(),
      NO_EXTRA_FILTER,
      t -> aggregationHelper.getSubAggregationHelper().buildSelectedItemsAggregation(facet.getName(), facet.getTopAggregationDef(), selectedValues)
        .ifPresent(t::subAggregation));
    esRequest.aggregation(topAggregation);
  }

  private void addSecurityCategoryFacetIfNeeded(String param, Facet facet, SearchOptions options, TopAggregationHelper aggregationHelper,
    SearchSourceBuilder esRequest,
    Object[] selectedValues) {
    if (!options.getFacets().contains(param)) {
      return;
    }

    AggregationBuilder aggregation = aggregationHelper.buildTermTopAggregation(
      facet.getName(), facet.getTopAggregationDef(), facet.getNumberOfTerms(),
      filter -> filter.must(isMQRMode() ? SECURITY_IMPACT_FILTER : termQuery(FIELD_ISSUE_TYPE, VULNERABILITY.name())),
      t -> aggregationHelper.getSubAggregationHelper().buildSelectedItemsAggregation(facet.getName(), facet.getTopAggregationDef(), selectedValues)
        .ifPresent(t::subAggregation));
    esRequest.aggregation(aggregation);
  }

  private static void addSeverityFacetIfNeeded(SearchOptions options, TopAggregationHelper aggregationHelper, SearchSourceBuilder esRequest) {
    if (!options.getFacets().contains(PARAM_SEVERITIES)) {
      return;
    }

    AggregationBuilder aggregation = aggregationHelper.buildTermTopAggregation(
      SEVERITIES.getName(), SEVERITIES.getTopAggregationDef(), SEVERITIES.getNumberOfTerms(),
      // Ignore severity of Security HotSpots
      filter -> filter.mustNot(termQuery(FIELD_ISSUE_TYPE, SECURITY_HOTSPOT.name())),
      NO_OTHER_SUBAGGREGATION);
    esRequest.aggregation(aggregation);
  }

  private static void addImpactSoftwareQualityFacetIfNeeded(SearchOptions options, IssueQuery query, TopAggregationHelper aggregationHelper,
    SearchSourceBuilder esRequest) {
    if (!options.getFacets().contains(PARAM_IMPACT_SOFTWARE_QUALITIES)) {
      return;
    }

    Function<SoftwareQuality, BoolQueryBuilder> mainQuery = softwareQuality -> boolQuery()
      .filter(termQuery(FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY, softwareQuality.name()));

    FiltersAggregator.KeyedFilter[] keyedFilters = Arrays.stream(SoftwareQuality.values())
      .map(softwareQuality -> new FiltersAggregator.KeyedFilter(softwareQuality.name(),
        query.impactSeverities().isEmpty() ? mainQuery.apply(softwareQuality)
          : mainQuery.apply(softwareQuality)
            .filter(termsQuery(FIELD_ISSUE_IMPACT_SEVERITY, query.impactSeverities()))))
      .toArray(FiltersAggregator.KeyedFilter[]::new);

    AggregationBuilder aggregation = aggregationHelper.buildTopAggregation(
      IMPACT_SOFTWARE_QUALITY.getName(), IMPACT_SOFTWARE_QUALITY.getTopAggregationDef(),
      NO_EXTRA_FILTER,
      t -> t.subAggregation(AggregationBuilders.nested("nested_" + IMPACT_SOFTWARE_QUALITY.getName(), FIELD_ISSUE_IMPACTS)
        .subAggregation(filters(IMPACT_SOFTWARE_QUALITY.getName(), keyedFilters))));

    esRequest.aggregation(aggregation);
  }

  private static void addImpactSeverityFacetIfNeeded(SearchOptions options, IssueQuery query, TopAggregationHelper aggregationHelper, SearchSourceBuilder esRequest) {
    if (!options.getFacets().contains(PARAM_IMPACT_SEVERITIES)) {
      return;
    }

    Function<org.sonar.api.issue.impact.Severity, BoolQueryBuilder> mainQuery = softwareQuality -> boolQuery()
      .filter(termQuery(FIELD_ISSUE_IMPACT_SEVERITY, softwareQuality.name()));

    FiltersAggregator.KeyedFilter[] keyedFilters = Arrays.stream(org.sonar.api.issue.impact.Severity.values())
      .map(severity -> new FiltersAggregator.KeyedFilter(severity.name(),
        query.impactSoftwareQualities().isEmpty() ? mainQuery.apply(severity)
          : mainQuery.apply(severity)
            .filter(termsQuery(FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY, query.impactSoftwareQualities()))))
      .toArray(FiltersAggregator.KeyedFilter[]::new);

    AggregationBuilder aggregation = aggregationHelper.buildTopAggregation(
      IMPACT_SEVERITY.getName(), IMPACT_SEVERITY.getTopAggregationDef(),
      NO_EXTRA_FILTER,
      t -> t.subAggregation(AggregationBuilders.nested("nested_" + IMPACT_SEVERITY.getName(), FIELD_ISSUE_IMPACTS)
        .subAggregation(filters(IMPACT_SEVERITY.getName(), keyedFilters)
          // we want to count the number of issues for each severity, so we need to reverse the nested aggregation
          .subAggregation(reverseNested("reverse_nested_" + IMPACT_SOFTWARE_QUALITY.getName())))));
    esRequest.aggregation(aggregation);
  }

  private static void addResolutionFacetIfNeeded(SearchOptions options, IssueQuery query, TopAggregationHelper aggregationHelper, SearchSourceBuilder esRequest) {
    if (!options.getFacets().contains(PARAM_RESOLUTIONS)) {
      return;
    }

    AggregationBuilder aggregation = aggregationHelper.buildTermTopAggregation(
      RESOLUTIONS.getName(), RESOLUTIONS.getTopAggregationDef(), RESOLUTIONS.getNumberOfTerms(),
      NO_EXTRA_FILTER,
      t ->
      // add aggregation of type "missing" to return count of unresolved issues in the facet
      t.subAggregation(
        addEffortAggregationIfNeeded(query, AggregationBuilders
          .missing(RESOLUTIONS.getName() + FACET_SUFFIX_MISSING)
          .field(RESOLUTIONS.getFieldName()))));
    esRequest.aggregation(aggregation);
  }

  private static void addAssigneesFacetIfNeeded(SearchOptions options, IssueQuery query, TopAggregationHelper aggregationHelper, SearchSourceBuilder esRequest) {
    if (!options.getFacets().contains(PARAM_ASSIGNEES)) {
      return;
    }

    Consumer<FilterAggregationBuilder> assigneeAggregations = t -> {
      // optional second aggregation to return the issue count for selected assignees (if any)
      Object[] assignees = query.assignees().toArray();
      aggregationHelper.getSubAggregationHelper().buildSelectedItemsAggregation(ASSIGNEES.getName(), ASSIGNEES.getTopAggregationDef(), assignees)
        .ifPresent(t::subAggregation);

      // third aggregation to always return the count of unassigned in the assignee facet
      t.subAggregation(addEffortAggregationIfNeeded(query, AggregationBuilders
        .missing(ASSIGNEES.getName() + FACET_SUFFIX_MISSING)
        .field(ASSIGNEES.getFieldName())));
    };

    AggregationBuilder aggregation = aggregationHelper.buildTermTopAggregation(
      ASSIGNEES.getName(), ASSIGNEES.getTopAggregationDef(), ASSIGNEES.getNumberOfTerms(),
      NO_EXTRA_FILTER, assigneeAggregations);
    esRequest.aggregation(aggregation);
  }

  private void addCreatedAtFacetIfNeeded(SearchOptions options, IssueQuery query, TopAggregationHelper aggregationHelper, AllFilters allFilters,
    SearchSourceBuilder esRequest) {
    if (options.getFacets().contains(PARAM_CREATED_AT)) {
      getCreatedAtFacet(query, aggregationHelper, allFilters).ifPresent(esRequest::aggregation);
    }
  }

  private Optional<AggregationBuilder> getCreatedAtFacet(IssueQuery query, TopAggregationHelper aggregationHelper, AllFilters allFilters) {
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
    DateHistogramInterval bucketSize = computeDateHistogramBucketSize(timeSpan);

    FilterAggregationBuilder topAggregation = aggregationHelper.buildTopAggregation(
      CREATED_AT.getName(),
      CREATED_AT.getTopAggregationDef(),
      NO_EXTRA_FILTER,
      t -> {
        AggregationBuilder dateHistogram = AggregationBuilders.dateHistogram(CREATED_AT.getName())
          .field(CREATED_AT.getFieldName())
          .calendarInterval(bucketSize)
          .minDocCount(0L)
          .format(DateUtils.DATETIME_FORMAT)
          .timeZone(Optional.ofNullable(query.timeZone()).orElse(system.getDefaultTimeZone().toZoneId()))
          // ES dateHistogram bounds are inclusive while createdBefore parameter is exclusive
          .extendedBounds(new LongBounds(startInclusive ? startTime : (startTime + 1), endTime - 1L));
        addEffortAggregationIfNeeded(query, dateHistogram);
        t.subAggregation(dateHistogram);
      });

    return Optional.of(topAggregation);
  }

  private static DateHistogramInterval computeDateHistogramBucketSize(Duration timeSpan) {
    if (timeSpan.isShorterThan(TWENTY_DAYS)) {
      return DateHistogramInterval.DAY;
    }
    if (timeSpan.isShorterThan(TWENTY_WEEKS)) {
      return DateHistogramInterval.WEEK;
    }
    if (timeSpan.isShorterThan(TWENTY_MONTHS)) {
      return DateHistogramInterval.MONTH;
    }
    return DateHistogramInterval.YEAR;
  }

  private OptionalLong getMinCreatedAt(AllFilters filters) {
    String facetNameAndField = CREATED_AT.getFieldName();

    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
      .size(0);
    BoolQueryBuilder esFilter = boolQuery();
    filters.stream().filter(Objects::nonNull).forEach(esFilter::must);
    if (esFilter.hasClauses()) {
      sourceBuilder.query(QueryBuilders.boolQuery().filter(esFilter));
    }
    sourceBuilder.aggregation(AggregationBuilders.min(facetNameAndField).field(facetNameAndField));

    SearchRequest request = EsClient.prepareSearch(TYPE_ISSUE.getMainType())
      .source(sourceBuilder);

    Min minValue = client.search(request).getAggregations().get(facetNameAndField);
    double actualValue = minValue.getValue();
    if (Double.isInfinite(actualValue)) {
      return OptionalLong.empty();
    }
    return OptionalLong.of((long) actualValue);
  }

  private void addAssignedToMeFacetIfNeeded(SearchOptions options, TopAggregationHelper aggregationHelper, SearchSourceBuilder esRequest) {
    String uuid = userSession.getUuid();
    if (options.getFacets().contains(ASSIGNED_TO_ME.getName()) && !StringUtils.isEmpty(uuid)) {
      AggregationBuilder aggregation = aggregationHelper.buildTopAggregation(
        ASSIGNED_TO_ME.getName(),
        ASSIGNED_TO_ME.getTopAggregationDef(),
        NO_EXTRA_FILTER,
        t ->
        // add sub-aggregation to return issue count for current user
        aggregationHelper.getSubAggregationHelper()
          .buildSelectedItemsAggregation(ASSIGNED_TO_ME.getName(), ASSIGNED_TO_ME.getTopAggregationDef(), new String[] {uuid})
          .ifPresent(t::subAggregation));
      esRequest.aggregation(aggregation);
    }
  }

  private static void addEffortTopAggregation(TopAggregationHelper aggregationHelper, SearchSourceBuilder esRequest) {
    AggregationBuilder topAggregation = aggregationHelper.buildTopAggregation(
      FACET_MODE_EFFORT,
      EFFORT_TOP_AGGREGATION,
      NO_EXTRA_FILTER,
      t -> t.subAggregation(EFFORT_AGGREGATION));
    esRequest.aggregation(topAggregation);
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
    SearchRequest requestBuilder = EsClient.prepareSearch(TYPE_ISSUE.getMainType());

    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
      // Avoids returning search hits
      .size(0);
    requestBuilder.source(sourceBuilder);

    sourceBuilder.query(boolQuery().must(QueryBuilders.matchAllQuery()).filter(createBoolFilter(query)));

    TermsAggregationBuilder aggreg = AggregationBuilders.terms("_ref")
      .field(fieldName)
      .size(size)
      .order(termsOrder)
      .minDocCount(1L);
    if (textQuery != null) {
      aggreg.includeExclude(new IncludeExclude(format(SUBSTRING_MATCH_REGEXP, escapeSpecialRegexChars(textQuery)), null));
    }

    sourceBuilder.aggregation(aggreg);

    SearchResponse searchResponse = client.search(requestBuilder);
    return searchResponse.getAggregations().get("_ref");
  }

  private BoolQueryBuilder createBoolFilter(IssueQuery query) {
    BoolQueryBuilder boolQuery = boolQuery();
    createAllFilters(query).stream()
      .filter(Objects::nonNull)
      .forEach(boolQuery::must);
    return boolQuery;
  }

  public List<ProjectStatistics> searchProjectStatistics(List<String> projectUuids, List<Long> froms, @Nullable String assigneeUuid) {
    checkState(projectUuids.size() == froms.size(),
      "Expected same size for projectUuids (had size %s) and froms (had size %s)", projectUuids.size(), froms.size());
    if (projectUuids.isEmpty()) {
      return Collections.emptyList();
    }
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
      .query(
        boolQuery()
          .mustNot(existsQuery(FIELD_ISSUE_RESOLUTION))
          .filter(termQuery(FIELD_ISSUE_ASSIGNEE_UUID, assigneeUuid))
          .mustNot(termQuery(FIELD_ISSUE_TYPE, SECURITY_HOTSPOT.name())))
      .size(0);

    IntStream.range(0, projectUuids.size()).forEach(i -> {
      String projectUuid = projectUuids.get(i);
      long from = froms.get(i);
      sourceBuilder
        .aggregation(AggregationBuilders
          .filter(projectUuid, boolQuery()
            .filter(termQuery(FIELD_ISSUE_PROJECT_UUID, projectUuid))
            .filter(rangeQuery(FIELD_ISSUE_FUNC_CREATED_AT).gte(from)))
          .subAggregation(
            AggregationBuilders.terms("branchUuid").field(FIELD_ISSUE_BRANCH_UUID)
              .subAggregation(
                AggregationBuilders.count(AGG_COUNT).field(FIELD_ISSUE_KEY))
              .subAggregation(
                AggregationBuilders.max("maxFuncCreatedAt").field(FIELD_ISSUE_FUNC_CREATED_AT))));
    });

    SearchRequest requestBuilder = EsClient.prepareSearch(TYPE_ISSUE.getMainType());

    requestBuilder.source(sourceBuilder);
    SearchResponse response = client.search(requestBuilder);
    return response.getAggregations().asList().stream()
      .map(ParsedFilter.class::cast)
      .flatMap(projectBucket -> ((ParsedStringTerms) projectBucket.getAggregations().get("branchUuid")).getBuckets().stream()
        .flatMap(branchBucket -> {
          long count = ((ParsedValueCount) branchBucket.getAggregations().get(AGG_COUNT)).getValue();
          if (count < 1L) {
            return Stream.empty();
          }
          long lastIssueDate = (long) ((ParsedMax) branchBucket.getAggregations().get("maxFuncCreatedAt")).getValue();
          return Stream.of(new ProjectStatistics(branchBucket.getKeyAsString(), count, lastIssueDate));
        }))
      .toList();
  }

  public List<PrStatistics> searchBranchStatistics(String projectUuid, List<String> branchUuids) {
    if (branchUuids.isEmpty()) {
      return Collections.emptyList();
    }

    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
        .query(
            boolQuery()
                .must(termsQuery(FIELD_ISSUE_BRANCH_UUID, branchUuids))
                .mustNot(existsQuery(FIELD_ISSUE_RESOLUTION))
                .must(termQuery(FIELD_ISSUE_IS_MAIN_BRANCH, Boolean.toString(false))))
        .size(0)
        .aggregation(AggregationBuilders.terms("branchUuids")
            .field(FIELD_ISSUE_BRANCH_UUID)
            .size(branchUuids.size())
            .subAggregation(AggregationBuilders.terms("types")
                .field(FIELD_ISSUE_TYPE)));

    SearchRequest requestBuilder = EsClient.prepareSearch(TYPE_ISSUE.getMainType())
        .routing(AuthorizationDoc.idOf(projectUuid));

    requestBuilder.source(sourceBuilder);
    SearchResponse response = client.search(requestBuilder);
    return ((ParsedStringTerms) response.getAggregations().get("branchUuids")).getBuckets().stream()
        .map(bucket -> new PrStatistics(bucket.getKeyAsString(),
            ((ParsedStringTerms) bucket.getAggregations().get("types")).getBuckets()
                .stream()
                .collect(Collectors.toMap(MultiBucketsAggregation.Bucket::getKeyAsString, MultiBucketsAggregation.Bucket::getDocCount))))
        .toList();
  }

  public List<SecurityStandardCategoryStatistics> getCweTop25Reports(String projectUuid, boolean isViewOrApp) {
    SearchSourceBuilder request = prepareNonClosedVulnerabilitiesAndHotspotSearch(projectUuid, isViewOrApp);
    CWES_BY_CWE_TOP_25.keySet()
      .forEach(cweYear -> request.aggregation(
        newSecurityReportSubAggregations(
          AggregationBuilders.filter(cweYear, boolQuery().filter(existsQuery(FIELD_ISSUE_CWE))),
          true,
          CWES_BY_CWE_TOP_25.get(cweYear))));
    List<SecurityStandardCategoryStatistics> result = search(request, true, null);
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

  private static SecurityStandardCategoryStatistics emptyCweStatistics(String rule) {
    return new SecurityStandardCategoryStatistics(rule, 0, OptionalInt.of(1), 0, 0, 1, null, null);
  }

  public List<SecurityStandardCategoryStatistics> getSonarSourceReport(String projectUuid, boolean isViewOrApp, boolean includeCwe) {
    SearchSourceBuilder request = prepareNonClosedVulnerabilitiesAndHotspotSearch(projectUuid, isViewOrApp);
    Arrays.stream(SQCategory.values())
      .forEach(sonarsourceCategory -> request.aggregation(
        newSecurityReportSubAggregations(
          AggregationBuilders.filter(sonarsourceCategory.getKey(), boolQuery().filter(termQuery(FIELD_ISSUE_SQ_SECURITY_CATEGORY, sonarsourceCategory.getKey()))),
          includeCwe,
          SecurityStandards.CWES_BY_SQ_CATEGORY.get(sonarsourceCategory))));
    return search(request, includeCwe, null);
  }

  public List<SecurityStandardCategoryStatistics> getPciDssReport(String projectUuid, boolean isViewOrApp, PciDssVersion version) {
    SearchSourceBuilder request = prepareNonClosedVulnerabilitiesAndHotspotSearch(projectUuid, isViewOrApp);
    Arrays.stream(PciDss.values())
      .forEach(pciDss -> request.aggregation(
        newSecurityReportSubAggregations(
          AggregationBuilders.filter(pciDss.category(), boolQuery().filter(prefixQuery(version.prefix(), pciDss.category() + "."))), version.prefix())));
    return searchWithDistribution(request, version.label(), null);
  }

  public List<SecurityStandardCategoryStatistics> getOwaspAsvsReport(String projectUuid, boolean isViewOrApp, RulesDefinition.OwaspAsvsVersion version, Integer level) {
    SearchSourceBuilder request = prepareNonClosedVulnerabilitiesAndHotspotSearch(projectUuid, isViewOrApp);
    Arrays.stream(SecurityStandards.OwaspAsvs.values())
      .forEach(owaspAsvs -> request.aggregation(
        newSecurityReportSubAggregations(
          AggregationBuilders.filter(
            owaspAsvs.category(),
            boolQuery().filter(termsQuery(version.prefix(), SecurityStandards.getRequirementsForCategoryAndLevel(owaspAsvs, level)))),
          version.prefix())));
    return searchWithDistribution(request, version.label(), level);
  }

  public List<SecurityStandardCategoryStatistics> getOwaspAsvsReportGroupedByLevel(String projectUuid, boolean isViewOrApp, RulesDefinition.OwaspAsvsVersion version, int level) {
    SearchSourceBuilder request = prepareNonClosedVulnerabilitiesAndHotspotSearch(projectUuid, isViewOrApp);
    request.aggregation(
      newSecurityReportSubAggregations(
        AggregationBuilders.filter(
          "l" + level,
          boolQuery().filter(termsQuery(version.prefix(), SecurityStandards.OWASP_ASVS_REQUIREMENTS_BY_LEVEL.get(version).get(level)))),
        version.prefix()));
    return searchWithLevelDistribution(request, version.label(), Integer.toString(level));
  }

  public List<SecurityStandardCategoryStatistics> getOwaspTop10Report(String projectUuid, boolean isViewOrApp, boolean includeCwe, OwaspTop10Version version) {
    SearchSourceBuilder request = prepareNonClosedVulnerabilitiesAndHotspotSearch(projectUuid, isViewOrApp);
    IntStream.rangeClosed(1, 10).mapToObj(i -> "a" + i)
      .forEach(owaspCategory -> request.aggregation(
        newSecurityReportSubAggregations(
          AggregationBuilders.filter(owaspCategory, boolQuery().filter(termQuery(version.prefix(), owaspCategory))),
          includeCwe,
          null)));
    return search(request, includeCwe, version.label());
  }

  public List<SecurityStandardCategoryStatistics> getStigReport(String projectUuid, boolean isViewOrApp, RulesDefinition.StigVersion stigVersion) {
    SearchSourceBuilder request = prepareNonClosedVulnerabilitiesAndHotspotSearch(projectUuid, isViewOrApp);
    Arrays.stream(StigSupportedRequirement.values())
      .forEach(stigSupportedRequirement -> request.aggregation(
        newSecurityReportSubAggregations(
          AggregationBuilders.filter(stigSupportedRequirement.getRequirement(), boolQuery().filter(termQuery(stigVersion.prefix(), stigSupportedRequirement.getRequirement()))),
          false, null)));
    return search(request, false, stigVersion.label());
  }

  public List<SecurityStandardCategoryStatistics> getCasaReport(String projectUuid, boolean isViewOrApp) {
    SearchSourceBuilder request = prepareNonClosedVulnerabilitiesAndHotspotSearch(projectUuid, isViewOrApp);
    IntStream.range(1, 15)
      .forEach(casaTopCategory -> request.aggregation(
        newSecurityReportSubAggregations(
          AggregationBuilders.filter(String.valueOf(casaTopCategory), boolQuery().filter(prefixQuery(FIELD_ISSUE_CASA, casaTopCategory + "."))), FIELD_ISSUE_CASA)));
    return searchWithDistribution(request, null, null);
  }

  private List<SecurityStandardCategoryStatistics> searchWithLevelDistribution(SearchSourceBuilder sourceBuilder, String version, @Nullable String level) {
    return getSearchResponse(sourceBuilder)
      .getAggregations().asList().stream()
      .map(c -> processSecurityReportIssueSearchResultsWithLevelDistribution((ParsedFilter) c, version, level))
      .toList();
  }

  private List<SecurityStandardCategoryStatistics> searchWithDistribution(SearchSourceBuilder sourceBuilder, @Nullable String version, @Nullable Integer level) {
    return getSearchResponse(sourceBuilder)
      .getAggregations().asList().stream()
      .map(c -> processSecurityReportIssueSearchResultsWithDistribution((ParsedFilter) c, version, level))
      .toList();
  }

  private List<SecurityStandardCategoryStatistics> search(SearchSourceBuilder sourceBuilder, boolean includeDistribution, @Nullable String version) {
    return getSearchResponse(sourceBuilder)
      .getAggregations().asList().stream()
      .map(c -> processSecurityReportIssueSearchResults((ParsedFilter) c, includeDistribution, version))
      .toList();
  }

  private SearchResponse getSearchResponse(SearchSourceBuilder sourceBuilder) {
    SearchRequest request = EsClient.prepareSearch(TYPE_ISSUE.getMainType())
      .source(sourceBuilder);
    return client.search(request);
  }

  private SecurityStandardCategoryStatistics processSecurityReportIssueSearchResultsWithDistribution(ParsedFilter categoryFilter,
    @Nullable String version,
    @Nullable Integer level) {
    var list = ((ParsedStringTerms) categoryFilter.getAggregations().get(AGG_DISTRIBUTION)).getBuckets();
    List<SecurityStandardCategoryStatistics> children = list.stream()
      .filter(categoryBucket -> StringUtils.startsWith(categoryBucket.getKeyAsString(), categoryFilter.getName() + "."))
      .filter(categoryBucket -> level == null || OWASP_ASVS_40_REQUIREMENTS_BY_LEVEL.get(level).contains(categoryBucket.getKeyAsString()))
      .map(categoryBucket -> processSecurityReportCategorySearchResults(categoryBucket, categoryBucket.getKeyAsString(), null, null))
      .toList();

    return processSecurityReportCategorySearchResults(categoryFilter, categoryFilter.getName(), children, version);
  }

  private SecurityStandardCategoryStatistics processSecurityReportIssueSearchResultsWithLevelDistribution(ParsedFilter categoryFilter,
    String version, String level) {
    var list = ((ParsedStringTerms) categoryFilter.getAggregations().get(AGG_DISTRIBUTION)).getBuckets();
    List<SecurityStandardCategoryStatistics> children = list.stream()
      .filter(categoryBucket -> OWASP_ASVS_40_REQUIREMENTS_BY_LEVEL.get(Integer.parseInt(level)).contains(categoryBucket.getKeyAsString()))
      .map(categoryBucket -> processSecurityReportCategorySearchResults(categoryBucket, categoryBucket.getKeyAsString(), null, null))
      .toList();

    return processSecurityReportCategorySearchResults(categoryFilter, categoryFilter.getName(), children, version);
  }

  private SecurityStandardCategoryStatistics processSecurityReportIssueSearchResults(ParsedFilter categoryBucket,
    boolean includeDistribution, String version) {
    List<SecurityStandardCategoryStatistics> children = new ArrayList<>();
    if (includeDistribution) {
      Stream<? extends Terms.Bucket> stream = ((ParsedStringTerms) categoryBucket.getAggregations().get(AGG_DISTRIBUTION)).getBuckets().stream();
      children = stream.map(cweBucket -> processSecurityReportCategorySearchResults(cweBucket, cweBucket.getKeyAsString(), null, null))
        .collect(toCollection(ArrayList<SecurityStandardCategoryStatistics>::new));
    }

    return processSecurityReportCategorySearchResults(categoryBucket, categoryBucket.getName(), children, version);
  }

  private SecurityStandardCategoryStatistics processSecurityReportCategorySearchResults(HasAggregations categoryBucket, String categoryName,
    @Nullable List<SecurityStandardCategoryStatistics> children, @Nullable String version) {

    Aggregation severitiesAggregations =
      ((ParsedFilter) categoryBucket.getAggregations().get(AGG_VULNERABILITIES)).getAggregations().get(AGG_SEVERITIES);

    CountAndRating countAndRating = getCountAndRating(severitiesAggregations);
    long vulnerabilities = countAndRating.getCount();
    // Worst severity having at least one issue
    OptionalInt severityRating = countAndRating.getRating();

    long toReviewSecurityHotspots = ((ParsedValueCount) ((ParsedFilter) categoryBucket.getAggregations().get(AGG_TO_REVIEW_SECURITY_HOTSPOTS)).getAggregations().get(AGG_COUNT))
      .getValue();
    long reviewedSecurityHotspots = ((ParsedValueCount) ((ParsedFilter) categoryBucket.getAggregations().get(AGG_REVIEWED_SECURITY_HOTSPOTS)).getAggregations().get(AGG_COUNT))
      .getValue();

    Optional<Double> percent = computePercent(toReviewSecurityHotspots, reviewedSecurityHotspots);
    Integer securityReviewRating = computeRating(percent.orElse(null)).getIndex();

    return new SecurityStandardCategoryStatistics(categoryName, vulnerabilities, severityRating, toReviewSecurityHotspots,
      reviewedSecurityHotspots, securityReviewRating, children, version);
  }

  private CountAndRating getCountAndRating(Aggregation severitiesAggregations) {
    if (isMQRMode()) {
      List<? extends Terms.Bucket> severityBuckets =
        ((ParsedStringTerms) ((ParsedFilter) ((ParsedNested) severitiesAggregations).getAggregations().get(ISSUES_WITH_SECURITY_IMPACT)).getAggregations().get(AGG_IMPACT_SEVERITIES)).getBuckets();
      long vulnerabilities =
        severityBuckets.stream().mapToLong(b -> ((ParsedValueCount) b.getAggregations().get(AGG_COUNT)).getValue()).sum();
      // Worst severity having at least one issue
      OptionalInt severityRating = severityBuckets.stream()
        .filter(b -> ((ParsedValueCount) b.getAggregations().get(AGG_COUNT)).getValue() != 0)
        .mapToInt(b -> org.sonar.api.issue.impact.Severity.valueOf(b.getKeyAsString()).ordinal() + 1)
        .max();
      return new CountAndRating(vulnerabilities, severityRating);
    } else {
      List<? extends Terms.Bucket> severityBuckets = ((ParsedStringTerms) severitiesAggregations).getBuckets();
      long vulnerabilities =
        severityBuckets.stream().mapToLong(b -> ((ParsedValueCount) b.getAggregations().get(AGG_COUNT)).getValue()).sum();
      // Worst severity having at least one issue
      OptionalInt severityRating = severityBuckets.stream()
        .filter(b -> ((ParsedValueCount) b.getAggregations().get(AGG_COUNT)).getValue() != 0)
        .mapToInt(b -> Severity.ALL.indexOf(b.getKeyAsString()) + 1)
        .max();
      return new CountAndRating(vulnerabilities, severityRating);
    }
  }

  private AggregationBuilder newSecurityReportSubAggregations(AggregationBuilder categoriesAggs, String securityStandardVersionPrefix) {
    AggregationBuilder aggregationBuilder = addSecurityReportIssueCountAggregations(categoriesAggs);
    final TermsAggregationBuilder distributionAggregation = AggregationBuilders.terms(AGG_DISTRIBUTION)
      .field(securityStandardVersionPrefix)
      // 100 should be enough to display all the requirements per category. If not, the UI will be broken anyway
      .size(MAX_FACET_SIZE);
    categoriesAggs.subAggregation(addSecurityReportIssueCountAggregations(distributionAggregation));

    return aggregationBuilder;
  }

  private AggregationBuilder newSecurityReportSubAggregations(AggregationBuilder categoriesAggs, boolean includeCwe,
    @Nullable Collection<String> cwesInCategory) {
    AggregationBuilder aggregationBuilder = addSecurityReportIssueCountAggregations(categoriesAggs);
    if (includeCwe) {
      final TermsAggregationBuilder cwesAgg = AggregationBuilders.terms(AGG_DISTRIBUTION)
        .field(FIELD_ISSUE_CWE)
        // 100 should be enough to display all CWEs. If not, the UI will be broken anyway
        .size(MAX_FACET_SIZE);
      if (cwesInCategory != null) {
        cwesAgg.includeExclude(new IncludeExclude(cwesInCategory.toArray(new String[0]), new String[0]));
      }
      categoriesAggs.subAggregation(addSecurityReportIssueCountAggregations(cwesAgg));
    }
    return aggregationBuilder;
  }

  private AggregationBuilder addSecurityReportIssueCountAggregations(AggregationBuilder categoryAggs) {
    return categoryAggs
      .subAggregation(
        AggregationBuilders.filter(AGG_VULNERABILITIES, getNonResolvedIssuesOrNonResolvedSecurityImpactQueryBuilderBasedOnMode())
          .subAggregation(getAggregationBuilderBasedOnMode()))
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

  private AggregationBuilder getAggregationBuilderBasedOnMode() {
    if (isMQRMode()) {
      return AggregationBuilders.nested(AGG_SEVERITIES, FIELD_ISSUE_IMPACTS)
        .subAggregation(AggregationBuilders.filter(ISSUES_WITH_SECURITY_IMPACT,
            boolQuery().filter(termsQuery(FIELD_ISSUE_IMPACT_SOFTWARE_QUALITY, SoftwareQuality.SECURITY.name())))
        .subAggregation(AggregationBuilders.terms(AGG_IMPACT_SEVERITIES).field(FIELD_ISSUE_IMPACT_SEVERITY)
          .subAggregation(
            AggregationBuilders.count(AGG_COUNT).field(FIELD_ISSUE_IMPACT_SEVERITY))));

    } else {
      return AggregationBuilders.terms(AGG_SEVERITIES).field(FIELD_ISSUE_SEVERITY)
        .subAggregation(
          AggregationBuilders.count(AGG_COUNT).field(FIELD_ISSUE_KEY));

    }
  }
  private QueryBuilder getNonResolvedIssuesOrNonResolvedSecurityImpactQueryBuilderBasedOnMode() {
    return isMQRMode() ? NON_RESOLVED_SECURITY_IMPACT_FILTER : NON_RESOLVED_VULNERABILITIES_FILTER;
  }

  private SearchSourceBuilder prepareNonClosedVulnerabilitiesAndHotspotSearch(String projectUuid, boolean isViewOrApp) {
    BoolQueryBuilder componentFilter = boolQuery();
    if (isViewOrApp) {
      componentFilter.filter(QueryBuilders.termsLookupQuery(FIELD_ISSUE_BRANCH_UUID,
        new TermsLookup(
          TYPE_VIEW.getIndex().getName(),
          projectUuid,
          ViewIndexDefinition.FIELD_PROJECTS)));
    } else {
      componentFilter.filter(termQuery(FIELD_ISSUE_BRANCH_UUID, projectUuid));
    }

    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    return sourceBuilder
      .query(
        componentFilter
          .should(getNonResolvedIssuesOrNonResolvedSecurityImpactQueryBuilderBasedOnMode())
          .should(TO_REVIEW_HOTSPOTS_FILTER)
          .should(IN_REVIEW_HOTSPOTS_FILTER)
          .should(REVIEWED_HOTSPOTS_FILTER)
          .minimumShouldMatch(1))
      .size(0);
  }

  private boolean isMQRMode() {
    return config.getBoolean(MULTI_QUALITY_MODE_ENABLED).orElse(MULTI_QUALITY_MODE_DEFAULT_VALUE);
  }


  private static class CountAndRating {
    private long count;
    private OptionalInt rating;

    public CountAndRating(long count, OptionalInt rating) {
      this.count = count;
      this.rating = rating;
    }

    public long getCount() {
      return count;
    }

    public OptionalInt getRating() {
      return rating;
    }
  }
}
