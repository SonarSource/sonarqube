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
package org.sonar.server.measure.index;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.NestedAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.util.NamedValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.server.es.ES8QueryHelper;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.newindex.DefaultIndexSettingsElement;
import org.sonar.server.es.searchrequest.NestedFieldTopAggregationDefinition;
import org.sonar.server.es.searchrequest.RequestFiltersComputer;
import org.sonar.server.es.searchrequest.RequestFiltersComputer.AllFilters;
import org.sonar.server.es.searchrequest.SimpleFieldTopAggregationDefinition;
import org.sonar.server.es.searchrequest.SubAggregationHelper;
import org.sonar.server.es.searchrequest.TopAggregationDefinition;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.NestedFieldFilterScope;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.SimpleFieldFilterScope;
import org.sonar.server.es.searchrequest.TopAggregationHelper;
import org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;
import static org.sonar.server.es.EsUtils.escapeSpecialRegexChars;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REVIEW_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REVIEW_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING_KEY;
import static org.sonar.core.metric.ScaMetrics.NEW_SCA_RATING_ANY_ISSUE_KEY;
import static org.sonar.core.metric.ScaMetrics.SCA_RATING_ANY_ISSUE_KEY;
import static org.sonar.server.es.IndexType.FIELD_INDEX_TYPE;
import static org.sonar.server.es.SearchOptions.MAX_PAGE_SIZE;
import static org.sonar.server.es.searchrequest.TopAggregationDefinition.STICKY;
import static org.sonar.server.measure.index.ProjectMeasuresDoc.QUALITY_GATE_STATUS;
import static org.sonar.server.measure.index.ProjectMeasuresIndex.Facet.ALERT_STATUS;
import static org.sonar.server.measure.index.ProjectMeasuresIndex.Facet.LANGUAGES;
import static org.sonar.server.measure.index.ProjectMeasuresIndex.Facet.TAGS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.AGGREGATION_PROJECTS_NOT_ANALYZED;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_ANALYSED_AT;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_CREATED_AT;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_LANGUAGES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_MEASURES_MEASURE_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_MEASURES_MEASURE_VALUE;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_NAME;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_NCLOC_DISTRIBUTION;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_NCLOC_DISTRIBUTION_LANGUAGE;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_NCLOC_DISTRIBUTION_NCLOC;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_QUALIFIER;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_QUALITY_GATE_STATUS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.FIELD_TAGS;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.SUB_FIELD_MEASURES_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresIndexDefinition.TYPE_PROJECT_MEASURES;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.SORT_BY_CREATION_DATE;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.SORT_BY_LAST_ANALYSIS_DATE;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.SORT_BY_NAME;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_LANGUAGES;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_QUALIFIER;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_TAGS;

@ServerSide
public class ProjectMeasuresIndex {
  private static final int FACET_DEFAULT_SIZE = 10;

  private static final String TERMS_AGG_SUFFIX = "_terms";
  private static final String NESTED_AGG_PREFIX = "nested_";
  private static final String FILTER_AGG_PREFIX = "filter_";

  private static final double[] LINES_THRESHOLDS = {1_000D, 10_000D, 100_000D, 500_000D};
  private static final double[] COVERAGE_THRESHOLDS = {30D, 50D, 70D, 80D};
  private static final double[] SECURITY_REVIEW_RATING_THRESHOLDS = {30D, 50D, 70D, 80D};
  private static final double[] DUPLICATIONS_THRESHOLDS = {3D, 5D, 10D, 20D};
  private static final int SCROLL_SIZE = 5000;

  public enum Facet {
    NCLOC(new RangeMeasureFacet(NCLOC_KEY, LINES_THRESHOLDS)),
    NEW_LINES(new RangeMeasureFacet(NEW_LINES_KEY, LINES_THRESHOLDS)),
    DUPLICATED_LINES_DENSITY(new RangeWithNoDataMeasureFacet(DUPLICATED_LINES_DENSITY_KEY, DUPLICATIONS_THRESHOLDS)),
    NEW_DUPLICATED_LINES_DENSITY(new RangeWithNoDataMeasureFacet(NEW_DUPLICATED_LINES_DENSITY_KEY, DUPLICATIONS_THRESHOLDS)),
    COVERAGE(new RangeWithNoDataMeasureFacet(COVERAGE_KEY, COVERAGE_THRESHOLDS)),
    NEW_COVERAGE(new RangeWithNoDataMeasureFacet(NEW_COVERAGE_KEY, COVERAGE_THRESHOLDS)),

    //RuleType ratings
    SQALE_RATING(new RatingMeasureFacet(SQALE_RATING_KEY)),
    NEW_MAINTAINABILITY_RATING(new RatingMeasureFacet(NEW_MAINTAINABILITY_RATING_KEY)),
    RELIABILITY_RATING(new RatingMeasureFacet(RELIABILITY_RATING_KEY)),
    NEW_RELIABILITY_RATING(new RatingMeasureFacet(NEW_RELIABILITY_RATING_KEY)),
    SECURITY_RATING(new RatingMeasureFacet(SECURITY_RATING_KEY)),
    NEW_SECURITY_RATING(new RatingMeasureFacet(NEW_SECURITY_RATING_KEY)),
    SECURITY_REVIEW_RATING(new RatingMeasureFacet(SECURITY_REVIEW_RATING_KEY)),
    NEW_SECURITY_REVIEW_RATING(new RatingMeasureFacet(NEW_SECURITY_REVIEW_RATING_KEY)),

    //Software quality ratings
    SOFTWARE_QUALITY_MAINTAINABILITY_RATING(new RatingMeasureFacet(SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY)),
    NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING(new RatingMeasureFacet(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY)),
    SOFTWARE_QUALITY_RELIABILITY_RATING(new RatingMeasureFacet(SOFTWARE_QUALITY_RELIABILITY_RATING_KEY)),
    NEW_SOFTWARE_QUALITY_RELIABILITY_RATING(new RatingMeasureFacet(NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY)),
    SOFTWARE_QUALITY_SECURITY_RATING(new RatingMeasureFacet(SOFTWARE_QUALITY_SECURITY_RATING_KEY)),
    NEW_SOFTWARE_QUALITY_SECURITY_RATING(new RatingMeasureFacet(NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY)),

    SECURITY_HOTSPOTS_REVIEWED(new RangeMeasureFacet(SECURITY_HOTSPOTS_REVIEWED_KEY, SECURITY_REVIEW_RATING_THRESHOLDS)),
    NEW_SECURITY_HOTSPOTS_REVIEWED(new RangeMeasureFacet(NEW_SECURITY_HOTSPOTS_REVIEWED_KEY, SECURITY_REVIEW_RATING_THRESHOLDS)),
    SCA_RATING_ANY_ISSUE(new RatingMeasureFacet(SCA_RATING_ANY_ISSUE_KEY)),
    NEW_SCA_RATING_ANY_ISSUE(new RatingMeasureFacet(NEW_SCA_RATING_ANY_ISSUE_KEY)),
    ALERT_STATUS(new MeasureFacet(ALERT_STATUS_KEY, ProjectMeasuresIndex::buildAlertStatusFacetV2)),
    LANGUAGES(FILTER_LANGUAGES, FIELD_LANGUAGES, STICKY, ProjectMeasuresIndex::buildLanguageFacetV2),
    QUALIFIER(FILTER_QUALIFIER, FIELD_QUALIFIER, STICKY, ProjectMeasuresIndex::buildQualifierFacetV2),
    TAGS(FILTER_TAGS, FIELD_TAGS, STICKY, ProjectMeasuresIndex::buildTagsFacetV2);

    private final String name;
    private final TopAggregationDefinition<?> topAggregation;
    private final FacetBuilderV2 facetBuilderV2;

    Facet(String name, String fieldName, boolean sticky, FacetBuilderV2 facetBuilderV2) {
      this.name = name;
      this.topAggregation = new SimpleFieldTopAggregationDefinition(fieldName, sticky);
      this.facetBuilderV2 = facetBuilderV2;
    }

    Facet(MeasureFacet measureFacet) {
      this.name = measureFacet.metricKey;
      this.topAggregation = measureFacet.topAggregation;
      this.facetBuilderV2 = measureFacet.facetBuilderV2;
    }

    public String getName() {
      return name;
    }

    public TopAggregationDefinition<?> getTopAggregationDef() {
      return topAggregation;
    }

    public TopAggregationDefinition.FilterScope getFilterScope() {
      return topAggregation.getFilterScope();
    }

    public FacetBuilderV2 getFacetBuilderV2() {
      return facetBuilderV2;
    }
  }

  private static final Map<String, Facet> FACETS_BY_NAME = Arrays.stream(Facet.values())
    .collect(Collectors.toMap(Facet::getName, Function.identity()));

  private final EsClient client;
  private final WebAuthorizationTypeSupport authorizationTypeSupport;
  private final System2 system2;

  public ProjectMeasuresIndex(EsClient client, WebAuthorizationTypeSupport authorizationTypeSupport, System2 system2) {
    this.client = client;
    this.authorizationTypeSupport = authorizationTypeSupport;
    this.system2 = system2;
  }

  public SearchIdResult<String> searchV2(ProjectMeasuresQuery query, SearchOptions searchOptions) {
    AllFilters allFilters = createFiltersV2(query);
    RequestFiltersComputer filtersComputer = createFiltersComputer(searchOptions, allFilters);

    co.elastic.clients.elasticsearch.core.SearchResponse<Object> response = client.searchV2(req -> {
      req.index(TYPE_PROJECT_MEASURES.getMainType().getIndex().getName())
        .source(s -> s.fetch(false))
        .trackTotalHits(t -> t.enabled(true))
        .from(searchOptions.getOffset())
        .size(searchOptions.getLimit());

      addFacetsV2(req, searchOptions, filtersComputer, query);
      addSortV2(query, req);

      filtersComputer.getQueryFiltersV2().ifPresent(req::query);
      filtersComputer.getPostFiltersV2().ifPresent(req::postFilter);
      return req;
    }, Object.class);

    return new SearchIdResult<>(response, id -> id, system2.getDefaultTimeZone().toZoneId());
  }

  private static RequestFiltersComputer createFiltersComputer(SearchOptions searchOptions, AllFilters allFilters) {
    Collection<String> facetNames = searchOptions.getFacets();
    Set<TopAggregationDefinition<?>> facets = facetNames.stream()
      .map(FACETS_BY_NAME::get)
      .filter(Objects::nonNull)
      .map(Facet::getTopAggregationDef)
      .collect(Collectors.toSet());
    return new RequestFiltersComputer(allFilters, facets);
  }

  public ProjectMeasuresStatistics searchSupportStatisticsV2() {
    co.elastic.clients.elasticsearch.core.SearchResponse<Object> response = client.searchV2(req -> req
      .index(TYPE_PROJECT_MEASURES.getMainType().getIndex().getName())
      .source(s -> s.fetch(false))
      .trackTotalHits(t -> t.enabled(true))
      .size(SCROLL_SIZE)
      .query(ES8QueryHelper.boolQuery(b -> b
        .filter(ES8QueryHelper.termQuery(FIELD_INDEX_TYPE, TYPE_PROJECT_MEASURES.getName()))
        .filter(ES8QueryHelper.termQuery(FIELD_QUALIFIER, ComponentQualifiers.PROJECT))))
      .aggregations(FIELD_LANGUAGES, Aggregation.of(a -> a.terms(t -> t
        .field(FIELD_LANGUAGES)
        .size(MAX_PAGE_SIZE)
        .minDocCount(1)
        .order(List.of(new NamedValue<>("_count", SortOrder.Desc))))))
      .aggregations(FIELD_NCLOC_DISTRIBUTION, Aggregation.of(a -> a
        .nested(n -> n.path(FIELD_NCLOC_DISTRIBUTION))
        .aggregations(FIELD_NCLOC_DISTRIBUTION + TERMS_AGG_SUFFIX, Aggregation.of(termsAgg -> termsAgg
          .terms(t -> t
            .field(FIELD_NCLOC_DISTRIBUTION_LANGUAGE)
            .size(MAX_PAGE_SIZE)
            .minDocCount(1)
            .order(List.of(new NamedValue<>("_count", SortOrder.Desc))))
          .aggregations(FIELD_NCLOC_DISTRIBUTION_NCLOC, Aggregation.of(sumAgg -> sumAgg
            .sum(s -> s.field(FIELD_NCLOC_DISTRIBUTION_NCLOC))))))))
      .aggregations(NCLOC_KEY, Aggregation.of(a -> a
        .nested(n -> n.path(FIELD_MEASURES))
        .aggregations(NCLOC_KEY + "_filter", Aggregation.of(filterAgg -> filterAgg
          .filter(ES8QueryHelper.termQuery(FIELD_MEASURES_MEASURE_KEY, NCLOC_KEY))
          .aggregations(NCLOC_KEY + "_filter_sum", Aggregation.of(sumAgg -> sumAgg
            .sum(s -> s.field(FIELD_MEASURES_MEASURE_VALUE))))))))
      .aggregations(AGGREGATION_PROJECTS_NOT_ANALYZED, Aggregation.of(a -> a
        .filter(ES8QueryHelper.boolQuery(b -> b.mustNot(ES8QueryHelper.existsQuery(FIELD_ANALYSED_AT)))))),
      Object.class);
    return buildProjectMeasuresStatisticsV2(response);
  }

  private static ProjectMeasuresStatistics buildProjectMeasuresStatisticsV2(co.elastic.clients.elasticsearch.core.SearchResponse<Object> response) {
    ProjectMeasuresStatistics.Builder statistics = ProjectMeasuresStatistics.builder();
    statistics.setProjectCount(response.hits().total().value());

    StringTermsAggregate languagesAgg = response.aggregations().get(FIELD_LANGUAGES).sterms();
    Map<String, Long> projectCountByLanguage = new LinkedHashMap<>();
    for (StringTermsBucket bucket : languagesAgg.buckets().array()) {
      projectCountByLanguage.put(bucket.key().stringValue(), bucket.docCount());
    }
    statistics.setProjectCountByLanguage(projectCountByLanguage);

    statistics.setProjectNotAnalyzedCount(response.aggregations().get(AGGREGATION_PROJECTS_NOT_ANALYZED).filter().docCount());

    NestedAggregate nclocDistribution = response.aggregations().get(FIELD_NCLOC_DISTRIBUTION).nested();
    StringTermsAggregate nclocTerms = nclocDistribution.aggregations().get(FIELD_NCLOC_DISTRIBUTION + TERMS_AGG_SUFFIX).sterms();
    Map<String, Long> nclocByLanguage = new LinkedHashMap<>();
    for (StringTermsBucket bucket : nclocTerms.buckets().array()) {
      double sum = bucket.aggregations().get(FIELD_NCLOC_DISTRIBUTION_NCLOC).sum().value();
      nclocByLanguage.put(bucket.key().stringValue(), Math.round(sum));
    }
    statistics.setNclocByLanguage(nclocByLanguage);

    return statistics.build();
  }

  private static void addSortV2(ProjectMeasuresQuery query, co.elastic.clients.elasticsearch.core.SearchRequest.Builder requestBuilder) {
    String sort = query.getSort();
    SortOrder order = query.isAsc() ? SortOrder.Asc : SortOrder.Desc;
    if (SORT_BY_NAME.equals(sort)) {
      requestBuilder.sort(s -> s.field(f -> f.field(DefaultIndexSettingsElement.SORTABLE_ANALYZER.subField(FIELD_NAME)).order(order)));
    } else if (SORT_BY_LAST_ANALYSIS_DATE.equals(sort)) {
      requestBuilder.sort(s -> s.field(f -> f.field(FIELD_ANALYSED_AT).order(order)));
    } else if (SORT_BY_CREATION_DATE.equals(sort)) {
      requestBuilder.sort(s -> s.field(f -> f.field(FIELD_CREATED_AT).order(order)));
    } else if (ALERT_STATUS_KEY.equals(sort)) {
      requestBuilder.sort(s -> s.field(f -> f.field(FIELD_QUALITY_GATE_STATUS).order(order)));
      requestBuilder.sort(s -> s.field(f -> f.field(DefaultIndexSettingsElement.SORTABLE_ANALYZER.subField(FIELD_NAME)).order(SortOrder.Asc)));
    } else {
      addMetricSortV2(query, requestBuilder, sort);
      requestBuilder.sort(s -> s.field(f -> f.field(DefaultIndexSettingsElement.SORTABLE_ANALYZER.subField(FIELD_NAME)).order(SortOrder.Asc)));
    }
    // last sort is by key in order to be deterministic when same value
    requestBuilder.sort(s -> s.field(f -> f.field(FIELD_KEY).order(SortOrder.Asc)));
  }

  private static void addMetricSortV2(ProjectMeasuresQuery query, co.elastic.clients.elasticsearch.core.SearchRequest.Builder requestBuilder, String sort) {
    SortOrder order = query.isAsc() ? SortOrder.Asc : SortOrder.Desc;
    requestBuilder.sort(s -> s.field(f -> f
      .field(FIELD_MEASURES_MEASURE_VALUE)
      .nested(n -> n.path(FIELD_MEASURES).filter(ES8QueryHelper.termQuery(FIELD_MEASURES_MEASURE_KEY, sort)))
      .order(order)));
  }

  private static void addFacetsV2(co.elastic.clients.elasticsearch.core.SearchRequest.Builder esRequest, SearchOptions options,
    RequestFiltersComputer filtersComputer, ProjectMeasuresQuery query) {
    TopAggregationHelper topAggregationHelper = new TopAggregationHelper(filtersComputer, new SubAggregationHelper());
    options.getFacets().stream()
      .map(FACETS_BY_NAME::get)
      .filter(Objects::nonNull)
      .forEach(facet -> esRequest.aggregations(facet.getName(),
        facet.getFacetBuilderV2().buildFacet(facet, query, topAggregationHelper)));
  }

  private AllFilters createFiltersV2(ProjectMeasuresQuery query) {
    AllFilters filters = RequestFiltersComputer.newAllFilters();
    filters.addFilterV2(
      "__indexType", new SimpleFieldFilterScope(FIELD_INDEX_TYPE),
      ES8QueryHelper.termQuery(FIELD_INDEX_TYPE, TYPE_PROJECT_MEASURES.getName()));
    if (!query.isIgnoreAuthorization()) {
      filters.addFilterV2("__authorization", new SimpleFieldFilterScope("parent"), authorizationTypeSupport.createQueryFilterV2());
    }
    Multimap<String, MetricCriterion> metricCriterionMultimap = ArrayListMultimap.create();
    query.getMetricCriteria()
      .forEach(metricCriterion -> metricCriterionMultimap.put(metricCriterion.getMetricKey(), metricCriterion));
    metricCriterionMultimap.asMap().forEach((key, value) -> {
      Query metricFilters = ES8QueryHelper.boolQuery(b -> value.stream()
        .map(ProjectMeasuresIndex::toQueryV2)
        .forEach(b::must));
      filters.addFilterV2(key, new NestedFieldFilterScope<>(FIELD_MEASURES, SUB_FIELD_MEASURES_KEY, key), metricFilters);
    });

    query.getQualityGateStatus().ifPresent(qualityGateStatus -> filters.addFilterV2(
      ALERT_STATUS_KEY, ALERT_STATUS.getFilterScope(),
      ES8QueryHelper.termQuery(FIELD_QUALITY_GATE_STATUS, QUALITY_GATE_STATUS.get(qualityGateStatus.name()))));

    query.getProjectUuids().ifPresent(projectUuids -> filters.addFilterV2(
      "ids", new SimpleFieldFilterScope("_id"),
      ES8QueryHelper.termsQuery("_id", projectUuids)));

    query.getLanguages()
      .ifPresent(languages -> filters.addFilterV2(FILTER_LANGUAGES, LANGUAGES.getFilterScope(), ES8QueryHelper.termsQuery(FIELD_LANGUAGES, languages)));

    query.getTags().ifPresent(tags -> filters.addFilterV2(FIELD_TAGS, TAGS.getFilterScope(), ES8QueryHelper.termsQuery(FIELD_TAGS, tags)));

    query.getQualifiers()
      .ifPresent(qualifiers -> filters.addFilterV2(FIELD_QUALIFIER, new SimpleFieldFilterScope(FIELD_QUALIFIER),
        ES8QueryHelper.termsQuery(FIELD_QUALIFIER, qualifiers)));

    query.getQueryText()
      .map(ProjectsTextSearchQueryFactory::createQueryV2)
      .ifPresent(q -> filters.addFilterV2("textQuery", new SimpleFieldFilterScope(FIELD_NAME), q));
    return filters;
  }

  private static Query toQueryV2(MetricCriterion criterion) {
    if (criterion.isNoData()) {
      return ES8QueryHelper.boolQuery(b -> b.mustNot(
        ES8QueryHelper.nestedQuery(
          FIELD_MEASURES,
          ES8QueryHelper.termQuery(FIELD_MEASURES_MEASURE_KEY, criterion.getMetricKey()),
          ChildScoreMode.Avg)));
    }
    return ES8QueryHelper.nestedQuery(
      FIELD_MEASURES,
      ES8QueryHelper.boolQuery(b -> b
        .filter(ES8QueryHelper.termQuery(FIELD_MEASURES_MEASURE_KEY, criterion.getMetricKey()))
        .filter(toValueQueryV2(criterion))),
      ChildScoreMode.Avg);
  }

  private static Query toValueQueryV2(MetricCriterion criterion) {
    String fieldName = FIELD_MEASURES_MEASURE_VALUE;

    switch (criterion.getOperator()) {
      case GT:
        return ES8QueryHelper.rangeQueryGt(fieldName, criterion.getValue());
      case GTE:
        return ES8QueryHelper.rangeQueryGte(fieldName, criterion.getValue());
      case LT:
        return ES8QueryHelper.rangeQueryLt(fieldName, criterion.getValue());
      case LTE:
        return ES8QueryHelper.rangeQueryLte(fieldName, criterion.getValue());
      case EQ:
        return Query.of(q -> q.term(t -> t.field(fieldName).value(criterion.getValue())));
      default:
        throw new IllegalStateException("Metric criteria non supported: " + criterion.getOperator().name());
    }
  }

  public List<String> searchTagsV2(@Nullable String textQuery, int page, int size) {
    int maxPageSize = 100;
    int maxPage = 20;
    checkArgument(size <= maxPageSize, "Page size must be lower than or equals to " + maxPageSize);
    checkArgument(page > 0 && page <= maxPage, "Page must be between 0 and " + maxPage);

    if (size <= 0) {
      return emptyList();
    }

    int totalSize = size * page;
    co.elastic.clients.elasticsearch.core.SearchResponse<Object> response = client.searchV2(req -> {
      req.index(TYPE_PROJECT_MEASURES.getMainType().getIndex().getName())
        .query(authorizationTypeSupport.createQueryFilterV2())
        .source(s -> s.fetch(false))
        .aggregations(FIELD_TAGS, Aggregation.of(a -> a.terms(t -> {
          t.field(FIELD_TAGS)
            .size(totalSize)
            .minDocCount(1)
            .order(List.of(new NamedValue<>("_key", SortOrder.Asc)));
          if (textQuery != null) {
            t.include(i -> i.regexp(".*" + escapeSpecialRegexChars(textQuery) + ".*"));
          }
          return t;
        })));
      return req;
    }, Object.class);

    StringTermsAggregate aggregation = response.aggregations().get(FIELD_TAGS).sterms();
    return aggregation.buckets().array().stream()
      .skip((page - 1L) * size)
      .map(b -> b.key().stringValue())
      .toList();
  }

  private interface FacetBuilderV2 {
    Aggregation buildFacet(Facet facet, ProjectMeasuresQuery query, TopAggregationHelper topAggregationHelper);
  }

  /**
   * A sticky facet on field {@link ProjectMeasuresIndexDefinition#FIELD_MEASURES_MEASURE_KEY}.
   */
  private static class MeasureFacet {
    private final String metricKey;
    private final TopAggregationDefinition<?> topAggregation;
    private final FacetBuilderV2 facetBuilderV2;

    private MeasureFacet(String metricKey, FacetBuilderV2 facetBuilderV2) {
      this.metricKey = metricKey;
      this.topAggregation = new NestedFieldTopAggregationDefinition<>(FIELD_MEASURES_MEASURE_KEY, metricKey, STICKY);
      this.facetBuilderV2 = facetBuilderV2;
    }
  }

  private static final class RangeMeasureFacet extends MeasureFacet {

    private RangeMeasureFacet(String metricKey, double[] thresholds) {
      super(metricKey, new MetricRangeFacetBuilderV2(metricKey, thresholds));
    }

    private static final class MetricRangeFacetBuilderV2 implements FacetBuilderV2 {
      private final String metricKey;
      private final double[] thresholds;

      private MetricRangeFacetBuilderV2(String metricKey, double[] thresholds) {
        this.metricKey = metricKey;
        this.thresholds = thresholds;
      }

      @Override
      public Aggregation buildFacet(Facet facet, ProjectMeasuresQuery query, TopAggregationHelper topAggregationHelper) {
        return topAggregationHelper.buildTopAggregationV2(
          facet.getName(), facet.getTopAggregationDef(),
          TopAggregationHelper.NO_EXTRA_FILTER_V2,
          subAggs -> subAggs.put(NESTED_AGG_PREFIX + metricKey, createRangeFacetV2(metricKey, thresholds)));
      }
    }
  }

  private static final class RangeWithNoDataMeasureFacet extends MeasureFacet {

    private RangeWithNoDataMeasureFacet(String metricKey, double[] thresholds) {
      super(metricKey, new MetricRangeWithNoDataFacetBuilderV2(metricKey, thresholds));
    }

    private static final class MetricRangeWithNoDataFacetBuilderV2 implements FacetBuilderV2 {
      private final String metricKey;
      private final double[] thresholds;

      private MetricRangeWithNoDataFacetBuilderV2(String metricKey, double[] thresholds) {
        this.metricKey = metricKey;
        this.thresholds = thresholds;
      }

      @Override
      public Aggregation buildFacet(Facet facet, ProjectMeasuresQuery query, TopAggregationHelper topAggregationHelper) {
        return topAggregationHelper.buildTopAggregationV2(
          facet.getName(), facet.getTopAggregationDef(),
          TopAggregationHelper.NO_EXTRA_FILTER_V2,
          subAggs -> {
            subAggs.put(NESTED_AGG_PREFIX + metricKey, createRangeFacetV2(metricKey, thresholds));
            subAggs.put("no_data_" + metricKey, createNoDataFacetV2(metricKey));
          });
      }

      private static Aggregation createNoDataFacetV2(String metricKey) {
        return Aggregation.of(a -> a.filter(ES8QueryHelper.boolQuery(b -> b.mustNot(
          ES8QueryHelper.nestedQuery(FIELD_MEASURES, ES8QueryHelper.termQuery(FIELD_MEASURES_MEASURE_KEY, metricKey), ChildScoreMode.Avg)))));
      }
    }
  }

  private static class RatingMeasureFacet extends MeasureFacet {

    private RatingMeasureFacet(String metricKey) {
      super(metricKey, new MetricRatingFacetBuilderV2(metricKey));
    }

    private static class MetricRatingFacetBuilderV2 implements FacetBuilderV2 {
      private final String metricKey;

      private MetricRatingFacetBuilderV2(String metricKey) {
        this.metricKey = metricKey;
      }

      @Override
      public Aggregation buildFacet(Facet facet, ProjectMeasuresQuery query, TopAggregationHelper topAggregationHelper) {
        return topAggregationHelper.buildTopAggregationV2(
          facet.getName(), facet.getTopAggregationDef(),
          TopAggregationHelper.NO_EXTRA_FILTER_V2,
          subAggs -> subAggs.put(NESTED_AGG_PREFIX + metricKey, createMeasureRatingFacetV2(metricKey)));
      }

      private static Aggregation createMeasureRatingFacetV2(String metricKey) {
        Map<String, Query> ratingFilters = new LinkedHashMap<>();
        ratingFilters.put("1", ES8QueryHelper.termQuery(FIELD_MEASURES_MEASURE_VALUE, 1L));
        ratingFilters.put("2", ES8QueryHelper.termQuery(FIELD_MEASURES_MEASURE_VALUE, 2L));
        ratingFilters.put("3", ES8QueryHelper.termQuery(FIELD_MEASURES_MEASURE_VALUE, 3L));
        ratingFilters.put("4", ES8QueryHelper.termQuery(FIELD_MEASURES_MEASURE_VALUE, 4L));
        ratingFilters.put("5", ES8QueryHelper.termQuery(FIELD_MEASURES_MEASURE_VALUE, 5L));

        Aggregation filtersAgg = Aggregation.of(a -> a.filters(f -> f.filters(fb -> fb.keyed(ratingFilters))));

        Aggregation filterAgg = Aggregation.of(a -> a
          .filter(ES8QueryHelper.termsQuery(FIELD_MEASURES_MEASURE_KEY, List.of(metricKey)))
          .aggregations(metricKey, filtersAgg));

        return Aggregation.of(a -> a
          .nested(n -> n.path(FIELD_MEASURES))
          .aggregations(FILTER_AGG_PREFIX + metricKey, filterAgg));
      }
    }
  }

  private static Aggregation buildLanguageFacetV2(Facet facet, ProjectMeasuresQuery query, TopAggregationHelper topAggregationHelper) {
    Consumer<Map<String, Aggregation>> extraSubAgg = subAggs -> query.getLanguages()
      .flatMap(languages -> topAggregationHelper.getSubAggregationHelper()
        .buildSelectedItemsAggregationV2(facet.getTopAggregationDef(), languages.toArray()))
      .ifPresent(agg -> subAggs.put(FILTER_LANGUAGES + Facets.SELECTED_SUB_AGG_NAME_SUFFIX, agg));
    return topAggregationHelper.buildTermTopAggregationV2(
      FILTER_LANGUAGES, facet.getTopAggregationDef(), FACET_DEFAULT_SIZE,
      TopAggregationHelper.NO_EXTRA_FILTER_V2, extraSubAgg);
  }

  private static Aggregation buildAlertStatusFacetV2(Facet facet, ProjectMeasuresQuery query, TopAggregationHelper topAggregationHelper) {
    return topAggregationHelper.buildTopAggregationV2(
      facet.getName(), facet.getTopAggregationDef(),
      TopAggregationHelper.NO_EXTRA_FILTER_V2,
      subAggs -> subAggs.put(ALERT_STATUS_KEY, createQualityGateFacetV2()));
  }

  private static Aggregation buildTagsFacetV2(Facet facet, ProjectMeasuresQuery query, TopAggregationHelper topAggregationHelper) {
    Consumer<Map<String, Aggregation>> extraSubAgg = subAggs -> query.getTags()
      .flatMap(tags -> topAggregationHelper.getSubAggregationHelper()
        .buildSelectedItemsAggregationV2(facet.getTopAggregationDef(), tags.toArray()))
      .ifPresent(agg -> subAggs.put(FILTER_TAGS + Facets.SELECTED_SUB_AGG_NAME_SUFFIX, agg));
    return topAggregationHelper.buildTermTopAggregationV2(
      FILTER_TAGS, facet.getTopAggregationDef(), FACET_DEFAULT_SIZE,
      TopAggregationHelper.NO_EXTRA_FILTER_V2, extraSubAgg);
  }

  private static Aggregation buildQualifierFacetV2(Facet facet, ProjectMeasuresQuery query, TopAggregationHelper topAggregationHelper) {
    return topAggregationHelper.buildTopAggregationV2(
      facet.getName(), facet.getTopAggregationDef(),
      TopAggregationHelper.NO_EXTRA_FILTER_V2,
      subAggs -> subAggs.put(FILTER_QUALIFIER, createQualifierFacetV2()));
  }

  private static Aggregation createRangeFacetV2(String metricKey, double[] thresholds) {
    final int lastIndex = thresholds.length - 1;
    Aggregation rangeAgg = Aggregation.of(a -> a.range(r -> {
      r.field(FIELD_MEASURES_MEASURE_VALUE);
      IntStream.range(0, thresholds.length)
        .forEach(i -> {
          if (i == 0) {
            r.ranges(ra -> ra.to(thresholds[0]));
            r.ranges(ra -> ra.from(thresholds[0]).to(thresholds[1]));
          } else if (i == lastIndex) {
            r.ranges(ra -> ra.from(thresholds[lastIndex]));
          } else {
            r.ranges(ra -> ra.from(thresholds[i]).to(thresholds[i + 1]));
          }
        });
      return r;
    }));

    Aggregation filterAgg = Aggregation.of(a -> a
      .filter(ES8QueryHelper.termsQuery(FIELD_MEASURES_MEASURE_KEY, List.of(metricKey)))
      .aggregations(metricKey, rangeAgg));

    return Aggregation.of(a -> a
      .nested(n -> n.path(FIELD_MEASURES))
      .aggregations(FILTER_AGG_PREFIX + metricKey, filterAgg));
  }

  private static Aggregation createQualityGateFacetV2() {
    Map<String, Query> qgFilters = new LinkedHashMap<>();
    QUALITY_GATE_STATUS.forEach((key, value) -> qgFilters.put(key, ES8QueryHelper.termQuery(FIELD_QUALITY_GATE_STATUS, value)));
    return Aggregation.of(a -> a.filters(f -> f.filters(fb -> fb.keyed(qgFilters))));
  }

  private static Aggregation createQualifierFacetV2() {
    Map<String, Query> qualifierFilters = new LinkedHashMap<>();
    qualifierFilters.put(ComponentQualifiers.APP, ES8QueryHelper.termQuery(FIELD_QUALIFIER, ComponentQualifiers.APP));
    qualifierFilters.put(ComponentQualifiers.PROJECT, ES8QueryHelper.termQuery(FIELD_QUALIFIER, ComponentQualifiers.PROJECT));
    return Aggregation.of(a -> a.filters(f -> f.filters(fb -> fb.keyed(qualifierFilters))));
  }

}
